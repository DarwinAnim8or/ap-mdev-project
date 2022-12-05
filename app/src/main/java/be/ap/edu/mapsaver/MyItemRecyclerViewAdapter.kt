package be.ap.edu.mapsaver

import android.content.ContentValues.TAG
import android.content.Context
import android.nfc.Tag
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

import be.ap.edu.mapsaver.placeholder.PlaceholderContent.PlaceholderItem
import be.ap.edu.mapsaver.databinding.FragmentItemBinding

/**
 * [RecyclerView.Adapter] that can display a [PlaceholderItem].
 * TODO: Replace the implementation with code for your data type.
 */
class MyItemRecyclerViewAdapter(
    private val values: List<PlaceholderItem>,
) : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        context = parent.context
        return ViewHolder(
            FragmentItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fm: FragmentManager = (context as AppCompatActivity).supportFragmentManager
        val item = values[position]
        holder.idView.text = item.id
        holder.contentView.text = item.content
        holder.detailView.text = item.details

        holder.itemView.setOnClickListener {
            fm.popBackStackImmediate()
        }

    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content
        val detailView: TextView = binding.detail

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}