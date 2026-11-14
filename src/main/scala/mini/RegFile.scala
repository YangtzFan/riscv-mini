// See LICENSE for license details.

package mini

import chisel3._

class MatrixIO(xlen: Int) extends Bundle {
  val a = Output(Vec(6, UInt(xlen.W)))
  val b = Output(Vec(15, UInt(xlen.W)))
  val c = Input(Vec(10, UInt(xlen.W)))
  val valid = Input(Bool())
}

class RegFileIO(xlen: Int) extends Bundle {
  val raddr1 = Input(UInt(5.W))
  val raddr2 = Input(UInt(5.W))
  val rdata1 = Output(UInt(xlen.W))
  val rdata2 = Output(UInt(xlen.W))
  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
  val wdata = Input(UInt(xlen.W))
}

class RegFile(xlen: Int) extends Module {
  val io = IO(new RegFileIO(xlen))
  val matrix = IO(new MatrixIO(xlen))
  val regs = Mem(32, UInt(xlen.W))
  io.rdata1 := Mux(io.raddr1.orR, regs(io.raddr1), 0.U)
  io.rdata2 := Mux(io.raddr2.orR, regs(io.raddr2), 0.U)

  // A: x1-x6 (2x3), B: x7-x21 (3x5), C: x22-x31 (2x5).
  for (i <- 0 until 6) {
    matrix.a(i) := regs(1 + i)
  }
  for (i <- 0 until 15) {
    matrix.b(i) := regs(7 + i)
  }
  when(matrix.valid) {
    for (i <- 0 until 10) {
      regs(22 + i) := matrix.c(i)
    }
  }

  // A matrix write takes priority only for a simultaneous scalar write to x22-x31.
  when(io.wen && io.waddr.orR && !(matrix.valid && io.waddr >= 22.U)) {
    regs(io.waddr) := io.wdata
  }
}
