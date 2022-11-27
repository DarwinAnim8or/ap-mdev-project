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
        db?.execSQL("create table preferences (id integer primary key autoincrement, name text, value text)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("drop table if exists preferences")
    }

    fun insertData(name: String, value: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("name", name)
        contentValues.put("value", value)
        db.insert("preferences", null, contentValues)
    }

    fun updateData(name: String, value: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("value", value)
        db.update("preferences", contentValues, "name = ?", arrayOf(name))
    }

    @SuppressLint("Range")
    fun getData(name: String): String {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("select * from preferences where name = ?", arrayOf(name))
        var value = ""
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndex("value"))
        }
        return value
    }

    fun deleteData(name: String) {
        val db = this.writableDatabase
        db.delete("preferences", "name = ?", arrayOf(name))
    }

    companion object {
        val DATABASE_NAME = "preferences.db"
    }

}