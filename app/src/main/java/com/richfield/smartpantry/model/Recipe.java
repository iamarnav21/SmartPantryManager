package com.richfield.smartpantry.model;

import java.util.ArrayList;
import java.util.List;

/** A recipe with its required ingredients and simple preparation steps. */
public class Recipe {
    private final long id;
    private final String name;
    private final String steps;
    private final List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe(long id, String name, String steps) {
        this.id = id;
        this.name = name;
        this.steps = steps;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getSteps() { return steps; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }

    public void addIngredient(RecipeIngredient ingredient) {
        ingredients.add(ingredient);
    }
}
