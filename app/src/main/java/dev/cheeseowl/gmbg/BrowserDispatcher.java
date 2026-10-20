package dev.cheeseowl.gmbg;

/** One replacement request at most. Original executes once on skip/failure, never after normal start return. */
final class BrowserDispatcher {
    enum Outcome { SKIPPED, REENTRANT, START_REQUESTED, START_FAILED }
    interface Launch { void start() throws Throwable; }
    interface Reporter { void report(Outcome outcome); }
    private final ThreadLocal<Boolean> active=new ThreadLocal<>();
    Object run(boolean eligible,Launch launch,Around.Call original,Reporter reporter) throws Throwable {
        if(!eligible){emit(reporter,Outcome.SKIPPED);return original.invoke();}
        if(Boolean.TRUE.equals(active.get())){emit(reporter,Outcome.REENTRANT);return original.invoke();}
        active.set(true);
        try {
            launch.start();emit(reporter,Outcome.START_REQUESTED);return null;
        }catch(Throwable ignored){emit(reporter,Outcome.START_FAILED);}
        finally{active.remove();}
        return original.invoke();
    }
    private static void emit(Reporter reporter,Outcome outcome){try{reporter.report(outcome);}catch(Throwable ignored){}}
}
