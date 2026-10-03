package com.example.dirt.measurement

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Measurement(val id: String, val angle: Float, val name: String, val time: Long)

class MeasurementStore(context: Context) {
    private val prefs = context.getSharedPreferences("dirt_fieldbook", Context.MODE_PRIVATE)

    fun load(): List<Measurement> = runCatching {
        val array = JSONArray(prefs.getString("measurements", "[]"))
        List(array.length()) { i ->
            val item = array.getJSONObject(i)
            Measurement(item.getString("id"), item.getDouble("angle").toFloat(), item.getString("name"), item.getLong("time"))
        }
    }.getOrDefault(emptyList())

    fun save(angle: Float, name: String): List<Measurement> {
        val item = Measurement(UUID.randomUUID().toString(), angle, name.trim(), System.currentTimeMillis())
        return (listOf(item) + load()).also(::write)
    }

    fun delete(id: String): List<Measurement> = load().filterNot { it.id == id }.also(::write)

    private fun write(items: List<Measurement>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("angle", item.angle).put("name", item.name).put("time", item.time))
        }
        prefs.edit { putString("measurements", array.toString()) }
    }
}
