package com.example.whetherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.example.whetherapp.pojo.ModelClass
import com.example.whetherapp.utilities.ApiUtilities
import com.example.whetherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar!!.hide()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.rlMainLayout.visibility = View.GONE
        getCurrentLocation()

        activityMainBinding.etGetCityName.setOnEditorActionListener { v, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view = this.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true
            } else false
        }


    }





    private fun getCityWeather(cityName: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)
            ?.enqueue(object : Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    if(response.isSuccessful) {
                        setDataOnViews(response.body())
                    }else{
                        Toast.makeText(applicationContext, "Not a Valid Name", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {

                }

            })
    }





    private fun getCurrentLocation() {

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
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(this, "Null Recieved", Toast.LENGTH_SHORT).show()
                    } else {
                        //Fetch Whether Here

                        fetchCurrentWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )
                    }

                }
            } else {
                //Open Settings here
                Toast.makeText(this, "Turn On Location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            //request permissions here
            requestPermission()
        }
    }






    private fun fetchCurrentWeather(latitude: String, longitude: String) {

        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)
            ?.enqueue(object :
                Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    if (response.isSuccessful) {
                        setDataOnViews(response.body())
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(applicationContext, "ERROR", Toast.LENGTH_SHORT).show()
                }

            })

    }





    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {

        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate = sdf.format(Date())
        activityMainBinding.tvDateAndTime.text = currentDate

        activityMainBinding.tvDayMaxTemperature.text =
            "Day " + kelvinToCelcius(body!!.main.temp_max) + "°"
        activityMainBinding.tvDayMinTemperature.text =
            "Night " + kelvinToCelcius(body.main.temp_min) + "°"
        activityMainBinding.tvTemp.text = "" + kelvinToCelcius(body.main.temp) + "°"
        activityMainBinding.tvFeelsLike.text =
            "Feels Alike" + kelvinToCelcius(body.main.feels_like) + "°"
        activityMainBinding.tvWhetherType.text = body.weather[0].main
        activityMainBinding.tvSunrise.text = timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.tvSunset.text = timeStampToLocalDate(body.sys.sunset.toLong())
        activityMainBinding.tvPressure.text = body.main.pressure.toString()
        activityMainBinding.tvHumidity.text = body.main.humidity.toString() + " %"
        activityMainBinding.tvWindSpeed.text = body.wind.speed.toString() + " m/s"

        activityMainBinding.tvTempFarenhite.text =
            "" + ((kelvinToCelcius(body.main.temp)).times(1.8).plus(32).roundToInt())
        activityMainBinding.etGetCityName.setText(body.name)

        updateUI(body.weather[0].id)


    }





    private fun updateUI(id: Int) {
        if (id in 200..232) {
            //thunderstorm
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setStatusBarColor(ContextCompat.getColor(applicationContext, R.color.thunderstorm))
            window.statusBarColor = ResourcesCompat.getColor(getResources() , R.color.thunderstorm , null)
            activityMainBinding.rlToolbar.setBackgroundColor(ResourcesCompat.getColor(getResources() , R.color.thunderstorm , null))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstrom_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstrom_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstrom_bg
            )
            activityMainBinding.ivWhetherBg.setImageResource(R.drawable.thunderstrom_bg)
            activityMainBinding.ivWhetherIcon.setImageResource(R.drawable.thunderstrom)

        } else if (id in 300..321) {
            //drizzle
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setStatusBarColor(ContextCompat.getColor(applicationContext, R.color.drizzle))
            window.statusBarColor = ResourcesCompat.getColor(getResources() , R.color.drizzle , null)
            activityMainBinding.rlToolbar.setBackgroundColor(ResourcesCompat.getColor(getResources() , R.color.drizzle , null))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainBinding.ivWhetherBg.setImageResource(R.drawable.drizzle_bg)
            activityMainBinding.ivWhetherIcon.setImageResource(R.drawable.drizzle)
        } else if (id in 500..531) {
            //Rain
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setStatusBarColor(ContextCompat.getColor(applicationContext, R.color.rain))
            window.statusBarColor = ResourcesCompat.getColor(getResources() , R.color.rain , null)
            activityMainBinding.rlToolbar.setBackgroundColor(ResourcesCompat.getColor(getResources() , R.color.rain , null))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )
            activityMainBinding.ivWhetherBg.setImageResource(R.drawable.rainy_bg)
            activityMainBinding.ivWhetherIcon.setImageResource(R.drawable.rain)

        } else if (id in 600..620) {
            //snow
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setStatusBarColor(ContextCompat.getColor(applicationContext, R.color.snow))
            window.statusBarColor = ResourcesCompat.getColor(getResources() , R.color.snow , null)
            activityMainBinding.rlToolbar.setBackgroundColor(ResourcesCompat.getColor(getResources() , R.color.snow , null))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )
            activityMainBinding.ivWhetherBg.setImageResource(R.drawable.snow_bg)
            activityMainBinding.ivWhetherIcon.setImageResource(R.drawable.snow)
        } else if (id in 701..781) {
            //atmosphere
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setStatusBarColor(ContextCompat.getColor(applicationContext, R.color.atmosphere))
            window.statusBarColor = ResourcesCompat.getColor(getResources() , R.color.atmosphere , null)
            activityMainBinding.rlToolbar.setBackgroundColor(ResourcesCompat.getColor(getResources() , R.color.atmosphere , null))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )
            activityMainBinding.ivWhetherBg.setImageResource(R.drawable.mist_bg)
            activityMainBinding.ivWhetherIcon.setImageResource(R.drawable.mist)
        } else if (id == 800) {
            //clear
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setStatusBarColor(ContextCompat.getColor(applicationContext, R.color.clear))
            window.statusBarColor = ResourcesCompat.getColor(getResources() , R.color.clear , null)
            activityMainBinding.rlToolbar.setBackgroundColor(ResourcesCompat.getColor(getResources() , R.color.clear , null))

            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )
            activityMainBinding.ivWhetherBg.setImageResource(R.drawable.clear_bg)
            activityMainBinding.ivWhetherIcon.setImageResource(R.drawable.clear)
        } else {
            //clouds
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setStatusBarColor(ContextCompat.getColor(applicationContext, R.color.clouds))
            window.statusBarColor = ResourcesCompat.getColor(getResources() , R.color.clouds , null)
            activityMainBinding.rlToolbar.setBackgroundColor(ResourcesCompat.getColor(getResources() , R.color.clouds , null))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )
            activityMainBinding.ivWhetherBg.setImageResource(R.drawable.clouds_bg)
            activityMainBinding.ivWhetherIcon.setImageResource(R.drawable.clouds)
        }

        activityMainBinding.pbLoading.visibility = View.GONE
        activityMainBinding.rlMainLayout.visibility = View.VISIBLE

    }





    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timeStamp: Long): String {
        val localTime = timeStamp.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return localTime.toString()
    }






    private fun kelvinToCelcius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }






    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }






    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }






    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val API_KEY = "f89a87c4ca53a9efa78d55b4e98f106b"
    }






    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }






    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "GRANTED", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "DENIED", Toast.LENGTH_SHORT).show()
            }
        }
    }






}