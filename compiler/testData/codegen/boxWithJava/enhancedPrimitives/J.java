public class J {
    public static String test(@org.jetbrains.annotations.NotNull Integer x) {
        if (x == 1) return "OK";
        throw new RuntimeException("fail");
    }
}
