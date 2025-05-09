//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.FileUnit;
import sric.compiler.ast.Loc;
import sric.compiler.ast.SModule;
import sric.compiler.ast.SModule.Depend;
import sric.compiler.backend.CppGenerator;
import sric.compiler.backend.DocGenerator;
import sric.compiler.backend.ScLibGenerator;
import sric.compiler.parser.DeepParser;
import sric.compiler.resolve.ErrorChecker;
import sric.compiler.resolve.ExprTypeResolver;
import sric.compiler.resolve.TopLevelTypeResolver;

/**
 *
 * @author yangjiandong
 */
public class Compiler {

    public ArrayList<File> sources;
    public SModule module;
    public CompilerLog log;
    
    public String outputDir;
    public String libPath;
    
    public boolean genCode = true;
    public boolean print = true;
    
    private HashMap<String, SModule> moduleCache = new HashMap<>();
    
    private HashMap<String, String> fmakeArgs = null;
    
    String cppVersion = null;
    
    public Compiler(SModule module, File sourceDir, String libPath, String outputDir) {
        this.module = module;
        log = new CompilerLog();
        if (sourceDir.isDirectory()) {
            this.sources = Util.listFile(sourceDir);
        }
        else {
            this.sources = new ArrayList<File>();
            this.sources.add(sourceDir);
        }
        this.libPath = libPath;
        this.outputDir = outputDir;
    }
    
    private static ArrayList<Depend> listDepends(File libDir) {
        ArrayList<Depend> depends = new ArrayList<Depend>();
        File[] list = libDir.listFiles();
        for (File file2 : list) {
            if (!file2.getName().endsWith(".meta")) {
                continue;
            }
            Depend depend = new Depend();
            depend.name = Util.getBaseName(file2.getName());
            depend.version = "1.0";
            depends.add(depend);
        }
        return depends;
    }
    
    public static Compiler makeDefault(String sourcePath, String libPath) {
        File sourceDir = new File(sourcePath);
        
        SModule module = new SModule();
        module.name = Util.getBaseName(sourceDir.getName());
        module.version = "1.0";
        module.sourcePath = new File(sourcePath).getAbsolutePath();
        module.outType = "exe";
        module.scriptMode = true;
        if (sourcePath.endsWith(".sch")) {
            module.isStubFile = true;
        }
        File libDir = new File(libPath);
        module.depends= listDepends(libDir);
        return new Compiler(module, sourceDir, libPath, libDir.getParent()+"/output/");
    }
    
    public static Compiler fromProps(String propsPath, String libPath) throws IOException {
        return fromProps(propsPath, libPath, null);
    }
    
    public static Compiler fromProps(String propsPath, String libPath, String srcDirs) throws IOException {
        var props = Util.readProps(propsPath);
        HashMap<String, String> fmakeArgs = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : props.entrySet()) {
            if (entry.getKey().startsWith("fmake.")) {
                String key = entry.getKey().substring("fmake.".length());
                fmakeArgs.put(key, entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : fmakeArgs.entrySet()) {
            props.remove("fmake."+entry.getKey());
        }
        
        SModule module = SModule.fromProps(props);
        if (srcDirs == null) {
            srcDirs = props.get("srcDirs");
            if (srcDirs == null) {
                throw new RuntimeException("Unknow srcDirs");
            }
            else {
                String parent = new File(propsPath).getParent();
                if (parent != null) {
                    srcDirs =  parent + "/" + srcDirs;
                }
            }
        }
        module.sourcePath = new File(propsPath).getAbsolutePath();
        File sourceDir = new File(srcDirs);
        File libDir = new File(libPath);
        Compiler c = new Compiler(module, sourceDir, libPath, libDir.getParent()+"/output/");
        c.fmakeArgs = fmakeArgs;
        return c;
    }
    
    public boolean run() throws IOException {
        for (File file : sources) {
            AstNode.FileUnit funit = parse(file);
            funit.module = module;
            module.fileUnits.add(funit);
        }
        
        if (log.hasError()) {
            if (print) {
                log.printError();
            }
            return false;
        }
        
        typeCheck();
        
        if (log.hasError()) {
            if (print) {
                log.printError();
            }
            return false;
        }

        if (genCode) {
            genOutput();
        }
        return true;
    }
    
    public AstNode.FileUnit updateFile(String file, String src) {
        this.log.removeByFile(file);
        
        AstNode.FileUnit funit = new AstNode.FileUnit(file);
        try {
            DeepParser parser = new DeepParser(log, src, funit);
            parser.parse();
            funit.module = module;

            for (FileUnit f : module.fileUnits) {
                if (f.file.endsWith(funit.file)) {
                    module.fileUnits.remove(f);
                    break;
                }
            }
            module.fileUnits.add(funit);
            module.clearCache();

            typeCheck();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return funit;
    }
    
    public SModule importModule(String moduleName, String version, Loc loc) {
        SModule s = moduleCache.get(moduleName);
        if (s != null) {
            return s;
        }
        
        String libFile = libPath + "/" + moduleName;
        try {
            Compiler compiler = Compiler.fromProps(libFile+".meta", libPath, libFile+".sch");
            compiler.genCode = false;
            compiler.moduleCache = this.moduleCache;
            compiler.run();
            
            moduleCache.put(moduleName, compiler.module);
            return compiler.module;
            
        } catch (Exception ex) {
            log.err("Load lib fail:"+libFile+".meta", loc);
            return null;
        }
    }
    
    private void typeCheck() {
        TopLevelTypeResolver slotResolver = new TopLevelTypeResolver(log, module, this);
        slotResolver.run();
        
//        if (log.hasError()) {
//            return;
//        }
        
        ExprTypeResolver exprResolver = new ExprTypeResolver(log, module);
        exprResolver.run();
        
        ErrorChecker errorChecker = new ErrorChecker(log, module);
        errorChecker.run();
        
    }
    
    public AstNode.FileUnit parse(File file) throws IOException {
        String src = Files.readString(file.toPath());
        
        AstNode.FileUnit unit = new AstNode.FileUnit(file.getCanonicalPath());
        DeepParser parser = new DeepParser(log, src, unit);
        parser.parse();
        return unit;
    }
    
    public void genOutput() throws IOException {
        String libFile = libPath + "/" + this.module.name;
        ScLibGenerator scGenerator = new ScLibGenerator(log, libFile + ".sch");
        scGenerator.run(module);
        
        var props = this.module.toMetaProps();
        Util.writeProps(libFile+".meta", props);
        
        new File(outputDir).mkdirs();
        
        String outputFile = outputDir + "/" + this.module.name;
        CppGenerator generator = new CppGenerator(log, outputFile+".h", true);
        generator.run(module);
        generator.close();
        
        CppGenerator generator2 = new CppGenerator(log, outputFile+".cpp", false);
        generator2.run(module);
        generator2.close();
        
        DocGenerator docGenerator = new DocGenerator(log, outputFile+".html");
        docGenerator.run(module);
        docGenerator.close();
        
        genFmake();
    }

    private void genFmake() throws IOException {
        if (this.module.scriptMode) {
            return;
        }
        
        String fmakeFile = outputDir + "/" + this.module.name + ".fmake";
        
        StringBuilder depends = new StringBuilder();

        for (Depend dp : module.depends) {
            if (depends.length() > 0) {
                depends.append(", ");
            }
            depends.append(dp.toString());
        }
        
        HashMap<String, String> fmakes = new LinkedHashMap <String, String>();
        fmakes.put("name", this.module.name);
        fmakes.put("summary", this.module.name);
        fmakes.put("version", "1.0");
        fmakes.put("depends", depends.toString());
        fmakes.put("srcDirs", this.module.name+".cpp");
        fmakes.put("incDirs", this.module.name+".h");
        fmakes.put("outType", this.module.outType);
        fmakes.put("extIncDirs", "./");
        
        if (cppVersion != null) {
            fmakes.put("msvc.extConfigs.cppflags", "/std:"+cppVersion);
            fmakes.put("gcc.extConfigs.cppflags", "-std="+cppVersion);
            fmakes.put("emcc.extConfigs.cppflags", "-std="+cppVersion);
        }
        
        if (fmakeArgs != null) {
            String srcDir = new File(this.module.sourcePath).getParent();
            for (Map.Entry<String, String> entry : fmakeArgs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.endsWith("Dirs")) {
                    value = Util.convertRelativePaths(srcDir, outputDir, value);
                }
                if (fmakes.containsKey(key) && value.length() > 0) {
                    
                    if (key.equals("srcDirs") || key.equals("depends") || key.equals("incDirs")) {
                        value = value +", " + fmakes.get(key);
                    }
                }
                fmakes.put(key, value);
            }
        }
        
        StringBuilder fmakeScript = new StringBuilder();
        for (Map.Entry<String, String> entry : fmakes.entrySet()) {
            fmakeScript.append(entry.getKey());
            fmakeScript.append(" = ");
            fmakeScript.append(entry.getValue());
            fmakeScript.append("\n");
        }
        
        Files.writeString(Path.of(fmakeFile), fmakeScript, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
}
