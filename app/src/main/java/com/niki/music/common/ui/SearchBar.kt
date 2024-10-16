package com.niki.music.common.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.niki.music.databinding.LayoutSearchBarBinding
import com.p1ay1s.base.extension.addLineDecoration
import com.p1ay1s.base.ui.PreloadLayoutManager

interface SearchBarListener {
    fun onContentChanged(keywords: String)
    fun onSubmit(keywords: String)
}

class SearchBar(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var binding: LayoutSearchBarBinding =
        LayoutSearchBarBinding.inflate(LayoutInflater.from(context), this, true)

    private var defaultList: List<String> = emptyList()
    private var shouldShowLiveData = MutableLiveData(true)

    var listener: SearchBarListener? = null
    private var enableSet = true

    private lateinit var suggestionAdapter: SuggestionAdapter

    private var preloadLayoutManager = PreloadLayoutManager(
        context,
        LinearLayoutManager.VERTICAL,
        3
    )

    fun init() {
        binding.run {
            searchEdit.editText?.run {
                addTextChangedListener(TextWatcherImpl())

                /**
                 * 搜索事件: 隐藏建议 & 回调 on submit 事件
                 *
                 * 要想不输入回车要在 edit text 设置 single line 属性
                 */
                setOnEditorActionListener { textView, actionId, event ->
                    textView.text?.let {
                        listener?.onSubmit(it.toString())
                    }
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        shouldShowLiveData.value = false
                    }
                    true
                }

                suggestionAdapter = SuggestionAdapter {
                    this.setText(it)
                    this.onEditorAction(EditorInfo.IME_ACTION_SEARCH) // 点击 item 时触发搜索事件
                }
            }

            shouldShowLiveData.observeForever { should ->
                recyclerView.visibility = if (should) View.VISIBLE else View.GONE
            }


            with(recyclerView) {
                adapter = suggestionAdapter
                layoutManager = preloadLayoutManager
                addLineDecoration(context, RecyclerView.VERTICAL)
            }
        }
    }

    fun showDefaultList(newList: List<String>? = null) {
        newList?.let { defaultList = it }
        setSuggestions(defaultList)
    }

    fun setSuggestions(list: List<String>?) {
        if (enableSet)
            suggestionAdapter.submitList(list)
    }

    inner class TextWatcherImpl : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val keywords = s?.toString()
            shouldShowLiveData.value = true
            enableSet = true
            if (!keywords.isNullOrBlank()) {
                listener?.onContentChanged(keywords)
            } else {
                showDefaultList()
                enableSet = false
            }
        }

        override fun afterTextChanged(s: Editable?) {
        }
    }
}