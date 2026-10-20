package dev.cheeseowl.gmbg;

import io.github.libxposed.api.XposedInterface;

/** Bounded receiver-domain adapter, separately armed and exception-isolated. */
final class HistoryDomain {
    private final HistoryHost host;private final HistoryScope scope=new HistoryScope();private final DiagnosticLog log;
    private volatile boolean armed;
    HistoryDomain(ClassLoader loader,DiagnosticLog log) throws ReflectiveOperationException {host=new HistoryHost(loader);this.log=log;}
    void arm(boolean value){armed=value;}
    private HistoryScope.Delivery read(Object receiver,Object intent){try{return host.read(receiver,intent);}catch(Throwable ignored){return null;}}
    private HistoryScope.Delivery fresh(){var d=scope.delivery();return d==null?null:read(d.receiver(),d.intent());}
    private void event(Trace.Frame frame,HistoryScope.Reason reason,HistoryScope.Source source){
        log.emit(DiagnosticLog.Event.HISTORY_SCOPE,frame!=null && frame.kind()==Trace.Kind.QUERY?DiagnosticLog.Hook.QUERY:DiagnosticLog.Hook.HISTORY_RECEIVE,
            DiagnosticLog.Phase.STATUS,frame,reason,source);
    }
    Object receive(XposedInterface.Chain chain,Trace.Frame frame,Around.Call original) throws Throwable {
        if(!armed)return original.invoke();
        var delivery=read(chain.getThisObject(),chain.getArg(1));var entry=scope.enter(delivery,frame);
        try{event(frame,entry.reason(),HistoryScope.source(delivery==null?null:delivery.source()));return original.invoke();}
        finally{scope.exit(entry.token());}
    }
    HistoryScope.Claim claim(Object context,String query,String source,String direct,boolean ai,Trace.Frame frame){
        var claim=armed?scope.claim(context,query,source,direct,ai,frame,fresh()):new HistoryScope.Claim(null,HistoryScope.Reason.NO_CONTEXT);
        event(frame,claim.reason(),HistoryScope.source(source));return claim;
    }
    boolean live(HistoryScope.Permit permit){return armed && scope.live(permit,fresh());}
}
