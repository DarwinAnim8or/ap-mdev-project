package be.ap.edu.mapsaver

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ItemFragment : Fragment() {

    private var columnCount = 1
    val db = Firebase.firestore
    lateinit var toilets : ArrayList<Toilet>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }

                toilets = arrayListOf()
                EventChangeListener { item: Int -> adapter?.notifyItemChanged(item) }

                adapter = MyItemRecyclerViewAdapter(toilets)
            }
        }
        return view
    }

    private fun EventChangeListener(callback: (Int) -> Unit) {
        db.collection("toilets").get().addOnSuccessListener { result ->
            for (document in result.documentChanges) {
                toilets.add(document.document.toObject(Toilet::class.java))
                callback(document.newIndex)
            }
        }

    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            ItemFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}