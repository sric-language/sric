//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.resolve;


import java.util.HashMap;
import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
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
    //private boolean hasReturn = false;
    
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
    
    public static boolean isCopyable(Type type) {
        if (type == null) {
            return true;
        }
        if (type.id.resolvedDef == null) {
            return false;
        }
        
        if (type.detail instanceof Type.PointerInfo p2) {
            if (p2.pointerAttr == Type.PointerAttr.own || p2.pointerAttr == Type.PointerAttr.uniq) {
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
                    if (f.isStatic()) {
                        continue;
                    }
                    if (f.fieldType == type) {
                        return true;
                    }
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
        
//        if (type.isImmutable) {
//            return false;
//        }
        
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
                    if (f.isStatic()) {
                        continue;
                    }
                    if (f.fieldType == type) {
                        return true;
                    }
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
        verifyTypeFit(target, to, loc, false, false, false);
    }
    private void verifyTypeFit(Expr target, Type to, Loc loc, boolean isCallArg, boolean isReturn, boolean isCompare) {
        Type from = target.resolvedType;
        if (from == null) {
            return;
        }
        
//        if (loc.line == 74) {
//            System.err.println("DEBUG");
//        }
        
        //implicit convert string
        //sric::String
        if (to.id.resolvedDef instanceof TypeDef td) {
            if (td.name.equals("String") && td.parent instanceof FileUnit funit) {
                if (funit.module.name.equals("sric")) {
                    //const char *;
                    if (from.detail instanceof Type.PointerInfo pinfo && from.genericArgs != null) {
                        if (from.isRawPointerType() && from.genericArgs.get(0).detail instanceof Type.NumInfo ninfo) {
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
        
//        if (from.isPointerType() && !to.isPointerType()) {
//            if (!from.isRawPointerType() && from.genericArgs != null) {
//                target.implicitDereference = true;
//                from = from.genericArgs.get(0);
//            }
//        }
//        else if (!from.isPointerType() && to.detail instanceof Type.PointerInfo pinfo) {
//            if (from.isRefable && pinfo.pointerAttr == Type.PointerAttr.ref) {
//                target.implicitGetAddress = true;
//                from = Type.pointerType(loc, from, Type.PointerAttr.ref, false);
//            }
//            else if (target instanceof AccessExpr aexpr && 
//                    (aexpr.target.resolvedType.isOwnOrRefPointerType() || aexpr.target.resolvedType.isRefable) && 
//                    pinfo.pointerAttr == Type.PointerAttr.ref) {
//                //target.implicitGetAddress = true;
//                aexpr._addressOf = true;
//                from = Type.pointerType(loc, from, Type.PointerAttr.ref, false);
//            }
//            else if (pinfo.pointerAttr == Type.PointerAttr.inst) {
//                target.implicitGetAddress = true;
//                from = Type.pointerType(loc, from, Type.PointerAttr.inst, false);
//            }
//        }

        if (!isCompare && !to.isVarArgType() && from.isNullablePointerType() && (!to.isNullablePointerType() && !to.isFuncType())) {
            target.checkNonnullable = true;
        }
        
        if (!from.fit(to)) {
            err("Type mismatch: " + from.getQName(false) + " => " + to.getQName(false) , loc);
            from.fit(to);
            return;
        }
        
        checkMove(target, isReturn, to, loc);

        
        if (target instanceof LiteralExpr lit && to.detail instanceof Type.PointerInfo p2) {
            if (p2.pointerAttr == Type.PointerAttr.own || p2.pointerAttr == Type.PointerAttr.uniq || p2.pointerAttr == Type.PointerAttr.ref) {
                lit.nullPtrType = to;
            }
        }
        
        if (from.detail instanceof Type.PointerInfo p1 && to.detail instanceof Type.PointerInfo p2) {
            if (p1.pointerAttr != Type.PointerAttr.raw && p2.pointerAttr == Type.PointerAttr.raw) {
                target.implicitTypeConvertTo = to;
                target.isPointerConvert = true;
            }
//            else if ((p1.pointerAttr == Type.PointerAttr.own || p1.pointerAttr == Type.PointerAttr.ref) && p2.pointerAttr == Type.PointerAttr.inst) {
//                target.implicitTypeConvertTo = to;
//                target.isPointerConvert = true;
//            }
            else if ((p1.pointerAttr == Type.PointerAttr.own || p1.pointerAttr == Type.PointerAttr.uniq) && p2.pointerAttr == Type.PointerAttr.ref) {
                target.implicitTypeConvertTo = to;
                target.isPointerConvert = true;
            }
            else if (p1.pointerAttr != p2.pointerAttr) {
                err("Unknow convert", loc);
            }
        }
        
        if (!from.typeEquals(to)) {
            if (target instanceof WithBlockExpr wbe) {
                wbe._storeVar = null;
            }
            target.implicitTypeConvertTo = to;
        }
    }

    private void checkMove(Expr target, boolean isReturn, Type to, Loc loc) {
        
        boolean targetNeedMove = false;
        //field not moved
        AstNode resolvedDef = idResolvedDef(target);
        if (resolvedDef != null) {
            if (resolvedDef instanceof AstNode.FieldDef) {
                targetNeedMove = true;
            }
        }
        //fix return reference
        //foo():&A;
        //var a: A = foo();
        if (target.resolvedType.isReference) {
            targetNeedMove = true;
        }
        
        if (targetNeedMove && !to.isReference && !isCopyable(target.resolvedType) ) {
            //local var auto move
            if (isReturn && resolvedDef instanceof AstNode.FieldDef f) {
                if (f.isLocalVar) {
                    //targetNeedMove = false;
                    target.implicitMove = true;
                    return;
                }
            }
            
            if (to.detail instanceof Type.PointerInfo p2) {
                if (p2.pointerAttr == Type.PointerAttr.own || p2.pointerAttr == Type.PointerAttr.uniq) {
                    err("Miss move keyword", loc);
                }
            }
            else {
                err("Miss move keyword", loc);
            }
        }
    }
    
    public static AstNode idResolvedDef(Expr target) {
//        if (target instanceof Expr.NonNullableExpr e) {
//            target = e.operand;
//        }

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

        if ((v.flags & FConst.Virtual) != 0 || (v.flags & FConst.Abstract) != 0) {
            err("Invalide flags", v.loc);
        }

        if (v.parent instanceof TypeDef sd && v.isExtern()) {
            err("Invalide extern", v.loc);
        }
        
        if (v.fieldType != null && v.fieldType.isReference) {
//            if (pinfo.pointerAttr == Type.PointerAttr.inst) {
//                if (!isInUnsafe()) {
//                    err("Cannot be inst pointer", v.loc);
//                }
//            }
            if (!isInUnsafe()) {
                err("Cannot define reference in safe mode", v.loc);
            }
        }
        
        //check constexpr
        if ((v.flags & FConst.ConstExpr) != 0) {
            if (v.fieldType != null && !v.fieldType.isImmutable) {
                err("constexpr must be const", v.loc);
            }
            if (v.initExpr == null) {
                if (!this.module.isStubFile && !v.isExtern())
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
                if (v.isEnumField()) {
                    //already checked in ExprTypeResolver
                }
                else if (v.initExpr.resolvedType == v.fieldType) {
                    //infered
                    checkMove(v.initExpr, false, v.fieldType, v.initExpr.loc);
                }
                else {
                    verifyTypeFit(v.initExpr, v.fieldType, v.loc);
                }
            }

            //check nullable
            if (v.initExpr == null) {
                if (v.parent instanceof TypeDef) {

                }
                else if (v.fieldType != null && !v.fieldType.hasDefaultValue()) {
                    if (!v.unkonwInit && !v.isExtern())
                        err("Variable is not initialized", v.loc);
                }
            }

            //check const is static
            if ((v.flags & FConst.ConstExpr) == 0) {
                boolean isStatic = v.isStatic();
                if (isStatic && !v.fieldType.isDeepImmutable()) {
                    if ((v.flags & FConst.Unsafe) == 0) {
                        err("Static var must be deep immutable", v.loc);
                    }
                }
            }
            
            if (v.fieldType.isMetaType()) {
                err("Unsupport MetaType", v.loc);
            }
            
            if (v.fieldType.detail instanceof Type.FuncInfo finfo) {
                if (finfo.isInstanceMethod()) {
                    err("Unsupport Member function pointer", v.loc);
                }
            }
        }
    }


    @Override
    public void visitFunc(AstNode.FuncDef v) {
        if (v.parent instanceof TypeDef sd) {
            if (v.name.equals(TokenKind.deleteKeyword.symbol) && sd.isPolymorphic()) {
                if ((v.flags & FConst.Virtual) == 0) {
                    err("Excpect virtual dector", v.loc);
                }
            }
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
                else if ((v.flags & FConst.Static) == 0) {
                    err("Must be abstract", v.loc);
                }
            }
        }
        else {
            if ((v.flags & FConst.Abstract) != 0 ||
                    (v.flags & FConst.Virtual) != 0) {
                err("Invalid abstract or virtual flags", v.loc);
            }
        }
        
        if (v.code == null && (!module.isStubFile)) {
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
                if (p.fieldType == null) {
                    //error already report
                    continue;
                }
                if (p.initExpr != null) {
                    hasDefaultValue = true;
                    AstNode idDef = idResolvedDef(p.initExpr);
                    if (idDef instanceof FuncDef f) {
                        if (!f.isStatic()) {
                            err("Unsupport param default value", p.initExpr.loc);
                        }
                    }
                    else if (idDef instanceof FieldDef f) {
                        if (!f.isStatic() && !f.isEnumField()) {
                            err("Unsupport param default value", p.initExpr.loc);
                        }
                    }
                    
                    this.verifyTypeFit(p.initExpr, p.fieldType, p.loc);
                }
                else {
                    if (hasDefaultValue) {
                        err("Default param must at last", p.loc);
                    }
                }
                
                if (p.fieldType.isVarArgType()) {
                    hasVararg = true;
                }
                else {
                    if (hasVararg) {
                        err("Vararg must at last", p.loc);
                    }
                }
            }
        }
        
        
        if ((v.flags & FConst.Reflect) != 0 ) {
            if (v.generiParamDefs != null) {
                err("Unsupport reflection for generic type", v.loc);
            }
        }
        
        if ((v.flags & FConst.Static) != 0 && ((v.flags & FConst.Abstract) != 0 || (v.flags & FConst.Virtual) != 0)) {
            err("Invalid flags", v.loc);
        }
        
        if ((v.flags & FConst.Private) != 0 && ((v.flags & FConst.Abstract) != 0 || (v.flags & FConst.Virtual) != 0)) {
            err("Invalid private", v.loc);
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

            this.visit(v.code);
            
            if (v.prototype.returnType != null && !v.prototype.returnType.isVoid()) {
                if (!v.code.isLastReturnValue()) {
                    err("Expect return value", v.loc);
                }
            }
            
            if ((v.flags & FConst.Unsafe) != 0) {
                --inUnsafe;
            }
        }
        
        if ((v.flags & FConst.Ctor) != 0) {
            if ((v.flags & FConst.Static) != 0) {
                err("Invalid static", v.loc);
            }
            if (v.prototype.paramDefs != null && v.prototype.paramDefs.size() > 0) {
                err("Ctor unsupport paramters", v.loc);
            }

            if (v.prototype.returnType != null && !v.prototype.returnType.isVoid()) {
                err("Cannot return from Ctor", v.loc);
            }
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {

        //if (v instanceof StructDef sd) {
        curStruct = v;

        if (v.inheritances != null) {
            int i = 0;
            TypeDef firstInherit = null;
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
                            firstInherit = superSd;
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
                            if (firstInherit != null && firstInherit.isInheriteFrom(superSd)) {
                                err("MultiInherit "+superSd.name+" in "+firstInherit.name, inh.loc);
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
        
        if (v.isEnum() && v.enumBase != null) {
            if (!v.enumBase.isInt()) {
                err("Enum base must be Int", v.enumBase.loc);
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
            if (exprs.expr instanceof IdExpr) {
                err("Can not be statment", exprs.expr.loc);
            }
            else if (exprs.expr instanceof AccessExpr) {
                if (exprs.expr.resolvedType != null && exprs.expr.resolvedType.isFuncType()) {
                    err("Can not be statment", exprs.expr.loc);
                }
            }
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
                
                //hasReturn = true;

                if (rets._funcReturnType.isVoid()) {
                    err("Invalid return", rets.loc);
                }
                else {
                    this.verifyTypeFit(rets.expr, rets._funcReturnType, rets.expr.loc, false, true, false);
                }
                
//                if (rets._funcReturnType != null && rets._funcReturnType.detail instanceof PointerInfo pinfo) {
//                    if (pinfo.pointerAttr == Type.PointerAttr.inst) {
//                        if (!isInUnsafe()) {
//                            if (rets.expr.resolvedType != null && !rets.expr.resolvedType.isNullType()) {
//                                err("Expect unsafe block", rets.loc);
//                            }
//                        }
//                    }
//                }
                
                if (rets._funcReturnType != null && rets._funcReturnType.isReference) {
                    if (!isInUnsafe()) {
                        if (rets.expr.resolvedType != null && !rets.expr.resolvedType.isNullType()) {
                            err("Expect unsafe block", rets.loc);
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
            err("Cannot be static", f.loc);
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
        else if (f.name.equals("add")) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (f.prototype.returnType.isVoid()) {
                err("Must return Self", f.loc);
            }
        }
        else {
            err("Unkonw operator", f.loc);
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
        if (isInUnsafe()) {
            return;
        }
        if (target instanceof IdExpr id) {
            if (id.name.equals(TokenKind.thisKeyword.symbol) || id.name.equals(TokenKind.superKeyword.symbol)
                    || id.name.equals(TokenKind.dot.symbol) ) {
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
                err("Expect unsafe block", target.loc);
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
                        if (e.operand.resolvedType != null) {
                            if (e.operand.resolvedType.isInt()) {
                            }
                            else if (e.operand.resolvedType.isRawPointerType()) {
                                if (!isInUnsafe()) {
                                    err("Expect unsafe", e.loc);
                                }
                            }
                            else {
                                err("Must be Int type", e.loc);
                            }
                        }
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
                                        err("Cannot move", e.loc);
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
                    case newKeyword: {
                        if (e.operand.resolvedType.detail instanceof Type.MetaTypeInfo typeInfo) {
                            if (typeInfo.type.id.resolvedDef instanceof TypeDef td) {
                                if (td.isAbstract()) {
                                    err("Cannot new abstract struct: " + td.name, e.loc);
                                }
                            }
                        }
                        break;
                    }
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
            if (e.resolvedOperator != null && e.resolvedOperator.prototype.paramDefs != null) {
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
                if (!e.trueExpr.resolvedType.semanticEquals(e.falseExpr.resolvedType)) {
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
            if (e.captures != null) {
                for (IdExpr ide : e.captures) {
                    FieldDef f = (FieldDef)ide.resolvedDef;
                    if (f != null && f.fieldType != null && !this.isCopyable(f.fieldType)) {
                        err("Capture a noncopyable field:" + f.name, ide.loc);
                    }
                }
                if (e.prototype.isStaticClosure()) {
                    err("Cannot capture in static closure", e.loc);
                }
            }
        }
//        else if (v instanceof Expr.NonNullableExpr e) {
//            this.visit(e.operand);
//            if (e.operand.resolvedType.detail instanceof Type.PointerInfo pinfo) {
//                if (!pinfo.isNullable) {
//                    err("Must nullable expr", v.loc);
//                }
//            }
//            else {
//                err("Must nullable expr", v.loc);
//            }
//        }
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
            WithBlockExpr savedCurItBlock = curItBlock;
            curItBlock = e;
            for (Stmt t : e.block.stmts) {
                this.visit(t);
                if (t instanceof ExprStmt exprStmt) {
                    if (exprStmt.expr instanceof CallExpr) {
                        hasFuncCall = true;
                    }
                }
            }
            curItBlock = savedCurItBlock;
        }
        if (!e.target.isResolved()) {
            return;
        }
        
        AstNode.TypeDef sd = e._structDef;
        if (sd != null) {            
            if (e._isType && sd.isAbstract()) {
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
                    if (f.unkonwInit) {
                        continue;
                    }
                    
                    if (f.uninit || !f.fieldType.hasDefaultValue()) {
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
                }
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

        if (!e.target.isResolved()) {
            return;
        }
        
        if (e.target instanceof IdExpr ide) {
            if (ide.resolvedDef instanceof FieldDef f) {
                e.target.checkNonnullable = true;
            }
        }
        
        if (!(e.target.resolvedType.detail instanceof Type.FuncInfo f)) {
            err("Call a non-function type", e.loc);
            return;
        }
        
        if (f.funcDef != null) {
            if (f.funcDef.generiParamDefs != null && !(e.target instanceof GenericInstance)) {
                err("Miss generic args", e.target.loc);
            }
        }
        
        if (f.funcDef != null && f.funcDef.parent instanceof FileUnit funit && funit.module.name.equals("sric")) {
            if (f.funcDef.name.equals("makePtr") || f.funcDef.name.equals("makeValue")) {
                //skip args check
                return;
            }
        }
        
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
                    verifyTypeFit(t.argExpr, f.prototype.paramDefs.get(i).fieldType, t.loc, true, false, false);
                    ++i;
                }
                if (i < e.args.size()) {
                    Type lastParamType = f.prototype.paramDefs.get(f.prototype.paramDefs.size()-1).fieldType;
                    if (!lastParamType.isVarArgType()) {
                        err("Too many args", e.loc);
                    }
                    else {
                        for (; i<e.args.size(); ++i) {
                            Expr.CallArg t = e.args.get(i);
                            verifyTypeFit(t.argExpr, lastParamType, t.loc, true, false, false);
                        }
                    }
                }

                if (i < f.prototype.paramDefs.size()) {
                    if (!f.prototype.paramDefs.get(i).hasParamDefaultValue() && !f.prototype.paramDefs.get(i).fieldType.isVarArgType()) {
                        err("Too few args", e.loc);
                    }
                }
            }
        }
        else if (f.prototype.paramDefs != null) {
            if (!f.prototype.paramDefs.get(0).hasParamDefaultValue() && !f.prototype.paramDefs.get(0).fieldType.isVarArgType()) {
                err("Arg number error", e.loc);
            }
        }
    }
    
    private void checkProtection(AstNode.TopLevelDef slot, AstNode parent, Loc loc, boolean isSet) {
        int slotFlags = slot.flags;
        if (isSet && slot instanceof AstNode.FieldDef f) {
            if ((f.flags & FConst.Readonly) != 0) {
                slotFlags |= FConst.Private;
            }
        }
        
        if (parent instanceof AstNode.TypeDef tparent) {
            if (parent != curStruct) {
                if ((slotFlags & FConst.Private) != 0) {
                    //readonly
                    if ((slot.flags & FConst.Private) == 0 && curStruct != null && curStruct.isInheriteFrom(tparent)) {
                        //allow acccess readonly from subclass
                    }
                    else {
                        err("It's private", loc);
                        return;
                    }
                }

                if ((slotFlags & FConst.Protected) != 0) {
                    if (((AstNode.FileUnit)tparent.parent).module != this.module) {
                        if (curStruct == null || !curStruct.isInheriteFrom(tparent)) {
                            err("It's protected", loc);
                        }
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
                    if (e.rhs.resolvedType.detail instanceof Type.MetaTypeInfo minfo) {
                        Type asType = minfo.type;
                        if (e.lhs.resolvedType.isPointerType() && asType.isPointerType()) {
                            //ok
                        }
                        else if (e.lhs.resolvedType.isNum() && asType.isNum()) {
                            //ok
                        }
                        else if (e.lhs.resolvedType.isBool() && asType.isNum()) {
                            //ok
                        }
                        else if (e.lhs.resolvedType.isEnumType() && asType.isNum()) {
                            //ok
                        }
                        else {
                            err("Invalide as", e.loc);
                        }
                        
                        if (!asType.isPrimitiveType() || !e.lhs.resolvedType.isPrimitiveType()) {
                            if (e.lhs.resolvedType.isImmutable && !asType.isImmutable) {
                                err("Use unsafeCast", e.loc);
                            }
                        }
                        
//                        if (asType.isReference != e.lhs.resolvedType.isReference) {
//                            err("Invalide as", e.loc);
//                        }
                        
                        if (asType.detail instanceof Type.PointerInfo pinfo && (pinfo.pointerAttr == Type.PointerAttr.own || pinfo.pointerAttr == Type.PointerAttr.uniq)) {
                            err("Cannot cast to own/uniq pointer", e.rhs.loc);
                        }
                        
                        if (e.lhs.resolvedType.isPointerType()) {
                            if (e.lhs.resolvedType.fit(asType)) {
                                //OK;
                            }
                            else if (e.lhs.resolvedType.genericArgs != null && asType.isPointerType() && asType.genericArgs != null) {
                                Type from = e.lhs.resolvedType.genericArgs.get(0);
                                Type to = asType.genericArgs.get(0);
                                
                                if (from.isImmutable && !to.isImmutable) {
                                    err("Use unsafeCast", e.loc);
                                }

                                if (from.isVoid()) {
                                    err("Use unsafeCast", e.loc);
                                }
                                else if (from.id.resolvedDef instanceof TypeDef tf && !tf.isPolymorphic()) {
                                    err("Use unsafeCast", e.loc);
                                }
                            }
                            else {
                                err("Use unsafeCast", e.loc);
                            }
                        }
                    }
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
                            if (!isInUnsafe()) {
                                if (!e.lhs.resolvedType.isNullablePointerType() && e.rhs.resolvedType.isNullType()) {
                                    err("Compare non-nullable pointer to null", e.loc);
                                }
                                else if (!e.rhs.resolvedType.isNullablePointerType() && e.lhs.resolvedType.isNullType()) {
                                    err("Compare non-nullable pointer to null", e.loc);
                                }
                            }
                        }
                        else if (e.lhs.resolvedType.detail instanceof Type.PointerInfo p1 && e.rhs.resolvedType.detail instanceof Type.PointerInfo p2) {
                            if (p1.pointerAttr != p2.pointerAttr) {
                                if (p1.pointerAttr.ordinal() > p2.pointerAttr.ordinal()) {
                                    verifyTypeFit(e.rhs, e.lhs.resolvedType, e.rhs.loc, true, false, true);
                                }
                                else {
                                    verifyTypeFit(e.lhs, e.rhs.resolvedType, e.lhs.loc, true, false, true);
                                }
                            }
                        }
                    }
                    else if (e.lhs.resolvedType.isFuncType() && e.rhs.resolvedType.isNullType() && (curt == eq || curt == notEq)) {
                        //OK
                    }
                    else if (e.resolvedOperator != null && e.resolvedOperator.prototype.paramDefs != null) {
                        Type paramType = e.resolvedOperator.prototype.paramDefs.get(0).fieldType;
                        verifyTypeFit(e.rhs, paramType, e.rhs.loc, true, false, true);
                    }
                    else if (!e.lhs.resolvedType.semanticEquals(e.rhs.resolvedType)) {
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
                    if (e.resolvedOperator != null && e.resolvedOperator.prototype.paramDefs != null) {
                        Type paramType = e.resolvedOperator.prototype.paramDefs.get(0).fieldType;
                        verifyTypeFit(e.rhs, paramType, e.rhs.loc, true, false, false);
                    }
                    verifyUnsafe(e.lhs);
                    if (e.resolvedType != null && e.resolvedType.isRawPointerType()) {
                        if (!isInUnsafe()) {
                            err("Expect unsafe", e.loc);
                        }
                    }
                    break;
                case assign:
                case assignPlus:
                case assignMinus:
                case assignStar:
                case assignSlash:
                case assignPercent:
                    boolean assignable = false;
                    if (e.lhs instanceof Expr.IdExpr idExpr) {
                        if (idExpr.name.equals(TokenKind.thisKeyword.symbol) || idExpr.name.equals(TokenKind.superKeyword.symbol)) {
                            //assignable = false;
                        }
                        else if (idExpr.resolvedDef instanceof AstNode.FieldDef f) {
                            assignable = true;
                        }
                    }
                    else if (e.lhs instanceof Expr.AccessExpr accessExpr) {
                        if (accessExpr.resolvedDef instanceof AstNode.FieldDef f) {
                            assignable = true;
                        }
                    }
                    else if (e.lhs instanceof Expr.IndexExpr indexExpr) {
                        if (indexExpr.resolvedOperator != null && indexExpr.resolvedOperator.prototype.paramDefs != null
                                && indexExpr.resolvedOperator.prototype.paramDefs.size() > 1) {
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
                            
//                            AstNode ldef = idResolvedDef(e.lhs);
//                            if (ldef instanceof FieldDef fd && e.rhs.resolvedType.detail instanceof Type.PointerInfo pinfo
//                                    && e.lhs.resolvedType.detail instanceof Type.PointerInfo lpinfo) {
//                                if (lpinfo.pointerAttr != Type.PointerAttr.raw && pinfo.pointerAttr == Type.PointerAttr.inst) {
//                                    if (!isInUnsafe()) {
//                                        err("Cannot store instant pointer", e.loc);
//                                        //e._refSafeCheck = true;
//                                    }
//                                }
//                            }
                        }
                        else {
                            if (e.lhs.resolvedType.isNum()) {
//                                if (!e.lhs.resolvedType.equals(e.rhs.resolvedType)) {
//                                    err("Type mismatch", e.loc);
//                                }
                                this.verifyTypeFit(e.rhs, e.lhs.resolvedType, e.rhs.loc);
                            }
                            else if (e.lhs.resolvedType.isRawPointerType()) {
//                                if (!e.lhs.resolvedType.equals(e.rhs.resolvedType)) {
//                                    err("Type mismatch", e.loc);
//                                }
                                this.verifyInt(e.rhs);
                            }
                            else {
                                err("Unsupport operator", e.loc);
                            }
                        }
                    }
                    else {
                        err("Not assignable", e.lhs.loc);
                    }
                    
                    if (curt != Token.TokenKind.assign && e.resolvedType != null && e.resolvedType.isRawPointerType()) {
                        if (!isInUnsafe()) {
                            err("Expect unsafe", e.loc);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}