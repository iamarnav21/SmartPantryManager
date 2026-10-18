package com.richfield.smartpantry.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.richfield.smartpantry.R;
import com.richfield.smartpantry.logic.ExpiryUtils;
import com.richfield.smartpantry.model.PantryItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Custom RecyclerView adapter that shows the pantry items.
 * onBindViewHolder() fills one row (item_pantry.xml) with the data of one PantryItem.
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    /** Callbacks to the Activity when the user taps Edit or Delete on a row. */
    public interface OnItemActionListener {
        void onEdit(PantryItem item);
        void onDelete(PantryItem item);
    }

    private final List<PantryItem> items = new ArrayList<>();
    private final OnItemActionListener listener;

    public PantryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    /** Replaces the displayed data and refreshes the list. */
    public void setItems(List<PantryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        final PantryItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvQuantity.setText(formatQuantity(item.getQuantity()) + " " + item.getUnit());

        // Show the expiry date; colour it red when the item expires within 3 days.
        if (item.getExpiry().isEmpty()) {
            holder.tvExpiry.setText("No expiry date");
            holder.tvExpiry.setTextColor(Color.parseColor("#616161"));
        } else {
            boolean soon = ExpiryUtils.isExpiringSoon(item.getExpiry(), new Date(), ExpiryUtils.EXPIRY_WARNING_DAYS);
            holder.tvExpiry.setText("Expires: " + item.getExpiry() + (soon ? "  (soon!)" : ""));
            holder.tvExpiry.setTextColor(Color.parseColor(soon ? "#C62828" : "#616161"));
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Shows 4.0 as "4" and 0.5 as "0.5". */
    static String formatQuantity(double q) {
        return (q == Math.floor(q)) ? String.valueOf((long) q) : String.valueOf(q);
    }

    /** Holds the views of one row so they are looked up only once. */
    static class PantryViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvQuantity;
        final TextView tvExpiry;
        final Button btnEdit;
        final Button btnDelete;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvQuantity = itemView.findViewById(R.id.tvItemQuantity);
            tvExpiry = itemView.findViewById(R.id.tvItemExpiry);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
