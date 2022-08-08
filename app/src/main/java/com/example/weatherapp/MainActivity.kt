package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.*
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.bhargavms.dotloader.DotLoader
import com.bumptech.glide.Glide
import com.example.weatherapp.model.WeatherData
import com.example.weatherapp.retrofitmodel.WeatherServices
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {
    lateinit var city:TextView
    lateinit var desc:TextView
    lateinit var temp:TextView
    lateinit var img:ImageView
    lateinit var message:TextView
    lateinit var dots:DotLoader
    lateinit var box:LinearLayoutCompat
    var setup:Boolean=false

    val key="e386d13b7eb04bc7834160138220407"
    var isInternetEnabled=false
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    var url:String="http://api.weatherapi.com/"
    var retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    var service: WeatherServices = retrofit.create(WeatherServices::class.java)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        city=findViewById(R.id.city)
        desc=findViewById(R.id.desc)
        temp=findViewById(R.id.temp)
        img=findViewById(R.id.img)
        message=findViewById(R.id.message)
        dots=findViewById(R.id.dots)
        box=findViewById(R.id.box)

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        val connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            isInternetEnabled=true
            if(!setup){
                runOnUiThread{
                    setUpDots("")
                }
                if(isLocationEnabled() && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                    requestNewLocationData()
                    setup=true
                }
                else if(!isLocationEnabled()){
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent,102)
                }
                else if((ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED))){
                    askPermission()
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            isInternetEnabled=false
        }
    }

    fun setUpLayout(){
        dots.visibility=View.INVISIBLE
        message.visibility=View.INVISIBLE
        box.visibility= View.VISIBLE
    }

    fun setUpDots(notify:String){
        message.text=notify
        box.visibility= View.INVISIBLE
        dots.visibility=View.VISIBLE
        message.visibility=View.VISIBLE
    }

    private fun isLocationEnabled():Boolean{
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            if(isInternetEnabled){
                var call: Call<WeatherData> = service.getWeather(key,"${mLastLocation!!.latitude},${mLastLocation.longitude}")
                call.enqueue(object :Callback<WeatherData>{
                    override fun onResponse(
                        call: Call<WeatherData>,
                        response: Response<WeatherData>
                    ) {
                        if(response.isSuccessful){
                            var info: WeatherData? = response.body()
                            Glide.with(this@MainActivity)
                                .load("http:${info?.current?.condition?.icon}")
                                .placeholder(R.drawable.place)
                                .error(R.drawable.place)
                                .into(img)

                            city.text=info?.location?.name
                            desc.text=info?.current?.condition?.text
                            temp.text="${info?.current?.temp_c}℃"
                            setup=true
                            setUpLayout()
                        }
                    }

                    override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                        Toast.makeText(this@MainActivity,"API call failed",Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private fun askPermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        if(isInternetEnabled){
                            requestNewLocationData()
                        }
                    }
                    if(multiplePermissionsReport.deniedPermissionResponses.isNotEmpty()){
                        var alertdialog= AlertDialog.Builder(this@MainActivity)

                        alertdialog.setTitle("Permission Required To Access Location")

                        alertdialog.setCancelable(false)

                        alertdialog.setMessage("Please go to settings and give permission.")

                        alertdialog.setPositiveButton("Go to Settings"){
                                _,_->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.parse("package:$packageName")
                            startActivityForResult(intent,101)
                        }

                        alertdialog.show()
                    }
                    if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                        var alertdialog= AlertDialog.Builder(this@MainActivity)

                        alertdialog.setTitle("Permission Required To Access Location")

                        alertdialog.setCancelable(false)

                        alertdialog.setMessage("Please go to settings and give permission.")

                        alertdialog.setPositiveButton("Go to Settings"){
                                _,_->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.parse("package:$packageName")
                            startActivityForResult(intent,101)
                        }

                        alertdialog.show()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest>,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).withErrorListener {
                Toast.makeText(applicationContext, "Error occurred! ", Toast.LENGTH_SHORT).show()
            }
            .onSameThread().check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==102){
            if (isLocationEnabled()) {
                if( ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                    if(isInternetEnabled){
                        requestNewLocationData()
                    }
                }
                else{
                    askPermission()
                }
            }
            else{
                var alertdialog= AlertDialog.Builder(this@MainActivity)

                alertdialog.setTitle("Location Required")

                alertdialog.setCancelable(false)

                alertdialog.setMessage("Please go to settings and on location provider")

                alertdialog.setPositiveButton("Go to Settings"){
                        _,_->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent,102)
                }
                alertdialog.show()
            }
        }
        if(requestCode==101){
            if( ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                if(isInternetEnabled){
                    requestNewLocationData()
                }
            }
            else{
                askPermission()
            }
        }
    }
}