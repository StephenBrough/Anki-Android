package com.ichi2.anki.junkdrawer.widgets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.ichi2.anki.R

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList


class DeckDropDownAdapter(private val context: Context, private val decks: ArrayList<JSONObject>) : BaseAdapter() {

    interface SubtitleListener {
        val subtitleText: String
    }

    internal class DeckDropDownViewHolder {
        var deckNameView: TextView? = null
        var deckCountsView: TextView? = null
    }


    override fun getCount(): Int = decks.size + 1


    override fun getItem(position: Int): Any? {
        return if (position == 0) {
            null
        } else {
            decks[position + 1]
        }
    }


    override fun getItemId(position: Int): Long = position.toLong()


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val viewHolder: DeckDropDownViewHolder
        val deckNameView: TextView?
        val deckCountsView: TextView?
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.dropdown_deck_selected_item, parent, false)
            deckNameView = convertView!!.findViewById<View>(R.id.dropdown_deck_name) as TextView
            deckCountsView = convertView.findViewById<View>(R.id.dropdown_deck_counts) as TextView
            viewHolder = DeckDropDownViewHolder()
            viewHolder.deckNameView = deckNameView
            viewHolder.deckCountsView = deckCountsView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as DeckDropDownViewHolder
            deckNameView = viewHolder.deckNameView
            deckCountsView = viewHolder.deckCountsView
        }
        if (position == 0) {
            deckNameView!!.text = context.resources.getString(R.string.deck_summary_all_decks)
        } else {
            val deck = decks[position - 1]
            try {
                val deckName = deck.getString("name")
                deckNameView!!.text = deckName
            } catch (ex: JSONException) {
                RuntimeException()
            }

        }
        deckCountsView!!.text = (context as SubtitleListener).subtitleText
        return convertView
    }


    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val deckNameView: TextView
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.dropdown_deck_item, parent, false)
            deckNameView = convertView!!.findViewById<View>(R.id.dropdown_deck_name) as TextView
            convertView.tag = deckNameView
        } else {
            deckNameView = convertView.tag as TextView
        }
        if (position == 0) {
            deckNameView.text = context.resources.getString(R.string.deck_summary_all_decks)
        } else {
            val deck = decks[position - 1]
            try {
                val deckName = deck.getString("name")
                deckNameView.text = deckName
            } catch (ex: JSONException) {
                RuntimeException()
            }

        }
        return convertView
    }
}