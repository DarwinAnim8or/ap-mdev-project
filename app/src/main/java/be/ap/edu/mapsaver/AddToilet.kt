package be.ap.edu.mapsaver

import DataModels.Toilet
import android.location.Address
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Switch
import android.widget.TimePicker
import androidx.appcompat.widget.AppCompatEditText
import be.ap.edu.mapsaver.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.NonCancellable.cancel
import org.osmdroid.util.GeoPoint
import kotlin.random.Random

class AddToilet : AppCompatActivity() {
    val db = Firebase.firestore
    
    //private vars for various ui elements from the layout
    private lateinit var toiletAddress: EditText

    //the switches for gender, handicap, and baby
    private lateinit var genderSwitch: Switch
    private lateinit var handicapSwitch: Switch
    private lateinit var babySwitch: Switch

    //time picker for opening and closing time
    private lateinit var closingTime: AppCompatEditText

    //fabs for cancel and save
    private lateinit var cancel: FloatingActionButton
    private lateinit var save: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_toilet)

        //get the ui elements from the layout
        toiletAddress = findViewById(R.id.toiletAddress)

        //get the switches
        genderSwitch = findViewById(R.id.swGender)
        handicapSwitch = findViewById(R.id.swHandicap)
        babySwitch = findViewById(R.id.swChangingTable)

        //get the time picker
        closingTime = findViewById(R.id.ettClosingTime)

        //get the fabs
        cancel = findViewById(R.id.fabCancelAdd)
        save = findViewById(R.id.fabAdd)

        //set the listeners for the fabs
        cancel.setOnClickListener { finish() }
        save.setOnClickListener { 
            saveToilet() 

            //wait a second for fb to save the toilet
            Thread.sleep(1500)

            //finish the activity
            finish()
        }

        //set the listener for the address
        toiletAddress.setOnFocusChangeListener { _, _ ->  }
    }

    private fun saveToilet() {
        val newToilet = Toilet()
        newToilet.id = (1000 + Random.nextInt(1000)).toString()

        //get the address
        val address = toiletAddress.text.toString()
        newToilet.naam = address
        newToilet.straat = address

        if (genderSwitch.isChecked) {
            newToilet.doelgroep = "man/vrouw"
        } else {
            newToilet.doelgroep = "man"
        }

        if (handicapSwitch.isChecked) {
            newToilet.handicap = "ja"
        } else {
            newToilet.handicap = "null"
        }

        if (babySwitch.isChecked) {
            newToilet.luiertafel = "ja"
        } else {
            newToilet.luiertafel = "nee"
        }

        newToilet.openingsuren = "00:00 - ${closingTime.text}"

        //get the long and latitude from geopoint, so we can save it
        try {

            var geoResults: MutableList<Address>? = MainActivity.geocoder!!.getFromLocationName(newToilet.naam, 1)
            if (geoResults?.isNotEmpty() == true) {
                val addr = geoResults?.get(0)
                val location = addr?.let { it1 -> GeoPoint(it1.latitude, addr.longitude) }

                if (location != null) {
                    newToilet.geoLat = location.latitude;
                    newToilet.geoLong = location.longitude;
                }
            }
        } catch (e: java.lang.Exception) {
            print(e.message)
        }

        //add the toilet to the firebase
        try {
            db.collection("toilets").document(newToilet.id!!).set(newToilet)
        } catch (e: Exception) {
            print(e.message)
        }
    }
}