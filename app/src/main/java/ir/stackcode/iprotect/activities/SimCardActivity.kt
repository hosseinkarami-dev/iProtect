package ir.stackcode.iprotect.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItems
import ir.stackcode.iprotect.BaseActivity
import ir.stackcode.iprotect.BlackHoleService
import ir.stackcode.iprotect.R
import ir.stackcode.iprotect.SimCardManager
import ir.stackcode.iprotect.adapters.SimCardListAdapter
import ir.stackcode.iprotect.database.SimCardDatabaseHelper
import ir.stackcode.iprotect.databinding.ActivitySimcardBinding
import ir.stackcode.iprotect.databinding.SimcardAddLayoutBinding
import ir.stackcode.iprotect.models.SimCard

class SimCardActivity : BaseActivity() {
    private lateinit var database: SimCardDatabaseHelper
    private lateinit var adapter: SimCardListAdapter
    private val items: ArrayList<SimCard> = ArrayList()
    private lateinit var iccids: ArrayList<String>
    private lateinit var binding: ActivitySimcardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(
            if (prefs.getBoolean(
                    "dark_theme",
                    false
                )
            ) R.style.AppThemeDark else R.style.AppTheme
        )

        binding = ActivitySimcardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iccids = SimCardManager.getICCIDList(this)

        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        supportActionBar!!.setTitle(R.string.manage_simcards)

        database = SimCardDatabaseHelper(this)
        items.addAll(database.all())
        adapter = SimCardListAdapter(this, items)

        binding.listview.adapter = adapter

        adapter.setOnItemClickListener(object : SimCardListAdapter.OnItemClickListener {
            @SuppressLint("CheckResult")
            override fun onClick(position: Int) {

                val dialog =
                    MaterialDialog(this@SimCardActivity, BottomSheet(LayoutMode.WRAP_CONTENT))
                        .message(text = "از حذف " + items[position].ICCID + " مطمئن هستید؟")

                dialog.listItems(
                    items = listOf(
                        getString(R.string.ok),
                        getString(R.string.cancel)
                    )
                ) { _, index, _ ->
                    when (index) {
                        0 -> {
                            database.remove(items[position].ICCID)
                            notifyChanged()
                        }

                        1 -> dialog.dismiss()
                    }
                }
                dialog.show()
                dialog.view.layoutDirection = View.LAYOUT_DIRECTION_RTL
            }

        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.simcard, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                val bind = SimcardAddLayoutBinding.inflate(layoutInflater)

                when (iccids.size) {
                    0 -> {
                        bind.simcard1.visibility = View.GONE
                        bind.simcard2.visibility = View.GONE
                    }

                    1 -> {
                        bind.simcard1.visibility = View.VISIBLE
                        bind.simcard2.visibility = View.GONE
                    }

                    2 -> {
                        bind.simcard1.visibility = View.VISIBLE
                        bind.simcard2.visibility = View.VISIBLE
                    }
                }

                bind.simcard1.setOnClickListener {
                    bind.text.setText(iccids[0])
                }

                bind.simcard2.setOnClickListener {
                    bind.text.setText(iccids[1])
                }

                MaterialDialog(this)
                    .title(R.string.add_simcard)
                    .message(R.string.add_iccid_of_simcard)
                    .negativeButton(R.string.cancel)
                    .positiveButton(R.string.ok)
                    .customView(view = bind.root)
                    .positiveButton {
                        val text = bind.text.text.toString()
                        if (text.length != 20) {
                            Toast.makeText(
                                this@SimCardActivity,
                                getString(R.string.iccid_must_be_20_length),
                                Toast.LENGTH_LONG
                            ).show()

                            return@positiveButton
                        }

                        if (database.get(text) != null) {
                            Toast.makeText(
                                this@SimCardActivity,
                                getString(R.string.iccid_already_added),
                                Toast.LENGTH_LONG
                            ).show()
                            return@positiveButton
                        }

                        database.add(text)
                        notifyChanged()
                    }
                    .show()
            }

            R.id.info -> {

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(baseContext, ("خطا رخ داد."), Toast.LENGTH_LONG).show()
                    return false
                }

                var description = ""
                var index = 1

                iccids.forEach {
                    description += "آی سی سی آیدی سیم کارت شماره $index: $it\n"
                    index++
                }

                MaterialDialog(this)
                    .title(R.string.guide)
                    .message(text = description)
                    .negativeButton(R.string.ok)
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun notifyChanged() {
        items.clear()
        items.addAll(database.all())
        adapter.notifyDataSetChanged()
        BlackHoleService.reload(null, this)
    }
}