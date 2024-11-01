/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sric.lsp;

import java.util.ArrayList;
import java.util.HashMap;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.FieldDef;
import sric.compiler.ast.AstNode.FileUnit;
import sric.compiler.ast.Buildin;
import sric.compiler.ast.Expr;
import sric.compiler.ast.Expr.IdExpr;
import sric.compiler.ast.Scope;
import sric.compiler.ast.Type;
import sric.compiler.resolve.ErrorChecker;

/**
 *
 * @author yangjiandong
 */
public class CompletionFinder {

    private ArrayList<AstNode> defs = new ArrayList<AstNode>();
    private AstNode target;
    private String text;

    public ArrayList<AstNode> findSugs(FileUnit funit, AstNode node, String text) {
        defs.clear();
        this.target = node;
        this.text = text;
        
        
        if (this.target instanceof Expr e) {
            AstNode resolvedDef = null;
            if (e.resolvedType != null) {
                resolvedDef = e.resolvedType.id.resolvedDef;
                if (e.resolvedType.isPointerType()) {
                    if (e.resolvedType.genericArgs == null || e.resolvedType.genericArgs.size() > 0) {
                        Type type = e.resolvedType.genericArgs.get(0);
                        resolvedDef = type.id.resolvedDef;
                    }
                    else {
                        resolvedDef = null;
                    }
                }
                else if (e.resolvedType.isRefableType()) {
                    if (e.resolvedType.genericArgs == null || e.resolvedType.genericArgs.size() > 0) {
                        Type type = e.resolvedType.genericArgs.get(0);
                        resolvedDef = type.id.resolvedDef;
                    }
                    else {
                        resolvedDef = null;
                    }
                }
            }
            if (resolvedDef == null) {
                resolvedDef = ErrorChecker.idResolvedDef(e);
            }
            
            if (resolvedDef != null) {
                if (resolvedDef instanceof AstNode.TypeDef t) {
                    Scope scope = t.getScope(null);
                    addScope(scope, text);
                    if (defs.size() == 0) {
                        addScope(scope, null);
                    }
                }
            }
        }
        
        if (defs.size() > 0) {
            return defs;
        }
        
        addScope(funit.importScope, text);
        addScope(funit.module.getScope(null), text);
        addScope(Buildin.getBuildinScope(), text);
        
        return defs;
    }
    
    private void addScope(Scope scope, String prefix) {
        for (HashMap.Entry<String, ArrayList<AstNode>> entry : scope.symbolTable.entrySet()) {
            String name = entry.getKey();
            if (prefix == null || name.startsWith(prefix)) {
                for (AstNode anode : entry.getValue()) {
                    defs.add(anode);
                }
            }
        }
    }

}

