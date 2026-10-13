package com.richfield.smartpantry.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.richfield.smartpantry.R;
import com.richfield.smartpantry.model.RecipeIngredient;
import com.richfield.smartpantry.model.RecipeMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView adapter for the recipe lists.
 * Used for both the strict "Ready to cook" list and the bonus "Almost there" list.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(RecipeMatch match);
    }

    private final List<RecipeMatch> matches = new ArrayList<>();
    private final OnRecipeClickListener listener;

    public RecipeAdapter(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setMatches(List<RecipeMatch> newMatches) {
        matches.clear();
        matches.addAll(newMatches);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        final RecipeMatch match = matches.get(position);
        int count = match.getRecipe().getIngredients().size();

        holder.tvName.setText(match.getRecipe().getName());

        if (match.isStrictMatch()) {
            holder.tvInfo.setText("You have all " + count + " ingredients");
        } else {
            // "Almost there" row: say exactly what is missing
            RecipeIngredient missing = match.getMissing().get(0);
            holder.tvInfo.setText("Missing: " + missing.getName()
                    + " (" + missing.describeAmount() + ")");
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(match));
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvInfo;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvInfo = itemView.findViewById(R.id.tvRecipeInfo);
        }
    }
}
