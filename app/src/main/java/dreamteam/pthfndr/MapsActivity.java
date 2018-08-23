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
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Calendar;

import dreamteam.pthfndr.models.FirebaseAccessor;
import dreamteam.pthfndr.models.MLocation;
import dreamteam.pthfndr.models.MPolyLine;
import dreamteam.pthfndr.models.Path;
import dreamteam.pthfndr.models.Trip;
import dreamteam.pthfndr.models.User;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static MapsActivity thisRef;
    boolean isActive = false;
    double latitude = 0;
    double longitude = 0;
    Location cLoc;
    Trip trip = new Trip(Calendar.getInstance().getTime());
    long time = 0;
    private User currentUser;
    private GoogleMap mMap;
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (isActive) {

                double longitudeNew = location.getLongitude();
                double latitudeNew = location.getLatitude();

                float currentSpeed = location.getSpeed() * 2.236936F;
                int width = checkPaths(5, longitudeNew, latitudeNew);
                Polyline l = null;
                MPolyLine mp = null;
                if (currentSpeed <= 10) {
                    mp = new MPolyLine(longitude, latitude, longitudeNew, latitudeNew, Color.argb(255, 0, 255, 0), width);
                } else if (currentSpeed >= 11 && currentSpeed <= 30) {
                    mp = new MPolyLine(longitude, latitude, longitudeNew, latitudeNew, Color.argb(255, 128, 255, 0), width);
                } else if (currentSpeed >= 31 && currentSpeed <= 60) {
                    mp = new MPolyLine(longitude, latitude, longitudeNew, latitudeNew, Color.argb(255, 255, 255, 0), width);
                } else if (currentSpeed >= 61 && currentSpeed <= 90) {
                    mp = new MPolyLine(longitude, latitude, longitudeNew, latitudeNew, Color.argb(255, 255, 163, 0), width);
                } else if (currentSpeed >= 91) {
                    mp = new MPolyLine(longitude, latitude, longitudeNew, latitudeNew, Color.argb(255, 255, 0, 0), width);
                } else {
                    mp = new MPolyLine(longitude, latitude, longitudeNew, latitudeNew, Color.argb(255, 0, 255, 0), width);

                }
                l = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(mp.getStartingLatitude(), mp.getStartingLongitude()), new LatLng(mp.getEndingLatitude(), mp.getEndingLongitude()))
                        .width(mp.getWidth())
                        .color(mp.getColor())
                );
                trip.paths.add(new Path(new MLocation(cLoc.getSpeed(), latitude, longitude), new MLocation(location.getSpeed(), latitudeNew, longitudeNew), mp, Color.DKGRAY, (int) (System.currentTimeMillis() - time) / 1000));
                cLoc = location;
            }
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        private int checkPaths(int i, double longitudeNew, double latitudeNew) {
            for (Trip t : currentUser.getTrips()) {
                for (Path p : t.getPaths()) {
                    if (p.getEndLocation().getLatitude() >= (latitudeNew - .0004) && p.getEndLocation().getLatitude() <= (latitudeNew + .0004) ||
                            p.getStartLocation().getLatitude() >= (latitudeNew - .0004) && p.getStartLocation().getLatitude() <= (latitudeNew + .0004)) {
                        if (p.getEndLocation().getLongitude() >= (longitudeNew - .0004) && p.getEndLocation().getLongitude() <= (longitudeNew + .0004) ||
                                p.getStartLocation().getLongitude() >= (longitudeNew - .0004) && p.getStartLocation().getLongitude() <= (longitudeNew + .0004)) {
                            i += 5;
                            i = i > 100 ? 100 : i;
                        }
                    }
                }
            }
            return i;
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
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String[] keys = getIntent().getExtras().keySet().toArray(new String[1]);
        currentUser = getIntent().getExtras().getParcelable(keys[0]);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        thisRef = this;
        currentUser = FirebaseAccessor.getUserModel();

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setActionBar(toolbar);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    // set item as selected to persist highlight
                    menuItem.setChecked(true);
                    // close drawer when item is tapped
                    mDrawerLayout.closeDrawers();

                    // Add code here to update the UI based on the item selected
                    // For example, swap UI fragments here

                    return true;
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (currentUser != null) {
            for (Trip t : currentUser.getTrips()) {
                for (Path p : t.getPaths()) {
                    Polyline l = mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(p.getEndLocation().getLatitude(), p.getEndLocation().getLongitude()), new LatLng(p.getStartLocation().getLatitude(), p.getStartLocation().getLongitude()))
                            .color(p.getColor())
                    );
                    if (p.getPl() != null) {
                        l.setWidth(p.getPl().getWidth());
                    } else {
                        l.setWidth(5);
                    }
                }
            }
        } else {
            // if the map is ready but we still don't have the user model
            // manually get it
            if (getIntent().getExtras() != null) {
                currentUser = getIntent().getExtras().getParcelable("user");
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
        cLoc = location;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
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
        User user = getIntent().getExtras().getParcelable("user");
        i.putExtra("user", user);
        startActivity(i);
    }

    public void manageTrip(View view) {
        isActive = !isActive;

        Button b = findViewById(R.id.TripButton);
        if (isActive) {
            if (trip == null) {
                trip = new Trip();
            }
            b.setText(R.string.endTrip);
        } else {
            mMap.clear();
            for (Trip t : currentUser.getTrips()) {
                for (Path p : t.getPaths()) {
                    Polyline l = mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(p.getEndLocation().getLatitude(), p.getEndLocation().getLongitude()), new LatLng(p.getStartLocation().getLatitude(), p.getStartLocation().getLongitude()))
                    );
                    if (p.getPl() != null) {
                        l.setWidth(p.getPl().getWidth());
                        l.setColor(p.getPl().getColor());
                    } else {
                        l.setWidth(5);
                        l.setColor(Color.BLACK);
                    }
                }
            }
            trip.endTrip();
            currentUser.addTrip(trip);
            trip = new Trip();
            b.setText(R.string.startTrip);
            updateUser();
        }
    }

    private void updateUser() {
        if (FirebaseAccessor.updateUserModel(currentUser)) {
            Toast.makeText(thisRef, "Trip Saved!", Toast.LENGTH_LONG).show();
        }
        mMap.clear();
        for (Trip t : currentUser.getTrips()) {
            for (Path p : t.getPaths()) {
                Polyline l = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(p.getEndLocation().getLatitude(), p.getEndLocation().getLongitude()), new LatLng(p.getStartLocation().getLatitude(), p.getStartLocation().getLongitude()))
                        .color(p.getPl().getColor())
                        .width(p.getPl().getWidth())
                );
                if (p.getPl() != null) {
                    l.setWidth(p.getPl().getWidth());
                } else {
                    l.setWidth(5);
                }
            }
        }
    }
}