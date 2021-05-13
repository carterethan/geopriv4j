package com.geopriv4j;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import geopriv4j.LaplaceAlgorithm;
import geopriv4j.utils.Mapper;

//import geopriv4j.*;
//import geopriv4j.VHCAlgorithm;


public class MapsActivityLaplace extends FragmentActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationClient;

    //this is used to get the current latitude and longitude
    private double currentLatitude = 0.0, currentLongitude = 0.0;
    //if this is set to false then there won't be any location updates
    private boolean requestingLocationUpdates = true;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;


    //specify the sample rate to generate locations
    public static int SAMPLERATE = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // reuqest for permission
            int locationRequestCode = 1000;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    locationRequestCode);

        } else {
            // already permission granted
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            locationRequest = LocationRequest.create();

            //set to low power mode
            locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

            //this delays the location sampling
            locationRequest.setInterval(SAMPLERATE * 1000);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {

                    Log.d("geopriv4j", "onLocationResult: ");
                    if (locationResult == null) {
                        return;
                    }
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();

                        Log.d("geopriv4j", String.format(Locale.US, "%s -- %s", currentLatitude, currentLongitude));

                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);

                        assert mapFragment != null;
                        mapFragment.getMapAsync(MapsActivityLaplace.this);

                    }

                }
            };
        }
    }


    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {

            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        //displaying actual location on the map
        LatLng loc = new LatLng(currentLatitude, currentLongitude);
        googleMap.addMarker(new MarkerOptions().position(loc).title("actual location").icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 14));

        //Convert current com.google.android.gms.maps.model.LatLng object to geopriv4j.utils.LatLng object
        geopriv4j.utils.LatLng location = new geopriv4j.utils.LatLng(loc.latitude, loc.longitude);

        /*
         * Laplace Algorithm example
         */

        //specify the offset in meters
        double e = 0.001;

        //generating the noise using the LaplaceAlgorithm from geopriv4j package
        geopriv4j.utils.LatLng generated_location = new LaplaceAlgorithm(e).generate(location);


        //Convert generated geopriv4j.utils.LatLng object to com.google.android.gms.maps.model.LatLng obejct
        LatLng laPlaceLatLng = new LatLng(generated_location.latitude, generated_location.longitude);

        //displaying the location on the map
        googleMap.addMarker(new MarkerOptions().position(laPlaceLatLng).title("Laplace location").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(laPlaceLatLng, 14));
    }
}