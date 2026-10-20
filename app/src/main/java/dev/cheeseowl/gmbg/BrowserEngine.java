package dev.cheeseowl.gmbg;

import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;

/** Independent read-only browser eligibility state; recovery codec/setup failure does not disable this adapter. */
final class BrowserEngine {
    private final Field instance,current,pref,scene,provider,loaded,map;
    BrowserEngine(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> f=Class.forName(TargetSpec.F,false,loader),c=Class.forName(TargetSpec.C,false,loader),set=Class.forName("E2.a",false,loader);
        instance=field(f,"h",f);current=field(f,"c",String.class);pref=field(f,"d",SharedPreferences.class);
        scene=field(f,"a",String.class);provider=field(f,"f",c);loaded=field(c,"d",set);map=field(set,"d",Object.class);
    }
    private static Field field(Class<?> owner,String name,Class<?> type) throws ReflectiveOperationException {
        Field f=owner.getDeclaredField(name);if(f.getType()!=type)throw new NoSuchFieldException();f.setAccessible(true);return f;
    }
    String ordinaryEngine() throws IllegalAccessException {
        Object host=instance.get(null);if(host==null)return null;
        SharedPreferences sp=(SharedPreferences)pref.get(host);if(sp==null)return null;
        Object value=current.get(host);String id=value instanceof String ? (String)value:sp.getString("current_engine",null);
        if(!EngineChoice.validNativeId(id) || id.equals("aisearch") || id.equals("onesearch"))return null;
        Object p=provider.get(host),s=p==null ? null:loaded.get(p);if(s==null)return null;
        Object modes=map.get(s);String key=(String)scene.get(host);
        if(modes==null || modes.getClass()!=LinkedHashMap.class ||
            !("globalSearchSearchBox".equals(key) || "globalSearchHotList".equals(key)))return null;
        return id.equals(((LinkedHashMap<?,?>)modes).get(key)) && id.equals(sp.getString("current_engine",null)) ? id:null;
    }
}
