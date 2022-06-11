package ir.stackcode.iprotect.helpers

import android.content.Context
import android.preference.PreferenceManager

object HelperPreferences {
    fun allowBluetooth(context: Context, show: Boolean) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putBoolean("bluetooth_on", show)
        editor.apply()
    }

    fun isBluetoothAllowed(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("bluetooth_on", true)
    }
}