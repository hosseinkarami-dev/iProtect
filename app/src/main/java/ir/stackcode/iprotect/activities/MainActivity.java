package ir.stackcode.iprotect.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.stackcode.iprotect.BaseActivity;
import ir.stackcode.iprotect.BlackHoleService;
import ir.stackcode.iprotect.R;
import ir.stackcode.iprotect.Rule;
import ir.stackcode.iprotect.RuleAdapter;
import ir.stackcode.iprotect.Util;
import ir.stackcode.iprotect.helpers.HelperPreferences;

public class MainActivity extends BaseActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Firewall.Main";

    private boolean running = false;
    private RuleAdapter adapter = null;
    private MenuItem searchItem = null;

    private static final int REQUEST_VPN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(prefs.getBoolean("dark_theme", false) ? R.style.AppThemeDark : R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        running = true;

        // Action bar
        View view = getLayoutInflater().inflate(R.layout.actionbar, null);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(view);

        // On/off switch
        SwitchCompat swEnabled = view.findViewById(R.id.swEnabled);
        swEnabled.setChecked(prefs.getBoolean("enabled", false));
        swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.i(TAG, "Switch on");
                Intent prepare = VpnService.prepare(MainActivity.this);
                if (prepare == null) {
                    Log.e(TAG, "Prepare done");
                    onActivityResult(REQUEST_VPN, RESULT_OK, null);
                } else {
                    Log.i(TAG, "Start intent=" + prepare);
                    try {
                        startActivityForResult(prepare, REQUEST_VPN);
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                        onActivityResult(REQUEST_VPN, RESULT_CANCELED, null);
                        Toast.makeText(MainActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Log.i(TAG, "Switch off");
                prefs.edit().putBoolean("enabled", false).apply();
                BlackHoleService.stop(MainActivity.this);
            }
        });

        // Listen for preference changes
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Fill application list
        fillApplicationList();

        // Listen for connectivity updates
        IntentFilter ifConnectivity = new IntentFilter();
        ifConnectivity.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityChangedReceiver, ifConnectivity);

        // Listen for added/removed applications
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        registerReceiver(packageChangedReceiver, intentFilter);

        view.findViewById(R.id.network_wifi).setOnClickListener(v -> {
            boolean blocked = v.getTag().toString().equals("1");

            ((ImageView) v).setImageResource(blocked ? R.drawable.ic_signal_wifi_off_white_24dp : R.drawable.ic_network_wifi_white_24dp);
            v.setTag(blocked ? "0" : "1");
            adapter.changeWifi(!blocked);
        });

        view.findViewById(R.id.network_data).setOnClickListener(v -> {
            boolean blocked = v.getTag().toString().equals("1");
            ((ImageView) v).setImageResource(blocked ? R.drawable.ic_signal_cellular_off_white_24dp : R.drawable.ic_network_cell_white_24dp);
            v.setTag(blocked ? "0" : "1");
            adapter.changeData(!blocked);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 1000);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroy");
        running = false;
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(connectivityChangedReceiver);
        unregisterReceiver(packageChangedReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver connectivityChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received " + intent);
            Util.logExtras(TAG, intent);
            invalidateOptionsMenu();
        }
    };

    private final BroadcastReceiver packageChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received " + intent);
            Util.logExtras(TAG, intent);
            fillApplicationList();
        }
    };

    private void fillApplicationList() {
        // Get recycler view
        final RecyclerView rvApplication = findViewById(R.id.rvApplication);
        rvApplication.setHasFixedSize(true);
        rvApplication.setLayoutManager(new LinearLayoutManager(this));

        // Get/set application list
        new AsyncTask<Object, Object, List<Rule>>() {
            @Override
            protected List<Rule> doInBackground(Object... arg) {
                return Rule.getRules(MainActivity.this);
            }

            @Override
            protected void onPostExecute(List<Rule> result) {
                if (running) {
                    if (searchItem != null)
                        MenuItemCompat.collapseActionView(searchItem);
                    adapter = new RuleAdapter(result, MainActivity.this);
                    rvApplication.setAdapter(adapter);
                }
            }
        }.execute();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String name) {
        Log.i(TAG, "Preference " + name + "=" + prefs.getAll().get(name));
        if ("enabled".equals(name)) {
            // Get enabled
            boolean enabled = prefs.getBoolean(name, false);

            // Check switch state
            SwitchCompat swEnabled = getSupportActionBar().getCustomView().findViewById(R.id.swEnabled);
            if (swEnabled.isChecked() != enabled)
                swEnabled.setChecked(enabled);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        // Search
        searchItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (adapter != null)
                    adapter.getFilter().filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null)
                    adapter.getFilter().filter(newText);
                return true;
            }
        });
        searchView.setOnCloseListener(() -> {
            if (adapter != null)
                adapter.getFilter().filter(null);
            return true;
        });

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        MenuItem wifi = menu.findItem(R.id.menu_whitelist_wifi);
        wifi.setChecked(prefs.getBoolean("whitelist_wifi", true));

        MenuItem other = menu.findItem(R.id.menu_whitelist_other);
        other.setChecked(prefs.getBoolean("whitelist_other", true));

        MenuItem bluetooth = menu.findItem(R.id.menu_block_bluetooth);
        bluetooth.setChecked(!HelperPreferences.INSTANCE.isBluetoothAllowed(this));

//        MenuItem dark = menu.findItem(R.id.menu_dark);
//        dark.setChecked(prefs.getBoolean("dark_theme", false));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Handle item selection
        switch (item.getItemId()) {
//            case R.id.menu_network:
//                Intent settings = new Intent(Util.isWifiActive(this)
//                        ? Settings.ACTION_WIFI_SETTINGS : Settings.ACTION_WIRELESS_SETTINGS);
//                if (settings.resolveActivity(getPackageManager()) != null)
//                    startActivity(settings);
//                else
//                    Log.w(TAG, settings + " not available");
//                return true;

            case R.id.menu_refresh:
                fillApplicationList();
                return true;

            case R.id.menu_simcards:
                startActivity(new Intent(this, SimCardActivity.class));
                return true;

            case R.id.menu_whitelist_wifi:
                prefs.edit().putBoolean("whitelist_wifi", !prefs.getBoolean("whitelist_wifi", true)).apply();
                fillApplicationList();
                BlackHoleService.reload("wifi", this);
                return true;

            case R.id.menu_block_bluetooth:
                HelperPreferences.INSTANCE.allowBluetooth(this, !HelperPreferences.INSTANCE.isBluetoothAllowed(this));
                return true;

            case R.id.menu_whitelist_other:
                prefs.edit().putBoolean("whitelist_other", !prefs.getBoolean("whitelist_other", true)).apply();
                fillApplicationList();
                BlackHoleService.reload("other", this);
                return true;

//            case R.id.menu_reset_wifi:
//                new AlertDialog.Builder(this)
//                        .setMessage(R.string.msg_sure)
//                        .setPositiveButton(android.R.string.yes, (dialog, which) -> reset("wifi"))
//                        .setNegativeButton(android.R.string.no, null)
//                        .show();
//                return true;

//            case R.id.menu_reset_other:
//                new AlertDialog.Builder(this)
//                        .setMessage(R.string.msg_sure)
//                        .setPositiveButton(android.R.string.yes, (dialog, which) -> reset("other"))
//                        .setNegativeButton(android.R.string.no, null)
//                        .show();
//                return true;

//            case R.id.menu_dark:
//                prefs.edit().putBoolean("dark_theme", !prefs.getBoolean("dark_theme", false)).apply();
//                recreate();
//                return true;

            case R.id.menu_about:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://stackcode.ir")));
                return true;

            case R.id.menu_vpn_settings:
                // Open VPN settings
                Intent vpn = new Intent("android.net.vpn.SETTINGS");
                if (vpn.resolveActivity(getPackageManager()) != null)
                    startActivity(vpn);
                else
                    Log.w(TAG, vpn + " not available");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void reset(String network) {
        SharedPreferences other = getSharedPreferences(network, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = other.edit();
        for (String key : other.getAll().keySet())
            edit.remove(key);
        edit.apply();
        fillApplicationList();
        BlackHoleService.reload(network, MainActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_VPN) {
            // Update enabled state
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("enabled", resultCode == RESULT_OK).apply();

            // Start service
            if (resultCode == RESULT_OK)
                BlackHoleService.start(this);
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
