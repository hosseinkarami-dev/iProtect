package ir.stackcode.iprotect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.ViewHolder> implements Filterable {
    private static final String TAG = "Firewall.Adapter";

    private final Context context;
    private final int colorText;
    private final int colorAccent;
    private final List<Rule> listAll;
    private final List<Rule> listSelected;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View view;
        public ImageView ivIcon;
        public TextView tvName;
        public TextView tvPackage;
        public CheckBox cbWifi;
        public CheckBox cbOther;

        public ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvPackage = itemView.findViewById(R.id.tvPackage);
            cbWifi = itemView.findViewById(R.id.cbWifi);
            cbOther = itemView.findViewById(R.id.cbOther);
        }
    }

    public RuleAdapter(List<Rule> listRule, Context context) {
        this.context = context;
        colorAccent = ContextCompat.getColor(context, R.color.colorAccent);
        TypedArray ta = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary});
        try {
            colorText = ta.getColor(0, 0);
        } finally {
            ta.recycle();
        }
        listAll = listRule;
        listSelected = new ArrayList<>();
        listSelected.addAll(listRule);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void changeWifi(boolean blocked) {
        for (Rule rule : listSelected) {
            rule.wifi_blocked = blocked;

            SharedPreferences prefs = context.getSharedPreferences("wifi", Context.MODE_PRIVATE);
            prefs.edit().putBoolean(rule.info.activityInfo.packageName, blocked).apply();
        }

        BlackHoleService.reload("wifi", context);

        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void changeData(boolean blocked) {
        for (Rule rule : listSelected) {
            rule.other_blocked = blocked;

            SharedPreferences prefs = context.getSharedPreferences("other", Context.MODE_PRIVATE);
            prefs.edit().putBoolean(rule.info.activityInfo.packageName, blocked).apply();
        }

        BlackHoleService.reload("other", context);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        // Get rule
        final Rule rule = listSelected.get(position);

        // Rule change listener
        CompoundButton.OnCheckedChangeListener cbListener = (buttonView, isChecked) -> {
            String network;
            if (buttonView == holder.cbWifi) {
                network = "wifi";
                rule.wifi_blocked = isChecked;
            } else {
                network = "other";
                rule.other_blocked = isChecked;
            }
            Log.i(TAG, rule.info.activityInfo.packageName + ": " + network + "=" + isChecked);

            SharedPreferences prefs = context.getSharedPreferences(network, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(rule.info.activityInfo.packageName, isChecked).apply();

            BlackHoleService.reload(network, context);
        };

        int color = rule.system ? colorAccent : colorText;
        if (rule.disabled)
            color = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color));

        holder.ivIcon.setImageDrawable(rule.getIcon(context));
        holder.tvName.setText(rule.name);
        holder.tvName.setTextColor(color);
        holder.tvPackage.setText(rule.info.activityInfo.packageName);
        holder.tvPackage.setTextColor(color);

        holder.cbWifi.setOnCheckedChangeListener(null);
        holder.cbWifi.setChecked(rule.wifi_blocked);
        holder.cbWifi.setOnCheckedChangeListener(cbListener);

        holder.cbOther.setOnCheckedChangeListener(null);
        holder.cbOther.setChecked(rule.other_blocked);
        holder.cbOther.setOnCheckedChangeListener(cbListener);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence query) {
                List<Rule> listResult = new ArrayList<>();
                if (query == null)
                    listResult.addAll(listAll);
                else {
                    query = query.toString().toLowerCase();
                    for (Rule rule : listAll)
                        if (rule.name.toLowerCase().contains(query))
                            listResult.add(rule);
                }

                FilterResults result = new FilterResults();
                result.values = listResult;
                result.count = listResult.size();
                return result;
            }

            @Override
            protected void publishResults(CharSequence query, FilterResults result) {
                listSelected.clear();
                if (result == null)
                    listSelected.addAll(listAll);
                else
                    for (Rule rule : (List<Rule>) result.values)
                        listSelected.add(rule);
                notifyDataSetChanged();
            }
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.rule, parent, false));
    }

    @Override
    public int getItemCount() {
        return listSelected.size();
    }
}
