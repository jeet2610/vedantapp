package com.jams.vedantattendancesystem

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.internal.ActivityLifecycleObserver.of
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.jams.vedantattendancesystem.model.punchInModel
import com.jams.vedantattendancesystem.viewmodel.punchInViewModel
import java.util.jar.Manifest


class PunchInFragment : Fragment() {

    val pic_id = 123
    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var bitmap: Bitmap
    lateinit var locationEditext: EditText
    lateinit var Camera: Button
    lateinit var PunchInNowBtn:android.widget.Button
    lateinit var imageView: ImageView
    lateinit var location: Location
    var describeContents = 0.0
    lateinit var addresses: List<Address>
    lateinit var geocoder: Geocoder
    lateinit var  Location  : String
    lateinit var  viewmodel : punchInViewModel


    @RequiresApi(api = Build.VERSION_CODES.P)
    private val CAMERA_REQUEST = 1888
    private val MY_CAMERA_PERMISSION_CODE = 100
    private val MY_LOCATION_PERMISSION_CODE = 1

    lateinit var locationRequest: LocationRequest
    lateinit var punchinbtn : Button
    lateinit var punchInModel: punchInModel




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

            checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION,MY_LOCATION_PERMISSION_CODE)
            viewmodel = activity?.let {
                ViewModelProvider(requireActivity()).get(punchInViewModel::class.java)
            } ?: throw Exception("Activity is null")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_punch_in, container, false)
        // Inflate the layout for this fragment

        Camera = view.findViewById<Button>(R.id.CameraBtn)
        imageView = view.findViewById<ImageView>(R.id.imageView)

        locationEditext = view.findViewById<EditText>(R.id.LocationEditText)
        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        punchinbtn = view.findViewById(R.id.PunchInNowBtn)





        punchinbtn.setOnClickListener {

            val  user_id = FirebaseAuth.getInstance().currentUser!!.uid

            viewmodel.createPunch(punchInModel(null,"IN",user_id, location = location.toString()))

        }



        getCurrentLocation();


        return view
    }

    private fun getCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity()?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } == PackageManager.PERMISSION_GRANTED
            ) {
                if (isGPSEnabled()) {
                    LocationServices.getFusedLocationProviderClient(requireActivity())
                        .requestLocationUpdates(locationRequest, object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                super.onLocationResult(locationResult)
                                LocationServices.getFusedLocationProviderClient(requireActivity())
                                    .removeLocationUpdates(this)
                                if (locationResult.getLocations().size > 0) {
                                    val index: Int = locationResult.getLocations().size - 1
                                    val latitude: Double =
                                        locationResult.getLocations().get(index).getLatitude()
                                    val longitude: Double =
                                        locationResult.getLocations().get(index).getLongitude()
                                    android.util.Log.d(
                                        "map",
                                        "onLocationResult: " + locationResult.getLocations()
                                    )
                                    Location = "https://maps.google.com/?q=$latitude,$longitude"
                                    Log.d("locatio",""+Location)
                                }
                            }
                        }, Looper.getMainLooper())
                } else {
                    turnOnGPS()
                }
            } else {
                requestPermissions(
                    arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            }
        }
    }

    fun checkPermission(permission: String, requestCode: Int) {
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {

                // Requesting the permission
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
            } else {
                Toast.makeText(activity, "Permission already granted", Toast.LENGTH_SHORT).show()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                activity.startActivityFromFragment(this@PuchInFragment, cameraIntent, CAMERA_REQUEST)
            }
        }
        if (requestCode == MY_LOCATION_PERMISSION_CODE) {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {

                // Requesting the permission
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
            } else {
                Log.d("Allowed", "checkPermission: location")
            }
        }
    }



    private fun isGPSEnabled(): Boolean {
        var locationManager: LocationManager? = null
        var isEnabled = false
        if (locationManager == null) {
            locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        }
        isEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

        return isEnabled
    }


    private fun turnOnGPS() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(Activity())
                .checkLocationSettings(builder.build())
        result.addOnCompleteListener(OnCompleteListener<LocationSettingsResponse?> { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                Toast.makeText(activity, "GPS is already tured on", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: ApiException) {
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvableApiException = e as ResolvableApiException
                        resolvableApiException.startResolutionForResult(requireActivity(), 2)
                    } catch (ex: SendIntentException) {
                        ex.printStackTrace()
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {}
                }
            }
        })
    }
}