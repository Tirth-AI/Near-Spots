package com.example.nearspots.DataModel

import com.google.android.gms.maps.model.LatLng

data class Place(
    val name: String,
    val address: String,
    val latLng: LatLng,
    val imageUrl: String)
