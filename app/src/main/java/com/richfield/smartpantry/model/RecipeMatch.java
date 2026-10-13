package com.richfield.smartpantry.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of testing one recipe against the pantry:
 * the recipe plus the ingredients the pantry is missing (or short of).
 * A recipe is a STRICT match only when the missing list is empty.
 */
public class RecipeMatch {
    private final Recipe recipe;
    private final List<RecipeIngredient> missing;

    public RecipeMatch(Recipe recipe, List<RecipeIngredient> missing) {
        this.recipe = recipe;
        this.missing = new ArrayList<>(missing);
    }

    public Recipe getRecipe() { return recipe; }
    public List<RecipeIngredient> getMissing() { return missing; }
    public boolean isStrictMatch() { return missing.isEmpty(); }
}
