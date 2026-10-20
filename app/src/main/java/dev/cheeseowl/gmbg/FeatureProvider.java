package dev.cheeseowl.gmbg;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/** Public, read-only booleans/engine enum/schema/revision. No external writes or search data. */
public final class FeatureProvider extends ContentProvider {
    static final String AUTHORITY="dev.cheeseowl.gmbg.flags";
    static final String FILE="features";
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        if(!"flags".equals(method) || getContext()==null)return null;
        SharedPreferences p=getContext().getSharedPreferences(FILE,0);
        // SharedPreferences.getAll returns one snapshot: do not mix old enum with new revision.
        java.util.Map<String,?> snapshot=p.getAll();
        Object takeover=snapshot.get("takeover_enabled"),browser=snapshot.get("browser_enabled"),choice=snapshot.get("engine_choice"),revision=snapshot.get("revision");
        if((takeover!=null && !(takeover instanceof Boolean)) || (browser!=null && !(browser instanceof Boolean)) ||
            (choice!=null && !(choice instanceof String)) || (revision!=null && !(revision instanceof Long)))return null;
        FeatureState state=SettingsPolicy.read((Boolean)takeover,(Boolean)browser,(String)choice,
            snapshot.containsKey("engine_choice"),revision==null?0:(Long)revision);
        if(!state.valid())return null;
        Bundle b=new Bundle();b.putInt("schema",2);
        b.putBoolean("takeover_enabled",state.engine());b.putBoolean("browser_enabled",state.browser());
        b.putString("engine_choice",state.choice().id);b.putLong("revision",state.revision());return b;
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){return null;}
    @Override public String getType(Uri uri){return null;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
}
