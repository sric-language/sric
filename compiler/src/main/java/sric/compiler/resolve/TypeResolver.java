//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
import sric.compiler.Compiler;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Expr.IdExpr;
import sric.compiler.ast.SModule.Depend;
import sric.compiler.ast.*;
/**
 *
 * @author yangjiandong
 */
public abstract class TypeResolver  extends CompilePass {
    
    protected ArrayList<Scope> scopes = new ArrayList<>();
    protected SModule module;
    
    
    public TypeResolver(CompilerLog log, SModule module) {
        super(log);
        this.module = module;
        this.log = log;
    }
        
    protected Scope pushScope() {
        Scope s = new Scope();
        scopes.add(s);
        return s;
    }
    
    protected Scope popScope() {
        return scopes.remove(scopes.size()-1);
    }
    
    private AstNode findSymbol(String name, Loc loc) {
        for (int i = scopes.size()-1; i >=0; --i) {
            Scope scope = scopes.get(i);
            AstNode node = scope.get(name, loc, log);
            if (node != null) {
                return node;
            }
        }
        err("Unknow symbol "+name, loc);
        return null;
    }
    
    protected void resolveId(Expr.IdExpr idExpr) {
        if (idExpr.namespace == null) {
            idExpr.resolvedDef = findSymbol(idExpr.name, idExpr.loc);
            return;
        }
        resolveId(idExpr.namespace);
        if (idExpr.namespace.resolvedDef == null) {
            return;
        }
        if (idExpr.namespace.resolvedDef instanceof SModule m) {
            AstNode node = m.getScope(log).get(idExpr.name, idExpr.loc, log);
            if (node == null) {
                err("Unknow symbol "+idExpr.name, idExpr.loc);
            }
            idExpr.resolvedDef = node;
            return;
        }
        else if (idExpr.namespace.resolvedDef instanceof TypeDef m) {
            AstNode node = m.getStaticScope(log).get(idExpr.name, idExpr.loc, log);
            if (node == null) {
                err("Unknow symbol "+idExpr.name, idExpr.loc);
            }
            idExpr.resolvedDef = node;
            return;
        }
        else {
            err("Unsupport :: for "+idExpr.namespace.name, idExpr.loc);
        }
    }
    
    protected void resolveTopLevelType(Type type, Loc loc) {
        if (type == null) {
            err("Type inference not support for top level node", loc);
            return;
        }
        resolveType(type, false);
    }

    protected void resolveType(Type type, boolean asExpr) {
        resolveId(type.id);
        if (type.id.resolvedDef != null) {
            if (type.id.resolvedDef instanceof GenericParamDef gpd) {
                if (asExpr) {
                    type.id.resolvedType = Type.metaType(type.loc, type);
                }
                //type.resolvedAlias = gpd.bound;
            }
            else if (type.id.resolvedDef instanceof TypeAlias ta) {
                if (ta.type.id.resolvedDef == null) {
                    resolveTopLevelType(ta.type, ta.loc);
                }
                type.copyFrom(ta.type, true, true);
                type.id.namespace = ta.type.id.namespace;
                type.id.name = ta.type.id.name;
                type.resolvedAliasDef = ta;
            }
            else if (type.id.resolvedDef instanceof TypeDef) {
                //ok
                if (asExpr) {
                    type.id.resolvedType = Type.metaType(type.loc, type);
                }
            }
            else {
                type.id.resolvedDef = null;
                err("It's not a type: "+type.id.name, type.loc);
            }
        }
        else {
            return;
        }
        
        if (type.isFuncType()) {
            for (int i=0; i<type.genericArgs.size(); ++i) {
                resolveType(type.genericArgs.get(i), asExpr);
            }
            return;
        }
        
        if (type.detail instanceof Type.ArrayInfo ainfo) {
            if (ainfo.sizeExpr != null) {
                this.visit(ainfo.sizeExpr);
                boolean isConstexpr = false;
                if (ainfo.sizeExpr instanceof Expr.LiteralExpr lexpr) {
                    if (lexpr.value instanceof Long) {
                        //ok
                    }
                    else {
                        err("Array size must be int", ainfo.sizeExpr.loc);
                    }
                    isConstexpr = true;
                }
                else if (ainfo.sizeExpr instanceof Expr.IdExpr idexpr) {
                    if (idexpr.resolvedDef instanceof FieldDef fd) {
                        if ((fd.flags & FConst.ConstExpr) != 0) {
                            isConstexpr = true;
                        }
                    }
                }

                if (!isConstexpr) {
                    err("Array size must be constexpr", ainfo.sizeExpr.loc);
                }
            }
        }
        
        if (type.genericArgs != null) {
            for (int i=0; i<type.genericArgs.size(); ++i) {
                resolveType(type.genericArgs.get(i), asExpr);
            }

            boolean genericOk = false;
            if (type.id.resolvedDef instanceof TypeDef sd) {
                if (sd.generiParamDefs != null) {
                    if (type.genericArgs.size() == sd.generiParamDefs.size()) {
                        Map<GenericParamDef, Type> typeGenericArgs = new HashMap<>();
                        for (int i=0; i<type.genericArgs.size(); ++i) {
                            typeGenericArgs.put(sd.generiParamDefs.get(i), type.genericArgs.get(i));
                        }
                        type.id.resolvedDef = sd.makeInstance(typeGenericArgs);
                        genericOk = true;
                    }
                }
            }
            if (!genericOk) {
                err("Generic args mismatch", type.loc);
            }
            
            if (type.detail instanceof Type.PointerInfo pinfo && pinfo.pointerAttr == Type.PointerAttr.uniq) {
                if (type.genericArgs.get(0).isVoid()) {
                    err("Unsupport uniq* Void", type.loc);
                }
            }
        }
        else if (type.id.resolvedDef instanceof TypeDef sd) {
            if (sd.generiParamDefs != null) {
                err("Miss generic args", type.loc);
            }
        }

    }
}