package dev.cheeseowl.gmbg;

import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;

/** Reads fields on objects already supplied by the invocation. No QSB method calls or singleton creation. */
final class ReadState {
    private final ClassLoader loader;
    ReadState(ClassLoader loader) { this.loader=loader; }
    private Object field(Object receiver, String owner, String name, String type) throws ReflectiveOperationException {
        if (receiver == null) return null;
        Class<?> cls = Class.forName(owner, false, loader);
        if (!cls.isInstance(receiver)) throw new IllegalArgumentException();
        Field f=cls.getDeclaredField(name);
        if (!f.getType().getName().equals(type)) throw new NoSuchFieldException();
        f.setAccessible(true);
        return f.get(receiver);
    }
    Object[] selection(Object receiver) throws ReflectiveOperationException {
        Object mem=field(receiver,TargetSpec.F,"c","java.lang.String");
        Object scene=field(receiver,TargetSpec.F,"a","java.lang.String");
        Object pref=field(receiver,TargetSpec.F,"d","android.content.SharedPreferences");
        Privacy.Engine saved=Privacy.Engine.UNKNOWN;
        Privacy.Tri clicked=Privacy.Tri.UNKNOWN;
        if (pref instanceof SharedPreferences) {
            // This is the existing in-process SP cache, NOT independent proof of disk persistence.
            SharedPreferences sp=(SharedPreferences)pref;
            saved=sp.contains("current_engine") ? Privacy.engine(sp.getString("current_engine",null)) : Privacy.Engine.NONE;
            clicked=sp.contains("engine_click_count") ? Privacy.tri(sp.getBoolean("engine_click_count",false)) : Privacy.Tri.UNKNOWN;
        }
        Object provider=field(receiver,TargetSpec.F,"f",TargetSpec.C);
        Object[] scenes=provider(provider);
        return new Object[]{Privacy.engine(mem),saved,clicked,Privacy.scene(scene),scenes[0],scenes[1]};
    }
    Object[] provider(Object receiver) throws ReflectiveOperationException {
        return scenes(field(receiver,TargetSpec.C,"d","E2.a"));
    }
    Object[] scenes(Object set) throws ReflectiveOperationException {
        Object value=field(set,"E2.a","d","java.lang.Object");
        if (value == null || value.getClass() != LinkedHashMap.class)
            return new Object[]{Privacy.Engine.UNKNOWN, Privacy.Engine.UNKNOWN};
        LinkedHashMap<?,?> map=(LinkedHashMap<?,?>)value;
        return new Object[]{Privacy.engine(map.get("globalSearchSearchBox")),Privacy.engine(map.get("globalSearchHotList"))};
    }
    Object[] parsed(Object set) throws ReflectiveOperationException {
        if (set == null) return new Object[]{Privacy.Tri.UNKNOWN,Privacy.Tri.UNKNOWN,Privacy.Engine.UNKNOWN,Privacy.Engine.UNKNOWN};
        Object box=field(set,"E2.a","b","java.lang.Object");
        Object hot=field(set,"E2.a","c","java.lang.Object");
        Object[] scenes=scenes(set);
        return new Object[]{Privacy.tri(field(box,TargetSpec.J,"e","boolean")),
            Privacy.tri(field(hot,TargetSpec.J,"e","boolean")), scenes[0],scenes[1]};
    }
}
