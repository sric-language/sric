//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.backend;

import java.io.IOException;
import java.io.PrintStream;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.Expr;
import sric.compiler.ast.Expr.ClosureExpr;
import sric.compiler.ast.FConst;
import sric.compiler.ast.SModule;
import sric.compiler.ast.Stmt;
import static sric.compiler.ast.Token.TokenKind.abstractKeyword;
import static sric.compiler.ast.Token.TokenKind.asyncKeyword;
import static sric.compiler.ast.Token.TokenKind.constKeyword;
import static sric.compiler.ast.Token.TokenKind.constexprKeyword;
import static sric.compiler.ast.Token.TokenKind.extensionKeyword;
import static sric.compiler.ast.Token.TokenKind.externKeyword;
import static sric.compiler.ast.Token.TokenKind.inlineKeyword;
import static sric.compiler.ast.Token.TokenKind.operatorKeyword;
import static sric.compiler.ast.Token.TokenKind.overrideKeyword;
import static sric.compiler.ast.Token.TokenKind.packedKeyword;
import static sric.compiler.ast.Token.TokenKind.privateKeyword;
import static sric.compiler.ast.Token.TokenKind.protectedKeyword;
import static sric.compiler.ast.Token.TokenKind.publicKeyword;
import static sric.compiler.ast.Token.TokenKind.readonlyKeyword;
import static sric.compiler.ast.Token.TokenKind.reflectKeyword;
import static sric.compiler.ast.Token.TokenKind.staticKeyword;
import static sric.compiler.ast.Token.TokenKind.throwKeyword;
import static sric.compiler.ast.Token.TokenKind.unsafeKeyword;
import static sric.compiler.ast.Token.TokenKind.virtualKeyword;
import sric.compiler.ast.Type;

/**
 *
 * @author yangjiandong
 */
public class ScLibGenerator extends BaseGenerator {
    public ScLibGenerator(CompilerLog log, String file) throws IOException {
        super(log, file);
    }
    
    public ScLibGenerator(CompilerLog log, PrintStream writer) {
        super(log, writer);
    }
    
    public void run(SModule module) {
        module.walkChildren(this);
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
        if ((flags & FConst.Async) != 0) {
            print("async ");
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
        
//        if (type.isImutable) {
//            print("const ");
//        }
//        
//        printIdExpr(type.id);
//        
//        if (type.genericArgs != null) {
//            print("$<");
//            int i = 0;
//            for (Type t : type.genericArgs) {
//                if (i > 0) {
//                    print(", ");
//                }
//                printType(t);
//                ++i;
//            }
//            print(">");
//        }
        print(type.toString());
    }

    private void printIdExpr(Expr.IdExpr id) {
        String ns = id.getNamespaceName();
        if (ns != null) {
            print(ns);
            print("::");
        }
        print(id.name);
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.walkChildren(this);
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        printFlags(v.flags);
        printLocalFieldDefAsExpr(v);
        print(";").newLine();
    }
    
    void printLocalFieldDefAsExpr(AstNode.FieldDef v) {
        print("var ");
        print(v.name);
        
        if (v.fieldType != null) {
            print(" : ");
            printType(v.fieldType);
        }
        
        if (v.initExpr != null) {
            print(" = ");
            this.visit(v.initExpr);
        }
    }
    
    @Override
    public void visitFunc(AstNode.FuncDef v) {
        boolean inlined = (v.flags & FConst.Inline) != 0 || v.generiParamDefs != null;
        
        if (!inlined) {
            if ((v.flags & FConst.Private) != 0) {
                return;
            }
        }
        
        printFlags(v.flags);
        print("fun ");
        print(v.name);
        
        if (v.generiParamDefs != null) {
            print("$<");
            int i = 0;
            for (var gp : v.generiParamDefs) {
                if (i > 0) print(", ");
                print(gp.name);
                ++i;
            }
            print(">");
        }

        printFuncPrototype(v.prototype);

        
        if (inlined && v.code != null) {
            this.visit(v.code);
        }
        else {
            print(";").newLine();
        }
    }
    
    private void printFuncPrototype(AstNode.FuncPrototype prototype) {
        print("(");
        if (prototype != null && prototype.paramDefs != null) {
            int i = 0;
            for (AstNode.ParamDef p : prototype.paramDefs) {
                if (i > 0) {
                    print(", ");
                }
                print(p.name);
                print(" : ");
                printType(p.paramType);
                if (p.defualtValue != null) {
                    print(" = ");
                    this.visit(p.defualtValue);
                }
                ++i;
            }
        }
        print(")");
        
        if (prototype != null) {
            if (prototype.returnType != null && !prototype.returnType.isVoid()) {
                print(" : ");
                printType(prototype.returnType);
            }
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        if (v instanceof AstNode.EnumDef edef) {
            printFlags(v.flags);
            print("enum class ");
            print(v.name);
            print(" {").newLine();
            indent();

            int i = 0;
            for (AstNode.FieldDef f : edef.enumDefs) {
                if (i > 0) {
                    print(",").newLine();
                }
                print(f.name);
                if (f.initExpr != null) {
                    print(" = ");
                    this.visit(f.initExpr);
                    print(";");
                }
                ++i;
            }
            newLine();

            unindent();
            print("};").newLine();
            return;
        }
        
        printFlags(v.flags);
        print("struct ");
        print(v.name);
        
        if (v instanceof AstNode.StructDef sd) {
            if (sd.generiParamDefs != null) {
                print("$<");
                int i = 0;
                for (var gp : sd.generiParamDefs) {
                    if (i > 0) print(", ");
                    print(gp.name);
                    ++i;
                }
                print(">");
            }
            
            if (sd.inheritances != null) {
                int i = 0;
                for (Type inh : sd.inheritances) {
                    if (i == 0) print(" : ");
                    else print(", ");
                    printType(inh);
                }
            }
        }
        
        print(" {").newLine();
        indent();
        
        v.walkChildren(this);
        
        unindent();
        print("}").newLine();
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof AstNode.Block bs) {
            print("{").newLine();
            indent();
            bs.walkChildren(this);
            unindent();
            print("}").newLine();
        }
        else if (v instanceof Stmt.IfStmt ifs) {
            print("if (");
            this.visit(ifs.condition);
            print(") ");
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                print("else ");
                this.visit(ifs.elseBlock);
            }
        }
        else if (v instanceof Stmt.LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof Stmt.WhileStmt whiles) {
            print("while (");
            this.visit(whiles.condition);
            print(") ");
            this.visit(whiles.block);
        }
        else if (v instanceof Stmt.ForStmt fors) {
            print("for (");
            if (fors.init != null) {
                if (fors.init instanceof Stmt.LocalDefStmt varDef) {
                    printLocalFieldDefAsExpr(varDef.fieldDef);
                }
                else if (fors.init instanceof Stmt.ExprStmt s) {
                    this.visit(s.expr);
                }
                else {
                    err("Unsupport for init stmt", fors.init.loc);
                }
            }
            print("; ");
            
            if (fors.condition != null) {
                this.visit(fors.condition);
            }
            print("; ");
            
            if (fors.update != null) {
                this.visit(fors.update);
            }
            print(") ");
            this.visit(fors.block);
        }
        else if (v instanceof Stmt.SwitchStmt switchs) {
            print("switch (");
            this.visit(switchs.condition);
            print(") {").newLine();
            
            this.indent();
            
            for (Stmt.CaseBlock cb : switchs.cases) {
                this.unindent();
                print("case ");
                this.visit(cb.caseExpr);
                print(":").newLine();
                this.indent();
                
                this.visit(cb.block);
                
                if (!cb.fallthrough) {
                    print("break;").newLine();
                }
            }
            
            if (switchs.defaultBlock != null) {
                this.unindent();
                print("default:").newLine();
                this.indent();
                this.visit(switchs.defaultBlock);
            }
 
            this.unindent();
            print("}").newLine();
        }
        else if (v instanceof Stmt.ExprStmt exprs) {
            this.visit(exprs.expr);
            print(";").newLine();
        }
        else if (v instanceof Stmt.JumpStmt jumps) {
            print(jumps.opToken.symbol).print(";").newLine();
        }
        else if (v instanceof Stmt.UnsafeBlock bs) {
            print("/*unsafe*/ ");
            this.visit(bs.block);
        }
        else if (v instanceof Stmt.ReturnStmt rets) {
            if (rets.expr != null) {
                print("return ");
                this.visit(rets.expr);
                print(";").newLine();
            }
            else {
                print("return;");
            }
        }
        else {
            err("Unkown stmt:"+v, v.loc);
        }
    }

    @Override
    public void visitExpr(Expr v) {
        boolean isPrimitive = false;
        if (v.isStmt || v instanceof Expr.IdExpr || v instanceof Expr.LiteralExpr || v instanceof Expr.CallExpr) {
            isPrimitive = true;
        }
        else {
            print("(");
        }
        
        if (v instanceof Expr.IdExpr e) {
            this.printIdExpr(e);
        }
        else if (v instanceof Expr.AccessExpr e) {
            this.visit(e.target);
            print(".");
            print(e.name);
        }
        else if (v instanceof Expr.LiteralExpr e) {
            printLiteral(e);
        }
        else if (v instanceof Expr.BinaryExpr e) {
            this.visit(e.lhs);
            print(e.opToken.symbol);
            this.visit(e.rhs);
        }
        else if (v instanceof Expr.CallExpr e) {
            this.visit(e.target);
            print("(");
            if (e.args != null) {
                int i = 0;
                for (Expr.CallArg t : e.args) {
                    if (i > 0) print(", ");
                    this.visit(t.argExpr);
                    ++i;
                }
            }
            print(")");
        }
        else if (v instanceof Expr.UnaryExpr e) {
            print(e.opToken.symbol);
            this.visit(e.operand);
        }
        else if (v instanceof Expr.TypeExpr e) {
            this.printType(e.type);
        }
        else if (v instanceof Expr.IndexExpr e) {
            this.visit(e.target);
            print("[");
            this.visit(e.index);
            print("]");
        }
        else if (v instanceof Expr.GenericInstance e) {
            this.visit(e.target);
            print("<");
            int i = 0;
            for (Type t : e.genericArgs) {
                if (i > 0) print(", ");
                this.visit(t);
                ++i;
            }
            print(" >");
        }
        else if (v instanceof Expr.IfExpr e) {
            this.visit(e.condition);
            print("?");
            this.visit(e.trueExpr);
            print(":");
            this.visit(e.falseExpr);
        }
        else if (v instanceof Expr.InitBlockExpr e) {
            printInitBlockExpr(e);
        }
        else if (v instanceof ClosureExpr e) {
            printClosureExpr(e);
        }
        else if (v instanceof Expr.OptionalExpr e) {
            this.visit(e.operand);
        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
        
        if (!isPrimitive) {
            print(")");
        }
    }
    
    void printLiteral(Expr.LiteralExpr e) {
        if (e.value instanceof Long li) {
            print(li.toString());
        }
        else if (e.value instanceof Double li) {
            print(li.toString());
        }
        else if (e.value instanceof Boolean li) {
            print(li.toString());
        }
        else if (e.value instanceof String li) {
            print("\"");
            for (int i=0; i<li.length(); ++i) {
                char c = li.charAt(i);
                printChar(c);
            }
            print("\"");
        }
    }

    void printChar(char c) {
        switch (c) {
            case '\b':
                print("\\b");
                break;
            case '\n':
                print("\\n");
                break;
            case '\r':
                print("\\r");
                break;
            case '\t':
                print("\\t");
                break;
            case '\"':
                print("\\\"");
                break;
            case '\'':
                print("\\\'");
                break;
            case '\\':
                writer.print('\\');
                break;
            default:
                writer.print(c);
        }
    }
    
    void printInitBlockExpr(Expr.InitBlockExpr e) {
        this.visit(e.target);
        if (e.args != null) {
            for (Expr.CallArg t : e.args) {
                print(",");
                this.visit(t.argExpr);
            }
        }
    }
    
    void printClosureExpr(ClosureExpr expr) {
        print("fun");
        
//        int i = 0;
//        if (expr.defaultCapture != null) {
//            print(expr.defaultCapture.symbol);
//            ++i;
//        }
//        
//        for (Expr t : expr.captures) {
//            if (i > 0) print(", ");
//            this.visit(t);
//            ++i;
//        }
//        print("]");
        
        this.printFuncPrototype(expr.prototype);
        
        this.visit(expr.code);
    }
}
