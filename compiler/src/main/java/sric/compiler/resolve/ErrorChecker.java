//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.resolve;


import java.util.HashMap;
import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Expr.*;
import sric.compiler.ast.Stmt.*;
import sric.compiler.ast.Token.*;
import static sric.compiler.ast.Token.TokenKind.*;
import sric.compiler.ast.Type.*;
import sric.compiler.ast.*;
/**
 *
 * @author yangjiandong
 */
public class ErrorChecker extends CompilePass {
    
    private SModule module;
    private TypeDef curStruct = null;
    private int inUnsafe = 0;
    private FileUnit curUnit = null;
    private WithBlockExpr curItBlock = null;
    private boolean hasReturn = false;
    
    public ErrorChecker(CompilerLog log, SModule module) {
        super(log);
        this.module = module;
        this.log = log;
    }
    
    public void run() {
        module.walkChildren(this);
    }
    
    boolean isInUnsafe() {
        return inUnsafe > 0;
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        curUnit = v;
        v.walkChildren(this);
        curUnit = null;
    }
    
    private boolean isCopyable(Type type) {
        if (type.id.resolvedDef == null) {
            return false;
        }
        
        if (type.detail instanceof Type.PointerInfo p2) {
            if (p2.pointerAttr == Type.PointerAttr.own) {
                return false;
            }
        }
        
        AstNode resolvedDef = type.id.resolvedDef;
        if (type.id.resolvedDef instanceof GenericParamDef td) {
            resolvedDef = td.bound.id.resolvedDef;
        }
        
        if (resolvedDef instanceof TopLevelDef td) {
            if ((td.flags & FConst.Noncopyable) != 0) {
                return false;
            }
            if (resolvedDef instanceof TypeDef sd && sd.isStruct()) {
                for (FieldDef f : sd.fieldDefs) {
                    if (!isCopyable(f.fieldType)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return true;
    }
    
    private boolean isMoveable(Type type) {
        if (type.id.resolvedDef == null) {
            return false;
        }
        
        if (type.isImmutable) {
            return false;
        }
        
        if (type.isPointerType() && !type.isNullablePointerType()) {
            return false;
        }
        
        AstNode resolvedDef = type.id.resolvedDef;
        if (type.id.resolvedDef instanceof GenericParamDef td) {
            resolvedDef = td.bound.id.resolvedDef;
        }
        
        if (resolvedDef instanceof TopLevelDef td) {
            if (resolvedDef instanceof TypeDef sd && sd.isStruct()) {
                for (FieldDef f : sd.fieldDefs) {
                    if (!isMoveable(f.fieldType)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return true;
    }
    
    private void verifyTypeFit(Expr target, Type to, Loc loc) {
        verifyTypeFit(target, to, loc, false);
    }
    private void verifyTypeFit(Expr target, Type to, Loc loc, boolean isCallArg) {
        Type from = target.resolvedType;
        if (from == null) {
            return;
        }
        
        //implicit convert string
        //sric::String
        if (to.id.resolvedDef instanceof TypeDef td) {
            if (td.name.equals("String") && td.parent instanceof FileUnit funit) {
                if (funit.module.name.equals("sric")) {
                    //const char *;
                    if (from.detail instanceof Type.PointerInfo pinfo && from.genericArgs != null) {
                        if (from.isRawOrInstPointerType() && from.genericArgs.get(0).detail instanceof Type.NumInfo ninfo) {
                            if (ninfo.size == 8) {
                                target.implicitTypeConvertTo = to;
                                target.implicitStringConvert = true;
                                return;
                            }
                        }
                    }
                }
            }
        }
        
        if (from.isPointerType() && !to.isPointerType()) {
            if (!from.isRawPointerType() && from.genericArgs != null) {
                target.implicitDereference = true;
                from = from.genericArgs.get(0);
            }
        }
        else if (!from.isPointerType() && to.detail instanceof Type.PointerInfo pinfo) {
            if (from.isRefable && pinfo.pointerAttr == Type.PointerAttr.ref) {
                target.implicitGetAddress = true;
                from = Type.pointerType(loc, from, Type.PointerAttr.ref, false);
            }
            else if (target instanceof AccessExpr aexpr && 
                    (aexpr.target.resolvedType.isOwnOrRefPointerType() || aexpr.target.resolvedType.isRefable) && 
                    pinfo.pointerAttr == Type.PointerAttr.ref) {
                //target.implicitGetAddress = true;
                aexpr._addressOf = true;
                from = Type.pointerType(loc, from, Type.PointerAttr.ref, false);
            }
            else if (pinfo.pointerAttr == Type.PointerAttr.inst) {
                target.implicitGetAddress = true;
                from = Type.pointerType(loc, from, Type.PointerAttr.inst, false);
            }
        }
        
        if (!from.fit(to)) {
            boolean allowUnsafeCast = false;
            if (!allowUnsafeCast) {
                from.fit(to);
                err("Type mismatch: " + from.toString() + " => " + to.toString() , loc);
                return;
            }
        }
        
        AstNode resolvedDef = idResolvedDef(target);
        if (resolvedDef != null) {
            if (resolvedDef instanceof AstNode.FieldDef) {
                if (!isCopyable(target.resolvedType)) {
                    if (to.detail instanceof Type.PointerInfo p2) {
                        if (p2.pointerAttr == Type.PointerAttr.own) {
                            err("Miss move keyword", loc);
                        }
                    }
                    else {
                        err("Miss move keyword", loc);
                    }
                }
            }
        }
        
        if (target instanceof LiteralExpr lit && to.detail instanceof Type.PointerInfo p2) {
            if (p2.pointerAttr == Type.PointerAttr.own || p2.pointerAttr == Type.PointerAttr.ref) {
                lit.nullPtrType = to;
            }
        }
        
        if (from.detail instanceof Type.PointerInfo p1 && to.detail instanceof Type.PointerInfo p2) {
            if (p1.pointerAttr != Type.PointerAttr.raw && p2.pointerAttr == Type.PointerAttr.raw) {
                target.implicitTypeConvertTo = to;
                target.isPointerConvert = true;
            }
            else if ((p1.pointerAttr == Type.PointerAttr.own || p1.pointerAttr == Type.PointerAttr.ref) && p2.pointerAttr == Type.PointerAttr.inst) {
                target.implicitTypeConvertTo = to;
                target.isPointerConvert = true;
            }
            else if (p1.pointerAttr == Type.PointerAttr.own && p2.pointerAttr == Type.PointerAttr.ref) {
                target.implicitTypeConvertTo = to;
                target.isPointerConvert = true;
            }
            else if (p1.pointerAttr != p2.pointerAttr) {
                err("Unknow convert", loc);
            }
        }
        
        if (!from.equals(to)) {
            if (target instanceof WithBlockExpr wbe) {
                wbe._storeVar = null;
            }
            target.implicitTypeConvertTo = to;
        }
    }
    
    public static AstNode idResolvedDef(Expr target) {
        if (target instanceof Expr.NonNullableExpr e) {
            target = e.operand;
        }

        if (target instanceof Expr.IdExpr e) {
            return e.resolvedDef;
        }
        else if (target instanceof Expr.AccessExpr e) {
            return e.resolvedDef;
        }
        else if (target instanceof Expr.IndexExpr e) {
            return e.resolvedOperator;
        }
        else if (target instanceof Expr.GenericInstance e) {
            return e.resolvedDef;
        }
        return null;
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        if (v.initExpr != null) {
            this.visit(v.initExpr);
        }
        
//        if ((v.flags & FConst.Static) != 0) {
//            err("Unsupport Static Field", v.loc);
//        }
        
        if (v.fieldType != null && v.fieldType.detail instanceof PointerInfo pinfo) {
            if (pinfo.pointerAttr == Type.PointerAttr.inst) {
                if (!isInUnsafe()) {
                    err("Can't be inst pointer", v.loc);
                }
            }
        }
        
        //check constexpr
        if ((v.flags & FConst.ConstExpr) != 0) {
            if (v.initExpr == null) {
                err("Must init constExpr", v.loc);
            }
            else if (v.initExpr instanceof Expr.LiteralExpr) {
                if (!v.isStatic()) {
                    err("The constExpr must be static", v.loc);
                }
            }
            else {
                err("Invalid constExpr flags", v.loc);
            }
        }
        
        if (v.fieldType == null) {
            err("Unkonw field type", v.loc);
        }
        else {
            if (v.initExpr != null) {
                boolean ok = false;
                if (v.parent instanceof TypeDef td) {
                    //already checked in ExprTypeResolver
                    if (td.isEnum()) {
                        ok = true;
                    }
                }
                if (!ok) {
                    verifyTypeFit(v.initExpr, v.fieldType, v.loc);
                }
            }

            //check nullable
            if (v.initExpr == null) {
                if (v.parent instanceof TypeDef) {

                }
                else if (!v.uninit) {
                    err("Variable is not initialized", v.loc);
                }
            }

            //check const is static
            if ((v.flags & FConst.ConstExpr) == 0) {
                boolean isStatic = v.isStatic();
                if (isStatic && !v.fieldType.isImmutable) {
                    if ((v.flags & FConst.Unsafe) == 0) {
                        err("Static var must be const", v.loc);
                    }
                }
            }
        }
    }


    @Override
    public void visitFunc(AstNode.FuncDef v) {
        if (v.parent instanceof TypeDef sd) {
            if (sd.isStruct()) {
                if ((v.flags & FConst.Virtual) != 0) {
                    if ((sd.flags & FConst.Virtual) != 0 || (sd.flags & FConst.Abstract) != 0) {
                        //ok
                    }
                    else {
                        err("Struct must be virtual or abstract", v.loc);
                    }
                }
                else if ((v.flags & FConst.Abstract) != 0) {
                    if ((sd.flags & FConst.Abstract) != 0) {
                        //ok
                    }
                    else {
                        err("Struct must be abstract", v.loc);
                    }
                    if (v.code != null) {
                        err("abstract method must no code", v.loc);
                    }
                }
            }
            else if (sd.isTrait()) {
                if ((v.flags & FConst.Abstract) != 0) {
                    if (v.code != null) {
                        err("abstract method must no code", v.loc);
                    }
                }
            }
        }
        else {
            if ((v.flags & FConst.Abstract) != 0 ||
                    (v.flags & FConst.Virtual) != 0) {
                err("Invalid abstract or virtual flags", v.loc);
            }
        }
        
        if (v.code == null && (!module.isImported && !module.scriptMode)) {
            if ((v.flags & (FConst.Abstract|FConst.Virtual|FConst.Extern| FConst.ExternC)) == 0) {
                if (curStruct != null) {
                    if ((curStruct.flags & (FConst.Abstract|FConst.Virtual|FConst.Extern| FConst.ExternC)) == 0) {
                        err("Miss fun code", v.loc);
                    }
                }
                else {
                    err("Miss fun code", v.loc);
                }
            }
        }
        
        if (v.prototype.paramDefs != null) {
            boolean hasDefaultValue = false;
            boolean hasVararg = false;
            for (FieldDef p : v.prototype.paramDefs) {
                if (p.initExpr != null) {
                    if (hasDefaultValue) {
                        err("Default param must at last", p.loc);
                    }
                    hasDefaultValue = true;
                }
                if (p.fieldType.isVarArgType()) {
                    if (hasVararg) {
                        err("Vararg must at last", p.loc);
                    }
                    hasVararg = true;
                }
            }
        }
        
        if ((v.flags & FConst.Reflect) != 0 ) {
            if (v.generiParamDefs != null) {
                err("Unsupport reflection for generic type", v.loc);
            }
        }
        
        if ((v.flags & FConst.Static) != 0 && v.parent instanceof FileUnit) {
            err("Invalid static flags", v.loc);
        }
        
        if ((v.flags & FConst.Readonly) != 0) {
            err("Invalid flags", v.loc);
        }
        
        if ((v.flags & FConst.ConstExpr) != 0) {
            err("Unsupport constExpr for func", v.loc);
        }
        
        if ((v.flags & FConst.Operator) != 0) {
            verifyOperatorDef(v);
        }
        
        if (v.code != null) {
            if ((v.flags & FConst.Unsafe) != 0) {
                ++inUnsafe;
            }
            hasReturn = false;
            this.visit(v.code);
            
            if (!hasReturn && v.prototype.returnType != null && !v.prototype.returnType.isVoid()) {
                err("Expect return value", v.loc);
            }
            
            if ((v.flags & FConst.Unsafe) != 0) {
                --inUnsafe;
            }
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {

        //if (v instanceof StructDef sd) {
        curStruct = v;

        if (v.inheritances != null) {
            int i = 0;
            for (Type inh : v.inheritances) {
                if (i == 0) {
                    if (inh.id.resolvedDef instanceof TypeDef superSd) {
                        if (superSd.isStruct()) {
                            if ((superSd.flags & FConst.Abstract) != 0 || (superSd.flags & FConst.Virtual) != 0) {
                                //ok
                            }
                            else {
                                err("Base struct must be abstract or virutal", inh.loc);
                            }
                        }
                        else if (superSd.isTrait()) {
                            //ok
                        }
                        else {
                            err("Invalid inheritance", inh.loc);
                        }
                    }
                    else {
                        err("Invalid inheritance", inh.loc);
                    }
                }
                if (i > 0) {
                    if (inh.id.resolvedDef != null) {
                        if (inh.id.resolvedDef instanceof TypeDef superSd) {
                            if (!superSd.isTrait()) {
                                err("Unsupport multi struct inheritance", inh.loc);
                            }
                        }
                    }
                }
                ++i;
            }
        }

        if ((v.flags & FConst.Reflect) != 0 ) {
            if (v.generiParamDefs != null) {
                err("Unsupport reflection for generic type", v.loc);
            }
        }
        //}
        v.walkChildren(this);

        curStruct = null;
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof AstNode.Block bs) {
            bs.walkChildren(this);
        }
        else if (v instanceof Stmt.IfStmt ifs) {
            this.visit(ifs.condition);
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                this.visit(ifs.elseBlock);
            }
            verifyBool(ifs.condition);
        }
        else if (v instanceof Stmt.LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof Stmt.WhileStmt whiles) {
            this.visit(whiles.condition);
            this.visit(whiles.block);
            verifyBool(whiles.condition);
        }
        else if (v instanceof Stmt.ForStmt fors) {
            if (fors.init != null) {
                if (fors.init instanceof Stmt.LocalDefStmt varDef) {
                    this.visit(varDef.fieldDef);
                }
                else if (fors.init instanceof Stmt.ExprStmt s) {
                    this.visit(s.expr);
                }
                else {
                    //err("Unsupport for init stmt", fors.init.loc);
                }
            }
            
            if (fors.condition != null) {
                this.visit(fors.condition);
                verifyBool(fors.condition);
            }
            
            if (fors.update != null) {
                this.visit(fors.update);
            }
            this.visit(fors.block);
        }
        else if (v instanceof Stmt.SwitchStmt switchs) {
            this.visit(switchs.condition);
            
            if (switchs.condition.resolvedType != null) {
                if (switchs.condition.resolvedType.isInt()) {
                }
                else if (switchs.condition.resolvedType.isEnumType()) {
                }
                else {
                    err("Must be Int or Enum type", switchs.condition.loc);
                }
            }
            
            for (Stmt.CaseBlock cb : switchs.cases) {
                this.visit(cb.caseExpr);
                this.visit(cb.block);
            }
            
            if (switchs.defaultBlock != null) {
                this.visit(switchs.defaultBlock);
            }
        }
        else if (v instanceof Stmt.ExprStmt exprs) {
            this.visit(exprs.expr);
        }
        else if (v instanceof Stmt.JumpStmt jumps) {

        }
        else if (v instanceof Stmt.UnsafeBlock bs) {
            ++inUnsafe;
            this.visit(bs.block);
            --inUnsafe;
        }
        else if (v instanceof Stmt.ReturnStmt rets) {
            if (curItBlock != null) {
                err("Return from with block", v.loc);
            }
            if (rets.expr != null) {
                this.visit(rets.expr);
                
                hasReturn = true;
                
                if (rets._funcReturnType.isVoid()) {
                    err("Invalid return", rets.loc);
                }
                else {
                    this.verifyTypeFit(rets.expr, rets._funcReturnType, rets.expr.loc);
                }
                
                if (rets._funcReturnType != null && rets._funcReturnType.detail instanceof PointerInfo pinfo) {
                    if (pinfo.pointerAttr == Type.PointerAttr.inst) {
                        if (!isInUnsafe()) {
                            if (rets.expr.resolvedType != null && !rets.expr.resolvedType.isNullType()) {
                                err("Expect unsafe block", rets.loc);
                            }
                        }
                    }
                }
            }
            else {
                if (!rets._funcReturnType.isVoid()) {
                    err("Invalid return", rets.loc);
                }
            }
        }
        else {
            err("Unkown stmt:"+v, v.loc);
        }
    }
    
    private void verifyBool(Expr condition) {
        if (condition.resolvedType != null && !condition.resolvedType.isBool()) {
            err("Must be Bool", condition.loc);
        }
    }
    
    private void verifyInt(Expr e) {
        if (e.resolvedType != null && !e.resolvedType.isInt()) {
            err("Must be Int type", e.loc);
        }
    }
    
    private void verifyMetType(Expr e) {
        if (e.resolvedType != null && !e.resolvedType.isMetaType()) {
            err("Type required", e.loc);
        }
    }
    
    private void verifyOperatorDef(AstNode.FuncDef f) {
        
        if (f.isStatic()) {
            err("Can't be static", f.loc);
        }
        
        if (f.name.equals("plus") || f.name.equals("minus") || 
                f.name.equals("mult") || f.name.equals("div")) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (f.prototype.returnType.isVoid()) {
                err("Must has return", f.loc);
            }
        }
        else if (f.name.equals("compare")) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (!f.prototype.returnType.isInt()) {
                err("Must return Int", f.loc);
            }
        }
        else if (f.name.equals(Buildin.getOperator)) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (f.prototype.returnType.isVoid()) {
                err("Must has return", f.loc);
            }
        }
        else if (f.name.equals(Buildin.setOperator)) {
            if (f.prototype.paramDefs.size() != 2) {
                err("Must 1 params", f.loc);
            }
        }
    }
    
//    private void verifyAccess(Expr target, AstNode resolvedSlotDef, Loc loc) {
////        if (target.resolvedType.detail instanceof Type.PointerInfo pinfo) {
////            if (pinfo.isNullable) {
////                err("Maybe null", target.loc);
////            }
////        }
//        if (target.resolvedType == null) {
//            return;
//        }
//        
//        boolean isImutable = target.resolvedType.isImmutable;
//        if (target.resolvedType.isPointerType() && target.resolvedType.genericArgs != null) {
//            isImutable = target.resolvedType.genericArgs.get(0).isImmutable;
//        }
//        
//        if (resolvedSlotDef instanceof AstNode.FuncDef f) {
//            if (isImutable && (f.prototype.postFlags & FConst.Mutable) != 0) {
//                err("Const error", loc);
//            }
//        }
//    }
    
    private void verifyUnsafe(Expr target) {
        if (target instanceof IdExpr id) {
            if (id.name.equals(TokenKind.thisKeyword.symbol) || id.name.equals(TokenKind.superKeyword.symbol)
                    || id.name.equals(TokenKind.dot.symbol)) {
                return;
            }
        }
        else if (target instanceof Expr.UnaryExpr uexpr) {
            if (uexpr.opToken == TokenKind.amp) {
                return;
            }
        }
        
        if (target.resolvedType != null && target.resolvedType.detail instanceof Type.PointerInfo pt) {
            if (pt.pointerAttr == Type.PointerAttr.raw) {
                if (!isInUnsafe()) {
                    err("Expect unsafe block", target.loc);
                }
            }
        }
        
        AstNode resolvedDef = idResolvedDef(target);
        if (resolvedDef != null) {
            if (resolvedDef instanceof AstNode.FuncDef f) {
                if ((f.flags & FConst.Unsafe) != 0) {
                    err("Expect unsafe block", target.loc);
                }
            }
            if (resolvedDef instanceof AstNode.FieldDef f) {
                if ((f.flags & FConst.Unsafe) != 0) {
                    err("Expect unsafe block", target.loc);
                }
            }
        }
    }

    @Override
    public void visitExpr(Expr v) {
        if (v instanceof Expr.IdExpr e) {
            if (e.resolvedDef != null) {                
                if (e.resolvedDef instanceof AstNode.FieldDef f) {
                    checkProtection(f, f.parent, v.loc, e.inLeftSide);
                }
                else if (e.resolvedDef instanceof AstNode.FuncDef f) {
                    checkProtection(f, f.parent, v.loc, e.inLeftSide);
                }
            }
        }
        else if (v instanceof Expr.AccessExpr e) {
            this.visit(e.target);
            verifyUnsafe(e.target);
            //verifyAccess(e.target, e.resolvedDef, e.loc);
            if (e.resolvedDef != null) {                
                if (e.resolvedDef instanceof AstNode.FieldDef f) {
                    checkProtection(f, f.parent, v.loc, e.inLeftSide);
                }
                else if (e.resolvedDef instanceof AstNode.FuncDef f) {
                    checkProtection(f, f.parent, v.loc, e.inLeftSide);
                    if (f.isStatic()) {
                        err("Access static by '::'", e.loc);
                    }
                }
            }
        }
        else if (v instanceof Expr.LiteralExpr e) {
        }
        else if (v instanceof Expr.BinaryExpr e) {
            resolveBinaryExpr(e);
        }
        else if (v instanceof Expr.CallExpr e) {
            resolveCallExpr(e);
        }
        else if (v instanceof Expr.UnaryExpr e) {
            this.visit(e.operand);
            if (e.operand.isResolved()) {
                Token.TokenKind curt = e.opToken;
                switch (curt) {
                    //~
                    case tilde:
                        verifyInt(e.operand);
                        break;
                    //!
                    case bang:
                        verifyBool(e.operand);
                        break;
                    //+, -
                    case plus:
                    case minus:
                        break;
                    //*
                    case star:
                        if (e.resolvedType != null) {
                            verifyUnsafe(e.operand);
                        }
                        break;
                    //++, --
                    case increment:
                    case decrement:
                        verifyInt(e.operand);
                        if (e.operand.resolvedType.isImmutable) {
                            err("Const error", e.loc);
                        }
                        break;
                    //&
                    case amp:
                        break;
                    case awaitKeyword:
                        break;
                    case moveKeyword:
                        AstNode defNode = idResolvedDef(e.operand);
                        if (defNode != null) {
                            if (defNode instanceof AstNode.FieldDef f) {
                                if (!f.isLocalOrParam()) {
                                    if (!isMoveable(f.fieldType)) {
                                        err("Can't move", e.loc);
                                    }
                                }
                            }
                            else {
                                err("Invalid move", e.loc);
                            }
                        }
                        else {
                            err("Invalid move", e.loc);
                        }
                        if (e.operand.resolvedType.isImmutable) {
                            err("Const error", e.loc);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        else if (v instanceof Expr.TypeExpr e) {
        }
        else if (v instanceof Expr.IndexExpr e) {
            this.visit(e.target);
            verifyUnsafe(e.target);
            this.visit(e.index);
            //verifyInt(e.index);
            if (e.resolvedOperator != null) {
                Type paramType = e.resolvedOperator.prototype.paramDefs.get(0).fieldType;
                verifyTypeFit(e.index, paramType, e.index.loc);
            }
        }
        else if (v instanceof Expr.GenericInstance e) {
            this.visit(e.target);
        }
        else if (v instanceof Expr.IfExpr e) {
            this.visit(e.condition);
            this.visit(e.trueExpr);
            this.visit(e.falseExpr);
            verifyBool(e.condition);
            if (e.trueExpr.isResolved() && e.falseExpr.isResolved()) {
                if (!e.trueExpr.resolvedType.equals(e.falseExpr.resolvedType)) {
                    err("Type must equals", e.falseExpr.loc);
                }
            }
        }
        else if (v instanceof Expr.WithBlockExpr e) {
            resolveWithBlockExpr(e);
        }
        else if (v instanceof Expr.ArrayBlockExpr e) {
            resolveArrayBlockExpr(e);
        }
        else if (v instanceof Expr.ClosureExpr e) {
            this.visit(e.code);
        }
        else if (v instanceof Expr.NonNullableExpr e) {
            this.visit(e.operand);
            if (e.operand.resolvedType.detail instanceof Type.PointerInfo pinfo) {
                if (!pinfo.isNullable) {
                    err("Must nullable expr", v.loc);
                }
            }
            else {
                err("Must nullable expr", v.loc);
            }
        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
    }

    private void resolveWithBlockExpr(Expr.WithBlockExpr e) {
        this.visit(e.target);
        
//        if (e._storeVar == null && e._isType) {
//            boolean ok = false;
//            if (e.target instanceof Expr.IdExpr id) {
//                ok = true;
//            }
//            if (!ok) {
//                err("Value type init block must in standalone assgin statement", e.loc);
//            }
//        }
        
        boolean hasFuncCall = false;
        if (e.block != null) {
            curItBlock = e;
            for (Stmt t : e.block.stmts) {
                this.visit(t);
                if (t instanceof ExprStmt exprStmt) {
                    if (exprStmt.expr instanceof CallExpr) {
                        hasFuncCall = true;
                    }
                }
            }
            curItBlock = null;
        }
        if (!e.target.isResolved()) {
            return;
        }
        
        AstNode.TypeDef sd = e._structDef;
        if (sd != null) {            
            if (e._isType && (sd.flags & FConst.Abstract) != 0) {
                err("It's abstract", e.target.loc);
            }
            
            if (e.block != null) {
                
                HashMap<String,FieldDef> fields = new HashMap<>();
                sd.getAllFields(fields);
                
                for (HashMap.Entry<String,FieldDef> entry : fields.entrySet()) {
                    AstNode.FieldDef f = entry.getValue();
                    if (f.initExpr != null) {
                        continue;
                    }
                    if (f.fieldType == null) {
                        continue;
                    }
                    if (f.fieldType.isNullablePointerType()) {
                        continue;
                    }
                    if (f.fieldType.isNum()) {
                        continue;
                    }
                    
                    boolean found = false;
                    for (Stmt t : e.block.stmts) {
                        if (t instanceof ExprStmt exprStmt) {
                            if (exprStmt.expr instanceof BinaryExpr bexpr) {
                                if (bexpr.opToken == TokenKind.assign) {
                                    if (bexpr.lhs instanceof IdExpr idExpr) {
                                        if (idExpr.resolvedDef == f) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    else if (bexpr.lhs instanceof AccessExpr accExpr) {
                                        if (accExpr.resolvedDef == f && accExpr.target instanceof IdExpr iexpr) {
                                            if (iexpr.name.equals(".")) {
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!found) {
                        err("Field not init:"+f.name, e.loc);
                    }
                }

//                for (Expr.CallArg t : e.args) {
//                    if (!fields.containsKey(t.name)) {
//                        err("Field not found:"+t.name, t.loc);
//                    }
//                }
            }
        }
    }
    
    private void resolveArrayBlockExpr(Expr.ArrayBlockExpr e) {
        
        if (!e.type.id.isResolved()) {
            return;
        }
        
        for (Expr t : e.args) {
            this.verifyTypeFit(t, e.type.genericArgs.get(0), t.loc);
        }
                
        if (e._storeVar == null) {
            err("Invalid ArrayBlock", e.loc);
        }
    }
    
    private void resolveCallExpr(Expr.CallExpr e) {
        this.visit(e.target);
        verifyUnsafe(e.target);
        if (e.args != null) {
            for (Expr.CallArg t : e.args) {
                this.visit(t.argExpr);
            }
        }
        if (e.target.isResolved()) {
            if (e.target.resolvedType.detail instanceof Type.FuncInfo f) {
                
                if (e.args != null) {
                    if (f.prototype.paramDefs == null) {
                        err("Args error", e.loc);
                    }
                    else {
                        int i = 0;
                        for (Expr.CallArg t : e.args) {
                            if (i >= f.prototype.paramDefs.size()) {
                                break;
                            }
                            if (t.name != null) {
                                if (!t.name.equals(f.prototype.paramDefs.get(i).name)) {
                                    err("Arg name error", t.loc);
                                }
                            }
                            verifyTypeFit(t.argExpr, f.prototype.paramDefs.get(i).fieldType, t.loc, true);
                            ++i;
                        }
                        if (i < e.args.size()) {
                            if (!f.prototype.paramDefs.get(f.prototype.paramDefs.size()-1).fieldType.isVarArgType()) {
                                err("Arg number error", e.loc);
                            }
                        }
                        
                        if (i < f.prototype.paramDefs.size()) {
                            if (f.prototype.paramDefs.get(i).initExpr == null && !f.prototype.paramDefs.get(i).fieldType.isVarArgType()) {
                                err("Arg number error", e.loc);
                            }
                        }
                    }
                }
                else if (f.prototype.paramDefs != null) {
                    if (f.prototype.paramDefs.get(0).initExpr == null) {
                        err("Arg number error", e.loc);
                    }
                }
                
                if (f.funcDef != null) {
                    if (f.funcDef.generiParamDefs != null && !(e.target instanceof GenericInstance)) {
                        err("Miss generic args", e.target.loc);
                    }
                }
            }
            else {
                err("Call a non-function type:"+e.target, e.loc);
            }
        }
        else {
            return;
        }
    }
    
    private boolean checkProtection(AstNode.TopLevelDef slot, AstNode parent, Loc loc, boolean isSet) {
        int slotFlags = slot.flags;
        if (isSet && slot instanceof AstNode.FieldDef f) {
            if ((f.flags & FConst.Readonly) != 0) {
                slotFlags |= FConst.Private;
            }
        }
        
        if (parent instanceof AstNode.TypeDef tparent) {
            if (parent != curStruct) {
                if ((slotFlags & FConst.Private) != 0) {
                    err("It's private", loc);
                    return false;
                }

                if ((slotFlags & FConst.Protected) != 0) {
                    if (curStruct == null || !curStruct.isInheriteFrom(tparent)) {
                        err("It's protected", loc);
                    }
                }
            }
        }
        else if (parent instanceof AstNode.FileUnit fu) {
            if ((slotFlags & FConst.Protected) != 0) {
                if (fu.module != this.module) {
                    err("It's private or protected", loc);
                }
            }
            
            if ((slotFlags & FConst.Private) != 0) {
                if (fu != this.curUnit) {
                    err("It's private or protected", loc);
                }
            }
        }
        return false;
    }

    private void resolveBinaryExpr(Expr.BinaryExpr e) {

        this.visit(e.lhs);
        this.visit(e.rhs);
        
        if (e.lhs.isResolved() && e.rhs.isResolved()) {
            Token.TokenKind curt = e.opToken;
            switch (curt) {
                case isKeyword: {
                    verifyMetType(e.rhs);
                    Type targetType = ((TypeExpr)e.rhs).type;
                    if (targetType.detail instanceof Type.PointerInfo pinfo) {
                        if (pinfo.isNullable) {
                            err("Must non-nullable", e.rhs.loc);
                        }
                    }
                }
                    break;
                case asKeyword: {
                    verifyMetType(e.rhs);
                }
                    break;
                case eq:
                case notEq:
                case same:
                case notSame:
                case lt:
                case gt:
                case ltEq:
                case gtEq:
                    if ((e.lhs.resolvedType.isFloat() && e.rhs.resolvedType.isInt()) ||
                            (e.lhs.resolvedType.isInt() && e.rhs.resolvedType.isFloat())) {
                        if (curt == Token.TokenKind.eq || curt == Token.TokenKind.notEq || curt == Token.TokenKind.same || curt == Token.TokenKind.notSame) {
                            err("Cant compare different type", e.loc);
                        }
                    }
                    else if (e.lhs.resolvedType.isPointerType() && e.rhs.resolvedType.isPointerType()) {
                        if (e.lhs.resolvedType.isNullType() || e.rhs.resolvedType.isNullType()) {
                            //OK
                        }
                        else if (e.lhs.resolvedType.detail instanceof Type.PointerInfo p1 && e.rhs.resolvedType.detail instanceof Type.PointerInfo p2) {
                            if (p1.pointerAttr != p2.pointerAttr) {
                                if (p1.pointerAttr.ordinal() > p2.pointerAttr.ordinal()) {
                                    verifyTypeFit(e.rhs, e.lhs.resolvedType, e.rhs.loc, true);
                                }
                                else {
                                    verifyTypeFit(e.lhs, e.rhs.resolvedType, e.lhs.loc, true);
                                }
                            }
                        }
                    }
                    else if (e.resolvedOperator != null) {
                        Type paramType = e.resolvedOperator.prototype.paramDefs.get(0).fieldType;
                        verifyTypeFit(e.rhs, paramType, e.rhs.loc, true);
                    }
                    else if (!e.lhs.resolvedType.equals(e.rhs.resolvedType)) {
                        err("Cant compare different type", e.loc);
                    }
                    break;
                case doubleAmp:
                case doublePipe:
                    verifyBool(e.lhs);
                    verifyBool(e.rhs);
                    break;
                case leftShift:
                case rightShift:
                case pipe:
                case caret:
                case amp:
                case percent:
                    verifyInt(e.lhs);
                    verifyInt(e.rhs);
                    break;
                case plus:
                case minus:
                case star:
                case slash:
                    if (e.resolvedOperator != null) {
                        Type paramType = e.resolvedOperator.prototype.paramDefs.get(0).fieldType;
                        verifyTypeFit(e.rhs, paramType, e.rhs.loc, true);
                    }
                    verifyUnsafe(e.lhs);
                    break;
                case assign:
                case assignPlus:
                case assignMinus:
                case assignStar:
                case assignSlash:
                case assignPercent:
                    boolean assignable = false;
                    if (e.lhs instanceof Expr.IdExpr idExpr) {
                        if (idExpr.resolvedDef instanceof AstNode.FieldDef f) {
                            assignable = true;
                        }
                    }
                    else if (e.lhs instanceof Expr.AccessExpr accessExpr) {
                        if (accessExpr.resolvedDef instanceof AstNode.FieldDef f) {
                            assignable = true;
                        }
                    }
                    else if (e.lhs instanceof Expr.IndexExpr indexExpr) {
                        if (indexExpr.resolvedOperator != null && indexExpr.resolvedOperator.prototype.paramDefs.size() > 1) {
                            Type paramType = indexExpr.resolvedOperator.prototype.paramDefs.get(1).fieldType;
                            verifyTypeFit(e.rhs, paramType, e.rhs.loc);
                        }
                        assignable = true;
                        return;
                    }
                    else if (e.lhs instanceof UnaryExpr lhsExpr) {
                        if (lhsExpr.opToken == TokenKind.star) {
                            assignable = true;
                        }
                    }
                    
                    if (assignable) {
                        if (e.resolvedType != null && e.resolvedType.isImmutable) {
                            err("Const error", e.loc);
                        }
                        
                        if (curt == Token.TokenKind.assign) {
                            verifyTypeFit(e.rhs, e.lhs.resolvedType, e.loc);
                            if (e.lhs instanceof IdExpr lr && e.rhs instanceof IdExpr ri) {
                                if (lr.namespace == ri.namespace) {
                                    if (lr.name.equals(ri.name)) {
                                        err("Self assign", e.loc);
                                    }
                                }
                            }
                            
                            AstNode ldef = idResolvedDef(e.lhs);
                            if (ldef instanceof FieldDef fd && e.rhs.resolvedType.detail instanceof Type.PointerInfo pinfo
                                    && e.lhs.resolvedType.detail instanceof Type.PointerInfo lpinfo) {
                                if (lpinfo.pointerAttr != Type.PointerAttr.raw && pinfo.pointerAttr == Type.PointerAttr.inst) {
                                    if (!isInUnsafe()) {
                                        err("Can't store instant pointer", e.loc);
                                        //e._refSafeCheck = true;
                                    }
                                }
                            }
                        }
                        else {
                            if (e.lhs.resolvedType.isNum() || e.lhs.resolvedType.isRawPointerType()) {
//                                if (!e.lhs.resolvedType.equals(e.rhs.resolvedType)) {
//                                    err("Type mismatch", e.loc);
//                                }
                                this.verifyTypeFit(e.rhs, e.lhs.resolvedType, e.rhs.loc);
                            }
                            else {
                                err("Unsupport operator", e.loc);
                            }
                        }
                    }
                    else {
                        err("Not assignable", e.lhs.loc);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}