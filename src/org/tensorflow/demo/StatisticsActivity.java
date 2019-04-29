package org.tensorflow.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class StatisticsActivity extends AppCompatActivity {

  private BottomNavigationView bottomNavigationView;
  private CameraRoomDatabase cameraDb;

  private TextView totalInArea;
  private TextView local7Days;
  private TextView local28Days;
  private TextView addedByUser;
  private TextView globalToday;
  private TextView global28Days;
  private TextView globalTotal;

  private List<HashMap<Date, Integer>> localCamerasPerDays;
  private List<HashMap<Date, Integer>> globalCamerasPerDays;

  private SharedPreferences sharedPreferences;

  private double latMin;
  private double latMax;
  private double lonMin;
  private double lonMax;

  private SynchronizedCameraRepository synchronizedCameraRepository;




  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_statistics);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());

    totalInArea = findViewById(R.id.total_cameras_statistics);

    local7Days = findViewById(R.id.added_past_7_days_statistics);
    local28Days = findViewById(R.id.added_past_28_days_statistics);
    addedByUser = findViewById(R.id.added_by_user_statistics);

    globalToday = findViewById(R.id.global_today_statistics);
    global28Days = findViewById(R.id.global_28days_statistics);
    globalTotal = findViewById(R.id.global_total_statistics);

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));


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
    int totalSevenDays = StatisticsUtils.getTotalCamerasInTimeframeFromDb(sevenDaysBeforeToday, currentDate, synchronizedCameraRepository);

    // only subtract 21 here because we've subtracted 7 earlier
    cal.add(Calendar.DATE, -21);
    Date twentyEightDaysBeforeToday = cal.getTime();

    int totalTwentyeightDays = StatisticsUtils.getTotalCamerasInTimeframeFromDb(twentyEightDaysBeforeToday, currentDate, synchronizedCameraRepository);

    int totalInDb = StatisticsUtils.totalCamerasInDb(synchronizedCameraRepository);

    totalInArea.setText(String.valueOf(totalInDb));
    local7Days.setText(String.valueOf(totalSevenDays));
    local28Days.setText(String.valueOf(totalTwentyeightDays));



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



  // Helper methods for querying database.
  private List<SurveillanceCamera> getCamerasInTimeframe(String sqlliteTimeStartpoint, String sqlliteTimeEndpoint) {
    return cameraDb.surveillanceCameraDao().getCamerasAddedInTimeframe(sqlliteTimeStartpoint, sqlliteTimeEndpoint);
  }

  private int getTotalCamerasUpTo(String sqliteTime) {
    return cameraDb.surveillanceCameraDao().getTotalCamerasUpTo(sqliteTime);
  }


}