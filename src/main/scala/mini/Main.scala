// See LICENSE for license details.

package mini

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.ChiselStage

object Main extends App {
  val config = MiniConfig()
  val options =
    if (args.nonEmpty) args
    else Array("--target", "systemverilog", "--split-verilog", "-td", "build/rtl")

  (new ChiselStage).execute(
    options,
    Seq(ChiselGeneratorAnnotation(() => Tile(config)))
  )
}
