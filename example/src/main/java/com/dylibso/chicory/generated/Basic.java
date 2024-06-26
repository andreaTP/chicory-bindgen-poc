package com.dylibso.chicory.generated;

import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.Instance;

public class Basic {

    private static final com.dylibso.chicory.wasm.Module wasmModule = Module.builder("basic.c.wasm").withInitialize(false).withStart(false).build().wasmModule();

    private final Module module;

    private final Instance instance;

    public Basic() {
        module = Module.builder(wasmModule).build();
        instance = module.instantiate();
    }

    public int memory() {
        var func = instance.export("memory");
        var result = func.apply(new Value[]{});
        return result[0].asInt();
    }

    public int run() {
        var func = instance.export("run");
        var result = func.apply(new Value[]{});
        return result[0].asInt();
    }
}
