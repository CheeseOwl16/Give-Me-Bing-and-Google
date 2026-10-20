package dev.cheeseowl.gmbg;

import io.github.libxposed.api.XposedInterface;

/** Single synchronous authority scope; every original callback still executes once. */
final class NativeSuggestionDomain {
    private final NativeSuggestionHost host;private final NativeSuggestionScope scope=new NativeSuggestionScope();private final DiagnosticLog log;
    private volatile boolean armed;
    NativeSuggestionDomain(ClassLoader loader,DiagnosticLog log) throws ReflectiveOperationException {host=new NativeSuggestionHost(loader);this.log=log;}
    void arm(boolean value){armed=value;}
    private NativeSuggestionScope.Row read(Object listener){try{return host.read(listener);}catch(Throwable ignored){return null;}}
    private NativeSuggestionScope.Row fresh(){NativeSuggestionScope.Row row=scope.current();return row==null?null:read(row.listener());}
    private void event(Trace.Frame frame,NativeSuggestionScope.Reason reason){
        DiagnosticLog.Hook point=frame!=null && frame.kind()==Trace.Kind.QUERY?DiagnosticLog.Hook.QUERY:
            frame!=null && frame.kind()==Trace.Kind.SUGGEST_NATIVE?DiagnosticLog.Hook.SUGGEST_NATIVE:DiagnosticLog.Hook.NATIVE_ROW;
        log.emit(DiagnosticLog.Event.NATIVE_SCOPE,point,DiagnosticLog.Phase.STATUS,frame,reason);
    }
    Object row(XposedInterface.Chain chain,Trace.Frame frame,Around.Call original) throws Throwable {
        if(!armed)return original.invoke();
        NativeSuggestionScope.Entry<NativeSuggestionScope.RowToken> entry=scope.enterRow(read(chain.getThisObject()),frame);
        event(frame,entry.reason());try{return original.invoke();}finally{scope.exitRow(entry.token());}
    }
    Object bridge(XposedInterface.Chain chain,Trace.Frame frame,Around.Call original) throws Throwable {
        if(!armed)return original.invoke();
        NativeSuggestionScope.Entry<NativeSuggestionScope.Hop> entry=scope.enterBridge(chain.getArg(0),string(chain.getArg(1)),
            string(chain.getArg(2)),!Boolean.FALSE.equals(chain.getArg(3)),frame,fresh());
        event(frame,entry.reason());try{return original.invoke();}finally{scope.exitBridge(entry.token());}
    }
    NativeSuggestionScope.Claim claim(Object context,String query,String source,String direct,boolean ai,Trace.Frame frame){
        NativeSuggestionScope.Claim claim=armed?scope.claim(context,query,source,direct,ai,frame,fresh()):
            new NativeSuggestionScope.Claim(null,NativeSuggestionScope.Reason.NO_ROW);
        event(frame,claim.reason());return claim;
    }
    boolean live(NativeSuggestionScope.Permit permit){return armed && scope.live(permit);}
    private static String string(Object value){return value instanceof String?(String)value:null;}
}
