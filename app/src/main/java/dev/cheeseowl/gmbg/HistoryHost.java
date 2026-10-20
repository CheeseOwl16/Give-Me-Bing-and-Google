package dev.cheeseowl.gmbg;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import java.lang.reflect.Field;

/** Read-only metadata at the pinned history receiver, never a global Intent hook. */
final class HistoryHost {
    private final Class<?> receiver,view;private final Field branch,owner,resumed;
    HistoryHost(ClassLoader loader) throws ReflectiveOperationException {
        receiver=Class.forName("m2.h",false,loader);view=Class.forName("y2.V",false,loader);
        if(!View.class.isAssignableFrom(view))throw new NoSuchFieldException();
        branch=field(receiver,"a",int.class);owner=field(receiver,"b",view);resumed=field(view,"E",boolean.class);
    }
    private static Field field(Class<?> c,String name,Class<?> type) throws ReflectiveOperationException {
        Field f=c.getDeclaredField(name);if(f.getType()!=type)throw new NoSuchFieldException();f.setAccessible(true);return f;
    }
    HistoryScope.Delivery read(Object object,Object argument) throws IllegalAccessException {
        if(object==null || object.getClass()!=receiver || !(argument instanceof Intent))return null;
        int variant=branch.getInt(object);Object v=owner.get(object);Intent i=(Intent)argument;
        if(variant!=1 || !view.isInstance(v) || !resumed.getBoolean(v) || !"action_search_history".equals(i.getAction()))return null;
        Context c=((View)v).getContext();if(c==null || !TargetSpec.PACKAGE.equals(c.getPackageName()))return null;
        String from=i.getStringExtra("intent_extra_from"),tab=i.getStringExtra("intent_extra_tab"),type=i.getStringExtra("intent_extra_type");
        boolean search=i.getBooleanExtra("intent_extra_search",false);
        // Read the private word only for the narrowly verified plain-web branch; never log it.
        String query="history_ime_g".equals(from) && "web_all".equals(tab) && "2".equals(type) && search?
            i.getStringExtra("intent_extra_word"):null;
        return new HistoryScope.Delivery(object,i,v,c,variant,true,i.getAction(),from,tab,type,search,query);
    }
}
