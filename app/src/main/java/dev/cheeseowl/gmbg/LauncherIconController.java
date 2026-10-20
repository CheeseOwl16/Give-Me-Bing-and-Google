package dev.cheeseowl.gmbg;

// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Cheese owl
/** Pure state logic. The backend changes only this app's launcher alias. */
final class LauncherIconController {
    enum State { VISIBLE, HIDDEN, UNKNOWN }
    interface Backend {
        int read();
        void setVisible(boolean visible);
    }
    static final class Result {
        final State state;
        final boolean success;
        Result(State state, boolean success) { this.state=state; this.success=success; }
    }
    private final Backend backend;
    LauncherIconController(Backend backend) { this.backend=backend; }
    State read() {
        try {
            // PackageManager DEFAULT=0 uses the alias's enabled=true manifest default.
            return switch (backend.read()) {
                case 0, 1 -> State.VISIBLE;
                case 2, 3, 4 -> State.HIDDEN;
                default -> State.UNKNOWN;
            };
        } catch (RuntimeException e) { return State.UNKNOWN; }
    }
    Result setHidden(boolean hidden) {
        if (read()==State.UNKNOWN) return new Result(State.UNKNOWN,false);
        boolean failed=false;
        try { backend.setVisible(!hidden); }
        catch (RuntimeException e) { failed=true; }
        State actual=read();
        return new Result(actual,!failed && actual==(hidden?State.HIDDEN:State.VISIBLE));
    }
}
