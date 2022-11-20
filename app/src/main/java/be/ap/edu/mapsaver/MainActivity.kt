package be.ap.edu.mapsaver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.beust.klaxon.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import okhttp3.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import java.io.File
import java.net.*
import java.util.*


class MainActivity : Activity() {

    val db = Firebase.firestore
    
    //arraylist of toilet objects for the map / init file
    var toilets = ArrayList<Toilet>()

    private var mMapView: MapView? = null
    private var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    private var items = ArrayList<OverlayItem>()
    private var searchField: EditText? = null
    private var searchButton: Button? = null
    private var clearButton: Button? = null
    private val urlNominatim = "https://nominatim.openstreetmap.org/"

    private var geocoder: Geocoder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        geocoder = Geocoder(this, Locale.getDefault())
        super.onCreate(savedInstanceState)

        //Don't forget to actually init firebase:
        FirebaseApp.initializeApp(this)

        //create geocoder object 


        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        // Problem with SQLite db, solution :
        // https://stackoverflow.com/questions/40100080/osmdroid-maps-not-loading-on-my-device
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        setContentView(R.layout.activity_main)
        mMapView = findViewById<MapView>(R.id.mapview)

        searchField = findViewById(R.id.search_txtview)
        searchButton = findViewById(R.id.search_button)
        searchButton?.setOnClickListener {
            try {
                //val geocoder = Geocoder(this, Locale.getDefault())
                var geoResults: MutableList<Address>? = geocoder!!.getFromLocationName(searchField!!.text.toString(), 1)
                if (geoResults?.isNotEmpty() == true) {
                    val addr = geoResults?.get(0)
                    val location = addr?.let { it1 -> GeoPoint(it1.latitude, addr.longitude) }

                    if (location != null) {
                        moveMap(location)
                    }
                }else{
                    Toast.makeText(this,"Location Not Found",Toast.LENGTH_LONG)
                }
            } catch (e: java.lang.Exception) {
                print(e.message)
            }
        }

        clearButton = findViewById(R.id.clear_button)
        clearButton?.setOnClickListener {
            mMapView?.overlays?.clear()
            // Redraw map
            mMapView?.invalidate()

            // Clear search field
            searchField?.setText("")

            //Add toilets

        }

        if (hasPermissions()) {
            initMap()
        }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        //pull toilets:
        MapInit().Initialize()
    }

    private fun moveMap(location: GeoPoint) {
        setCenter(location, "test")
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (hasPermissions()) {
                initMap()
            } else {
                finish()
            }
        }
    }

    fun initMap() {
        mMapView?.setTileSource(TileSourceFactory.MAPNIK)
        // create a static ItemizedOverlay showing some markers
        addMarker(GeoPoint(51.2162764, 4.41160291036386), "Campus Meistraat")
        addMarker(GeoPoint(51.2196911, 4.4092625), "Campus Lange Nieuwstraat")
        // add receiver to get location from tap


         // MiniMap
        //val miniMapOverlay = MinimapOverlay(this, mMapView!!.tileRequestCompleteHandler)
        //this.mMapView?.overlays?.add(miniMapOverlay)

        mMapView?.controller?.setZoom(17.0)
        // default = Ellermanstraat 33
        setCenter(GeoPoint(51.23020595, 4.41655480828479), "Campus Ellermanstraat")
    }

    fun addToilet(toilet: Toilet) {
        Thread {
            while (geocoder == null) {
                Thread.sleep(100)
            }

            toilets.add(toilet)

            try {
                if (geocoder == null) geocoder = Geocoder(this)
                var geoResults: MutableList<Address>? = geocoder!!.getFromLocationName(toilet.straat, 1)
                if (geoResults?.isNotEmpty() == true) {
                    val addr = geoResults?.get(0)
                    val location = addr?.let { it1 -> GeoPoint(it1.latitude, addr.longitude) }

                    if (location != null) {
                        addMarker(location, "de poot")
                    }
                }
            } catch (e: java.lang.Exception) {
                //print to console if error
                print(e.message)
            }
        }.start()


        
        //get lat and long from street using geocoder

        

        /*try {
            var searchString = toilet.straat; //+ " " + toilet.huisnummer

            var geoResults: MutableList<Address>? = geocoder!!.getFromLocationName(searchString, 1)
            if (geoResults?.isNotEmpty() == true) {
                val addr = geoResults?.get(0)
                val location = addr?.let { it1 -> GeoPoint(it1.latitude, addr.longitude) }

                if (location != null) {
                    if (searchString != null) {
                        addMarker(location, searchString)
                    }
                }
            }else{
                Toast.makeText(this,"Location Not Found",Toast.LENGTH_LONG)
            }
        } catch (e: java.lang.Exception) {
            println(e.message)
        }*/

        //convert lat/long to doubles:
        /*var lat = 0.0
        var long = 0.0

        if (toilet.long != "null" && toilet.lat != "null") {
            lat = toilet.lat!!.toDouble()
            long = toilet.long!!.toDouble()
        }

        if (lat == 0.0 || long == 0.0) {
            //get lat/long from address
            var geoResults: MutableList<Address>? = geocoder!!.getFromLocationName(toilet.straat, 1)
            if (geoResults?.isNotEmpty() == true) {
                val addr = geoResults?.get(0)
                val location = addr?.let { it1 -> GeoPoint(it1.latitude, addr.longitude) }

                if (location != null) {
                    lat = location.latitude
                    long = location.longitude
                }
            }
            else {
                System.out.println("Address Not Found For " + toilet.straat)
            }
        }

        var geoResults: MutableList<Address>? = this.geocoder!!.getFromLocation(lat!!, long!!, 1)
        if (geoResults?.isNotEmpty() == true) {
            val addr = geoResults?.get(0)
            val location = addr?.let { it1 -> GeoPoint(it1.latitude, addr.longitude) }

            if (location != null) {
                addMarker(location, addr.getAddressLine(0))
            }

        }else{
            System.out.println("Location Not Found For " + toilet.long + " - " + toilet.lat)
        }*/
    }

    private fun addMarker(geoPoint: GeoPoint, name: String) {
        val startMarker = Marker(mMapView)
        startMarker.position = geoPoint
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = name;
        mMapView?.overlays?.add(startMarker)
    }

    fun setCenter(geoPoint: GeoPoint, name: String) {
        addMarker(geoPoint, name)
        mMapView?.controller?.setZoom(18.0)
        mMapView?.controller?.setCenter(geoPoint);
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    // AsyncTask inner class
    inner class MyAsyncTask : AsyncTask<URL, Int, String>() {


        override fun doInBackground(vararg params: URL?): String {

             return ""
        }

        // vararg : variable number of arguments
        // * : spread operator, unpacks an array into the list of values from the array
        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

        }
    }
}
