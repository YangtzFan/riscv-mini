// See LICENSE for license details.

package mini

import chisel3._
import mini.Multiplier

class MatrixMultiplier(val M: Int, val K: Int, val N: Int, val width: Int) extends RawModule {
  require(M > 0 && K > 0 && N > 0 && width > 0)

  val io = IO(new Bundle {
    val A = Input(Vec(M, Vec(K, UInt(width.W))))
    val B = Input(Vec(K, Vec(N, UInt(width.W))))
    val C = Output(Vec(M, Vec(N, UInt(width.W))))
  })

  for (i <- 0 until M; j <- 0 until N) {
    val products = (0 until K).map { k =>
      val multiplier = Module(new Multiplier(width))
      multiplier.io.A := io.A(i)(k)
      multiplier.io.B := io.B(k)(j)
      multiplier.io.out(width - 1, 0)
    }
    // Keep the result modulo 2^width, as required by the output data width.
    io.C(i)(j) := products.reduce(_ + _)
  }
}
