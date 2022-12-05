//helper class for the sqite database for preferences
package be.ap.edu.mapsaver

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("create table faves (id integer primary key autoincrement, name text, value text)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("drop table if exists faves")
    }

    fun insertFave(Id: String, value: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("Id", Id)
        contentValues.put("value", value)
        db.insert("faves", null, contentValues)
    }

    @SuppressLint("Range")
    fun getFave(Id: String): String {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("select * from faves where Id = ?", arrayOf(Id))
        var value = ""
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndex("value"))
        }
        return value
    }

    fun deleteFaves() {
        val db = this.writableDatabase
        db?.execSQL("drop table if exists faves")
        db?.execSQL("create table faves (id integer primary key autoincrement, name text, value text)")
    }

    companion object {
        val DATABASE_NAME = "preferences.db"
    }

}