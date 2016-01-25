// !DIAGNOSTICS: -USELESS_CAST -UNUSED_PARAMETER

// FILE: j/Base.java
package j;
public interface Base {
    void foo();
}

// FILE: j/Impl.java
package j;

/* package */ abstract class Impl implements Base {
    public void foo() {}
}

// FILE: j/Derived1.java
package j;

public class Derived1 extends Impl {}

// FILE: j/Derived2.java
package j;

public class Derived2 extends Impl {}

// FILE: k/Client.kt
package k

import j.*

val d1 = Derived1()
val d2 = Derived2()

fun <T> select(x1: T, x2: T) = x1
fun <T> selectn(vararg xx: T) = xx[0]

val test1: Base = <!INACCESSIBLE_TYPE!>if (true) d1 else d2<!>
val test2 = if (true) d1 as Base else d2
<!EXPOSED_PROPERTY_TYPE!>val test3 = <!INACCESSIBLE_TYPE!>when { true -> d1; else -> d2 }<!><!>
val test4 = when { true -> d1 as Base; else -> d2 }
<!EXPOSED_PROPERTY_TYPE!>val test5 = <!INACCESSIBLE_TYPE!>select(d1, d2)<!><!>
val test6 = select<Base>(d1, d2)
val test7 = select(d1 as Base, d2)
<!EXPOSED_PROPERTY_TYPE!>val test8 = <!INACCESSIBLE_TYPE!>selectn(d1, d2)<!><!>
val test9 = selectn<Base>(d1, d2)
