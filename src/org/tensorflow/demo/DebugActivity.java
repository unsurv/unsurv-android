package org.tensorflow.demo;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn;


public class DebugActivity extends AppCompatActivity {

  private String TAG = "DebugActivity";

  private BottomNavigationView bottomNavigationView;

  private SynchronizedCameraRepository synchronizedCameraRepository;

  private int CHECK_DB_SIZE = 0;
  private int DELETE_DB = 1;

  private SharedPreferences sharedPreferences;
  private Boolean notificationPreference;

  private MapView mapView;
  private ItemizedOverlay<OverlayItem> cameraOverlay;
  private ArrayList<OverlayItem> itemsToDisplay = new ArrayList<>();

  private InfoWindow infoWindow;
  private ImageView infoImage;
  private TextView infoLatestTimestamp;
  private TextView infoComment;
  private ImageButton infoEscape;

  private List<CameraCapture> allCamerasInArea;
  private List<SynchronizedCamera> camerasToSync;

  private WifiManager wifiManager;




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
        Intent settingsIntent = new Intent(DebugActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

        return true;



      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }



  //TODO ADD REFRESH BUTTON

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_debug);
    final String TAG = "DebugActivity";
    final TextView debugTextView = findViewById(R.id.debug_textview);
    final Button debugDbSync = findViewById(R.id.sync_db);
    final Button debugDbCheck = findViewById(R.id.check_db);
    final Button debugDbDelete = findViewById(R.id.delete_db);
    final Button debugAlarm = findViewById(R.id.alarm_test);
    final Button debugTutorial = findViewById(R.id.start_tutorial);
    final Button debugCheckJobs = findViewById(R.id.check_jobs);
    final Button debugShowPrefs = findViewById(R.id.show_preferences);

    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    sharedPreferences.edit().clear().apply();

    sharedPreferences.edit().putBoolean("notifications", false).apply();

    sharedPreferences.edit().putString("lastUpdated", "2018-01-01").apply();
    sharedPreferences.edit().putString("synchronizationInterval", String.valueOf(15*60*1000)).apply();
    sharedPreferences.edit().putString("synchronizationURL", "http://192.168.1.137:5000/").apply();
    sharedPreferences.edit().putString("area", "49.6391,50.3638,7.8648,8.6888").apply();
    sharedPreferences.edit().putBoolean("buttonCapture", false).apply();
    sharedPreferences.edit().putBoolean("offlineMode", false).apply();
    sharedPreferences.edit().putBoolean("allowServerQueries", true).apply();
    sharedPreferences.edit().putBoolean("askForConnections", true).apply();
    sharedPreferences.edit().putBoolean("clusteringEnabled", true).apply();
    sharedPreferences.edit().putString("apiKey", "abc").apply();
    sharedPreferences.edit().putString("apiKeyExpiration", "2018-01-01 00:00:00").apply();


    // sharedPreferences.edit().putLong("minUploadDelay", 1000*60*60*24*10).apply();
    // sharedPreferences.edit().putLong("maxUploadDelay", 1000*60*60*24*48).apply();


    // set in timemachineSpinner
    // sharedPreferences.edit().putInt("timemachineValue", null).apply();

    notificationPreference = sharedPreferences.getBoolean("notifications", false);
    debugTextView.setText(String.valueOf(notificationPreference));

    wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    mapView = findViewById(R.id.debug_map);

    mapView.setTilesScaledToDpi(true);
    mapView.setClickable(true);

    mapView.setTileSource(TileSourceFactory.OpenTopo);


    //enable pinch to zoom
    mapView.setMultiTouchControls(true);

    final IMapController mapController = mapView.getController();

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(50.0027, 8.2771);
    mapController.setZoom(14.0);
    mapController.setCenter(startPoint);



    mapView.addMapListener(new DelayedMapListener(new MapListener() {
      @Override
      public boolean onScroll(ScrollEvent event) {
        reloadMarker();
        closeAllInfoWindowsOn(mapView);
        return false;
      }

      @Override
      public boolean onZoom(ZoomEvent event) {
        reloadMarker();
        closeAllInfoWindowsOn(mapView);
        return false;
      }
    }, 50)); // delay for refresh in ms after zooming/scrolling


    debugDbSync.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        String baseURL = sharedPreferences.getString("synchronizationURL", "http://192.168.2.137:5000/");

        SynchronizationUtils.synchronizeCamerasWithServer(
                baseURL + "cameras/?",
                "area=" + sharedPreferences.getString("area", null),
                sharedPreferences,
                true,
                null,
                synchronizedCameraRepository);

      }
    });

    debugDbCheck.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        new DbAsyncTask(getApplication(), CHECK_DB_SIZE).execute();

      }
    });

    debugDbDelete.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        new DbAsyncTask(getApplication(), DELETE_DB).execute();

      }
    });

    debugAlarm.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        SynchronizationUtils.scheduleSyncIntervalJob(getApplicationContext(), null);

        JobScheduler jobScheduler = getApplicationContext().getSystemService(JobScheduler.class);

        List<JobInfo> allJobsPending = jobScheduler.getAllPendingJobs();

        SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));


      }
    });


    debugTutorial.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent tutorialIntent = new Intent(DebugActivity.this, TutorialActivity.class);
        startActivity(tutorialIntent);
      }
    });

    debugCheckJobs.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        JobScheduler jobScheduler = getApplicationContext().getSystemService(JobScheduler.class);

        List<JobInfo> b = jobScheduler.getAllPendingJobs();

        debugTextView.setText(b.toString());
      }
    });

    debugShowPrefs.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        debugTextView.setText(
                sharedPreferences.getString("lastUpdated", "2018-01-01") + "\n" +
                sharedPreferences.getString("synchronizationInterval", String.valueOf(15*60*1000)) + "\n" +
                sharedPreferences.getString("synchronizationURL", "http://192.168.2.159:5000/cameras/?") + "\n" +
                sharedPreferences.getString("area", "49.6391,50.3638,7.8648,8.6888") + "\n" +
                sharedPreferences.getBoolean("buttonCapture", false) + "\n" +
                sharedPreferences.getString("timemachineValueInDays", "0") + "\n" +
                sharedPreferences.getString("apiKey", "ApiKeyFailed") + "\n" +
                sharedPreferences.getString("apiKeyExpiration", "ApiKeyFailed"));

      }
    });


    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
    String picturesPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/";



    CameraCapture cameraCapture1 = new CameraCapture(99.9f,
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99452, 8.24688,
            10.3345, 3.1414 - 3.14/10, 12.3313, 170.3332);

    CameraCapture cameraCapture2 = new CameraCapture(99.9f,
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99455, 8.24705,
            10.3345, -3.1414 + 3.14/10, 12.3313, 170.3332);

    CameraCapture cameraCapture3 = new CameraCapture(99.9f,
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99458, 8.24725,
            10.3345, -3.1414 + 3.14/10, 12.3313, 170.3332);

    CameraCapture cameraCapture4 = new CameraCapture(99.9f,
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99455, 8.24740,
            10.3345, -3.1414 + 3.14/10, 12.3313, 170.3332);

    List<CameraCapture> captureListTest = Arrays.asList(cameraCapture1, cameraCapture2, cameraCapture3, cameraCapture4);

    SurveillanceCamera testCamera = new SurveillanceCamera(
            "asd",
            "asd",
            50.000,
            8.0000,
            "asd",
            "2019-01-03",
            "2019-05-30");
    CameraRepository cameraRepository = new CameraRepository(getApplication());
    cameraRepository.insert(testCamera);

    allCamerasInArea = captureListTest;

    reloadMarker();


    // reoccuring task


    /*
    final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
          Boolean scanComplete = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
          List<ScanResult> mScanResult = wifiManager.getScanResults();

          StringBuilder allNetworksAvailable = new StringBuilder();
          for (int i=0; i < mScanResult.size(); i++) {
            allNetworksAvailable.append(mScanResult.get(i).SSID + "\n");
          }

          debugTextView.setText(allNetworksAvailable.toString());
        }

      }
    };

    registerReceiver(mWifiScanReceiver,
            new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

    // wifiManager.startScan();
    int pasd = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    List<ScanResult> masdScanResult = wifiManager.getScanResults();


     */





    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(DebugActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            Intent cameraIntent = new Intent(DebugActivity.this, DetectorActivity.class);
            startActivity(cameraIntent);
            return true;

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(DebugActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(DebugActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

  }




  private class DbAsyncTask extends AsyncTask<Void, Void, Void> {

    final TextView debugTextView = findViewById(R.id.debug_textview);
    private SynchronizedCameraRepository synchronizedCameraRepository;
    private int dbSize;
    private int MODE;

    private int CHECK_DB_SIZE = 0;
    private int DELETE_DB = 1;

    private String TAG = "checkDbAsync";


    public DbAsyncTask(Application application, int DbMode) {
      synchronizedCameraRepository = new SynchronizedCameraRepository(application);
      MODE = DbMode;
    }


    @Override
    protected Void doInBackground(Void... voids) {

      //synchronizedCameraRepository.deleteAll();

      if (MODE == CHECK_DB_SIZE) {
        dbSize = synchronizedCameraRepository.getAllSynchronizedCameras().size();
        Log.i(TAG, "doInBackground: " + String.valueOf(synchronizedCameraRepository.getAllSynchronizedCameras().size()));
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            debugTextView.setText(String.valueOf(dbSize));
            Toast DbSizeToast = Toast.makeText(getApplicationContext(), String.valueOf(dbSize), Toast.LENGTH_SHORT);
            DbSizeToast.show();

          }
        });


      } else if (MODE == DELETE_DB) {
        synchronizedCameraRepository.deleteAll();

        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast DbSizeToast = Toast.makeText(getApplicationContext(), "DB RESET", Toast.LENGTH_SHORT);
            DbSizeToast.show();

          }
        });
      }

      return null;
    }
  }



  private DebugActivity.BackgroundMarkerLoaderTask mCurrentBackgroundMarkerLoaderTask = null;

  private void reloadMarker() {

    if (mCurrentBackgroundMarkerLoaderTask == null) {
      // start background load
      double zoom = this.mapView.getZoomLevelDouble();
      BoundingBox world = this.mapView.getBoundingBox();

      reloadMarker(world, zoom);
    }
  }


  private void reloadMarker(BoundingBox latLonArea, double zoom) {
    Log.d(TAG, "reloadMarker " + latLonArea + ", zoom " + zoom);
    this.mCurrentBackgroundMarkerLoaderTask = new DebugActivity.BackgroundMarkerLoaderTask();
    this.mCurrentBackgroundMarkerLoaderTask.execute(
            latLonArea.getLatSouth(), latLonArea.getLatNorth(),
            latLonArea.getLonEast(), latLonArea.getLonWest(), zoom);

  }

  private class BackgroundMarkerLoaderTask extends AsyncTask<Double, Integer, List<CameraCapture>> {

    /**
     * Computation of the map itmes in the non-gui background thread. .
     *
     * @param params latMin, latMax, lonMin, longMax, zoom.
     * @return List of Surveillance Cameras in the current Map window.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected List<CameraCapture> doInBackground(Double... params) {
      FolderOverlay result = new FolderOverlay();

      try {
        if (params.length != 5)
          throw new IllegalArgumentException("expected latMin, latMax, lonMin, longMax, zoom");

        int paramNo = 0;
        double latMin = params[paramNo++];
        double latMax = params[paramNo++];
        double lonMin = params[paramNo++];
        double lonMax = params[paramNo++];

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
        double zoom = params[paramNo++];

        Log.d(TAG, "doInBackground" +
                " latMin=" + latMin +
                " ,latMax=" + latMax +
                " ,lonMin=" + lonMin +
                " ,lonMax=" + lonMax +
                ", zoom=" + zoom);


        itemsToDisplay.clear();
        for (int i = 0; i < allCamerasInArea.size(); i++) {
          itemsToDisplay.add(new OverlayItem(String.valueOf(i), "test_camera", "no comment", new GeoPoint(allCamerasInArea.get(i).getLatitude(), allCamerasInArea.get(i).getLongitude())));

        }

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
    protected void onPostExecute(List<CameraCapture> camerasToDisplay) {
      if (!isCancelled() && (camerasToDisplay != null)) {

        mapView.getOverlays().remove(cameraOverlay);
        mapView.invalidate();

        for (int i = 0; i < camerasToDisplay.size(); i++) {
          itemsToDisplay.add(new OverlayItem("test_camera", "no comment", new GeoPoint(camerasToDisplay.get(i).getLatitude(), camerasToDisplay.get(i).getLongitude())));
        }

        Drawable customMarker = ResourcesCompat.getDrawableForDensity(getResources(), R.drawable.standard_camera_marker_5_dpi, 12, null);
        //TODO scaling marker
        cameraOverlay = new ItemizedIconOverlay<>(itemsToDisplay, customMarker,
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
                        int cameraIndex = Integer.parseInt(cameraItem.getUid());

                        infoWindow.setRelatedObject(allCamerasInArea.get(cameraIndex));

                        // Setting content for infoWindow.
                        infoImage = infoWindow.getView().findViewById(R.id.info_image);
                        infoLatestTimestamp = infoWindow.getView().findViewById(R.id.info_latest_timestamp);
                        infoComment = infoWindow.getView().findViewById(R.id.info_comment);


                        File thumbnail = new File(allCamerasInArea.get(cameraIndex).getThumbnailPath());
                        Picasso.get().load(thumbnail)
                                .into(infoImage);
                        infoLatestTimestamp.setText("cameraCaptures have no timestamp now");
                        infoComment.setText("no comment");

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
                            DebugActivity.this,
                            "Item '" + cameraItem.getTitle() + "' (index=" + index
                                    + ") got long pressed", Toast.LENGTH_LONG).show();
                    return false;
                  }
                }, getApplicationContext());
        mapView.getOverlays().add(cameraOverlay);

      }
      mCurrentBackgroundMarkerLoaderTask = null;
      // there was map move/zoom while {@link BackgroundMarkerLoaderTask} was active. must reload


    }
  }

}


