package com.example.recipemng;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RecipeItem {
    public String name;
    public String ingridients;
    public String imageUrl;
    public String instructions;

    public RecipeItem() {
    }

    public RecipeItem(String name, String ingridients, String imageUrl, String instructions) {
        this.name = name;
        this.ingridients = ingridients;
        this.imageUrl = imageUrl;
        this.instructions = instructions;
    }
}