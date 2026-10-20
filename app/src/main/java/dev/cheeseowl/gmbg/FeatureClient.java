package dev.cheeseowl.gmbg;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

/** Each relevant operation reads fresh settings; no stale enabled cache or search data sent to the provider. */
final class FeatureClient {
    private final Context context;
    FeatureClient(Context context){this.context=context;}
    FeatureState read(){
        try {
            Bundle b=context.getContentResolver().call(Uri.parse("content://"+FeatureProvider.AUTHORITY),"flags",null,null);
            if(b==null || b.getInt("schema",0)!=2 || !b.containsKey("takeover_enabled") || !b.containsKey("browser_enabled")
                || !b.containsKey("engine_choice") || !b.containsKey("revision"))return FeatureState.unavailable();
            return FeatureState.checked(b.getBoolean("takeover_enabled",false),b.getBoolean("browser_enabled",false),true,
                EngineChoice.from(b.getString("engine_choice")),b.getLong("revision",-1));
        }catch(Throwable ignored){return FeatureState.unavailable();}
    }
}
