package com.test.ssmc.hiscream.Views.greendao;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.test.ssmc.hiscream.R;

import java.util.List;
import java.util.Locale;

/**
 * Created by Crazypudding on 2017/1/3.
 */

public class ListAdapter extends ArrayAdapter<HistoryRecord> {
    public final static String TAG = "ListAdapter";
    private List<HistoryRecord> mData;
    private int resourceId;

    //Set up ViewHolder
    private static class ViewHolder {
        ImageView timeImageView;
        TextView nameTextView;
        ImageView deleteImageView;
    }

    public ListAdapter(Context context, int resource, List<HistoryRecord> objects) {
        super(context, resource, objects);
        resourceId = resource;
        mData = objects;
        Log.d(TAG, "ListAdapter: " + mData.size());
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, null);
            holder = new ViewHolder();
            holder.timeImageView = (ImageView)convertView.findViewById(R.id.time_iv);
            holder.nameTextView = (TextView) convertView.findViewById(R.id.record_tv);
            holder.deleteImageView = (ImageView)convertView.findViewById(R.id.delete_iv);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        HistoryRecord historyRecord = mData.get(position);
        holder.timeImageView.setImageResource(R.drawable.time);
        holder.nameTextView.setText(" " +historyRecord.getName());
        holder.deleteImageView.setImageResource(R.drawable.delete);
        holder.deleteImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemDeleteListener.onDeleteClick(position);
            }
        });
        return convertView;
    }

    /**
     * 删除按钮的监听接口
     */
    public interface onItemDeleteListener {
        void onDeleteClick(int i);
    }
    private onItemDeleteListener mOnItemDeleteListener;
    public void setOnItemDeleteClickListener(onItemDeleteListener mOnItemDeleteListener) {
        this.mOnItemDeleteListener = mOnItemDeleteListener;
    }
}
