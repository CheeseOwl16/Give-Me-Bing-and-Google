package dev.cheeseowl.gmbg;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** P1 DEX descriptors, not JADX aliases or copied proprietary implementations. */
final class TargetSpec {
    static final String PACKAGE = "com.android.quicksearchbox";
    static final String F = PACKAGE + ".xiaomi.searchengine.f";
    static final String C = PACKAGE + ".xiaomi.searchengine.c";
    static final String J = PACKAGE + ".xiaomi.searchengine.j";
    static final String VERSION = "12.10.0.07203";
    static final long CODE = 212100003L;
    static final String APK_SHA = "5567e02259a58b6499b121da09a37424824d419eaeb99b62520649e8302ca6e8";
    static final String CERT_SHA = "c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025";
    static boolean knownVersion(String version,long code){return VERSION.equals(version) && code==CODE;}
    record Spec(DiagnosticLog.Hook id, Trace.Kind kind, String owner, String name,
                String result, boolean isStatic, String... params) {}
    static final Spec[] HOOKS = {
        new Spec(DiagnosticLog.Hook.USER, Trace.Kind.USER, F, "l", "void", false, "java.lang.String"),
        new Spec(DiagnosticLog.Hook.WRITE, Trace.Kind.WRITE, F, "k", "void", false, "java.lang.String"),
        new Spec(DiagnosticLog.Hook.PARSE, Trace.Kind.PARSE, "E2.a", "a", "E2.a", true, "Q3.a", "boolean"),
        new Spec(DiagnosticLog.Hook.PROPAGATE, Trace.Kind.PROPAGATE, C, "d", "void", false),
        new Spec(DiagnosticLog.Hook.BROWSER, Trace.Kind.BROWSER, "V2.j1", "c", "void", true,
            "android.content.Context", "android.content.Intent", "java.lang.String", "java.lang.String", "java.util.HashMap"),
        new Spec(DiagnosticLog.Hook.QUERY, Trace.Kind.QUERY, "V2.j1", "x", "boolean", true,
            "android.content.Context", "java.lang.String", "java.lang.String", "java.lang.String", "boolean"),
        new Spec(DiagnosticLog.Hook.TEMPLATE, Trace.Kind.TEMPLATE, F, "h", "java.lang.String", false,"android.content.Context"),
        new Spec(DiagnosticLog.Hook.ITEM, Trace.Kind.ITEM, F, "e", "com.android.quicksearchbox.xiaomi.searchengine.h", false),
        new Spec(DiagnosticLog.Hook.ENCODE, Trace.Kind.ENCODE, "V2.j1", "h", "java.lang.String", true,"java.lang.String","java.lang.String")
    };
    /** Diagnostic-only probes. Their installation never arms or changes engine/browser behavior. */
    static final Spec[] OBSERVERS = {
        new Spec(DiagnosticLog.Hook.COMPLETION, Trace.Kind.COMPLETION, "y2.U", "n", "void", false,"java.lang.String","java.lang.String","boolean"),
        new Spec(DiagnosticLog.Hook.UI_SEARCH, Trace.Kind.UI_SEARCH, "y2.V", "g", "void", true,"y2.V","java.lang.String","java.lang.String"),
        new Spec(DiagnosticLog.Hook.UI_EXECUTE, Trace.Kind.UI_EXECUTE, "y2.V", "X", "void", false,"java.lang.String","java.lang.String","boolean"),
        new Spec(DiagnosticLog.Hook.WEB_DIRECT, Trace.Kind.WEB_DIRECT, "V2.j1", "N", "boolean", true,"android.content.Context","java.lang.String","java.lang.String"),
        new Spec(DiagnosticLog.Hook.WEB_PREPARE, Trace.Kind.WEB_PREPARE, "V2.j1", "K", "boolean", true,"android.content.Context","java.lang.String","java.lang.String","java.lang.String"),
        new Spec(DiagnosticLog.Hook.SUGGEST_JS, Trace.Kind.SUGGEST_JS, "com.android.quicksearchbox.jsapi.l", "searchArgs", "void", false,"java.lang.String"),
        new Spec(DiagnosticLog.Hook.SUGGEST_NATIVE, Trace.Kind.SUGGEST_NATIVE, "com.android.quicksearchbox.appmodule.serviceimpls.OpenAppInterfaceImpl", "jumpBrowser", "boolean", false,"android.content.Context","java.lang.String","java.lang.String","boolean"),
        new Spec(DiagnosticLog.Hook.ACTIVITY_DISPATCH, Trace.Kind.BUSINESS_DISPATCH, "V2.j1", "G", "boolean", true,"android.content.Context","android.content.Intent"),
        new Spec(DiagnosticLog.Hook.FOREGROUND_SERVICE_DISPATCH, Trace.Kind.BUSINESS_DISPATCH, "V2.j1", "C", "boolean", true,"android.content.Context","android.content.Intent"),
        new Spec(DiagnosticLog.Hook.SERVICE_DISPATCH, Trace.Kind.BUSINESS_DISPATCH, "V2.j1", "P", "void", true,"android.content.Context","android.content.Intent"),
        new Spec(DiagnosticLog.Hook.SPECIAL_DISPATCH, Trace.Kind.BUSINESS_DISPATCH, "V2.j1", "M", "void", true,"android.content.Context","android.content.Intent","java.lang.String","java.lang.String")
    };
    static final Spec[] NATIVE_HOOKS={
        new Spec(DiagnosticLog.Hook.NATIVE_ROW, Trace.Kind.NATIVE_ROW, "I5.d", "onClick", "void", false,"android.view.View"),
        new Spec(DiagnosticLog.Hook.HISTORY_RECEIVE, Trace.Kind.HISTORY_RECEIVE, "m2.h", "onReceive", "void", false,"android.content.Context","android.content.Intent")
    };
    static Spec[] all(){
        Spec[] result=new Spec[HOOKS.length+OBSERVERS.length+NATIVE_HOOKS.length];
        System.arraycopy(HOOKS,0,result,0,HOOKS.length);System.arraycopy(OBSERVERS,0,result,HOOKS.length,OBSERVERS.length);
        System.arraycopy(NATIVE_HOOKS,0,result,HOOKS.length+OBSERVERS.length,NATIVE_HOOKS.length);return result;
    }
    static Method resolve(ClassLoader loader, Spec spec) throws ReflectiveOperationException {
        Class<?> owner = Class.forName(spec.owner(), false, loader);
        Class<?>[] params = new Class<?>[spec.params().length];
        for (int i=0;i<params.length;i++) params[i] = spec.params()[i].equals("boolean")
            ? boolean.class : Class.forName(spec.params()[i], false, loader);
        Method method = owner.getDeclaredMethod(spec.name(), params);
        if (!method.getReturnType().getName().equals(spec.result()) ||
            Modifier.isStatic(method.getModifiers()) != spec.isStatic() ||
            !Modifier.isPublic(method.getModifiers())) throw new NoSuchMethodException();
        return method;
    }
    private TargetSpec() {}
}
