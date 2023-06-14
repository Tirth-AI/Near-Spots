package com.example.nearspots.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.nearspots.DataModel.Place
import com.example.nearspots.R
import com.example.nearspots.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Timer
import kotlin.concurrent.timerTask


class MainActivity : AppCompatActivity(), OnMarkerClickListener {
    var fusedLocationProviderClient: FusedLocationProviderClient? = null
    var mapFragment: SupportMapFragment? = null
    private var googleMap: GoogleMap? = null
    private var location: Location? = null
    private val PERMISSION_ID = 101
    private var locationRequest: LocationRequest? = null
    private var placesList: ArrayList<Place>? = null
    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapFragment =
            supportFragmentManager.findFragmentById(R.id.frag_myMap) as SupportMapFragment
        mapFragment!!.getMapAsync { it ->
            googleMap = it
        }
        binding.svSearchPlace.clearFocus()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getUserLocation()

        binding.btnGetLocation.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            binding.progressBar.isVisible = true
            getUserLocation()
            binding.progressBar.isVisible = false
        }

        binding.svSearchPlace.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.svSearchPlace.clearFocus()
                binding.progressBar.isVisible = true
                return if (query!!.isNotEmpty()) {
                    searchLocation(query)
                    true
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Please enter correct city name",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }
                binding.progressBar.isVisible = false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }

        })

        binding.cvRestaurant.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Restaurant", R.mipmap.restaurant)
        }

        binding.cvCafe.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Hospital", R.mipmap.cafe)
        }

        binding.cvHotel.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Hotel", R.mipmap.hotel)
        }

        binding.cvHospital.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Hospital", R.mipmap.hospital)
        }

        binding.cvSchool.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("School", R.mipmap.school)
        }

        binding.cvPark.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Park", R.mipmap.park)
        }

        binding.cvMuseums.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Museum", R.mipmap.museum)
        }

        binding.cvPoliceStation.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("PoliceStation", R.mipmap.police)
        }

        binding.cvBank.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Bank", R.mipmap.bank)
        }

        binding.cvBar.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Bar", R.mipmap.bar)
        }
    }

    private fun callAPI(place: String, drawable: Int) {
        googleMap!!.clear()
        binding.progressBar.isVisible = true

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
            return
        } else {
            if (location != null) {
                val url =
                    "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + "${location!!.latitude},${location!!.longitude}" +
                            "&radius=10000&types=" + place + "&key=AIzaSyA3zeQUA47kyCgI5XJFJMn6zybxb3jPqeQ"

                val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.GET, url, null,
                    { response ->
                        val resultsArray = response.getJSONArray("results")
                        placesList = ArrayList()
                        for (i in 0 until resultsArray.length()) {
                            val singleResultObject = resultsArray.getJSONObject(i)
                            val name = singleResultObject.get("name").toString()
                            val geometry = singleResultObject.getJSONObject("geometry")
                            val searchedLocation = geometry.getJSONObject("location")
                            val lat = searchedLocation.get("lat") as Double
                            val lng = searchedLocation.get("lng") as Double
                            val latLng = LatLng(lat, lng)
                            val address = singleResultObject.get("vicinity").toString()
                            val imageUrl = singleResultObject.get("icon").toString()
                            val currentPlace = Place(name, address, latLng, imageUrl)
                            placesList!!.add(currentPlace)

                            googleMap!!.addMarker(
                                MarkerOptions().position(latLng)
                                    .title(name).icon(setIcon(this@MainActivity, drawable))
                            )
                            googleMap!!.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    latLng,
                                    15F
                                )
                            )
                        }
                        binding.progressBar.isVisible = false
                        googleMap!!.setOnMarkerClickListener(this)
                    },

                    {
                        Toast.makeText(
                            this,
                            "Place Not Found !!!",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.isVisible = false
                    })

                MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
            } else {
                binding.progressBar.isVisible = false
                Toast.makeText(this@MainActivity, "Location is NULL! Try again after the Current Location! marker appears.", Toast.LENGTH_SHORT).show()
                getUserLocation()
            }
        }
    }


    private fun searchLocation(query: String) {
        val geoCoder = Geocoder(this)
        var addressList: List<Address>? = null
        try {
            addressList = geoCoder.getFromLocationName(query, 1)

        } catch (e: Exception) {
            Toast.makeText(
                this@MainActivity,
                "Please enter the correct place name",
                Toast.LENGTH_SHORT
            ).show()
        }

        val address = addressList!![0]
        if (address == null) {
            Toast.makeText(
                this@MainActivity,
                "Please enter the correct place name",
                Toast.LENGTH_SHORT
            ).show()
        }
        val latLng = LatLng(address.latitude, address.longitude)
        googleMap!!.addMarker(
            MarkerOptions().position(latLng)
                .title("Searched Location!")
        )
        googleMap!!.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                15F
            )
        )
        googleMap!!.setOnMarkerClickListener(this)
    }

    private fun getUserLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                } else {
                    fusedLocationProviderClient!!.lastLocation.addOnCompleteListener { task ->
                        location = task.result
                        if (location != null) {
                            try {
                                val locationLatLng =
                                    LatLng(location!!.latitude, location!!.longitude)
                                googleMap!!.clear()
                                googleMap!!.addMarker(
                                    MarkerOptions().position(locationLatLng)
                                        .title("Current Location!").icon(
                                            setIcon(
                                                this@MainActivity,
                                                R.mipmap.human
                                            )
                                        )
                                )
                                googleMap!!.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        locationLatLng,
                                        15F
                                    )
                                )
                                googleMap!!.setOnMarkerClickListener(this)
                            } catch (e: IOException) {

                            }

                        }
                    }
                }
            } else {
                turnOnGPS()
            }
        } else {
            requestPermission()
        }
    }

    private fun turnOnGPS() {
        locationRequest = LocationRequest.create()
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest!!.interval = 5000
        locationRequest!!.fastestInterval = 2000

        var builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)

        builder.setAlwaysShow(true)

        val result = LocationServices.getSettingsClient(
            this.applicationContext
        )
            .checkLocationSettings(builder.build())

        result.addOnCompleteListener { task ->
            try {
                // when GPS is ON
                    val response = task.getResult(
                    ApiException::class.java
                )
                Timer().schedule(timerTask {
                    TODO("Do something")
                }, 2000)
                getUserLocation()

            } catch (e: ApiException) {
                // when GPS is OFF
                e.printStackTrace()

                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        // here we send the request to enable the GPS
                        val resolvableApiException = e as ResolvableApiException
                        resolvableApiException.startResolutionForResult(this, 200)

                    } catch (sendIntentException: IntentSender.SendIntentException) {
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // when settings is unavailable
                    }
                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(
                    this@MainActivity,
                    "GPS is turned On by requesting",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "GPS is not turned On by requesting",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
                getUserLocation()
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setIcon(context: Context, drawableID: Int): BitmapDescriptor {
        val drawable = ActivityCompat.getDrawable(context, drawableID)
        drawable?.setBounds(0, 0, drawable.intrinsicHeight, drawable.intrinsicWidth)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    @SuppressLint("SetTextI18n")
    override fun onMarkerClick(marker: Marker): Boolean {
        val pressedLatLng = marker.position
        for (i in 0 until placesList!!.size) {
            if (placesList!![i].latLng == pressedLatLng) {
                binding.cvClickedPlace.isVisible = true
                Glide.with(binding.ivPlaceIcon).load(placesList!![i].imageUrl)
                    .into(findViewById(R.id.iv_placeIcon))
                binding.tvClickedPlaceName.text = placesList!![i].name
                binding.tvClickedPlaceAddress.text = "Address: " + placesList!![i].address
            }
        }
        return false
    }
}

class MySingleton constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: MySingleton? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MySingleton(context).also {
                    INSTANCE = it
                }
            }
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }
}
