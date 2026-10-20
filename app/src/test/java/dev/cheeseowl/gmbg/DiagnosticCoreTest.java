package dev.cheeseowl.gmbg;

import org.junit.Test;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class DiagnosticCoreTest {
    @Test public void nestedWriteSourcesAndParents() {
        Trace t=new Trace();
        Trace.Frame parse=t.enter(Trace.Kind.PARSE);
        Trace.Frame write=t.enter(Trace.Kind.WRITE);
        assertEquals(parse.id(),write.parent()); assertEquals(Trace.Source.CONFIG,t.source());
        t.exit(write); t.exit(parse); assertEquals(Trace.Source.OTHER,t.source());
        Trace.Frame user=t.enter(Trace.Kind.USER);
        assertEquals(Trace.Source.USER,t.source());
        Trace.Frame nested=t.enter(Trace.Kind.PARSE);
        assertEquals(Trace.Source.USER_CONFIG,t.source());
        t.exit(nested); t.exit(user); assertEquals(Trace.Source.OTHER,t.source());
    }
    @Test public void attributionDoesNotCrossThreads() throws Exception {
        Trace t=new Trace(); Trace.Frame parent=t.enter(Trace.Kind.USER);
        AtomicReference<Trace.Source> other=new AtomicReference<>();
        Thread thread=new Thread(() -> other.set(t.source())); thread.start(); thread.join();
        assertEquals(Trace.Source.OTHER,other.get()); t.exit(parent);
    }
    @Test public void mismatchedCleanupDiscardsUncertainContext() {
        Trace t=new Trace(); Trace.Frame a=t.enter(Trace.Kind.PARSE); t.enter(Trace.Kind.WRITE);
        t.exit(a); assertEquals(Trace.Source.OTHER,t.source());
        Trace.Frame b=t.enter(Trace.Kind.BROWSER); assertEquals(0,b.parent()); t.exit(b);
    }
    @Test public void resultIdentityAndSingleExecutionSurviveObserverFailures() throws Throwable {
        Object expected=new Object(); AtomicInteger calls=new AtomicInteger(); AtomicInteger clean=new AtomicInteger();
        AtomicReference<Object> observed=new AtomicReference<>(); AtomicReference<Boolean> failed=new AtomicReference<>();
        Object result=Around.run(() -> { throw new IllegalStateException("private before"); },
            () -> { calls.incrementAndGet(); return expected; },
            (r,threw) -> { observed.set(r); failed.set(threw); throw new RuntimeException("private after"); },
            () -> { clean.incrementAndGet(); throw new RuntimeException(); });
        assertSame(expected,result); assertEquals(1,calls.get()); assertEquals(1,clean.get());
        assertSame(expected,observed.get()); assertEquals(Boolean.FALSE,failed.get());
    }
    @Test public void originalThrowableIdentityAndCleanupPreserved() {
        Error original=new AssertionError("sensitive target message"); AtomicInteger calls=new AtomicInteger();
        AtomicInteger clean=new AtomicInteger();
        AtomicReference<Boolean> failed=new AtomicReference<>();
        try {
            Around.run(() -> {},() -> { calls.incrementAndGet(); throw original; },
                (r,threw) -> { failed.set(threw); throw new Error("observer"); },clean::incrementAndGet);
            fail();
        } catch (Throwable got) { assertSame(original,got); }
        assertEquals(1,calls.get()); assertEquals(1,clean.get());
        assertEquals(Boolean.TRUE,failed.get());
    }
    @Test public void normalNullAndVoidPassThrough() throws Throwable {
        AtomicReference<Boolean> failed=new AtomicReference<>();
        assertNull(Around.run(() -> {},() -> null,(r,threw) -> failed.set(threw),() -> {}));
        assertEquals(Boolean.FALSE,failed.get());
    }
    @Test public void callerObjectsAreNotReplacedOrWritten() throws Throwable {
        Object[] args={new Object(),"private query"}; Object first=args[0];
        assertSame(first,Around.run(() -> {},() -> args[0],(r,threw) -> {},() -> {}));
        assertSame(first,args[0]); assertEquals("private query",args[1]);
    }
    @Test public void closedVocabularyRejectsHostileInputs() {
        Object hostile=new Object(){ @Override public String toString(){ throw new AssertionError("must not stringify"); } };
        assertEquals(Privacy.Engine.UNKNOWN,Privacy.engine(hostile));
        assertEquals(Privacy.Engine.OTHER,Privacy.engine("my private custom engine"));
        assertEquals(Privacy.Engine.OTHER,Privacy.engine("https://secret.invalid/?q=private"));
        assertEquals(Privacy.Engine.BING,Privacy.engine("bing"));
        assertEquals(Privacy.Scene.OTHER,Privacy.scene("private scene"));
        assertEquals(Privacy.Action.OTHER,Privacy.action("private action"));
        assertEquals(Privacy.Scheme.OTHER,Privacy.scheme("private scheme"));
        assertEquals(Privacy.Target.OTHER,Privacy.target("my.private.browser"));
        assertEquals(Privacy.Target.MI_FULLSEARCH,Privacy.target("com.android.browser.fullsearch.FullSearchActivity"));
        assertEquals(Privacy.Tri.UNKNOWN,Privacy.tri(hostile));
    }
    @Test public void logNeverStringifiesUnknownObjectsOrExceptions() {
        ArrayList<String> lines=new ArrayList<>(); DiagnosticLog log=new DiagnosticLog(lines::add);
        Object hostile=new Object(){ @Override public String toString(){ throw new Error("secret"); } };
        log.emit(DiagnosticLog.Event.BEGIN,DiagnosticLog.Hook.USER,DiagnosticLog.Phase.BEFORE,null,
            "private query",new RuntimeException("https://secret.invalid"),hostile,Privacy.Engine.CUSTOM,true);
        assertEquals(1,lines.size()); String line=lines.get(0);
        assertFalse(line.contains("private")); assertFalse(line.contains("secret")); assertFalse(line.contains("http"));
        assertTrue(line.contains("v0=UNKNOWN v1=UNKNOWN v2=UNKNOWN v3=CUSTOM v4=TRUE"));
    }
    @Test public void loggingIsBoundedAndSinkFailureContained() {
        ArrayList<String> lines=new ArrayList<>(); DiagnosticLog log=new DiagnosticLog(lines::add,2);
        for(int i=0;i<10;i++) log.emit(DiagnosticLog.Event.BEGIN,DiagnosticLog.Hook.USER,DiagnosticLog.Phase.BEFORE,null);
        assertEquals(3,lines.size()); assertTrue(lines.get(2).contains("event=LIMIT"));
        DiagnosticLog bad=new DiagnosticLog(line -> { throw new Error("sensitive sink"); });
        bad.emit(DiagnosticLog.Event.GATE_OK,DiagnosticLog.Hook.MODULE,DiagnosticLog.Phase.STATUS,null);
    }
}
