package com.richfield.smartpantry.model;

/** One ingredient line required by a recipe (name, quantity and unit). */
public class RecipeIngredient {
    private final String name;
    private final double quantity;
    private final String unit;

    public RecipeIngredient(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }

    /** Human readable quantity, e.g. "200 g" or "0.5 tsp". */
    public String describeAmount() {
        String q = (quantity == Math.floor(quantity))
                ? String.valueOf((long) quantity)
                : String.valueOf(quantity);
        return q + " " + unit;
    }
}
