package ir.stackcode.iprotect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.core.app.ActivityCompat

object SimCardManager {
    fun getICCIDList(context: Context): ArrayList<String> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ArrayList()
        }

        val list = ArrayList<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val tm2 = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val phoneAccounts = tm2.callCapablePhoneAccounts.listIterator()
            phoneAccounts.forEach {
                if (it.id.length == 20)
                    list.add(it.id.toString())
                if (it.id.length >= 21)
                    list.add(it.id.substring(1, 21))
            }

        } else {
            val sis =
                (context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager).activeSubscriptionInfoList

            sis.forEach {
                list.add(it.iccId)
            }
        }
        return list
    }
}