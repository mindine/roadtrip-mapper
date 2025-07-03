

package com.example.roadtripmapper;
import android.util.Log;
import android.location.Address;
import android.location.Geocoder;
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.roadtripmapper.databinding.ActivityMapsBinding;
import java.util.ArrayList;
import android.widget.Button;

import android.graphics.Color;
import com.google.android.gms.maps.model.PolylineOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;



public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private List<LatLng> pinList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        EditText searchLocation = findViewById(R.id.searchLocation);

        searchLocation.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String cityName = searchLocation.getText().toString();
                    if (!cityName.isEmpty()) {
                        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(cityName, 1);
                            if (!addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                                mMap.addMarker(new MarkerOptions().position(latLng).title(cityName));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));

                                // Store the pin
                                pinList.add(latLng);

                            } else {
                                Toast.makeText(MapsActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(MapsActivity.this, "Error finding location", Toast.LENGTH_SHORT).show();
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        Button btnRoute = findViewById(R.id.btnRoute);

        btnRoute.setOnClickListener(view -> {
            if (pinList.size() >= 2) {
                drawRouteBetweenPins(pinList);
            } else {
                Toast.makeText(this, "Add at least 2 cities to generate a route", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void drawRouteBetweenPins(List<LatLng> pinList) {
        if (pinList.size() < 2) return;

        String origin = pinList.get(0).latitude + "," + pinList.get(0).longitude;
        String destination = pinList.get(pinList.size() - 1).latitude + "," + pinList.get(pinList.size() - 1).longitude;

        StringBuilder waypoints = new StringBuilder();
        for (int i = 1; i < pinList.size() - 1; i++) {
            LatLng point = pinList.get(i);
            waypoints.append(point.latitude).append(",").append(point.longitude).append("|");
        }

        String waypointParam = waypoints.length() > 0 ? "&waypoints=" + waypoints.toString() : "";

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin +
                "&destination=" + destination +
                waypointParam +
                "&key=" + getString(R.string.google_maps_key);

        Log.d("ROUTE_URL", url);

        new Thread(() -> {
            try {
                URL directionUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) directionUrl.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder responseBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }

                String response = responseBuilder.toString();
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray routes = jsonResponse.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String points = overviewPolyline.getString("points");

                    List<LatLng> decodedPoints = decodePolyline(points);

                    runOnUiThread(() -> {
                        PolylineOptions options = new PolylineOptions().addAll(decodedPoints).width(10).color(Color.BLUE);
                        mMap.addPolyline(options);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }

        return poly;
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
        mMap = googleMap;

        // Add a marker in Austin and move the camera
        LatLng austin = new LatLng(30.2672, -97.7431);
        mMap.addMarker(new MarkerOptions().position(austin).title("Marker in Austin, TX"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(austin, 10));
    }
}