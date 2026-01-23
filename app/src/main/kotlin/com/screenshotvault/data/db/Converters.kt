package com.screenshotvault.data.db

import androidx.room.TypeConverter
import com.screenshotvault.data.db.entities.ProcessingStatus

class Converters {

    @TypeConverter
    fun fromProcessingStatus(status: ProcessingStatus): String = status.name

    @TypeConverter
    fun toProcessingStatus(value: String): ProcessingStatus = ProcessingStatus.valueOf(value)
}
