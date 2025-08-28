package com.alimaharramly.gpstracker.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track")
data class TrackItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    @ColumnInfo(name = "time")
    val time: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "distance")
    val distance: Double,  // ← теперь Double, не String
    @ColumnInfo(name = "velocity")
    val velocity: String,
    @ColumnInfo(name = "geo_points")
    val geoPoints: String
)





