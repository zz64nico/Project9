package com.example.note_kotlin

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 文件名：MyItemDecoration
 * 描述：RecycleView 分割线
 */
class MyItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = space
        outRect.bottom = space
        outRect.left = space
        outRect.right = space
    }
}