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
import net.minecraft.resource.featuretoggle.FeatureSet
import net.minecraft.world.World
import java.lang.reflect.InvocationTargetException

internal fun craftlessRecipeRecord(
    entry: Any,
    craftable: Boolean,
    world: World?,
): JsonObject =
    craftlessRecipeRecord(
        recipe = entry.toCraftlessRecipeProjection(world),
        craftable = craftable,
    )

internal fun Any.craftlessOutputItems(world: World?): List<CraftlessRecipeItem> =
    craftlessDisplayObject()
        .toCraftlessRecipeOutputDisplay()
        ?.toCraftlessRecipeItems(world)
        ?: emptyList()

internal fun Any.craftlessRecipeHandleKey(): String =
    craftlessIdObject()
        .craftlessNetworkRecipeIndex()
        ?.toString()
        .orEmpty()

internal fun Any.craftlessIdObject(): Any = recordValue(0) ?: this

internal fun Any.craftlessDisplayObject(): Any = recordValue(1) ?: this

internal fun Any.craftlessIsEnabled(features: FeatureSet?): Boolean =
    features == null ||
        javaClass.methods
            .firstOrNull { method ->
                method.parameterCount == 1 &&
                    method.returnType == Boolean::class.javaPrimitiveType &&
                    method.parameterTypes[0].isInstance(features)
            }?.let { method ->
                try {
                    method.invoke(this, features) as? Boolean
                } catch (_: IllegalAccessException) {
                    null
                } catch (_: InvocationTargetException) {
                    null
                }
            } ?: true

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
        put("handle", "recipe.handle:${recipe.handle}")
        put("kind", recipe.kind)
        put("craftable", craftable)
        put("outputs", outputs)
        put("ingredients", ingredients)
        put("produces", outputs)
        put("requires", ingredients)
        if (!craftable) {
            put("reason", "recipe-not-craftable")
        }
        recipe.station?.let { station ->
            put("station", station.toCraftlessRecipeItem())
        }
    }
}

internal data class CraftlessRecipeProjection(
    val handle: String,
    val kind: String,
    val outputs: List<CraftlessRecipeItem>,
    val ingredients: List<CraftlessRecipeItem> = emptyList(),
    val station: CraftlessRecipeItem? = null,
) {
    constructor(
        handleIndex: Int,
        kind: String,
        outputs: List<CraftlessRecipeItem>,
        ingredients: List<CraftlessRecipeItem> = emptyList(),
        station: CraftlessRecipeItem? = null,
    ) : this(
        handle = handleIndex.toString(),
        kind = kind,
        outputs = outputs,
        ingredients = ingredients,
        station = station,
    )
}

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

private fun Any.toCraftlessRecipeProjection(world: World?): CraftlessRecipeProjection {
    val display = craftlessDisplayObject()
    return CraftlessRecipeProjection(
        handle = craftlessRecipeHandleKey(),
        kind = display.toCraftlessRecipeKind(),
        outputs = display.toCraftlessRecipeOutputDisplay()?.toCraftlessRecipeItems(world).orEmpty(),
        ingredients =
            display
                .toCraftlessIngredientDisplays()
                .flatMap { ingredient -> ingredient.toCraftlessRecipeItems(world) },
        station = display.toCraftlessCraftingStationDisplay()?.toCraftlessRecipeItems(world)?.firstOrNull(),
    )
}

private fun Any.toCraftlessRecipeKind(): String =
    when (javaClass.recordComponents?.size) {
        5 -> "shaped-crafting"
        3 -> "shapeless-crafting"
        6 -> "furnace"
        else -> "recipe"
    }

private fun Any.toCraftlessIngredientDisplays(): List<Any> =
    when (javaClass.recordComponents?.size) {
        5 -> (recordValue(2).asIterable()?.toList() ?: listOfNotNull(recordValue(0), recordValue(1), recordValue(2)))
        3 -> recordValue(0).asIterable()?.toList() ?: listOfNotNull(recordValue(0))
        6 -> listOfNotNull(recordValue(0), recordValue(1))
        else -> emptyList()
    }

private fun Any.toCraftlessRecipeOutputDisplay(): Any? =
    when (javaClass.recordComponents?.size) {
        5 -> recordValue(3)
        3 -> recordValue(1)
        6 -> recordValue(2)
        else -> null
    }

private fun Any.toCraftlessCraftingStationDisplay(): Any? =
    when (javaClass.recordComponents?.size) {
        5 -> recordValue(4)
        3 -> recordValue(2)
        6 -> recordValue(3)
        else -> null
    }

private fun Any.toCraftlessRecipeItems(
    world: World? = null,
    depth: Int = 0,
): List<CraftlessRecipeItem> {
    if (depth > 5) {
        return emptyList()
    }
    if (this is ItemStack) {
        return listOf(toCraftlessRecipeItem()).filterNot { item -> item.label.isBlank() }
    }
    noArgItemStackResults()
        .firstOrNull()
        ?.let { stack -> return listOf(stack.toCraftlessRecipeItem()).filterNot { item -> item.label.isBlank() } }
    return javaClass
        .recordComponents
        .orEmpty()
        .mapNotNull { component ->
            try {
                component.accessor.invoke(this)
            } catch (_: IllegalAccessException) {
                null
            } catch (_: InvocationTargetException) {
                null
            }
        }.flatMap { value ->
            value.asIterable()?.flatMap { element -> element.toCraftlessRecipeItems(world, depth + 1) }
                ?: value.toCraftlessRecipeItems(world, depth + 1)
        }.filterNot { item -> item.label.isBlank() }
}

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

internal fun Any?.asIterable(): Iterable<Any>? =
    when (this) {
        is Iterable<*> -> filterNotNull()
        else -> null
    }

internal fun Any.recordValue(index: Int): Any? =
    try {
        javaClass.recordComponents
            ?.getOrNull(index)
            ?.accessor
            ?.invoke(this)
    } catch (_: IllegalAccessException) {
        null
    } catch (_: InvocationTargetException) {
        null
    }

internal fun Any?.craftlessNetworkRecipeIndex(): Int? =
    this
        ?.javaClass
        ?.recordComponents
        ?.singleOrNull { component -> component.type == Int::class.javaPrimitiveType }
        ?.let { component ->
            try {
                component.accessor.invoke(this) as? Int
            } catch (_: IllegalAccessException) {
                null
            } catch (_: InvocationTargetException) {
                null
            }
        }

private fun Any.noArgItemStackResults(): List<ItemStack> =
    javaClass.methods
        .asSequence()
        .filter { method -> method.parameterCount == 0 && method.returnType == ItemStack::class.java }
        .mapNotNull { method ->
            try {
                method.invoke(this) as? ItemStack
            } catch (_: IllegalAccessException) {
                null
            } catch (_: InvocationTargetException) {
                null
            }
        }.toList()

internal fun Any.invokeNoArg(name: String): Any? =
    try {
        javaClass.methods
            .firstOrNull { method -> method.name == name && method.parameterCount == 0 }
            ?.invoke(this)
    } catch (_: IllegalAccessException) {
        null
    } catch (_: InvocationTargetException) {
        null
    } catch (_: SecurityException) {
        null
    }

internal fun Any.invokeBoolean(
    name: String,
    argument: Any,
): Boolean? =
    try {
        javaClass.methods
            .firstOrNull { method -> method.name == name && method.parameterCount == 1 }
            ?.invoke(this, argument) as? Boolean
    } catch (_: IllegalAccessException) {
        null
    } catch (_: InvocationTargetException) {
        null
    } catch (_: SecurityException) {
        null
    }

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
            key.contains("mutton") -> "food"
        else -> "item"
    }
}

private fun String.toCraftlessTitle(): String =
    split('_', '-', '.')
        .filter { part -> part.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { char -> char.uppercase() } }
