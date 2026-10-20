package dev.cheeseowl.gmbg;

import android.content.SharedPreferences;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;

/** Read-only native model plus detached per-request item construction. No catalog/SP writes. */
final class EngineHost {
    record Candidate(Object host,Object model,Object item,String template,CatalogPolicy.Origin origin){}
    private final Field instance,current,prefs,scene,provider,loaded,items,effective,id,url;
    private final Class<?> itemClass;private final Constructor<?> constructor;
    EngineHost(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> f=Class.forName(TargetSpec.F,false,loader),c=Class.forName(TargetSpec.C,false,loader),set=Class.forName("E2.a",false,loader);
        itemClass=Class.forName(TargetSpec.PACKAGE+".xiaomi.searchengine.h",false,loader);
        instance=field(f,"h",f);if(!Modifier.isStatic(instance.getModifiers()))throw new NoSuchFieldException();
        current=field(f,"c",String.class);prefs=field(f,"d",SharedPreferences.class);scene=field(f,"a",String.class);
        provider=field(f,"f",c);loaded=field(c,"d",set);items=field(set,"e",Object.class);effective=field(set,"d",Object.class);
        id=field(itemClass,"a",String.class);url=field(itemClass,"d",String.class);
        constructor=itemClass.getDeclaredConstructor(String.class,String.class,boolean.class,String.class,String.class,
            String.class,String.class,String.class,String.class,String.class);
        if(!Modifier.isPublic(constructor.getModifiers()))throw new NoSuchMethodException();
    }
    private static Field field(Class<?> cls,String name,Class<?> type) throws ReflectiveOperationException {
        Field result=cls.getDeclaredField(name);if(result.getType()!=type)throw new NoSuchFieldException();result.setAccessible(true);return result;
    }
    Object singleton() throws IllegalAccessException{return instance.get(null);}
    private Object model(Object host) throws IllegalAccessException {Object p=provider.get(host);return p==null?null:loaded.get(p);}
    private LinkedHashMap<?,?> box(Object model) throws IllegalAccessException {
        if(model==null)return null;Object all=items.get(model);if(all==null || all.getClass()!=LinkedHashMap.class)return null;
        Object map=((LinkedHashMap<?,?>)all).get("globalSearchSearchBox");
        return map!=null && map.getClass()==LinkedHashMap.class && ((LinkedHashMap<?,?>)map).size()<=64 ? (LinkedHashMap<?,?>)map:null;
    }
    private boolean ordinary(Object host,Object model) throws IllegalAccessException {
        if(!"globalSearchSearchBox".equals(scene.get(host)))return false;
        Object memory=current.get(host);SharedPreferences sp=(SharedPreferences)prefs.get(host);
        String nativeId=memory instanceof String && !((String)memory).isEmpty()?(String)memory:sp==null?null:sp.getString("current_engine",null);
        Object modes=effective.get(model);
        if(!EngineChoice.validNativeId(nativeId) || modes==null || modes.getClass()!=LinkedHashMap.class)return false;
        Object active=((LinkedHashMap<?,?>)modes).get("globalSearchSearchBox");
        return active instanceof String && EngineChoice.validNativeId((String)active) && !"aisearch".equals(nativeId) && !"onesearch".equals(nativeId)
            && !"aisearch".equals(active) && !"onesearch".equals(active);
    }
    Candidate select(Object host,EngineChoice choice) throws ReflectiveOperationException {
        if(host==null || singleton()!=host || choice==EngineChoice.UNKNOWN)return null;
        Object model=model(host);LinkedHashMap<?,?> map=box(model);
        if(map==null || !ordinary(host,model))return null;
        CatalogPolicy.Selected selected=CatalogPolicy.choose(map.containsKey(choice.id),map.get(choice.id),
            () -> constructor.newInstance(choice.id,"",false,choice.template,"",choice.id,choice.id,choice.id,choice.id,choice.id),
            item -> item.getClass()==itemClass && choice.id.equals(id.get(item)) && url.get(item) instanceof String &&
                SearchUrlPolicy.template(choice,(String)url.get(item)));
        return selected==null?null:new Candidate(host,model,selected.item(),(String)url.get(selected.item()),selected.origin());
    }
    boolean stable(Candidate candidate,EngineChoice choice) throws IllegalAccessException {
        Object host=candidate.host();if(singleton()!=host || model(host)!=candidate.model() || !ordinary(host,candidate.model()))return false;
        LinkedHashMap<?,?> map=box(candidate.model());if(map==null)return false;
        Object existing=map.get(choice.id);
        return (candidate.origin()==CatalogPolicy.Origin.DETACHED_QSB_FIX ? !map.containsKey(choice.id):existing==candidate.item())
            && choice.id.equals(id.get(candidate.item())) && candidate.template().equals(url.get(candidate.item()));
    }
}
