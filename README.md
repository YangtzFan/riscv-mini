# riscv-mini RTL

This repository contains the riscv-mini Chisel hardware sources and their
SystemVerilog generation flow, following the ChiselTemplate project layout.
The processor implements RV32I with instruction and data caches. The generated
top-level module is `Tile`.

## Build

The tool versions match ChiselTemplate:

- Mill 1.0.6 (`.mill-version`)
- Scala 2.13.18
- Chisel 7.9.0, including the matching compiler plugin

Generate split SystemVerilog with Mill:

```bash
mill -i riscvMini.runMain mini.Main \
  --target systemverilog --split-verilog -td build/rtl
```

Or use the xmake wrapper:

```bash
xmake r rtl
```

`xmake r clean` removes `build/`. Mill's own cache is in `out/`.

## Layout

```text
.
├── .mill-version
├── .scalafmt.conf
├── build.sc
├── xmake.lua
├── LICENSE
└── src/main/scala/
    ├── junctions/nasti.scala
    └── mini/                 # Core, caches, Tile, and mini.Main generator
```

Simulation environments, stimulus, program images, and checkers belong in the
parent verification project. This repository has no Scala test target, C++
simulator, benchmark programs, or verification toolchain setup scripts.

Original riscv-mini author: Donggyu Kim (dgkim@eecs.berkeley.edu).
See `LICENSE` for the retained upstream license.
