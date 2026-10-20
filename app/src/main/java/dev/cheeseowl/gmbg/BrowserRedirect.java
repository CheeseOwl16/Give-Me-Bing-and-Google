package dev.cheeseowl.gmbg;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import io.github.libxposed.api.XposedInterface;

/** Sole replacement-start location. Never modifies the original Intent, URL or arguments. */
final class BrowserRedirect {
    enum Gate { ELIGIBLE, DISABLED, SETTINGS_UNKNOWN, NOT_ORDINARY, NO_FULLSEARCH, UNKNOWN }
    private final FeatureClient features;private final BrowserEngine engine;private final DiagnosticLog log;
    private final BrowserDispatcher dispatch=new BrowserDispatcher();
    BrowserRedirect(FeatureClient features,ClassLoader loader,DiagnosticLog log) throws ReflectiveOperationException {
        this.features=features;this.engine=new BrowserEngine(loader);this.log=log;
    }
    Object invoke(XposedInterface.Chain chain,Around.Call original,Trace.Frame frame,EngineTakeover takeover) throws Throwable {
        Context context=null;Uri data=null;boolean allowed=false;Gate gate=Gate.NOT_ORDINARY;
        try {
            FeatureState flags=features.read();Object ctx=chain.getArg(0),arg=chain.getArg(1),url=chain.getArg(2);
            if(!flags.valid())gate=Gate.SETTINGS_UNKNOWN;else if(!flags.browser())gate=Gate.DISABLED;
            if(flags.browser() && ctx instanceof Context && arg instanceof Intent && url instanceof String){
                context=(Context)ctx;Intent input=(Intent)arg;data=input.getData();
                int unsafe=Intent.FLAG_ACTIVITY_FORWARD_RESULT|Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                allowed=TargetSpec.PACKAGE.equals(context.getPackageName()) && data!=null &&
                    WebPolicy.allows(input.getAction(),data.toString(),(String)url,
                        takeover!=null && takeover.authoritative()?takeover.browserEngine():engine.ordinaryEngine(),
                        input.getComponent()!=null,input.getSelector()!=null,input.getPackage(),(input.getFlags()&unsafe)!=0);
                if(allowed){
                    ActivityInfo full=context.getPackageManager().getActivityInfo(new ComponentName("com.android.browser",
                        "com.android.browser.fullsearch.FullSearchActivity"),0);
                    allowed=full.enabled && full.exported;
                    gate=allowed ? Gate.ELIGIBLE:Gate.NO_FULLSEARCH;
                }
            }
        }catch(Throwable ignored){allowed=false;gate=Gate.UNKNOWN;}
        log.emit(DiagnosticLog.Event.BROWSER_GATE,DiagnosticLog.Hook.BROWSER,DiagnosticLog.Phase.STATUS,frame,gate);
        final Context targetContext=context;final Uri targetData=data;
        return dispatch.run(allowed,() -> {
            // Preserve the already-built Uri exactly: no query extraction, template rebuild or second encoding.
            Intent replacement=new Intent(Intent.ACTION_VIEW,targetData);
            replacement.addCategory(Intent.CATEGORY_BROWSABLE);
            if(!(targetContext instanceof Activity))replacement.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            targetContext.startActivity(replacement);
        },original,outcome -> log.emit(DiagnosticLog.Event.BROWSER_ROUTE,DiagnosticLog.Hook.BROWSER,
            DiagnosticLog.Phase.STATUS,frame,outcome));
    }
}
