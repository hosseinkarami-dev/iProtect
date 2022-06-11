package ir.stackcode.iprotect.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import ir.stackcode.iprotect.BlackHoleService
import ir.stackcode.iprotect.R
import ir.stackcode.iprotect.database.SimCardDatabaseHelper
import ir.stackcode.iprotect.databinding.SimcardItemBinding
import ir.stackcode.iprotect.models.SimCard

class SimCardListAdapter(
    context: Context,
    items: ArrayList<SimCard>
) : ArrayAdapter<SimCard>(context, R.layout.simcard_item, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        var convertView1 = convertView

        val binding: SimcardItemBinding

        if (convertView1 == null) {
            binding =
                SimcardItemBinding.inflate(LayoutInflater.from(context), parent, false)
            convertView1 = binding.root
            convertView1.tag = binding
        } else
            binding = convertView1.tag as SimcardItemBinding

        item?.let {
            binding.name.text = item.ICCID
            val dbHelper = SimCardDatabaseHelper(context)

            // Rule change listener
            val cbListener =
                CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
                    var wifiBlocked = it.wifi
                    var otherBlocked = it.other

                    val network = if (buttonView === binding.cbWifi) {
                        wifiBlocked = isChecked
                        "wifi"
                    } else {
                        otherBlocked = isChecked
                        "other"
                    }

                    it.wifi = wifiBlocked
                    it.other = otherBlocked

                    dbHelper.edit(item.ICCID, wifiBlocked, otherBlocked)
                    BlackHoleService.reload(network, context)
                }

            binding.cbWifi.setOnCheckedChangeListener(null)
            binding.cbWifi.isChecked = it.wifi
            binding.cbWifi.setOnCheckedChangeListener(cbListener)

            binding.cbOther.setOnCheckedChangeListener(null)
            binding.cbOther.isChecked = it.other
            binding.cbOther.setOnCheckedChangeListener(cbListener)

            binding.remove.setOnClickListener {
                listener?.onClick(position)
            }

        }

        return convertView1
    }

    interface OnItemClickListener {
        fun onClick(position: Int)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
}