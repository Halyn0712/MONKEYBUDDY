package com.shuagoumei.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lets the user pick which installed apps to monitor. */
public class AppPickerActivity extends Activity {

    private static class Item {
        String pkg;
        String label;
        Drawable icon;
    }

    private final List<Item> items = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private PackageManager pm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        selected.addAll(Prefs.getWhitelist(this));
        loadApps();
        setContentView(buildUi());
    }

    private void loadApps() {
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(main, 0);
        Set<String> seen = new HashSet<>();
        String self = getPackageName();
        for (ResolveInfo ri : resolved) {
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(self) || !seen.add(pkg)) continue;
            Item it = new Item();
            it.pkg = pkg;
            it.label = ri.loadLabel(pm).toString();
            it.icon = ri.loadIcon(pm);
            items.add(it);
        }
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(items, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) {
                return collator.compare(a.label, b.label);
            }
        });
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFDF6EC);

        TextView hint = new TextView(this);
        hint.setText("勾选你想管住的应用。打开它们时，Monkey Face 会先提醒你约定时长。");
        hint.setTextColor(0xFFA08A76);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        int pad = dp(16);
        hint.setPadding(pad, pad, pad, dp(8));
        root.addView(hint);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        final AppAdapter adapter = new AppAdapter();
        list.setAdapter(adapter);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        list.setLayoutParams(lp);
        root.addView(list);

        Button save = new Button(this);
        save.setAllCaps(false);
        save.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        save.setTextColor(Color.WHITE);
        save.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMargins(pad, dp(8), pad, dp(16));
        save.setLayoutParams(sp);
        updateSaveText(save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.setWhitelist(AppPickerActivity.this, selected);
                Toast.makeText(AppPickerActivity.this,
                        "已保存 " + selected.size() + " 个应用", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        root.addView(save);
        return root;
    }

    private void updateSaveText(Button save) {
        save.setText("保存名单（已选 " + selected.size() + "）");
    }

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
            } else {
                row = new LinearLayout(AppPickerActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                int p = dp(14);
                row.setPadding(p, dp(10), p, dp(10));

                ImageView icon = new ImageView(AppPickerActivity.this);
                icon.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
                row.addView(icon);

                TextView label = new TextView(AppPickerActivity.this);
                label.setTextColor(0xFF3A2A1E);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                llp.setMargins(dp(12), 0, dp(12), 0);
                label.setLayoutParams(llp);
                row.addView(label);

                CheckBox cb = new CheckBox(AppPickerActivity.this);
                cb.setClickable(false);
                cb.setFocusable(false);
                row.addView(cb);
            }

            final Item it = items.get(position);
            ((ImageView) row.getChildAt(0)).setImageDrawable(it.icon);
            ((TextView) row.getChildAt(1)).setText(it.label);
            final CheckBox cb = (CheckBox) row.getChildAt(2);
            cb.setChecked(selected.contains(it.pkg));

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (selected.contains(it.pkg)) {
                        selected.remove(it.pkg);
                        cb.setChecked(false);
                    } else {
                        selected.add(it.pkg);
                        cb.setChecked(true);
                    }
                }
            });
            return row;
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
