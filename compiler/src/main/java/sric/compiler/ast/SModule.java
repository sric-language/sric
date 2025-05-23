//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import sric.compiler.CompilerLog;
import sric.compiler.Util;

/**
 *
 * @author yangjiandong
 */
public class SModule extends AstNode {
    
    public static class Depend {
        public String name;
        public String version;
        //public SModule cache;

        public String toString() {
            return name + " " + version;
        }
    }
    
    public String name;
    public String version;
    public HashMap<String, String> metaProps = null;
    public String outType;
    
    public ArrayList<FileUnit> fileUnits = new ArrayList<>();
    private Scope scope = null;
    public ArrayList<Depend> depends = new ArrayList<>();
    
    public String sourcePath;
    public boolean scriptMode = false;
    public boolean isStubFile = false;
    
    public FileUnit findFileUnit(String file) {
        for (FileUnit v : fileUnits) {
            if (v.file.equals(file)) {
                return v;
            }
        }
        return null;
    }
    
    public static SModule fromProps(HashMap<String, String> props) {
        SModule m = new SModule();
        m.name = props.get("name");
        if (m.name == null) {
            throw new RuntimeException("Unknow name");
        }
        if (!Util.isValidIdentifier(m.name)) {
            throw new RuntimeException("Invalid module name: "+m.name);
        }
        m.version = props.get("version");
        if (m.version == null) {
            throw new RuntimeException("Unknow version");
        }
        String dependsStr = props.get("depends");
        if (dependsStr == null) {
            throw new RuntimeException("Unknow depends");
        }
        m.outType = props.get("outType");
        if (m.outType == null) {
            throw new RuntimeException("Unknow outType");
        }
        
        if (!dependsStr.isEmpty()) {
            var dependsA = dependsStr.split(",");
            for (String depStr : dependsA) {
                depStr = depStr.trim();
                var fs = depStr.split(" ");
                if (fs.length != 2) {
                    throw new RuntimeException("parse depends error: "+depStr);
                }
                Depend depend = new Depend();
                depend.name = fs[0];
                depend.version = fs[1];
                m.depends.add(depend);
            }
        }
        
        if (props.get("sourcePath") != null) {
            m.isStubFile = true;
        }
        
        m.metaProps = props;
        return m;
    }
    
    public HashMap<String, String> toMetaProps() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", name);
        map.put("version", version);
        map.put("sourcePath", sourcePath);
        map.put("outType", outType);
        map.put("buildTime", ""+System.currentTimeMillis());
        
        StringBuilder sb = new StringBuilder();
        for (var d : depends) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(d.toString());
        }
        map.put("depends", sb.toString());
        
        if (metaProps != null) {
            map.put("summary", this.metaProps.get("summary"));
            map.put("license", this.metaProps.get("license"));
        }
        return map;
    }
    
    public void clearCache() {
        scope = null;
    }

    public Scope getScope(CompilerLog log) {
        if (scope == null) {
            scope = new Scope();
            for (FileUnit v : fileUnits) {
                for (FieldDef f : v.fieldDefs) {
                    if (!scope.put(f.name, f)) {
                        if (log != null) log.err("Duplicate name: " + f.name, f.loc);
                    }
                }
                for (FuncDef f : v.funcDefs) {
                    if (!scope.put(f.name, f)) {
                        if (log != null) log.err("Duplicate name: " + f.name, f.loc);
                    }
                }
                for (TypeDef f : v.typeDefs) {
                    if (!scope.put(f.name, f)) {
                        if (log != null) log.err("Duplicate name: " + f.name, f.loc);
                    }
                }
                for (TypeAlias f : v.typeAlias) {
                    if (!scope.put(f.name, f)) {
                        if (log != null) log.err("Duplicate name: " + f.name, f.loc);
                    }
                }
            }
        }
        return scope;
    }
    
    @java.lang.Override
    public void walkChildren(Visitor visitor) {
        for (FileUnit v : fileUnits) {
            visitor.visit(v);
        }
    }
}
