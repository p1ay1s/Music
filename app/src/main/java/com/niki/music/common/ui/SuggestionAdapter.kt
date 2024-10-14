package com.niki.music.common.ui

import androidx.recyclerview.widget.DiffUtil
import com.niki.music.databinding.LayoutSuggestionBinding
import com.p1ay1s.impl.ui.ViewBindingListAdapter


class SuggestionAdapter(private val callback: (String) -> Unit) :
    ViewBindingListAdapter<LayoutSuggestionBinding, String, String>(SuggestionCallback()) {
    class SuggestionCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

    override fun LayoutSuggestionBinding.onBindViewHolder(data: String, position: Int) {
        suggestion.text = data
        root.setOnClickListener { callback(data) }
    }
}