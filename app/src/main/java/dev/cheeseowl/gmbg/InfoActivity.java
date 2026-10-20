package dev.cheeseowl.gmbg;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.RadioGroup;
import android.widget.RadioButton;

/** Local feature booleans and a closed engine enum. No query/template storage. */
public final class InfoActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(32,40,32,32);
        TextView info=new TextView(this);info.setText(R.string.diagnostic_info);layout.addView(info);
        TextView status=new TextView(this);
        SharedPreferences settings=getSharedPreferences(FeatureProvider.FILE,MODE_PRIVATE);
        addSwitch(layout,settings,"takeover_enabled",R.string.engine_switch,false,status);
        RadioGroup engines=new RadioGroup(this);
        RadioButton bing=new RadioButton(this);bing.setId(R.id.engine_bing);bing.setText(R.string.bing_label);engines.addView(bing);
        RadioButton google=new RadioButton(this);google.setId(R.id.engine_google);google.setText(R.string.google_label);engines.addView(google);
        String chosen=settings.getString("engine_choice","bing");
        if("bing".equals(chosen))engines.check(R.id.engine_bing);else if("google".equals(chosen))engines.check(R.id.engine_google);
        engines.setOnCheckedChangeListener((group,id)->save(settings,settings.edit().putString("engine_choice",id==R.id.engine_google?"google":"bing"),status));
        layout.addView(engines);
        addSwitch(layout,settings,"browser_enabled",R.string.browser_switch,true,status);
        layout.addView(status);setContentView(layout);
    }
    private void addSwitch(LinearLayout layout,SharedPreferences settings,String key,int label,boolean defaultValue,TextView status){
        Switch control=new Switch(this);control.setText(label);control.setChecked(settings.getBoolean(key,defaultValue));
        control.setOnCheckedChangeListener((button,checked)->save(settings,settings.edit().putBoolean(key,checked),status));layout.addView(control);
    }
    private void save(SharedPreferences settings,SharedPreferences.Editor editor,TextView status){
        long revision=settings.getLong("revision",0);
        status.setText(revision>=0 && revision<Long.MAX_VALUE && editor.putLong("revision",revision+1).commit()?R.string.saved:R.string.save_failed);
    }
}
