//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.backend;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.Expr;
import sric.compiler.ast.Expr.*;
import sric.compiler.ast.Stmt;
import sric.compiler.ast.Stmt.*;
import sric.compiler.ast.Type;
import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Buildin;
import sric.compiler.ast.FConst;
import sric.compiler.ast.SModule;
import sric.compiler.ast.SModule.Depend;
import sric.compiler.ast.Token.TokenKind;
import static sric.compiler.ast.Token.TokenKind.eq;
import static sric.compiler.ast.Token.TokenKind.gt;
import static sric.compiler.ast.Token.TokenKind.gtEq;
import static sric.compiler.ast.Token.TokenKind.lt;
import static sric.compiler.ast.Token.TokenKind.ltEq;
import static sric.compiler.ast.Token.TokenKind.notEq;
import static sric.compiler.ast.Token.TokenKind.notSame;
import static sric.compiler.ast.Token.TokenKind.same;
import sric.compiler.ast.Type.*;
import sric.compiler.resolve.ErrorChecker;

/**
 *
 * @author yangjiandong
 */
public class CppGenerator extends BaseGenerator {
    
    public boolean headMode = true;
    protected SModule module;
    private TypeDef curStruct;
    
    private String curItName;
    
    private HashMap<TypeDef, Integer> emitState = new HashMap<>();
    
    public CppGenerator(CompilerLog log, String file, boolean headMode) throws IOException {
        super(log, file);
        this.headMode = headMode;
    }
    
    public CppGenerator(CompilerLog log, PrintStream writer) {
        super(log, writer);
    }
    
    private void printCommentInclude(TopLevelDef type) {
        if (type.comment != null) {
            for (Comment comment : type.comment.comments) {
               if (comment.content.startsWith("#")) {
                   print(comment.content);
                   newLine();
               }
            }
        }
    }
    
    private String filterSymbolName(String sym) {
        if (sym.equals("int")) {
            return "_int";
        }
        else if (sym.equals("float")) {
            return "_float";
        }
        return sym;
    }
    
    protected String getSymbolName(TopLevelDef type) {
        String sym = getExternSymbol(type);
        if (sym != null) {
            return sym;
        }
        return filterSymbolName(type.name);
    }
    
    private String getExternSymbol(TopLevelDef type) {
        if ((type.flags & FConst.Extern) != 0 && type.comment != null) {
            for (Comment comment : type.comment.comments) {
               String key = "extern symbol:";
               if (comment.content.startsWith(key)) {
                   return comment.content.substring(key.length()).trim();
               }
            }
        }
        return null;
    }
    
    public void run(SModule module) {
        this.module = module;
        if (headMode) {
            String marcoName = module.name.toUpperCase()+"_H_";
            print("#ifndef ").print(marcoName).newLine();
            print("#define ").print(marcoName).newLine();

            newLine();
            print("#include \"sc_runtime.h\"").newLine();
            
            for (Depend d : module.depends) {
                print("#include \"");
                print(d.name).print(".h");
                print("\"").newLine();
            }
            newLine();
            
            /////////////////////////////////////////////////////////////
            for (FileUnit funit : module.fileUnits) {
                for (TypeAlias type : funit.typeAlias) {
                    printCommentInclude(type);
                }
                for (TypeDef type : funit.typeDefs) {
                    printCommentInclude(type);
                }
                for (FieldDef type : funit.fieldDefs) {
                    printCommentInclude(type);
                }
                for (FuncDef type : funit.funcDefs) {
                    printCommentInclude(type);
                }
            }
            
            /////////////////////////////////////////////////////////////
            print("namespace ");
            print(module.name);
            print(" {").newLine();
            
            this.indent();
            
            //types decleartion
            for (FileUnit funit : module.fileUnits) {
                for (TypeDef type : funit.typeDefs) {
                    if (type.isExtern() || this.getExternSymbol(type) != null) {
                        continue;
                    }
                    
                    printGenericParamDefs(type.generiParamDefs);
                    
                    if (type.isEnum()) {
                        print("enum struct ");
                        print(getSymbolName(type));
                        if (type.enumBase != null) {
                            print(" : ");
                            this.printType(type.enumBase);
                        }
                        print(";").newLine();
                        if ((type.flags & FConst.Reflect) != 0) {
                            print("const char* ").print(type.name).print("_toString(int i);").newLine();
                        }
                    }
                    else {
                        print("struct ");
                        print(getSymbolName(type)).print(";").newLine();
                    }
                }
            }
            
            this.unindent();
            newLine();
            print("} //ns").newLine();
            
            /////////////////////////////////////////////////////////////
            
//            print("extern \"C\" {");
//            this.indent();
//            
//            for (FileUnit funit : module.fileUnits) {
//                for (TypeDef type : funit.typeDefs) {
//                    if ((type.flags & FConst.ExternC) != 0) {
//                        print("struct ");
//                        print(getSymbolName(type)).print(";").newLine();
//                    }
//                }
//                for (FuncDef f : funit.funcDefs) {
//                    if ((f.flags & FConst.ExternC) != 0) {
//                        printFunc(f, false);
//                    }
//                }
//                for (FieldDef f : funit.fieldDefs) {
//                    if ((f.flags & FConst.ExternC) != 0) {
//                        visitField(f);
//                    }
//                }
//            }
//            
//            this.unindent();
//            newLine();
//            print("}").newLine();

            /////////////////////////////////////////////////////////////
            print("namespace ");
            print(module.name);
            print(" {").newLine();
            
            this.indent();
            
            module.walkChildren(this);

            this.unindent();
            
            newLine();
            print("} //ns").newLine();
            
            newLine();
            print("#endif //");
            print(marcoName).newLine();
        }
        else {
            print("#include \"");
            print(module.name).print(".h");
            print("\"").newLine();
            
            newLine();
            
            module.walkChildren(this);
            
            newLine();
            
            new ReflectionGen(this).printReflection(module);
            
            newLine();
        }
    }
    
    protected void printType(Type type) {
        printType(type, true);
    }

    private void printType(Type type, boolean isRoot) {
        if (type == null) {
            print("auto");
            return;
        }
        
        if (type.resolvedAliasDef != null) {
            if (type.resolvedAliasDef.isExtern()) {
                print(this.getSymbolName(type.resolvedAliasDef));
                return;
            }
        }
        
        if (type.isImmutable && !type.id.name.equals(Buildin.pointerTypeName)) {
            print("const ");
        }
        
//        if (type.isRefable) {
//            print("sric::StackRefable<");
//        }
        
        boolean printGenericParam = true;
        switch (type.id.name) {
            case "Void":
                print("void");
                break;
            case "Int":
                NumInfo intType = (NumInfo)type.detail;
                if (intType.size == 8 && intType.isUnsigned == false) {
                    print("char");
                }
                else {
                    if (intType.isUnsigned) {
                        print("u");
                    }
                    print("int"+intType.size+"_t");
                }
                break;
            case "Float":
                NumInfo floatType = (NumInfo)type.detail;
                if (floatType.size == 64) {
                    print("double");
                }
                else {
                    print("float");
                }
                break;
            case "Bool":
                print("bool");
                break;
            case Buildin.pointerTypeName:
                PointerInfo pt = (PointerInfo)type.detail;
                if (type.isRawPointerType()) {
                    printType(type.genericArgs.get(0), false);
                    
                    print("*");
                    if (type.isImmutable) {
                        print(" const");
                    }
                }
                else {
                    if (type.isImmutable) {
                        print("const ");
                    }
                    if (pt.pointerAttr == Type.PointerAttr.own) {
                        print("sric::OwnPtr");
                    }
                    else if (pt.pointerAttr == Type.PointerAttr.uniq) {
                        print("sric::UniquePtr");
                    }
                    else if (pt.pointerAttr == Type.PointerAttr.ref) {
                        print("sric::RefPtr");
                    }

//                    else if (pt.pointerAttr == Type.PointerAttr.weak) {
//                        print("sric::WeakPtr");
//                    }

                        print("<");
                        printType(type.genericArgs.get(0), false);
                        print(">");

                }
                printGenericParam = false;
                break;
            case Buildin.arrayTypeName:
                ArrayInfo arrayType = (ArrayInfo)type.detail;
                print("sric::Array<");
                printType(type.genericArgs.get(0));
                print(", ");
                this.visit(arrayType.sizeExpr);
                print(">");
                printGenericParam = false;
                break;
            case Buildin.funcTypeName:
                FuncInfo ft = (FuncInfo)type.detail;
                if (ft.prototype.isStaticClosure()) {
                    print("sric::Func<");
                    printType(ft.prototype.returnType);
                    print("(");
                    if (ft.prototype.paramDefs != null) {
                        int i = 0;
                        for (FieldDef p : ft.prototype.paramDefs) {
                            if (i > 0) print(", ");
                            printType(p.fieldType, false);
                            ++i;
                        }
                    }
                    print(")>::Type");
                }
                else {
                    print("std::function<");
                    printType(ft.prototype.returnType);
                    print("(");
                    if (ft.prototype.paramDefs != null) {
                        int i = 0;
                        for (FieldDef p : ft.prototype.paramDefs) {
                            if (i > 0) print(", ");
                            printType(p.fieldType, false);
                            ++i;
                        }
                    }
                    print(")>");
                }
                printGenericParam = false;
                break;
            default:
                printIdExpr(type.id);
                break;
        }

        if (printGenericParam && type.genericArgs != null) {
            print("<");
            int i= 0;
            for (Type p : type.genericArgs) {
                if (i > 0) {
                    print(", ");
                }
                printType(p, false);
                ++i;
            }
            print(" >");
        }
        
//        if (type.isRefable) {
//            print(" >");
//        }
        
        if (type.isReference) {
            print("&");
        }
    }

    private void printIdExpr(IdExpr id) {
        if (id.resolvedDef instanceof TopLevelDef td) {
            String symbolName = getExternSymbol(td);
            if (symbolName != null) {
                print(symbolName);
                return;
            }
        }

        
        if (id.namespace != null) {
            printIdExpr(id.namespace);
            print("::");
        }
        else {
            if (id.resolvedDef instanceof TopLevelDef td) {
                if ((td.flags & FConst.ExternC) == 0 && td.parent instanceof FileUnit fu) {
                    print(fu.module.name);
                    print("::");
                }
            }
            else if (id.name.equals(TokenKind.superKeyword.symbol)) {
                printType(curStruct.inheritances.get(0));
                return;
            }
            else if (id.name.equals(TokenKind.dot.symbol)) {
                print(this.curItName);
                return;
            }
            else if (id.name.equals(TokenKind.thisKeyword.symbol)) {
                if (curStruct != null && curStruct.isSafe()) {
                    if (id._isAccessExprTarget) {
                        print("sc_this");
                    }
                    else {
                        print("sc_thisref");
                    }
                }
                else {
                    print("this");
                }
                return;
            }
        }
        
        if (id._autoDerefRefableVar && id.resolvedDef instanceof FieldDef df && df.isRefable) {
            print("(*");
        }
        
        if (id.implicitThis && curStruct != null && curStruct.isSafe()) {
            print("sc_this->");
        }
        
        if (id.resolvedDef instanceof TopLevelDef td) {
            print(getSymbolName(td));
        }
        else {
            print(filterSymbolName(id.name));
        }
        
        if (id._autoDerefRefableVar && id.resolvedDef instanceof FieldDef df && df.isRefable) {
            print(")");
        }
    }
    
    @Override
    public void visitTypeAlias(AstNode.TypeAlias v) {
        if (v.isExtern()) {
            return;
        }
        print("typedef ");
        printType(v.type);
        print(" ");
        print(getSymbolName(v));
        print(";");
        newLine();
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.walkChildren(this);
    }

    @Override
    public void visitField(AstNode.FieldDef v) {

        if (v.isExtern()) {
            return;
        }
        
        if (v.isLocalVar) {
            if (printLocalFieldDefAsExpr(v)) {
                print(";").newLine();
            }
            return;
        }
        
        if (headMode && v.parent instanceof FileUnit) {
            if ((v.flags & FConst.ConstExpr) == 0) {
                print("extern ");
            }
        }
        
        if (headMode) {
            if (printLocalFieldDefAsExpr(v)) {
                print(";").newLine();
            }
        }
        else if (v.isStatic()) {
            if (printLocalFieldDefAsExpr(v)) {
                print(";").newLine();
            }
        }
    }
    
    boolean printLocalFieldDefAsExpr(AstNode.FieldDef v) {
        boolean isImpl = implMode();
        boolean isStatic = v.isStatic();//(v.flags & FConst.Static) != 0;
        
        boolean isConstExpr = false;
        if ((v.flags & FConst.ConstExpr) != 0) {
            if (isImpl) {
                return false;
            }
            print("constexpr ");
            isConstExpr = true;
        }
        
        if (isStatic && !isImpl && v.parent instanceof TypeDef) {
            print("static ");
        }
               
        if (v.isRefable) {
            print("sric::StackRefable<");
        }
        printType(v.fieldType);
        if (v.isRefable) {
            print(">");
        }
        print(" ");
        if (isStatic && isImpl && !v.isLocalVar) {
            if (v.parent instanceof TypeDef t) {
                if (t.parent instanceof FileUnit fu) {
                    if (fu.module != null) {
                        print(fu.module.name).print("::");
                    }
                }
                print(getSymbolName(t)).print("::");
            }
            else if (v.parent instanceof FileUnit fu) {
                if (fu.module != null) {
                    print(fu.module.name).print("::");
                }
            }
        }

        print(getSymbolName(v));
//        if (v.fieldType.isArray()) {
//            ArrayInfo arrayType = (ArrayInfo)v.fieldType.detail;
//            print("[");
//            this.visit(arrayType.sizeExpr);
//            print("]");
//        }
        
        boolean init = false;
        if (v.isLocalVar) {
            init = true;
        }
        else if (isConstExpr) {
            init = true;
        }
        else if (isStatic && isImpl) {
            init = true;
        }
        else if (!isStatic && !isImpl) {
            init = true;
        }
        
        if (init) {
            if (v.initExpr != null) {
                if (v.initExpr instanceof Expr.WithBlockExpr wbe && wbe._storeVar != null) {
                    print(" ");
                    this.visit(v.initExpr);
                }
                else if (v.initExpr instanceof Expr.ArrayBlockExpr) {
                    print(" ");
                    this.visit(v.initExpr);
                }
                else {
                    print(" = ");
                    this.visit(v.initExpr);
                }
            }
            else if (!v.uninit && v.fieldType != null) {
                if (v.fieldType.isNum()) {
                    print(" = 0");
                }
                else if (v.fieldType.isBool()) {
                    print(" = false");
                }
                else if (v.fieldType.isArray()) {
                    print(" = {}");
                }
                else if (v.fieldType.isRawPointerType()) {
                    print(" = nullptr");
                }
                else if (v.fieldType.detail instanceof Type.FuncInfo finfo && finfo.prototype.isStaticClosure()) {
                    print(" = nullptr");
                }
                else if (v.fieldType.id.resolvedDef instanceof GenericParamDef) {
                    print("{}");
                }
            }
        }
        
        return true;
    }
    
    private boolean implMode() {
        return !headMode;
    }
    
    private void printGenericParamDefs(ArrayList<GenericParamDef> generiParamDefs) {
        if (generiParamDefs != null) {
            print("template ");
            print("<");
            int i = 0;
            for (var gp : generiParamDefs) {
                if (i > 0) print(", ");
                print("typename ");
                print(gp.name);
                ++i;
            }
            print(">").newLine();
        }
    }
    
    private boolean isEntryPoint(AstNode.FuncDef v) {
        if (v.parent instanceof FileUnit &&  v.name.equals("main")) {
            return true;
        }
        return false;
    }
    
    private void printFunc(AstNode.FuncDef v, boolean isOperator) {
        boolean inlined = (v.flags & FConst.Inline) != 0 || v.generiParamDefs != null;
        if (v.parent instanceof TypeDef sd) {
            if (sd.generiParamDefs != null) {
                inlined = true;
            }
        }
        if (implMode()) {
            if (v.code == null || inlined) {
                return;
            }
        }
        
        newLine();
        
        printGenericParamDefs(v.generiParamDefs);
        
        if (headMode) {
            if (v.parent instanceof TypeDef sd && !sd.isEnum()) {
                if ((v.flags & FConst.Private) != 0) {
                    print("private: ");
                }
                else {
                    print("public: ");
                }
            }
            
            if ((v.flags & FConst.Inline) != 0) {
                print("inline ");
            }
            if ((v.flags & FConst.Virtual) != 0 || (v.flags & FConst.Abstract) != 0) {
                print("virtual ");
            }
            if ((v.flags & FConst.Static) != 0) {
                print("static ");
            }
        }
        
//        if ((v.flags & FConst.Extern) != 0) {
//            print("extern ");
//        }
        if ((v.flags & FConst.Ctor) == 0) {
            if (v.isAsync()) {
                print("sric::Promise<");
            }
            printType(v.prototype.returnType);
            if (v.isAsync()) {
                print(" >");
            }
            print(" ");
        }
        
        if (implMode()) {
            if (v.parent instanceof TypeDef t) {
                if (t.parent instanceof FileUnit fu) {
                    if (fu.module != null) {
                        print(fu.module.name).print("::");
                    }
                }
                print(getSymbolName(t)).print("::");
            }
            else if (v.parent instanceof FileUnit fu) {
                if (fu.module != null && !isEntryPoint(v)) {
                    print(fu.module.name).print("::");
                }
            }
        }
        
        if ((v.flags & FConst.Ctor) != 0) {
            if (!v.name.equals("new")) {
                print("~");
            }
            if (v.parent instanceof TypeDef t) {
                print(t.name);
            }
        }
        else if (isOperator) {
            print("operator");
            switch (v.name) {
                case "plus":
                    print("+");
                    break;
//                case "set":
//                    print("[]");
//                    break;
                case "get":
                    print("[]");
                    break;
                case "minus":
                    print("-");
                    break;
                case "mult":
                    print("*");
                    break;
                case "div":
                    print("/");
                    break;
                case "compare":
                    print("<=>");
                    break;
                default:
                    break;
            }
        }
        else {
            print(getSymbolName(v));
        }
        
        printFuncPrototype(v.prototype, false, v.isStatic(), implMode() || inlined, v.isAsync());
        
        if (v.code == null) {
            if ((v.flags & FConst.Abstract) != 0) {
                print(" = 0");
            }
            print(";");
        }
        else {
            if (implMode() || inlined) {
                boolean isSafe = false;
                if (curStruct != null && curStruct.isSafe() && !v.isStatic()) {
                    print(" {").newLine();
                    this.indent();
                    if (v._useThisAsRefPtr) {
                        print("SC_DEFINE_THIS_REFPTR");
                    }
                    else {
                        print("SC_DEFINE_THIS");
                    }
                    newLine();
                    isSafe = true;
                    v.code._printBrace = false;
                }
                else {
                    print(" ");
                }
                
                this.visit(v.code);
                
                if (isSafe) {
                    this.unindent();
                    print("}").newLine();
                }
            }
            else {
                print(";");
            }
        }
    }
    
    @Override
    public void visitFunc(AstNode.FuncDef v) {

        if (v.isExtern()) {
            if (v.parent instanceof TypeDef sd && headMode) {
            }
            else {
                return;
            }
        }
        
        if (isEntryPoint(v) && headMode) {
            return;
        }
        
        printFunc(v, false);
        if ((v.flags & FConst.Operator) != 0 && !v.name.equals("set") && !v.name.equals("compare")) {
            printFunc(v, true);
        }
    }
    
    private void printFuncPrototype(FuncPrototype prototype, boolean isLambda, boolean isStatic, boolean isImple, boolean isAsync) {
        print("(");
        if (prototype != null && prototype.paramDefs != null) {
            int i = 0;
            for (FieldDef p : prototype.paramDefs) {
                if (i > 0) {
                    print(", ");
                }
                if (p.fieldType.isVarArgType()) {
                    print("...");
                }
                else {
                    printType(p.fieldType);
                    print(" ");
                    print(p.name);
                }
                if (p.initExpr != null && !isImple) {
                    print(" = ");
                    this.visit(p.initExpr);
                }
                ++i;
            }
        }
        print(")");
        
        if (!isLambda && !isStatic && prototype.isThisImmutable()) {
            print(" const");
        }
        else if (isLambda && !prototype.isThisImmutable()) {
            print(" mutable");
        }
        
        if (!isLambda) {
            print(" SC_NOTHROW");
        }
        
        if (isLambda && prototype != null) {
            if (prototype.returnType != null && !prototype.returnType.isVoid()) {
                print("->");
                printType(prototype.returnType);
            }
        }
    }

    @Override
    public void visitTypeDef(TypeDef v) {
        if (v.isExtern()) {
            return;
        }
        
        if (headMode) {
            if (!topoSort(v)) return;
        }
        
        curStruct =  v;
        
        if (implMode()) {

            v.walkChildren(this);

        }
        else {
            newLine();

            if (v.isEnum()) {
                print("enum struct ");
                print(getSymbolName(v));
                if (v.enumBase != null) {
                    print(" : ");
                    this.printType(v.enumBase);
                }
                print(" {").newLine();
                indent();

                int i = 0;
                for (FieldDef f : v.fieldDefs) {
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
                print("};").newLine();
                return;
            }

            //if (v instanceof StructDef sd) {
                printGenericParamDefs(v.generiParamDefs);
            //}

            print("struct ");
            print(getSymbolName(v));
            
            boolean isSafeInherit = false;
            if (v.isSafe() && !v.isConcroteInherit()) {
                isSafeInherit = true;
                print(" SC_OBJ_BASE ");
            }

            //must before inheritances
            boolean isDynamicReflect = v.isDynamicReflect();
            boolean needInheriteReflectable = isDynamicReflect;
            if (isDynamicReflect) {
                if (v.inheritances != null && v.inheritances.size() > 0) {
                    if (v.inheritances.get(0).id.resolvedDef instanceof TypeDef tf && tf.isDynamicReflect()) {
                        needInheriteReflectable = false;
                    }
                }
                if (needInheriteReflectable) {
                    if (isSafeInherit) {
                        print(" SC_BEGIN_INHERIT ");
                    }
                    else {
                        print(" : ");
                    }
                    print("public sric::Reflectable");
                }
            }
            
            if (v.inheritances != null) {
                int i = 0;
                for (Type inh : v.inheritances) {
                    if (i == 0 && !needInheriteReflectable) {
                        if (isSafeInherit) {
                            print(" SC_BEGIN_INHERIT ");
                        }
                        else {
                            print(" : ");
                        }
                    }
                    else print(", ");
                    print("public ");
                    printType(inh);
                    ++i;
                }
            }
            
            if ((v.flags & FConst.Noncopyable) != 0) {
                if (v.inheritances != null) {
                    print(", public Noncopyable");
                }
                else {
                    print(" : public Noncopyable");
                }
            }
            //}

            print(" {").newLine();
            indent();
            
            if (isSafeInherit) {
                print("SC_SAFE_STRUCT").newLine();
            }

            v.walkChildren(this);

            newLine();
            
            if (v.isPolymorphic()) {
                if (!v._hasDecotr) {
                    print("public: virtual ~");
                    print(getSymbolName(v));
                    print("() SC_NOTHROW {}").newLine();
                }
                if (!v._hasCotr && v.isStruct()) {
                    print("public: ");
                    print(getSymbolName(v));
                    print("() SC_NOTHROW {}").newLine();
                }
            }
            
            if (isDynamicReflect) {
                print("public: const char* _typeof() const SC_NOTHROW { return \"");
                print(this.module.name).print("::").print(v.name);
                print("\"; }");
            }
            
            unindent();
            newLine();
            
            print("};").newLine();
        
        }
        
        curStruct =  null;
    }

    private boolean topoSort(TypeDef v) {
        //Topo sort
        if (this.emitState.get(v) != null) {
            int state = this.emitState.get(v);
            if (state == 2) {
                return false;
            }
            err("Cyclic dependency", v.loc);
            return false;
        }
        this.emitState.put(v, 1);
        //if (v instanceof StructDef sd) {
        if (v.inheritances != null) {
            for (Type t : v.inheritances) {
                if (t.id.resolvedDef != null && t.id.resolvedDef instanceof TypeDef td) {
                    if (td.parent != null && ((FileUnit)td.parent).module == this.module) {
                        //if (td instanceof StructDef tds) {
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
        
        if (!v.isEnum()) {
            for (FieldDef f : v.fieldDefs) {
                if (f.isStatic() && f.fieldType.id.resolvedDef instanceof TypeDef td) {
                    if (td.originGenericTemplate != null) {
                        td  = td.originGenericTemplate;
                    }
                    if (td == v) {
                        continue;
                    }
                }
                if (!f.fieldType.isPointerType() && f.fieldType.id.resolvedDef != null && f.fieldType.id.resolvedDef instanceof TypeDef td) {
                    if (td.parent != null && td.parent instanceof FileUnit unit) {
                        if (unit.module == this.module) {
                            //if (td instanceof StructDef tds) {
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
        }
        //}
        this.emitState.put(v, 2);
        return true;
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof Block bs) {
            if (bs._printBrace) {
                print("{").newLine();
                indent();
            }
            bs.walkChildren(this);
            if (bs._printBrace) {
                unindent();
                print("}");
                if (!bs.isWithBlockExpr) {
                    newLine();
                }
            }
        }
        else if (v instanceof IfStmt ifs) {
            print("if (");
            this.visit(ifs.condition);
            print(") ");
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                print("else ");
                this.visit(ifs.elseBlock);
            }
        }
        else if (v instanceof LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof WhileStmt whiles) {
            print("while (");
            this.visit(whiles.condition);
            print(") ");
            this.visit(whiles.block);
        }
        else if (v instanceof ForStmt fors) {
            print("for (");
            if (fors.init != null) {
                if (fors.init instanceof LocalDefStmt varDef) {
                    printLocalFieldDefAsExpr(varDef.fieldDef);
                }
                else if (fors.init instanceof ExprStmt s) {
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
        else if (v instanceof SwitchStmt switchs) {
            print("switch (");
            this.visit(switchs.condition);
            print(") {").newLine();
            
            this.indent();
            
            for (CaseBlock cb : switchs.cases) {
                this.unindent();
                print("case ");
                this.visit(cb.caseExpr);
                print(":").newLine();
                this.indent();
                
                this.visit(cb.block);
                
                if (!cb.fallthrough) {
                    print("break;").newLine();
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
        else if (v instanceof ExprStmt exprs) {
            this.visit(exprs.expr);
            print(";").newLine();
        }
        else if (v instanceof JumpStmt jumps) {
            print(jumps.opToken.symbol).print(";").newLine();
        }
        else if (v instanceof UnsafeBlock bs) {
            print("/*unsafe*/ ");
            this.visit(bs.block);
        }
        else if (v instanceof ReturnStmt rets) {
            if (rets.expr != null) {
                if (rets._isCoroutineRet) {
                    print("co_return ");
                }
                else {
                    print("return ");
                }
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
        int parentheses = 0;
        if (v.isStmt || v.isTopExpr || v instanceof IdExpr || v instanceof LiteralExpr || v instanceof CallExpr || v instanceof GenericInstance 
                || v instanceof AccessExpr || v instanceof WithBlockExpr || v instanceof ArrayBlockExpr || v instanceof TypeExpr) {
            
        }
        else {
            print("(");
            parentheses++;
        }

        
        if (v.implicitTypeConvertTo != null && !v.implicitTypeConvertTo.isVarArgType()) {
            boolean ok = false;
            if (v.implicitStringConvert) {
                print("sric::strStatic(");
                parentheses++;
                ok = true;
            }
            else if (v.isPointerConvert) {
                if (v.resolvedType.detail instanceof Type.PointerInfo p1 && v.implicitTypeConvertTo.detail instanceof Type.PointerInfo p2) {
                    if ((p1.pointerAttr == Type.PointerAttr.own || p1.pointerAttr == Type.PointerAttr.uniq) && p2.pointerAttr == Type.PointerAttr.ref) {
                        print("sric::RefPtr<");

                        printType(v.implicitTypeConvertTo.genericArgs.get(0));
                        
                        print(" >(");
                        parentheses++;
                        ok = true;
                    }
//                    else if (p1.pointerAttr == Type.PointerAttr.raw && p2.pointerAttr == Type.PointerAttr.ref) {
//                        print("sric::RefPtr<");
//                        printType(v.implicitTypeConvertTo.genericArgs.get(0));
//                        print(" >(");
//                        convertParentheses = true;
//                    }
                }
            }
            
            if (!ok) {
                print("(");
                printType(v.implicitTypeConvertTo);
                print(")");
            }
        }
        
        if (v.implicitMove && (v.implicitTypeConvertTo != null || v.checkNonnullable)) {
            if (v instanceof IdExpr e && e.resolvedDef instanceof FieldDef df && df.isLocalVar) {
                print("std::move(");
                parentheses++;
            }
        }

        if (v.checkNonnullable) {
            print("sc_notNull(");
            parentheses++;
        }
        
        if (v instanceof IdExpr e) {
            this.printIdExpr(e);
        }
        else if (v instanceof AccessExpr e) {
            if (e._addressOf && e.target.resolvedType != null) {
                if (e.target.resolvedType.detail instanceof Type.PointerInfo pinfo) {
                    print("sric::RefPtr<");
                    this.printType(e.resolvedType);
                    print(">(");
                    this.visit(e.target);
                    print(",");

                    print("&");
                    this.visit(e.target);
                    print("->");
                    print(e.name);

                    print(")");
                }
                else {
                    print("sric::RefPtr<");
                    this.printType(e.resolvedType);
                    print(">(&");
                    this.visit(e.target);
                    print(",");

                    print("&");
                    this.visit(e.target);
                    print("->");
                    print(e.name);

                    print(")");
                }
            }
            else {
                boolean isNullable = false;
                if (e.target.resolvedType != null && e.target.resolvedType.detail instanceof Type.PointerInfo pinfo) {
                    if (pinfo.isNullable && pinfo.pointerAttr == Type.PointerAttr.raw) {
                        print("sc_notNull(");
                        isNullable = true;
                    }
                }

                this.visit(e.target);
                if (isNullable) {
                    print(")");
                }

                if (e.target instanceof IdExpr ide && ide.name.equals(TokenKind.superKeyword.symbol)) {
                    print("::");
                }
                else if (e.target.resolvedType != null && e.target.resolvedType.isPointerType()) {
                    print("->");
                }
//                else if (e.target.resolvedType != null && e.target.resolvedType.isRefable) {
//                    print("->");
//                }
                else {
                    print(".");
                }
                print(e.name);
            }
        }
        else if (v instanceof LiteralExpr e) {
            printLiteral(e);
        }
        else if (v instanceof BinaryExpr e) {
            printBinaryExpr(e);
        }
        else if (v instanceof CallExpr e) {
            this.visit(e.target);
            print("(");
            if (e.args != null) {
                int i = 0;
                for (CallArg t : e.args) {
                    if (i > 0) print(", ");
                    this.visit(t.argExpr);
                    ++i;
                }
            }
            print(")");
        }
        else if (v instanceof UnaryExpr e) {
            if (null == e.opToken) {
                print(e.opToken.symbol);
                this.visit(e.operand);
            }
            else switch (e.opToken) {
                case amp:
                    if (e._addressOfSafeStruct && e.resolvedType.genericArgs != null) {
                        print("sric::addressOf<");
                        this.printType(e.resolvedType.genericArgs.get(0));
                        print(">(");
                        this.visit(e.operand);
                        print(")");
                    }
                    else if (e._addressOfField) {
                        this.visit(e.operand);
                    }
                    else {
                        print("&");
                        this.visit(e.operand);
                    }   break;
                case moveKeyword:
                    print("std::move(");
                    this.visit(e.operand);
                    print(")");
                    break;
                case awaitKeyword:
                    print("co_await ");
                    this.visit(e.operand);
                    //print("");
                    break;
                case newKeyword:
                    print("sric::new_<");
                    this.visit(e.operand);
                    print(">()");
                    break;
                default:
                    print(e.opToken.symbol);
                    this.visit(e.operand);
                    break;
            }
        }
        else if (v instanceof TypeExpr e) {
            this.printType(e.type);
        }
        else if (v instanceof IndexExpr e) {
//            if (e.resolvedOperator != null) {
//                this.visit(e.target);
//                print(".get(");
//                this.visit(e.index);
//                print(")");
//            }
//            else {
//                boolean p = false;
//                if (e.target.resolvedType != null && (e.target.resolvedType.isRefable)) {
//                    print("(*");
//                    p = true;
//                }
                this.visit(e.target);
//                if (p) {
//                    print(")");
//                }
                print("[");
                this.visit(e.index);
                print("]");
//            }
        }
        else if (v instanceof GenericInstance e) {
            this.visit(e.target);
            print("<");
            int i = 0;
            for (Type t : e.genericArgs) {
                if (i > 0) print(", ");
                this.printType(t);
                ++i;
            }
            print(" >");
        }
        else if (v instanceof IfExpr e) {
            this.visit(e.condition);
            print("?");
            this.visit(e.trueExpr);
            print(":");
            this.visit(e.falseExpr);
        }
        else if (v instanceof Expr.WithBlockExpr e) {
            printWithBlockExpr(e);
        }
        else if (v instanceof Expr.ArrayBlockExpr e) {
            printArrayBlockExpr(e);
        }
        else if (v instanceof ClosureExpr e) {
            printClosureExpr(e);
        }
//        else if (v instanceof NonNullableExpr e) {
//            print("sc_notNull(");
//            this.visit(e.operand);
//            print(")");
//        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
        
        while (parentheses > 0) {
            print(")");
            --parentheses;
        }
    }

    private void printBinaryExpr(BinaryExpr e) {
        if (e.opToken == TokenKind.asKeyword) {
            Type targetType = ((TypeExpr)e.rhs).type;
            boolean processed = false;
            if (targetType.isPointerType()) {
                if (targetType.detail instanceof Type.PointerInfo pinfo) {
                    
                    if (!pinfo.isNullable) {
                        print("sc_notNull(");
                    }
                    
                    Type elemType = targetType.genericArgs.get(0);
                    if (!targetType.isRawPointerType() && targetType.genericArgs != null) {
                        this.visit(e.lhs);
                        if (elemType.id.resolvedDef instanceof TypeDef td && td.isPolymorphic()) {
                            print(".dynamicCastTo<");
                        }
                        else {
                            print(".castTo<");
                        }
                        printType(elemType);
                        print(" >()");
                        processed = true;
                    }
                    else if (elemType.id.resolvedDef instanceof TypeDef td && td.isPolymorphic()) {
                        print("dynamic_cast<");
                        printType(targetType);
                        print(" >(");
                        this.visit(e.lhs);
                        print(")");
                        processed = true;
                    }
                    else {
                        print("(");
                        printType(targetType);
                        print(")(");
                        this.visit(e.lhs);
                        print(")");
                        processed = true;
                    }
                    
                    if (!pinfo.isNullable) {
                        print(")");
                    }
                }
            }
            else {
                print("(");
                printType(targetType);
                print(")(");
                this.visit(e.lhs);
                print(")");
            }
        }
        else if (e.opToken == TokenKind.isKeyword) {
            Type targetType = ((TypeExpr)e.rhs).type;
            if (targetType.isPointerType()) {
                if (targetType.genericArgs != null) {
                    print("sric::ptrIs<");
                    printType(targetType.genericArgs.get(0));
                    print(" >(");
                    this.visit(e.lhs);
                    print(")");
                }
            }
            else {
                print(e.lhs.resolvedType.typeEquals(targetType) ? "true" : "false");
            }
        }
        //index set operator: a[i] = b
        else if (e.opToken == TokenKind.assign && e.lhs instanceof IndexExpr iexpr) {
//            boolean p = false;
//            if (iexpr.target.resolvedType != null && (iexpr.target.resolvedType.isRefable)) {
//                print("(*");
//                p = true;
//            }
            this.visit(iexpr.target);
//            if (p) {
//                print(")");
//            }

            if (iexpr.resolvedOperator != null) {
                print(".set(");
                this.visit(iexpr.index);
                print(", ");
                this.visit(e.rhs);
                print(")");
            }
            else {
                print("[");
                this.visit(iexpr.index);
                print("] = ");
                this.visit(e.rhs);
            }
        }
        else {
            if (e.resolvedOperator !=  null) {
                if (e.opToken == TokenKind.minus || e.opToken == TokenKind.plus || e.opToken == TokenKind.star || e.opToken == TokenKind.slash) {
                    this.visit(e.lhs);
                    print(" ");
                    print(e.opToken.symbol);
                    print(" ");
                    this.visit(e.rhs);
                }
                else {
                    this.visit(e.lhs);
                    print(".").print(e.resolvedOperator.name).print("(");
                    this.visit(e.rhs);
                    print(")");
                    switch (e.opToken) {
                        case eq:
                            print(" == 0");
                            break;
                        case notEq:
                            print(" != 0");
                            break;
                        case lt:
                            print(" < 0");
                            break;
                        case gt:
                            print(" > 0");
                            break;
                        case ltEq:
                            print(" <= 0");
                            break;
                        case gtEq:
                            print(" >= 0");
                            break;
                        default:
                    }
                }
            }
            else {
                this.visit(e.lhs);
                print(" ");
                print(e.opToken.symbol);
                print(" ");
                this.visit(e.rhs);
            }
        }
    }
    
    void printLiteral(LiteralExpr e) {
        if (e.value == null) {
            if (e.nullPtrType != null && e.nullPtrType.detail instanceof PointerInfo pinfo) {
                if (pinfo.pointerAttr == Type.PointerAttr.own) {
                    print("sric::OwnPtr<");
                    printType(e.nullPtrType.genericArgs.get(0));
                    print(">()");
                }
                else if (pinfo.pointerAttr == Type.PointerAttr.uniq) {
                    print("sric::UniquePtr<");
                    printType(e.nullPtrType.genericArgs.get(0));
                    print(">()");
                }
                else if (pinfo.pointerAttr == Type.PointerAttr.ref) {
                    print("sric::RefPtr<");
                    printType(e.nullPtrType.genericArgs.get(0));
                    print(">()");
                }
            }
            else {
                print("nullptr");
            }
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
            printStringLiteral(li);
        }
    }
    
    void printStringLiteral(String li) {
        print("\"");
        for (int i=0; i<li.length(); ++i) {
            char c = li.charAt(i);
            printChar(c);
        }
        print("\"");
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
    
    private void printItBlockArgs(WithBlockExpr e, String varName, boolean printBrace) {
        if (e.block != null) {
            String savedName = curItName;
            curItName = varName;
            
            e.block._printBrace = printBrace;
            this.visit(e.block);
            
            curItName = savedName;
        }
    }
    
    void printWithBlockExpr(WithBlockExpr e) {
//        if (!e.target.isResolved()) {
//            return;
//        }

        /* Local var define:
            var x = a { ... };
            =>
                x = a; { x.name = y; }
                x = {};
                x; { x.name = y; }
        */
        if (e._storeVar != null && e._storeVar.isLocalVar && (e._storeVar.fieldType == null || !e._storeVar.fieldType.isImmutable)) {
            if (!e._isType) {
                print(" = ");
                this.visit(e.target);
            }
            else if (e.block.stmts.size() == 0) {
                //print(" = {}");
                return;
            }
            print(";");
            
            String targetName = e._storeVar.name;
            if (e._storeVar.isRefable) {
                targetName = "(*" + targetName + ")";
            }
            printItBlockArgs(e, targetName, true);
            return;
        }
        
        /* single name var with:
            a { ... }
        */
        if (e.isStmt && !e._isType && e.target instanceof Expr.IdExpr id && id.namespace == null) {
            String targetId = id.name;
            if (id._autoDerefRefableVar && id.resolvedDef instanceof FieldDef fd && fd.isRefable) {
                targetId = "(*" + targetId + ")";
            }
            if (targetId != null) {
                printItBlockArgs(e, targetId, true);
                return;
            }
        }
        
        //to closure
        if (e.target.isResolved()) {
            if (e._storeVar != null) {
                if (e.block.stmts.size() == 0) {
                    print(" = {}");
                    return;
                }
                print(" = ");
            }
            //[&]()->T{ T it = target(); it.name =1; return it; }()
            if (e._storeVar == null || (e._storeVar.isLocalOrParam())) {
                print("[&]()->");
            }
            else {
                print("[]()->");
            }
            printType(e.resolvedType);
            print("{").newLine();
            this.indent();
            
            printType(e.resolvedType);
            print(" it");
            if (!e._isType) {
                print(" = ");
                this.visit(e.target);
            }
            print(";").newLine();
            
            printItBlockArgs(e, "it", false);
            
            print("return it;").newLine();
            
            this.unindent();
            
            print("}()");
        }
    }
    
    void printArrayBlockExpr(ArrayBlockExpr e) {
        if (e._storeVar != null) {
            print(";");
        }
        int i = 0;
        print("{");
        if (e.args != null) {
            String targetName = e._storeVar.name;
            if (e._storeVar.isRefable) {
                targetName = "(*" + targetName + ")";
            }
            for (Expr t : e.args) {
                print(targetName);
                print("[");
                print(""+i);
                print("] = ");
                this.visit(t);
                print("; ");
                ++i;
            }
        }
        print("}");
    }
    
    void printClosureExpr(ClosureExpr expr) {
        if (expr.captures != null) {
            print("[=");
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
            print("]");
        }
        else {
            print("[]");
        }
        
        this.printFuncPrototype(expr.prototype, true, false, false, false);
        
        print(" ");
        this.visit(expr.code);
    }
}
