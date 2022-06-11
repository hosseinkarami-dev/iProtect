package ir.stackcode.iprotect.receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.stackcode.iprotect.helpers.HelperPreferences

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED && !HelperPreferences.isBluetoothAllowed(
                context
            )
        )
            BluetoothAdapter.getDefaultAdapter().disable()
    }
}