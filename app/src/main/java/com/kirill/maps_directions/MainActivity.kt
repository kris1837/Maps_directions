package com.kirill.maps_directions

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnFailureListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    val tag = "MainActivity"
    var isLocationPermissionGranted = false
    val PERMISSIONS_REQUEST_ENABLE_GPS = 1
    val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2
    val ERROR_DIALOG_REQUEST = 3
    val MAPVIEW_BUNDLE_KEY = "map_view_bundle_key"

    var mMenu : Menu? = null

    protected val viewModelJob = SupervisorJob()
    protected val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (getLocationPermission()) {
            if (checkMapServices()) {
                var bundleToSend = Bundle()
                savedInstanceState?.let { bundle ->
                    bundle.getBundle(MAPVIEW_BUNDLE_KEY)?.let {
                        bundleToSend = it
                    }
                }
                uiMap.onCreate(bundleToSend)
                uiMap.getMapAsync(this)

            }
        }
    }

    private fun checkMapServices(): Boolean {
        if (isServicesOK()) {
            if (isMapsEnabled()) {
                return true
            }
        }
        return false
    }

    private fun buildAlertMessageNoGps() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
                val enableGpsIntent =
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS)
            })
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    fun isMapsEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            return false
        }
        return true
    }

    private fun getLocationPermission() : Boolean { /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionGranted = true
            return true
            //getChatrooms()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
            return false
        }
    }

    fun isServicesOK(): Boolean {
        Log.d(tag, "isServicesOK: checking google services version")
        val available =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this@MainActivity)
        if (available == ConnectionResult.SUCCESS) { //everything is fine and the user can make map requests
            Log.d(tag, "isServicesOK: Google Play Services is working")
            return true
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) { //an error occured but we can resolve it
            Log.d(tag, "isServicesOK: an error occured but we can fix it")
            val dialog: Dialog = GoogleApiAvailability.getInstance()
                .getErrorDialog(this@MainActivity, available, ERROR_DIALOG_REQUEST)
            dialog.show()
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        isLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    isLocationPermissionGranted = true
                }
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(tag, "onActivityResult: called.")
        when (requestCode) {
            PERMISSIONS_REQUEST_ENABLE_GPS -> {
                if (isLocationPermissionGranted) {
                    //getChatrooms()
                } else {
                    getLocationPermission()
                }
            }
        }
    }

    var currentLocation = LatLng(-33.852, 151.211)

    override fun onMapReady(map: GoogleMap?) {
        if (!checkMapServices()) {return}
        val sydney = LatLng(-33.852, 151.211)
        map?.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map?.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        if (map == null) {
            return
        }

        map.isMyLocationEnabled = true

        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mFusedLocationClient.lastLocation.addOnSuccessListener{ location ->
            val la = location.getLatitude()
            val lo = location.getLongitude()
            currentLocation = LatLng(la, lo)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18.0f))
        }.addOnFailureListener(OnFailureListener {

        })

        map.setOnMapLongClickListener {
            map.clear()
            val marker = MarkerOptions()

            marker.position(it)
            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_action_name))
            map.addMarker(marker)

            uiScope.async {
                // Here is supposed to go API key
                val distanantionHelper = DestinationHelper(resources.getString(R.string.google_api_key))
                val result = async(Dispatchers.IO) {
                    distanantionHelper.getRout(currentLocation, it)
                }.await()
                // Update UI here
                map.addPolyline(result?.polyline)
                mMenu?.findItem(R.id.uiMiles)?.title = result?.distance
            }

        }

        map.setOnMarkerClickListener(object: GoogleMap.OnMarkerClickListener{
            override fun onMarkerClick(p0: Marker?): Boolean {
                Toast.makeText(applicationContext, "OnMarkerClicked", Toast.LENGTH_LONG).show()
                return true
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mMenu = menu
        getMenuInflater().inflate(R.menu.main_menu, menu)
        return true
    }


    override fun onStart() {
        if (isLocationPermissionGranted) {
            uiMap.onStart()
        }
        super.onStart()
    }

    override fun onResume() {
        if (isLocationPermissionGranted) {
            uiMap.onResume()
        }
        super.onResume()
    }

    override fun onPause() {
        uiMap.onPause()
        viewModelJob.cancel()
        super.onPause()
    }

    override fun onStop() {
        uiMap.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        uiMap.onDestroy()
        super.onDestroy()
    }
}
