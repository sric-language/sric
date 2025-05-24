//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.resolve;

import java.util.ArrayDeque;
import sric.compiler.ast.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Expr.*;
import sric.compiler.ast.Stmt.LocalDefStmt;
import sric.compiler.ast.Token.TokenKind;
import static sric.compiler.ast.Token.TokenKind.*;
import sric.compiler.ast.*;
import sric.compiler.ast.Type.MetaTypeInfo;
/**
 *
 * @author yangjiandong
 */
public class ExprTypeResolver extends TypeResolver {
    
    //for func param or for init
    private Scope preScope = null;
    
    private ArrayDeque<AstNode> funcs = new ArrayDeque<AstNode>();
    private ArrayDeque<AstNode> loops = new ArrayDeque<AstNode>();
    
    protected TypeDef curStruct = null;
    protected WithBlockExpr curItBlock = null;
    protected FuncDef curFunc = null;
    
    public ExprTypeResolver(CompilerLog log, SModule module) {
        super(log, module);
        this.log = log;
    }
    
    public void run() {
        module.walkChildren(this);
    }
    
    private Scope lastScope() {
        if (preScope != null) {
            return preScope;
        }
        return scopes.get(scopes.size()-1);
    }
    
    @Override
    protected void resolveId(Expr.IdExpr idExpr) {
        if (idExpr.namespace == null) {
            if (idExpr.name.equals(TokenKind.dot.symbol)) {
//                AstNode func = this.funcs.peek();
//                if (func instanceof FuncDef f) {
                    if (curItBlock == null) {
                        err("Invalid '.' call", idExpr.loc);
                        return;
                    }
                    //Type self = new Type(curItBlock.loc, curItBlock._structDef.name);
                    //self.id.resolvedDef = curItBlock._structDef;

                    idExpr.resolvedType = curItBlock.resolvedType;
                    return;
//                }
//                else {
//                    err("Use '.' out of struct", idExpr.loc);
//                }
//                return;
            }
            else if (idExpr.name.equals(TokenKind.thisKeyword.symbol) || 
                    idExpr.name.equals(TokenKind.superKeyword.symbol) ) {
                
                if (curFunc != null) {
                    if (curFunc.isStatic()) {
                        err("No this in static", idExpr.loc);
                    }
                    if (curStruct == null) {
                        err("Use super out of struct", idExpr.loc);
                        return;
                    }
                    if (idExpr.name.equals(TokenKind.superKeyword.symbol)) {
                        if (this.getCurClosure() != null) {
                            err("Cannot capture super", idExpr.loc);
                        }
                        if (curStruct.inheritances == null) {
                            err("Invalid super", idExpr.loc);
                            return;
                        }
                        else {
                            idExpr.resolvedType = Type.pointerType(idExpr.loc, curStruct.inheritances.get(0), Type.PointerAttr.raw, false);
                            idExpr.resolvedType.isImmutable = true;
                        }
                    }
                    else if (idExpr.name.equals(TokenKind.thisKeyword.symbol)) {
                        Type self = new Type(curStruct.loc, curStruct.name);
                        self.id.resolvedDef = curStruct;
                        if (curStruct.isSafe()) {
                            idExpr.resolvedType = Type.pointerType(idExpr.loc, self, Type.PointerAttr.ref, false);
                        }
                        else {
                            idExpr.resolvedType = Type.pointerType(idExpr.loc, self, Type.PointerAttr.raw, false);
                        }
                        idExpr.resolvedType.isImmutable = true;
                        if (!idExpr._isAccessExprTarget) {
                            curFunc._useThisAsRefPtr = true;
                        }
                    }
                    
                    //check closure captures
                    ClosureExpr closure = getCurClosure();
                    if (closure != null) {
                        if (closure.captures == null) {
                            closure.captures = new ArrayList<>();
                        }
                        closure.captures.add(idExpr);
                    }
                }
                else {
                    err("Use this/super out of struct", idExpr.loc);
                }
                
                return;
            }
        }
        super.resolveId(idExpr);
        
        if (curStruct != null) {
            boolean inStaticScope = false;
            AstNode func = this.funcs.peek();
            if (func instanceof FuncDef f) {
                if ((f.flags & FConst.Static) != 0) {
                    inStaticScope = true;
                }
            }
            
            if (idExpr.resolvedDef != null && idExpr.namespace == null) {
                if (idExpr.resolvedDef instanceof FieldDef f) {
                    if (!f.isStatic() && !f.isLocalOrParam()) {
                        if (inStaticScope) {
                            err("Cannot access from static scope", idExpr.loc);
                        }
                        idExpr.implicitThis = true;
                    }
                }
                else if (idExpr.resolvedDef instanceof FuncDef f) {
                    if (!f.isStatic()) {
                        if (inStaticScope) {
                            err("Cannot access from static scope", idExpr.loc);
                        }
                        idExpr.implicitThis = true;
                    }
                }
            }
        }
        
        //check closure captures
        ClosureExpr closure = getCurClosure();
        if (closure != null) {
            if (idExpr.resolvedDef != null) {
                if (idExpr.resolvedDef instanceof FieldDef f) {
                    //f.parent assigned in visitField
                    if (!f.isStatic() && f.parent != closure) {
                        if (closure.captures == null) {
                            closure.captures = new ArrayList<>();
                        }
                        closure.captures.add(idExpr);
                    }
                }
                else if (idExpr.resolvedDef instanceof FuncDef f) {
                    if (!f.isStatic()) {
                        err("Can only access instance method by 'this'", idExpr.loc);
                    }
                }
            }
        }
    }

    @Override
    public void visitUnit(FileUnit v) {
        scopes.add(v.importScope);
        Scope moduleScope = module.getScope(log);
    
        scopes.add(moduleScope);
        this.scopes.add(Buildin.getBuildinScope());
        
        v.walkChildren(this);
        
        popScope();
        popScope();
        popScope();
    }

    @Override
    public void visitField(FieldDef v) {
        
        if (v.initExpr != null) {
            if (v.initExpr instanceof Expr.WithBlockExpr initBlockExpr) {
                initBlockExpr._storeVar = v;
            }
            if (v.initExpr instanceof Expr.ArrayBlockExpr initBlockExpr) {
                initBlockExpr._storeVar = v;
            }
        }
        
        if (v.fieldType != null) {
            resolveType(v.fieldType, false);
        }
        if (v.initExpr != null) {
            this.visit(v.initExpr);
        }

        if (v.fieldType == null) {
            if (v.initExpr == null) {
                err("Miss var type", v.loc);
            }
            else {
                //Type inference
                v.fieldType = v.initExpr.resolvedType;
                if (v.fieldType != null && v.fieldType.isImmutable && !v.fieldType.isReference) {
                    v.fieldType = v.fieldType.toMutable();
                }
//                if (v.fieldType != null && v.fieldType.detail instanceof Type.PointerInfo pinfo) {
//                    if (pinfo.pointerAttr == Type.PointerAttr.inst) {
//                        v.fieldType = v.fieldType.toRawPointer();
//                    }
//                }
            }
        }
        
        if (v.isLocalVar) {
            lastScope().put(v.name, v);
        }
        
        if (v.parent == null) {
            v.parent = this.funcs.peek();
        }
        
    }
    
    private void visitFuncPrototype(AstNode.FuncPrototype prototype, Scope scope) {
        if (prototype != null && prototype.paramDefs != null) {
            AstNode parent = this.funcs.peek();
            for (AstNode.FieldDef p : prototype.paramDefs) {
                this.resolveType(p.fieldType, false);
                if (p.initExpr != null) {
                    this.visit(p.initExpr);
                }
                p.parent = parent;
                scope.put(p.name, p);
            }
        }
        
        if (prototype != null) {
            if (prototype.returnType != null && !prototype.returnType.isVoid()) {
                this.resolveType(prototype.returnType, false);
            }
        }
    }

    @Override
    public void visitFunc(FuncDef v) {
        this.curFunc = v;
        this.funcs.push(v);
        
        if ((v.flags & FConst.Ctor) != 0) {
            if (v.parent instanceof TypeDef td) {
                if (v.name.equals(newKeyword.symbol)) {
                    td._hasCotr = true;
                }
                else {
                    td._hasDecotr = true;
                }
            }
            else {
                err("Invalid Ctor", v.loc);
            }
        }
        
        if (v.generiParamDefs != null) {
            Scope scope = this.pushScope();
            for (GenericParamDef gp : v.generiParamDefs) {
                scope.put(gp.name, gp);
            }
        }
        
        preScope = new Scope();

        visitFuncPrototype(v.prototype, preScope);
        if (v.code != null) {
            this.visit(v.code);
        }
        preScope = null;
        
        if (v.generiParamDefs != null) {
            this.popScope();
        }
        
        funcs.pop();
        this.curFunc = null;
    }
    
    private boolean isIniheriReflectable(TypeDef v) {
        if (v.inheritances != null && v.inheritances.size() > 0) {
            if (v.inheritances.get(0).id.resolvedDef instanceof TypeDef superSd) {
                //auto reflectable
                if (superSd.isStruct() && v.generiParamDefs == null) {
                    if ((superSd.flags & FConst.Reflect) != 0) {
                        return true;
                    }
                    else {
                        return isIniheriReflectable(superSd);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void visitTypeDef(TypeDef v) {
        int scopeCount = 0;
        if (v.isStruct()) {
            curStruct = v;
            //auto reflectable
            if ((v.flags & FConst.Reflect) == 0 && v.generiParamDefs == null) {
                if (isIniheriReflectable(v)) {
                    v.flags |= FConst.Reflect;
                }
            }
            
            if (v.inheritances != null) {
                Scope inhScopes = v.getInstanceInheriteScope();
                this.scopes.add(inhScopes);
                ++scopeCount;
                
                Scope staticInhScopes = v.getStaticInheriteScope();
                this.scopes.add(staticInhScopes);
                ++scopeCount;
                
                for (FieldDef f : v.fieldDefs) {
                    if (inhScopes.contains(f.name)) {
                        err("Field name is already exsits"+f.name, f.loc);
                    }
                }
                
                for (FuncDef f : v.funcDefs) {
                    if ((f.flags & FConst.Static) != 0) {
                        continue;
                    }
                    if ((f.flags & FConst.Ctor) != 0) {
                        continue;
                    }
                    if (inhScopes.contains(f.name)) {
                        if ((f.flags | FConst.Override) != 0) {
                            AstNode old = inhScopes.get(f.name, v.loc, null);
                            if (old instanceof FuncDef oldF) {
                                if ((oldF.flags & FConst.Abstract) == 0 && (oldF.flags & FConst.Virtual) == 0 && (oldF.flags & FConst.Override) == 0) {
                                    err("Cannot override non-virtual method", f.loc);
                                }
                                if (!oldF.prototype.match(f.prototype)) {
                                    err("Invalide override. funtion prototype not match", f.loc);
                                }
                            }
                            else {
                                err("Invalide override", f.loc);
                            }
                        }
                        else {
                            err("Expected override keyword"+f.name, f.loc);
                        }
                    }
                }
            }
        }
        else if (v.isEnum()) {
            int enumValue = 0;
            for (FieldDef f : v.fieldDefs) {
                if (f.initExpr != null) {
                    boolean ok = false;
                    if (f.initExpr instanceof Expr.LiteralExpr li) {
                        if (li.value instanceof Long liv) {
                            enumValue = liv.intValue();
                            ok = true;
                        }
                    }
                    if (!ok) {
                        err("Enum value must int literal", v.loc);
                    }
                }
                f._enumValue = enumValue;
                ++enumValue;
            }
        }
        Scope scope = v.getInstanceScope(log);
        this.scopes.add(scope);
        ++scopeCount;
        
        Scope scope2 = v.getStaticScope(log);
        this.scopes.add(scope2);
        ++scopeCount;
        
        v.walkChildren(this);
        
        for (int i=0; i<scopeCount; ++i) {
            popScope();
        }
        curStruct = null;
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof Block bs) {
            if (preScope != null) {
                this.scopes.add(preScope);
                preScope = null;
            }
            else {
                pushScope();
            }
            bs.walkChildren(this);
            popScope();
        }
        else if (v instanceof Stmt.IfStmt ifs) {
            this.visit(ifs.condition);
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                this.visit(ifs.elseBlock);
            }
        }
        else if (v instanceof Stmt.LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof Stmt.WhileStmt whiles) {
            this.loops.push(v);
            this.visit(whiles.condition);
            this.visit(whiles.block);
            this.loops.pop();
        }
        else if (v instanceof Stmt.ForStmt fors) {
            this.loops.push(v);
            if (fors.init != null) {
                pushScope();
                
                if (fors.init instanceof Stmt.LocalDefStmt varDef) {
                    this.visit(varDef.fieldDef);
                }
                else if (fors.init instanceof Stmt.ExprStmt s) {
                    this.visit(s.expr);
                }
                else {
                    err("Unsupport for init stmt", fors.init.loc);
                }
            }
            
            if (fors.condition != null) {
                this.visit(fors.condition);
            }
            
            if (fors.update != null) {
                this.visit(fors.update);
            }
            this.visit(fors.block);
            
            if (fors.init != null) {
                this.popScope();
            }
            this.loops.pop();
        }
        else if (v instanceof Stmt.SwitchStmt switchs) {
            //avoid jump out from switchs
            ArrayDeque<AstNode> savedLoop = this.loops;
            this.loops = new ArrayDeque<AstNode>();
            
            this.visit(switchs.condition);
            
            for (Stmt.CaseBlock cb : switchs.cases) {
                this.visit(cb.caseExpr);
                this.visit(cb.block);
            }
            
            if (switchs.defaultBlock != null) {
                this.visit(switchs.defaultBlock);
            }
            this.loops = savedLoop;
        }
        else if (v instanceof Stmt.ExprStmt exprs) {
            this.visit(exprs.expr);
        }
        else if (v instanceof Stmt.JumpStmt jumps) {
            if (this.loops.size() == 0) {
                err("break, continue outside of loop", v.loc);
            }
        }
        else if (v instanceof Stmt.UnsafeBlock bs) {
            this.visit(bs.block);
        }
        else if (v instanceof Stmt.ReturnStmt rets) {
            if (rets.expr != null) {
                this.visit(rets.expr);
            }
            
            AstNode func = this.funcs.peek();
            if (func != null) {
                FuncPrototype prototype;
                if (func instanceof FuncDef f) {
                    prototype = f.prototype;
                    if (f.isAsync()) {
                        rets._isCoroutineRet = true;
                    }
                }
                else {
                    ClosureExpr f = (ClosureExpr)func;
                    prototype = f.prototype;
                }
                rets._funcReturnType = prototype.returnType;
            }
            else {
                err("Invalid return", v.loc);
            }
        }
        else {
            err("Unkown stmt:"+v, v.loc);
        }
    }
    
    private Type getSlotType(AstNode resolvedDef, boolean targetImmutable, Loc loc) {
        if (resolvedDef instanceof FieldDef f) {
            if (targetImmutable) {
                if (f.fieldType == null) {
                    return null;
                }
                return f.fieldType.toImmutable();
            }
            return f.fieldType;
        }
        else if (resolvedDef instanceof FuncDef f) {
            if (targetImmutable) {
                if (!f.prototype.isThisImmutable()) {
                    err("Mutable function: " + f.name, loc);
                }
            }
            return Type.funcType(f);
        }
        else if (resolvedDef instanceof TypeAlias f) {
            return Type.metaType(f.loc, f.type);
        }
        else if (resolvedDef instanceof GenericParamDef f) {
            Type type = new Type(f.loc, f.name);
            type.id.resolvedDef = f;
            return Type.metaType(f.loc, type);
        }
        else if (resolvedDef instanceof TypeDef f) {
            Type type = new Type(f.loc, f.name);
            type.id.resolvedDef = f;
            return Type.metaType(f.loc, type);
        }
//        else if (resolvedDef instanceof ParamDef p) {
//            return p.paramType;
//        }
        return null;
    }
    
    private AstNode resoveOnTarget(Expr target, String name, Loc loc, boolean autoDeref) {
        if (!target.isResolved()) {
            return null;
        }
        AstNode resolvedDef = target.resolvedType.id.resolvedDef;
        
        if (target.resolvedType.isPointerType() && autoDeref) {
            if (target.resolvedType.genericArgs == null || target.resolvedType.genericArgs.size() > 0) {
                Type type = target.resolvedType.genericArgs.get(0);
//                if (type.isPointerType() &&  type.genericArgs != null && type.genericArgs.size() > 0) {
//                    type = type.genericArgs.get(0);
//                }
                resolvedDef = type.id.resolvedDef;
            }
            else {
                resolvedDef = null;
            }
        }
        
        if (resolvedDef == null) {
            return null;
        }
        
        if (resolvedDef instanceof TypeAlias ta) {
            resolvedDef = ta.type.id.resolvedDef;
        }
        
        if (resolvedDef instanceof GenericParamDef t) {
            resolvedDef = t.bound.id.resolvedDef;
        }

        if (resolvedDef instanceof TypeDef t) {
            Scope scope;
            boolean isStatic = false;
            if (target.resolvedType.isMetaType()) {
                scope = t.getStaticScope(log);
                isStatic = true;
            }
            else {
                scope = t.getInstanceScope(log);
            }
            AstNode def = scope.get(name, loc, log);
            if (def == null) {
                //if (t instanceof StructDef sd) {
                    if (t.inheritances != null && !isStatic) {
                        Scope inhScopes = t.getInstanceInheriteScope();
                        def = inhScopes.get(name, loc, log);
                    }
                //}
            }
            if (def == null) {
                err("Unkown name:"+name, loc);
            }
            return def;
        }

        return null;
    }

    @Override
    public void visitExpr(Expr v) {
        if (v instanceof Expr.IdExpr e) {
            resolveId(e);
            if (e.resolvedDef != null && e.resolvedType == null) {
                boolean targetImmutable = false;
                if (e.implicitThis) {
                    AstNode func = this.funcs.peek();
                    if (func instanceof FuncDef ef) {
                        if (ef.prototype.isThisImmutable()) {
                            targetImmutable = true;
                        }
                    }
                }
                e.resolvedType = getSlotType(e.resolvedDef, targetImmutable, e.loc);
            }
        }
        else if (v instanceof Expr.AccessExpr e) {
            if (e.target instanceof IdExpr ide) {
                ide._isAccessExprTarget = true;
            }
            this.visit(e.target);
            
            e.resolvedDef = resoveOnTarget(e.target, e.name, e.loc, true);
            if (e.resolvedDef != null) {
                boolean targetImmutable = e.target.resolvedType.isImmutable;
                if (e.target.resolvedType.isPointerType() && e.target.resolvedType.genericArgs != null) {
                    targetImmutable = e.target.resolvedType.genericArgs.get(0).isImmutable;
                }
                e.resolvedType = getSlotType(e.resolvedDef, targetImmutable, e.loc);
            }
            else {
                if (e.target.resolvedType != null && e.target.resolvedType.isMetaType()) {
                    err("Cannot call method on Type", e.loc);
                }
                else {
                    err("Unknow access:"+e.name, e.loc);
                }
            }
        }
        else if (v instanceof Expr.LiteralExpr e) {
            if (e.value == null) {
                v.resolvedType = Type.nullType(e.loc);
            }
            else if (e.value instanceof Long) {
                v.resolvedType = Type.intType(e.loc);
            }
            else if (e.value instanceof Double) {
                v.resolvedType = Type.floatType(e.loc);
            }
            else if (e.value instanceof Boolean) {
                v.resolvedType = Type.boolType(e.loc);
            }
            else if (e.value instanceof String) {
                v.resolvedType = Type.strType(e.loc);
            }
            if (e.resolvedType != null) {
                e.resolvedType.isImmutable = true;
            }
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
                TokenKind curt = e.opToken;
                switch (curt) {
                    //~
                    case tilde:
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    //!
                    case bang:
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    //+, -
                    case plus:
                    case minus:
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    //*
                    case star:
                        if (!e.operand.resolvedType.isPointerType()) {
                            err("Invalid * for non pointer", e.loc);
                        }
                        else {
                            e.resolvedType = e.operand.resolvedType.genericArgs.get(0);
                        }
                        break;
                    //++, --
                    case increment:
                    case decrement:
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    //&
                    case amp: {
                        if (e.operand instanceof Expr.LiteralExpr lexpr) {
                            err("Invalid & for literal", e.loc);
                        }
                        Type elmentType = e.operand.resolvedType;
                        
                        //arrray address to raw* T;
                        if (e.operand.resolvedType.isArray()) {
                            elmentType = e.operand.resolvedType.genericArgs.get(0);
                        }
                        //safe struct
                        else if (e.operand.resolvedType.id.resolvedDef instanceof TypeDef td && td.isSafe()) {
                            e.resolvedType = Type.pointerType(e.loc, elmentType, Type.PointerAttr.ref, false);
                            e._addressOfSafeStruct = true;
                        }
                        //address of local field
                        else if (e.operand instanceof IdExpr idExpr && idExpr.resolvedDef instanceof FieldDef f && f.isLocalVar) {
                            e.resolvedType = Type.pointerType(e.loc, elmentType, Type.PointerAttr.ref, false);
                            f.isRefable = true;
                            idExpr._autoDerefRefableVar = false;
                        }
                        else if (e.operand instanceof AccessExpr aexpr) {
                            //own pointer
                            if (aexpr.target.resolvedType.isOwnOrRefPointerType()) {
                                e._addressOfField = true;
                                aexpr._addressOf = true;
                                e.resolvedType = Type.pointerType(e.loc, elmentType, Type.PointerAttr.ref, false);
                            }
                            else {
                                //local field access: a.b;
                                if (aexpr.target instanceof IdExpr idExpr && idExpr.resolvedDef instanceof FieldDef f && f.isLocalVar) {
                                    e._addressOfField = true;
                                    aexpr._addressOf = true;
                                    e.resolvedType = Type.pointerType(e.loc, elmentType, Type.PointerAttr.ref, false);
                                    f.isRefable = true;
                                    idExpr._autoDerefRefableVar = false;
                                }
                            }
                        }
                        
                        if (e.resolvedType == null) {
                            e.resolvedType = Type.pointerType(e.loc, elmentType, Type.PointerAttr.raw, false);
                        }
                    }
                        break;
                    case awaitKeyword:
                        if (e.operand.resolvedType.isPromiseType() && e.operand.resolvedType.genericArgs != null) {
                            e.resolvedType = e.operand.resolvedType.genericArgs.get(0);
                        }
                        else {
                            err("Not awaitable", e.loc);
                            e.resolvedType = e.operand.resolvedType;
                        }
                        
                        boolean isAsync = false;
                        AstNode func = this.funcs.peek();
                        if (func != null) {
                            if (func instanceof FuncDef f) {
                                if (f.isAsync()) {
                                    isAsync = true;
                                }
                            }
                        }
                        if (!isAsync) {
                            err("Expect async func", e.loc);
                        }
                        break;
                    case moveKeyword:
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    case newKeyword: {
                        if (e.operand.resolvedType.detail instanceof Type.MetaTypeInfo typeInfo) {
                            e.resolvedType = Type.pointerType(e.loc, typeInfo.type, Type.PointerAttr.own, false);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        else if (v instanceof Expr.TypeExpr e) {
            this.resolveType(e.type, true);
            e.resolvedType = Type.metaType(e.loc, e.type);
        }
        else if (v instanceof Expr.IndexExpr e) {
            this.visit(e.target);
            this.visit(e.index);
            
            if (e.target.isResolved()) {
                if (e.target.resolvedType.isArray() && e.target.resolvedType.genericArgs != null) {
                    e.resolvedType = e.target.resolvedType.genericArgs.get(0);
                }
                else if (e.target.resolvedType.isRawPointerType()) {
                    if (e.target.resolvedType.genericArgs != null) {
                        e.resolvedType = e.target.resolvedType.genericArgs.get(0);
                    }
                    else {
                        err("Unknow operator []", e.loc);
                    }
                }
                else {
                    String operatorName = e.inLeftSide ? Buildin.setOperator : Buildin.getOperator;
                    AstNode rdef = resoveOnTarget(e.target, operatorName, e.loc, false);
                    if (rdef == null) {
                        err("Unknow operator []", e.loc);
                    }
                    else if (rdef instanceof FuncDef f) {
                        if ((f.flags & FConst.Operator) == 0) {
                            err("Expected operator", e.loc);
                        }
                        e.resolvedOperator = f;
                        e.resolvedType = f.prototype.returnType;
                    }
                    else {
                        err("Invalid operator []", e.loc);
                    }
                }
            }
        }
        else if (v instanceof Expr.GenericInstance e) {

            resolveGenericInstance(e);
        }
        else if (v instanceof Expr.IfExpr e) {
            this.visit(e.condition);
            this.visit(e.trueExpr);
            this.visit(e.falseExpr);
            e.resolvedType = e.trueExpr.resolvedType;
        }
        else if (v instanceof Expr.WithBlockExpr e) {
            resolveWithBlockExpr(e);
        }
        else if (v instanceof Expr.ArrayBlockExpr e) {
            resolveArrayBlockExpr(e);
        }
        else if (v instanceof ClosureExpr e) {
            this.funcs.push(v);

//            for (Expr t : e.captures) {
//                this.visit(t);
//            }
            
            preScope = new Scope();
            
            visitFuncPrototype(e.prototype, preScope);
            this.visit(e.code);
            
            preScope = null;
            this.funcs.pop();
            
            if (e.captures == null) {
                e.prototype.postFlags |= FConst.Const;
                e.prototype.postFlags |= FConst.Static;
            }
            
            e.resolvedType = Type.funcType(e);
        }
//        else if (v instanceof NonNullableExpr e) {
//            this.visit(e.operand);
//            boolean ok = false;
//            if (e.operand.resolvedType != null) {
//                if (e.operand.resolvedType.detail instanceof Type.PointerInfo pt) {
//                    if (pt.isNullable) {
//                        e.resolvedType = e.operand.resolvedType.toNonNullable();
//                        ok = true;
//                    }
//                }
//            }
//            if (!ok) {
//                err("Invalid non-nullable", e.operand.loc);
//            }
//        }
        else {
            err("Unkown expr type:"+v, v.loc);
            return;
        }
        
        if (v.resolvedType == null && !hasError()) {
            err("Resolved fail", v.loc);
        }
    }
    
    private ClosureExpr getCurClosure() {
        AstNode f = this.funcs.peek();
        if (f != null && f instanceof ClosureExpr ce) {
            return ce;
        }
        return null;
    }
    
    private TypeDef getTypeStructDef(Type type) {
        if (type.isPointerType() && type.genericArgs != null) {
            type = type.genericArgs.get(0);
        }
        if (type.id.resolvedDef instanceof TypeDef sd && (sd.isStruct() || sd.isTrait())) {
            return sd;
        }
        return null;
    }

    private void resolveWithBlockExpr(Expr.WithBlockExpr e) {
        this.visit(e.target);
        if (!e.target.isResolved()) {
            return;
        }
        
        TypeDef sd = null;
        if (e.target instanceof IdExpr id) {
            if (id.resolvedDef instanceof TypeDef sd0) {
                sd = sd0;
                //e._isType = true;
            }
            else if (id.resolvedDef instanceof FieldDef fd) {
                if (fd.fieldType.id.resolvedDef instanceof TypeDef fieldSF) {
                    sd = fieldSF;
                }
            }
            else {
                sd = getTypeStructDef(e.target.resolvedType);
            }
        }
        else if (e.target instanceof GenericInstance gi) {
            if (gi.resolvedDef instanceof TypeDef) {
                sd = (TypeDef)gi.resolvedDef;
                //e._isType = true;
            }
        }
        else if (e.target instanceof CallExpr call) {
            sd = getTypeStructDef(e.target.resolvedType);
        }
        else if (e.target instanceof UnaryExpr ue) {
            if (ue.opToken == newKeyword) {
                if (ue.operand.resolvedType.detail instanceof Type.MetaTypeInfo typeInfo) {
                    sd = getTypeStructDef(typeInfo.type);
                }
            }
        }
        else if (e.target instanceof TypeExpr te) {
            if (te.type.id.resolvedDef instanceof TypeDef) {
                sd = (TypeDef)te.type.id.resolvedDef;
                //e._isType = true;
            }
            //e._isType = true;
        }
        
        e._structDef = sd;
        e._isType = e.target.resolvedType.isMetaType();

        if (sd != null) {
            if (e.target.resolvedType.detail instanceof MetaTypeInfo mt) {
                e.resolvedType = mt.type;
            }
            else {
                e.resolvedType = e.target.resolvedType;
            }
        }
        
        if (e.block != null && sd != null) {
            WithBlockExpr savedCurItBlock = curItBlock;
            curItBlock = e;
            
            //avoid jump out from with block
            ArrayDeque<AstNode> savedLoop = this.loops;
            this.loops = new ArrayDeque<AstNode>();
            
            this.visit(e.block);

            curItBlock = savedCurItBlock;
            this.loops = savedLoop;
        }
        
        if (e.resolvedType == null) {
            err("Unknow target of with block ", e.loc);
        }
    }
    
    private void resolveArrayBlockExpr(Expr.ArrayBlockExpr e) {
        this.resolveType(e.type, true);
        if (!e.type.id.isResolved()) {
            return;
        }
        
        if (e.type.detail instanceof Type.ArrayInfo at) {
            at.sizeExpr = new LiteralExpr(Long.valueOf(e.args.size()));
            at.sizeExpr.loc = e.loc;
            //at.size = e.args.size();

            e.resolvedType = e.type;
        }
        else {
            err("Invalid array", e.loc);
            return;
        }
        
        if (e.args != null) {
            for (Expr t : e.args) {
                this.visit(t);
            }
        }

    }
    
    private void resolveGenericInstance(Expr.GenericInstance e) {
        this.visit(e.target);
        for (Type t : e.genericArgs) {
            this.resolveType(t, false);
        }
        if (!e.target.isResolved()) {
            return;
        }
        
        IdExpr idExpr;
        if (e.target instanceof IdExpr) {
            idExpr = (IdExpr)e.target;
        }
        else {
            err("Unexpected generic args", e.loc);
            return;
        }
        
        if (e.genericArgs != null) {
            boolean genericOk = false;
            if (idExpr.resolvedDef instanceof TypeDef sd) {
                if (sd.generiParamDefs != null) {
                    if (e.genericArgs.size() == sd.generiParamDefs.size()) {
                        Map<GenericParamDef, Type> typeGenericArgs = new HashMap<>();
                        for (int i=0; i<e.genericArgs.size(); ++i) {
                            typeGenericArgs.put(sd.generiParamDefs.get(i), e.genericArgs.get(i));
                        }
                        e.resolvedDef = sd.makeInstance(typeGenericArgs).templateInstantiate();
                        Type type = new Type(e.loc, sd.name);
                        type.genericArgs = e.genericArgs;
                        type.id.resolvedDef = e.resolvedDef;
                        e.resolvedType = Type.metaType(e.loc, type);;
                        genericOk = true;
                    }
                }
            }
            else if (idExpr.resolvedDef instanceof FuncDef sd) {
                if (sd.generiParamDefs != null) {
                    if (e.genericArgs.size() == sd.generiParamDefs.size()) {
                        Map<GenericParamDef, Type> typeGenericArgs = new HashMap<>();
                        for (int i=0; i<e.genericArgs.size(); ++i) {
                            typeGenericArgs.put(sd.generiParamDefs.get(i), e.genericArgs.get(i));
                        }
                        e.resolvedDef = sd.templateInstantiate(typeGenericArgs);
                        e.resolvedType = getSlotType(e.resolvedDef, false, e.loc);
                        genericOk = true;
                    }
                }
            }
            if (!genericOk) {
                err("Generic args size not match", e.loc);
            }
        }
        else if (idExpr.resolvedDef instanceof TypeDef sd) {
            if (sd.generiParamDefs != null) {
                err("Miss generic args", idExpr.loc);
            }
        }
        else if (idExpr.resolvedDef instanceof FuncDef sd) {
            if (sd.generiParamDefs != null) {
                err("Miss generic args", idExpr.loc);
            }
        }
    }

    private void resolveCallExpr(Expr.CallExpr e) {
        this.visit(e.target);
        if (e.args != null) {
            for (Expr.CallArg t : e.args) {
                this.visit(t.argExpr);
            }
        }
        
        if (e.target.isResolved()) {
            if (e.target.resolvedType.detail instanceof Type.FuncInfo f) {
                //infer generic type
                if (f.funcDef != null && f.funcDef.generiParamDefs != null && f.funcDef.generiParamDefs.size() == 1) {
                    if (f.funcDef.prototype.paramDefs != null && f.funcDef.prototype.paramDefs.size() > 0 &&
                            e.args != null && e.args.size() > 0 && e.args.get(0).argExpr.resolvedType != null) {
                        Map<GenericParamDef, Type> typeGenericArgs = new HashMap<>();
                        
                        Type exprType = e.args.get(0).argExpr.resolvedType;
                        Type paramType = f.funcDef.prototype.paramDefs.get(0).fieldType;
                        if (paramType.isPointerType() && paramType.genericArgs != null && paramType.genericArgs.size() > 0 && 
                                exprType.isPointerType() && exprType.genericArgs != null && exprType.genericArgs.size() > 0) {
                            paramType = paramType.genericArgs.get(0);
                            exprType = exprType.genericArgs.get(0);
                        }
                        
                        if (paramType.id.resolvedDef == f.funcDef.generiParamDefs.get(0)) {
                            typeGenericArgs.put(f.funcDef.generiParamDefs.get(0), exprType);
                        }

                        FuncDef nf = f.funcDef.templateInstantiate(typeGenericArgs);
                        if (e.target instanceof IdExpr ie) {
                            ie.resolvedDef = nf;
                            ie.resolvedType = getSlotType(nf, false, e.loc);
                        }
                        e.resolvedType = nf.prototype.returnType;
                    }
                }
                
                if (e.resolvedType == null) {
                    e.resolvedType = f.prototype.returnType;
                }
                
                if (e.resolvedType != null && f.funcDef != null) {
                    if (f.funcDef.isAsync()) {
                        e.resolvedType = Type.promiseType(e.resolvedType.loc, e.resolvedType);
                        if (!e.resolvedType.id.isResolved()) {
                            this.resolveType(e.resolvedType, false);
                        }
                    }
                }
            }
            else {
                boolean report = false;
                if (e.target instanceof UnaryExpr ue) {
                    if (ue.opToken == TokenKind.newKeyword) {
                        err("Unxpected '()' after new T", e.loc);
                        report = true;
                    }
                }
                if (!report) {
                    err("Invalid call target", e.loc);
                }
            }
        }
    }
    
    private void resolveBinaryExpr(Expr.BinaryExpr e) {

        this.visit(e.lhs);
        this.visit(e.rhs);
        
        if (e.lhs.isResolved() && e.rhs.isResolved()) {
            TokenKind curt = e.opToken;
            switch (curt) {
                case isKeyword:
                    e.resolvedType = Type.boolType(e.loc);
                    break;
                case asKeyword:
                    if (e.rhs instanceof TypeExpr te) {
                        Type from = e.lhs.resolvedType;
                        Type to = te.type;
                        if (from.detail instanceof Type.PointerInfo p1 && to.detail instanceof Type.PointerInfo p2) {
                            if (p1.pointerAttr != Type.PointerAttr.raw && p2.pointerAttr == Type.PointerAttr.raw) {
                                e.lhs.implicitTypeConvertTo = Type.pointerType(e.lhs.loc, from.genericArgs.get(0), p2.pointerAttr, p2.isNullable);
                                e.lhs.isPointerConvert = true;
                            }
//                            else if ((p1.pointerAttr == Type.PointerAttr.own || p1.pointerAttr == Type.PointerAttr.ref) && p2.pointerAttr == Type.PointerAttr.inst) {
//                                e.lhs.implicitTypeConvertTo = Type.pointerType(e.lhs.loc, from.genericArgs.get(0), p2.pointerAttr, p2.isNullable);
//                                e.lhs.isPointerConvert = true;
//                            }
                            else if (p1.pointerAttr == Type.PointerAttr.own && p2.pointerAttr == Type.PointerAttr.ref) {
                                e.lhs.implicitTypeConvertTo = Type.pointerType(e.lhs.loc, from.genericArgs.get(0), p2.pointerAttr, p2.isNullable);
                                e.lhs.isPointerConvert = true;
                            }
                            else if (p1.pointerAttr != p2.pointerAttr) {
                                err("Unknow convert", e.loc);
                            }
                        }
                        e.resolvedType = to;
                    }
                    break;
                case eq:
                case notEq:
//                case same:
//                case notSame:
                case lt:
                case gt:
                case ltEq:
                case gtEq:
                    if (curt == eq || curt == notEq) {
                        if (e.lhs.resolvedType.isFuncType() && e.rhs.resolvedType.isNullType()) {
                            e.resolvedType = Type.boolType(e.loc);
                            break;
                        }
                    }
                    if (e.lhs.resolvedType.isNum() && e.rhs.resolvedType.isNum()) {
                        //OK
                    }
                    else if (e.lhs.resolvedType.isBool() && e.rhs.resolvedType.isBool()) {
                        //OK
                    }
                    else if (e.lhs.resolvedType.isPointerType() && e.rhs.resolvedType.isPointerType()) {
                        //OK
                    }
                    else if (e.lhs.resolvedType.isEnumType() && e.rhs.resolvedType.isEnumType()) {
                        //OK
                    }
                    else {
                        resolveMathOperator(TokenKind.cmp, e);
                    }
                    e.resolvedType = Type.boolType(e.loc);
                    break;
                case doubleAmp:
                case doublePipe:
                    e.resolvedType = Type.boolType(e.loc);
                    break;
                case leftShift:
                case rightShift:
                case pipe:
                case caret:
                case amp:
                case percent:
                    e.resolvedType = Type.intType(e.loc);
                    break;
                case plus:
                case minus:
                case star:
                case slash:
                    Type lt = e.lhs.resolvedType;
                    Type rt = e.rhs.resolvedType;
                    //pointer arithmetic: +,-
                    if ((curt == plus || curt == minus)) {
                        if (lt.isRawPointerType() && rt.isInt()) {
                            e.resolvedType = lt;
                        }
                        else if (rt.isRawPointerType() && lt.isInt()) {
                            e.resolvedType = rt;
                        }
                        else {
                            resolveMathOperator(curt, e);
                        }
                    }
                    else {
                        resolveMathOperator(curt, e);
                    }
                    break;
                case assign:
                case assignPlus:
                case assignMinus:
                case assignStar:
                case assignSlash:
                case assignPercent:
                    if (e.lhs.resolvedType.isNum() && e.rhs.resolvedType.isNum()) {
                        //ok
                    }
                    else if (e.lhs.resolvedType.isRawPointerType() && e.rhs.resolvedType.isNum()) {
                        //ok
                    }
                    else {
                        if (curt != TokenKind.assign) {
                            err("Unsupport operator:"+curt, e.loc);
                        }
                    }
                    
                    //assinable closure captures
                    ClosureExpr closure = getCurClosure();
                    if (closure != null) {
                        if (e.lhs instanceof Expr.IdExpr idExpr && idExpr.resolvedDef != null) {
                            if (idExpr.resolvedDef instanceof FieldDef f) {
                                if (f.isLocalOrParam() && f.parent != closure) {
                                    err("Not assignable", e.lhs.loc);
                                }
                            }
                        }
                    }
                    
                    e.resolvedType = e.lhs.resolvedType;

                    break;
                default:
                    break;
            }
        }
    }

    private void resolveMathOperator(TokenKind curt, Expr.BinaryExpr e) {
        String operatorName = Buildin.operatorToName(curt);
        if (operatorName == null) {
            err("Unknow operator:"+curt, e.loc);
        }
        
        Type lt = e.lhs.resolvedType;
        Type rt = e.rhs.resolvedType;
        
        if (lt.id.resolvedDef instanceof GenericParamDef gd) {
            lt = gd.bound;
        }
        if (rt.id.resolvedDef instanceof GenericParamDef gd) {
            rt = gd.bound;
        }
        
        if (lt.isNum() && rt.isNum()) {
            if (curt != TokenKind.cmp && lt.detail != null && rt.detail != null) {
                Type.NumInfo li = (Type.NumInfo)lt.detail;
                Type.NumInfo ri = (Type.NumInfo)rt.detail;
                if (lt.isFloat()) {
                    e.resolvedType = lt;
                    if (rt.isFloat() && ri.size > li.size) {
                        e.resolvedType = rt;
                    }
                }
                else if (rt.isFloat()) {
                    e.resolvedType = rt;
                }
                else {
                    e.resolvedType = lt;
                    if (ri.size > li.size) {
                        e.resolvedType = rt;
                    }
                }
            }
            return;
        }
        
        AstNode rdef = resoveOnTarget(e.lhs, operatorName, e.loc, false);
        if (rdef == null) {
            err("Unknow operator:"+curt, e.loc);
        }
        else if (rdef instanceof FuncDef f) {
            if ((f.flags & FConst.Operator) == 0) {
                err("Expected operator", e.loc);
            }
            e.resolvedType = f.prototype.returnType;
            e.resolvedOperator = f;
        }
        else {
            err("Invalid operator:"+curt, e.loc);
        }
    }

}
