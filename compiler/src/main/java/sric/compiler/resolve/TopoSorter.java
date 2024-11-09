//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.resolve;

import java.util.HashMap;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.FConst;
import sric.compiler.ast.SModule;
import sric.compiler.ast.Type;

/**
 *
 * @author yangjiandong
 */
public class TopoSorter {
    private HashMap<AstNode.TypeDef, Integer> emitState = new HashMap<>();
    CompilerLog log;
    private SModule module;
    
    public void visitTypeDef(AstNode.TypeDef v) {
        if ((v.flags & FConst.ExternC) != 0 || (v.flags & FConst.Extern) != 0) {
            return;
        }

        //Topo sort
        if (this.emitState.get(v) != null) {
            int state = this.emitState.get(v);
            if (state == 2) {
                return;
            }
            log.err("Cyclic dependency", v.loc);
            return;
        }
        this.emitState.put(v, 1);
        //if (v instanceof AstNode.StructDef sd) {
        if (v.inheritances != null) {
            for (Type t : v.inheritances) {
                if (t.id.resolvedDef != null && t.id.resolvedDef instanceof AstNode.TypeDef td) {
                    if (td.parent != null && ((AstNode.FileUnit)td.parent).module == this.module) {
                        //if (td instanceof AstNode.StructDef tds) {
                            if (td.originGenericTemplate != null) {
                                this.visitTypeDef(td.originGenericTemplate);
                                continue;
                            }
                        //}
                        this.visitTypeDef(td);
                    }
                }
            }
        }
        for (AstNode.FieldDef f : v.fieldDefs) {
            if (!f.fieldType.isPointerType() && f.fieldType.id.resolvedDef != null && f.fieldType.id.resolvedDef instanceof AstNode.TypeDef td) {
                if (td.parent != null && td.parent instanceof AstNode.FileUnit unit) {
                    if (unit.module == this.module) {
                        //if (td instanceof AstNode.StructDef tds) {
                            if (td.originGenericTemplate != null) {
                                this.visitTypeDef(td.originGenericTemplate);
                                continue;
                            }
                        //}
                        this.visitTypeDef(td);
                    }
                }
            }
        }
        //}
        this.emitState.put(v, 2);
    }
}
