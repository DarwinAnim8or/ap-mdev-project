package be.ap.edu.mapsaver

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

import be.ap.edu.mapsaver.databinding.FragmentItemBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import java.text.DecimalFormat
import java.util.*
import kotlin.concurrent.thread

class MyItemRecyclerViewAdapter(
    private val values: List<Toilet>,
) : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    private var context: Context? = null
    private lateinit var roadManager: RoadManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        context = parent.context
        roadManager = OSRMRoadManager(context, BuildConfig.APPLICATION_ID)
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        MainActivity.geocoder = Geocoder(parent.context, Locale.getDefault())

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
        val distance = getDistance(item.geoLat, item.geoLong)
        holder.afstandView.text = "${DecimalFormat("#.##").format(distance)} Km"

        holder.itemView.setOnClickListener {
            getRoute(item.geoLat, item.geoLong)
            fm.popBackStackImmediate()
            MainActivity.listOpen = false
        }

    }

    fun getDistance(lat: Double, long: Double): Double {
        return getRoad(lat, long).mLength
    }

    fun getRoute(lat: Double, long: Double) {
        var overlay = RoadManager.buildRoadOverlay(getRoad(lat, long))
        MainActivity.mMapView?.overlays?.removeFirstOrNull()
        MainActivity.mMapView?.overlays?.add(overlay)
        MainActivity.mMapView?.invalidate()
    }

    fun getRoad(lat: Double, long: Double): Road {
        var output = Road()
        var thread = thread {
            if(MainActivity.mUserLocation != null) {
                try {
                    output = roadManager.getRoad(arrayListOf(MainActivity.mUserLocation, GeoPoint(lat, long)))
                } catch (e : java.lang.NullPointerException) {
                    Log.e(TAG, "$e")
                }
            }
        }
        if(thread.state == Thread.State.NEW) thread.start()
        thread.join()
        return output
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