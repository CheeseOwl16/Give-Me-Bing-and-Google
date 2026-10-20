package dev.cheeseowl.gmbg;

/** Explicit pre-request frames prevent call=0 rejections from being falsely joined to another query. */
final class QueryStatus {
    enum Reason { SETTINGS, DISABLED, CONTEXT, AI_ARGUMENT, AI_BRANCH, DIRECT_URL, QUERY_INVALID,
        SOURCE_NOT_ALLOWED, NATIVE_DOMAIN_REQUIRED, HISTORY_DOMAIN_REQUIRED }
    static void emit(DiagnosticLog log,Enum<?> outcome,Trace.Frame frame,EngineChoice choice,Reason reason){
        log.emit(DiagnosticLog.Event.TAKEOVER,DiagnosticLog.Hook.QUERY,DiagnosticLog.Phase.STATUS,frame,outcome,choice,reason);
    }
    private QueryStatus(){}
}
