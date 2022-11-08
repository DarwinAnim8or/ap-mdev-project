package be.ap.edu.mapsaver

import android.app.PendingIntent.getActivity
import android.widget.Toast
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.graalvm.compiler.core.common.LIRKind.reference
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
        return !toilets.get().result.isEmpty()
    }

    fun createNewMap() {
        //1. download data from antw
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
        //3. insert data to toilets collection
    }

    fun loadMapData() {
        //this functions just loads the toilets data from firestore
    }
}