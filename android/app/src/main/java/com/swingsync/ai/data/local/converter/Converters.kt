package com.swingsync.ai.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.swingsync.ai.data.local.entity.KeypointEntity
import com.swingsync.ai.data.local.entity.LandmarkEntity

/**
 * Type converters for Room database to handle complex data types.
 */
class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
    }
    
    @TypeConverter
    fun fromFloatMap(value: Map<String, Float>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toFloatMap(value: String): Map<String, Float> {
        return gson.fromJson(value, object : TypeToken<Map<String, Float>>() {}.type)
    }
    
    @TypeConverter
    fun fromKeypointList(value: List<KeypointEntity>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toKeypointList(value: String): List<KeypointEntity> {
        return gson.fromJson(value, object : TypeToken<List<KeypointEntity>>() {}.type)
    }
    
    @TypeConverter
    fun fromLandmarkList(value: List<LandmarkEntity>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toLandmarkList(value: String): List<LandmarkEntity> {
        return gson.fromJson(value, object : TypeToken<List<LandmarkEntity>>() {}.type)
    }
}