/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sric.compiler.backend;

import sric.compiler.ast.AstNode;
import sric.compiler.ast.FConst;
import sric.compiler.ast.SModule;
import sric.compiler.ast.Token;
import sric.compiler.ast.Type;
import sric.compiler.resolve.ErrorChecker;

/**
 *
 * @author yangjiandong
 */
public class ReflectionGen {
    CppGenerator cppGenerator;
    
    ReflectionGen(CppGenerator cppGenerator) {
        this.cppGenerator = cppGenerator;
    }
    
    protected void indent()
    {
        cppGenerator.indent();
    }

    protected void unindent()
    {
        cppGenerator.unindent();
    }
    
    protected BaseGenerator print(String str) {
        return cppGenerator.print(str);
    }
    
    protected void newLine() {
        cppGenerator.newLine();
    }
    
    void printStringLiteral(String li) {
        cppGenerator.printStringLiteral(li);
    }
    
    private String getSymbolName(AstNode.TopLevelDef type) {
        return cppGenerator.getSymbolName(type);
    }
    
    private SModule getModule() {
        return cppGenerator.module;
    }
    
    private void printType(Type type) {
        cppGenerator.printType(type);
    }
    
    private void reflectionTopLevelDef(AstNode.TopLevelDef node, String varName) {
        print(varName).print(".flags = ").print(""+node.flags).print(";").newLine();
        print(varName).print(".name = \"").print(node.name).print("\";").newLine();
        
        if (node instanceof AstNode.TypeDef td) {
            print(varName).print(".kind = ").print(""+td.kind.ordinal()).print(";").newLine();
        }
        
        if (node.comment != null) {
            for (var c : node.comment.comments) {
                if (c.type != Token.TokenKind.cmdComment) {
                    continue;
                }
                
                print("{sric::RComment comment;");
                print("comment.type = ").print(c.type == Token.TokenKind.cmdComment ? "0" : "1").print(";");
                print("comment.content = "); printStringLiteral(c.content); print(";");
                
                print(varName).print(".comments.add(std::move(comment));}");
                this.newLine();
            }
        }
    }
    
    private void reflectParamDef(AstNode.FieldDef f, String parentName) {
        print("{");
        
        print("param.name = \"").print(f.name).print("\";");
        print("param.fieldType = ");printStringLiteral(f.fieldType.getQName(true));print(";");
        print("param.baseType = ");printStringLiteral(f.fieldType.getEasyName());print(";");

        print("param.hasDefaultValue = ").print(f.hasParamDefaultValue() ? "1" : "0").print(";");
        print(parentName).print(".params.add(std::move(param));");
        print("}");
        this.newLine();
    }
    
    private void reflectFieldDef(AstNode.FieldDef f, String parentName, boolean isEnumSlot) {
        print("{");
        this.indent();
        newLine();
        
        reflectionTopLevelDef(f, "v");
        
        String moduleName = this.getModule().name;
        if (f.isStatic()) {
            print("v.offset = 0;").newLine();
            print("v.pointer = (void*) &");print(moduleName);print("::");
            if (f.parent instanceof AstNode.TypeDef td) {
                print(this.getSymbolName(td));
                print("::");
            }
            print(this.getSymbolName(f)).print(";").newLine();
        }
        else if (isEnumSlot) {
            print("v.offset = 0;").newLine();
            print("v.pointer = nullptr;").newLine();
        }
        else {
            print("v.offset = offsetof("); print(moduleName);print("::").print(this.getSymbolName((AstNode.TopLevelDef)f.parent));
                print(",").print(this.getSymbolName(f)).print(");").newLine();
            print("v.pointer = nullptr;").newLine();
        }
        
        print("v.fieldType = ");printStringLiteral(f.fieldType.getQName(true));print(";").newLine();
        print("v.baseType = ");printStringLiteral(f.fieldType.getEasyName());print(";").newLine();

        print("v.hasDefaultValue = ").print(f.hasParamDefaultValue() ? "1" : "0").print(";").newLine();
        
        print("v.enumValue = ").print(""+f._enumValue).print(";").newLine();
        
        print(parentName).print(".fields.add(std::move(v));").newLine();
        
        this.unindent();
        print("}");
        newLine();
    }
    
    private void printMethodWrapFunc(AstNode.FuncDef f) {
        this.printType(f.prototype.returnType);
        print(" ").print(this.getModule().name).print("_").print(((AstNode.TopLevelDef)f.parent).name).
                print("_").print(f.name).print("(");
        
        print(this.getModule().name).print("::").print(this.getSymbolName((AstNode.TopLevelDef)f.parent)).print("* self");
        if (f.prototype.paramDefs != null) {
            for (AstNode.FieldDef p : f.prototype.paramDefs) {
                print(", ");
                printType(p.fieldType);
                print(" ").print(p.name);
            }
        }
        print(") {").newLine();
        this.indent();
        
        if (!f.prototype.returnType.isVoid()) {
            print("return ");
        }
        
        print("self->").print(this.getSymbolName(f)).print("(");
        if (f.prototype.paramDefs != null) {
            int i = 0;
            for (AstNode.FieldDef p : f.prototype.paramDefs) {
                if (i>0) print(", ");
                if (p.fieldType.isReference || ErrorChecker.isCopyable(p.fieldType)) {
                    print(p.name);
                }
                else {
                    print("std::move(").print(p.name).print(")");
                }
                ++i;
            }
        }
        print(");").newLine();
        
        this.unindent();
        print("}").newLine();
    }
    
    private void reflectFuncDef(AstNode.FuncDef f, String parentName) {
        print("{");
        this.indent();
        newLine();
        
        reflectionTopLevelDef(f, "f");
        
        String moduleName = this.getModule().name;
        if (f.isStatic()) {
            if (f.parent instanceof AstNode.TopLevelDef) {
                print("f.pointer = (void*) &");print(moduleName);print("::").print(this.getSymbolName((AstNode.TopLevelDef)f.parent)).
                    print("::").print(this.getSymbolName(f)).print(";").newLine();
            }
            else {
                print("f.pointer = (void*) &");print(moduleName);print("::").print(this.getSymbolName(f)).print(";").newLine();
            }
        }
        else {
            print("f.pointer = (void*) &");print(moduleName).print("_").print(((AstNode.TopLevelDef)f.parent).name).
                print("_").print(f.name).print(";").newLine();
        }

        print("f.returnType = ");printStringLiteral(f.prototype.returnType.getQName(true));print(";").newLine();
        print("f.returnBaseType = ");printStringLiteral(f.prototype.returnType.getEasyName());print(";").newLine();

        if (f.prototype.paramDefs != null) {
            for (AstNode.FieldDef p : f.prototype.paramDefs) {
                reflectParamDef(p, "f");
            }
        }
        
        if (f.generiParamDefs != null) {
            for (AstNode.GenericParamDef p : f.generiParamDefs) {
                print("f.genericParams.add(std::move(");printStringLiteral(p.name); print("));");
                this.newLine();
            }
        }
        
        print(parentName).print(".funcs.add(std::move(f));");

        this.unindent();
        newLine();
        print("}");
        newLine();
    }
    
    private void printEnumSwitch(AstNode.TypeDef td) {
        if (!td.isEnum()) {
            return;
        }
        String typeName = this.getSymbolName(td);
        print("const char* ");
        print(this.getModule().name).print("::").print(td.name). print("_toString(int i) {").newLine();
        this.indent();
        print(this.getModule().name).print("::").print(typeName).print(" e = (");
        print(this.getModule().name).print("::").print(typeName).print(")i;").newLine();
        
        print("switch (e) {").newLine();
        
        for (AstNode.FieldDef f : td.fieldDefs) {
            print("case ").print(this.getModule().name).print("::").print(typeName).print("::").print(this.getSymbolName(f)).print(":").newLine();
            print("    return \"").print(f.name).print("\";").newLine();
        }
        
        print("default: ").newLine();
        print("    return \"\";").newLine();
        
        print("}").newLine();
        this.unindent();
        print("}");
        newLine();
    }
    
    private boolean hasReflectable(SModule module) {
        for (AstNode.FileUnit funit : module.fileUnits) {
            for (AstNode.TypeDef type : funit.typeDefs) {
                if ((type.flags & FConst.Reflect) != 0 ) {
                    return true;
                }
                
                for (AstNode.FuncDef f : type.funcDefs) {
                    if ((f.flags & FConst.Reflect) != 0 ) {
                        return true;
                    }
                }
                
                for (AstNode.FieldDef f : type.fieldDefs) {
                    if ((f.flags & FConst.Reflect) != 0 ) {
                        return true;
                    }
                }
            }
            
            for (AstNode.FuncDef f : funit.funcDefs) {
                if ((f.flags & FConst.Reflect) != 0 ) {
                    return true;
                }
            }

            for (AstNode.FieldDef f : funit.fieldDefs) {
                if ((f.flags & FConst.Reflect) != 0 ) {
                    return true;
                }
            }
        }
        return false;
    }
    
    void printReflection(SModule module) {
        if (!hasReflectable(module)) {
            return;
        }
        
        newLine();
        print("//////////////////////////////////////////// reflect");
        newLine();
        
//        boolean hasSricLib = false;
//        for (SModule.Depend d : module.depends) {
//            if (d.name.equals("sric")) {
//                hasSricLib = true;
//                break;
//            }
//        }
        
        //print method wrap
        for (AstNode.FileUnit funit : module.fileUnits) {
            for (AstNode.TypeDef type : funit.typeDefs) {
                if ((type.flags & FConst.Reflect) == 0 ) {
                    continue;
                }
                printEnumSwitch(type);
                
                for (AstNode.FuncDef f : type.funcDefs) {
                    if (f.isStatic()) continue;
                    if ((f.flags & FConst.Ctor) != 0) continue;
                    printMethodWrapFunc(f);
                }
            }
        }
        
        print("void registReflection_").print(module.name).print("() {").newLine();
        this.indent();
        
        print("sric::RModule m;").newLine();
        print("m.name = \"").print(module.name).print("\";").newLine();
        print("m.version = \"").print(module.version).print("\";").newLine();
        
        print("sric::RType s;").newLine();
        print("sric::RFunc f;").newLine();
        print("sric::RField v;").newLine();
        print("sric::RField param;").newLine();
        
        for (AstNode.FileUnit funit : module.fileUnits) {
            for (AstNode.TypeDef type : funit.typeDefs) {
                
                if ((type.flags & FConst.Reflect) == 0 ) {
                    continue;
                }
                
                print("{");
                this.indent();
                newLine();
                
                reflectionTopLevelDef(type, "s");
                

                if (type.generiParamDefs != null) {
                    for (AstNode.GenericParamDef p : type.generiParamDefs) {
                        print("s.genericParams.add(");printStringLiteral(p.name); print(");");
                        this.newLine();
                    }
                }

                boolean hasBaseType = false;
                if (type.inheritances != null) {
                    for (Type p : type.inheritances) {
                        if (p.id.resolvedDef instanceof AstNode.TypeDef tf && tf.isStruct()) {
                            print("s.superType = ");printStringLiteral(p.getQName(true)); print(";");
                            hasBaseType = true;
                        }
                        else {
                            print("s.traits.add(");printStringLiteral(p.getQName(true)); print(");");
                        }
                        this.newLine();
                    }
                }
                if (!hasBaseType) {
                    print("s.superType = nullptr;");
                    this.newLine();
                }

                for (AstNode.FieldDef f : type.fieldDefs) {
                    reflectFieldDef(f, "s", type.isEnum());
                }

                for (AstNode.FuncDef f : type.funcDefs) {
                    if ((f.flags & FConst.Ctor) != 0) continue;
                    reflectFuncDef(f, "s");
                }
                
                if (type.generiParamDefs == null && type.isStruct() && (!type.isAbstract())) {
                    print("s.ctor = (void*) &");print("sric::newVoid<").print(this.getModule().name).print("::").
                            print(this.getSymbolName(type)).print(">").print(";").newLine();
                }
                else {
                    print("s.ctor = nullptr;").newLine();
                }
                
                if (type.isEnum()) {
                    print("s.enumToStr = (void*) &").print(this.getModule().name).print("::").print(type.name). print("_toString;").newLine();
                }
                else {
                    print("s.enumToStr = nullptr;").newLine();
                }
                
                if (type.isEnum() && type.enumBase != null) {
                    print("s.enumBase = ");this.printStringLiteral(type.enumBase.getEasyName()); print(";").newLine();
                }
                else {
                    print("s.enumBase = nullptr;").newLine();
                }
                
                print("m.types.add(std::move(s));").newLine();
                
                this.unindent();
                print("}");
                newLine();
            }
            
            for (AstNode.FieldDef f : funit.fieldDefs) {
                if ((f.flags & FConst.Reflect) == 0 ) {
                    continue;
                }
                reflectFieldDef(f, "m", false);
            }
            
            for (AstNode.FuncDef f : funit.funcDefs) {
                if ((f.flags & FConst.Reflect) == 0 ) {
                    continue;
                }
                reflectFuncDef(f, "m");
            }
            
        }
        
        
        newLine();
        print("sric::registModule(&m);").newLine();
        
        this.unindent();
        print("}");
        newLine();
        
        print("SC_AUTO_REGIST_MODULE("+ module.name +");").newLine();
    }
}
