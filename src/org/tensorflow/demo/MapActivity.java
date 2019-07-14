package org.tensorflow.demo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;


import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import com.squareup.picasso.Picasso;

import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn;


public class MapActivity extends AppCompatActivity {
  public static final String TAG = "MapActivity";

  private MapView mapView;
  private RadiusMarkerClusterer cameraCluster;

  private MyLocationNewOverlay myLocationOverlay;

  private BottomNavigationView bottomNavigationView;

  private CameraViewModel cameraViewModel;

  private List<SynchronizedCamera> itemsToDisplay = new ArrayList<>();
  private List<OverlayItem> overlayItemsToDisplay = new ArrayList<>();
  private ItemizedOverlay<OverlayItem> cameraOverlay;

  private ImageButton myLocationButton;


  private SynchronizedCameraRepository synchronizedCameraRepository;

  private List<SynchronizedCamera> allCamerasInArea = new ArrayList<>();
  private List<String> allIDsInArea = new ArrayList<>();

  private InfoWindow infoWindow;
  private ImageView infoImage;
  private TextView infoLatestTimestamp;
  private TextView infoComment;
  private ImageButton infoEscape;

  private boolean allowOneServerQuery;
  private boolean mapScrollingEnabled;

  private List<SynchronizedCamera> camerasInAreaFromServer = new ArrayList<>();
  private List<SynchronizedCamera> camerasFromLastUpdate = new ArrayList<>();
  private String lastArea = "";

  private ImageButton timemachineButton;
  private ImageButton infoButton;
  private View timemachineView;
  private View timeframeView;

  private TextView timeframeTextView;

  private Date timemachineMaxInterval;
  private Date currentSeekBarDate;
  private int daysBetween;

  private boolean isInitialSpinnerSelection;

  private SharedPreferences sharedPreferences;

  private List<SynchronizedCamera> camerasNotInDb = new ArrayList<>();
  private List<SynchronizedCamera> allCamerasInAreaFromDb = new ArrayList<>();

  private AreaOfflineAvailableRepository areaOfflineAvailableRepository;
  private List<AreaOfflineAvailable> areasOfflineAvailable = new ArrayList<>();
  private SimpleDateFormat timestampIso8601DaysAccuracy;
  private SimpleDateFormat timestampIso8601SecondsAccuracy;
  private Date latestUpdateForArea;
  private boolean timeBasedQuery;
  private double latMin;
  private double latMax;
  private double lonMin;
  private double lonMax;

  String latMinString;
  String latMaxString;
  String lonMinString;
  String lonMaxString;

  private String areaString;

  boolean offlineMode;

  private double lastZoomLevel;

  private AreaOfflineAvailable mostRecentArea;

  private String picturesPath = StorageUtils.SYNCHRONIZED_PATH;

  private TextView amountOnMapTextView;
  private TextView infoTextView;
  boolean infoIsShown = false;

  private LocalBroadcastManager localBroadcastManager;
  IntentFilter intentFilter;
  private BroadcastReceiver br;

  private boolean abortedServerQuery;

  int readStoragePermission;
  int writeStoragePermission;
  int fineLocationPermission;


  // TODO set max amount visible

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);
    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());
    areaOfflineAvailableRepository = new AreaOfflineAvailableRepository(getApplication());
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    localBroadcastManager = LocalBroadcastManager.getInstance(MapActivity.this);

    areaOfflineAvailableRepository.deleteAll();

    mapView = findViewById(R.id.map);
    mapScrollingEnabled = true;
    isInitialSpinnerSelection = true;

    timestampIso8601DaysAccuracy = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    timestampIso8601DaysAccuracy.setTimeZone(TimeZone.getTimeZone("UTC"));

    timestampIso8601SecondsAccuracy = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    lastZoomLevel = 1000; // start high to always refresh on first redraw

    //TODO find solution to do the same at the beginning of a gesture.
    // Reloads markers in visible area after scrolling. Closes infowindow if open.
    mapView.addMapListener(new DelayedMapListener(new MapListener() {
      @Override
      public boolean onScroll(ScrollEvent event) {
        reloadMarker();
        closeAllInfoWindowsOn(mapView);
        return false;
      }

      @Override
      public boolean onZoom(ZoomEvent event) {

        boolean isZoomingOut = lastZoomLevel > event.getZoomLevel();

        if (isZoomingOut) {
          reloadMarker();

        }

        closeAllInfoWindowsOn(mapView);

        lastZoomLevel = event.getZoomLevel();
        return false;
      }
    }, 150)); // delay for updating in ms after zooming/scrolling


    mapView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mapScrollingEnabled) {
          closeAllInfoWindowsOn(mapView);
          return false;
        } else {
          return true;
        }

      }
    });

    mapView.setTilesScaledToDpi(true);
    mapView.setClickable(true);

    //enable pinch to zoom
    mapView.setMultiTouchControls(true);

    // MAPNIK fix
    // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");

    // TODO add choice + backup strategy here
    mapView.setTileSource(TileSourceFactory.OpenTopo);

    final IMapController mapController = mapView.getController();

    final CustomZoomButtonsController zoomController = mapView.getZoomController();
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(50.0027, 8.2771);
    mapController.setZoom(7.0);
    mapController.setCenter(startPoint);

    amountOnMapTextView = findViewById(R.id.map_count_textview);
    infoTextView = findViewById(R.id.map_cameras_in_frame_text);

    amountOnMapTextView.setVisibility(View.GONE);
    infoTextView.setVisibility(View.GONE);

    infoButton = findViewById(R.id.map_info_button);
    infoButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        if (!infoIsShown){
          amountOnMapTextView.setVisibility(View.VISIBLE);
          infoTextView.setVisibility(View.VISIBLE);
          infoIsShown = true;

        } else {
          amountOnMapTextView.setVisibility(View.GONE);
          infoTextView.setVisibility(View.GONE);
          infoIsShown = false;
        }
      }
    });

    // myLocationOverlay
    myLocationOverlay = new MyLocationNewOverlay(mapView);
    myLocationOverlay.enableMyLocation();
    // TODO manage following
    // myLocationOverlay.enableFollowLocation();
    myLocationOverlay.setDrawAccuracyEnabled(true);
    mapController.setCenter(myLocationOverlay.getMyLocation());
    mapController.setZoom(14.00);
    mapView.getOverlays().add(myLocationOverlay);

    // Button in to find user location.
    myLocationButton = findViewById(R.id.my_location_button);
    myLocationButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mapController.setCenter(myLocationOverlay.getMyLocation());
        mapController.setZoom(15.50);
      }
    });

    final RelativeLayout mapLayout = findViewById(R.id.map_rel_layout);
    ViewGroup.LayoutParams layoutParams = mapLayout.getLayoutParams();

    offlineMode = sharedPreferences.getBoolean("offlineMode", true);

    timemachineButton = findViewById(R.id.map_timemachine_button);

    // timemachine is a scrollable timeline to show cameras cumulatively based on their date
    timemachineButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        LayoutInflater layoutInflater = (LayoutInflater) MapActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        timemachineView = findViewById(R.id.timemachine_view);
        timeframeView = findViewById(R.id.map_timeframe);

        // if timemachine not active
        if (timemachineView == null) {
          timemachineView = layoutInflater.inflate(R.layout.scrolling_timemachine, mapLayout);
          timeframeView = layoutInflater.inflate(R.layout.map_timeframe, mapLayout);

          timeframeTextView = findViewById(R.id.map_timeframe_textview);

          final SeekBar timemachineSeekBar = timemachineView.findViewById(R.id.map_timemachine_seekbar);
          final Spinner timemachineSpinner = timemachineView.findViewById(R.id.map_timemachine_spinner);

          // Create an ArrayAdapter using the string array and a default spinner layout
          ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                  R.array.map_timemachine_timeframes, android.R.layout.simple_spinner_item);
          // Specify the layout to use when the list of choices appears
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

          // Apply the adapter to the spinners
          timemachineSpinner.setAdapter(adapter);



          timemachineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {



              long currentTime = System.currentTimeMillis();
              Date currentDate = new Date(currentTime);

              Calendar cal = Calendar.getInstance();
              cal.setTime(currentDate);

              // standard timeframe includes all cameras
              if (isInitialSpinnerSelection) {
                isInitialSpinnerSelection = false;

                refreshSharedPreferencesObject();

                int savedValue = sharedPreferences.getInt("timemachineValueInDays", 0);
                // if user set timeframe before use it
                if (savedValue != 0) {
                  cal.add(Calendar.DATE, - savedValue);
                  timemachineMaxInterval = cal.getTime();
                  daysBetween = savedValue;
                } else {
                  cal.set(2018, 0, 1);
                  timemachineMaxInterval = cal.getTime();
                  daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                }

                timemachineSeekBar.invalidate();
                // steps in timeline = days in timeframe
                timemachineSeekBar.setMax(daysBetween + 1);
                timemachineSeekBar.setProgress(daysBetween + 1);

              } else {

                switch (i) {

                  case 0: // 7 days
                    cal.add(Calendar.DATE, -7);

                    // used in seekbarchanged listener
                    timemachineMaxInterval = cal.getTime();

                    sharedPreferences.edit().putInt("timemachineValueInDays", 7).apply();
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(7);
                    timemachineSeekBar.setProgress(7);
                    reloadMarker();
                    break;


                  case 1: // 4 weeks
                    cal.add(Calendar.DATE, -28);
                    timemachineMaxInterval = cal.getTime();
                    sharedPreferences.edit().putInt("timemachineValueInDays", 28).apply();
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(28);
                    timemachineSeekBar.setProgress(28);
                    reloadMarker();
                    break;

                  case 2: // 3 months
                    cal.add(Calendar.MONTH, -3);
                    timemachineMaxInterval = cal.getTime();
                    daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                    sharedPreferences.edit().putInt("timemachineValueInDays", daysBetween).apply();
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(daysBetween);
                    timemachineSeekBar.setProgress(daysBetween);
                    reloadMarker();
                    break;

                  case 3: // 6 months
                    cal.add(Calendar.MONTH, -6);
                    timemachineMaxInterval = cal.getTime();
                    daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                    sharedPreferences.edit().putInt("timemachineValueInDays", daysBetween).apply();
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(daysBetween);
                    timemachineSeekBar.setProgress(daysBetween);
                    reloadMarker();
                    break;

                  case 4: // 1 year
                    cal.add(Calendar.MONTH, -12);
                    timemachineMaxInterval = cal.getTime();
                    daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                    sharedPreferences.edit().putInt("timemachineValueInDays", daysBetween).apply();
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(daysBetween);
                    timemachineSeekBar.setProgress(daysBetween);
                    reloadMarker();
                    break;

                  case 5: // all
                    cal.set(2016, 0, 1);
                    timemachineMaxInterval = cal.getTime();
                    daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                    sharedPreferences.edit().putInt("timemachineValueInDays", daysBetween).apply();
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(daysBetween);
                    timemachineSeekBar.setProgress(daysBetween);
                    reloadMarker();
                    break;

                }
              }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

              long currentTime = System.currentTimeMillis();
              Date currentDate = new Date(currentTime);

              Calendar cal = Calendar.getInstance();
              cal.setTime(currentDate);

              cal.add(Calendar.MONTH, -6);
              timemachineMaxInterval = cal.getTime();
              daysBetween = daysBetween(timemachineMaxInterval, currentDate);
              timemachineSeekBar.invalidate();
              timemachineSeekBar.setMax(daysBetween + 1);
              timemachineSeekBar.setProgress(0);
              reloadMarker();



            }
          });


          timemachineSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {


              Calendar tempcal = Calendar.getInstance();
              tempcal.setTime(timemachineMaxInterval);
              tempcal.add(Calendar.DATE, i);
              currentSeekBarDate = tempcal.getTime();

              SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
              timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
              // Show current date in timemachine
              timeframeTextView.setText(getString(R.string.timemachine_text, timestampIso8601.format(currentSeekBarDate)));
              redrawMarkers(allCamerasInArea);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
          });




        } else {
          // disable timemachine on buttonclick if timemachine is active
          mapLayout.removeView(timemachineView);
          mapLayout.removeView(timeframeView);
          isInitialSpinnerSelection = true;
          redrawMarkers(allCamerasInArea);
        }

      }
    });

    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    // bottom navigation bar
    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(MapActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:

            if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
              Intent manualCaptureIntent = new Intent(MapActivity.this, ManualCaptureActivity.class);
              startActivity(manualCaptureIntent);
              return true;
            } else {
              Intent cameraIntent = new Intent(MapActivity.this, DetectorActivity.class);
              startActivity(cameraIntent);
              return true;
            }


          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(MapActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(MapActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_map).setChecked(true);

  }

  @Override
  protected void onResume() {
    super.onResume();

    br = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        reloadMarker();
        Toast.makeText(MapActivity.this, "Server authorization refreshed, please repeat your last action", Toast.LENGTH_LONG).show();
      }
    };

    intentFilter = new IntentFilter("org.unsurv.API_KEY_CHANGED");

    localBroadcastManager.registerReceiver(br, intentFilter);
    refreshSharedPreferencesObject();
    reloadMarker();

    readStoragePermission = ContextCompat.checkSelfPermission(MapActivity.this,
            Manifest.permission.READ_EXTERNAL_STORAGE);
    writeStoragePermission = ContextCompat.checkSelfPermission(MapActivity.this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);
    fineLocationPermission = ContextCompat.checkSelfPermission(MapActivity.this,
            Manifest.permission.ACCESS_FINE_LOCATION);


    List<String> permissionList = new ArrayList<>();

    if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }


    if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
    }


    String[] neededPermissions = permissionList.toArray(new String[0]);

    if (!permissionList.isEmpty()) {
      ActivityCompat.requestPermissions(MapActivity.this, neededPermissions, 2);
    }

    super.onResume();

  }

  @Override
  protected void onPause() {

  localBroadcastManager.unregisterReceiver(br);

  super.onPause();

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

      // settings Button
      case R.id.action_settings:
        Intent settingsIntent = new Intent(MapActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

        return true;

      case R.id.action_refresh:

        if (!offlineMode) {

          queryServerForCameras("area=" + areaString);
        } else {
          updateAllCamerasInArea(true);
          redrawMarkers(allCamerasInArea);
        }

        return true;

      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }


  // Modified from osm example app.
  /**
   * Load {@link SurveillanceCamera} in area  in a Background Task {@link BackgroundMarkerLoaderTask}.
   * mCurrentBackgroundMarkerLoaderTask.cancel() allows aborting the loading task on screen rotation.
   * There are 0 or one tasks running at a time.
   */
  private BackgroundMarkerLoaderTask mCurrentBackgroundMarkerLoaderTask = null;


  /**
   * if > 0 there where zoom/scroll events while {@link BackgroundMarkerLoaderTask} was active so
   * {@link #reloadMarker()} bust be called again.
   */
  private int mMissedMapZoomScrollUpdates = 0;


  private void reloadMarker() {

    if (abortedServerQuery) {
      queryServerForCameras(lastArea);
      abortedServerQuery = false;
    }

    if (mCurrentBackgroundMarkerLoaderTask == null) {
      // start background load
      double zoom = this.mapView.getZoomLevelDouble();
      BoundingBox world = this.mapView.getBoundingBox();

      // WHY IS LONGITUDE EAST < LONGITUDE WEST
      reloadMarker(world, zoom);

    } else {
      // background load is already active. Remember that at least one scroll/zoom was missing
      mMissedMapZoomScrollUpdates++;
    }
  }

  /**
   * called by MapView if zoom or scroll has changed to reload marker for new visible region
   */
  private void reloadMarker(BoundingBox latLonArea, double zoom) {
    Log.d(TAG, "reloadMarker " + latLonArea + ", zoom " + zoom);
    this.mCurrentBackgroundMarkerLoaderTask = new BackgroundMarkerLoaderTask();
    this.mCurrentBackgroundMarkerLoaderTask.execute(
            latLonArea.getLatSouth(), latLonArea.getLatNorth(),
            latLonArea.getLonEast(), latLonArea.getLonWest(), zoom);

  }


  private class BackgroundMarkerLoaderTask extends AsyncTask<Double, Integer, List<SynchronizedCamera>> {

    /**
     * Computation of the map itmes in the non-gui background thread. .
     *
     * @param params latMin, latMax, lonMin, longMax, zoom.
     * @return List of SynchronizedCamera[s] in the current Map window.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected List<SynchronizedCamera> doInBackground(Double... params) {
      FolderOverlay result = new FolderOverlay();

      // clear vars to work with
      allCamerasInArea.clear();
      camerasNotInDb.clear();
      areasOfflineAvailable.clear();

      // set standard to query for complete timeline of area
      timeBasedQuery = false;

      try {
        if (params.length != 5)
          throw new IllegalArgumentException("expected latMin, latMax, lonMin, longMax, zoom");

        int paramNo = 0;
        latMin = params[paramNo++];
        latMax = params[paramNo++];
        lonMin = params[paramNo++];
        lonMax = params[paramNo++];

        // change values if in wrong order
        if (latMin > latMax) {
          double t = latMax;
          latMax = latMin;
          latMin = t;
        }
        if (latMax - latMin < 0.00001)
          return null;
        //this is a problem, abort https://github.com/osmdroid/osmdroid/issues/521

        if (lonMin > lonMax) {
          double t = lonMax;
          lonMax = lonMin;
          lonMin = t;
        }

        latMinString = String.valueOf(latMin);
        latMaxString = String.valueOf(latMax);
        lonMinString = String.valueOf(lonMin);
        lonMaxString = String.valueOf(lonMax);

        areaString =
                latMinString + ","
                        + latMaxString + ","
                        + lonMinString + ","
                        + lonMaxString;

        // use "cached" cameras if area the same as last update except manual user update
        if (areaString.equals(lastArea)) { // TODO manual user update
          return camerasFromLastUpdate;
        }

        double zoom = params[paramNo++];

        Log.d(TAG, "doInBackground" +
                " latMin=" + latMin +
                " ,latMax=" + latMax +
                " ,lonMin=" + lonMin +
                " ,lonMax=" + lonMax +
                ", zoom=" + zoom);

        refreshSharedPreferencesObject();

        // TODO maybe move more or less static stuff out of background query? maybe keep to stay updated

        String offlineArea = sharedPreferences.getString("area", null); // SNWE
        String[] splitBorders = offlineArea.split(",");

        // "homezone" offline available borders
        double offlineLatMin = Double.parseDouble(splitBorders[0]);
        double offlineLatMax = Double.parseDouble(splitBorders[1]);
        double offlineLonMin = Double.parseDouble(splitBorders[2]);
        double offlineLonMax = Double.parseDouble(splitBorders[3]);

        final boolean allowServerQueries = sharedPreferences.getBoolean("allowServerQueries", false);

        // always query db
        allCamerasInAreaFromDb.clear();
        allCamerasInAreaFromDb = synchronizedCameraRepository.getSynchronizedCamerasInArea(latMin, latMax, lonMin, lonMax);
        allIDsInArea.clear();
        allIDsInArea = synchronizedCameraRepository.getIDsInArea(latMin, latMax, lonMin, lonMax);

        updateAllCamerasInArea(false);

        boolean outsideOfflineArea = latMin < offlineLatMin ||
                latMax > offlineLatMax ||
                lonMin < offlineLonMin ||
                lonMax > offlineLonMax;


        // not in offline mode & outside offline area
        if (!offlineMode && outsideOfflineArea) {

          // check if area was visited before
          areasOfflineAvailable = areaOfflineAvailableRepository.isOfflineavailable(latMin, latMax, lonMin, lonMax);

          // debug ... always query for now
          areasOfflineAvailable.clear();

          // find latest update for current area
          if (!areasOfflineAvailable.isEmpty()) {
            timeBasedQuery = true;

            // set to early date temporarily
            latestUpdateForArea = new Date(0);

            for (AreaOfflineAvailable item : areasOfflineAvailable) {
              // use precise timings for areas

              Date itemUpdate = timestampIso8601SecondsAccuracy.parse(item.getLastUpdated());


              if (itemUpdate.after(latestUpdateForArea)) {
                latestUpdateForArea = itemUpdate;
                mostRecentArea = item;
              }
            }
          }

          // TODO query with start= if timebasedquery == true;

          runOnUiThread(new Runnable() {
            @Override
            public void run() {

              // create popupView to ask user if he wants to connect to a server to get data
              // saves preference if checkbox ticked
              LayoutInflater layoutInflater = (LayoutInflater) MapActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
              View popupView = layoutInflater.inflate(R.layout.leaving_offline_popup, null);

              final CheckBox dontAskAgainACheckBox = popupView.findViewById(R.id.map_popup_dont_show_again_checkbox);

              Button yesButton = popupView.findViewById(R.id.map_popup_yes_button);
              Button noButton = popupView.findViewById(R.id.map_popup_no_button);

              final PopupWindow popupWindow =
                      new PopupWindow(popupView,
                      MapView.LayoutParams.WRAP_CONTENT,
                      MapView.LayoutParams.WRAP_CONTENT);

              boolean askForConnection = sharedPreferences.getBoolean("askForConnections", true);

              if (!popupWindow.isShowing() && !allowServerQueries && askForConnection) {
                // freeze map while popupWindow is showing
                mapScrollingEnabled = false;

                popupWindow.showAtLocation(mapView, Gravity.CENTER, 0, 0);
                yesButton.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {


                    if (dontAskAgainACheckBox.isChecked()) {
                      sharedPreferences.edit().putBoolean("allowServerQueries", true).apply();
                      sharedPreferences.edit().putBoolean("askForConnections", false).apply();

                    }

                    allowOneServerQuery = true;
                    popupWindow.dismiss();
                    reloadMarker();
                    mapScrollingEnabled = true;

                  }
                });

                noButton.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {

                    if (dontAskAgainACheckBox.isChecked()) {
                      sharedPreferences.edit().putBoolean("allowServerQueries", false).apply();
                      sharedPreferences.edit().putBoolean("askForConnections", false).apply();

                    }
                    popupWindow.dismiss();
                    mapScrollingEnabled = true;

                  }
                });
              }
            }
          });


          // permanent permisson or onetime permission for outside area
          if (allowServerQueries || allowOneServerQuery) {

            String areaQuery = "area=" + areaString;

            // TODO add start value to only update not download all everytime. need per area lastUpdated in own db

            String currentQuery = areaQuery; // TODO add startQuery when needed


            // area not the same as last update -> query server for data
            if (!lastArea.equals(currentQuery)) {

              if(timeBasedQuery) {
                // area already visited and therefore in db, only query for updates since last "visit"

                // don't query if last visit less than 5 mins ago
                Date today = new Date(System.currentTimeMillis() - 1000*60);

                if (latestUpdateForArea.before(today)) {
                  String latestUpdate = timestampIso8601DaysAccuracy.format(latestUpdateForArea);
                  currentQuery = currentQuery.concat("&start=" + latestUpdate);
                  queryServerForCameras(currentQuery);
                  lastArea = areaString;
                }

              } else {
                queryServerForCameras(currentQuery);
                lastArea = areaString;

              }
            }

          }


        } else if (offlineMode && outsideOfflineArea) {
          Toast.makeText(getApplicationContext(), "Not available in Offline-Mode", Toast.LENGTH_SHORT).show();
        }


        // cache for current map
        camerasFromLastUpdate = allCamerasInArea;

        Log.d(TAG, "doInBackground: " + allCamerasInArea.size());


      } catch (Exception ex) {
        // TODO more specific error handling
        Log.e(TAG, "doInBackground  " + ex.getMessage(), ex);
        cancel(false);
      }

      if (!isCancelled()) {
        Log.d(TAG, "doInBackground result " + result.getItems().size());
        return allCamerasInArea;
      }
      Log.d(TAG, "doInBackground cancelled");
      return null;
    }

    @Override
    protected void onPostExecute(List<SynchronizedCamera> camerasToDisplay) {
      if (!isCancelled() && (camerasToDisplay != null)) {

        redrawMarkers(camerasToDisplay);

      }

      mCurrentBackgroundMarkerLoaderTask = null;
      // there was map move/zoom while {@link BackgroundMarkerLoaderTask} was active. must reload
      if (mMissedMapZoomScrollUpdates > 0) {
        Log.d(TAG, "onPostExecute: lost  " + mMissedMapZoomScrollUpdates + " MapZoomScrollUpdates. Reload items.");
        mMissedMapZoomScrollUpdates = 0;
        reloadMarker();
      }
    }
  }

  /**
   * calculates days inbetween 2 Date[s]
   * @param d1
   * @param d2
   * @return
   */
  public int daysBetween(Date d1, Date d2){
    return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
  }

  private void updateAllCamerasInArea(boolean queryDB){
    allCamerasInArea.clear();

    if (queryDB) {
      allCamerasInAreaFromDb = synchronizedCameraRepository.getSynchronizedCamerasInArea(latMin, latMax, lonMin, lonMax);
    }

    allCamerasInArea.addAll(allCamerasInAreaFromDb);
    allCamerasInArea.addAll(camerasNotInDb);
  }

  private void redrawMarkers(List<SynchronizedCamera> camerasToDisplay) {

    itemsToDisplay.clear();
    overlayItemsToDisplay.clear();

    timemachineView = findViewById(R.id.map_timemachine_seekbar);

    Drawable cameraMarkerIcon = getDrawable(R.drawable.standard_camera_marker_5_dpi);
    //Drawable clusterCameraMarkerIcon = ResourcesCompat.getDrawableForDensity(getResources(), R.drawable.standard_camera_marker_5_dpi, 400, null);
    Drawable clusterCameraMarkerIcon = getDrawable(R.drawable.standard_camera_marker_15dpi);

    //Drawable cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(view.getContext().getResources(), R.drawable.ic_close_red_24dp, 12, null);

    refreshSharedPreferencesObject();
    boolean clusteringEnabled = sharedPreferences.getBoolean("clusteringEnabled", true);

    // normal behaviour if timemachine not active. clusters cameras
    if (timemachineView == null && clusteringEnabled) {

      mapView.getOverlays().remove(cameraCluster);
      mapView.getOverlays().remove(cameraOverlay);

      cameraCluster = new RadiusMarkerClusterer(this);

      cameraCluster.setRadius(150);
      cameraCluster.setMaxClusteringZoomLevel(14);
      itemsToDisplay.addAll(camerasToDisplay);


      Drawable clusterIconDrawable = getDrawable(R.drawable.ic_brightness_1_red_24dp);
      // Bitmap clusterIcon = ((BitmapDrawable)clusterIconDrawable).getBitmap();
      Bitmap clusterIcon = BitmapFactory.decodeResource(getResources(), R.drawable.marker_cluster);

      cameraCluster.setIcon(clusterIcon);

      int size = camerasToDisplay.size();
      amountOnMapTextView.setText(String.valueOf(size));
      String infoText = " " + getResources().getQuantityString(R.plurals.total_cameras_in_map_text, size);
      infoTextView.setText(infoText);

      for (int i = 0; i < itemsToDisplay.size(); i++) {
        Marker cameraMarker = new Marker(mapView);
        SynchronizedCamera currentCamera = itemsToDisplay.get(i);

        cameraMarker.setPosition(new GeoPoint(
                currentCamera.getLatitude(),
                currentCamera.getLongitude()));

        cameraMarker.setIcon(clusterCameraMarkerIcon);

        cameraMarker.setRelatedObject(currentCamera);
        cameraMarker.setInfoWindow(new CustomInfoWindow(mapView, sharedPreferences));
        cameraMarker.setPanToView(false);
        //cameraMarker.setTitle(camerasToDisplay.get(i).getComments());

        cameraCluster.add(cameraMarker);
      }

      mapView.getOverlays().add(cameraCluster);
      mapView.invalidate();



    } else { // clustering slows down timemachine immensely, use itemized overlay instead

      final int maxMarkersOnMap = Integer.parseInt(sharedPreferences.getString("maxMapMarkers", "400"));

      mapView.getOverlays().remove(cameraCluster);
      mapView.getOverlays().remove(cameraOverlay);

      if (clusteringEnabled) {

        try{
          // display only cameras between seekbar max amount in the past and current seekbardate chosen by user
          // if seekbar is set to last week, only cameras added less than a week ago will be shown

          Iterator<SynchronizedCamera> iter = camerasToDisplay.iterator();

          int counter = 0;

          while (iter.hasNext()) {
            SynchronizedCamera nextCamera = iter.next();
            Date cameraLastUpdated = timestampIso8601DaysAccuracy.parse(nextCamera.getLastUpdated());
            if (cameraLastUpdated.before(currentSeekBarDate) && cameraLastUpdated.after(timemachineMaxInterval)) {

              overlayItemsToDisplay.add(new OverlayItem(
                      nextCamera.getExternalID(),
                      "test_camera",
                      nextCamera.getComments(),
                      new GeoPoint(nextCamera.getLatitude(), nextCamera.getLongitude()
                      )));

            }

            counter++;
            if (counter > maxMarkersOnMap) {
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(getApplicationContext(), String.valueOf(maxMarkersOnMap)
                          + " Markers reached, please zoom in or increase amount in settings",
                          Toast.LENGTH_SHORT).show();
                }
              });

              break;


            }
          }

        } catch (ParseException parseExc) {
          Log.i(TAG, "parseException");
        }

      } else { // add all if clustering is disabled and no timemachine active

        int counter = 0;

        for (SynchronizedCamera camera : camerasToDisplay) {
          overlayItemsToDisplay.add(new OverlayItem(
                  camera.getExternalID(),
                  "test_camera",
                  camera.getComments(),
                  new GeoPoint(camera.getLatitude(), camera.getLongitude()
                  )));
          counter++;


          if (counter > maxMarkersOnMap) {
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                Toast.makeText(getApplicationContext(), String.valueOf(maxMarkersOnMap)
                                + " Markers reached, please zoom in or increase amount in settings",
                        Toast.LENGTH_SHORT).show();
              }
            });

            break;

          }
        }

      }

      int size = camerasToDisplay.size();
      amountOnMapTextView.setText(String.valueOf(overlayItemsToDisplay.size()));
      String infoText = " " + getResources().getQuantityString(R.plurals.total_cameras_in_map_text, size);
      infoTextView.setText(infoText);

      cameraOverlay = new ItemizedIconOverlay<>(overlayItemsToDisplay, cameraMarkerIcon,
              new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

                @Override
                public boolean onItemSingleTapUp(final int index, final OverlayItem cameraItem) {
                  GeoPoint markerLocation = new GeoPoint(cameraItem.getPoint().getLatitude(), cameraItem.getPoint().getLongitude());

                  //close existing infoWindow
                  if (infoWindow != null) {
                    infoWindow.close();
                  }

                  infoWindow = new InfoWindow(R.layout.camera_info_window, mapView) {
                    @Override
                    public void onOpen(Object item) {
                      final SynchronizedCamera highlightedCamera = synchronizedCameraRepository.findByID(cameraItem.getUid());

                      infoWindow.setRelatedObject(highlightedCamera);

                      // Setting content for infoWindow.
                      infoImage = infoWindow.getView().findViewById(R.id.info_image);
                      infoLatestTimestamp = infoWindow.getView().findViewById(R.id.info_latest_timestamp);
                      infoComment = infoWindow.getView().findViewById(R.id.info_comment);

                      // TODO add logic for querying the server for individual pictures if not in offline area etc

                      File thumbnail = new File(picturesPath + highlightedCamera.getImagePath());

                      Picasso.get().load(thumbnail)
                              .placeholder(R.drawable.ic_file_download_grey_48dp)
                              .into(infoImage);

                      infoImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                          String baseUrl = sharedPreferences.getString("synchronizationURL", null);
                          SynchronizationUtils.downloadImagesFromServer(
                                  baseUrl,
                                  Collections.singletonList(highlightedCamera),
                                  sharedPreferences);

                          Handler handler = new Handler();
                          handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                              File updatedThumbnail = new File(picturesPath + highlightedCamera.getImagePath());

                              infoImage.setImageDrawable(null);
                              Picasso.get().load(updatedThumbnail)
                                      .placeholder(R.drawable.ic_file_download_grey_48dp)
                                      .into(infoImage);

                            }
                          }, 500);


                        }
                      });


                      infoLatestTimestamp.setText(highlightedCamera.getLastUpdated());
                      infoComment.setText(highlightedCamera.getComments());

                    }

                    @Override
                    public void onClose() {

                    }
                  };

                  infoWindow.open(cameraItem, markerLocation, 0, 0);

                    /*
                    Toast.makeText(
                            MapActivity.this,
                            "Item '" + cameraItem.getTitle() + "' (index=" + index
                                    + ") got single tapped up", Toast.LENGTH_LONG).show();
                    */
                  return true; // We 'handled' this event.
                }

                @Override
                public boolean onItemLongPress(final int index, final OverlayItem cameraItem) {
                  Toast.makeText(
                          MapActivity.this,
                          "Item '" + cameraItem.getTitle() + "' (index=" + index
                                  + ") got long pressed", Toast.LENGTH_LONG).show();
                  return false;
                }
              }, getApplicationContext());

      mapView.getOverlays().add(cameraOverlay);
      mapView.invalidate();
    }

  }


  /**
   *
   * @param queryString url query string i.e. "area=8.2699,50.0201,8.2978,50.0005&start=2018-01-01"
   */
  void queryServerForCameras(String queryString) {

    // aborts current query if API key expired. starts same query after a new API key is aquired
    refreshSharedPreferencesObject();

    String apiKey = sharedPreferences.getString("apiKey", null);
    String apiKeyExp = sharedPreferences.getString("apiKeyExpiration", null);


    if (SynchronizationUtils.refreshApiKeyIfExpired(sharedPreferences, getApplicationContext())){
      abortedServerQuery = true;
      lastArea = queryString;
      // abort current query
      return;
    }

    camerasInAreaFromServer.clear();

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    // String url = "http://192.168.2.159:5000/cameras/?area=8.2699,50.0201,8.2978,50.0005";
    refreshSharedPreferencesObject();
    String baseURL = sharedPreferences.getString("synchronizationURL", null) + "cameras/?";

    String url = baseURL + queryString;

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                JSONObject JSONToSynchronize;

                try {

                  for (int i = 0; i < response.getJSONArray("cameras").length(); i++) {
                    JSONToSynchronize = new JSONObject(String.valueOf(response.getJSONArray("cameras").get(i)));

                    SynchronizedCamera cameraToAdd = new SynchronizedCamera(
                            JSONToSynchronize.getString("id") + ".jpg",
                            JSONToSynchronize.getString("id"),
                            JSONToSynchronize.getDouble("lat"),
                            JSONToSynchronize.getDouble("lon"),
                            JSONToSynchronize.getString("comments"),
                            JSONToSynchronize.getString("last_updated"),
                            JSONToSynchronize.getString("uploaded_at"),
                            JSONToSynchronize.getBoolean("manual_capture")


                    );

                    camerasInAreaFromServer.add(cameraToAdd);

                  }

                } catch (Exception e) {
                  Log.i(TAG, "onResponse: " + e.toString());

                  Toast.makeText(
                          MapActivity.this,
                          "Error retrieving data from Server. Try again later.", Toast.LENGTH_LONG).show();

                }
              }

            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // TODO: Handle different http Errors
        Log.i(TAG, "HTTP Error: " + error.toString());
      }

    }) {
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {

        Map<String, String> headers = new HashMap<>();
        refreshSharedPreferencesObject();
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

    if (allowOneServerQuery) {
      allowOneServerQuery = false; // used up single permission for querying
    }

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {

        camerasNotInDb.clear();

        Set<String> IDsetFromDb = new HashSet<>(allIDsInArea);
        // check if data already in db
        for (SynchronizedCamera item : camerasInAreaFromServer) {
          if (!IDsetFromDb.contains(item.getExternalID())) {
            camerasNotInDb.add(item);
          }
        }

        // update local db with new data
        synchronizedCameraRepository.insert(camerasNotInDb);

        if(timeBasedQuery) {
          areaOfflineAvailableRepository.update(mostRecentArea);
        } else {
          long currentTimeInMillis = System.currentTimeMillis();
          String currentIsoDate = timestampIso8601SecondsAccuracy.format(new Date(currentTimeInMillis));
          areaOfflineAvailableRepository.insert(new AreaOfflineAvailable(latMin, latMax, lonMin, lonMax, currentIsoDate));
        }

        // TODO add area to db

        updateAllCamerasInArea(false);
        redrawMarkers(allCamerasInArea);
      }
    });

  }

  private void refreshSharedPreferencesObject() {

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

  }

}