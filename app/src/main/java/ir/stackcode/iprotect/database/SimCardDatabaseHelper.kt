package ir.stackcode.iprotect.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ir.stackcode.iprotect.models.SimCard

class SimCardDatabaseHelper(val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun add(ICCID: String) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(SimCard.COLUMN_ICCID_ID, ICCID)
        }

        db?.insert(SimCard.TABLE, null, values)
    }

    fun edit(ICCID: String, wifiBlocked: Boolean, otherBlocked: Boolean) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(SimCard.COLUMN_WIFI, if (wifiBlocked) 1 else 0)
            put(SimCard.COLUMN_OTHER, if (otherBlocked) 1 else 0)
        }

        val selection = "${SimCard.COLUMN_ICCID_ID} = ?"
        val selectionArgs = arrayOf(ICCID)
        db.update(
            SimCard.TABLE,
            values,
            selection,
            selectionArgs
        )

    }

    fun remove(ICCD: String) {
        val db = writableDatabase
        db.delete(SimCard.TABLE, "${SimCard.COLUMN_ICCID_ID} = ?", arrayOf(ICCD))
    }

    fun removeAll() {
        val db = writableDatabase
        db.delete(SimCard.TABLE, null, null)
    }

    fun getSize() = all().size

    fun get(ICCID: String): SimCard? {
        val db = readableDatabase

        val projection =
            arrayOf(SimCard.COLUMN_ID, SimCard.COLUMN_WIFI, SimCard.COLUMN_OTHER, SimCard.COLUMN_ICCID_ID)

        val selection = "${SimCard.COLUMN_ICCID_ID} = ?"
        val selectionArgs = arrayOf(ICCID)
        val sortOrder = "${SimCard.COLUMN_ID} DESC"

        val cursor = db.query(
            SimCard.TABLE,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            selection,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )

        val permissions = ArrayList<SimCard>()
        val simCard: SimCard

        with(cursor) {
            if (!moveToFirst())
                return null

            simCard = SimCard(
                ICCID = getString(getColumnIndexOrThrow(SimCard.COLUMN_ICCID_ID)),
                wifi = getInt(getColumnIndexOrThrow(SimCard.COLUMN_WIFI)) == 1,
                other = getInt(getColumnIndexOrThrow(SimCard.COLUMN_OTHER)) == 1,
                id = getInt(getColumnIndexOrThrow(SimCard.COLUMN_ID))
            )
        }
        cursor.close()

        return simCard
    }

    fun all(): ArrayList<SimCard> {
        val db = readableDatabase

        val projection =
            arrayOf(SimCard.COLUMN_ID, SimCard.COLUMN_ICCID_ID, SimCard.COLUMN_WIFI, SimCard.COLUMN_OTHER)

//        val selection = "${FeedEntry.COLUMN_NAME_TITLE} = ?"
//        val selectionArgs = arrayOf("My Title")
        val sortOrder = "${SimCard.COLUMN_ID} DESC"

        val cursor = db.query(
            SimCard.TABLE,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            null,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )

        val permissions = ArrayList<SimCard>()
        with(cursor) {
            while (moveToNext())
                permissions.add(
                    SimCard(
                        ICCID = getString(getColumnIndexOrThrow(SimCard.COLUMN_ICCID_ID)),
                        wifi = getInt(getColumnIndexOrThrow(SimCard.COLUMN_WIFI)) == 1,
                        other = getInt(getColumnIndexOrThrow(SimCard.COLUMN_OTHER)) == 1,
                        id = getInt(getColumnIndexOrThrow(SimCard.COLUMN_ID))
                    )
                )
        }
        cursor.close()

        return permissions
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = SimCard.TABLE + ".db"

        private const val SQL_CREATE_ENTRIES = "CREATE TABLE " + SimCard.TABLE + " (" +
                "${SimCard.COLUMN_ID} INTEGER PRIMARY KEY," +
                "${SimCard.COLUMN_WIFI} INTEGER DEFAULT 0," +
                "${SimCard.COLUMN_OTHER} INTEGER DEFAULT 0," +
                "${SimCard.COLUMN_ICCID_ID} TEXT)"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + SimCard.TABLE
    }
}