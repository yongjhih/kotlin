package foo

open class A(val x: Int, val y: Int) {
    inner class B(val z: Int) {
        fun foo() = x + y + z
    }
}

fun box(): Boolean {
    val a = A(2, 3)
    val b = a.B(4)
    return b.foo() == 9
}

