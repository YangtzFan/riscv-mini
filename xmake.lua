---@diagnostic disable: undefined-global

local prj_dir = os.scriptdir()

target("rtl", function()
    set_kind("phony")
    set_default(false)
    on_run(function()
        os.execv("mill", {
            "-i", "riscvMini.runMain", "mini.Main",
            "--target", "systemverilog", "--split-verilog",
            "-td", path.join(prj_dir, "build", "rtl")
        }, { curdir = prj_dir })
    end)
end)

target("clean", function()
    set_kind("phony")
    set_default(false)
    on_run(function()
        os.tryrm(path.join(prj_dir, "build"))
    end)
end)
