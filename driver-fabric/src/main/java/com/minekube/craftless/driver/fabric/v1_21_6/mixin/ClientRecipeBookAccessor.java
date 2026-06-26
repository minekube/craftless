package com.minekube.craftless.driver.fabric.v1_21_6.mixin;

import java.util.Map;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientRecipeBook.class)
public interface ClientRecipeBookAccessor {
    @Accessor("recipes")
    Map<NetworkRecipeId, RecipeDisplayEntry> craftless$getRecipes();
}
