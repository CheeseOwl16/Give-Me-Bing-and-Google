package dev.cheeseowl.gmbg;

/** All observer failures are isolated. The original result/throwable passes through exactly once. */
final class Around {
    interface Call { Object invoke() throws Throwable; }
    interface After { void accept(Object result, boolean threw); }
    static Object run(Runnable before, Call original, After after, Runnable cleanup) throws Throwable {
        try { before.run(); } catch (Throwable ignored) { }
        Object result = null;
        boolean threw = true;
        try {
            result = original.invoke();
            threw = false;
            return result;
        } finally {
            try { after.accept(result, threw); } catch (Throwable ignored) { }
            try { cleanup.run(); } catch (Throwable ignored) { }
        }
    }
    private Around() {}
}
