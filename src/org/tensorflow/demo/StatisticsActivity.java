package org.tensorflow.demo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NoCache;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class StatisticsActivity extends AppCompatActivity {

  private BottomNavigationView bottomNavigationView;
  private CameraRoomDatabase cameraDb;

  private TextView totalInAreaTextView;
  private TextView local7DaysTextView;
  private TextView local28DaysTextView;
  private TextView totalByUserTextView;
  private TextView globalTodayTextView;
  private TextView global28DaysTextView;
  private TextView globalTotalTextView;

  private List<HashMap<Date, Integer>> localCamerasPerDays;
  private List<HashMap<Date, Integer>> globalCamerasPerDays;

  private SharedPreferences sharedPreferences;

  private double latMin;
  private double latMax;
  private double lonMin;
  private double lonMax;

  private SynchronizedCameraRepository synchronizedCameraRepository;
  private CameraRepository cameraRepository;

  private int globalTodayAmount;
  private int global28DaysAmount;
  private int globalTotalAmount;

  private int local7DaysAmount;
  private int totalLocal28Days;
  private int totalLocal;
  private int totalByUser;

  private SimpleDateFormat timestampIso8601SecondsAccuracy;


  private LocalBroadcastManager localBroadcastManager;
  private IntentFilter intentFilter;
  private BroadcastReceiver br;

  private int readStoragePermission;
  private int writeStoragePermission;




  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_statistics);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());
    cameraRepository = new CameraRepository(getApplication());

    totalInAreaTextView = findViewById(R.id.total_cameras_statistics);

    local7DaysTextView = findViewById(R.id.added_past_7_days_statistics);
    local28DaysTextView = findViewById(R.id.added_past_28_days_statistics);
    totalByUserTextView = findViewById(R.id.added_by_user_statistics);

    globalTodayTextView = findViewById(R.id.global_today_statistics);
    global28DaysTextView = findViewById(R.id.global_28days_statistics);
    globalTotalTextView = findViewById(R.id.global_total_statistics);

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    refreshSharedPreferencesObject();
    String homeZone = sharedPreferences.getString("area", null);

    String[] splitBorders = homeZone.split(",");

    latMin = Double.valueOf(splitBorders[0]);
    latMax = Double.valueOf(splitBorders[1]);
    lonMin = Double.valueOf(splitBorders[2]);
    lonMax = Double.valueOf(splitBorders[3]);

    long currentTime = System.currentTimeMillis();
    Date currentDate = new Date(currentTime);

    Calendar cal = Calendar.getInstance();
    cal.setTime(currentDate);

    // cal.add(Calendar.MONTH, -12);

    cal.add(Calendar.DATE, -7);
    Date sevenDaysBeforeToday = cal.getTime();

    // data from -7 days until today
    local7DaysAmount = StatisticsUtils.getTotalCamerasInTimeframeFromDb(sevenDaysBeforeToday, currentDate, synchronizedCameraRepository);

    // only subtract 21 here because we've subtracted 7 earlier
    cal.add(Calendar.DATE, -21);
    Date twentyEightDaysBeforeToday = cal.getTime();

    totalLocal28Days = StatisticsUtils.getTotalCamerasInTimeframeFromDb(twentyEightDaysBeforeToday, currentDate, synchronizedCameraRepository);

    totalLocal = StatisticsUtils.totalCamerasInDb(synchronizedCameraRepository);
    totalByUser = StatisticsUtils.totalCamerasAddedByUser(cameraRepository);

    totalInAreaTextView.setText(String.valueOf(totalLocal));
    local7DaysTextView.setText(String.valueOf(local7DaysAmount));
    local28DaysTextView.setText(String.valueOf(totalLocal28Days));
    totalByUserTextView.setText(String.valueOf(totalByUser));

    localBroadcastManager = LocalBroadcastManager.getInstance(StatisticsActivity.this);

    refreshSharedPreferencesObject();
    String baseURL = sharedPreferences.getString("synchronizationURL", null);
    queryServerForStatistics(baseURL, "global", "2018-01-01", "2019-01-01");

    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(StatisticsActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            Intent cameraIntent = new Intent(StatisticsActivity.this, DetectorActivity.class);
            startActivity(cameraIntent);
            return true;

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(StatisticsActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(StatisticsActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }
        return false;
      }
    });

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_stats).setChecked(true);
  }


  @Override
  protected void onResume() {
    super.onResume();

    br = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        refreshSharedPreferencesObject();
        String baseURL = sharedPreferences.getString("synchronizationURL", null);
        queryServerForStatistics(baseURL, "global", "2018-01-01", "2019-01-01");
      }
    };

    intentFilter = new IntentFilter("org.unsurv.API_KEY_CHANGED");

    localBroadcastManager.registerReceiver(br, intentFilter);

    readStoragePermission = ContextCompat.checkSelfPermission(StatisticsActivity.this,
            Manifest.permission.READ_EXTERNAL_STORAGE);
    writeStoragePermission = ContextCompat.checkSelfPermission(StatisticsActivity.this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);



    List<String> permissionList = new ArrayList<>();

    if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }


    String[] neededPermissions = permissionList.toArray(new String[0]);

    if (!permissionList.isEmpty()) {
      ActivityCompat.requestPermissions(StatisticsActivity.this, neededPermissions, 3);
    }

    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    localBroadcastManager.unregisterReceiver(br);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //return super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.actionbar, menu);
    return true;
  }



  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case R.id.action_settings:
        Intent settingsIntent = new Intent(StatisticsActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

        return true;

      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }


  void queryServerForStatistics(String baseURL, String area, String startDate, String endDate){

    final String TAG = "StatisticsUtils";
    //TODO check api for negative values in left right top bottom see if still correct

    refreshSharedPreferencesObject();
    SynchronizationUtils.refreshApiKeyIfExpired(sharedPreferences, getApplicationContext());

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    String url = baseURL + "statistics/?area=" + area;

    if (startDate != null){
     url += "&start=" + startDate;
    }

    if (endDate != null){
      url += "&end=" + endDate;
    }

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                JSONObject JSONFromServer;

                try {

                  JSONFromServer = response.getJSONObject("global");

                  globalTodayAmount = JSONFromServer.getInt("global_today");
                  global28DaysAmount = JSONFromServer.getInt("global_28days");
                  globalTotalAmount = JSONFromServer.getInt("global_total");



                } catch (Exception e) {
                  Log.i(TAG, "onResponse: " + e.toString());

                }

              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // TODO: Handle Errors
      }
    }
    );

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.add(jsonObjectRequest);

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {

        redrawGlobalTextViews();

      }
    });

  }

  /**
   * updates all TextViews which rely on outside data
   */

  void redrawGlobalTextViews(){

    globalTodayTextView.setText(String.valueOf(globalTodayAmount));
    global28DaysTextView.setText(String.valueOf(global28DaysAmount));
    globalTotalTextView.setText(String.valueOf(globalTotalAmount));

  }

  private void refreshSharedPreferencesObject() {

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

  }
}