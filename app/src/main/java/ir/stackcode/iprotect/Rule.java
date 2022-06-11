package ir.stackcode.iprotect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Rule implements Comparable<Rule> {
    public ResolveInfo info;
    public String name;
    public boolean system;
    public boolean disabled;
    public boolean wifi_blocked;
    public boolean other_blocked;
    public boolean changed;

    private Rule(ResolveInfo info, boolean wifi_blocked, boolean other_blocked, boolean changed, Context context) {
        PackageManager pm = context.getPackageManager();
        this.info = info;
        this.name = info.loadLabel(pm).toString();
        this.system = ((info.activityInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

        int setting = pm.getApplicationEnabledSetting(info.activityInfo.packageName);
        if (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            this.disabled = !info.activityInfo.enabled;
        else
            this.disabled = (setting != PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        this.wifi_blocked = wifi_blocked;
        this.other_blocked = other_blocked;
        this.changed = changed;
    }

    public static List<Rule> getRules(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences wifi = context.getSharedPreferences("wifi", Context.MODE_PRIVATE);
        SharedPreferences other = context.getSharedPreferences("other", Context.MODE_PRIVATE);

        boolean wlWifi = prefs.getBoolean("whitelist_wifi", true);
        boolean wlOther = prefs.getBoolean("whitelist_other", true);

        List<Rule> listRules = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(mainIntent, 0)) {
            boolean blWifi = wifi.getBoolean(info.activityInfo.packageName, wlWifi);
            boolean blOther = other.getBoolean(info.activityInfo.packageName, wlOther);
            boolean changed = (blWifi != wlWifi || blOther != wlOther);
            listRules.add(new Rule(info, blWifi, blOther, changed, context));
        }

        Collections.sort(listRules);

        return listRules;
    }

    public Drawable getIcon(Context context) {
        return info.activityInfo.loadIcon(context.getPackageManager());
    }

    @Override
    public int compareTo(Rule other) {
        if (changed == other.changed) {
            int i = name.compareToIgnoreCase(other.name);
            return (i == 0 ? info.activityInfo.packageName.compareTo(other.info.activityInfo.packageName) : i);
        }
        return (changed ? -1 : 1);
    }
}
