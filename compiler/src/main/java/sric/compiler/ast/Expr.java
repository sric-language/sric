//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import sric.compiler.ast.AstNode.Block;
import sric.compiler.ast.Token.TokenKind;
import java.util.ArrayList;

/**
 *
 * @author yangjiandong
 */
public abstract class Expr extends AstNode {

    public Type resolvedType;
    public boolean inLeftSide = false;
    public boolean isStmt = false;
    public boolean isPointerConvert = false;
    public Type implicitTypeConvertTo = null;
    public boolean implicitStringConvert = false;
    public boolean checkNonnullable = false;
    public boolean implicitMove = false;
    public boolean forcedMutable = false;
//    public boolean implicitDereference = false;
//    public boolean implicitGetAddress = false;
//    public boolean isAwaitTarget = false;
    public boolean isTopExpr = false;
        
    public boolean isResolved() {
        return resolvedType != null;
    }
    
    public static class UnaryExpr extends Expr {
        public Token.TokenKind opToken;   // operator token type (Token.bang, etc)
        public Expr operand;    // operand expression
        
        public boolean _addressOfField = false;
        public boolean _addressOfSafeStruct = false;
        
        public UnaryExpr(TokenKind tok, Expr operand) {
            this.opToken = tok;
            this.operand = operand;
        }
    }
    
    public static class BinaryExpr extends Expr {
        public Token.TokenKind opToken;      // operator token type (Token.and, etc)
        public Expr lhs;           // left hand side
        public Expr rhs;           // right hand side
        
        public FuncDef resolvedOperator = null;// resolved opertor name
//        public boolean _refSafeCheck = false;
        
        public BinaryExpr(Expr lhs, TokenKind tok, Expr rhs) {
            this.lhs = lhs;
            this.opToken = tok;
            this.rhs = rhs;
        }
        
        public BinaryExpr() {
            
        }
    }
    
//    public static class NonNullableExpr extends Expr {
//        public Expr operand;    // operand expression
//        
//        public NonNullableExpr(Expr operand) {
//            this.operand = operand;
//        }
//    }
    
    /**
     * wrap type for sizeof(t) or 'epxr is/as T'
     */
    public static class TypeExpr extends Expr {
        public Type type;           // right hand side
        
        public TypeExpr(Type type) {
            this.type = type;
        }
    }
    
    public static class IndexExpr extends Expr {
        public Expr target;
        public Expr index;
        public AstNode.FuncDef resolvedOperator;
    }
    
    public static class GenericInstance extends Expr {
        public Expr target;
        public AstNode resolvedDef;
        public ArrayList<Type> genericArgs = new ArrayList<Type>();
    }
    
    public static class CallExpr extends Expr {
        public Expr target;
        public ArrayList<CallArg> args = null;
    }
    
    public static class CallArg extends AstNode {
        public String name;
        public Expr argExpr;
        
        public CallArg() {
        }
        
        public CallArg(Expr argExpr) {
            this.argExpr = argExpr;
            this.loc = argExpr.loc;
            this.len = argExpr.len;
        }
    }

    
    public static class IdExpr extends Expr {
        public IdExpr namespace;
        public String name;
        
        public AstNode resolvedDef;
        public boolean implicitThis;
        
        public boolean _autoDerefRefableVar = true;
        public boolean _isAccessExprTarget = false;
        
        public IdExpr(String name) {
            this.name = name;
        }
        
        public void setNamespace(IdExpr ns) {
            if (namespace == null) {
                namespace = ns;
            }
            else {
                namespace.setNamespace(ns);
            }
        }
        
        private String getNamespaceName() {
            if (this.namespace != null) {
                return namespace.getQName();
            }
            
            if (this.resolvedDef instanceof TopLevelDef f) {
                if (f.parent instanceof FileUnit u) {
                    return u.module.name;
                }
            }
            return null;
        }
        
        public String getQName() {
            String ns = getNamespaceName();
            if (ns != null) {
                return ns + "::" + name;
            }
            return name;
        }
    }
    
    public static class AccessExpr extends Expr {
        public Expr target;
        public String name;
        public AstNode resolvedDef;
        public Token.TokenKind opToken;
        
        public boolean _addressOf = false;
    }
    
    public static class IfExpr extends Expr {
        public Expr condition;     // boolean test
        public Expr trueExpr;      // result of expression if condition is true
        public Expr falseExpr;     // result of expression if condition is false
    }
    
    public static class ArrayBlockExpr extends Expr {
        public Type type;
        public ArrayList<Expr> args = null;
        public FieldDef _storeVar;
    }
    
    public static class WithBlockExpr extends Expr {
        public Expr target;
        public Block block;
        
        public FieldDef _storeVar;
        public TypeDef _structDef;
        public boolean _isType = false;
        public boolean _isConstruction = false;
    }
    
    public static class LiteralExpr extends Expr {
        public Object value;
        public Type nullPtrType;
        
        public LiteralExpr(Object value) {
            this.value = value;
        }
    }
    
    public static class ClosureExpr extends Expr {
        public FuncPrototype prototype = new FuncPrototype();// function signature
        public Block code;             // code block
        public ArrayList<IdExpr> captures;
    }

}
