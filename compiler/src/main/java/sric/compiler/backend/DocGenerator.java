//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.backend;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.FieldDef;
import sric.compiler.ast.AstNode.FuncDef;
import sric.compiler.ast.AstNode.TopLevelDef;
import sric.compiler.ast.Expr;
import sric.compiler.ast.FConst;
import sric.compiler.ast.SModule;
import sric.compiler.ast.Stmt;
import sric.compiler.ast.Token;
import sric.compiler.ast.Type;

/**
 *
 * @author Admin
 */
public class DocGenerator extends BaseGenerator {
    public DocGenerator(CompilerLog log, String file) throws IOException {
        super(log, file);
    }
    
    public DocGenerator(CompilerLog log, PrintStream writer) {
        super(log, writer);
    }
    
    public void run(SModule module) {
        print("<html>").newLine();
        print("<head>").newLine();
        print("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">");
        print("<head>").newLine();
        print("<body>").newLine();
        print("<div class=\"module\">");
            print("<p>");
                print("<span class=\"modulename\">");
                    print(module.name);
                print("</span>");
                print("<span class=\"moduleversion\">");
                    print(module.version);
                print("</span>");
            print("</p>");
        print("</div>");
        print("<div class=\"index\">");
            for (AstNode.FileUnit v : module.fileUnits) {
                for (AstNode.FieldDef f : v.fieldDefs) {
                    if (!f.isPublic() || f.isNoDoc()) {
                        continue;
                    }
                    print("<a href=\"#"+f.name+"\">");
                        print(f.name);
                    print("</a>");
                }
                for (AstNode.FuncDef f : v.funcDefs) {
                    if (!f.isPublic() || f.isNoDoc()) {
                        continue;
                    }
                    print("<a href=\"#"+f.name+"\">");
                        print(f.name);
                    print("</a>");
                }
                for (AstNode.TypeDef f : v.typeDefs) {
                    if (!f.isPublic() || f.isNoDoc()) {
                        continue;
                    }
                    print("<a href=\"#"+f.name+"\">");
                        print(f.name);
                    print("</a>");
                }
                for (AstNode.TypeAlias f : v.typeAlias) {
                    if (!f.isPublic() || f.isNoDoc()) {
                        continue;
                    }
                    print("<a href=\"#"+f.name+"\">");
                        print(f.name);
                    print("</a>");
                }
            }
            
        print("</div>");
        newLine();
        print("<div class=\"content\">");
            module.walkChildren(this);
        print("</div>");
        print("</body>").newLine();
        print("</html>");
    }
    
    private void printFlags(int flags) {
        if ((flags & FConst.Abstract) != 0) {
            print("abstract ");
        }
        if ((flags & FConst.Const) != 0) {
            print("const ");
        }
        if ((flags & FConst.Readonly) != 0) {
            print("readonly ");
        }
        if ((flags & FConst.Extern) != 0) {
            print("extern ");
        }
        if ((flags & FConst.ExternC) != 0) {
            print("externc ");
        }
        if ((flags & FConst.Extension) != 0) {
            print("extension ");
        }
        if ((flags & FConst.Override) != 0) {
            print("override ");
        }
        if ((flags & FConst.Private) != 0) {
            print("private ");
        }
        if ((flags & FConst.Protected) != 0) {
            print("protected ");
        }
        if ((flags & FConst.Public) != 0) {
            print("public ");
        }
        if ((flags & FConst.Static) != 0) {
            print("static ");
        }
        if ((flags & FConst.Virtual) != 0) {
            print("virtual ");
        }
        if ((flags & FConst.Async) != 0) {
            print("async ");
        }
        if ((flags & FConst.Reflect) != 0) {
            print("reflect ");
        }
        if ((flags & FConst.Unsafe) != 0) {
            print("unsafe ");
        }
        if ((flags & FConst.Throws) != 0) {
            print("throws ");
        }
        if ((flags & FConst.Inline) != 0) {
            print("inline ");
        }
        if ((flags & FConst.Inline) != 0) {
            print("inline ");
        }
        if ((flags & FConst.Packed) != 0) {
            print("packed ");
        }
        if ((flags & FConst.ConstExpr) != 0) {
            print("constexpr ");
        }
        if ((flags & FConst.Operator) != 0) {
            print("operator ");
        }
    }

    private void printType(Type type) {
        if (type == null) {
            return;
        }
//        print(type.toString().replace(" ", "&nbsp;"));
        print(type.getQName(false));
    }
    
    @Override
    public void visitTypeAlias(AstNode.TypeAlias v) {
        if (!v.isPublic() || v.isNoDoc()) {
            return;
        }
        print("<div class=\"typealias\" id=\""+v.name+"\">");
            print("<p class=\"typeinfo\">");
                print("<span class=\"kind\">");
                    print("typealias");
                print("</span>");
                
                print("<span class=\"name\">");
                    print(v.name);
                print("</span>");
                print("<span class=\"flags\">");
                    printFlags(v.flags);
                print("</span>");

                printType(v.type);
            print("</p>");
            newLine();
            if (v.comment != null && v.comment.getDoc() != null) {
                print("<div class=\"doc\">");
                print(v.comment.getDoc());
                print("</div>");
            }
        print("</div>");
        newLine();
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.walkChildren(this);
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        if (!v.isPublic() || v.isNoDoc()) {
            return;
        }
        print("<div class=\"field\" id=\""+v.name+"\">");
            print("<p class=\"fieldinfo\">");
                print("<span class=\"kind\">");
                    print("var");
                print("</span>");
                print("<span class=\"name\">");
                    print(v.name);
                print("</span>");
                print("<span class=\"flags\">");
                    printFlags(v.flags);
                print("</span>");
                print("<span class=\"type\">");
                    print(" : ");
                    printType(v.fieldType);
                print("</span>");
            print("</p>");
            

            if (v.comment != null && v.comment.getDoc() != null) {
                print("<div class=\"doc\">");
                print(v.comment.getDoc());
                print("</div>");
            }
            
        print("</div>");
        newLine();
    }

    @Override
    public void visitFunc(AstNode.FuncDef v) {
        if (!v.isPublic() || v.isNoDoc()) {
            return;
        }
        print("<div class=\"func\" id=\""+v.name+"\">");
            print("<p class=\"funcinfo\">");
            print("<span class=\"kind\">");
                print("fun");
            print("</span>");
            print("<span class=\"name\">");
                print(v.name);
            print("</span>");
            print("<span class=\"flags\">");
                printFlags(v.flags);
            print("</span>");

            printGenericParamDefs(v.generiParamDefs);

            printFuncPrototype(v.prototype, false, v.isStatic());
            
            print("</p>");
            
            if (v.comment != null && v.comment.getDoc() != null) {
                print("<div class=\"doc\">");
                print(v.comment.getDoc());
                print("</div>");
            }
        print("</div>");
        newLine();
    }
    
    private void printFuncPrototype(AstNode.FuncPrototype prototype, boolean isLambda, boolean isStatic) {

        
        print("<span class=\"prams\">(");
        if (prototype != null && prototype.paramDefs != null) {
            int i = 0;
            for (AstNode.FieldDef p : prototype.paramDefs) {
                if (i > 0) {
                    print(", ");
                }
                print("<span class=\"param\">");
                    print("<span class=\"name\">");
                        print(p.name);
                        print(" : ");
                    print("</span>");
                    print("<span class=\"type\">");
                        printType(p.fieldType);
                    print("</span>");
                print("</span>");
                ++i;
            }
        }
        print(")</span>");
        
        print("<span class=\"returnType\">");
            print(" : ");
            printType(prototype.returnType);
        print("</span>");
    }
    
    private void printGenericParamDefs(ArrayList<AstNode.GenericParamDef> generiParamDefs) {
        if (generiParamDefs == null) {
            return;
        }
        print("<span class=\"gnericParams\">$&lt;");
        if (generiParamDefs != null) {
            int i = 0;
            for (var gp : generiParamDefs) {
                if (i > 0) {
                    print(", ");
                }
                print("<span>");
                    print("<span class=\"name\">");
                        print(gp.name);
                        print(" : ");
                    print("</span>");
                    if (!gp.bound.isGenericParamType()) {
                        print("<span class=\"type\">");
                            printType(gp.bound);
                        print("</span>");
                    }
                print("</span>");
                ++i;
            }
        }
        print("&gt;</span>");
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        if (!v.isPublic() || v.isNoDoc()) {
            return;
        }
        print("<div class=\"typedef\" id=\""+v.name+"\">");
            print("<p class=\"typeinfo\">");
                print("<span class=\"kind\">");
                    print(v.kind.toString().toLowerCase());
                print("</span>");
                print("<span class=\"name\">");
                    print(v.name);
                print("</span>");
                print("<span class=\"flags\">");
                    printFlags(v.flags);
                print("</span>");
                printGenericParamDefs(v.generiParamDefs);

            print("</p>");
            newLine();
            
            if (v.comment != null && v.comment.getDoc() != null) {
                print("<div class=\"doc\">");
                print(v.comment.getDoc());
                print("</div>");
            }
            newLine();
            indent();

            print("<div class=\"typefield\">");
            for (FieldDef f : v.fieldDefs) {
                this.visitField(f);
            }
            print("</div>");
            newLine();
            
            print("<div class=\"typefunc\">");
            for (FuncDef f : v.funcDefs) {
                this.visitFunc(f);
            }
            print("</div>");
            newLine();
            
            unindent();
            newLine();
        print("</div>");
    }
}
