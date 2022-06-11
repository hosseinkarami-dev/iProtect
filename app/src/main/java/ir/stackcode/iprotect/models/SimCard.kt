package ir.stackcode.iprotect.models

import android.provider.BaseColumns

data class SimCard(
    val id: Int? = null,
    val ICCID: String,
    var wifi: Boolean,
    var other: Boolean,
) {
    companion object {
        const val COLUMN_ICCID_ID = "iccid_id"
        const val COLUMN_WIFI = "wifi"
        const val COLUMN_OTHER = "other"
        const val COLUMN_ID = BaseColumns._ID
        const val TABLE = "simcards"
    }
}