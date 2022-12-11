package Helpers

import android.location.Address
import be.ap.edu.mapsaver.MainActivity
import DataModels.Toilet
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
//import org.graalvm.compiler.core.common.LIRKind.reference
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.*
import java.util.*

class MapInit {
    val db = Firebase.firestore

    fun Initialize() {
        //createNewMap()

        //check firebase db to see if there are toilets saved:
        db.collection("toilets").get().addOnSuccessListener(OnSuccessListener<QuerySnapshot> { querySnapshot ->
            //if there are 0 toilets in this collection or the collection does not exist
            if (querySnapshot.isEmpty) {
                createNewMap()
            } else {
                loadMapData()
            }
        })
    }

    fun createNewMap() {
        //start new thread to download geojson data
        Thread {
            //download data
            val url = URL("https://opendata.arcgis.com/api/v3/datasets/eda49af804c9467e97393ca35e34714b_8/downloads/data?format=geojson&spatialRefId=4326&where=1%3D1")
            val response = url.readText()

            //convert to json
            val json = JSONObject(response)

            /*
                velden:
                    -categorie
                    -omschrijving
                    -stadseigendom
                    -betalend
                    -straat
                    -huisnummer
                    -postcode
                    -luiertafel
                    -doelgroep
                    -OPENINGSUREN_OPM
                    -lat
                    -long
                    -INTEGRAAL_TOEGANKELIJK (handicap)
             */

            //read json
            val toilets = ArrayList<Toilet>()
            val jsonToilets = json.getJSONArray("features")
            for (i in 0 until jsonToilets.length()) {
                val toilet = Toilet()
                val toiletJson = jsonToilets.getJSONObject(i).getJSONObject("properties")

                toilet.id = toiletJson.getString("ID")
                toilet.categorie = toiletJson.getString("CATEGORIE")
                toilet.omschrijving = toiletJson.getString("OMSCHRIJVING")
                toilet.stadseigendom = toiletJson.getString("STADSEIGENDOM")
                toilet.betalend = toiletJson.getString("BETALEND")
                toilet.straat = toiletJson.getString("STRAAT")
                toilet.huisnummer = toiletJson.getString("HUISNUMMER")
                toilet.postcode = toiletJson.getString("POSTCODE")
                toilet.luiertafel = toiletJson.getString("LUIERTAFEL")
                toilet.doelgroep = toiletJson.getString("DOELGROEP")
                toilet.openingsuren = toiletJson.getString("OPENINGSUREN_OPM")

                toilet.lat = toiletJson.getString("LAT")
                toilet.long = toiletJson.getString("LONG")

                toilet.handicap = toiletJson.getString("INTEGRAAL_TOEGANKELIJK")
                toilet.naam = toilet.straat + " " + toilet.huisnummer;

                //get the long and latitude from geopoint, so we can save it
                try {

                    var geoResults: MutableList<Address>? = MainActivity.geocoder!!.getFromLocationName(toilet.naam, 1)
                    if (geoResults?.isNotEmpty() == true) {
                        val addr = geoResults?.get(0)
                        val location = addr?.let { it1 -> GeoPoint(it1.latitude, addr.longitude) }

                        if (location != null) {
                            toilet.geoLat = location.latitude;
                            toilet.geoLong = location.longitude;
                        }
                    }
                } catch (e: java.lang.Exception) {
                    print(e.message)
                }

                toilets.add(toilet)
            }

            //4. save to firebase
            for (toilet in toilets) {
                try {
                    db.collection("toilets").document(toilet.id!!).set(toilet)
                } catch (e: Exception) {
                    print(e.message)
                }
                //toilet.id?.let { db.collection("toilets").document(it).set(toilet) }
            }

            loadMapData()
        }.start()
    }

    fun loadMapData() {
        //this functions just loads the toilets data from firestore
        //and puts it in the toilets arraylist
        val toilets = db.collection("toilets")
        toilets.get().addOnSuccessListener { result ->
            for (document in result) {
                val toilet = Toilet()
                toilet.id = document.id
                toilet.categorie = document.getString("categorie")
                toilet.omschrijving = document.getString("omschrijving")
                toilet.stadseigendom = document.getString("stadseigendom")
                toilet.betalend = document.getString("betalend")
                toilet.straat = document.getString("straat")
                toilet.huisnummer = document.getString("huisnummer")
                toilet.postcode = document.getString("postcode")
                toilet.luiertafel = document.getString("luiertafel")
                toilet.doelgroep = document.getString("doelgroep")
                toilet.openingsuren = document.getString("openingsuren")
                toilet.lat = document.getString("lat")
                toilet.long = document.getString("long")
                toilet.handicap = document.getString("handicap")

                //custom fields
                toilet.naam = document.getString("naam")
                toilet.geoLat = document.getDouble("geoLat")!!
                toilet.geoLong = document.getDouble("geoLong")!!

                //call the add function from the mainActivity
                MainActivity().addToilet(toilet)
            }
        }
    }
}