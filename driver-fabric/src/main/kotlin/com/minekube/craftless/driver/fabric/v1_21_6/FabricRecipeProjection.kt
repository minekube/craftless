package com.minekube.craftless.driver.fabric.v1_21_6

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.minecraft.item.ItemStack
import net.minecraft.recipe.RecipeDisplayEntry
import net.minecraft.recipe.display.FurnaceRecipeDisplay
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay
import net.minecraft.recipe.display.SlotDisplay

internal fun craftlessRecipeRecord(
    entry: RecipeDisplayEntry,
    craftable: Boolean,
): JsonObject {
    val display = entry.display()
    return craftlessRecipeRecord(
        CraftlessRecipeProjection(
            handleIndex = entry.id().index(),
            kind = display.toCraftlessRecipeKind(),
            outputs = display.result().toCraftlessRecipeItems(),
            ingredients =
                display
                    .toCraftlessIngredientDisplays()
                    .flatMap { ingredient -> ingredient.toCraftlessRecipeItems() },
            station = display.craftingStation().toCraftlessRecipeItems().firstOrNull(),
        ),
        craftable = craftable,
    )
}

internal fun RecipeDisplayEntry.craftlessOutputItems(): List<CraftlessRecipeItem> = display().result().toCraftlessRecipeItems()

internal fun craftlessRecipeRecord(
    recipe: CraftlessRecipeProjection,
    craftable: Boolean,
): JsonObject {
    val outputs =
        buildJsonArray {
            recipe.outputs.forEach { output -> add(output.toCraftlessRecipeItem()) }
        }
    val ingredients =
        buildJsonArray {
            recipe.ingredients.forEach { ingredient -> add(ingredient.toCraftlessRecipeItem()) }
        }
    return buildJsonObject {
        put("handle", "recipe.handle:${recipe.handleIndex}")
        put("kind", recipe.kind)
        put("craftable", craftable)
        put("outputs", outputs)
        put("ingredients", ingredients)
        put("produces", outputs)
        put("requires", ingredients)
        recipe.station?.let { station ->
            put("station", station.toCraftlessRecipeItem())
        }
    }
}

internal data class CraftlessRecipeProjection(
    val handleIndex: Int,
    val kind: String,
    val outputs: List<CraftlessRecipeItem>,
    val ingredients: List<CraftlessRecipeItem> = emptyList(),
    val station: CraftlessRecipeItem? = null,
)

internal fun craftlessRecipeItem(
    rawName: String,
    translationKey: String,
    count: Int = 1,
): CraftlessRecipeItem =
    CraftlessRecipeItem(
        label = translationKey.toCraftlessItemLabel(rawName),
        count = count.coerceAtLeast(1),
        category = translationKey.toCraftlessItemCategory(),
    )

internal fun JsonObject.matchesCraftlessRecipeQuery(
    category: String?,
    output: String?,
    craftable: Boolean?,
): Boolean {
    if (craftable != null && this["craftable"]?.jsonPrimitive?.content != craftable.toString()) {
        return false
    }
    val outputs = this["outputs"]?.jsonArray.orEmpty().map { element -> element.jsonObject }
    if (category != null && outputs.none { recipeOutput -> recipeOutput["category"]?.jsonPrimitive?.content == category }) {
        return false
    }
    if (output != null) {
        val normalizedOutput = output.lowercase()
        if (outputs.none { recipeOutput ->
                recipeOutput["label"]
                    ?.jsonPrimitive
                    ?.content
                    ?.lowercase()
                    ?.contains(normalizedOutput) == true
            }
        ) {
            return false
        }
    }
    return true
}

private fun net.minecraft.recipe.display.RecipeDisplay.toCraftlessRecipeKind(): String =
    when (this) {
        is ShapedCraftingRecipeDisplay -> "shaped-crafting"
        is ShapelessCraftingRecipeDisplay -> "shapeless-crafting"
        is FurnaceRecipeDisplay -> "furnace"
        else -> "recipe"
    }

private fun net.minecraft.recipe.display.RecipeDisplay.toCraftlessIngredientDisplays(): List<SlotDisplay> =
    when (this) {
        is ShapedCraftingRecipeDisplay -> ingredients()
        is ShapelessCraftingRecipeDisplay -> ingredients()
        is FurnaceRecipeDisplay -> listOf(ingredient(), fuel())
        else -> emptyList()
    }

private fun SlotDisplay.toCraftlessRecipeItems(): List<CraftlessRecipeItem> =
    when (this) {
        is SlotDisplay.StackSlotDisplay ->
            listOf(stack().toCraftlessRecipeItem())
        is SlotDisplay.ItemSlotDisplay ->
            listOf(item().value().defaultStack.toCraftlessRecipeItem())
        is SlotDisplay.CompositeSlotDisplay ->
            contents().flatMap { content -> content.toCraftlessRecipeItems() }
        is SlotDisplay.WithRemainderSlotDisplay ->
            input().toCraftlessRecipeItems()
        else -> emptyList()
    }.filterNot { item -> item.label.isBlank() }

internal data class CraftlessRecipeItem(
    val label: String,
    val count: Int,
    val category: String,
) {
    fun toCraftlessRecipeItem(): JsonObject =
        buildJsonObject {
            put("label", label)
            put("count", count)
            put("category", category)
        }
}

internal fun ItemStack.toCraftlessRecipeItem(): CraftlessRecipeItem =
    craftlessRecipeItem(
        rawName = name.string,
        translationKey = item.translationKey,
        count = count,
    )

private fun String.toCraftlessItemLabel(rawName: String): String {
    val fallback = substringAfterLast('.').toCraftlessTitle()
    return rawName
        .takeUnless { name -> name.contains('.') || name.contains('_') }
        ?.takeIf { name -> name.isNotBlank() }
        ?: fallback
}

private fun String.toCraftlessItemCategory(): String {
    val key = substringAfterLast('.')
    return when {
        key.contains("sword") || key.contains("bow") || key.contains("trident") || key.contains("mace") -> "weapon"
        key.contains("pickaxe") || key.contains("shovel") || key.contains("axe") || key.contains("hoe") -> "tool"
        key.contains("helmet") || key.contains("chestplate") || key.contains("leggings") || key.contains("boots") -> "armor"
        key.contains("planks") ||
            key.contains("log") ||
            key.contains("stick") ||
            key.contains("ingot") ||
            key.contains("gem") -> "material"
        key.contains("bread") ||
            key.contains("apple") ||
            key.contains("carrot") ||
            key.contains("potato") ||
            key.contains("beef") ||
            key.contains("pork") ||
            key.contains("chicken") ||
            key.contains("mutton") ||
            key.contains("cod") ||
            key.contains("salmon") ||
            key.contains("stew") ||
            key.contains("soup") -> "food"
        else -> "item"
    }
}

private fun String.toCraftlessTitle(): String =
    split('_', '-', ' ')
        .filter { part -> part.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
