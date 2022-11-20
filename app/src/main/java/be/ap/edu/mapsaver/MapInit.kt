package be.ap.edu.mapsaver

import android.app.PendingIntent.getActivity
import android.widget.Toast
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
//import org.graalvm.compiler.core.common.LIRKind.reference
import org.json.JSONObject
import java.net.*
import java.util.*

class MapInit {
    val db = Firebase.firestore

    fun Initialize() {
        //check firebase db to see if there is any map data:
        if (checkForData()) {
            //if there is data, load it
            loadMapData()
        } else {
            //if there is no data, create a new map
            createNewMap()
        }
    }

    fun checkForData(): Boolean {
        //simply check to see if there's anything in the firebase db
        //if there is, return true
        val toilets = db.collection("toilets")

        //temp variable for snapshot
        var snapshot: QuerySnapshot? = null
        
        toilets.get().addOnSuccessListener { result ->
            for (document in result) {
                println("DocumentSnapshot data: ${document.data}")
            }

            snapshot = result
        }

        if (snapshot != null) {
            return true
        } else {
            return false
        }

        return false
    }

    fun createNewMap() {
        //1. download geojson data from antw
        //2. convert to toilet objects
        //3. save to firebase
        //4. load map data

        //1. download geojson data from antw on a new thread

        //start new thread to download geojson data
        Thread {
            //download data
            val url = URL("https://opendata.arcgis.com/api/v3/datasets/eda49af804c9467e97393ca35e34714b_8/downloads/data?format=geojson&spatialRefId=4326&where=1%3D1")
            val response = url.readText()

            //convert to json
            val json = JSONObject(response)

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
                toilets.add(toilet)
            }

            //4. save to firebase
            for (toilet in toilets) {
                toilet.id?.let { db.collection("toilets").document(it).set(toilet) }
            }
        }.start()

            


        /*val url = URL("https://opendata.arcgis.com/api/v3/datasets/eda49af804c9467e97393ca35e34714b_8/downloads/data?format=geojson&spatialRefId=4326&where=1%3D1")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.connect()

        val responseCode = connection.responseCode
        println("Response code: $responseCode")

        val inputStream = connection.inputStream
        val json = inputStream.bufferedReader().use { it.readText() }
        println(json)*/

        //val url = URL("https://opendata.arcgis.com/api/v3/datasets/eda49af804c9467e97393ca35e34714b_8/downloads/data?format=geojson&spatialRefId=4326&where=1%3D1")
        //val json = url.readText()

        //2. sanitize data

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

        //5. load map data
        loadMapData()
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

                //call the add function from the mainActivity
                MainActivity().addToilet(toilet)
            }
        }
    }
}