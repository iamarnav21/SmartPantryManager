package com.richfield.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.richfield.smartpantry.R;
import com.richfield.smartpantry.data.DatabaseHelper;
import com.richfield.smartpantry.logic.RecipeMatcher;
import com.richfield.smartpantry.model.PantryItem;
import com.richfield.smartpantry.model.Recipe;
import com.richfield.smartpantry.model.RecipeMatch;

import java.util.List;

/**
 * Screen 3: Suggested Recipes.
 * Runs the STRICT matching rule against the current pantry and lists only the recipes
 * the user can cook right now. A separate bonus tab, "Almost there", lists recipes that
 * are missing exactly one ingredient; it is never mixed into the strict list.
 */
public class SuggestedRecipesActivity extends AppCompatActivity
        implements RecipeAdapter.OnRecipeClickListener {

    private DatabaseHelper db;
    private RecipeAdapter adapter;
    private TextView tvEmpty;
    private TextView tvHeading;
    private Button btnReady;
    private Button btnAlmost;
    private boolean showingAlmost = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);
        setTitle(R.string.title_suggested);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = new DatabaseHelper(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvHeading = findViewById(R.id.tvListHeading);
        btnReady = findViewById(R.id.btnReady);
        btnAlmost = findViewById(R.id.btnAlmost);

        RecyclerView recycler = findViewById(R.id.recyclerRecipes);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(this);
        recycler.setAdapter(adapter);

        btnReady.setOnClickListener(v -> { showingAlmost = false; refresh(); });
        btnAlmost.setOnClickListener(v -> { showingAlmost = true; refresh(); });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();   // re-run the matching every time the screen is shown
    }

    /** Reads the pantry and recipes from SQLite, runs the matcher and updates the list. */
    private void refresh() {
        List<PantryItem> pantry = db.getAllPantryItems();
        List<Recipe> recipes = db.getAllRecipes();

        List<RecipeMatch> shown;
        if (showingAlmost) {
            shown = RecipeMatcher.almostThere(pantry, recipes);
            tvHeading.setText("Bonus list - recipes missing exactly ONE ingredient (not ready to cook)");
            tvEmpty.setText(R.string.empty_almost);
        } else {
            shown = RecipeMatcher.strictMatches(pantry, recipes);
            tvHeading.setText("Recipes you can make right now with what you already have");
            tvEmpty.setText(R.string.empty_suggestions);
        }

        adapter.setMatches(shown);
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);

        // Visually mark which list is active
        btnReady.setEnabled(showingAlmost);
        btnAlmost.setEnabled(!showingAlmost);
    }

    @Override
    public void onRecipeClick(RecipeMatch match) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, match.getRecipe().getId());
        startActivity(intent);
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
