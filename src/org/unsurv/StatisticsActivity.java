package org.unsurv;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


/**
 * This is one of the 4 main activities. It displays statistics from your homezone as well as
 * global stats.
 */
public class StatisticsActivity extends AppCompatActivity {

  private BottomNavigationView bottomNavigationView;

  private TextView totalInAreaTextView;
  private TextView totalInAreaInfoTextView;
  private TextView local7DaysTextView;
  private TextView local28DaysTextView;
  private TextView totalByUserTextView;
  private TextView totalByUserInfoTextView;
  private TextView globalTodayTextView;
  private TextView global28DaysTextView;
  private TextView globalTotalTextView;

  private SharedPreferences sharedPreferences;

  private SynchronizedCameraRepository synchronizedCameraRepository;
  private CameraRepository cameraRepository;

  private int globalTodayAmount;
  private int global28DaysAmount;
  private int globalTotalAmount;

  int local7DaysAmount;
  int totalLocal28Days;
  int totalLocal;
  int totalByUser;

  private LocalBroadcastManager localBroadcastManager;
  IntentFilter intentFilter;
  private BroadcastReceiver br;

  int readStoragePermission;
  int writeStoragePermission;

  private SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

  private String baseURL;
  private String today;

  Context context;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_statistics);

    context = this;

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());
    cameraRepository = new CameraRepository(getApplication());

    totalInAreaTextView = findViewById(R.id.total_cameras_in_area_statistics);
    totalInAreaInfoTextView = findViewById(R.id.total_cameras_in_area_info_statistics);

    local7DaysTextView = findViewById(R.id.added_past_7_days_statistics);
    local28DaysTextView = findViewById(R.id.added_past_28_days_statistics);
    totalByUserTextView = findViewById(R.id.added_by_user_statistics);
    totalByUserInfoTextView = findViewById(R.id.added_by_user_info_statistics);

    globalTodayTextView = findViewById(R.id.global_today_statistics);
    global28DaysTextView = findViewById(R.id.global_28days_statistics);
    globalTotalTextView = findViewById(R.id.global_total_statistics);

    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    refreshSharedPreferencesObject();
    updateLocalTextViews();

    long currentTime = System.currentTimeMillis();

    localBroadcastManager = LocalBroadcastManager.getInstance(StatisticsActivity.this);


    baseURL = sharedPreferences.getString("synchronizationUrl", null);
    today = timestampIso8601.format(new Date(currentTime));

    queryServerForStatistics(baseURL, "global", "2018-01-01", today);

    androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
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
            if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
              Intent manualCaptureIntent = new Intent(StatisticsActivity.this, ManualCaptureActivity.class);
              startActivity(manualCaptureIntent);
              return true;
            } else {
              Intent cameraIntent = new Intent(StatisticsActivity.this, DetectorActivity.class);
              startActivity(cameraIntent);
              return true;
            }


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

    long currentTime = System.currentTimeMillis();
    today = timestampIso8601.format(new Date(currentTime));

    br = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        refreshSharedPreferencesObject();
        String baseURL = sharedPreferences.getString("synchronizationUrl", null);
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

    updateLocalTextViews();

    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);

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

    MenuItem trainingItem = menu.findItem(R.id.action_training);
    trainingItem.setVisible(false);

    return true;
  }



  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case R.id.action_settings:
        Intent settingsIntent = new Intent(StatisticsActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

        return true;

      case R.id.action_refresh:

        long currentTime = System.currentTimeMillis();

        today = timestampIso8601.format(new Date(currentTime));

        queryServerForStatistics(baseURL, "global", "2018-01-01", today);

        updateGlobalTextViews();
        updateLocalTextViews();

        return true;

      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }


  /**
   * Queries server fpr statistics
   * @param baseURL base url https://api.unsurv.org/
   * @param area area string "global" or "latmin,latmax,lonmin,lonmax"
   * @param startDate yyyy-mm-dd if needed
   * @param endDate yyyy-mm-dd if needed
   */
  void queryServerForStatistics(String baseURL, String area, @Nullable String startDate,@Nullable String endDate){

    final String TAG = "StatisticsUtils, queryServer";
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
    ){
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        String apiKey = sharedPreferences.getString("apiKey", null);
        headers.put("Authorization", apiKey);
        headers.put("Content-Type", "application/json");

        return headers;
      }
    };

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.add(jsonObjectRequest);

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {

        updateGlobalTextViews();

      }
    });

  }

  /**
   * Updates all TextViews which rely on outside data. Outside data should already be queried
   */

  void updateGlobalTextViews(){

    if (globalTodayAmount > 1000){
      globalTodayTextView.setText(abbreviateLargeNumber(globalTodayAmount));
    } else {
      globalTodayTextView.setText(String.valueOf(globalTodayAmount));
    }

    if (global28DaysAmount > 1000){
      global28DaysTextView.setText(abbreviateLargeNumber(global28DaysAmount));
    } else {
      global28DaysTextView.setText(String.valueOf(global28DaysAmount));
    }

    if (globalTotalAmount > 1000){
      globalTotalTextView.setText(abbreviateLargeNumber(globalTotalAmount));
    } else {
      globalTotalTextView.setText(String.valueOf(globalTotalAmount));
    }

  }

  /**
   * updates TextViews that represent local data from the device only
   */
  void updateLocalTextViews(){

    long currentTime = System.currentTimeMillis();
    Date currentDate = new Date(currentTime);

    Calendar cal = Calendar.getInstance();
    cal.setTime(currentDate);


    cal.add(Calendar.DATE, -7);
    Date sevenDaysBeforeToday = cal.getTime();

    // data from -7 days until today
    local7DaysAmount = StatisticsUtils.getTotalCamerasAddedInTimeframeFromDb(sevenDaysBeforeToday, currentDate, synchronizedCameraRepository);

    // only subtract 21 here because we've subtracted 7 earlier
    cal.add(Calendar.DATE, -21);
    Date twentyEightDaysBeforeToday = cal.getTime();

    totalLocal28Days = StatisticsUtils.getTotalCamerasAddedInTimeframeFromDb(twentyEightDaysBeforeToday, currentDate, synchronizedCameraRepository);
    totalLocal = StatisticsUtils.totalCamerasInDb(synchronizedCameraRepository);
    totalByUser = StatisticsUtils.totalCamerasCapturedOnDevice(cameraRepository);

    // "camera" instead of cameras when there is only 1 camera in your homezone
    if (totalLocal == 1){
      String infoText = getResources().getQuantityString(R.plurals.number_of_cameras_in_area, 1);

      totalInAreaInfoTextView.setText(infoText);
    } else {
      String infoText = getResources().getQuantityString(R.plurals.number_of_cameras_in_area, 0);

      totalInAreaInfoTextView.setText(infoText);
    }

      String infoText = getResources().getQuantityString(R.plurals.captures_by_user, totalByUser);

      totalByUserInfoTextView.setText(infoText);


    totalInAreaTextView.setText(String.valueOf(totalLocal));
    local7DaysTextView.setText(String.valueOf(local7DaysAmount));
    local28DaysTextView.setText(String.valueOf(totalLocal28Days));
    totalByUserTextView.setText(String.valueOf(totalByUser));

  }

  private void refreshSharedPreferencesObject() {

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

  }

  /**
   * Returns a abbreviated version of large numbers. 18324 => 18.3 k
   * @param number integer number to shorten
   * @return a shortened easier readable String representing the number
   */
  private String abbreviateLargeNumber(int number){
    int thousandsInNumber = Math.floorDiv(number,  1000);

    int amountAboveThousands = number % (thousandsInNumber*1000);

    int hundredsAboveThousands = Math.floorDiv(amountAboveThousands, 100);

    //char abbreviatedHundreds = String.valueOf(hundredsInNumber).charAt(0);

    return thousandsInNumber + "." + hundredsAboveThousands + " k";
  }
}