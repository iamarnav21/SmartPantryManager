package com.richfield.smartpantry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.richfield.smartpantry.logic.RecipeMatcher;
import com.richfield.smartpantry.model.PantryItem;
import com.richfield.smartpantry.model.Recipe;
import com.richfield.smartpantry.model.RecipeIngredient;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for the strict-matching rule (run with: Right-click > Run 'RecipeMatcherTest'). */
public class RecipeMatcherTest {

    private Recipe omelette() {
        Recipe r = new Recipe(1, "Omelette", "cook");
        r.addIngredient(new RecipeIngredient("egg", 3, "pcs"));
        r.addIngredient(new RecipeIngredient("tomato", 1, "pcs"));
        r.addIngredient(new RecipeIngredient("butter", 10, "g"));
        r.addIngredient(new RecipeIngredient("salt", 1, "tsp"));
        return r;
    }

    private List<Recipe> recipes() {
        List<Recipe> l = new ArrayList<>();
        l.add(omelette());
        return l;
    }

    private List<PantryItem> fullPantry() {
        List<PantryItem> p = new ArrayList<>();
        p.add(new PantryItem(1, "Eggs", 6, "pcs", ""));
        p.add(new PantryItem(2, "Tomatoes", 2, "pcs", ""));
        p.add(new PantryItem(3, "Butter", 250, "g", ""));
        p.add(new PantryItem(4, "Salt", 1, "tbsp", ""));   // 3 tsp worth
        return p;
    }

    @Test
    public void allIngredientsPresent_recipeIsSuggested() {
        assertEquals(1, RecipeMatcher.strictMatches(fullPantry(), recipes()).size());
    }

    @Test
    public void oneIngredientMissing_recipeIsNotSuggested() {
        List<PantryItem> p = fullPantry();
        p.remove(3);   // remove salt: 3 of 4 ingredients only
        assertEquals(0, RecipeMatcher.strictMatches(p, recipes()).size());
        assertEquals(1, RecipeMatcher.almostThere(p, recipes()).size());
    }

    @Test
    public void notEnoughQuantity_recipeIsNotSuggested() {
        List<PantryItem> p = fullPantry();
        p.set(0, new PantryItem(1, "Eggs", 2, "pcs", ""));   // needs 3
        assertTrue(RecipeMatcher.strictMatches(p, recipes()).isEmpty());
    }

    @Test
    public void singularPluralNamesMatch() {
        assertEquals(RecipeMatcher.normalizeName("tomato"), RecipeMatcher.normalizeName("Tomatoes"));
        assertEquals(RecipeMatcher.normalizeName("egg"), RecipeMatcher.normalizeName(" EGGS "));
        assertEquals(RecipeMatcher.normalizeName("potato"), RecipeMatcher.normalizeName("potatoes"));
    }

    @Test
    public void unitsAreConverted() {
        List<PantryItem> p = fullPantry();
        p.set(2, new PantryItem(3, "butter", 0.5, "kg", ""));   // 500 g
        assertEquals(1, RecipeMatcher.strictMatches(p, recipes()).size());
    }

    @Test
    public void duplicateEntriesAreAddedUp() {
        List<PantryItem> p = fullPantry();
        p.set(0, new PantryItem(1, "egg", 1, "pcs", ""));
        p.add(new PantryItem(9, "eggs", 2, "pcs", ""));   // 1 + 2 = 3 eggs
        assertEquals(1, RecipeMatcher.strictMatches(p, recipes()).size());
    }

    @Test
    public void incompatibleUnitFamilyDoesNotMatch() {
        List<PantryItem> p = fullPantry();
        p.set(0, new PantryItem(1, "egg", 500, "g", ""));   // grams cannot replace pieces
        assertFalse(RecipeMatcher.strictMatches(p, recipes()).size() == 1);
    }

    @Test
    public void emptyPantryMatchesNothing() {
        assertTrue(RecipeMatcher.strictMatches(new ArrayList<PantryItem>(), recipes()).isEmpty());
    }

    @Test
    public void cupsAreConvertedToMillilitres() {
        Recipe r = new Recipe(2, "Porridge", "cook");
        r.addIngredient(new RecipeIngredient("milk", 300, "ml"));
        List<Recipe> list = new ArrayList<>();
        list.add(r);
        List<PantryItem> pantry = new ArrayList<>();
        pantry.add(new PantryItem(1, "Milk", 2, "cup", ""));   // 2 cups = 480 ml
        assertEquals(1, RecipeMatcher.strictMatches(pantry, list).size());
    }
}
