package be.ap.edu.mapsaver

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
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
    private val values: List<Toilet>,
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
        holder.omschrijvingView.text = item.omschrijving
        holder.openingsurenView.text = "${if(item.openingsuren == "null") "24 uur / 24 uur" else "${item.openingsuren}"}"
        holder.betalendView.text = "Betalend: ${if(item.betalend == "null") "nee" else "${item.betalend}"}"
        holder.afstandView.text = ""

        holder.itemView.setOnClickListener {
            fm.popBackStackImmediate()
        }

    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val omschrijvingView: TextView = binding.omschrijving
        val openingsurenView: TextView = binding.openingsuren
        val betalendView: TextView = binding.betalend
        val afstandView: TextView = binding.afstand

        override fun toString(): String {
            return super.toString() + " '" + openingsurenView.text + "'"
        }
    }

}