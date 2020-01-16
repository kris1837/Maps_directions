package com.kirill.maps_directions.entities

data class MapResponse (
    val geocodedWaypoints: List<GeocodedWaypoint>,
    val routes: List<Route>,
    val status: String
)

data class GeocodedWaypoint (
    val geocoderStatus: String,
    val placeID: String,
    val types: List<String>
)

data class Route (
    val bounds: Bounds,
    val copyrights: String,
    val legs: List<Leg>,
    val overviewPolyline: Polyline,
    val summary: String,
    val warnings: List<Any?>,
    val waypointOrder: List<Any?>
)

data class Bounds (
    val northeast: Northeast,
    val southwest: Northeast
)

data class Northeast (
    val lat: Double,
    val lng: Double
)

data class Leg (
    val distance: Distance,
    val duration: Distance,
    val endAddress: String,
    val endLocation: Northeast,
    val startAddress: String,
    val startLocation: Northeast,
    val steps: List<Step>,
    val trafficSpeedEntry: List<Any?>,
    val viaWaypoint: List<Any?>
)

data class Distance (
    val text: String,
    val value: Long
)

data class Step (
    val distance: Distance,
    val duration: Distance,
    val endLocation: Northeast,
    val htmlInstructions: String,
    val polyline: Polyline,
    val startLocation: Northeast,
    val travelMode: String
)

data class Polyline (
    val points: String
)