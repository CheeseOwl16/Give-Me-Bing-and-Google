package dev.cheeseowl.gmbg;

import android.content.Context;
import android.content.Intent;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.json.JSONObject;

/** Synchronous x -> h/e -> encoder -> c authority, never a global getter or state repair. */
final class EngineTakeover {
    enum Outcome { DISABLED, SETTINGS_UNKNOWN, OUTSIDE_SCOPE, ITEM_UNAVAILABLE, ITEM_READY, TEMPLATE_OK,
        TEMPLATE_MISMATCH, ENCODING_OK, ENCODING_MISMATCH, FINAL_OK, NATIVE_REPLAY, REPLAY_FAILED, REPLAY_NO_START, NO_DISPATCH }
    enum FinalReason { MATCH, CONFIG_CHANGED, MODEL_CHANGED, NATIVE_SCOPE_EXPIRED, HISTORY_SCOPE_EXPIRED, UNSUPPORTED_INTENT, URL_MISMATCH, UNKNOWN }
    private static final class Request {
        final FeatureState flags;final String query;final Object[] original;final Trace.Frame frame;
        EngineHost.Candidate candidate;DecisionLease lease;int templateDepth;boolean dispatched;
        NativeSuggestionScope.Permit nativePermit;
        HistoryScope.Permit historyPermit;
        Request(FeatureState flags,String query,Object[] original,Trace.Frame frame){this.flags=flags;this.query=query;this.original=original;this.frame=frame;}
    }
    private final FeatureClient features;private final EngineHost host;private final DiagnosticLog log;private final Method queryMethod;
    private final NativeSuggestionDomain nativeSuggestions;
    private final HistoryDomain histories;
    private final ThreadLocal<Request> requests=new ThreadLocal<>();private final ThreadLocal<Boolean> replaying=new ThreadLocal<>();
    private volatile boolean armed;
    EngineTakeover(FeatureClient features,ClassLoader loader,DiagnosticLog log,NativeSuggestionDomain nativeSuggestions,HistoryDomain histories) throws ReflectiveOperationException {
        this.features=features;this.host=new EngineHost(loader);this.log=log;
        this.nativeSuggestions=nativeSuggestions;
        this.histories=histories;
        queryMethod=TargetSpec.resolve(loader,TargetSpec.HOOKS[5]);
    }
    void arm(boolean value){armed=value;}
    private void emit(Outcome outcome,Request r){
        log.emit(DiagnosticLog.Event.TAKEOVER,DiagnosticLog.Hook.QUERY,DiagnosticLog.Phase.STATUS,r==null?null:r.frame,
            outcome,r==null?EngineChoice.UNKNOWN:r.flags.choice());
    }
    Object invoke(DiagnosticLog.Hook kind,XposedInterface.Chain chain,Trace.Frame frame) throws Throwable {
        if(kind==DiagnosticLog.Hook.QUERY)return query(chain,frame);
        Request r=requests.get();if(!armed || r==null)return chain.proceed();
        if(kind==DiagnosticLog.Hook.TEMPLATE)return template(chain,r);
        if(kind==DiagnosticLog.Hook.ITEM){
            Object nativeItem=chain.proceed();
            if(r.templateDepth>0 && r.candidate!=null && chain.getThisObject()==r.candidate.host()){
                try{if(r.dispatched || (r.flags.sameDecision(features.read()) && host.stable(r.candidate,r.flags.choice())))return r.candidate.item();}
                catch(Throwable ignored){};
            }return nativeItem;
        }
        Object result=chain.proceed();
        if(kind==DiagnosticLog.Hook.ENCODE && r.lease!=null){
            boolean matched=chain.getArg(0) instanceof String && chain.getArg(1) instanceof String && result instanceof String &&
                r.lease.encoded((String)chain.getArg(0),(String)chain.getArg(1),(String)result);
            emit(matched?Outcome.ENCODING_OK:Outcome.ENCODING_MISMATCH,r);
        }return result;
    }
    private Object query(XposedInterface.Chain chain,Trace.Frame frame) throws Throwable {
        Request previous=requests.get();requests.remove();
        try{
            if(!armed || frame==null || Boolean.TRUE.equals(replaying.get()) || previous!=null)return chain.proceed();
            FeatureState flags=features.read();
            if(!flags.valid() || !flags.engine()){
                QueryStatus.emit(log,flags.valid()?Outcome.DISABLED:Outcome.SETTINGS_UNKNOWN,frame,flags.choice(),
                    flags.valid()?QueryStatus.Reason.DISABLED:QueryStatus.Reason.SETTINGS);return chain.proceed();
            }
            String raw=chain.getArg(1) instanceof String?(String)chain.getArg(1):null;
            String direct=chain.getArg(2) instanceof String?(String)chain.getArg(2):null;
            String origin=chain.getArg(3) instanceof String?(String)chain.getArg(3):null;
            String source=source(origin);String trimmed=QueryScope.normalize(raw);
            Object context=chain.getArg(0);
            QueryStatus.Reason rejection=null;
            if(!(context instanceof Context) || !TargetSpec.PACKAGE.equals(((Context)context).getPackageName()))rejection=QueryStatus.Reason.CONTEXT;
            else if(!(chain.getArg(4) instanceof Boolean))rejection=QueryStatus.Reason.AI_ARGUMENT;
            else if(Boolean.TRUE.equals(chain.getArg(4)))rejection=QueryStatus.Reason.AI_BRANCH;
            else if(direct!=null && !direct.isEmpty())rejection=QueryStatus.Reason.DIRECT_URL;
            else if(!SearchUrlPolicy.query(trimmed))rejection=QueryStatus.Reason.QUERY_INVALID;
            NativeSuggestionScope.Permit permit=null;
            HistoryScope.Permit historyPermit=null;
            if(rejection==null && !QueryScope.allows(source,direct,false)){
                if("sug_page".equals(origin) && nativeSuggestions!=null){
                    NativeSuggestionScope.Claim claim=nativeSuggestions.claim(context,raw,origin,direct,false,frame);permit=claim.permit();
                }
                if("history_ime_g".equals(origin) && histories!=null){
                    HistoryScope.Claim claim=histories.claim(context,raw,origin,direct,false,frame);historyPermit=claim.permit();
                }
                if(permit==null && historyPermit==null)rejection="sug_page".equals(origin)?QueryStatus.Reason.NATIVE_DOMAIN_REQUIRED:
                    "history_ime_g".equals(origin)?QueryStatus.Reason.HISTORY_DOMAIN_REQUIRED:QueryStatus.Reason.SOURCE_NOT_ALLOWED;
            }
            if(rejection!=null){QueryStatus.emit(log,Outcome.OUTSIDE_SCOPE,frame,flags.choice(),rejection);return chain.proceed();}
            Object[] args={context,raw,chain.getArg(2),chain.getArg(3),chain.getArg(4)};
            Request r=new Request(flags,trimmed,args,frame);r.nativePermit=permit;r.historyPermit=historyPermit;requests.set(r);
            try{return chain.proceed();}
            finally{if(r.lease!=null && !r.dispatched)emit(Outcome.NO_DISPATCH,r);}
        }finally{requests.remove();if(previous!=null)requests.set(previous);}
    }
    private static String source(String origin){
        if(origin==null || origin.length()>2048)return null;
        if("history".equals(origin))return origin;
        try{Object value=new JSONObject(origin).opt("source");return value instanceof String?(String)value:null;}
        catch(Exception ignored){return null;}
    }
    private Object template(XposedInterface.Chain chain,Request r) throws Throwable {
        if(r.dispatched && r.candidate==null)return chain.proceed();
        try{
            if(r.candidate==null && r.flags.sameDecision(features.read())){
                r.candidate=host.select(chain.getThisObject(),r.flags.choice());
                if(r.candidate!=null){
                    r.lease=new DecisionLease(r.flags,r.query,r.candidate.template(),r.candidate.model(),r.candidate.item());
                    log.emit(DiagnosticLog.Event.TAKEOVER,DiagnosticLog.Hook.ITEM,DiagnosticLog.Phase.STATUS,r.frame,
                        Outcome.ITEM_READY,r.flags.choice(),r.candidate.origin());
                }else emit(Outcome.ITEM_UNAVAILABLE,r);
            }
        }catch(Throwable ignored){emit(Outcome.ITEM_UNAVAILABLE,r);}
        r.templateDepth++;
        Object result;
        try{result=chain.proceed();}finally{r.templateDepth--;}
        if(r.lease!=null){
            boolean match=r.lease.template.equals(result);
            // c() also asks for urlTemp after final validation; never erase encoded evidence.
            if(!r.dispatched)r.lease.templated=match;
            emit(match?Outcome.TEMPLATE_OK:Outcome.TEMPLATE_MISMATCH,r);
        }return result;
    }
    /** Must run before either the P3B replacement or the original c startup. */
    Object dispatch(XposedInterface.Chain chain,Around.Call next) throws Throwable {
        Request r=requests.get();if(r==null)return next.invoke();
        if(r.lease==null){r.dispatched=true;return next.invoke();}
        boolean valid=false;FinalReason reason=FinalReason.UNKNOWN;
        try{
            Object arg=chain.getArg(1),url=chain.getArg(2);
            FeatureState now=features.read();
            if(!r.flags.sameDecision(now))reason=FinalReason.CONFIG_CHANGED;
            else if(r.nativePermit!=null && (nativeSuggestions==null || !nativeSuggestions.live(r.nativePermit)))reason=FinalReason.NATIVE_SCOPE_EXPIRED;
            else if(r.historyPermit!=null && (histories==null || !histories.live(r.historyPermit)))reason=FinalReason.HISTORY_SCOPE_EXPIRED;
            else if(!host.stable(r.candidate,r.flags.choice()))reason=FinalReason.MODEL_CHANGED;
            else if(arg instanceof Intent && url instanceof String && chain.getArg(0)==r.original[0]){
                Intent intent=(Intent)arg;
                boolean ordinary=intent.getData()!=null &&
                    WebPolicy.allows(intent.getAction(),intent.getData().toString(),(String)url,r.flags.choice().id,
                        intent.getComponent()!=null,intent.getSelector()!=null,intent.getPackage(),
                        (intent.getFlags()&(Intent.FLAG_ACTIVITY_FORWARD_RESULT|Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))!=0);
                valid=ordinary && r.lease.dispatch(now,r.candidate.model(),r.candidate.item(),intent.getData().toString(),(String)url);
                reason=valid?FinalReason.MATCH:ordinary?FinalReason.URL_MISMATCH:FinalReason.UNSUPPORTED_INTENT;
            }else reason=FinalReason.UNSUPPORTED_INTENT;
        }catch(Throwable ignored){valid=false;}
        log.emit(DiagnosticLog.Event.URL_CHECK,DiagnosticLog.Hook.BROWSER,DiagnosticLog.Phase.STATUS,r.frame,r.flags.choice(),valid,reason);
        return DispatchDecision.run(valid,() -> {r.dispatched=true;emit(Outcome.FINAL_OK,r);return next.invoke();},() -> replay(r));
    }
    private Object replay(Request r) throws Throwable {
        emit(Outcome.NATIVE_REPLAY,r);
        // No launch has occurred through this c invocation. Replay only raw original x arguments, once.
        requests.remove();replaying.set(true);
        try{Object result=queryMethod.invoke(null,r.original);if(!Boolean.TRUE.equals(result))emit(Outcome.REPLAY_NO_START,r);return null;}
        catch(InvocationTargetException failure){emit(Outcome.REPLAY_FAILED,r);throw failure.getCause();}
        catch(Throwable failure){emit(Outcome.REPLAY_FAILED,r);throw failure;}
        finally{replaying.remove();requests.set(r);r.dispatched=true;}
    }
    String browserEngine() {
        Request r=requests.get();if(r==null || r.lease==null || !r.dispatched)return null;
        return r.flags.choice().id;
    }
    boolean authoritative(){Request r=requests.get();return r!=null && r.lease!=null;}
}
