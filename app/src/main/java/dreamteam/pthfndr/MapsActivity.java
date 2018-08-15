package dreamteam.pthfndr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import dreamteam.pthfndr.models.MLocation;
import dreamteam.pthfndr.models.Path;
import dreamteam.pthfndr.models.Trip;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    boolean isActive = false;
    dreamteam.pthfndr.models.User currentUser;
    double latitude = 0;
    double longitude = 0;
    Trip trip = new Trip(Calendar.getInstance().getTime());
    long time = 0;
    private GoogleMap mMap;
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (isActive) {
                double longitudeNew = location.getLongitude();
                double latitudeNew = location.getLatitude();
                Polyline l = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(latitude, longitude), new LatLng(latitudeNew, longitudeNew))
                        .width(5)
                        .color(Color.DKGRAY)
                );
                trip.paths.add(new Path(new MLocation(location.getSpeed(), new LatLng(latitude, longitude)), new MLocation(location.getSpeed(), new LatLng(latitudeNew, longitudeNew)), l, Color.DKGRAY, (int) (System.currentTimeMillis() - time) / 1000));
                longitude = location.getLongitude();
                latitude = location.getLatitude();
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };
    private boolean mLocationPermissionGranted;

    private Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final DatabaseReference fDB = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());
        fDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentUser = snapshot.getValue(dreamteam.pthfndr.models.User.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        for (Trip t: currentUser.getTrips()) {
            for(Path p : t.getPaths()){
                Polyline l = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(p.getEndLocation().getPlace().latitude,p.getEndLocation().getPlace().longitude),new LatLng(p.getStartLocation().getPlace().latitude,p.getStartLocation().getPlace().longitude) )
                        .width(5)
                        .color(Color.DKGRAY)
                );
            }
        }

        getLocationPermission();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setBuildingsEnabled(true);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

        time = System.currentTimeMillis();
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                }
            }
        }
    }

    public void signOut(View view) {
        Intent i = new Intent(this, ProfileActivity.class);
        startActivity(i);
    }

    public void onLocationChanged(Location location) {
        mLastLocation = location;
        Log.d("aaaaaaaa===>", "" + location.getLatitude() + "\n" + location.getLongitude());
        mMap.clear();
        final LatLng loc = new LatLng(location.getLongitude(), location.getLongitude());

        Marker ham = mMap.addMarker(new MarkerOptions().position(loc).title("This is Me"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
    }

    public void manageTrip(View view){
        isActive = isActive ? false : true;

        Button b = findViewById(R.id.TripButton);
        if (isActive) {
            if(trip == null){
                trip = new Trip();
            }
            b.setText(R.string.endTrip);
        }else{
            trip.end_trip();
            currentUser.add_trip(trip);
            trip = new Trip();
            b.setText(R.string.startTrip);
            updateUser();
        }
    }

    private void updateUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final DatabaseReference fDB = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());
        fDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Object> userValues = currentUser.toMap();
                Map<String, Object> userUpdates = new HashMap<>();
                userUpdates.put("/" + "", userValues);
                fDB.updateChildren(userUpdates);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}