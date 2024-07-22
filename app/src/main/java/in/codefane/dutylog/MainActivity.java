package in.codefane.dutylog;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

interface ApiServiceMain {
    @GET("start-tracking/")
    Call<TrackingResponse> startTracking(@Header("Authorization") String token, @Header("User-Agent") String userAgent);

    @POST("end-tracking/")
    Call<EndTrackingResponse> endTracking(@Header("Authorization") String token, @Header("User-Agent") String userAgent, @Body EndTrackingRequest request);
}

class TrackingResponse {
    private String session_id;

    public String getSessionId() {
        return session_id;
    }

    public void setSessionId(String sessionId) {
        this.session_id = sessionId;
    }
}

class RetrofitClientMain {
    private static final String BASE_URL = "http://dutylog.codefane.in/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

class EndTrackingRequest {
    private String session_id;
    private List<Sqlitedatabase.LocationData> locations;

    public EndTrackingRequest(String sessionId, List<Sqlitedatabase.LocationData> locationData) {
        this.session_id = sessionId;
        this.locations = locationData;
    }

    public String getSessionId() {
        return session_id;
    }

    public List<Sqlitedatabase.LocationData> getLocationData() {
        return locations;
    }
}

class EndTrackingResponse {
    private String distance; // Assuming the API returns distance as a String

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "MainActivity";
    private Sqlitedatabase dbhelper;
    private LocationUpdateReceiver locationUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbhelper = new Sqlitedatabase(this);

        // Initialize the SeekBar
        SeekBar workSeekBar = findViewById(R.id.slider);

        // Set SeekBar listener
        workSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Handle SeekBar progress change here if needed
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Handle when the user starts touching the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Handle when the user stops touching the SeekBar
                boolean isWorkModeEnabled = seekBar.getProgress() > 0; // Assuming any progress > 0 means Work Mode is on
                if (isWorkModeEnabled) {
                    if (hasLocationPermissions()) {
                        String token = dbhelper.getToken();
                        if (token != null) {
                            startTracking(token);
                        } else {
                            Toast.makeText(MainActivity.this, "No token found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        requestLocationPermissions();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Work mode disabled", Toast.LENGTH_SHORT).show();
                    stopLocationService();
                    handleWorkModeDisable();
                }
            }
        });

        locationUpdateReceiver = new LocationUpdateReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(locationUpdateReceiver,
                new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationUpdateReceiver);
    }

    private void startTracking(String token) {
        Retrofit retrofit = RetrofitClientMain.getClient();
        ApiServiceMain apiService = retrofit.create(ApiServiceMain.class);

        Call<TrackingResponse> call = apiService.startTracking("Token " + token, "ThunderClient");
        call.enqueue(new Callback<TrackingResponse>() {
            @Override
            public void onResponse(Call<TrackingResponse> call, Response<TrackingResponse> response) {
                Log.d(TAG, "API Response Code: " + response.code());
                Log.d(TAG, "API Response Message: " + response.message());
                if (response.isSuccessful() && response.body() != null) {
                    String sessionId = response.body().getSessionId();
                    dbhelper.saveSessionId(sessionId);
                    Toast.makeText(MainActivity.this, "Tracking started", Toast.LENGTH_SHORT).show();
                    startLocationService();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to start tracking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TrackingResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleWorkModeDisable() {
        // Retrieve session ID and location data from database
        String sessionId = dbhelper.getSessionId();
        List<Sqlitedatabase.LocationData> locationDataList = dbhelper.getLocationData(); // You need to implement getLocationData() method in Sqlitedatabase

        // Create the data to send
        EndTrackingRequest endTrackingRequest = new EndTrackingRequest(sessionId, locationDataList);

        // Send POST request to end-tracking endpoint
        Retrofit retrofit = RetrofitClientMain.getClient();
        ApiServiceMain apiService = retrofit.create(ApiServiceMain.class);
        Call<EndTrackingResponse> call = apiService.endTracking("Token " + dbhelper.getToken(), "ThunderClient", endTrackingRequest);

        call.enqueue(new Callback<EndTrackingResponse>() {
            @Override
            public void onResponse(Call<EndTrackingResponse> call, Response<EndTrackingResponse> response) {
                Log.d(TAG, "End Tracking API Response Code: " + response.code());
                Log.d(TAG, "End Tracking API Response Message: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    String distance = response.body().getDistance();

                    // Start ResultActivity and pass the distance
                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("distance", distance);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to end tracking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<EndTrackingResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start location service
                String token = dbhelper.getToken();
                if (token != null) {
                    startTracking(token);
                } else {
                    Toast.makeText(this, "No token found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class LocationUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                double latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0);
                double longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0);
                String coordinates = "Coordinates: " + latitude + ", " + longitude;
                // coordinatesTextView.setText(coordinates); // Commented out for now
                Log.d(TAG, coordinates);
            }
        }
    }
}


