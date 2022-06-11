package ir.stackcode.iprotect.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import ir.stackcode.iprotect.R
import ir.stackcode.iprotect.SimCardManager
import ir.stackcode.iprotect.Util
import ir.stackcode.iprotect.database.SimCardDatabaseHelper


object HelperSimCard {
    fun isAllowed(context: Context): Boolean {
        val database = SimCardDatabaseHelper(context)

        val wifi = Util.isWifiActive(context)

        val simCards = database.all()
        if (simCards.size == 0)
            return true
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                context.getString(R.string.phone_state_permission_not_allowed),
                Toast.LENGTH_LONG
            ).show()

            return true
        }

        val sis = SimCardManager.getICCIDList(context)

        sis.forEach { si ->
            simCards.forEach { si2 ->
                if (
                // si.subscriptionId == getDefaultDataSubscriptionId(sm) &&
                    si == si2.ICCID && !(if (wifi) si2.wifi else si2.other))
                    return true
            }
        }

        return false
    }

//
//    private fun getDefaultDataSubscriptionId(subscriptionManager: SubscriptionManager): Int {
//        if (Build.VERSION.SDK_INT >= 24) {
//            val nDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId()
//            if (nDataSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
//                return nDataSubscriptionId
//            }
//        }
//        try {
//            val subscriptionClass = Class.forName(subscriptionManager.javaClass.name)
//            try {
//                val getDefaultDataSubscriptionId: Method =
//                    subscriptionClass.getMethod("getDefaultDataSubId")
//                try {
//                    return getDefaultDataSubscriptionId.invoke(subscriptionManager).toString()
//                        .toInt()
//                } catch (e1: IllegalAccessException) {
//                    e1.printStackTrace()
//                } catch (e1: InvocationTargetException) {
//                    e1.printStackTrace()
//                }
//            } catch (e1: NoSuchMethodException) {
//                e1.printStackTrace()
//            }
//        } catch (e1: ClassNotFoundException) {
//            e1.printStackTrace()
//        }
//        return -1
//    }
}