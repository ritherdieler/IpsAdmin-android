package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.databinding.FragmentSubscriptionFinderBinding
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.locationMapView.SelectableLocationMapViewDialogFragment
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderScreen
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import org.koin.androidx.viewmodel.ext.android.viewModel

class SubscriptionFinderFragment : Fragment() {

    private val viewModel: SubscriptionFinderViewModel by viewModel()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Fine location permission granted
                getCurrentLocation()
            }

            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Only coarse location permission granted
                getCurrentLocation()
            }

            else -> {
                // No location permissions granted
                Toast.makeText(
                    requireContext(),
                    "Se requieren permisos de ubicación para esta función",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.onLocationError()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSubscriptionFinderBinding.inflate(layoutInflater)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val navController = findNavController()

        binding.root.setContent {
            MyTheme {
                SubscriptionFinderScreen(
                    navController = navController,
                    viewModel = viewModel,
                    onShowMapSelector = { geoLocation ->
                        activity?.supportFragmentManager?.let { fm ->
                            // Convert GeoLocation to LatLng before passing
                            geoLocation?.let {
                                val latLng = LatLng(it.latitude, it.longitude)
                                SelectableLocationMapViewDialogFragment(latLng).show(
                                    fm, SelectableLocationMapViewDialogFragment::class.simpleName
                                )
                            } ?: run {
                                SelectableLocationMapViewDialogFragment().show(
                                    fm, SelectableLocationMapViewDialogFragment::class.simpleName
                                )
                            }
                        }
                    },
                    onGetCurrentLocation = {
                        checkLocationPermissionsAndGetLocation()
                    }
                )
            }
        }
        return binding.root
    }

    private fun checkLocationPermissionsAndGetLocation() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Fine location permission already granted
                getCurrentLocation()
            }

            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Coarse location permission already granted
                getCurrentLocation()
            }

            else -> {
                // Request location permissions
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        // Check if location services are enabled
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            Toast.makeText(
                requireContext(),
                "Por favor active los servicios de ubicación",
                Toast.LENGTH_LONG
            ).show()
            viewModel.onLocationError()
            return
        }

        // Get last known location first for faster response
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                viewModel.updateCurrentLocation(location.latitude, location.longitude)
            } else {
                // If last location is null, request a new location update
                requestNewLocation()
            }
        }.addOnFailureListener {
            viewModel.onLocationError()
            Toast.makeText(
                requireContext(),
                "No se pudo obtener la ubicación",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        // This would normally use LocationRequest and LocationCallback for more precise updates
        // For simplicity, we're just reporting the error
        Toast.makeText(
            requireContext(),
            "No se pudo obtener la ubicación actual",
            Toast.LENGTH_LONG
        ).show()
        viewModel.onLocationError()
    }
}
