package com.richfield.smartpantry.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.richfield.smartpantry.R;
import com.richfield.smartpantry.data.DatabaseHelper;
import com.richfield.smartpantry.logic.RecipeMatcher;
import com.richfield.smartpantry.model.PantryItem;
import com.richfield.smartpantry.model.Recipe;
import com.richfield.smartpantry.model.RecipeIngredient;

import java.util.List;
import java.util.Map;

/**
 * Screen 4: Recipe Detail.
 * Receives a recipe id through an Intent, loads the recipe from SQLite and shows the full
 * ingredient list (marking what the pantry covers) and the preparation method.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);
        setTitle(R.string.title_recipe);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1);
        DatabaseHelper db = new DatabaseHelper(this);
        Recipe recipe = db.getRecipe(recipeId);

        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<PantryItem> pantry = db.getAllPantryItems();
        Map<String, Map<String, Double>> totals = RecipeMatcher.buildPantryTotals(pantry);

        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        TextView tvIngredients = findViewById(R.id.tvDetailIngredients);
        TextView tvSteps = findViewById(R.id.tvDetailSteps);

        tvName.setText(recipe.getName());

        // One line per ingredient, marked as in the pantry or missing
        StringBuilder sb = new StringBuilder();
        int missing = 0;
        for (RecipeIngredient ing : recipe.getIngredients()) {
            boolean have = RecipeMatcher.hasEnough(ing, totals);
            if (!have) missing++;
            sb.append(have ? "[x]  " : "[ ]  ")
                    .append(ing.describeAmount()).append("  ")
                    .append(ing.getName())
                    .append(have ? "" : "   (missing)")
                    .append("\n");
        }
        tvIngredients.setText(sb.toString().trim());
        tvSteps.setText(recipe.getSteps());

        if (missing == 0) {
            tvStatus.setText("You have everything you need!");
            tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            tvStatus.setText("Missing " + missing + " ingredient(s)");
            tvStatus.setTextColor(Color.parseColor("#C62828"));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
