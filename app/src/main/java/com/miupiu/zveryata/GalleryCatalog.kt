package com.miupiu.zveryata

import android.content.Context
import org.json.JSONObject

internal data class AnimalCard(
    val id: String,
    val label: String,
    val asset: String
)

/**
 * Reads a staged runtime catalog independently of content-mode and image count.
 * The full 142-card source catalog is stored at catalog/catalog-v2.json.
 * Replace the preview manifest only after the new photos are approved.
 */
internal fun loadCards(context: Context): List<AnimalCard> {
    val json = context.assets.open("catalog.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    val rows = JSONObject(json).getJSONArray("cards")
    val cards = (0 until rows.length()).map { index ->
        val item = rows.getJSONObject(index)
        AnimalCard(
            id = item.getString("id"),
            label = item.getString("label"),
            asset = item.getString("asset")
        )
    }
    require(cards.isNotEmpty()) { "Animal preview catalog is empty" }
    require(cards.map { it.id }.distinct().size == cards.size) { "Duplicate animal identifiers" }
    require(cards.map { it.asset }.distinct().size == cards.size) { "Duplicate preview images" }
    return cards
}
