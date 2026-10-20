package dev.cheeseowl.gmbg;

/** No replacement or native business launch may precede this decision. */
final class DispatchDecision {
    static Object run(boolean valid,Around.Call dispatch,Around.Call nativeReplay) throws Throwable {
        return valid?dispatch.invoke():nativeReplay.invoke();
    }
    private DispatchDecision(){}
}
