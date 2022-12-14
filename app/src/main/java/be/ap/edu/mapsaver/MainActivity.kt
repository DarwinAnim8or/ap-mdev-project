package be.ap.edu.mapsaver

import DataModels.Toilet
import Helpers.DatabaseHelper
import Helpers.MapInit
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.beust.klaxon.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    val db = Firebase.firestore
    
    //arraylist of toilet objects for the map / init file
    
    var toiletsFiltered = ArrayList<Toilet>()
    var toiletsFave = ArrayList<String>() //array list of Ids for toilets we like

    var dbHelper: DatabaseHelper? = null

    //private var mMapView: MapView? = null
    private var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    private var items = ArrayList<OverlayItem>()

    //checkboxes for filters
    private var cbBothGenders: CheckBox? = null
    private var cbDiaper: CheckBox? = null
    private var cbHandi: CheckBox? = null

    private var addButton: FloatingActionButton? = null
    private var listButton: FloatingActionButton? = null

    private val urlNominatim = "https://nominatim.openstreetmap.org/"

    //static geocoder for use in other classes
    companion object {
        var geocoder: Geocoder? = null
        var mMapView: MapView? = null
        var listOpen : Boolean = false
        lateinit var mUserLocation: GeoPoint
        var toilets = ArrayList<Toilet>()

        //Variables for filters:
        var mustBeBothGenders: Boolean = false //must have toilets for both genders
        var mustBeDiaper: Boolean = false //must have diaper changing area
        var mustBeHandi: Boolean = false //must have a handicap-accessible toilet

        lateinit var fusedLocationClient: FusedLocationProviderClient
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        //Don't forget to actually init firebase:
        FirebaseApp.initializeApp(this)

        //create geocoder object
        geocoder = Geocoder(this, Locale.getDefault())

        //get location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this!!)
        mUserLocation = getuserPosition()

        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(applicationContext, getDefaultSharedPreferences(applicationContext))
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

        addButton = findViewById(R.id.fab_add)
        addButton?.setOnClickListener {
            //start new activity to add a toilet
            val intent = Intent(this, AddToilet::class.java)

            //when we return, refresh map
            startActivityForResult(intent, 1)

            //toilets.clear()
            MapInit().Initialize()
        }

        listButton = findViewById(R.id.fab_list)
        listButton?.setOnClickListener {
            if(listOpen) {
                supportFragmentManager.popBackStackImmediate()
            } else {
                supportFragmentManager.commit {
                    replace<ItemFragment>(R.id.fragment_container_view)
                    setReorderingAllowed(true)
                    addToBackStack("List")
                }
            }
            listOpen = !listOpen
        }

        if (hasPermissions()) {
            initMap()
        }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        //set up checkboxes
        cbBothGenders = findViewById(R.id.cbGender)
        cbDiaper = findViewById(R.id.cbLuier)
        cbHandi = findViewById(R.id.cbHandi)

        //on click listeners for checkboxes, taking toilets with it and refreshing map
        cbBothGenders!!.setOnClickListener {
            mustBeBothGenders = cbBothGenders!!.isChecked
            filterToilets(mustBeBothGenders, mustBeHandi, mustBeDiaper)
            saveFilters(mustBeBothGenders, mustBeHandi, mustBeDiaper)
        }

        cbDiaper!!.setOnClickListener {
            mustBeDiaper = cbDiaper!!.isChecked
            filterToilets(mustBeBothGenders, mustBeHandi, mustBeDiaper)
            saveFilters(mustBeBothGenders, mustBeHandi, mustBeDiaper)
        }

        cbHandi!!.setOnClickListener {
            mustBeHandi = cbHandi!!.isChecked
            filterToilets(mustBeBothGenders, mustBeHandi, mustBeDiaper)
            saveFilters(mustBeBothGenders, mustBeHandi, mustBeDiaper)
        }

        //pull toilets:
        MapInit().Initialize()

        //read our preferences
        dbHelper = DatabaseHelper(this)
        readFilters()
        readFavesFromLocalSQLite()

        //set the states of the checkboxes
        cbHandi!!.isChecked = mustBeHandi
        cbDiaper!!.isChecked = mustBeDiaper
        cbBothGenders!!.isChecked = mustBeBothGenders
    }

    private fun moveMap(location: GeoPoint) {
        setCenter(location, "test")
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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

    @SuppressLint("MissingPermission")
    fun initMap() {
        mMapView?.setTileSource(TileSourceFactory.MAPNIK)

         // MiniMap
        //val miniMapOverlay = MinimapOverlay(this, mMapView!!.tileRequestCompleteHandler)
        //this.mMapView?.overlays?.add(miniMapOverlay)

        //set our default location to Ellermanstraat 33
        val startPoint = GeoPoint(51.219447, 4.402464)
        setCenter(startPoint, "Ellermanstraat 33")

        //get location of user
        addUserPosition()
    }

    fun addToilet(toilet: Toilet, isFiltered: Boolean = false) {
        if (!isFiltered)
            toilets.add(toilet);

        addMarker(GeoPoint(toilet.geoLat, toilet.geoLong), toilet.naam!!)
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

    fun clearMap() {
        mMapView?.overlays?.clear()
        mMapView?.invalidate() // Redraw map
    }

    fun addUserPosition() {
        setCenter(getuserPosition(), "You are here")
    }

    @SuppressLint("MissingPermission")
    fun getuserPosition(): GeoPoint {
        var output: GeoPoint = GeoPoint(51.219447, 4.402464)
        fusedLocationClient.lastLocation.addOnSuccessListener { location->
            if (location != null) {
                output = GeoPoint(location.latitude, location.longitude)
            }
        }
        return output
    }

    public fun filterToilets(mustBeBothGenders: Boolean, mustBeHandi: Boolean, mustBeDiaper: Boolean) {
        //erase toiletsFiltered and map icons:
        toiletsFiltered.clear()
        clearMap()

        //filter toilets:
        for (toilet in toilets) {
            if (mustBeBothGenders && !toilet.doelgroep.equals("man/vrouw")) {
                continue
            }
            if (mustBeHandi && !toilet.handicap.equals("ja")) {
                continue
            }
            if (mustBeDiaper && !toilet.luiertafel.equals("ja")) {
                continue
            }
            toiletsFiltered.add(toilet)
        }

        //add toilets to map:
        for (toilet in toiletsFiltered) {
            addToilet(toilet, true)
        }


        addUserPosition()
    }

    fun readFilters() {
        //load filters from shared preferences:
        val sharedPref = getSharedPreferences("filters", Context.MODE_PRIVATE)

        mustBeBothGenders = sharedPref.getBoolean("mustBeBothGenders", false)
        mustBeHandi = sharedPref.getBoolean("mustBeHandi", false)
        mustBeDiaper = sharedPref.getBoolean("mustBeDiaper", false)

        //filter toilets:
        filterToilets(mustBeBothGenders, mustBeHandi, mustBeDiaper)
    }

    @SuppressLint("Range")
    fun readFavesFromLocalSQLite() {
        //load faves
        for (toilet in toilets) {
            val isF = toilet.id?.let { dbHelper!!.getFave(it) }
            if (isF != "") {
                toilet.id?.let { toiletsFave.add(it) }
            }
        }
    }

    fun saveFilters(mustBeBothGenders: Boolean, mustBeHandi: Boolean, mustBeDiaper: Boolean) {
        val sharedPref = getSharedPreferences("filters", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("mustBeBothGenders", mustBeBothGenders)
            putBoolean("mustBeHandi", mustBeHandi)
            putBoolean("mustBeDiaper", mustBeDiaper)
            apply()
        }
    }

    fun saveFavesToLocalSQLite() {
        val dpHelper = DatabaseHelper(this)
        dbHelper?.deleteFaves() //clear previous faves

        for (Id: String in toiletsFave) {
            dbHelper?.insertFave(Id, "true")
        }
    }
}
