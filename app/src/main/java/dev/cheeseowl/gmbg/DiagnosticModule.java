package dev.cheeseowl.gmbg;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedInterface;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiagnosticModule extends XposedModule {
    private final Trace trace=new Trace();
    private final DiagnosticLog events=new DiagnosticLog(line -> log(Log.INFO,"QSBP2",line));
    private final AtomicBoolean ready=new AtomicBoolean();
    private final AtomicBoolean installed=new AtomicBoolean();
    private boolean mainProcess;
    private EngineTakeover takeover;
    private NativeSuggestionDomain nativeSuggestions;
    private HistoryDomain histories;
    private FeatureClient features;
    private BrowserRedirect browser;
    private volatile FeatureState lastFlags;

    @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        try {
            mainProcess=!param.isSystemServer() && TargetSpec.PACKAGE.equals(param.getProcessName());
            if (mainProcess) events.emit(DiagnosticLog.Event.API,DiagnosticLog.Hook.MODULE,DiagnosticLog.Phase.STATUS,
                null,getApiVersion()==102);
        } catch (Throwable ignored) { mainProcess=false; }
    }
    @Override public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        try {
            if (!mainProcess || !param.isFirstPackage() || !TargetSpec.PACKAGE.equals(param.getPackageName()) ||
                getApiVersion()!=102 || !ready.compareAndSet(false,true)) return;
            // Exact base APK identity also pins the P1 DEX and signer. No APK contents are loaded into this module.
            if (!TargetSpec.APK_SHA.equals(fileSha(param.getApplicationInfo().sourceDir))) {
                status(DiagnosticLog.Event.GATE_REJECT,DiagnosticLog.Hook.MODULE,DiagnosticLog.Reason.IDENTITY); return;
            }
            ClassLoader loader=param.getClassLoader();
            Method attach=Application.class.getDeclaredMethod("attach",Context.class);
            hook(attach).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).setId("qsbp2.bootstrap")
                .intercept(chain -> {
                    // Original is outside all observer catch blocks and runs once with the original arguments.
                    Object result=chain.proceed();
                    try {
                        Object arg=chain.getArg(0);
                        if (arg instanceof Context && TargetSpec.PACKAGE.equals(((Context)arg).getPackageName())) {
                            if (!verify((Context)arg)) {
                                status(DiagnosticLog.Event.GATE_REJECT,DiagnosticLog.Hook.MODULE,DiagnosticLog.Reason.IDENTITY);
                            } else if (installed.compareAndSet(false,true)) {
                                status(DiagnosticLog.Event.GATE_OK,DiagnosticLog.Hook.MODULE,DiagnosticLog.Reason.NONE);
                                installAll(loader,(Context)arg);
                            }
                        }
                    } catch (Throwable ignored) {
                        status(DiagnosticLog.Event.GATE_REJECT,DiagnosticLog.Hook.MODULE,DiagnosticLog.Reason.IDENTITY);
                    }
                    return result;
                });
            status(DiagnosticLog.Event.INSTALL_OK,DiagnosticLog.Hook.BOOTSTRAP,DiagnosticLog.Reason.NONE);
        } catch (Throwable ignored) {
            status(DiagnosticLog.Event.INSTALL_FAIL,DiagnosticLog.Hook.BOOTSTRAP,DiagnosticLog.Reason.INSTALL);
        }
    }
    private boolean verify(Context context) throws Exception {
        if (!TargetSpec.PACKAGE.equals(context.getPackageName())) return false;
        PackageInfo p=context.getPackageManager().getPackageInfo(TargetSpec.PACKAGE,PackageManager.GET_SIGNING_CERTIFICATES);
        if (!TargetSpec.knownVersion(p.versionName,p.getLongVersionCode()) || p.signingInfo==null) return false;
        Signature[] signers=p.signingInfo.getApkContentsSigners();
        return signers.length==1 && TargetSpec.CERT_SHA.equals(hex(MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray())));
    }
    private void installAll(ClassLoader loader,Context context) {
        ReadState state=new ReadState(loader);
        features=new FeatureClient(context);
        try{browser=new BrowserRedirect(features,loader,events);}catch(Throwable ignored){browser=null;}
        try{nativeSuggestions=new NativeSuggestionDomain(loader,events);}catch(Throwable ignored){nativeSuggestions=null;}
        try{histories=new HistoryDomain(loader,events);}catch(Throwable ignored){histories=null;}
        try{takeover=new EngineTakeover(features,loader,events,nativeSuggestions,histories);}catch(Throwable ignored){takeover=null;}
        SuggestionDiagnostics probeState;
        try{probeState=new SuggestionDiagnostics(loader);}catch(Throwable ignored){probeState=null;}
        final SuggestionDiagnostics suggestions=probeState;
        java.util.EnumSet<DiagnosticLog.Hook> hooks=java.util.EnumSet.noneOf(DiagnosticLog.Hook.class);
        for (TargetSpec.Spec spec:TargetSpec.all()) {
            try {
                Method method=TargetSpec.resolve(loader,spec);
                hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).setId("qsbp2."+spec.id().name())
                    .intercept(chain -> {
                        Trace.Frame[] frame={null};
                        return Around.run(() -> {
                            frame[0]=trace.enter(spec.kind());
                            refreshFeatures();
                            observe(spec,DiagnosticLog.Phase.BEFORE,frame[0],chain,null,false,state,suggestions);
                        },() -> {
                            if(histories!=null && spec.id()==DiagnosticLog.Hook.HISTORY_RECEIVE)
                                return histories.receive(chain,frame[0],chain::proceed);
                            if(nativeSuggestions!=null && spec.id()==DiagnosticLog.Hook.NATIVE_ROW)
                                return nativeSuggestions.row(chain,frame[0],chain::proceed);
                            if(nativeSuggestions!=null && spec.id()==DiagnosticLog.Hook.SUGGEST_NATIVE)
                                return nativeSuggestions.bridge(chain,frame[0],chain::proceed);
                            if(takeover!=null && (spec.id()==DiagnosticLog.Hook.QUERY || spec.id()==DiagnosticLog.Hook.TEMPLATE ||
                                spec.id()==DiagnosticLog.Hook.ITEM || spec.id()==DiagnosticLog.Hook.ENCODE))return takeover.invoke(spec.id(),chain,frame[0]);
                            if(spec.id()==DiagnosticLog.Hook.BROWSER){
                                Around.Call route=() -> browser==null ? chain.proceed():browser.invoke(chain,chain::proceed,frame[0],takeover);
                                return takeover==null ? route.invoke():takeover.dispatch(chain,route);
                            }
                            return chain.proceed();
                        },(result,threw) -> {
                            observe(spec,DiagnosticLog.Phase.AFTER,frame[0],chain,result,threw,state,suggestions);
                        },() -> { if (frame[0]!=null) trace.exit(frame[0]); });
                    });
                status(DiagnosticLog.Event.INSTALL_OK,spec.id(),DiagnosticLog.Reason.NONE);
                hooks.add(spec.id());
            } catch (Throwable ignored) { status(DiagnosticLog.Event.INSTALL_FAIL,spec.id(),DiagnosticLog.Reason.SIGNATURE); }
        }
        boolean enabled=takeover!=null && TakeoverCapability.ready(hooks);
        if(takeover!=null)takeover.arm(enabled);
        boolean nativeEnabled=enabled && nativeSuggestions!=null && hooks.contains(DiagnosticLog.Hook.NATIVE_ROW)
            && hooks.contains(DiagnosticLog.Hook.SUGGEST_NATIVE);
        if(nativeSuggestions!=null)nativeSuggestions.arm(nativeEnabled);
        boolean historyEnabled=enabled && histories!=null && hooks.contains(DiagnosticLog.Hook.HISTORY_RECEIVE);
        if(histories!=null)histories.arm(historyEnabled);
        refreshFeatures();
        events.emit(DiagnosticLog.Event.TAKEOVER_READY,DiagnosticLog.Hook.MODULE,DiagnosticLog.Phase.STATUS,null,enabled,lastFlags!=null && lastFlags.engine());
        events.emit(DiagnosticLog.Event.BROWSER_READY,DiagnosticLog.Hook.BROWSER,DiagnosticLog.Phase.STATUS,null,
            browser!=null && hooks.contains(DiagnosticLog.Hook.BROWSER),lastFlags!=null && lastFlags.browser());
        boolean probesReady=suggestions!=null;
        for(TargetSpec.Spec probe:TargetSpec.OBSERVERS)probesReady &= hooks.contains(probe.id());
        events.emit(DiagnosticLog.Event.SUGGEST_DIAGNOSTICS_READY,DiagnosticLog.Hook.MODULE,DiagnosticLog.Phase.STATUS,null,probesReady);
        events.emit(DiagnosticLog.Event.NATIVE_READY,DiagnosticLog.Hook.NATIVE_ROW,DiagnosticLog.Phase.STATUS,null,nativeEnabled);
        events.emit(DiagnosticLog.Event.HISTORY_READY,DiagnosticLog.Hook.HISTORY_RECEIVE,DiagnosticLog.Phase.STATUS,null,historyEnabled);
    }
    private void refreshFeatures(){
        FeatureState flags=features==null ? FeatureState.unavailable():features.read();
        if(!flags.equals(lastFlags)){
            lastFlags=flags;events.emit(DiagnosticLog.Event.FEATURE_SWITCH,DiagnosticLog.Hook.MODULE,
                DiagnosticLog.Phase.STATUS,null,flags.engine(),flags.browser(),flags.valid(),flags.choice());
        }
    }
    private void observe(TargetSpec.Spec spec, DiagnosticLog.Phase phase, Trace.Frame frame,
                         XposedInterface.Chain chain,Object result,boolean threw,ReadState state,SuggestionDiagnostics suggestions) {
        DiagnosticLog.Event event=phase==DiagnosticLog.Phase.BEFORE ? DiagnosticLog.Event.BEGIN : DiagnosticLog.Event.END;
        try {
            Object[] detail=switch(spec.id()) {
                case USER,WRITE -> {
                    Object[] s=state.selection(chain.getThisObject());
                    yield new Object[]{trace.source(),Privacy.engine(chain.getArg(0)),s[0],s[1],s[2],s[3],s[4],s[5],threw};
                }
                case PARSE -> {
                    Object[] s=phase==DiagnosticLog.Phase.AFTER && !threw ? state.parsed(result) :
                        new Object[]{Privacy.Tri.UNKNOWN,Privacy.Tri.UNKNOWN,Privacy.Engine.UNKNOWN,Privacy.Engine.UNKNOWN};
                    yield new Object[]{trace.source(),Privacy.tri(chain.getArg(1)),s[0],s[1],s[2],s[3],threw};
                }
                case PROPAGATE -> {
                    Object[] s=state.provider(chain.getThisObject());
                    yield new Object[]{trace.source(),s[0],s[1],threw};
                }
                case BROWSER -> web(chain.getArg(1),threw);
                default -> new Object[]{DiagnosticLog.Reason.NONE};
            };
            events.emit(event,spec.id(),phase,frame,detail);
            if(suggestions!=null && SuggestionDiagnostics.observes(spec.id())){
                Object[] path=SuggestionDiagnostics.join(suggestions.read(spec.id(),chain),
                    new Object[]{result instanceof Boolean?Privacy.tri(result):Privacy.Tri.UNKNOWN,threw});
                events.emit(DiagnosticLog.Event.SUGGEST_PATH,spec.id(),phase,frame,path);
            }
        } catch (Throwable ignored) {
            events.emit(event,spec.id(),phase,frame,trace.source(),Privacy.Tri.UNKNOWN,threw);
            events.emit(DiagnosticLog.Event.OBSERVER_FAIL,spec.id(),phase,frame,DiagnosticLog.Reason.OBSERVER);
        }
    }
    private Object[] web(Object value,boolean threw) {
        if (!(value instanceof Intent)) return new Object[]{Privacy.Action.UNKNOWN,threw};
        Intent i=(Intent)value;
        Uri data=i.getData();
        ComponentName c=i.getComponent();
        int flags=i.getFlags();
        return new Object[]{Privacy.action(i.getAction()),Privacy.scheme(data==null ? null:data.getScheme()),
            Privacy.target(i.getPackage()),Privacy.target(c==null ? null:c.getPackageName()),
            Privacy.target(c==null ? null:c.getClassName()),(flags&Intent.FLAG_ACTIVITY_NEW_TASK)!=0,
            (flags&Intent.FLAG_ACTIVITY_CLEAR_TOP)!=0,(flags&Intent.FLAG_ACTIVITY_SINGLE_TOP)!=0,
            (flags&Intent.FLAG_ACTIVITY_NEW_DOCUMENT)!=0,(flags&Intent.FLAG_ACTIVITY_MULTIPLE_TASK)!=0,
            (flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))!=0,
            (flags&Intent.FLAG_ACTIVITY_CLEAR_TASK)!=0,(flags&Intent.FLAG_ACTIVITY_NO_HISTORY)!=0,
            (flags&Intent.FLAG_ACTIVITY_FORWARD_RESULT)!=0,threw};
    }
    private void status(DiagnosticLog.Event e,DiagnosticLog.Hook h,DiagnosticLog.Reason r) {
        try { events.emit(e,h,DiagnosticLog.Phase.STATUS,null,r); } catch (Throwable ignored) { }
    }
    private static String fileSha(String path) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try (FileInputStream in=new FileInputStream(path)) {
            byte[] buffer=new byte[32768]; int n;
            while ((n=in.read(buffer))!=-1) digest.update(buffer,0,n);
        }
        return hex(digest.digest());
    }
    private static String hex(byte[] bytes) {
        char[] chars=new char[bytes.length*2]; String alphabet="0123456789abcdef";
        for(int n=0;n<bytes.length;n++){ chars[n*2]=alphabet.charAt((bytes[n]>>>4)&15); chars[n*2+1]=alphabet.charAt(bytes[n]&15); }
        return new String(chars);
    }
}
