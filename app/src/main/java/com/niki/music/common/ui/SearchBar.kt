package com.niki.music.common.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.niki.music.databinding.LayoutSearchBarBinding
import com.p1ay1s.base.log.logE

interface SearchBarListener {
    fun onContentChanged(keywords: String)
    fun onSubmit(keywords: String)
}

class SearchBar(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var binding: LayoutSearchBarBinding =
        LayoutSearchBarBinding.inflate(LayoutInflater.from(context), this, true)

    private var defaultList: List<String> = emptyList()

    var listener: SearchBarListener? = null

    private lateinit var adapter: SuggestionAdapter
    private var manager = LinearLayoutManager(context)

    fun init() {
        binding.run {
            searchEdit.editText?.addTextChangedListener(EditListener())
            searchEdit.editText?.setOnEditorActionListener { textView, actionId, _ ->
                textView.text?.let {
                    listener?.onSubmit(it.toString())
                }
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideList()
                }
                false
            }
            adapter = SuggestionAdapter {
                searchEdit.editText?.setText(it)
            }
            with(recyclerView) {
                adapter = adapter
                manager.orientation = LinearLayoutManager.VERTICAL
                layoutManager = manager
            }
        }
    }

    private fun hideList() {
//        binding.recyclerView.visibility = View.GONE
    }

    private fun showList() {
//        binding.recyclerView.visibility = View.VISIBLE
    }

    fun showDefaultList(newList: List<String>? = null) {
        newList?.let { defaultList = it }
        setSuggestions(defaultList)
    }

    fun setSuggestions(list: List<String>?) {
        list?.let { logE("####", it.toString()) }
        showList()
        adapter.submitList(list)
    }

    inner class EditListener : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val keywords = s?.toString()
            if (keywords != null) {
                listener?.onContentChanged(keywords)
            } else {
                showDefaultList()
            }
        }

        override fun afterTextChanged(s: Editable?) {
        }
    }
}