package com.openwatchproject.launcher.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.room.Room;
import androidx.wear.widget.WearableRecyclerView;

import com.openwatchproject.launcher.databinding.LauncherItemBinding;
import com.openwatchproject.launcher.listener.LauncherItemClickListener;
import com.openwatchproject.launcher.listener.LauncherItemLongClickListener;
import com.openwatchproject.launcher.model.LauncherItem;
import com.openwatchproject.launcher.persistence.AppDatabase;
import com.openwatchproject.launcher.R;

import java.util.List;

import io.reactivex.schedulers.Schedulers;

public class LauncherAdapter extends ListAdapter<LauncherItem, LauncherAdapter.ViewHolder> {

    private LauncherItemClickListener clickListener;
    private LauncherItemLongClickListener longClickListener;

    private List<LauncherItem> items;

    private SharedPreferences sharedPrefs;
    private AppDatabase db;

    private static final DiffUtil.ItemCallback<LauncherItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<LauncherItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull LauncherItem oldItem, @NonNull LauncherItem newItem) {
            return oldItem.getPackageName().equals(newItem.getPackageName())
                    && oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull LauncherItem oldItem, @NonNull LauncherItem newItem) {
            return oldItem.getPackageName().equals(newItem.getPackageName())
                    && oldItem.getName().equals(newItem.getName());
        }
    };

    public LauncherAdapter(Context context, LauncherItemClickListener clickListener, LauncherItemLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "openwatch-launcher").build();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LauncherItemBinding binding = LauncherItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding, viewType == 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (sharedPrefs.getBoolean("recentsEnabled", true)) {
            int recentCount = Math.min(sharedPrefs.getInt("recentCount", 2),
                    db.recentDao().getRecentsCount().subscribeOn(Schedulers.io())
                            .blockingGet());

            if (position < recentCount
                    && packageExists(getItem(position).getPackageName())) {
                return 0;
            }
        }

        return 1;
    }

    private boolean packageExists(String packageName) {
        for (LauncherItem item : items) {
            if (item.getPackageName().equals(packageName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void submitList(@Nullable List<LauncherItem> list) {
        this.items = list;
        super.submitList(list);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setLauncherItem(getItem(position));
    }

    public class ViewHolder extends WearableRecyclerView.ViewHolder {

        private ImageView icon;
        private TextView title;

        private LauncherItem launcherItem;

        public ViewHolder(@NonNull LauncherItemBinding binding, boolean isRecent) {
            super(binding.getRoot());
            icon = binding.icon;
            title = binding.title;
            if (isRecent) binding.recent.setVisibility(View.VISIBLE);
            itemView.setOnClickListener(v -> clickListener.onClick(launcherItem));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onLongClick(launcherItem);
                return true;
            });
        }

        public void setLauncherItem(LauncherItem launcherItem) {
            icon.setImageDrawable(launcherItem.getIcon());
            title.setText(launcherItem.getName());
            this.launcherItem = launcherItem;
        }
    }
}
