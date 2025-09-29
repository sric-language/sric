//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//   29 Aug 06  Brian Frank  Ported from Java to Fan
//
package sric.compiler.parser;

import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode.FileUnit;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Token.TokenKind;
import sric.compiler.CompilerLog.CompilerErr;
import java.util.ArrayList;
import sric.compiler.ast.*;
import sric.compiler.ast.Expr.*;
import static sric.compiler.ast.Token.TokenKind.*;


/**
 *
 * @author yangjiandong
 */
public class Parser {

    FileUnit unit;    // compilation unit to generate
    ArrayList<Token> tokens;       // tokens all read in
    protected int numTokens;           // number of tokens
    protected int pos;                 // offset into tokens for cur
    protected Token cur;           // current token
    protected TokenKind curt;             // current token type
    protected Token peek;          // next token
    protected TokenKind peekt;            // next token type
//    protected boolean inFieldInit;        // are we currently in a field initializer
//    protected Type curType;        // current TypeDef scope

    CompilerLog log;
    
    public Parser(CompilerLog log, String code, FileUnit unit) {
        this.log = log;
        this.unit = unit;
        Tokenizer toker = new Tokenizer(log, unit.file, code);
        tokens = toker.tokenize();
        
        this.numTokens = tokens.size();
        reset(0);
    }
    
    Loc curLoc() {
        return cur.loc;
    }

    public void parse() {
        usings();
        while (curt != TokenKind.eof) {
            try {
                TopLevelDef defNode = topLevelDef();
                if (defNode != null) {
                    unit.addDef(defNode);
                }
                else {
                    recoverToDef();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!recoverToDef()) {
                    break;
                }
            }
        }
    }

    private boolean recoverToDef() {
        while (curt != TokenKind.eof) {
            switch (curt) {
                case traitKeyword:
                case enumKeyword:
                case structKeyword:
                case funKeyword:
                case varKeyword:
                case typealiasKeyword:
                    return true;
            }
            consume();
        }
        return false;
    }
    
    private boolean recoverToSlotDef() {
        while (curt != TokenKind.eof) {
            switch (curt) {
                case funKeyword:
                case varKeyword:
                    return true;
            }
            consume();
        }
        return false;
    }

//////////////////////////////////////////////////////////////////////////
// Usings
//////////////////////////////////////////////////////////////////////////
    /**
     ** <using> :=  <usingPod> | <usingType>
     **   <usingPod> := "import" <id> <eos>
     **   <usingType> := "import" <type>  ["as" <id>] <eos>
     */
    private void usings() {
        while (curt == TokenKind.importKeyword) {
            parseImports();
        }
    }
    
    private TypeAlias parseTypeAlias(Comments doc, int flags) {
        Loc loc = curLoc();
        consume(TokenKind.typealiasKeyword);
        TypeAlias u = new TypeAlias();
        u.name = consumeId();
        u.flags = flags;
        u.comment = doc;
        
        consume(TokenKind.assign);
        u.type = typeRef();

        endOfStmt();
        endLoc(u, loc);
        return u;
    }

    private void parseImports() {
        Loc loc = curLoc();
        consume(TokenKind.importKeyword);
        Import u = new Import();
        u.id = idExpr();
        if (curt == TokenKind.doubleColon && peekt == TokenKind.star) {
            consume();
            consume();
            u.star = true;
        }

        endOfStmt();
        endLoc(u, loc);
        unit.imports.add(u);
    }

    private TopLevelDef topLevelDef() {
        Loc loc = curLoc();
        // [<doc>]
        Comments doc = doc();

        if (curt == TokenKind.eof) {
            return null;
        }

        // <flags>
        int flags = flags();

        switch (curt) {
            case traitKeyword:
            case enumKeyword:
            case structKeyword:
                return typeDef(doc, flags);
            case funKeyword:
            {
                consume();
                String sname = consumeId();
                return methodDef(loc, doc, flags, null, sname);
            }
            //case constKeyword:
            case varKeyword:
            {
                boolean isConst = false;
                if (curt == TokenKind.constKeyword) {
                    isConst = true;
                }
                consume();
                String sname = consumeId();
                return fieldDef(loc, doc, flags, null, sname, isConst);
            }
            case typealiasKeyword:
                return parseTypeAlias(doc, flags);
        }
        err("Expected var,const,fun keyword");
        return null;
    }
    
    private AstNode slotDef(Comments doc) {
        Loc loc = curLoc();
        // <flags>
        int flags = flags();

        switch (curt) {
            case funKeyword:
            {
                consume();
                if (curt == newKeyword || curt == deleteKeyword) {
                    String sname = curt.symbol;
                    consume();
                    flags |= FConst.Ctor;
                    return methodDef(loc, doc, flags, null, sname);
                }
                String sname = consumeId();
                return methodDef(loc, doc, flags, null, sname);
            }
            //case constKeyword:
            case varKeyword:
            {
                boolean isConst = false;
                if (curt == TokenKind.constKeyword) {
                    isConst = true;
                }
                consume();
                String sname = consumeId();
                return fieldDef(loc, doc, flags, null, sname, isConst);
            }
        }
        err("Expected var,const,fun keyword");
        return null;
    }

    /**
     ** Identifier expression:
     **   <idExpr> =  [(<id>"::")*] <id>
     */
    protected IdExpr idExpr() {
        Loc loc = curLoc();
        Expr.IdExpr e = new Expr.IdExpr(null);
        String id = consumeId();
        e.name = id;
        endLoc(e, loc);
        
        while (curt == TokenKind.doubleColon) {
            //a::* for import
            if (peekt == TokenKind.star) {
                break;
            }
            
            consume();
            IdExpr idexpr = idExpr();
            idexpr.setNamespace(e);
            e = idexpr;
        }
        
        return e;
    }

//////////////////////////////////////////////////////////////////////////
// TypeDef
//////////////////////////////////////////////////////////////////////////
    
    private ArrayList<GenericParamDef> tryGenericParamDef() {
        if (curt == TokenKind.dollar && !peek.whitespace && peekt == TokenKind.lt) {
            consume();
            consume();
            ArrayList<GenericParamDef> gparams = new ArrayList<GenericParamDef>();
            while (curt != TokenKind.eof) {
                Loc gloc = curLoc();
                String paramName = consumeId();
                GenericParamDef param = new GenericParamDef();
                param.name = paramName;
                //param.parent = parent;
                //param.index = gparams.size();
                
                if (curt == TokenKind.colon) {
                    consume();
                    param.bound = this.typeRef();
                }
                else {
                    param.bound = Type.defaultGenericParamType(gloc);
                }
                
                endLoc(param, gloc);
                gparams.add(param);
                if (curt == TokenKind.comma) {
                    consume();
                    continue;
                } else if (curt == TokenKind.gt) {
                    consume();
                    break;
                } else {
                    err("Error token: " + curt);
                    consume();
                }
            }
            return gparams;
        }
        return null;
    }
    
    /**
     ** TypeDef:
     **   <typeDef> :=  <classDef> | <mixinDef> | <enumDef>
     **
     **   <classDef> :=  <classHeader> <classBody>
     **   <classHeader> := [<doc>] <facets> <typeFlags> "class" [<inheritance>]
     **   <classFlags> := [<protection>] ["abstract"] ["virtual"]
     **   <classBody> := "{" <slotDefs> "}"
     **   <enumDef> :=  <enumHeader> * <enumBody>
     **   <enumHeader> := [<doc>] <facets> <protection> "enum" [<inheritance>]
     **   <enumBody> := "{" <enumDefs> <slotDefs> "}" * * <facetDef :=
     **   <mixinHeader> := [<doc>] <facets> <protection> "mixin" [<inheritance>]
     **   <mixinBody> := "{" <slotDefs> "}" 
     **   <protection> := "public" | "protected" | "private" | "internal"
     **   <inheritance> := ":" <typeList>*
     */
    TypeDef typeDef(Comments doc, int flags) {
        // local working variables
        Loc loc = curLoc();
        boolean isMixin = false;
        boolean isEnum = false;

        TypeDef typeDef = null;
        // mixin
        if (curt == TokenKind.traitKeyword) {
            if ((flags & FConst.Abstract) != 0) {
                //err("The 'abstract' modifier is implied on trait");
            }
            //flags = flags | FConst.Mixin | FConst.Abstract;
            flags = flags | FConst.Abstract;
            isMixin = true;
            consume();
            
            // name
            String name = consumeId();
            // lookup TypeDef
            typeDef = new TypeDef(doc, flags, name);
            typeDef.kind = TypeDef.Kind.Tarit;
        }
        else if (curt == TokenKind.enumKeyword) {
            if ((flags & FConst.Const) != 0) {
                err("The 'const' modifier is implied on enum");
            }
            if ((flags & FConst.Abstract) != 0) {
                err("Cannot use 'abstract' modifier on enum");
            }
            //flags = flags | FConst.Enum;
            isEnum = true;
            consume();
            
            // name
            String name = consumeId();
            // lookup TypeDef
            typeDef = new TypeDef(doc, flags, name);
            typeDef.kind = TypeDef.Kind.Enum;
            
            if (curt == TokenKind.colon) {
                consume();
                Type first = inheritType();
                typeDef.enumBase = first;
            }
        } // class
        else {
            consume(TokenKind.structKeyword);
            //flags = flags | FConst.Struct;
            // name
            String name = consumeId();
            // lookup TypeDef
            typeDef = new TypeDef(doc, flags, name);
            
            //GenericType Param
            typeDef.generiParamDefs = tryGenericParamDef();
            typeDef.kind = TypeDef.Kind.Struct;
        }


        if (!isMixin && !isEnum) {
            // inheritance
            if (curt == TokenKind.colon) {
                // first inheritance type can be extends or mixin
                consume();
                Type first = inheritType();
                typeDef.inheritances = new ArrayList<Type>();
                typeDef.inheritances.add(first);

                // additional mixins
                while (curt == TokenKind.comma) {
                    consume();
                    typeDef.inheritances.add(inheritType());
                }
            }
        }

        // start class body
        consume(TokenKind.lbrace);

        // if enum, parse values
        if (isEnum) {
            enumDefs(typeDef);
        }
        else {
            // slots
            while (curt != TokenKind.eof) {
                Comments sdoc = this.doc();
                if (curt == TokenKind.rbrace) {
                    break;
                }
                AstNode slot = slotDef(sdoc);
                if (slot == null) {
                    recoverToSlotDef();
                    continue;
                }
                if (isMixin) {
                    if (slot instanceof FuncDef) {
                        typeDef.addSlot((FuncDef)slot);
                    }
                    else {
                        err("Cannot define field in trait");
                    }
                }
                else {
                    if (slot instanceof FieldDef) {
                        if (typeDef.funcDefs.size() > 0) {
                            err("Field should come before methods");
                        }
                    }
                    typeDef.addSlot(slot);
                }
            }
        }

        // end of class body
        consume(TokenKind.rbrace);
        endLoc(typeDef, loc);

        return typeDef;
    }

    private Type inheritType() {
        Type t = simpleType();
        return t;
    }

//////////////////////////////////////////////////////////////////////////
// Flags
//////////////////////////////////////////////////////////////////////////
    
    private int flags() {
//    Loc loc = cur.loc;
        int flags = 0;
        boolean protection = false;
        for (boolean done = false; !done;) {
            int oldFlags = flags;
            switch (curt) {
                case abstractKeyword:
                    flags = flags | (FConst.Abstract);
                    break;
//                case constKeyword:
//                    flags = flags | (FConst.Const);
//                    break;
                case readonlyKeyword:
                    flags = flags | (FConst.Readonly);
                    break;
//                case finalKeyword:
//                    flags = flags | (AstNode.Final);
//                    break;
//                case internalKeyword:
//                    flags = flags | (AstNode.Internal);
//                    protection = true;
//                    break;
                case externKeyword:
                    flags = flags | (FConst.Extern);
                    break;
                case externcKeyword:
                    flags = flags | (FConst.ExternC);
                    break;
                case extensionKeyword:
                    flags = flags | (FConst.Extension);
                    break;
                case overrideKeyword:
                    flags = flags | (FConst.Override);
                    break;
                case privateKeyword:
                    flags = flags | (FConst.Private);
                    protection = true;
                    break;
                case protectedKeyword:
                    flags = flags | (FConst.Protected);
                    protection = true;
                    break;
                case publicKeyword:
                    flags = flags | (FConst.Public);
                    protection = true;
                    break;
                case staticKeyword:
                    flags = flags | (FConst.Static);
                    break;
                case virtualKeyword:
                    flags = flags | (FConst.Virtual);
                    break;
                //case TokenKind.rtconstKeyword:   flags = flags.or(FConst.RuntimeConst)
                case asyncKeyword:
                    flags = flags | (FConst.Async);
                    break;
                case reflectKeyword:
                    flags = flags | (FConst.Reflect);
                    break;
                case unsafeKeyword:
                    flags = flags | (FConst.Unsafe);
                    break;
                case throwKeyword:
                    flags = flags | (FConst.Throws);
                    break;
                case inlineKeyword:
                    flags = flags | (FConst.Inline);
                    break;
                case packedKeyword:
                    flags = flags | (FConst.Packed);
                    break;
                case constexprKeyword:
                    flags = flags | (FConst.ConstExpr);
                    break;
                case operatorKeyword:
                    flags = flags | (FConst.Operator);
                    break;
                case noncopyableKeyword:
                    flags = flags | (FConst.Noncopyable);
                    break;
                default:
                    done = true;
            }
            if (done) {
                break;
            }
            if (oldFlags == flags) {
                err("Repeated modifier");
            }
            oldFlags = flags;
            consume();
        }

        return flags;
    }
    
    private void funcPostFlags(FuncPrototype prototype) {

        for (boolean done = false; !done;) {
            if (cur.newline) {
                break;
            }
            switch (curt) {
                case constKeyword:
                    prototype._isImmutable = true;
                    prototype._explicitImmutability = true;
                    break;
                case mutableKeyword:
                    prototype._isImmutable = false;
                    prototype._explicitImmutability = true;
                    break;
                case staticKeyword:
                    prototype._isStaticClosure = true;
                    break;
                default:
                    done = true;
            }
            if (done) {
                break;
            }
            consume();
        }
    }

//////////////////////////////////////////////////////////////////////////
// Enum
//////////////////////////////////////////////////////////////////////////
    
    /**
     ** Enum definition list:
     **   <enumDefs> :=  <enumDef> ("," <enumDef>)* <eos>
     */
    private void enumDefs(TypeDef def) {
        // parse each enum def
        int ordinal = 0;
        def.addSlot(enumSlotDef(ordinal++));
        while (curt == TokenKind.comma) {
            consume();
            FieldDef enumDef = enumSlotDef(ordinal++);
            def.addSlot(enumDef);
        }
        endOfStmt();
    }

    /**
     ** Enum definition:
     **   <enumDef> :=  <facets> <id> ["(" <args> ")"]*
     */
    private FieldDef enumSlotDef(int ordinal) {
        Comments doc = doc();
        Loc loc = curLoc();
        FieldDef def = new FieldDef(doc, consumeId());

        // optional ctor args
        if (curt == TokenKind.assign) {
            consume(TokenKind.assign);
            if (curt == TokenKind.dotDotDot) {
                consume();
                def.unkonwInit = true;
            }
            else {
                def.initExpr = expr();
            }
        }

        endLoc(def, loc);
        return def;
    }

//////////////////////////////////////////////////////////////////////////
// Deep parser
//////////////////////////////////////////////////////////////////////////
    /**
     ** Top level for blocks which must be surrounded by braces
     */
    Block block() {
        consume(TokenKind.lbrace);
        int deep = 1;
        while (deep > 0 && curt != TokenKind.eof) {
            if (curt == TokenKind.rbrace) {
                --deep;
            } else if (curt == TokenKind.lbrace) {
                ++deep;
            }
            consume();
        }
        return null;
    }

    private boolean skipBracket() {
        return skipBracket(true);
    }

    private boolean skipBracket(boolean brace) {
        boolean success = false;
        if (curt == TokenKind.lparen) {
            consume();
            int deep = 1;
            while (deep > 0 && curt != TokenKind.eof) {
                if (curt == TokenKind.rparen) {
                    --deep;
                } else if (curt == TokenKind.lparen) {
                    ++deep;
                }
                consume();
            }
            success = true;
        }
        if (brace && curt == TokenKind.lbrace) {
            consume();
            int deep = 1;
            while (deep > 0 && curt != TokenKind.eof) {
                if (curt == TokenKind.rbrace) {
                    --deep;
                } else if (curt == TokenKind.lbrace) {
                    ++deep;
                }
                consume();
            }
            success = true;
        }
        return success;
    }

    private static boolean isExprValue(TokenKind t) {
        switch (t) {
            case identifier:
            case intLiteral:
            case strLiteral:
            case floatLiteral:
            case trueKeyword:
            case falseKeyword:
            case thisKeyword:
            case superKeyword:
            //case itKeyword:
            case nullKeyword:
                return true;
        }
        return false;
    }

    private static boolean isJoinToken(TokenKind t) {
        switch (t) {
            case dot://        ("."),
            case colon://         (":"),
            case doubleColon://   ("::"),
            case plus://          ("+"),
            case minus://         ("-"),
            case star://          ("*"),
            case slash://         ("/"),
            case percent://       ("%"),
            case pound://         ("#"),
            case increment://     ("++"),
            case decrement://     ("--"),
            case isKeyword://,
//      case isnotKeyword://,
            case asKeyword://,
            case tilde://         ("~"),
            case pipe://          ("|"),
            case amp://           ("&"),
            case caret://         ("^"),
            case at://            ("@"),
            case doublePipe://    ("||"),
            case doubleAmp://     ("&&"),
            case same://          ("==="),
            case notSame://       ("!=="),
            case eq://            ("=="),
            case notEq://         ("!="),
            case cmp://           ("<=>"),
            case lt://            ("<"),
            case ltEq://          ("<="),
            case gt://            (">"),
            case gtEq://          (">="),
            case dotDot://        (".."),
            case dotDotLt://      ("..<"),
            case arrow://         ("->"),
            case tildeArrow://    ("~>"),
            case elvis://         ("?:"),
            case safeDot://       ("?."),
            case safeArrow://     ("?->"),
            case safeTildeArrow://("?~>"),
                return true;
        }
        return false;
    }

    Expr expr() {
        while (curt != TokenKind.eof) {
            if (isExprValue(curt)) {
                consume();
                skipBracket();
                if (isExprValue(curt)) {
                    break;
                }
                continue;
            }

            if (isJoinToken(curt)) {
                consume();
                if (skipBracket()) {
                    if (isExprValue(curt)) {
                        break;
                    }
                }
                continue;
            }
            break;
        }
        return null;
    }

//////////////////////////////////////////////////////////////////////////
// FieldDef
//////////////////////////////////////////////////////////////////////////
    /**
     ** Field definition:
     **   <fieldDef> :=  <facets> <fieldFlags> <id> ":" [<type>] ["=" <expr>] eos
     **   <fieldFlags> := [<protection>] ["readonly"] ["static"]
     */
    private FieldDef fieldDef(Loc loc, Comments doc, int flags, Type type, String name, boolean isConst) {
        // define field itself
        FieldDef field = new FieldDef(doc, name);
        if (isConst) {
            flags |= FConst.Const;
        }
        field.flags = flags;
        field.fieldType = type;
        
        if (curt == TokenKind.colon) {
            consume();
            field.fieldType = typeRef();
            field.fieldType.initDefaultImmutability(isConst ? Type.FieldType.Const : Type.FieldType.Var);
        }

        // field initializer
        if (curt == TokenKind.assign) {
            //if (curt === TokenKind.assign) err("Must use := for field initialization")
            consume();
            if (curt == TokenKind.dotDotDot) {
                consume();
                field.unkonwInit = true;
            }
            else {
                field.initExpr = expr();
            }
        }

        // disable type inference for now - doing inference for literals is
        // pretty trivial, but other types is tricky;  I'm not sure it is such
        // a hot idea anyways so it may just stay disabled forever
        if (field.fieldType == null) {
            err("Type inference not supported for top-level fields");
        }

        endOfStmt();
        endLoc(field, loc);
        return field;
    }

//////////////////////////////////////////////////////////////////////////
// MethodDef
//////////////////////////////////////////////////////////////////////////
    /**
     ** Method definition:
     **   <methodDef> :=  <facets> <methodFlags> <type> <id> "(" <params> ")"
     * <methodBody>
     **   <methodFlags> := [<protection>] ["virtual"] ["override"] ["abstract"]
     * ["static"]
     **   <params> := [<param> ("," <param>)*]
     **   <param> :=  <type> <id> [":=" <expr>]
     **   <methodBody> :=  <eos> | ( "{" <stmts> "}" )*
     */
    private FuncDef methodDef(Loc loc, Comments doc, int flags, Type ret, String name) {
        FuncDef method = new FuncDef();
        method.loc = loc;
        method.comment = doc;
        method.flags = flags;
        method.prototype.returnType = ret;
        method.name = name;
        method.generiParamDefs = tryGenericParamDef();
        
        funcPrototype(method.prototype);
        method.prototype.funcDef = method;
        if ((flags & FConst.Ctor) != 0) {
            method.prototype._isImmutable = false;
        }

        // if This is returned, then we configure inheritedRet
        // right off the bat (this is actual signature we will use)
        //if (ret.isThis) method.inheritedRet = parent.asRef
        // if no body expected
        //if (parent.isNative) flags = flags.or(FConst.Native)
        if (curt == TokenKind.lbrace) {
            method.code = block();  // keep parsing
        } else {
            endOfStmt();
        }

        endLoc(method, loc);
        return method;
    }
    
    protected void funcPrototype(FuncPrototype prototype) {
        consume(TokenKind.lparen);
        if (curt != TokenKind.rparen) {
            prototype.paramDefs = new ArrayList<FieldDef>();
            while (curt != TokenKind.eof) {
                FieldDef newParam = paramDef();
                prototype.paramDefs.add(newParam);
                if (curt == TokenKind.rparen) {
                    break;
                }
                consume(TokenKind.comma);
            }
        }
        consume(TokenKind.rparen);
        
        funcPostFlags(prototype);
        
        if (curt == TokenKind.colon) {
            consume();
            prototype.returnType = typeRef();
            prototype.returnType.initDefaultImmutability(Type.FieldType.ReturnType);
        }
        else {
            prototype.returnType = Type.voidType(cur.loc);
        }
    }

    private FieldDef paramDef() {
        Loc loc = curLoc();

        FieldDef param = new FieldDef(consumeId(), null);
        param.isParamDef = true;
        
        consume(TokenKind.colon);
        
        if (curt == TokenKind.dotDotDot) {
            param.fieldType = Type.varArgType(cur.loc);
            consume();
        }
        else {
            param.fieldType = typeRef();
            param.fieldType.initDefaultImmutability(Type.FieldType.Param);
        }
        
        //param type default to const
//        if (!param.fieldType.explicitImmutable) {
//            param.fieldType.isImmutable = true;
//        }
        
        if (curt == TokenKind.assign) {
            //if (curt === TokenKind.assign) err("Must use := for parameter default");
            consume();
            param.initExpr = expr();
        }
        endLoc(param, loc);
        return param;
    }

//////////////////////////////////////////////////////////////////////////
// Types
//////////////////////////////////////////////////////////////////////////

    /**
     ** Type signature:
     **   <type> :=  <simpleType> | <pointerType> | <funcType> | <arrayType> | <constType>
     */
    protected Type typeRef() {
        Loc loc = curLoc();
        Type type;
        switch (curt) {
            case ownKeyword:
                consume();
                return pointerType(Type.PointerAttr.own);
            case uniqKeyword:
                consume();
                return pointerType(Type.PointerAttr.uniq);
            case rawKeyword:
                consume();
                return pointerType(Type.PointerAttr.raw);
//            case refKeyword:
//                consume();
//                return pointerType(Type.PointerAttr.ref);
//            case weakKeyword:
//                consume();
//                return pointerType(Type.PointerAttr.weak);
            case amp: {
                consume();
                Type stype = typeRef();
                stype.isReference = true;
                endLoc(stype, loc);
                return stype;
            }
            case star:
                return pointerType(Type.PointerAttr.ref);
            case constKeyword:
                return imutableType();
//            case mutableKeyword:
//                return imutableType();
            case lbracket:
                return arrayType();
            default:
                break;
        }
        
        return funcOrSimpleType();
    }
    
    private Type pointerType(Type.PointerAttr pointerAttr) {
        Loc loc = curLoc();
        consume(TokenKind.star);
        boolean isNullable = false;
        if (curt == TokenKind.question) {
            consume(TokenKind.question);
            isNullable = true;
        }
        
        Type type = typeRef();
        type = Type.pointerType(loc, type, pointerAttr, isNullable);
        endLoc(type, loc);
        return type;
    }
    
    private Type arrayType() {
        Loc loc = curLoc();
        consume(TokenKind.lbracket);
        Expr size = null;
        if (curt != TokenKind.rbracket) {
            size = expr();
        }
        consume(TokenKind.rbracket);
        
        Type type = typeRef();
        type = Type.arrayType(loc, type, size);
        endLoc(type, loc);
        return type;
    }
    
    private Type imutableType() {
        boolean imutable = false;
        boolean explicitImutable = false;
        if (curt == TokenKind.constKeyword) {
            consume();
            imutable = true;
            explicitImutable = true;
        }
//        else if (curt == TokenKind.mutableKeyword) {
//            consume();
//            imutable = false;
//            explicitImutable = true;
//        }
        
        Type type = typeRef();
        type.explicitImmutability = explicitImutable;
        type.isImmutable = imutable;
        return type;
    }

    private Type funcOrSimpleType() {
        if (curt == TokenKind.funKeyword) {
            return funcType();
        }
        return simpleType();
    }

    /**
     ** Simple type signature:
     **   <simpleType> :=  <id> ["::" <id>]*
     */
    private Type simpleType() {
        Loc loc = cur.loc;
        IdExpr id = idExpr();

        Type type = null;
        
        //type name rewrite
        if (id.namespace == null) {
            Type ntype = null;
            switch (id.name) {
                case "Int8":
                    ntype = Type.intType(loc);
                    ((Type.NumInfo)ntype.detail).size = 8;
                    break;
                case "Int16":
                    ntype = Type.intType(loc);
                    ((Type.NumInfo)ntype.detail).size = 16;
                    break;
                case "Int32":
                    ntype = Type.intType(loc);
                    ((Type.NumInfo)ntype.detail).size = 32;
                    break;
                case "Int64":
                    ntype = Type.intType(loc);
                    ((Type.NumInfo)ntype.detail).size = 64;
                    break;
                case "UInt8":
                    ntype = Type.intType(loc);
                    ((Type.NumInfo)ntype.detail).size = 8;
                    ((Type.NumInfo)ntype.detail).isUnsigned = true;
                    break;
                case "UInt16":
                    ntype = Type.intType(loc);
                    ((Type.NumInfo)ntype.detail).size = 16;
                    ((Type.NumInfo)ntype.detail).isUnsigned = true;
                    break;
                case "UInt32":
                    ntype = Type.intType(loc);
                    ((Type.NumInfo)ntype.detail).size = 32;
                    ((Type.NumInfo)ntype.detail).isUnsigned = true;
                    break;
                case "UInt64":
                    ntype = Type.intType(loc);
                    ((Type.NumInfo)ntype.detail).size = 64;
                    ((Type.NumInfo)ntype.detail).isUnsigned = true;
                    break;
                case "Float32":
                    ntype = Type.floatType(loc);
                    ((Type.NumInfo)ntype.detail).size = 32;
                    break;
                case "Float64":
                    ntype = Type.floatType(loc);
                    ((Type.NumInfo)ntype.detail).size = 64;
                    break;
                case "Int":
                    ntype = Type.intType(loc);
                    break;
                case "Float":
                    ntype = Type.floatType(loc);
                    break;
            }
            if (ntype != null) {
                type = ntype;
            }
        }
        
        if (type == null) {
            type = new Type(id);
        }

        //generic param
        if (curt == TokenKind.dollar && !peek.whitespace && peekt == TokenKind.lt) {
            type.genericArgs = genericArgs();
        }

        // got it
        endLoc(type, loc);
        return type;
    }
    
    protected ArrayList<Type> genericArgs() {
        if (peek.whitespace) {
            err("Expected $<");
        }
        consume(TokenKind.dollar);
        consume(TokenKind.lt);
        ArrayList<Type> params = new ArrayList<Type>();
        while (curt != TokenKind.eof) {
            Type type1 = typeRef();
            params.add(type1);
            if (curt == TokenKind.gt) {
                break;
            }
            consume(TokenKind.comma);
        }
        consume(TokenKind.gt);
        return params;
    }

    /**
     ** Method type signature:
     **   <funcType> := "fun" "(" <args> ")" [<type>]
     */
    private Type funcType() {
        Loc loc = cur.loc;
        
        consume(TokenKind.funKeyword);
        
        FuncPrototype prototype = new FuncPrototype();
        funcPrototype(prototype);
       
        Type t = Type.funcType(loc, prototype);

        endLoc(t, loc);
        return t;
    }

//////////////////////////////////////////////////////////////////////////
// Misc
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse fandoc or return null
     *
     */
    private Comments doc() {
        Loc loc0 = cur.loc;
        Comments comments = null;
        while (curt == TokenKind.docComment || curt == TokenKind.cmdComment) {
            Loc loc = cur.loc;
            TokenKind kind = curt;
            String lines = (String) consume().val;
            Comment doc = new Comment(lines, kind);
            if (comments == null) {
                comments = new Comments();
                comments.loc = loc;
            }
            comments.comments.add(doc);
            endLoc(doc, loc);
        }
        if (comments != null) {
            endLoc(comments, loc0);
        }
        return comments;
    }

//////////////////////////////////////////////////////////////////////////
// Errors
//////////////////////////////////////////////////////////////////////////
    
    CompilerErr err(String msg) {
        return log.err(msg, cur.loc);
    }

//////////////////////////////////////////////////////////////////////////
// Tokens
//////////////////////////////////////////////////////////////////////////
    /**
     ** Verify current is an identifier, consume it, and return it.
     *
     */
    protected String consumeId() {
        if (curt != TokenKind.identifier) {
            err("Expected identifier, not '"+cur+"'");
            //consume();
            return "";
        }
        return (String) consume().val;
    }

    /**
     ** Check that the current token matches the specified * type, but do not
     * consume it.
     *
     */
    private void verify(TokenKind kind) {
        if (!curt.equals(kind)) {
            err("Expected '"+kind.symbol+"', not '"+cur+"'");
        }
    }

    /**
     ** Consume the current token and return consumed token. * If kind is
     * non-null then verify first
  *
     */
    protected Token consume() {
        return consume(null);
    }

    protected Token consume(TokenKind kind) {
        // verify if not null
        if (kind != null) {
            verify(kind);
        }

        // save the current we are about to consume for return
        Token result = cur;

        // get the next token from the buffer, if pos is past numTokens,
        // then always use the last token which will be eof
        Token next;
        pos++;
        if (pos + 1 < numTokens) {
            next = tokens.get(pos + 1);  // next peek is cur+1
        } else {
            next = tokens.get(numTokens - 1);
        }

        this.cur = peek;
        this.peek = next;
        this.curt = cur.kind;
        this.peekt = peek.kind;

        return result;
    }

    //** next next token
    protected Token peekpeek() {
        if (pos + 2 < numTokens) {
            return tokens.get(pos + 2);
        }
        return tokens.get(numTokens - 1);
    }

    /**
     ** update loc.len field
     *
     */
    protected void endLoc(AstNode node, Loc loc) {
        node.loc = loc;
        
        Token preToken = (pos > 0 && pos<numTokens) ? tokens.get(pos - 1) : cur;
        int end = preToken.loc.offset + preToken.len;
        int begin = loc.offset;
        int len = end - begin;
        
        if (len <= 0) {
            return;
        }
        node.len = len;
    }

    /**
     ** Statements can be terminated with a semicolon
     */
    protected void endOfStmt() {
        String errMsg = "Expected end of statement with : semicolon, newline, or end of block; not '" + cur + "'";
        endOfStmt(errMsg);
    }
    
    protected boolean endOfStmt(String errMsg)
    {
      if (cur.newline) return true;
      if (curt == TokenKind.semicolon) { consume(); return true; }
      if (curt == TokenKind.rbrace) return true;
      if (curt == TokenKind.eof) return true;
      if (errMsg == null) return false;
      err(errMsg);
      return false;
    }

    /**
     ** Reset the current position to the specified tokens index.
  *
     */
    protected void reset(int pos) {
        this.pos = pos;
        this.cur = tokens.get(pos);
        if (pos + 1 < numTokens) {
            this.peek = tokens.get(pos + 1);
        } else {
            this.peek = tokens.get(pos);
        }
        this.curt = cur.kind;
        this.peekt = peek.kind;
    }
}
