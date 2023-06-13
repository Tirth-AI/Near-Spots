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
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
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
import java.util.Locale


class MainActivity : AppCompatActivity(), OnMarkerClickListener {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var mapFragment: SupportMapFragment
    private lateinit var googleMap: GoogleMap
    private var location: Location? = null
    private val PERMISSION_ID = 101;
    private lateinit var placesList: ArrayList<Place>
    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            callAPI("Restaurant", location!!, R.mipmap.restaurant)
        }

        binding.cvCafe.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Hospital", location!!, R.mipmap.cafe)
        }

        binding.cvHotel.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Hotel", location!!, R.mipmap.hotel)
        }

        binding.cvHospital.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Hospital", location!!, R.mipmap.hospital)
        }

        binding.cvSchool.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("School", location!!, R.mipmap.school)
        }

        binding.cvPark.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Park", location!!, R.mipmap.park)
        }

        binding.cvMuseums.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Museum", location!!, R.mipmap.museum)
        }

        binding.cvPoliceStation.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("PoliceStation", location!!, R.mipmap.police)
        }

        binding.cvBank.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Bank", location!!, R.mipmap.bank)
        }

        binding.cvBar.setOnClickListener {
            binding.cvClickedPlace.isVisible = false
            callAPI("Bar", location!!, R.mipmap.bar)
        }

    }


    private fun callAPI(place: String, location: Location, drawable: Int) {
        googleMap.clear()
        binding.progressBar.isVisible = true
        val url =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + "${location.latitude},${location.longitude}" +
                    "&radius=10000&types=" + place + "&key=AIzaSyA3zeQUA47kyCgI5XJFJMn6zybxb3jPqeQ"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val resultsArray = response.getJSONArray("results")
                placesList = ArrayList<Place>()
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
                    placesList.add(currentPlace)

                    googleMap.addMarker(
                        MarkerOptions().position(latLng)
                            .title(name).icon(setIcon(this@MainActivity, drawable))
                    )
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            15F
                        )
                    )
                }
                binding.progressBar.isVisible = false
                googleMap.setOnMarkerClickListener(this)
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
        googleMap.addMarker(
            MarkerOptions().position(latLng)
                .title("Searched Location!")
        )
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                15F
            )
        )
        googleMap.setOnMarkerClickListener(this)
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
                    fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                        task.addOnSuccessListener { it ->
                            location = it
                            if (location != null) {
                                mapFragment =
                                    supportFragmentManager.findFragmentById(R.id.frag_myMap) as SupportMapFragment
                                mapFragment.getMapAsync { it ->
                                    googleMap = it
                                    val locationLatLng =
                                        LatLng(location!!.latitude, location!!.longitude)
                                    googleMap.clear()
                                    googleMap.addMarker(
                                        MarkerOptions().position(locationLatLng)
                                            .title("Current Location!").icon(
                                                setIcon(
                                                    this@MainActivity,
                                                    R.mipmap.human
                                                )
                                            )
                                    )
                                    googleMap.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            locationLatLng,
                                            15F
                                        )
                                    )
                                    googleMap.setOnMarkerClickListener(this)
                                }
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Location Not Found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                            .addOnFailureListener {
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
        val locationRequest = LocationRequest.Builder(1000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateDelayMillis(1000L)
            .build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        builder.setAlwaysShow(true)

        val task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())

        task
            .addOnSuccessListener { response ->
                val states = response.locationSettingsStates
                if (states!!.isLocationPresent) {
                    getUserLocation()
                }
            }
            .addOnFailureListener { exception ->
                val statusCode = (exception as ResolvableApiException).statusCode
                if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        exception.startResolutionForResult(this, 100)
                    } catch (sendEx: IntentSender.SendIntentException) {

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

    private fun getCityName(lat: Double, long: Double): String? {
        var cityName: String?
        val geoCoder = Geocoder(this, Locale.getDefault())
        val address = geoCoder.getFromLocation(lat, long, 1)
        cityName = address?.get(0)?.adminArea
        if (cityName == null) {
            cityName = address?.get(0)?.locality
            if (cityName == null) {
                cityName = address?.get(0)?.subAdminArea
            }
        }
        return cityName
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
        for (i in 0 until placesList.size) {
            if (placesList[i].latLng == pressedLatLng) {
                binding.cvClickedPlace.isVisible = true
                Glide.with(binding.ivPlaceIcon).load(placesList[i].imageUrl)
                    .into(findViewById(R.id.iv_placeIcon))
                binding.tvClickedPlaceName.text = placesList[i].name
                binding.tvClickedPlaceAddress.text = "Address: " + placesList[i].address
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
