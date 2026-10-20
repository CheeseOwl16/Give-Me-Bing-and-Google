package dev.cheeseowl.gmbg;

import android.content.Context;
import android.view.View;
import java.lang.reflect.Field;

/** Exact DEX fields; no query getter invocation, Intent/extras reading, or native state writes. */
final class NativeSuggestionHost {
    private final Class<?> listenerClass,itemClass;
    private final Field branch,item,holder,root,type,title,category,packageName,shortcut,app,contact,email,function,exposed;
    NativeSuggestionHost(ClassLoader loader) throws ReflectiveOperationException {
        listenerClass=Class.forName("I5.d",false,loader);itemClass=Class.forName("n2.g",false,loader);
        Class<?> holderClass=Class.forName("I5.a",false,loader),viewHolder=Class.forName("androidx.recyclerview.widget.h0",false,loader);
        branch=field(listenerClass,"a",int.class);item=field(listenerClass,"b",itemClass);holder=field(listenerClass,"d",holderClass);
        root=field(viewHolder,"a",View.class);type=field(itemClass,"a",String.class);title=field(itemClass,"e",String.class);
        category=field(itemClass,"p0",String.class);packageName=field(itemClass,"i",String.class);shortcut=field(itemClass,"j",String.class);
        app=field(itemClass,"X",Class.forName("n2.h",false,loader));contact=field(itemClass,"h0",Class.forName("n2.l",false,loader));
        email=field(itemClass,"i0",String.class);function=field(itemClass,"Z",Class.forName("n2.j",false,loader));exposed=field(itemClass,"c0",boolean.class);
    }
    private static Field field(Class<?> owner,String name,Class<?> type) throws ReflectiveOperationException {
        Field field=owner.getDeclaredField(name);if(field.getType()!=type)throw new NoSuchFieldException();field.setAccessible(true);return field;
    }
    private static boolean nonempty(Object value){return value instanceof String && !((String)value).isEmpty();}
    NativeSuggestionScope.Row read(Object listener) throws IllegalAccessException {
        if(listener==null || listener.getClass()!=listenerClass)return null;
        Object selected=item.get(listener),viewHolder=holder.get(listener);if(selected==null || selected.getClass()!=itemClass || viewHolder==null)return null;
        Object view=root.get(viewHolder);if(!(view instanceof View))return null;
        Context context=((View)view).getContext();if(context==null || !TargetSpec.PACKAGE.equals(context.getPackageName()))return null;
        int variant=branch.getInt(listener);String kind=(String)type.get(selected),group=(String)category.get(selected);
        // Do not even fetch titles/local payload fields for a different branch/type/category.
        if(variant!=1 || !"net_sug_word".equals(kind) || (group!=null && !group.isEmpty()))
            return new NativeSuggestionScope.Row(listener,selected,context,variant,kind,null,group,false);
        boolean special=nonempty(packageName.get(selected)) || nonempty(shortcut.get(selected)) || app.get(selected)!=null ||
            contact.get(selected)!=null || nonempty(email.get(selected)) || function.get(selected)!=null || exposed.getBoolean(selected);
        return new NativeSuggestionScope.Row(listener,selected,context,variant,kind,special?null:(String)title.get(selected),group,special);
    }
}
