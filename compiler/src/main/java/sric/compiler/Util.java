//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yangjiandong
 */
public class Util {
        
    public static String getBaseName(String name) {
        int pos = name.lastIndexOf(".");
        if (pos == -1) {
            return name;
        }
        String basename = name.substring(0, pos);
        return basename;
    }
    
    public static ArrayList<File> listFile(File file) {
        ArrayList<File> list = new ArrayList<File>();
        doListFile(file, list, ".sric");
        return list;
    }
    
    private static void doListFile(File file, ArrayList<File> sources, String extName) {
        if (!file.isDirectory()) {
            if (file.getName().endsWith(extName)) {
                sources.add(file);
            }
        }
        else {
            File[] list = file.listFiles();
            for (File file2 : list) {
                doListFile(file2, sources, extName);
            }
        }
    }
    
    public static HashMap<String, String> readProps(String file) throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        List<String> lines = Files.readAllLines(Path.of(file));
        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith("//")) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }

            var fs = line.split("=", 2);
            if (fs.length == 1 && line.endsWith("=")) {
                map.put(fs[0].trim(), "");
                continue;
            }
            if (fs.length != 2) {
                System.out.println("props parse error:"+line);
                continue;
            }
            map.put(fs[0].trim(), fs[1].trim());
        }
        return map;
    }
    
    public static void writeProps(String file, HashMap<String, String> props) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        for (var e : props.entrySet()) {
            sb.append(e.getKey());
            sb.append(" = ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        
        String data = sb.toString();
        Files.writeString(Path.of(file), data, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    
    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.startsWith("Windows");
    }
    
    
    public static int exec(String cmd) throws IOException {
        Process pr = Runtime.getRuntime().exec(cmd);
        
        InputStreamReader inst2 = new InputStreamReader(pr.getInputStream());
        BufferedReader br2 = new BufferedReader(inst2);
        String res2 = null;
        while ((res2 = br2.readLine()) != null) {
            System.err.println(res2);
        }
        br2.close();
        
        InputStreamReader inst = new InputStreamReader(pr.getErrorStream());
        BufferedReader br = new BufferedReader(inst);
        String res = null;
        while ((res = br.readLine()) != null) {
            System.err.println(res);
        }
        br.close();

        int rc = 1;
        try {
            rc = pr.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        pr.destroy();
        return rc;
    }
    
    public static String convertRelativePaths(String oldDir, String newDir, String fileNames) {
        String[] fs = fileNames.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<fs.length; ++i) {
            File file = new File(oldDir + "/" + fs[i].trim());
            String srcFile;
            try {
                srcFile = file.getCanonicalPath();
            } catch (IOException ex) {
                srcFile = file.getPath();
            }
            if (File.separatorChar != '/') {
                srcFile = "/"+srcFile.replace('\\', '/');
            }
            if (file.isDirectory()) {
                srcFile += "/";
            }
            if (i > 0) sb.append(',');
            sb.append(srcFile);
        }
        return sb.toString();
    }
    
    public static boolean isValidIdentifier(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        String regex = "^[a-zA-Z_][a-zA-Z\\d_]*$";

        boolean isValid = str.matches(regex);

        if (isValid && isJavaKeyword(str)) {
            isValid = false;
        }

        return isValid;
    }

    private static boolean isJavaKeyword(String str) {
        String[] keywords = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", 
            "char", "class", "const", "continue", "default", "do", "double", 
            "else", "enum", "extends", "final", "finally", "float", "for", 
            "goto", "if", "implements", "import", "instanceof", "int", 
            "interface", "long", "native", "new", "package", "private", 
            "protected", "public", "return", "short", "static", "strictfp", 
            "super", "switch", "synchronized", "this", "throw", "throws", 
            "transient", "try", "void", "volatile", "while", "true", "false", "null"
        };

        for (String keyword : keywords) {
            if (keyword.equals(str)) {
                return true;
            }
        }
        return false;
    }
}
