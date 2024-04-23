/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.R;
import com.example.model.Property;

import java.util.ArrayList;

public class PropListAdapter extends BaseAdapter {
    private ArrayList<Property> mListItems;
    private LayoutInflater mLayoutInflater;

    public PropListAdapter(Context context, ArrayList<Property> arrayList) {
        mListItems = arrayList;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mListItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mListItems.get(position);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @SuppressLint({"SetTextI18n", "InflateParams"})
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = mLayoutInflater.inflate(R.layout.list_prop, null);
            holder.itemID = view.findViewById(R.id.item_id);
            holder.itemName = view.findViewById(R.id.item_text_view);
            holder.editButton = view.findViewById(R.id.edit_button);
            holder.deleteButton = view.findViewById(R.id.delete_button);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        Property property = mListItems.get(position);
        if (property != null) {
            if (holder.itemName != null) {
                holder.itemName.setText(property.key+" = "+property.value);
            }
            if (holder.itemID != null) {
                holder.itemID.setText(property._id);
            }
        }
        return view;
    }

    private static class ViewHolder {
        protected TextView itemName;
        protected TextView itemID;
        protected ImageButton editButton;
        protected ImageButton deleteButton;
    }
}