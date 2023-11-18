package com.example.note_kotlin

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class NoteAdapter(context: Context, var listener: INoteListener?) :
    BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_note) {
    override fun convert(baseViewHolder: BaseViewHolder, notes: String) {
        Glide.with(context).load(notes).into(baseViewHolder.getView(R.id.img));
        baseViewHolder.getView<View>(R.id.card_view).setOnClickListener {
            if (listener != null) {
                listener!!.onNoteClick(notes)
            }
        }
    }
}