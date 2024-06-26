package com.dylibso.chicory.generated;

import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.Instance;

public class Sum {

    private static final com.dylibso.chicory.wasm.Module wasmModule = Module.builder("sum.rust.wasm").withInitialize(false).withStart(false).build().wasmModule();

    private final Module module;

    private final Instance instance;

    public Sum() {
        module = Module.builder(wasmModule).build();
        instance = module.instantiate();
    }

    public int add(int arg0, int arg1) {
        var func = instance.export("add");
        var result = func.apply(new Value[]{Value.i32(arg0), Value.i32(arg1)});
        return result[0].asInt();
    }

    public int memory(int arg0, int arg1) {
        var func = instance.export("memory");
        var result = func.apply(new Value[]{Value.i32(arg0), Value.i32(arg1)});
        return result[0].asInt();
    }
}
