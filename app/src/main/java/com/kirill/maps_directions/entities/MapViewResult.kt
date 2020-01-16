package com.kirill.maps_directions.entities

import com.google.android.gms.maps.model.PolylineOptions

data class MapViewResult(val polyline: PolylineOptions?, val distance: String)
