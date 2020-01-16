package com.kirill.maps_directions

import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.kirill.maps_directions.entities.MapViewResult
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DestinationHelper (val apiKey: String) {

    suspend fun getRout(origin: LatLng, dest: LatLng): MapViewResult? {
        val routFromServer = downloadRout(origin, dest)
        if (routFromServer != null) {
            val parsedResult = parseResult(routFromServer)
            if (parsedResult != null) {
                val resultPolyline = getPolyline(parsedResult)
                if (resultPolyline != null) {
                    return resultPolyline
                } else {
                    return null
                }
            } else {
                Log.e("RequestError", "Parsed result is null")
                return null
            }
        } else {
            Log.e("RequestError", "null in response")
            return null
        }
    }

    suspend fun downloadRout(origin: LatLng, dest: LatLng): String? {
        val distanation = getDirectionsUrl(origin, dest)
        distanation?.let {
            try {
                return downloadUrl(it)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    suspend fun parseResult(jsonData: String): List<List<HashMap<String, String>>>? {
        val jObject: JSONObject
        var routes: List<List<HashMap<String, String>>>? = null
        try {
            jObject = JSONObject(jsonData)
            val parser = DirectionsJSONParser()
            routes = parser.parse(jObject)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return routes
    }

    suspend fun getPolyline(routes: List<List<HashMap<String, String>>>): MapViewResult? {
        var points = ArrayList<LatLng>()
        var lineOptions: PolylineOptions? = null
        var distance : String = ""
        for (i in routes.indices) {
            points = ArrayList()
            lineOptions = PolylineOptions()
            val path: List<HashMap<String, String>> = routes[i]
            for (j in path.indices) {
                val point = path[j]
                val latS = point["lat"]
                val lngS = point["lng"]
                if (latS != null && lngS != null) {
                    val lat: Double = latS.toDouble()
                    val lng: Double = lngS.toDouble()
                    val position = LatLng(lat, lng)
                    points.add(position)
                }
                val distanceS = point["distance"]
                if (!distanceS.isNullOrEmpty()) {
                    distance = distanceS
                }
            }
            lineOptions.addAll(points)
            lineOptions.width(12f)
            lineOptions.color(Color.RED)
            lineOptions.geodesic(true)
        }
        return MapViewResult(lineOptions, distance)
    }


    fun getDirectionsUrl(origin: LatLng, dest: LatLng): String? { // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude
        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude
        // Sensor enabled
        val sensor = "sensor=false"
        val mode = "mode=driving"
        val units = "units=imperial"
        // Building the parameters to the web service
        val parameters = "$units&$str_origin&$str_dest&$sensor&$mode"
        // Output format
        val output = "json"
        // Building the url to the web service
        val key = "&key=$apiKey"
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters?$key"
    }

    @Throws(IOException::class)
    suspend fun downloadUrl(strUrl: String): String? {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()
            iStream = urlConnection.getInputStream()
            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()
            var line: String? = ""
            while (br.readLine().also({ line = it }) != null) {
                sb.append(line)
            }
            data = sb.toString()
            br.close()
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        } finally {
            iStream?.close()
            urlConnection?.disconnect()
        }
        return data
    }

}


