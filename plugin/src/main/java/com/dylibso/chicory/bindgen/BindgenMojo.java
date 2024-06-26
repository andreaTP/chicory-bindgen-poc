package com.dylibso.chicory.bindgen;

import com.dylibso.chicory.runtime.Module;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.util.List;
import java.util.Locale;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin should generate bindings for exported functions
 */
@Mojo(name = "wasm-bindgen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class BindgenMojo extends AbstractMojo {

    private final Log log = new SystemStreamLog();

    @Parameter(property = "wasm-bindgen.files", required = true)
    private List<File> files;

    @Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/java")
    private File targetDirectory;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        targetDirectory.mkdirs();
        final SourceRoot dest = new SourceRoot(targetDirectory.toPath());
        for (var file : files) {
            var cu = new CompilationUnit("com.dylibso.chicory.generated");
            var wasmFile =
                    file.toPath()
                            .getParent()
                            .resolve("src")
                            .resolve("main")
                            .resolve("resources")
                            .resolve(file.getName())
                            .toFile();

            log.info("Generating bindgen for module " + file.getName());
            var moduleName = file.getName().split("\\.")[0];
            moduleName =
                    moduleName.substring(0, 1).toUpperCase(Locale.ROOT) + moduleName.substring(1);

            cu.setStorage(
                    targetDirectory
                            .toPath()
                            .resolve("com")
                            .resolve("dylibso")
                            .resolve("chicory")
                            .resolve("generated")
                            .resolve(moduleName + ".java"));

            cu.addImport("com.dylibso.chicory.wasm.types.Value");
            cu.addImport("com.dylibso.chicory.runtime.Module");
            cu.addImport("com.dylibso.chicory.runtime.Instance");

            var moduleClass = cu.addClass(moduleName);

            var wasmModuleField =
                    moduleClass.addField(
                            "com.dylibso.chicory.wasm.Module",
                            "wasmModule",
                            Modifier.Keyword.PRIVATE,
                            Modifier.Keyword.STATIC,
                            Modifier.Keyword.FINAL);
            wasmModuleField
                    .getVariable(0)
                    .setInitializer(
                            "Module.builder(\""
                                    + file.getName()
                                    + "\").withInitialize(false).withStart(false).build().wasmModule()");

            moduleClass.addField(
                    "Module", "module", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
            moduleClass.addField(
                    "Instance", "instance", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

            var constructor = moduleClass.addConstructor(Modifier.Keyword.PUBLIC);
            constructor.setBody(
                    new BlockStmt()
                            .addStatement(
                                    new NameExpr("module = Module.builder(wasmModule).build()"))
                            .addStatement(new NameExpr("instance = module.instantiate()")));

            var module = Module.builder(wasmFile).withInitialize(false).withStart(false).build();
            var instance = module.instantiate();
            var exports = module.exports();

            for (var export : exports.entrySet()) {
                var exportSig = module.export(export.getKey());

                try {
                    var typeId = instance.functionType(exportSig.index());
                    var type = instance.type(typeId);

                    var method = moduleClass.addMethod(export.getKey(), Modifier.Keyword.PUBLIC);
                    for (var i = 0; i < type.params().size(); i++) {
                        method.addParameter("int", "arg" + i);
                    }

                    assert (type.returns().size() <= 1);

                    if (type.returns().size() > 0) {
                        method.setType("int");
                    } else {
                        method.setType("void");
                    }

                    var methodBody = method.createBody();

                    var params = "";
                    for (var i = 0; i < type.params().size(); i++) {
                        if (i != 0) {
                            params += ", ";
                        }
                        params += "Value.i32(arg" + i + ")";
                    }

                    methodBody
                            .addStatement(
                                    new NameExpr(
                                            "var func = instance.export(\""
                                                    + export.getKey()
                                                    + "\")"))
                            .addStatement(
                                    new NameExpr(
                                            "var result = func.apply(new Value[]{"
                                                    + params
                                                    + "})"));

                    if (type.returns().size() > 0) {
                        methodBody.addStatement(new NameExpr("return result[0].asInt()"));
                    }
                } catch (Exception e) {
                    log.debug("Skipping", e);
                }
            }

            dest.add(cu);
        }

        dest.saveAll(targetDirectory.toPath());
        project.addCompileSourceRoot(targetDirectory.getPath());
    }
}
