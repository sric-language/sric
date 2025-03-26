//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler;

import java.io.File;
import java.io.IOException;
import sric.compiler.ast.SModule;
import sric.lsp.LanguageServer;

/**
 *
 * @author yangjiandong
 */
public class Main {
    
    static private void printHelp() {
        System.out.println("Sric compiler");
        System.out.println("Usage:");
        System.out.println("  sric [options] <filename>");
        System.out.println("Options:");
        System.out.println("  -help,-? \tprint help");
        System.out.println("  -lib \t\tlibrary path");
        System.out.println("  -lsp \t\tstart LanguageServer");
        System.out.println("  -r \t\trecursively build depends");
        System.out.println("  -version \tprint version");
    }
    
    static private void printVersion() {
        System.out.println("Sric version 1.9");
        System.out.println("Copyright (c) 2022-2024, chunquedong");
        System.out.println("Licensed under the Academic Free License version 3.0");
    }
    
    public static void main(String[] args) throws IOException {
        String sourcePath = null;
        String libPath = "res/lib";
        boolean recursion = false;
        boolean lsp = false;
        boolean verbose = false;
        boolean scriptMode = false;
        for (int i = 0; i<args.length; ++i) {
            if (args[i].equals("-lib")) {
                ++i;
                libPath = args[i];
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
            else {
                sourcePath = args[i];
            }
        }
        
        if (lsp) {
            LanguageServer ls = new LanguageServer(libPath, verbose);
            ls.start();
            return;
        }
        
        if (sourcePath == null) {
            printHelp();
            return;
        }
        
        if ( !compile(sourcePath, libPath, recursion, scriptMode)) {
            System.err.println("Compile Fail");
        }
    }
    
    public static boolean compile(String sourcePath, String libPath, boolean recursion, boolean scriptMode) throws IOException {
        Compiler compiler;
        if (scriptMode) {
            compiler = Compiler.makeDefault(sourcePath, libPath);
        }
        else {
            compiler = Compiler.fromProps(sourcePath, libPath);
        }
        
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
                    if (!compile(sourcePath2, libPath, recursion, false)) {
                        System.err.println("Compile Fail: "+sourcePath2);
                        return false;
                    }
                }
            }
        }
        
        return compiler.run();
    }
}
