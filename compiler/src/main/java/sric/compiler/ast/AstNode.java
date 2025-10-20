//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sric.compiler.CompilerLog;
import sric.compiler.ast.Expr.IdExpr;
import sric.compiler.ast.Token.TokenKind;

/**
 *
 * @author yangjiandong
 */
public class AstNode {

    public Loc loc;
    public int len = 0;
    
    public boolean copyTo(AstNode node) {
        node.len = this.len;
        node.loc = this.loc;
        return true;
    }
        
    public interface Visitor {
        public void visit(AstNode node);
    }
    
    public void walkChildren(Visitor visitor) {
    }
    
    public static class Comment extends AstNode {
        public String content;
        public TokenKind type;
        
        public Comment(String content, TokenKind type) {
            this.content = content;
            this.type = type;
        }
    }
    
    public static class Comments extends AstNode {
        public ArrayList<Comment> comments = new ArrayList<Comment>();
        
        public String getDoc() {
            for (var c : comments) {
                if (c.type == TokenKind.docComment) {
                    return c.content;
                }
            }
            return null;
        }
    }
    
    public static abstract class TopLevelDef extends AstNode {
        public AstNode parent;
        public int flags;
        public Comments comment;
        public String name;
        
        public boolean isDeprecated() {
            if (comment != null) {
                for (var c : comment.comments) {
                    if (c.type == TokenKind.cmdComment) {
                        if (c.content.startsWith("Deprecated")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
        public boolean isNoDoc() {
            if (comment != null) {
                for (var c : comment.comments) {
                    if (c.type == TokenKind.cmdComment) {
                        if (c.content.startsWith("NoDoc")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
        public boolean isPublic() {
            if ((flags & FConst.Private) != 0) {
                return false;
            }
            if ((flags & FConst.Protected) != 0) {
                if (parent instanceof FileUnit) {
                    return false;
                }
            }
            return true;
        }
        
        public boolean isExtern() {
            if ((this.flags & FConst.ExternC) != 0 || (this.flags & FConst.Extern) != 0) {
                return true;
            }
            return false;
        }
    }
    
    public static class FieldDef extends TopLevelDef {
        public Type fieldType;        // field type
        public Expr initExpr;         // init expression or null
        public boolean uninit = false;
        public boolean unkonwInit = false;
        
        public boolean isLocalVar = false;
        public boolean isParamDef = false;
        public boolean isRefable = false;
        
        public int _enumValue = -1;
        
        public boolean isConstDef() {
            return (this.flags & FConst.Const) != 0;
        }
        
        public boolean isLocalOrParam() {
            return isLocalVar || isParamDef;
        }
        
        public boolean hasParamDefaultValue() {
            if (initExpr != null) return true;
            if (unkonwInit) return true;
            return false;
        }
        
        public FieldDef(Comments comment, String name) {
            this.comment = comment;
            this.name = name;
        }
        
        public FieldDef(String name, Type type) {
            this.fieldType = type;
            this.name = name;
        }
        
        public FieldDef templateInstantiate(Map<GenericParamDef, Type> typeGenericArgs) {
            FieldDef nf = new FieldDef(this.comment, this.name);
            nf.loc = this.loc;
            nf.len = this.len;
            nf.flags = this.flags;
            nf.parent = this.parent;
            nf.initExpr = this.initExpr;
            nf.isLocalVar = this.isLocalVar;
            nf.fieldType = this.fieldType.templateInstantiate(typeGenericArgs);
            return nf;
        }
        
        public boolean isStatic() {
            if (this.parent instanceof FileUnit) {
                return true;
            }
            if ((this.flags & FConst.Static) != 0) {
                return true;
            }
            return false;
        }
        
        public boolean isEnumField() {
            if (parent instanceof TypeDef td) {
                //already checked in ExprTypeResolver
                if (td.isEnum()) {
                    return true;
                }
            }
            return false;
        }
    }
    
    public static class TypeDef extends TopLevelDef {
        public ArrayList<Type> inheritances = null;
        public ArrayList<FieldDef> fieldDefs = new ArrayList<>();
        public ArrayList<FuncDef> funcDefs = new ArrayList<>();
        public ArrayList<GenericParamDef> generiParamDefs = null;
        public Type enumBase = null;
        
        private Scope instanceScope = null;
        private Scope staticScope = null;
        private Scope instanceInheritScopes = null;
        private Scope staticInheritScopes = null;
        public TypeDef originGenericTemplate = null;
        private Map<GenericParamDef, Type> typeGenericArgs;
        private boolean genericInited = false;
        private TypeDef genericTemplate = null;
//        private HashMap<String, StructDef> parameterizeCache;
        
        public boolean _hasCotr = false;
        public boolean _hasDecotr = false;
        
        public enum Kind {
            Struct, Enum, Tarit
        }
        public Kind kind = Kind.Struct;
        
        public TypeDef(Comments comment, int flags, String name) {
            this.comment = comment;
            this.flags = flags;
            this.name = name;
        }
        
        public boolean isSafe() {
            if (!isStruct()) {
                return false;
            }
            if ((this.flags & FConst.Unsafe) != 0) {
                return false;
            }
            if (this.isExtern()) {
                return false;
            }
            return true;
        }
        
        public boolean isDynamicReflect() {
            if (isStruct() && (flags & FConst.Reflect) != 0 && this.isPolymorphic()) {
                return true;
            }
            return false;
        }
        
        //is inherit from struct
        public boolean isConcroteInherit() {
            if (this.inheritances == null) return false;
            Type inh = this.inheritances.get(0);
            if (inh.id.resolvedDef instanceof TypeDef superSd && superSd.isStruct()) {
                return true;
            }
            return false;
        }

        public boolean isTrait() {
            return kind == Kind.Tarit;
        }
        public boolean isEnum() {
            return kind == Kind.Enum;
        }
        public boolean isStruct() {
            return kind == Kind.Struct;
        }
        public boolean isAbstract() {
            if (isTrait()) {
                return true;
            }
            if ((this.flags & FConst.Abstract) != 0) {
                return true;
            }
            return false;
        }
        public boolean isPolymorphic() {
            if (isTrait()) {
                return true;
            }
            if ((this.flags & FConst.Abstract) != 0) {
                return true;
            }
            if ((this.flags & FConst.Virtual) != 0) {
                return true;
            }
            if (this.inheritances != null && !this.inheritances.isEmpty()) {
                return true;
            }
            return false;
        }
        
        public void addSlot(AstNode node) {
            if (node instanceof FieldDef f) {
                fieldDefs.add(f);
                f.parent = this;
            }
            else if (node instanceof FuncDef f) {
                funcDefs.add(f);
                f.parent = this;
            }
        }
        
        @Override public void walkChildren(Visitor visitor) {
//            if (this.inheritances != null) {
//                for (Type ins : this.inheritances) {
//                    visitor.visit(ins);
//                }
//            }
            for (FieldDef field : fieldDefs) {
                visitor.visit(field);
            }
            for (FuncDef func : funcDefs) {
                visitor.visit(func);
            }
        }
        
        public void getAllFields(HashMap<String,FieldDef> fields) {
            for (FieldDef field : fieldDefs) {
                fields.put(field.name, field);
            }
            
            if (this.inheritances != null) {
                for (Type inh : this.inheritances) {
                    if (inh.id.resolvedDef != null) {
                        if (inh.id.resolvedDef instanceof TypeDef inhSd) {
                            inhSd.getAllFields(fields);
                        }
                    }
                }
            }
        }
        
        public Scope getInstanceScope(CompilerLog log) {
            if (instanceScope == null) {
                templateInstantiate();
                Scope scope = new Scope();
                if (this.generiParamDefs != null) {
                    for (GenericParamDef gp : this.generiParamDefs) {
                        if (!scope.put(gp.name, gp)) {
                            if (log != null) log.err("Duplicate name: " + gp.name, gp.loc);
                        }
                    }
                }
                for (FieldDef f : fieldDefs) {
                    if (!f.isStatic() && !this.isEnum() && !scope.put(f.name, f)) {
                        if (log != null) log.err("Duplicate name: " + f.name, f.loc);
                    }
                }
                for (FuncDef f : funcDefs) {
                    if (!f.isStatic() && !scope.put(f.name, f)) {
                        if (log != null) log.err("Duplicate name: " + f.name, f.loc);
                    }
                }
                instanceScope = scope;
            }
            return instanceScope;
        }
        
        public Scope getStaticScope(CompilerLog log) {
            if (staticScope == null) {
                templateInstantiate();
                staticScope = new Scope();
                for (FieldDef f : fieldDefs) {
                    if ((f.isStatic() || this.isEnum()) && !staticScope.put(f.name, f)) {
                        if (log != null) log.err("Duplicate name: " + f.name, f.loc);
                    }
                }
                for (FuncDef f : funcDefs) {
                    if (f.isStatic() && !staticScope.put(f.name, f)) {
                        if (log != null) log.err("Duplicate name: " + f.name, f.loc);
                    }
                }
            }
            return staticScope;
        }
        
        public Scope getInstanceInheriteScope() {
            if (instanceInheritScopes == null) {
                if (this.inheritances == null) {
                    return null;
                }
                Scope s = new Scope();
                for (Type inh : this.inheritances) {
                    if (inh.id.resolvedDef != null) {
                        if (inh.id.resolvedDef instanceof TypeDef inhSd) {
                            inhSd.getScopeNoPrivate(s, false);
                            Scope inhScope2 = inhSd.getInstanceInheriteScope();
                            if (inhScope2 != null) {
                                s.addOverride(inhScope2);
                            }
                        }
                    }
                }
                instanceInheritScopes = s;
            }
            return instanceInheritScopes;
        }
        
        public Scope getStaticInheriteScope() {
            if (staticInheritScopes == null) {
                if (this.inheritances == null) {
                    return null;
                }
                Scope s = new Scope();
                for (Type inh : this.inheritances) {
                    if (inh.id.resolvedDef != null) {
                        if (inh.id.resolvedDef instanceof TypeDef inhSd) {
                            inhSd.getScopeNoPrivate(s, true);
                            Scope inhScope2 = inhSd.getStaticInheriteScope();
                            if (inhScope2 != null) {
                                s.addAll(inhScope2);
                            }
                        }
                    }
                }
                staticInheritScopes = s;
            }
            return staticInheritScopes;
        }
        
        private void getScopeNoPrivate(Scope scope, boolean isStatic) {
            templateInstantiate();
            for (FieldDef f : fieldDefs) {
                if ((f.flags & FConst.Private) != 0) {
                    continue;
                }
                boolean fstatic = (f.flags & FConst.Static) !=0;
                if (fstatic) {
                    if (isStatic)
                        scope.put(f.name, f);
                }
                else {
                    if (!isStatic)
                        scope.put(f.name, f);
                }
            }
            for (FuncDef f : funcDefs) {
                if ((f.flags & FConst.Private) != 0) {
                    continue;
                }
                boolean fstatic = (f.flags & FConst.Static) !=0;
                if (fstatic) {
                    if (isStatic)
                        scope.put(f.name, f);
                }
                else {
                    if (!isStatic)
                        scope.put(f.name, f);
                }
            }
        }
        
        public TypeDef makeInstance(Map<GenericParamDef, Type> typeGenericArgs) {
            TypeDef nt = new TypeDef(this.comment, this.flags, this.name);
            nt.parent = this.parent;
            nt.loc = this.loc;
            nt.len = this.len;
            nt.genericTemplate = this;
            nt.kind = this.kind;
            
            if (this.originGenericTemplate != null) {
                nt.originGenericTemplate = this.originGenericTemplate;
            }
            else {
                nt.originGenericTemplate = this;
            }

            nt.typeGenericArgs = typeGenericArgs;
            return nt;
        }
        
        public TypeDef templateInstantiate() {
            if (this.genericTemplate == null) return this;
            if (genericInited) return this;
            genericInited = true;
            
            this.genericTemplate.templateInstantiate();
            
            for (FieldDef f : this.genericTemplate.fieldDefs) {
                this.addSlot(f.templateInstantiate(typeGenericArgs));
            }
            for (FuncDef f : this.genericTemplate.funcDefs) {
                this.addSlot(f.templateInstantiate(typeGenericArgs));
            }
            
            return this;
        }
        
        public boolean isInheriteFrom(TypeDef parent) {
            if (this.inheritances == null) {
                return false;
            }
            for (Type t : this.inheritances) {
                if (t.id.resolvedDef == parent) {
                    return true;
                }
                if (t.id.resolvedDef instanceof TypeDef sd) {
                    boolean res = sd.isInheriteFrom(parent);
                    if (res) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class FuncPrototype {
        public Type returnType;       // return type
        public ArrayList<FieldDef> paramDefs = null;   // parameter definitions
        
        public boolean _explicitImmutability = false;
        public boolean _isImmutable = false;
        public boolean _isStaticClosure = false;
        public boolean _isDConst = false;
        public FuncDef funcDef = null;
        
        public boolean isThisImmutable() {
            return _isImmutable;
        }
        
        public boolean isStaticClosure() {
            return _isStaticClosure;
        }
        
        public boolean match(FuncPrototype p) {
            if (!this.returnType.strictEquals(p.returnType)) {
                return false;
            }
            if (this._isImmutable != p._isImmutable) {
                return false;
            }
            if (this._isStaticClosure != p._isStaticClosure) {
                return false;
            }
            if (this.paramDefs == null && p.paramDefs != null) {
                return false;
            }
            if (this.paramDefs != null && p.paramDefs == null) {
                return false;
            }
            if (this.paramDefs != null && p.paramDefs != null) {
                if (this.paramDefs.size() != p.paramDefs.size()) {
                    return false;
                }
                for (int i = 0; i< this.paramDefs.size(); ++i) {
                    if (this.paramDefs.get(i).fieldType != null && p.paramDefs.get(i).fieldType != null) {
                        if (!this.paramDefs.get(i).fieldType.strictEquals(p.paramDefs.get(i).fieldType)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        
        public String getQName(boolean convertAlias) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            if (paramDefs != null) {
                int i = 0;
                for (FieldDef p : paramDefs) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(p.name);
                    sb.append(" : ");
                    sb.append(p.fieldType.getQName(convertAlias));
                    ++i;
                }
            }
            sb.append(")");
            
            if (_isDConst) {
                sb.append(" dconst ");
            }
            else if (!_isImmutable) {
                sb.append(" mut ");
            }
            if (_isStaticClosure) {
                sb.append(" static ");
            }

            if (returnType != null && !returnType.isVoid()) {
                sb.append(":");
                sb.append(returnType.getQName(convertAlias));
            }
            return sb.toString();
        }
    }
    
    public static class FuncDef extends TopLevelDef {
        public FuncPrototype prototype = new FuncPrototype();       // return type
        public Block code;            // code block
        public ArrayList<GenericParamDef> generiParamDefs = null;
        
        public boolean _useThisAsRefPtr = false;
        
        public FuncDef templateInstantiate(Map<GenericParamDef, Type> typeGenericArgs) {
//            if ("hashCode".equals(this.name)) {
//                System.out.print("DEBUG");
//            }
            
            FuncDef nf = new FuncDef();
            nf.comment = this.comment;
            nf.flags = this.flags;
            nf.loc = this.loc;
            nf.len = this.len;
            nf.name = this.name;
            nf.code = this.code;
            nf.parent = this.parent;
            nf.prototype = new FuncPrototype();
            nf.prototype.returnType = this.prototype.returnType.templateInstantiate(typeGenericArgs);
            nf.prototype._isImmutable = this.prototype._isImmutable;
            nf.prototype._isStaticClosure = this.prototype._isStaticClosure;
            nf.prototype._isDConst = this.prototype._isDConst;
            
            if (this.prototype.paramDefs != null) {
                nf.prototype.paramDefs = new ArrayList<FieldDef>();
                for (FieldDef p : this.prototype.paramDefs) {
                    FieldDef np = new FieldDef(null, p.name);
                    //np.name = p.name;
                    np.initExpr = p.initExpr;
                    np.loc = p.loc;
                    np.len = p.len;
                    np.fieldType = p.fieldType.templateInstantiate(typeGenericArgs);
                    nf.prototype.paramDefs.add(np);
                }
            }
            return nf;
        }
        
        public boolean isStatic() {
            if (this.parent instanceof FileUnit) {
                return true;
            }
            if ((this.flags & FConst.Static) != 0) {
                return true;
            }
            return false;
        }
        
        public boolean isCtor() {
            return (this.flags & FConst.Ctor) != 0;
        }
        
        public boolean isAsync() {
            return (this.flags & FConst.Async) != 0;
        }
        
        public boolean isDConst() {
            return this.prototype._isDConst;
        }
    }

    
    public static class FileUnit extends AstNode {
        public String file;
        public ArrayList<TypeDef> typeDefs = new ArrayList<TypeDef>();
        public ArrayList<FieldDef> fieldDefs = new ArrayList<FieldDef>();
        public ArrayList<FuncDef> funcDefs = new ArrayList<FuncDef>();
        public ArrayList<Import> imports = new ArrayList<Import>();
        public ArrayList<TypeAlias> typeAlias = new ArrayList<TypeAlias>();
        public SModule module;
        
        public Scope importScope = null;
        
        public FileUnit(String file) {
            this.file = file;
        }
        
        public void addDef(TopLevelDef node) {
            node.parent = this;
            if (node instanceof TypeDef) {
                typeDefs.add((TypeDef)node);
            }
            else if (node instanceof FieldDef) {
                fieldDefs.add((FieldDef)node);
            }
            else if (node instanceof FuncDef) {
                funcDefs.add((FuncDef)node);
            }
            else if (node instanceof TypeAlias) {
                typeAlias.add((TypeAlias)node);
            }
        }
        
        @Override public void walkChildren(Visitor visitor) {
            for (TypeAlias typeAlias : typeAlias) {
                visitor.visit(typeAlias);
            }
            for (TypeDef typeDef : typeDefs) {
                visitor.visit(typeDef);
            }
            for (FieldDef field : fieldDefs) {
                visitor.visit(field);
            }
            for (FuncDef func : funcDefs) {
                visitor.visit(func);
            }
        }
    }
    
    
    public static class Import extends AstNode {
        public IdExpr id;
        public boolean star = false;
    }
    
    public static class TypeAlias extends TopLevelDef {
        public Type type;
    }
    
    public static class Block extends Stmt {
        //print '{' for code generator
        public boolean _printBrace = true;
        public boolean isWithBlockExpr = false;
        
        public ArrayList<Stmt> stmts = new ArrayList<Stmt>();
        @Override public void walkChildren(Visitor visitor) {
            for (Stmt s : stmts) {
                visitor.visit(s);
            }
        }
        
        public boolean isLastReturnValue() {
            if (stmts.isEmpty()) {
                return false;
            }
            Stmt last = stmts.get(stmts.size()-1);
            if (last instanceof ReturnStmt retStmt && retStmt.expr != null) {
                return true;
            }
            else if (last instanceof UnsafeBlock unsafeStmt && unsafeStmt.block != null) {
                return unsafeStmt.block.isLastReturnValue();
            }
            else if (last instanceof IfStmt ifStmt) {
                return ifStmt.allPathReturnValue();
            }
            return false;
        }
    }
    
    public static class GenericParamDef extends AstNode {
        public String name;
        public Type bound;
    }
}
