package dev.cheeseowl.gmbg;

import java.util.UUID;

/** The logging boundary only accepts enums, booleans and event counters. */
final class DiagnosticLog {
    interface Sink { void write(String line); }
    enum Event { API, GATE_OK, GATE_REJECT, INSTALL_OK, INSTALL_FAIL, BEGIN, END, OBSERVER_FAIL,
        REPAIR_READY, REPAIR, TAKEOVER_READY, TAKEOVER, URL_CHECK, BROWSER_READY, BROWSER_GATE, BROWSER_ROUTE, FEATURE_SWITCH,
        SUGGEST_PATH, SUGGEST_DIAGNOSTICS_READY, NATIVE_READY, NATIVE_SCOPE, HISTORY_READY, HISTORY_SCOPE, LIMIT }
    enum Hook { BOOTSTRAP, USER, WRITE, PARSE, PROPAGATE, BROWSER, QUERY, TEMPLATE, ITEM, ENCODE, MODULE,
        COMPLETION, UI_SEARCH, UI_EXECUTE, WEB_DIRECT, WEB_PREPARE, SUGGEST_JS, SUGGEST_NATIVE,
        ACTIVITY_DISPATCH, FOREGROUND_SERVICE_DISPATCH, SERVICE_DISPATCH, SPECIAL_DISPATCH, NATIVE_ROW, HISTORY_RECEIVE }
    enum Phase { BEFORE, AFTER, STATUS }
    enum Reason { NONE, API, PROCESS, IDENTITY, SIGNATURE, INSTALL, OBSERVER }
    private final Sink sink;
    private final String session = UUID.randomUUID().toString().replace("-", "");
    private long seq;
    private boolean capped;
    private final int limit;
    DiagnosticLog(Sink sink) { this(sink, 2000); }
    DiagnosticLog(Sink sink, int limit) { this.sink = sink; this.limit = limit; }
    synchronized void emit(Event event, Hook hook, Phase phase, Trace.Frame frame, Object... values) {
        if (capped) return;
        if (++seq > limit) { capped = true; safeWrite("QSBP2 s=" + session + " n=" + seq + " event=LIMIT"); return; }
        StringBuilder b = new StringBuilder("QSBP2 s=").append(session).append(" n=").append(seq)
            .append(" event=").append(event.name()).append(" hook=").append(hook.name())
            .append(" phase=").append(phase.name()).append(" call=").append(frame == null ? 0 : frame.id())
            .append(" parent=").append(frame == null ? 0 : frame.parent());
        // Defensive schema: never call toString() on any arbitrary object.
        for (int i = 0; i < values.length; i++) {
            Object v = values[i];
            b.append(" v").append(i).append('=');
            if (v instanceof Enum<?>) b.append(((Enum<?>)v).name());
            else if (v instanceof Boolean) b.append((Boolean)v ? "TRUE" : "FALSE");
            else b.append("UNKNOWN");
        }
        safeWrite(b.toString());
    }
    private void safeWrite(String line) { try { sink.write(line); } catch (Throwable ignored) { /* no host impact */ } }
}
