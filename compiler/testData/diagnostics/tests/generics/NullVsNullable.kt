// See also comments to KT-10390

fun foo(arg: Any?) {
    arg?.hashCode()
}

fun bar(arg: Any?): Int? = arg?.hashCode()

fun <T> id(arg: T) = arg

open class Base

class Derived : Base()

class Consume<T>(val x: T, val f: (T) -> Unit)

class Printer<T>(val x: T, val f: (T) -> String)

class Mapper<T, R>(val x: T, val f: (T) -> R)

class Transform<T>(val x: T, val f: (T) -> T)

class Produce<T>(val x: T, val f: () -> T)

val c: Consume<Int?> = Consume(null) { y -> foo(y) }

val cc = Consume(null) { y -> foo(<!DEBUG_INFO_CONSTANT!>y<!>) }

val ccc: Consume<Base?> = Consume(Derived()) { y -> foo(y) }

val r: Printer<Int?> = Printer(null) { y -> y.toString() }

val rr = Printer(null) { y -> y.toString() }

val rrr: Printer<Base?> = Printer(Derived()) { y -> y.toString() }

val m: Mapper<Int?, String> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>Mapper(null) { y -> y.toString() }<!>

val mm = Mapper(null) { y -> y.toString() }

val mmm: Mapper<Base?, String> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>Mapper(Derived()) { y -> y.toString() }<!>

val t: Transform<Int?> = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Transform<!>(null) { y -> bar(<!DEBUG_INFO_CONSTANT!>y<!>) }

val tt = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Transform<!>(null) { y -> bar(<!DEBUG_INFO_CONSTANT!>y<!>) }

val ttt: Transform<Base?> = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Transform<!>(Derived()) { y -> bar(y) }

val i: Transform<Int?> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>Transform(null) { y -> id(<!DEBUG_INFO_CONSTANT!>y<!>) }<!>

val ii = Transform(null) { y -> id(<!DEBUG_INFO_CONSTANT!>y<!>) }

val iii: Transform<Base?> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>Transform(Derived()) { y -> id(y) }<!>

val p: Produce<Int?> = Produce(null) { 42 }

val pp = Produce(null) { 42 }

val ppp: Produce<Base?> = Produce(Derived()) { Base() }