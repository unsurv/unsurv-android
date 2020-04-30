package org.unsurv;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;


import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
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

/**
 * This is one of the 4 main activities. It displays cameras on a adjustable
 * osmdroid map.
 */

public class MapActivity extends AppCompatActivity {
  public static final String TAG = "MapActivity";

  private MapView mapView;
  private RadiusMarkerClusterer cameraCluster;

  private MyLocationNewOverlay myLocationOverlay;

  private BottomNavigationView bottomNavigationView;

  private List<SynchronizedCamera> itemsToDisplay = new ArrayList<>();
  private List<OverlayItem> overlayItemsToDisplay = new ArrayList<>();
  private ItemizedOverlay<OverlayItem> cameraOverlay;

  ImageButton myLocationButton;


  private SynchronizedCameraRepository synchronizedCameraRepository;

  private List<SynchronizedCamera> allCamerasInArea = new ArrayList<>();
  private List<String> allIDsInArea = new ArrayList<>();

  private InfoWindow infoWindow;
  private ImageView infoImage;
  private TextView infoLatestTimestamp;
  // private TextView infoComment;
  // private ImageButton infoEscape;

  private boolean allowOneServerQuery;
  private boolean mapScrollingEnabled;

  private List<SynchronizedCamera> camerasInAreaFromServer = new ArrayList<>();
  private List<SynchronizedCamera> camerasFromLastUpdate = new ArrayList<>();
  private String lastArea = "";

  ImageButton timemachineButton;
  ImageButton infoButton;
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

  private ImageButton showStandardCamerasButton;
  private ImageButton showDomeCamerasButton;
  private ImageButton showUnknownCamerasButton;

  boolean showStandardCameras = true;
  boolean showDomeCameras = true;
  boolean showUnknownCameras = true;

  private LocalBroadcastManager localBroadcastManager;
  IntentFilter intentFilter;
  private BroadcastReceiver br;

  private boolean abortedServerQuery;

  int readStoragePermission;
  int writeStoragePermission;
  int fineLocationPermission;

  Context context;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);

    context = this;

    // db access
    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());
    areaOfflineAvailableRepository = new AreaOfflineAvailableRepository(getApplication());

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    // disables clustering until a bug in library has been fixed
    sharedPreferences.edit().putBoolean("clusteringEnabled", false).apply();

    localBroadcastManager = LocalBroadcastManager.getInstance(MapActivity.this);

    areaOfflineAvailableRepository.deleteAll();

    mapView = findViewById(R.id.tutorial_map);
    mapScrollingEnabled = true;

    CopyrightOverlay copyrightOverlay = new CopyrightOverlay(context);
    mapView.getOverlays().add(copyrightOverlay);

    // sets timemachine spinner to default value
    isInitialSpinnerSelection = true;

    timestampIso8601DaysAccuracy = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    timestampIso8601DaysAccuracy.setTimeZone(TimeZone.getTimeZone("UTC"));

    timestampIso8601SecondsAccuracy = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    // later used to only refresh map when zooming in, start high to always refresh on first redraw
    lastZoomLevel = 1000;

    // refresh map and close infowindows when scrolling
    mapView.addMapListener(new DelayedMapListener(new MapListener() {
      @Override
      public boolean onScroll(ScrollEvent event) {
        reloadMarker();
        closeAllInfoWindowsOn(mapView);
        return false;
      }

      // refresh if zooming out
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

    // blocks zooming and moving when needed
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

    if (offlineMode) {

      //first we'll look at the default location for tiles that we support
      File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/osmdroid/");
      if (f.exists()) {

        File[] list = f.listFiles();
        if (list != null) {
          for (int i = 0; i < list.length; i++) {
            if (list[i].isDirectory()) {
              continue;
            }
            String name = list[i].getName().toLowerCase();
            if (!name.contains(".")) {
              continue; //skip files without an extension
            }
            name = name.substring(name.lastIndexOf(".") + 1);
            if (name.length() == 0) {
              continue;
            }
            if (ArchiveFileFactory.isFileExtensionRegistered(name)) {


              try {
                //ok found a file we support and have a driver for the format, for this demo, we'll just use the first one

                //create the offline tile provider, it will only do offline file archives
                //again using the first file
                OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(getApplication()),
                        new File[]{list[i]});
                //tell osmdroid to use that provider instead of the default rig which is (asserts, cache, files/archives, online
                mapView.setTileProvider(tileProvider);

                mapView.setTileSource(new XYTileSource(
                        "tiles",
                        6,
                        16,
                        256,
                        ".png",
                        new String[]{""}));

              } catch (Exception ex) {
                Toast.makeText(context, "Could not load offline tiles", Toast.LENGTH_LONG).show();
              }
            }
          }
        }
      }

    } else {

      // MAPNIK fix
      // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");
      // TODO add choice + backup strategy here
      mapView.setTileSource(TileSourceFactory.OpenTopo);
    }

    final IMapController mapController = mapView.getController();

    // remove big + nad - buttons at the bottom of the map
    final CustomZoomButtonsController zoomController = mapView.getZoomController();
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

    // Setting starting position and zoom level.
    String homeZone = sharedPreferences.getString("area", null);

    String[] coordinates = homeZone.split(",");

    double latMin = Double.valueOf(coordinates[0]);
    double latMax = Double.valueOf(coordinates[1]);
    double lonMin = Double.valueOf(coordinates[2]);
    double lonMax = Double.valueOf(coordinates[3]);

    double centerLat = (latMin + latMax) / 2;
    double centerLon = (lonMin + lonMax) / 2;

    // Setting starting position and zoom level. Use center of homezone for now
    GeoPoint startPoint = new GeoPoint(centerLat, centerLon);
    mapController.setZoom(10.0);
    mapController.setCenter(startPoint);

    showStandardCamerasButton = findViewById(R.id.map_show_standard_cameras_button);
    showDomeCamerasButton = findViewById(R.id.map_show_dome_cameras_button);
    showUnknownCamerasButton = findViewById(R.id.map_show_unknown_cameras_button);

    amountOnMapTextView = findViewById(R.id.map_count_textview);
    infoTextView = findViewById(R.id.map_info_text);

    amountOnMapTextView.setVisibility(View.GONE);
    infoTextView.setVisibility(View.GONE);

    // info button
    infoButton = findViewById(R.id.map_info_button);
    infoButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        // activates info overlay if not already present, deactivates it if present
        if (!infoIsShown){
          amountOnMapTextView.setVisibility(View.VISIBLE);
          infoTextView.setVisibility(View.VISIBLE);
          showStandardCamerasButton.setVisibility(View.VISIBLE);
          showDomeCamerasButton.setVisibility(View.VISIBLE);
          showUnknownCamerasButton.setVisibility(View.VISIBLE);

          // start with green background meaning all cameras are shown
          showStandardCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterEnabled, null));
          showDomeCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterEnabled, null));
          showUnknownCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterEnabled, null));

          infoIsShown = true;

        } else {
          amountOnMapTextView.setVisibility(View.GONE);
          infoTextView.setVisibility(View.GONE);
          showStandardCamerasButton.setVisibility(View.GONE);
          showDomeCamerasButton.setVisibility(View.GONE);
          showUnknownCamerasButton.setVisibility(View.GONE);

          showStandardCameras = true;
          showDomeCameras = true;
          showUnknownCameras = true;

          infoIsShown = false;

          redrawMarkers(allCamerasInArea);
        }
      }
    });


    // enable filters based on button clicks
    showStandardCamerasButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (showStandardCameras) {
          showStandardCameras = false;
          showStandardCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterDisabled, null));

        } else {
          showStandardCameras = true;
          showStandardCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterEnabled, null));
        }

        redrawMarkers(allCamerasInArea);

      }
    });

    showDomeCamerasButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (showDomeCameras) {
          showDomeCameras = false;
          showDomeCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterDisabled, null));

        } else {
          showDomeCameras = true;
          showDomeCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterEnabled, null));
        }

        redrawMarkers(allCamerasInArea);

      }
    });

    showUnknownCamerasButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (showUnknownCameras) {
          showUnknownCameras = false;
          showUnknownCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterDisabled, null));

        } else {
          showUnknownCameras = true;
          showUnknownCamerasButton.setBackgroundColor(getResources().getColor(R.color.cameraFilterEnabled, null));
        }

        redrawMarkers(allCamerasInArea);

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

    // Button to find user location.
    myLocationButton = findViewById(R.id.my_location_button);
    myLocationButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mapController.setCenter(myLocationOverlay.getMyLocation());
        mapController.setZoom(15.50);
      }
    });

    final RelativeLayout mapLayout = findViewById(R.id.map_rel_layout);

    offlineMode = sharedPreferences.getBoolean("offlineMode", true);

    timemachineButton = findViewById(R.id.map_timemachine_button);

    // timemachine is a scrollable bar to show cameras cumulatively based on their date
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

          // when user chooses a different timeframe
          timemachineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {


              long currentTime = System.currentTimeMillis();
              Date currentDate = new Date(currentTime);

              Calendar cal = Calendar.getInstance();
              cal.setTime(currentDate);

              // first timeframe includes all cameras
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
                  // month starts at 0 date at 1 WTF Java
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

            // use 6 months back as timeframe if nothing is selected in spinner by user
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


          // detects changes in timemachine
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
              // TODO fix "1 cameras in area"
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

    androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
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

    // refresh markers when api key is changed, useful if user is on an area outside of his homezone
    br = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        reloadMarker();
        Toast.makeText(MapActivity.this, "Server authorization refreshed, please repeat your last action", Toast.LENGTH_LONG).show();
      }
    };

    intentFilter = new IntentFilter("org.unsurv.API_KEY_CHANGED");

    localBroadcastManager.registerReceiver(br, intentFilter);

    // refresh on resume
    refreshSharedPreferencesObject();
    reloadMarker();

    // get permissions if not already given
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

    // clear bottom navigation map badge and get badges for other items
    BottomNavigationBadgeHelper.clearMenuItemBadge(bottomNavigationView, R.id.bottom_navigation_map, context);
    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);

    super.onResume();

  }

  @Override
  protected void onPause() {

  localBroadcastManager.unregisterReceiver(br);

  super.onPause();

  }

  // ActionBar on top with settings
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //return super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.actionbar, menu);

    MenuItem trainingItem = menu.findItem(R.id.action_training);
    trainingItem.setVisible(false);
    return true;
  }

  // handles clicks in the top ActionBar
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
          queryServerForCameras(areaString);
          updateAllCamerasInArea(true);
          redrawMarkers(allCamerasInArea);
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

    // query is aborted if api key expired
    // reloadMarker is called after api key is refreshed and server query is repeated here
    if (abortedServerQuery) {
      queryServerForCameras(lastArea);
      abortedServerQuery = false;
    }

    // there is no current background task active
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
     * Computation of the map items in the non-gui background thread. .
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
        // this is a problem, abort https://github.com/osmdroid/osmdroid/issues/521

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

        // detect if an area on the map is outside of the homezone
        boolean outsideOfflineArea =
                latMin < offlineLatMin ||
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

          // shows an alert when leaving your homezone and server queries are not enabled by default
          // asks the user if he wants to query the server
          runOnUiThread(new Runnable() {
            @Override
            public void run() {

              boolean allowServerQueries = sharedPreferences.getBoolean("allowServerQueries", true);

              if (!allowServerQueries) {

                LayoutInflater layoutInflater = (LayoutInflater) MapActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                View dontAskAgainLinearLayout = layoutInflater.inflate(R.layout.alert_dialog_dont_ask_again, null);
                final CheckBox dontAskAgainCheckBox = dontAskAgainLinearLayout.findViewById(R.id.dismiss_popup_dont_show_again_checkbox);
                alertDialogBuilder.setView(dontAskAgainLinearLayout);

                alertDialogBuilder.setTitle("You are leaving your offline area.");

                alertDialogBuilder.setMessage("Start downloading from the server?");

                alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {
                    if (dontAskAgainCheckBox.isChecked()) {
                      sharedPreferences.edit().putBoolean("allowServerQueries", true).apply();

                    }

                    allowOneServerQuery = true;
                    reloadMarker();

                  }
                });

                alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {

                    if (dontAskAgainCheckBox.isChecked()) {
                      sharedPreferences.edit().putBoolean("allowServerQueries", false).apply();

                    }

                  }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();

                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                  @Override
                  public void onShow(DialogInterface dialogInterface) {
                    mapScrollingEnabled = false;
                  }
                });

                alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                  @Override
                  public void onDismiss(DialogInterface dialogInterface) {
                    mapScrollingEnabled = true;
                  }
                });
                alertDialog.show();


              }

            }
          });


          // permanent permisson or onetime permission for outside area
          if (allowServerQueries || allowOneServerQuery) {

            // TODO add start value to only update not download all everytime. need per area lastUpdated in own db
            String areaQuery = areaString;

            String currentQuery = areaQuery; // TODO add startQuery when needed

            // area not the same as last update -> query server for data
            if (!lastArea.equals(currentQuery)) {

              if(timeBasedQuery) {
                // area already visited and therefore in db, only query for updates since last "visit"

                // don't query if last visit less than 1 min ago
                Date dontQueryInTimeframe = new Date(System.currentTimeMillis() - 1000*60);

                if (latestUpdateForArea.before(dontQueryInTimeframe)) {
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

        } else if (offlineMode && outsideOfflineArea) { // not in homezone but offline mode is on
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
   * @param d1 day 1
   * @param d2 day 2
   * @return interval between d1 and d2 in days
   */

  // TODO test this
  public int daysBetween(Date d1, Date d2){
    return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
  }

  /**
   * populates and updates local variables with new data
   * @param queryDB should the local database be queried
   */
  private void updateAllCamerasInArea(boolean queryDB){
    allCamerasInArea.clear();

    if (queryDB) {
      allCamerasInAreaFromDb = synchronizedCameraRepository.getSynchronizedCamerasInAreaAsync(latMin, latMax, lonMin, lonMax);
    }

    allCamerasInArea.addAll(allCamerasInAreaFromDb);
    allCamerasInArea.addAll(camerasNotInDb);
  }

  /**
   * Draws camera markers on the map.
   * @param camerasToDisplay list of SynchronizedCameras to display
   */
  private void redrawMarkers(List<SynchronizedCamera> camerasToDisplay) {

    itemsToDisplay.clear();
    overlayItemsToDisplay.clear();

    timemachineView = findViewById(R.id.map_timemachine_seekbar);

    // TODO different markers for different types
    Drawable cameraMarkerIcon = getDrawable(R.drawable.simple_marker_5dpi);
    //Drawable clusterCameraMarkerIcon = ResourcesCompat.getDrawableForDensity(getResources(), R.drawable.standard_camera_marker_5_dpi, 400, null);
    Drawable clusterCameraMarkerIcon = getDrawable(R.drawable.standard_camera_marker_15dpi);

    //Drawable cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(view.getContext().getResources(), R.drawable.ic_close_red_24dp, 12, null);

    refreshSharedPreferencesObject();
    boolean clusteringEnabled = sharedPreferences.getBoolean("clusteringEnabled", true);

    // normal behaviour if timemachine not active. clusters cameras
    // clustering is disabled and the setting for it removed since there is a bug in the library

    // TODO fix conditions
    if (timemachineView == null && clusteringEnabled) {

      mapView.getOverlays().remove(cameraCluster);
      mapView.getOverlays().remove(cameraOverlay);

      cameraCluster = new RadiusMarkerClusterer(this);

      cameraCluster.setRadius(150);
      cameraCluster.setMaxClusteringZoomLevel(14);
      itemsToDisplay.addAll(camerasToDisplay);


      // Drawable clusterIconDrawable = getDrawable(R.drawable.ic_brightness_1_red_24dp);
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
        cameraMarker.setInfoWindow(new CustomInfoWindow(mapView, sharedPreferences, synchronizedCameraRepository));
        cameraMarker.setPanToView(false);
        //cameraMarker.setTitle(camerasToDisplay.get(i).getComments());

        cameraCluster.add(cameraMarker);
      }

      mapView.getOverlays().add(cameraCluster);
      mapView.invalidate();


    // timemachine != null || !clusteringEnabled
    } else {


      final int maxMarkersOnMap = Integer.parseInt(sharedPreferences.getString("maxMapMarkers", "400"));

      mapView.getOverlays().remove(cameraCluster);
      mapView.getOverlays().remove(cameraOverlay);

      if (timemachineView != null) {
        // clustering slows down timemachine immensely, use itemized overlay instead
        try{
          // display only cameras between seekbar max amount in the past and current seekbardate chosen by user
          // if seekbar is set to last week, only cameras added less than a week ago will be shown

          Iterator<SynchronizedCamera> iter = camerasToDisplay.iterator();

          int counter = 0;

          while (iter.hasNext()) {
            SynchronizedCamera nextCamera = iter.next();
            Date cameraUploaded = timestampIso8601DaysAccuracy.parse(nextCamera.getUploadedAt());
            if (cameraUploaded.before(currentSeekBarDate) && cameraUploaded.after(timemachineMaxInterval)) {

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

          int cameraType = camera.getType();

          // TODO switch case here

          // if filters are active only add corresponding cameras to items to display
          if (showStandardCameras && cameraType == StorageUtils.FIXED_CAMERA){

            overlayItemsToDisplay.add(new OverlayItem(
                    camera.getExternalID(),
                    "test_camera",
                    camera.getComments(),
                    new GeoPoint(camera.getLatitude(), camera.getLongitude()
                    )));

            counter++;
          }

          if (showDomeCameras && cameraType == StorageUtils.DOME_CAMERA){

            overlayItemsToDisplay.add(new OverlayItem(
                    camera.getExternalID(),
                    "test_camera",
                    camera.getComments(),
                    new GeoPoint(camera.getLatitude(), camera.getLongitude()
                    )));

            counter++;
          }

          if (showUnknownCameras && cameraType == StorageUtils.PANNING_CAMERA){

            overlayItemsToDisplay.add(new OverlayItem(
                    camera.getExternalID(),
                    "test_camera",
                    camera.getComments(),
                    new GeoPoint(camera.getLatitude(), camera.getLongitude()
                    )));

            counter++;
          }

          if (counter > maxMarkersOnMap) {
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                Toast.makeText(getApplicationContext(), maxMarkersOnMap
                                + " markers reached, please zoom in or increase amount in settings.",
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

                // marker gets touch
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
                      // infoComment = infoWindow.getView().findViewById(R.id.info_comment);

                      File thumbnail = new File(picturesPath + highlightedCamera.getImagePath());

                      Picasso.get().load(thumbnail)
                              .placeholder(R.drawable.ic_file_download_grey_48dp)
                              .into(infoImage);

                      // TODO disable if image already present
                      // download image
                      infoImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                          String baseUrl = sharedPreferences.getString("synchronizationUrl", null);
                          SynchronizationUtils.downloadImagesFromServer(
                                  baseUrl,
                                  highlightedCamera.getExternalID());
                          highlightedCamera.setImagePath(highlightedCamera.getExternalID() + ".jpg");
                          synchronizedCameraRepository.update(highlightedCamera);

                          Handler handler = new Handler();
                          handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                              File updatedThumbnail = new File(picturesPath + highlightedCamera.getImagePath());

                              Picasso.get().load(updatedThumbnail)
                                      .placeholder(R.drawable.ic_do_not_disturb_red_48dp)
                                      .into(infoImage);

                            }
                          }, 1000);


                        }
                      });


                      //infoLatestTimestamp.setText(highlightedCamera.getLastUpdated());
                      infoLatestTimestamp.setText(getString(R.string.window_upload_text, highlightedCamera.getUploadedAt()));
                      //infoComment.setText(highlightedCamera.getComments());

                    }

                    @Override
                    public void onClose() {

                    }
                  };

                  infoWindow.open(cameraItem, markerLocation, 0, 0);
                  return true; // We 'handled' this event.
                }

                @Override
                public boolean onItemLongPress(final int index, final OverlayItem cameraItem) {
                  return false;
                }
              }, getApplicationContext());

      mapView.getOverlays().add(cameraOverlay);
      mapView.invalidate();
    }

  }


  /**
   *
   * @param areaString url query string i.e. "area=8.2699,50.0201,8.2978,50.0005&start=2018-01-01"
   */
  void queryServerForCameras(String areaString) {

    // aborts current query if API key expired. starts same query after a new API key is aquired
    refreshSharedPreferencesObject();


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
    String baseUrl = sharedPreferences.getString("synchronizationUrl", null);

    String[] borders = areaString.split(",");

    double queryLatMin = Double.valueOf(borders[0]);
    double queryLatMax = Double.valueOf(borders[1]);
    double queryLonMin = Double.valueOf(borders[2]);
    double queryLonMax = Double.valueOf(borders[3]);

    String comepleteUrl = String.format(baseUrl + "data=[out:json];node[man_made=surveillance](%s,%s,%s,%s);out meta;", queryLatMin, queryLonMin, queryLatMax, queryLonMax);

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            comepleteUrl,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                List<SynchronizedCamera> camerasToSync = new ArrayList<>();

                JSONObject cameraJSON;

                try {

                  String osm_db_timestamp = response.getJSONObject("osm3s").getString("timestamp_osm_base");

                  for (int i = 0; i < response.getJSONArray("elements").length(); i++) {

                    cameraJSON = new JSONObject(String.valueOf(response.getJSONArray("elements").get(i)));
                    String timestamp = cameraJSON.getString("timestamp");

                    JSONObject tags = cameraJSON.getJSONObject("tags");

                    int type = 0; // default for fixed camera
                    int area = 0; // outdoor
                    int direction = -1; // unknown
                    int mount = 0; // unknown
                    int height = -1; // unknown
                    int angle = -1; // unknown

                    List<String> tagsAvailable = new ArrayList<>();

                    // loop through tag keys for more information: area, angle, height etc.
                    Iterator<String> iterTags = tags.keys();
                    while (iterTags.hasNext()) {
                      String key = iterTags.next();
                      tagsAvailable.add(key);
                    }

                    for (String tag : tagsAvailable){

                      try {

                        switch (tag) {

                          case "surveillance":
                            area = StorageUtils.areaList.indexOf(tags.getString(tag));
                            break;

                          case "camera:type":
                            type = StorageUtils.typeList.indexOf(tags.getString(tag));
                            break;

                          case "camera:mount":
                            mount = StorageUtils.mountList.indexOf(tags.getString(tag));
                            break;

                          case "camera:direction":
                            direction = tags.getInt("camera:direction");
                            break;

                          case "height":
                            height = tags.getInt("height");
                            break;
                        }

                      } catch (Exception ex) {
                        Log.i(TAG, "Error creating value from overpass api response: " + ex.toString());
                        continue;
                      }



                    }

                    SynchronizedCamera cameraToAdd = new SynchronizedCamera(
                            cameraJSON.getString("id") + ".jpg",
                            cameraJSON.getString("id"),
                            type,
                            area,
                            mount,
                            direction,
                            height,
                            angle,
                            cameraJSON.getDouble("lat"),
                            cameraJSON.getDouble("lon"),
                            "",
                            osm_db_timestamp,
                            timestamp,
                            false


                    );

                    camerasToSync.add(cameraToAdd);

                  }


                  synchronizedCameraRepository.insertAll(camerasToSync);


                } catch (Exception e) {
                  Log.i(TAG, "onResponse: " + e.toString());

                }

              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // TODO: Handle Errors
        Log.i(TAG, "Error in connection " + error.toString());
      }
    }) {
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {

        Map<String, String> headers = new HashMap<>();
        // headers.put("Content-Type", "application/json");

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
        synchronizedCameraRepository.insertAll(camerasNotInDb);

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