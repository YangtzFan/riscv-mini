// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._

object Alu {
  val ALU_ADD = 0.U(5.W)
  val ALU_SUB = 1.U(5.W)
  val ALU_AND = 2.U(5.W)
  val ALU_OR = 3.U(5.W)
  val ALU_XOR = 4.U(5.W)
  val ALU_SLT = 5.U(5.W)
  val ALU_SLL = 6.U(5.W)
  val ALU_SLTU = 7.U(5.W)
  val ALU_SRL = 8.U(5.W)
  val ALU_SRA = 9.U(5.W)
  val ALU_COPY_A = 10.U(5.W)
  val ALU_COPY_B = 11.U(5.W)
  // Multiply opcodes: bit 4 = 1, bit 3 = 0, bits 2:0 = funct3.
  val ALU_MUL = 16.U(5.W)
  val ALU_MULH = 17.U(5.W)
  val ALU_MULHSU = 18.U(5.W)
  val ALU_MULHU = 19.U(5.W)
  val ALU_XXX = 15.U(5.W)
}

class AluIO(width: Int) extends Bundle {
  val A = Input(UInt(width.W))
  val B = Input(UInt(width.W))
  val alu_op = Input(UInt(5.W))
  val out = Output(UInt(width.W))
  val sum = Output(UInt(width.W))
}

import mini.Alu._

trait Alu extends Module {
  def width: Int
  val io: AluIO
}

class AluSimple(val width: Int) extends Alu {
  val io = IO(new AluIO(width))

  val shamt = io.B(4, 0).asUInt

  io.out := MuxLookup(io.alu_op, io.B)(
    Seq(
      ALU_ADD -> (io.A + io.B),
      ALU_SUB -> (io.A - io.B),
      ALU_SRA -> (io.A.asSInt >> shamt).asUInt,
      ALU_SRL -> (io.A >> shamt),
      ALU_SLL -> (io.A << shamt),
      ALU_SLT -> (io.A.asSInt < io.B.asSInt),
      ALU_SLTU -> (io.A < io.B),
      ALU_AND -> (io.A & io.B),
      ALU_OR -> (io.A | io.B),
      ALU_XOR -> (io.A ^ io.B),
      ALU_COPY_A -> io.A
    )
  )

  io.sum := io.A + Mux(io.alu_op(0), -io.B, io.B)
}

class AluArea(val width: Int) extends Alu {
  val io = IO(new AluIO(width))
  val sum = io.A + Mux(io.alu_op(0), -io.B, io.B)
  val cmp =
    Mux(io.A(width - 1) === io.B(width - 1), sum(width - 1), Mux(io.alu_op(1), io.B(width - 1), io.A(width - 1)))
  val shamt = io.B(4, 0).asUInt
  val shin = Mux(io.alu_op(3), io.A, Reverse(io.A))
  val shiftr = (Cat(io.alu_op(0) && shin(width - 1), shin).asSInt >> shamt)(width - 1, 0)
  val shiftl = Reverse(shiftr)

  val out =
    Mux(
      io.alu_op === ALU_ADD || io.alu_op === ALU_SUB,
      sum,
      Mux(
        io.alu_op === ALU_SLT || io.alu_op === ALU_SLTU,
        cmp,
        Mux(
          io.alu_op === ALU_SRA || io.alu_op === ALU_SRL,
          shiftr,
          Mux(
            io.alu_op === ALU_SLL,
            shiftl,
            Mux(
              io.alu_op === ALU_AND,
              io.A & io.B,
              Mux(
                io.alu_op === ALU_OR,
                io.A | io.B,
                Mux(io.alu_op === ALU_XOR, io.A ^ io.B, Mux(io.alu_op === ALU_COPY_A, io.A, io.B))
              )
            )
          )
        )
      )
    )

  io.out := out
  io.sum := sum
}

// Combinational unsigned multiplication using shifted partial products.
class Multiplier(val width: Int) extends Module {
  require(width > 0)

  val io = IO(new Bundle {
    val A = Input(UInt(width.W))
    val B = Input(UInt(width.W))
    val out = Output(UInt((width + width).W))
  })

  val fullWidth = width + width
  val extendedA = Cat(0.U(width.W), io.A)
  val partialProducts = (0 until width).map { bit =>
    val shiftedA = (extendedA << bit)(fullWidth - 1, 0)
    Mux(io.B(bit), shiftedA, 0.U(fullWidth.W))
  }
  io.out := partialProducts.reduce(_ + _)
}

// Adds RV32M multiply results without duplicating the existing ALU operations.
class AluMul(val width: Int, is_area: Boolean = true) extends Alu {
  val io = IO(new AluIO(width))
  val base_alu = if (is_area) Module(new AluArea(width)) else Module(new AluSimple(width))
  val multiplier = Module(new Multiplier(width))

  base_alu.io.A := io.A
  base_alu.io.B := io.B
  base_alu.io.alu_op := io.alu_op
  multiplier.io.A := io.A
  multiplier.io.B := io.B

  val product = multiplier.io.out
  val highUnsigned = product(width + width - 1, width)
  val highSignedUnsigned = highUnsigned - Mux(io.A(width - 1), io.B, 0.U(width.W))
  val highSigned = highSignedUnsigned - Mux(io.B(width - 1), io.A, 0.U(width.W))

  io.out := MuxLookup(io.alu_op, base_alu.io.out)(
    Seq(
      ALU_MUL -> product(width - 1, 0),
      ALU_MULH -> highSigned,
      ALU_MULHSU -> highSignedUnsigned,
      ALU_MULHU -> highUnsigned
    )
  )
  io.sum := base_alu.io.sum
}
