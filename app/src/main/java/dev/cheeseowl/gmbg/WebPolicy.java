package dev.cheeseowl.gmbg;

import java.net.URI;
import java.util.Locale;

/** Ordinary-web allowlist at the one verified business boundary. No URL normalization/encoding. */
final class WebPolicy {
    static boolean allows(String action,String uri,String builtUrl,String engine,
                          boolean hasComponent,boolean hasSelector,String forcedPackage,boolean unsafeFlags) {
        if(!"android.intent.action.VIEW".equals(action) || uri==null || !uri.equals(builtUrl) ||
            uri.length()>8192 || !EngineChoice.validNativeId(engine) || "aisearch".equals(engine) || "onesearch".equals(engine) ||
            hasComponent || hasSelector || unsafeFlags || (forcedPackage!=null && !forcedPackage.equals("com.android.browser")))return false;
        for(int i=0;i<uri.length();i++)if(Character.isISOControl(uri.charAt(i)) || Character.isWhitespace(uri.charAt(i)))return false;
        try {
            URI u=new URI(uri);
            if(!("http".equals(u.getScheme()) || "https".equals(u.getScheme())) || u.isOpaque() ||
                u.getHost()==null || u.getHost().isEmpty() || u.getUserInfo()!=null || u.getPort()>65535)return false;
            String path=u.getPath()==null ? "":u.getPath().toLowerCase(Locale.ROOT);
            return !path.contains("/aisearch") && !path.contains("/onesearch");
        }catch(Exception ignored){return false;}
    }
    private WebPolicy() { }
}
