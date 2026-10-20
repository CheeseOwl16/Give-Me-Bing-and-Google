package dev.cheeseowl.gmbg;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-local causality, not stack traces or cross-thread attribution. */
final class Trace {
    enum Kind { USER, WRITE, PARSE, PROPAGATE, BROWSER, QUERY, TEMPLATE, ITEM, ENCODE,
        COMPLETION, UI_SEARCH, UI_EXECUTE, WEB_DIRECT, WEB_PREPARE, SUGGEST_JS, SUGGEST_NATIVE, BUSINESS_DISPATCH, NATIVE_ROW, HISTORY_RECEIVE }
    enum Source { USER, CONFIG, USER_CONFIG, OTHER }
    record Frame(long id, long parent, Kind kind) {}
    private final AtomicLong calls = new AtomicLong();
    private final ThreadLocal<ArrayDeque<Frame>> frames = ThreadLocal.withInitial(ArrayDeque::new);
    Frame enter(Kind kind) {
        ArrayDeque<Frame> stack = frames.get();
        Frame frame = new Frame(calls.incrementAndGet(), stack.isEmpty() ? 0 : stack.peek().id(), kind);
        stack.push(frame);
        return frame;
    }
    Source source() {
        boolean user = false, config = false;
        for (Frame f : frames.get()) { user |= f.kind() == Kind.USER; config |= f.kind() == Kind.PARSE; }
        return user ? (config ? Source.USER_CONFIG : Source.USER) : (config ? Source.CONFIG : Source.OTHER);
    }
    void exit(Frame frame) {
        ArrayDeque<Frame> stack = frames.get();
        if (!stack.isEmpty() && stack.peek() == frame) stack.pop();
        else stack.clear(); // Uncertain causality must not leak into later calls.
        if (stack.isEmpty()) frames.remove();
    }
}
