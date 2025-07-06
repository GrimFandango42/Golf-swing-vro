package com.swingsync.ai.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.swingsync.ai.data.models.*
import java.util.Date

class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromAnalysisResults(value: AnalysisResults?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toAnalysisResults(value: String?): AnalysisResults? {
        return value?.let { gson.fromJson(it, AnalysisResults::class.java) }
    }
    
    @TypeConverter
    fun fromSwingType(value: SwingType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toSwingType(value: String?): SwingType? {
        return value?.let { SwingType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromHandType(value: HandType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toHandType(value: String?): HandType? {
        return value?.let { HandType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromExperienceLevel(value: ExperienceLevel?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toExperienceLevel(value: String?): ExperienceLevel? {
        return value?.let { ExperienceLevel.valueOf(it) }
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        }
    }
    
    @TypeConverter
    fun fromUserSettings(value: UserSettings?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toUserSettings(value: String?): UserSettings? {
        return value?.let { gson.fromJson(it, UserSettings::class.java) }
    }
    
    @TypeConverter
    fun fromVideoQuality(value: VideoQuality?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toVideoQuality(value: String?): VideoQuality? {
        return value?.let { VideoQuality.valueOf(it) }
    }
    
    @TypeConverter
    fun fromAnalysisSensitivity(value: AnalysisSensitivity?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toAnalysisSensitivity(value: String?): AnalysisSensitivity? {
        return value?.let { AnalysisSensitivity.valueOf(it) }
    }
}