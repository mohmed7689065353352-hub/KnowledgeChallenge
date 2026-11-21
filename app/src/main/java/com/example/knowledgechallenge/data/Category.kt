package com.example.knowledgechallenge.data

import androidx.annotation.StringRes
import com.example.knowledgechallenge.R

enum class Category(val id: String, @StringRes val title: Int, val emoji: String) {
    GENERAL("general", R.string.cat_general, "🌍"),
    SCIENCE("science", R.string.cat_science, "🔬"),
    LOGIC("logic", R.string.cat_logic, "🧩"),
    QUICK("quick", R.string.cat_quick, "⚡");

    companion object {
        fun fromId(id: String?): Category? = entries.firstOrNull { it.id == id }
    }
}
