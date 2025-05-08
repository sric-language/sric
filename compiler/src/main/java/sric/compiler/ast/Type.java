//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import sric.compiler.ast.AstNode.TypeDef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sric.compiler.ast.Expr.ClosureExpr;
import sric.compiler.ast.Expr.IdExpr;

/**
 *
 * @author yangjiandong
 */
public class Type extends AstNode {
    public IdExpr id;
    public ArrayList<Type> genericArgs = null;
    public TypeAlias resolvedAliasDef = null;
    
    public static enum PointerAttr {
        own, ref, raw
    };
    
//    public boolean explicitImmutable = false;
    public boolean isImmutable = false;
    
    public boolean isReference = false;
    
    public TypeInfo detail = null;
    
    public static abstract class TypeInfo {
    }
    
    public static class FuncInfo extends TypeInfo {
        public FuncPrototype prototype;
        public FuncDef funcDef = null;
        
        public boolean isStatic() {
            return funcDef == null || funcDef.isStatic();
        }
    }
    
    public static class PointerInfo extends TypeInfo {
        public PointerAttr pointerAttr = PointerAttr.ref;
        //** Is this is a nullable type (marked with trailing ?)
        public boolean isNullable = false;
    }
    
    public static class MetaTypeInfo extends TypeInfo {
        public Type type;
    }
    
    public static class ArrayInfo extends TypeInfo {
        public Expr sizeExpr;
        //public int size = -1;
        public int getSize() {
            if (sizeExpr == null) return -1;
            if (sizeExpr instanceof Expr.LiteralExpr li && li.value instanceof Long v) {
                return v.intValue();
            }
            if (sizeExpr instanceof IdExpr id && id.resolvedDef instanceof FieldDef f) {
                if (f.initExpr != null && f.initExpr instanceof Expr.LiteralExpr li && li.value instanceof Long v) {
                    return v.intValue();
                }
            }
            return -1;
        }
    }
    
    public static class NumInfo extends TypeInfo {
        //** primitive type sized. the Int32 size is 32
        public int size = 0;

        //unsigned int
        public boolean isUnsigned = false;
    }
    
    public Type(IdExpr id) {
        this.id = id;
        this.loc = id.loc;
    }
    
    public Type(Loc loc, String name) {
        this.loc = loc;
        this.id = new IdExpr(name);
        this.id.loc = loc;
    }
    
    public boolean isDeepImmutable() {
        if (!this.isImmutable) {
            return false;
        }
        if (this.isPointerType()) {
            if (this.genericArgs != null) {
                return this.genericArgs.get(0).isDeepImmutable();
            }
        }
        return true;
    }
    
    public boolean isVoid() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Void");
    }
    
    public boolean isGenericParamType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.genericParamTypeName);
    }
    
    public boolean isBool() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Bool");
    }
    
    public boolean isInt() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Int");
    }
    
    public boolean isNum() {
        return isInt() || isFloat();
    }
    
    public boolean isFloat() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Float");
    }
    
    public boolean isArray() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.arrayTypeName);
    }
    
    public boolean isMetaType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.metaTypeTypeName);
    }
    
    public boolean isPointerType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.pointerTypeName);
    }
    
    public boolean isNullablePointerType() {
        if (!isPointerType()) {
            return false;
        }
        if (this.detail instanceof PointerInfo pinfo) {
            return pinfo.isNullable;
        }
        return true;
    }
    
    public boolean isPromiseType() {
        return id == Type.promiseCache;
    }
    
    public boolean hasDefaultValue() {
        if (this.isPointerType()) {
            if (!this.isNullablePointerType()) {
                return false;
            }
            return true;
        }
        if (this.isReference) {
            return false;
        }
        if (this.id.resolvedDef instanceof TypeDef td) {
            if (td.isEnum()) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isEnumType() {
        if (this.id.resolvedDef instanceof TypeDef td) {
            if (td.isEnum()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isRawPointerType() {
        if (!isPointerType()) {
            return false;
        }
        if (this.detail instanceof PointerInfo pinfo) {
            return pinfo.pointerAttr == PointerAttr.raw;
        }
        return false;
    }
    
//    public boolean isRawOrInstPointerType() {
//        if (!isPointerType()) {
//            return false;
//        }
//        if (this.detail instanceof PointerInfo pinfo) {
//            return pinfo.pointerAttr == PointerAttr.raw || pinfo.pointerAttr == PointerAttr.inst;
//        }
//        return false;
//    }
    
    public boolean isOwnOrRefPointerType() {
        if (!isPointerType()) {
            return false;
        }
        if (this.detail instanceof PointerInfo pinfo) {
            return pinfo.pointerAttr == PointerAttr.own || pinfo.pointerAttr == PointerAttr.ref;
        }
        return false;
    }
    
    public boolean isNullType() {
        if (isPointerType()) {
            if (this.genericArgs == null) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isFuncType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.funcTypeName);
    }
    
    public boolean isVarArgType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.varargTypeName);
    }
    
    public boolean fit(Type target) {
        
        if (target.isVarArgType()) {
            if (this.isBool() || this.isNum() || this.isRawPointerType() || this.isNullType())
                return true;
            else
                return false;
        }

        if (this.isImmutable && !this.isNullType()) {
            if (target.isReference && !target.isImmutable) {
                return false;
            }
//            if (target.isPointerType() && target.genericArgs != null && !target.genericArgs.get(0).isImmutable) {
//                return false;
//            }
        }
        
        if (semanticEquals(target)) {
            return true;
        }
        
        if (this.isNullType() && target.detail instanceof PointerInfo a) {
            if (a.isNullable) {
                return true;
            }
            return false;
        }
        
        if (this.isNullType() && target.isFuncType()) {
            return true;
        }
        

        //pointer fit
        if (this.detail instanceof PointerInfo e && target.detail instanceof PointerInfo a) {

//            if (e.isNullable && !a.isNullable) {
//                //error to nonnullable
//                return false;
//            }

            if ((e.pointerAttr != a.pointerAttr)) {
                if (e.pointerAttr == PointerAttr.own) {
                    //ok
                }
                else if (e.pointerAttr == PointerAttr.ref && (a.pointerAttr != PointerAttr.own)) {
                    //ok
                }
                else if (a.pointerAttr == PointerAttr.raw) {
                    //ok
                }
                else {
                    return false;
                }
            }
            
            if (target.genericArgs != null && target.genericArgs.get(0).isVoid()) {
                //ok
            }
            else if (!genericArgsFit(target)) {
                return false;
            }
            

            if (this.id.resolvedDef == target.id.resolvedDef) {
                return true;
            }

            if (this.id.resolvedDef != null && target.id.resolvedDef != null) {
                if (this.id.resolvedDef instanceof TypeDef sd && target.id.resolvedDef instanceof TypeDef td) {
                    
                    if (sd.originGenericTemplate != null || td.originGenericTemplate != null) {
                        if (sd.originGenericTemplate == td.originGenericTemplate || sd == td.originGenericTemplate || sd.originGenericTemplate == td) {
                            return true;
                        }
                    }

                    if (sd.isInheriteFrom(td)) {
                        return true;
                    }
                }
            }
        }
        
        //number fit
        if (this.detail instanceof NumInfo n1 && target.detail instanceof NumInfo n2) {
            if (this.isFloat() && target.isInt()) {
                return false;
            }
            if (n1.size > n2.size) {
                return false;
            }
            return true;
        }
        return false;
    }
    
    public boolean typeEquals(Type target) {
        return equals(target, false, true);
    }
    
    public boolean strictEquals(Type target) {
        return equals(target, true, true);
    }
    
    public boolean semanticEquals(Type target) {
        return equals(target, false, false);
    }
    
    private boolean equals(Type target, boolean strict, boolean checkNumber) {
        if (this == target) {
            return true;
        }

        if (strict) {
            if (this.isImmutable != target.isImmutable) {
                return false;
            }
            if (this.isReference != target.isReference) {
                return false;
            }
        }
        
        if (strict || checkNumber) {
            if (this.resolvedAliasDef != null) {
                if (!this.resolvedAliasDef.type.equals(target, strict, checkNumber)) {
                    return false;
                }
            }
            if (target.resolvedAliasDef != null) {
                if (!this.equals(target.resolvedAliasDef.type, strict, checkNumber)) {
                    return false;
                }
            }
        }
        
//        if (checkRefable || strict) {
//            if (this.isRefable != target.isRefable) {
//                return false;
//            }
//        }
        
        if (!genericArgsEquals(target)) {
            return false;
        }
        
        if (this.isPointerType()) {
            if (this.detail instanceof PointerInfo e && target.detail instanceof PointerInfo a) {
                if ( (e.pointerAttr != a.pointerAttr) || (e.isNullable != a.isNullable))  {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else if (this.isArray()) {
            if (this.detail instanceof ArrayInfo e && target.detail instanceof ArrayInfo a) {
                if (e.sizeExpr != a.sizeExpr)  {
                    int s1 = e.getSize();
                    int s2 = a.getSize();
                    if (s1 != s2 || s1 == -1) {
                        return false;
                    }
                }
            }
            else {
                return false;
            }
        }
        else if (this.isNum()) {
            if (this.detail instanceof NumInfo e && target.detail instanceof NumInfo a) {
                if (checkNumber && (e.isUnsigned != a.isUnsigned))  {
                    return false;
                }
                else if (checkNumber && (e.size != a.size))  {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else if (this.isFuncType()) {
            if (this.detail instanceof FuncInfo e && target.detail instanceof FuncInfo a) {
                if (e.isStatic() != a.isStatic())  {
                    return false;
                }
            }
            else {
                return false;
            }
        }

        if (this.id.resolvedDef == target.id.resolvedDef) {
            return true;
        }
        
        if (this.id.resolvedDef != null && target.id.resolvedDef != null) {
            if (this.id.resolvedDef instanceof TypeDef sd && target.id.resolvedDef instanceof TypeDef td) {
                if (sd.originGenericTemplate != null && sd.originGenericTemplate == td.originGenericTemplate) {
                    return true;
                }
                if (sd == td.originGenericTemplate) {
                    return true;
                }
                if (sd.originGenericTemplate == td) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected boolean genericArgsEquals(Type target) {
        if (this.genericArgs != null || target.genericArgs != null) {
            if (this.genericArgs == null || target.genericArgs == null) {
                return false;
            }
            if (this.genericArgs.size() != target.genericArgs.size()) {
                return false;
            }
            for (int i=0; i<this.genericArgs.size(); ++i) {
                if (!this.genericArgs.get(i).strictEquals(target.genericArgs.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean genericArgsFit(Type target) {
        if (this.genericArgs != null || target.genericArgs != null) {
            if (this.genericArgs == null || target.genericArgs == null) {
                return false;
            }
            if (this.genericArgs.size() != target.genericArgs.size()) {
                return false;
            }
            for (int i=0; i<this.genericArgs.size(); ++i) {
                Type from = this.genericArgs.get(i);
                Type to = target.genericArgs.get(i);
                boolean ok = false;
                if (from.fit(to)) {
                    ok = true;
                }
                
                if (from.isImmutable && !to.isImmutable && !from.isPointerType()) {
                    return false;
                }
                
                if (from.id.resolvedDef instanceof TypeDef sd && to.id.resolvedDef instanceof TypeDef td) {
                    if (sd.originGenericTemplate != null || td.originGenericTemplate != null) {
                        if (sd.originGenericTemplate == td.originGenericTemplate || sd == td.originGenericTemplate || sd.originGenericTemplate == td) {
                            ok = true;
                        }
                    }

                    if (sd.isInheriteFrom(td)) {
                        ok = true;
                    }
                }
                if (!ok) {
                    return false;
                }
            }
        }
        return true;
    }
        
    public static Type funcType(Loc loc, FuncPrototype prototype) {
        Type type = new Type(loc, Buildin.funcTypeName);
        FuncInfo info = new FuncInfo();
        info.prototype = prototype;
        type.detail = info;
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(prototype.returnType);
        if (prototype.paramDefs != null) {
            for (FieldDef p : prototype.paramDefs) {
                type.genericArgs.add(p.fieldType);
            }
        }
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type funcType(FuncDef f) {
        Type type = funcType(f.loc, f.prototype);
        ((FuncInfo)type.detail).funcDef = f;
        return type;
    }
    
    public static Type funcType(ClosureExpr f) {
        Type type = funcType(f.loc, f.prototype);
        return type;
    }
    
    public static Type voidType(Loc loc) {
        Type type = new Type(loc, "Void");
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type genericParamType(Loc loc) {
        Type type = new Type(loc, Buildin.genericParamTypeName);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type boolType(Loc loc) {
        Type type = new Type(loc, "Bool");
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type intType(Loc loc) {
        Type type = new Type(loc, "Int");
        NumInfo info = new NumInfo();
        info.size = 32;
        type.detail = info;
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type floatType(Loc loc) {
        Type type = new Type(loc, "Float");
        NumInfo info = new NumInfo();
        info.size = 64;
        type.detail = info;
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type strType(Loc loc) {
        Type type = intType(loc);
        ((NumInfo)type.detail).size = 8;
        type.isImmutable = true;
        return pointerType(loc, type, PointerAttr.raw, false);
    }
    
    public static Type nullType(Loc loc) {
        Type type = new Type(loc, Buildin.pointerTypeName);
        type.isImmutable = true;
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }

    public static Type arrayType(Loc loc, Type elemType, Expr size) {
        Type type = new Type(loc, Buildin.arrayTypeName);
        ArrayInfo info = new ArrayInfo();
        info.sizeExpr = size;
        type.detail = info;
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(elemType);
        
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type pointerType(Loc loc, Type elemType, PointerAttr pointerAttr, boolean nullable) {
        Type type = new Type(loc, Buildin.pointerTypeName);
        if (elemType != null) {
            type.genericArgs = new ArrayList<>();
            type.genericArgs.add(elemType.toDereference());
        }
        PointerInfo info = new PointerInfo();
        info.pointerAttr = pointerAttr;
        info.isNullable = nullable;
        type.detail = info;
        
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    private static IdExpr promiseCache;
    
    public static Type promiseType(Loc loc, Type rawType) {
        if (promiseCache == null) {
            IdExpr ns = new IdExpr("sric");
            IdExpr id = new IdExpr("Promise");
            id.loc = Buildin.loc;
            id.namespace = ns;
            promiseCache = id;
        }
        
        Type type = new Type(promiseCache);
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(rawType);
        return type;
    }
    
    public static Type varArgType(Loc loc) {
        Type type = new Type(loc, Buildin.varargTypeName);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type metaType(Loc loc, Type type) {
        Type t = new Type(loc, Buildin.metaTypeTypeName);
        MetaTypeInfo info = new MetaTypeInfo();
        info.type = type;
        t.detail = info;
        if (type.id.resolvedDef == null && type.id.namespace == null) {
            type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        }
        return t;
    }
    
    //easy parser name for reflect
    public String getEasyName() {
        
        if (this.isVarArgType()) {
            return Buildin.varargTypeName;
        }
        
        StringBuilder sb = new StringBuilder();

        if (this.isReference) {
            sb.append("&");
        }
        
        if (isArray()) {
            ArrayInfo info = (ArrayInfo)this.detail;
            sb.append("[");
            if (info.sizeExpr != null) {
                sb.append(info.getSize());
            }
            sb.append("]");
        }
        else if (isNum()) {
            NumInfo info = (NumInfo)this.detail;
            if (info.isUnsigned) {
                sb.append("U");
            }

            sb.append(id.getQName());

            if (info.size != 0) {
                sb.append(info.size);
            }
        }
        else if (isPointerType()) {
            if (this.isNullType()) {
                sb.append("null");
            }
            else {
                PointerInfo info = (PointerInfo)this.detail;
                if (info.pointerAttr != PointerAttr.ref) {
                    sb.append(info.pointerAttr);
                }
                sb.append("*");
                //if (info.isNullable) sb.append("?");
                //sb.append(" ");
            }
        }
        else if (isFuncType()) {
            sb.append("fun");
        }
        else if (isMetaType()) {
            sb.append(Buildin.metaTypeTypeName);
        }
        else {
            boolean parentResolved = false;
            if (id.resolvedDef instanceof TypeDef f) {
                if (f.parent instanceof FileUnit u) {
                    sb.append(u.module.name);
                    sb.append("::");
                    sb.append(f.name);
                    parentResolved = true;
                }
            }
            if (!parentResolved) {
                sb.append(id.getQName());
            }
        }
        
        if (this.genericArgs != null) {
            sb.append("<");
            int i = 0;
            for (Type t : this.genericArgs) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(t.getEasyName());
                ++i;
            }
            sb.append(">");
        }
        
        return sb.toString();
    }
    
    public String getQName(boolean convertAlias) {
        if (!convertAlias && this.resolvedAliasDef != null) {
            return this.resolvedAliasDef.type.getQName(convertAlias);
        }
        
        if (this.isVarArgType()) {
            return Buildin.varargTypeName;
        }
        
        StringBuilder sb = new StringBuilder();

//        if (this.isRefable) {
//            sb.append("refable ");
//        }
        
        if (this.isImmutable) {
            sb.append("const ");
        }

        if (this.isReference) {
            sb.append("&");
        }
        
        if (isArray()) {
            ArrayInfo info = (ArrayInfo)this.detail;
            sb.append("[");
            if (info.sizeExpr != null) {
                sb.append(info.getSize());
            }
            sb.append("]");
            sb.append(this.genericArgs.get(0).getQName(convertAlias));
            return sb.toString();
        }
        else if (isNum()) {
            NumInfo info = (NumInfo)this.detail;
            if (info.isUnsigned) {
                sb.append("U");
            }

            sb.append(id.getQName());

            if (info.size != 0) {
                sb.append(info.size);
            }
            return sb.toString();
        }
        else if (isPointerType()) {
            if (this.isNullType()) {
                sb.append("null");
            }
            else {
                PointerInfo info = (PointerInfo)this.detail;
                if (info.pointerAttr != PointerAttr.ref) {
                    sb.append(info.pointerAttr);
                }
                sb.append("*");
                if (info.isNullable) sb.append("?");
                sb.append(" ");
                sb.append(this.genericArgs.get(0).getQName(convertAlias));
            }
            return sb.toString();
        }
        else if (isFuncType()) {
            sb.append("fun");
            sb.append(((FuncInfo)this.detail).prototype.getQName(convertAlias));
            return sb.toString();
        }
        else if (isMetaType()) {
            sb.append(((MetaTypeInfo)this.detail).type.getQName(convertAlias));
            return sb.toString();
        }
        
        boolean parentResolved = false;
        if (id.resolvedDef instanceof TypeDef f) {
            if (f.parent instanceof FileUnit u) {
                sb.append(u.module.name);
                sb.append("::");
                sb.append(f.name);
                parentResolved = true;
            }
        }
        if (!parentResolved) {
            sb.append(id.getQName());
        }
        
        if (this.genericArgs != null) {
            sb.append("$<");
            int i = 0;
            for (Type t : this.genericArgs) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(t.getQName(convertAlias));
                ++i;
            }
            sb.append(">");
        }
        
        return sb.toString();
    }

    public Type templateInstantiate(Map<GenericParamDef, Type> typeGenericArgs) {
        if (!(this.id.resolvedDef instanceof GenericParamDef g) && this.genericArgs == null) {
            return this;
        }
        
        Type nt = new Type(this.id);
        if (this.genericArgs != null) {
            nt.genericArgs = new ArrayList<Type>();
            for (int i=0; i<this.genericArgs.size(); ++i) {
                Type t = this.genericArgs.get(i).templateInstantiate(typeGenericArgs);
                nt.genericArgs.add(t);
            }
        }
        nt.isImmutable = this.isImmutable;
        nt.isReference = this.isReference;
        nt.detail = this.detail;
        if (this.id.resolvedDef instanceof GenericParamDef g) {
            if (typeGenericArgs.containsKey(g)) {
                Type at = typeGenericArgs.get(g);
                if (at != null) {
                    nt.id = at.id;
                    nt.detail = at.detail;
                    nt.genericArgs = at.genericArgs;
                    if (at.isImmutable) {
                        nt.isImmutable = true;
                    }
                }
            }
        }
        else if (this.id.resolvedDef instanceof TypeDef sd) {
            this.id.resolvedDef = sd.makeInstance(typeGenericArgs);
        }
        return nt;
    }
    
    public void copyFrom(Type that, boolean deep) {
        copyFrom(that, deep, false);
    }
    
    public void copyFrom(Type that, boolean deep, boolean merge) {
        Type type = this;
        if (deep) {
            if (that.genericArgs !=  null) {
                type.genericArgs = new ArrayList<>();
                for (int i=0; i<that.genericArgs.size(); ++i) {
                    Type t = that.genericArgs.get(i);
                    type.genericArgs.add(t);
                }
            }
            else {
                type.genericArgs = that.genericArgs;
            }
        }
        else {
            type.genericArgs = that.genericArgs;
        }
        
        type.id.resolvedDef = that.id.resolvedDef;
        type.id.resolvedType = that.id.resolvedType;
        
        type.resolvedAliasDef = that.resolvedAliasDef;
        //type.explicitImmutable = this.explicitImmutable;
        if (merge) {
            type.isImmutable |= that.isImmutable;
            type.isReference |= that.isReference;
        }
        else {
            type.isImmutable = that.isImmutable;
            type.isReference = that.isReference;
        }
        
        if (deep && that.detail instanceof PointerInfo dinfo) {
            PointerInfo info = new PointerInfo();
            info.pointerAttr = dinfo.pointerAttr;
            info.isNullable = dinfo.isNullable;
            type.detail = info;
        }
        else {
            type.detail = that.detail;
        }
    }
    
    public Type toNonNullable() {
        if (!this.isPointerType()) {
            return this;
        }
        if (!((PointerInfo)this.detail).isNullable) {
            return this;
        }
        
        Type type = new Type(loc, "*");
        type.copyFrom(this, true);

        ((PointerInfo)type.detail).isNullable = false;

        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public Type toImmutable() {
        if (this.isImmutable) {
            return this;
        }
        
        //shadow copy
        Type type = new Type(this.id);
        type.copyFrom(this, false);
        type.isImmutable = true;
        return type;
    }
    
    public Type toDereference() {
        if (!this.isReference) {
            return this;
        }
        
        //shadow copy
        Type type = new Type(this.id);
        type.copyFrom(this, false);
        type.isReference = false;
        return type;
    }
    
    public Type toMutable() {
        if (this.isImmutable == false) {
            return this;
        }
        
        //shadow copy
        Type type = new Type(this.id);
        type.copyFrom(this, false);
        type.isImmutable = false;
        return type;
    }
    
    public Type toRawPointer() {
        if (!(this.detail instanceof PointerInfo)) {
            return null;
        }
        
        //shadow copy
        Type type = new Type(this.id);
        type.copyFrom(this, true);
        ((PointerInfo)type.detail).pointerAttr = PointerAttr.raw;
        return type;
    }
}
