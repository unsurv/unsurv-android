package org.unsurv;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import static org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn;
import static org.unsurv.StorageUtils.SYNCHRONIZED_PATH;


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
  private List<SynchronizedCamera> camerasToSync = new ArrayList<>();

  private WifiManager wifiManager;

  private String picturesPath = SYNCHRONIZED_PATH;

  private int randomCamerasAdded;
  private List<SurveillanceCamera> allCameras = new ArrayList<>();
  private CameraRepository cameraRepository;

  private ProgressBar progress;
  private int imagesDownloaded;
  private int currentBatchSize;

  private LayoutInflater layoutInflater;
  private TextView progressPercentage;

  private boolean downloadStoppedByUser = false;

  private CameraViewModel cameraViewModel;

  private Context context;




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

      case R.id.action_training:
        Intent trainingCaptureIntent = new Intent(DebugActivity.this, CaptureTrainingImageActivity.class);
        startActivity(trainingCaptureIntent);

        return true;

      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }


  @Override
  protected void onResume() {
    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);

    super.onResume();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_debug);
    context = this;

    final String TAG = "DebugActivity";
    final TextView debugTextView = findViewById(R.id.debug_textview);
    final Button debugDbSync = findViewById(R.id.sync_db);
    final Button debugDbCheck = findViewById(R.id.check_db);
    final Button debugDbDelete = findViewById(R.id.delete_db);
    final Button debugAlarm = findViewById(R.id.alarm_test);
    final Button debugTutorial = findViewById(R.id.start_tutorial);
    final Button debugCheckJobs = findViewById(R.id.check_jobs);
    final Button debugShowPrefs = findViewById(R.id.show_preferences);
    final Button addCam = findViewById(R.id.add_surveilllance_camera);
    final Button uploadCameras = findViewById(R.id.upload_cameras);
    final Button getKeyButton = findViewById(R.id.get_key);
    final Button abortSyncButton = findViewById(R.id.abort_job);
    final Button drawButton = findViewById(R.id.draw);

    cameraRepository = new CameraRepository(getApplication());
    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());
    cameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

    randomCamerasAdded = 0;
    imagesDownloaded = 0;

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    sharedPreferences.edit().clear().apply();

    sharedPreferences.edit().putBoolean("notifications", false).apply();

    sharedPreferences.edit().putString("lastUpdated", "2018-01-01").apply();
    sharedPreferences.edit().putString("synchronizationInterval", String.valueOf(1000*60*15)).apply();
    sharedPreferences.edit().putString("synchronizationUrl", "https://api.unsurv.org/").apply();
    // sharedPreferences.edit().putString("synchronizationUrl", "http://192.168.178.137:5000/").apply();
    sharedPreferences.edit().putString("area", "49.6391,50.3638,7.8648,8.6888").apply();
    sharedPreferences.edit().putBoolean("buttonCapture", false).apply();
    sharedPreferences.edit().putBoolean("offlineMode", false).apply();
    sharedPreferences.edit().putBoolean("allowServerQueries", false).apply();
    sharedPreferences.edit().putBoolean("clusteringEnabled", false).apply();
    sharedPreferences.edit().putString("apiKey", "abc").apply();
    sharedPreferences.edit().putString("apiKeyExpiration", "2018-01-01 00:00:00").apply();
    sharedPreferences.edit().putBoolean("showCaptureTimestamps", false).apply();
    sharedPreferences.edit().putBoolean("deleteOnUpload", false).apply();
    sharedPreferences.edit().putBoolean("quickDeleteCameras", false).apply();
    sharedPreferences.edit().putBoolean("downloadImages", true).apply();
    sharedPreferences.edit().putBoolean("alwaysEnableManualCapture", false).apply();
    sharedPreferences.edit().putInt("bottomNavigationHistoryBadgeCount", 0).apply();
    sharedPreferences.edit().putInt("bottomNavigationMapBadgeCount", 0).apply();




    sharedPreferences.edit().putInt("minUploadDelay", 86400).apply(); // multiply by 1000 when called to get real value in ms
    sharedPreferences.edit().putInt("maxUploadDelay", 604800).apply();

    // SynchronizationUtils.getAPIkey(DebugActivity.this, sharedPreferences);
    // set in timemachineSpinner
    // sharedPreferences.edit().putInt("timemachineValue", null).apply();

    progress = new ProgressBar(DebugActivity.this);

    layoutInflater = (LayoutInflater) DebugActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);



    notificationPreference = sharedPreferences.getBoolean("notifications", false);
    debugTextView.setText(String.valueOf(notificationPreference));

    wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


    androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    mapView = findViewById(R.id.debug_map);

    mapView.setTilesScaledToDpi(true);
    mapView.setClickable(true);


    // MAPNIK fix
    // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");

    mapView.setTileSource(TileSourceFactory.OpenTopo);


    //enable pinch to zoom
    mapView.setMultiTouchControls(true);

    final IMapController mapController = mapView.getController();

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(50.0027, 8.2771);
    mapController.setZoom(14.0);
    mapController.setCenter(startPoint);


    SynchronizedCamera debugCamera = new SynchronizedCamera(
            "test_pixel_2.jpg",
            "asd",
            2,
            49.99819,
            8.25949,
            "no comments",
            "2019-05-30",
            "2019-05-30",
            false

    );

    synchronizedCameraRepository.insert(Collections.singletonList(debugCamera));


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

        String baseURL = sharedPreferences.getString("synchronizationUrl", null);
        String homeArea = sharedPreferences.getString("area", null);


        SynchronizationUtils.downloadCamerasFromServer(
                baseURL,
                "area=" + homeArea,
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

        List<JobInfo> jobs = jobScheduler.getAllPendingJobs();

        debugTextView.setText(jobs.toString());
      }
    });

    debugShowPrefs.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        debugTextView.setText(
                sharedPreferences.getString("lastUpdated", "2018-01-01") + "\n" +
                sharedPreferences.getString("synchronizationInterval", String.valueOf(15*60*1000)) + "\n" +
                sharedPreferences.getString("synchronizationUrl", "http://192.168.2.159:5000/cameras/?") + "\n" +
                sharedPreferences.getString("area", "49.6391,50.3638,7.8648,8.6888") + "\n" +
                sharedPreferences.getBoolean("buttonCapture", false) + "\n" +
                sharedPreferences.getString("timemachineValueInDays", "0") + "\n" +
                sharedPreferences.getString("apiKey", "ApiKeyFailed") + "\n" +
                sharedPreferences.getString("apiKeyExpiration", "ApiKeyFailed"));

      }
    });

    addCam.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        Random random = new Random();
        String nullOrDate;

        if (random.nextFloat() < 0.5) {
          nullOrDate = null;
        } else {
          nullOrDate = "2018-01-02";
        }

        // 50.000 * random.nextFloat(),
        // 8.0000 * random.nextFloat(),


        SurveillanceCamera randomCamera = new SurveillanceCamera(
                0,
                "test_pixel_2.jpg",
                "test_nexus_10.jpg",
                null,
                50.00165,
                8.25941,
                "asd",
                nullOrDate,
                "2019-05-30",
                false,
                false,
                false,
                false,
                "",
                "\"[test_pixel_2.jpg,1562076737965_thumbnail.jpg,1562076726264_thumbnail.jpg]\"");

        SurveillanceCamera randomCamera2 = new SurveillanceCamera(
                1,
                "test_pixel_2.jpg",
                "test_nexus_10.jpg",
                null,
                50.00165,
                8.25941,
                "asd",
                nullOrDate,
                "2019-05-30",
                false,
                false,
                false,
                false,
                "",
                "\"[test_pixel_2.jpg,1562076737965_thumbnail.jpg,1562076726264_thumbnail.jpg]\"");

        SurveillanceCamera notRandomCamera = new SurveillanceCamera(
                0,
                "test_pixel_2.jpg",
                "asd.jpg",
                null,
                0, 0,
                "",
                "2018-01-01",
                "2018-02-02",
                false,
                false,
                false,
                true,
                "[{\"1\":\"420 709 863 1069\"},{\"0\":\"859 656 1432 1044\"},{\"0\":\"81 1018 460 1334\"},{\"0\":\"48 678 385 1049\"}]",
                "");

        SynchronizedCamera debugCamera1 = new SynchronizedCamera(
                "test_pixel_2.jpg",
                "asdf",
                0,
                49.99827,
                8.28407,
                "no comments",
                "2019-05-30",
                "2019-05-30",
                false

        );

        SynchronizedCamera debugCamera2 = new SynchronizedCamera(
                "test_pixel_2.jpg",
                "asdfg",
                1,
                49.9976,
                8.28489,
                "no comments",
                "2019-05-30",
                "2019-05-30",
                false

        );

        SynchronizedCamera debugCamera3 = new SynchronizedCamera(
                "test_pixel_2.jpg",
                "asdfgh",
                2,
                49.9969,
                8.28570,
                "no comments",
                "2019-05-30",
                "2019-05-30",
                false

        );

        // synchronizedCameraRepository.deleteAll();

        synchronizedCameraRepository.insert(Collections.singletonList(debugCamera1));
        synchronizedCameraRepository.insert(Collections.singletonList(debugCamera2));
        synchronizedCameraRepository.insert(Collections.singletonList(debugCamera3));


        cameraRepository.insert(randomCamera);
        cameraRepository.insert(randomCamera2);
        BottomNavigationBadgeHelper.incrementBadge(bottomNavigationView, context, R.id.bottom_navigation_history, 2);

        // cameraRepository.insert(notRandomCamera);


        randomCamerasAdded += 1;

        debugTextView.setText(randomCamerasAdded + " random cameras added");

      }
    });

    uploadCameras.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        allCameras = cameraRepository.getAllCameras();
        String url = sharedPreferences.getString("synchronizationUrl", null);
        SynchronizationUtils.uploadSurveillanceCamera(allCameras, url, sharedPreferences, cameraViewModel, null, false);

        List<String> externalIds = new ArrayList<>();
        String externalId = "145432e0e9c54d0d";
        //externalIds.add(externalId);
        //SynchronizationUtils.downloadImages(url + "images/", externalIds, sharedPreferences);

      }
    });

    getKeyButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        SynchronizationUtils.getAPIkey(DebugActivity.this, sharedPreferences);
      }
    });

    abortSyncButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        JobScheduler jobScheduler = getApplicationContext().getSystemService(JobScheduler.class);

        List<JobInfo> allJobsPending = jobScheduler.getAllPendingJobs();

        jobScheduler.cancel(0);

      }
    });

    drawButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent drawIntent = new Intent(DebugActivity.this, DrawOnTrainingImageActivity.class);
        startActivity(drawIntent);
      }
    });


    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));




    CameraCapture cameraCapture1 = new CameraCapture(0, 99.9f,
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99452, 8.24688,
            10.3345, 3.1414 - 3.14/10, 12.3313, 170.3332);

    CameraCapture cameraCapture2 = new CameraCapture(0, 99.9f,
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99455, 8.24705,
            10.3345, -3.1414 + 3.14/10, 12.3313, 170.3332);

    CameraCapture cameraCapture3 = new CameraCapture(0, 99.9f,
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99458, 8.24725,
            10.3345, -3.1414 + 3.14/10, 12.3313, 170.3332);

    CameraCapture cameraCapture4 = new CameraCapture(0, 99.9f,
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99455, 8.24740,
            10.3345, -3.1414 + 3.14/10, 12.3313, 170.3332);

    List<CameraCapture> captureListTest = Arrays.asList(cameraCapture1, cameraCapture2, cameraCapture3, cameraCapture4);

    SurveillanceCamera testCamera = new SurveillanceCamera(
            0,
            "test_pixel_2.jpg",
            "asd",
            null,
            50.000,
            8.0000,
            "asd",
            null,
            "2019-05-30",
            false,
            false,
            false,
            false,
            null,
            "");

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
            if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
              Intent manualCaptureIntent = new Intent(DebugActivity.this, ManualCaptureActivity.class);
              startActivity(manualCaptureIntent);
              return true;
            } else {
              Intent cameraIntent = new Intent(DebugActivity.this, DetectorActivity.class);
              startActivity(cameraIntent);
              return true;
            }

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


    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, this);

  }




  private class DbAsyncTask extends AsyncTask<Void, Void, Void> {

    final TextView debugTextView = findViewById(R.id.debug_textview);
    private SynchronizedCameraRepository synchronizedCameraRepository;
    private int dbSize;
    private int MODE;

    private int CHECK_DB_SIZE = 0;
    private int DELETE_DB = 1;

    private String TAG = "checkDbAsync";


    DbAsyncTask(Application application, int DbMode) {
      synchronizedCameraRepository = new SynchronizedCameraRepository(application);
      MODE = DbMode;
    }


    @Override
    protected Void doInBackground(Void... voids) {

      //synchronizedCameraRepository.deleteAll();

      if (MODE == CHECK_DB_SIZE) {
        dbSize = synchronizedCameraRepository.getAllSynchronizedCameras(false).size();
        Log.i(TAG, "doInBackground: " + dbSize);
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


  void downloadCamerasFromServerWithProgressBar(String baseUrl, String areaQuery, final SharedPreferences sharedPreferences, final boolean insertIntoDb, @Nullable String startQuery, @Nullable SynchronizedCameraRepository synchronizedCameraRepository){

    //TODO check api for negative values in left right top bottom see if still correct

    camerasToSync.clear();

    final SynchronizedCameraRepository crep = synchronizedCameraRepository;

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    String URL = baseUrl  + "cameras/?" + areaQuery;

    if (startQuery != null) {
      URL += "&" + startQuery;
    }

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            URL,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {


                JSONObject JSONToSynchronize;

                try {

                  for (int i = 0; i < response.getJSONArray("cameras").length(); i++) {
                    JSONToSynchronize = new JSONObject(String.valueOf(response.getJSONArray("cameras").get(i)));

                    SynchronizedCamera cameraToAdd = new SynchronizedCamera(
                            null,
                            JSONToSynchronize.getString("id"),
                            JSONToSynchronize.getInt("type"),
                            JSONToSynchronize.getDouble("lat"),
                            JSONToSynchronize.getDouble("lon"),
                            JSONToSynchronize.getString("comments"),
                            JSONToSynchronize.getString("lastUpdated"),
                            JSONToSynchronize.getString("uploadedAt"),
                            false

                    );

                    camerasToSync.add(cameraToAdd);

                  }

                  if (insertIntoDb) {
                    crep.insert(camerasToSync);
                  }

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

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {

      startImageDownloadWithProgressBar();

      }
    });

    mRequestQueue.add(jsonObjectRequest);

  }


  void downloadImagesFromServer(String baseUrl, final List<String> externalIds, final SynchronizedCameraRepository synchronizedCameraRepository, final SharedPreferences sharedPreferences) {

    JSONObject postObject = new JSONObject();

    JSONArray ids = new JSONArray();

    for (String id : externalIds) {
      ids.put(id);
    }

    try {

      postObject.put("ids", ids);

    } catch (JSONException jse){
      Log.i(TAG, "downloadImages: " + jse.toString());
    }

    String url = baseUrl + "images/";

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.POST,
            url,
            postObject,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                try {

                  for (String id : externalIds) {

                    String base64Image = response.getString(id);

                    byte[] imageAsBytes = Base64.decode(base64Image, Base64.DEFAULT);

                    StorageUtils.saveBytesToFile(imageAsBytes, id + ".jpg", SYNCHRONIZED_PATH);

                  }


                } catch (Exception e) {
                  Log.i(TAG, "response to jpg error: " + e.toString());
                }

              }
            },

            new Response.ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "downloadImages: " + error.toString());

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

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {
        Log.i(TAG, "post request finished");

        imagesDownloaded += externalIds.size();
        progress.setProgress(imagesDownloaded);
        int percentCompleted = 0;

        if (currentBatchSize != 0) {
          percentCompleted = (imagesDownloaded / currentBatchSize) * 100;
        }

        if (progressPercentage != null) {
          // is null if server sends error
          progressPercentage.setText(percentCompleted + " %");
        }

        for (String externalId : externalIds){

          SynchronizedCamera camera = synchronizedCameraRepository.findByID(externalId);
          camera.setImagePath(camera.getExternalID() + ".jpg");
          synchronizedCameraRepository.update(camera);

        }


      }
    });

    mRequestQueue.add(jsonObjectRequest);


  }

  void startImageDownloadWithProgressBar(){

    String baseURL = sharedPreferences.getString("synchronizationUrl", "http://192.168.2.137:5000/");

    imagesDownloaded = 0;

    List<String> idsForImageDownload = new ArrayList<>();

    for (SynchronizedCamera camera : camerasToSync){

      idsForImageDownload.add(camera.getExternalID());

    }

    int size = idsForImageDownload.size();
    currentBatchSize = size;

    progress.setMax(size);

    // split list in 10 "even" (add rest of division to last part) parts

    List<List<String>> splitLists = new ArrayList<>();

    int amountPerList = Math.floorDiv(size,  10);

    for (int i = 0; i < 10; i++) {

      int currentMinIndex = i * amountPerList;

      if (i != 9){
        splitLists.add(idsForImageDownload.subList(currentMinIndex, currentMinIndex + amountPerList));

      } else {
        splitLists.add(idsForImageDownload.subList(currentMinIndex, size));
      }
    }


    if (size > 50){

      View popupView = layoutInflater.inflate(R.layout.progress_bar_popup, null);

      Button cancelButton = popupView.findViewById(R.id.progress_popup_cancel_button);
      Button hideButton = popupView.findViewById(R.id.progress_popup_hide_button);
      progressPercentage = popupView.findViewById(R.id.progress_percentage_text);

      final PopupWindow popupWindow =
              new PopupWindow(popupView,
                      RecyclerView.LayoutParams.WRAP_CONTENT,
                      RecyclerView.LayoutParams.WRAP_CONTENT);

      if (!popupWindow.isShowing()) {

        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        cancelButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            downloadStoppedByUser = true;
            popupWindow.dismiss();

          }
        });


        hideButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {

            popupWindow.dismiss();

          }
        });
      }

      for (List<String> splitIds : splitLists) {

        if (!downloadStoppedByUser) {
          downloadImagesFromServer(baseURL,
                  splitIds,
                  synchronizedCameraRepository,
                  sharedPreferences);
        }

      }

    } else {
      downloadImagesFromServer(baseURL,
              idsForImageDownload,
              synchronizedCameraRepository,
              sharedPreferences);
    }



  }

}


