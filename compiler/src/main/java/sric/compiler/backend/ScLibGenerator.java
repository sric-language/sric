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
import sric.compiler.ast.AstNode.Comment;
import sric.compiler.ast.AstNode.FieldDef;
import sric.compiler.ast.AstNode.TopLevelDef;
import sric.compiler.ast.AstNode.TypeDef;
import sric.compiler.ast.Buildin;
import sric.compiler.ast.Expr;
import sric.compiler.ast.Expr.ClosureExpr;
import sric.compiler.ast.FConst;
import sric.compiler.ast.SModule;
import sric.compiler.ast.Stmt;
import sric.compiler.ast.Token.TokenKind;
import sric.compiler.ast.Type;

/**
 *
 * @author yangjiandong
 */
public class ScLibGenerator extends BaseGenerator {
    public boolean isPrintAll = false;
    
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
    
    private void printCommont(TopLevelDef f) {
        if (f.comment != null) {
            for (Comment comment : f.comment.comments) {
                if (comment.type == TokenKind.cmdComment) {
                    print("//@");
                    print(comment.content);
                    newLine();
                }
            }
        }
    }

    private void printType(Type type) {
        if (type == null) {
            return;
        }
        
        if (type.isVarArgType()) {
            print(Buildin.varargTypeName);
            return;
        }
        
        if (type.isRefable) {
            print("refable ");
        }
        if (type.isImmutable) {
            print("const ");
        }
        else if (type.explicitImmutable) {
            print("mut ");
        }
        
        if (type.isArray()) {
            Type.ArrayInfo info = (Type.ArrayInfo)type.detail;
            print("[");
            if (info.sizeExpr != null) {
                visit(info.sizeExpr);
            }
            print("]");
            print(type.genericArgs.get(0).toString());
            return;
        }
        else if (type.isNum()) {
            Type.NumInfo info = (Type.NumInfo)type.detail;
            if (info.isUnsigned) {
                print("U");
            }

            printIdExpr(type.id);

            if (info.size != 0) {
                print(""+info.size);
            }
            return;
        }
        else if (type.isPointerType()) {
            if (type.isNullType()) {
                print("null");
            }
            else {
                Type.PointerInfo info = (Type.PointerInfo)type.detail;
                if (info.pointerAttr != Type.PointerAttr.inst) {
                    print(info.pointerAttr.toString());
                }
                print("*");
                if (info.isNullable) print("?");
                print(" ");
                printType(type.genericArgs.get(0));
            }
            return;
        }
        else if (type.isFuncType()) {
            print("fun");
            printFuncPrototype(((Type.FuncInfo)type.detail).prototype, false, true);
            return;
        }
        else if (type.isMetaType()) {
            printType(((Type.MetaTypeInfo)type.detail).type);
            return;
        }
        
        printIdExpr(type.id);
        
        if (type.genericArgs != null) {
            print("$<");
            int i = 0;
            for (Type t : type.genericArgs) {
                if (i > 0) {
                    print(", ");
                }
                printType(t);
                ++i;
            }
            print(">");
        }
    }

    private void printIdExpr(Expr.IdExpr id) {
        if (id.namespace != null) {
            printIdExpr(id.namespace);
            print("::");
        }
        if (id.name.equals(".")) {
            return;
        }
        print(id.name);
    }
    
    @Override
    public void visitTypeAlias(AstNode.TypeAlias v) {
        print("typealias ");
        print(v.name);
        print(" = ");
        printType(v.type);
        print(";");
        newLine();
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        
        for (AstNode.Import i : v.imports) {
            print("import ");
            printIdExpr(i.id);
            if (i.star) {
                print("::*");
            }
            print(";");
            newLine();
        }
        newLine();
        v.walkChildren(this);
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        printCommont(v);
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
        
        if (v.uninit) {
            print(" = uninit");
        }
        
        if (v.initExpr != null) {
            print(" = ");
            this.visit(v.initExpr);
        }
    }
    
    @Override
    public void visitFunc(AstNode.FuncDef v) {
        boolean inlined = isPrintAll || (v.flags & FConst.Inline) != 0 || v.generiParamDefs != null;
        if (!inlined && v.parent instanceof TypeDef sd) {
            if (sd.generiParamDefs != null) {
                inlined = true;
            }
        }
        
        if (!inlined) {
            if ((v.flags & FConst.Private) != 0) {
                return;
            }
        }
        
        printCommont(v);
        printFlags(v.flags);
        print("fun ");
        print(v.name);
        
        printGenericParamDefs(v.generiParamDefs);

        printFuncPrototype(v.prototype, false, v.isStatic());

        
        if (inlined && v.code != null) {
            this.visit(v.code);
        }
        else {
            print(";").newLine();
        }
    }
    
    private void printFuncPrototype(AstNode.FuncPrototype prototype, boolean isLambda, boolean isStatic) {
        print("(");
        if (prototype != null && prototype.paramDefs != null) {
            int i = 0;
            for (FieldDef p : prototype.paramDefs) {
                if (i > 0) {
                    print(", ");
                }
                print(p.name);
                print(" : ");
                printType(p.fieldType);
                if (p.initExpr != null) {
                    print(" = ");
                    this.visit(p.initExpr);
                }
                ++i;
            }
        }
        print(")");
        
        if (!isLambda && !isStatic && prototype.isThisImmutable()) {
            print(" const ");
        }
        
        if (prototype != null) {
            if (prototype.returnType != null && !prototype.returnType.isVoid()) {
                print(" : ");
                printType(prototype.returnType);
            }
        }
    }
    
    private void printGenericParamDefs(ArrayList<AstNode.GenericParamDef> generiParamDefs) {
        if (generiParamDefs != null) {
            print("$<");
            int i = 0;
            for (var gp : generiParamDefs) {
                if (i > 0) print(", ");
                print(gp.name);
                if (gp.bound != null && !gp.bound.isGenericParamType()) {
                    print(" : ");
                    printType(gp.bound);
                }
                ++i;
            }
            print(">");
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        if (v.isEnum()) {
            printCommont(v);
            printFlags(v.flags);
            print("enum ");
            print(v.name);
            print(" {").newLine();
            indent();

            int i = 0;
            for (AstNode.FieldDef f : v.fieldDefs) {
                if (i > 0) {
                    print(",").newLine();
                }
                print(f.name);
                if (f.initExpr != null) {
                    print(" = ");
                    this.visit(f.initExpr);
                }
                ++i;
            }
            newLine();

            unindent();
            print("}").newLine();
            return;
        }
        
        printCommont(v);
        printFlags(v.flags);
        print("struct ");
        print(v.name);
        
        //if (v instanceof AstNode.StructDef sd) {
            printGenericParamDefs(v.generiParamDefs);
            
            if (v.inheritances != null) {
                int i = 0;
                for (Type inh : v.inheritances) {
                    if (i == 0) print(" : ");
                    else print(", ");
                    printType(inh);
                    ++i;
                }
            }
        //}
        
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
                
                if (cb.fallthrough) {
                    print("fallthrough;").newLine();
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
            print("unsafe ");
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
        boolean parentheses = true;
        if (v.isStmt || v instanceof Expr.IdExpr || v instanceof Expr.LiteralExpr || v instanceof Expr.CallExpr || v instanceof Expr.GenericInstance 
                || v instanceof Expr.AccessExpr || v instanceof Expr.NonNullableExpr || v instanceof Expr.WithBlockExpr || v instanceof Expr.ArrayBlockExpr
                || v instanceof Expr.TypeExpr || v instanceof ClosureExpr) {
            parentheses = false;
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
            print(" ");
            print(e.opToken.symbol);
            print(" ");
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
            if (e.opToken.keyword) {
                print(" ");
            }
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
            print("$<");
            int i = 0;
            for (Type t : e.genericArgs) {
                if (i > 0) print(", ");
                this.printType(t);
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
        else if (v instanceof Expr.WithBlockExpr e) {
            printItBlockExpr(e);
        }
        else if (v instanceof Expr.ArrayBlockExpr e) {
            printArrayBlockExpr(e);
        }
        else if (v instanceof ClosureExpr e) {
            printClosureExpr(e);
        }
        else if (v instanceof Expr.NonNullableExpr e) {
            this.visit(e.operand);
            print("!");
        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
        
        if (parentheses) {
            print(")");
        }
    }
    
    void printLiteral(Expr.LiteralExpr e) {
        if (e.value == null) {
            print("null");
        }
        else if (e.value instanceof Long li) {
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
    
    void printItBlockExpr(Expr.WithBlockExpr e) {
        this.visit(e.target);
        this.visit(e.block);
    }
    
    void printArrayBlockExpr(Expr.ArrayBlockExpr e) {
        printType(e.type);
        print("{");
        if (e.args != null) {
            for (Expr t : e.args) {
                print(",");
                this.visit(t);
            }
        }
        print("}");
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
        
        this.printFuncPrototype(expr.prototype, true, false);
        
        this.visit(expr.code);
    }

}
