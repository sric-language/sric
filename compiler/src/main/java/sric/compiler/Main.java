//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import sric.compiler.ast.SModule;
import sric.lsp.LanguageServer;

/**
 *
 * @author yangjiandong
 */
public class Main {
    
    String homePath = "./";
    boolean recursion = false;
    boolean lsp = false;
    boolean verbose = false;
    boolean scriptMode = false;
    boolean compileNative = false;
    boolean debug = false;
    //boolean execute = false;
    String cppVersion = "c++20";
    
    static private void printHelp() {
        System.out.println("Sric compiler");
        System.out.println("Usage:");
        System.out.println("  sric [options] <filename>");
        System.out.println("Options:");
        System.out.println("  -help,-? \tprint help");
        System.out.println("  -lsp \t\tstart LanguageServer");
        System.out.println("  -r \t\trecursively build depends");
        System.out.println("  -version \tprint version");
        System.out.println("  -fmake \tbuild native by fmake");
        System.out.println("  -debug \tdebug build");
        System.out.println("  -c++20 \tc++ std version");
    }
    
    static private void printVersion() {
        System.out.println("Sric version 1.10");
        System.out.println("Copyright (c) 2022-2024, chunquedong");
        System.out.println("Licensed under the Academic Free License version 3.0");
    }
    
    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.run(args);
    }
    
    public void run(String[] args) throws IOException {
        String sourcePath = null;
        
        for (int i = 0; i<args.length; ++i) {
            if (args[i].equals("-home")) {
                ++i;
                homePath = args[i];
            }
            else if (args[i].equals("-lsp")) {
                lsp = true;
            }
            else if (args[i].equals("-r")) {
                recursion = true;
            }
            else if (args[i].equals("-?") || args[i].equals("-help")) {
                printHelp();
                return;
            }
            else if (args[i].equals("-version")) {
                printVersion();
                return;
            }
            else if (args[i].equals("-verbose")) {
                verbose = true;
            }
            else if (args[i].equals("-scriptMode")) {
                scriptMode = true;
            }
            else if (args[i].equals("-fmake")) {
                compileNative = true;
            }
            else if (args[i].equals("-debug")) {
                debug = true;
            }
            else if (args[i].startsWith("-c++")) {
                cppVersion = args[i].substring(1).trim();
            }
            else if (args[i].startsWith("-")) {
                System.err.print("unknow flags:"+ args[i]);
            }
            else {
                sourcePath = args[i];
            }
        }
        
        if (lsp) {
            LanguageServer ls = new LanguageServer(homePath+"/lib", verbose);
            ls.start();
            return;
        }
        
        if (sourcePath == null) {
            printHelp();
            System.exit(2);
            return;
        }
        
        if (!compile(sourcePath)) {
            System.err.println("Compile Fail");
            System.exit(1);
        }
    }
    
    public boolean compile(String sourcePath) throws IOException {
        String libPath = homePath + "/lib";
        Compiler compiler;
        if (scriptMode) {
            compiler = Compiler.makeDefault(sourcePath, libPath);
        }
        else {
            compiler = Compiler.fromProps(sourcePath, libPath);
        }
        
        compiler.cppVersion = this.cppVersion;
        
        if (recursion) {
            for (SModule.Depend dep: compiler.module.depends) {
                String libFile = libPath + "/" + dep.name;
                String propsPath = libFile+".meta";
                var props = Util.readProps(propsPath);
                String sourcePath2 = props.get("sourcePath");
                if (sourcePath2 != null) {
                    if(!new File(sourcePath2).exists()) {
                        System.out.println("file not found: "+sourcePath2);
                        continue;
                    }
                    if (!compile(sourcePath2)) {
                        System.err.println("Compile Fail: "+sourcePath2);
                        return false;
                    }
                }
            }
        }
        
        boolean rc = compiler.run();
        if (rc && compileNative) {
            StringBuilder sb = new StringBuilder();
            if (Util.isWindows()) {
                sb.append("fan.bat ");
            }
            else {
                sb.append("fan ");
            }
            sb.append("fmake ");
            if (debug) {
                sb.append("-debug ");
            }
            sb.append("-f ");
            if (Util.isWindows()) {
                sb.append("/");
            }
            sb.append(homePath);
            sb.append("/output/");
            sb.append(compiler.module.name);
            sb.append(".fmake");

            String cmd = sb.toString();
            if (verbose) {
                System.out.println("exec: "+cmd);
            }

            int error = Util.exec(cmd);
            if (error != 0) {
                rc = false;
            }
        }
        return rc;
    }

}
