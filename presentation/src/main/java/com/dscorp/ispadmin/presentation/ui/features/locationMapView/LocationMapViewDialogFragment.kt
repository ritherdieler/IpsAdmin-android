package com.dscorp.ispadmin.presentation.ui.features.locationMapView

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.ViewMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng

abstract class LocationMapViewDialogFragment(
    val initialLocation: LatLng? = null,
) : DialogFragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    protected lateinit var googleMap: GoogleMap

    lateinit var binding: ViewMapBinding

    override fun getTheme(): Int = R.style.Theme_IspAdminAndroid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ViewMapBinding.inflate(inflater, container, false)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        afterBindingInflated()
        return binding.root
    }

    protected open fun afterBindingInflated(){

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        val location = LatLng(initialLocation?.latitude ?: 0.0, initialLocation?.longitude ?: 0.0)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
        afterMapReady()
    }

    protected open fun afterMapReady() {

    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
