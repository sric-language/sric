
from cxxheaderparser.types import *
from cxxheaderparser.simple import *
from cxxheaderparser.options import ParserOptions
from cxxheaderparser.preprocessor import make_pcpp_preprocessor
import sys
import dataclasses
import json

"""
parser C++ header file and generaor scric extern api
install: 
  pip install cxxheaderparser
  pip install pcpp
"""
input_file = sys.argv[1]

def onSimpleType(t: Type):
    mapping = {"int":"Int", "float":"Float32", "double":"Float",  "char":"Int8"}
    name = t.format()
    name = mapping.get(name, name)
    print(name, end='')

def onType(t: DecoratedType):
    if isinstance(t, Pointer):
        if t.const:
            print('const ', end='')
        print('raw* ', end='')
        onType(t.ptr_to)
    elif isinstance(t, Array):
        print('[', end='')
        print(t.size.format(), end='')
        print(']', end='')
        onType(t.array_of)
    else:
        onSimpleType(t)

def onName(t: PQName):
    print(t.format(), end='')

def onVar(f: Variable):
    print("var ", end='')
    print(f.name, end='')
    print(" : ", end='')
    onType(f.type)

def onParameter(f: Parameter):
    print(f.name, end='')
    print(" : ", end='')
    onType(f.type)

def onFunc(f: Function):
    if f.doxygen:
        print("/**")
        print(f.doxygen)
        print("*/")
    print("extern fun ", end='')
    onName(f.name)
    print("(", end='')
    i = 0
    for p in f.parameters:
        if i > 0:
            print(', ', end='')
        onParameter(p)
        i += 1
    print(")", end='')

    if f.return_type and f.return_type.format() != "void":
        print(" : ", end='')
        onType(f.return_type)

def onField(f: Field):
    print("var ", end='')
    print(f.name, end='')
    print(" : ", end='')
    onType(f.type)

def onMethod(f: Method):
    if f.doxygen:
        print("/**")
        print(f.doxygen)
        print("*/")
    if f.static:
        print("static ", end='')
    print("fun ", end='')
    onName(f.name)
    print("(", end='')
    i = 0
    for p in f.parameters:
        if i > 0:
            print(', ', end='')
        onParameter(p)
        i += 1
    print(")", end='')

    if f.return_type and f.return_type.format() != "void":
        print(" : ", end='')
        onType(f.return_type)

def onClass(c: ClassScope):
    print("extern struct ", end='')
    onName(c.class_decl.typename)
    print(" {")

    for v in c.fields:
        print("    ", end='')
        onField(v)
        print(";\n")

    for f in c.methods:
        print("    ", end='')
        onMethod(f)
        print(";\n")

    print("}")

preprocessor = make_pcpp_preprocessor(encoding=None, retain_all_content=True)

# with open(input_file, "r", encoding=None) as fp:
#     pp_content = preprocessor(input_file, fp.read())
# sys.stdout.write(pp_content)
# sys.exit(0)

options = ParserOptions(verbose=False, preprocessor=preprocessor)
data = parse_file(input_file, encoding=None, options=options)

# ddata = dataclasses.asdict(data)
# json.dump(ddata, sys.stdout, indent=2)

for v in data.namespace.variables:
    onVar(v)
    print(";\n")

for f in data.namespace.functions:
    onFunc(f)
    print(";\n")

for cs in data.namespace.classes:
    onClass(cs)
    print("\n")
