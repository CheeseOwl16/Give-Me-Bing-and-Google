package dev.cheeseowl.gmbg;

import android.app.Activity;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.RadioGroup;
import android.widget.RadioButton;

/** Local feature booleans and a closed engine enum. No query/template storage. */
public final class InfoActivity extends Activity {
    private Switch iconSwitch;
    private TextView iconStatus;
    private LauncherIconController iconController;
    private boolean syncingIcon;
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
        layout.addView(status);
        addIconSwitch(layout);
        ScrollView scroll=new ScrollView(this);scroll.addView(layout);setContentView(scroll);
    }
    private void addIconSwitch(LinearLayout layout) {
        ComponentName alias=new ComponentName(this,getPackageName()+".LauncherAlias");
        PackageManager pm=getPackageManager();
        iconController=new LauncherIconController(new LauncherIconController.Backend() {
            @Override public int read() { return pm.getComponentEnabledSetting(alias); }
            @Override public void setVisible(boolean visible) {
                pm.setComponentEnabledSetting(alias,visible?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
            }
        });
        iconSwitch=new Switch(this);iconSwitch.setText(R.string.hide_icon);layout.addView(iconSwitch);
        TextView description=new TextView(this);description.setText(R.string.hide_icon_description);layout.addView(description);
        iconStatus=new TextView(this);layout.addView(iconStatus);
        syncIcon(iconController.read());
        iconSwitch.setOnCheckedChangeListener((button,hidden)->{
            if(syncingIcon)return;
            LauncherIconController.Result result=iconController.setHidden(hidden);
            syncIcon(result.state);
            int message=result.success?(hidden?R.string.hide_icon_hint:R.string.icon_restored):R.string.icon_failed;
            iconStatus.setText(message);Toast.makeText(this,message,Toast.LENGTH_LONG).show();
        });
    }
    private void syncIcon(LauncherIconController.State state) {
        syncingIcon=true;
        try {
            iconSwitch.setEnabled(state!=LauncherIconController.State.UNKNOWN);
            iconSwitch.setChecked(state==LauncherIconController.State.HIDDEN);
            if(state==LauncherIconController.State.UNKNOWN)iconStatus.setText(R.string.icon_unknown);
            else iconStatus.setText("");
        } finally { syncingIcon=false; }
    }
    @Override protected void onResume() {
        super.onResume();
        if(iconController!=null)syncIcon(iconController.read());
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
