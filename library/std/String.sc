//@#include "Str.h"

extern noncopyable struct String {
    fun c_str(): raw* Int8;
    fun size() const: Int;

    operator fun get(i: Int) const: Int8;

    fun iequals(other: ref* String) : Bool;
    fun contains(other: ref* String) : Bool;
    fun startsWith(other: ref* String) : Bool;
    fun endsWith(other: ref* String) : Bool;

    fun find(other: ref* String, start: Int = 0): Int;

    fun replace(src: ref* String, dst: ref* String);
    fun split(sep: ref* String): DArray$<String>;
    fun substr(pos:Int, len:Int = -1): String;

    operator fun plus(other: ref* String) : String;
    fun add(cstr: raw*const Int8);

    fun trimEnd();
    fun trimStart();
    fun trim() { trimStart(); trimEnd(); }
    fun removeLastChar();

    fun toLower(): String;
    fun toUpper(): String;

    fun toInt(): Int32;
    fun toLong(): Int64;
    fun toFloat(): Float32;
    fun toDouble(): Float64;

    fun hashCode() const: Int;
    operator fun compare(p: *String) const : Int;
}

//@extern symbol:sric::String::fromInt
extern fun String_fromInt(i: Int32): String;
extern fun String_fromLong(i: Int64): String;
extern fun String_fromDouble(f: Float64): String;
extern fun String_fromFloat(f: Float): String;

/**
* 'printf' style format
*/
extern fun String_format(format: raw*const Int8, args: ...): String;

extern fun asStr(cstr: raw*const Int8): String;