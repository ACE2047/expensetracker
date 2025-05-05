package com.example.expensetracker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryLegendAdapter extends RecyclerView.Adapter<CategoryLegendAdapter.ViewHolder> {

    private List<CategoryLegendItem> items;

    public static class CategoryLegendItem {
        String name;
        int color;

        public CategoryLegendItem(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }

    public CategoryLegendAdapter(List<CategoryLegendItem> items) {
        this.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_legend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CategoryLegendItem item = items.get(position);
        holder.colorView.setBackgroundColor(item.color);
        holder.nameText.setText(item.name); // This now includes the percentage
        holder.nameText.setTextColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorView;
        TextView nameText;

        ViewHolder(View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.categoryColor);
            nameText = itemView.findViewById(R.id.categoryName);
        }
    }
}