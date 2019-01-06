package org.tensorflow.demo;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
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

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
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
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import static org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn;


public class DebugActivity extends AppCompatActivity {

  private String TAG = "DebugActivity";

  private BottomNavigationView bottomNavigationView;
  private JSONObject JSONToSynchronize;
  private SynchronizedCamera cameraToAdd;

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

  private List<SurveillanceCamera> allCamerasInArea;
  private List<SynchronizedCamera> camerasToSync;

  private PendingIntent pendingSyncIntent;
  private AlarmManager syncAlarmManager;




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

    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    notificationPreference = sharedPreferences.getBoolean("notifications", false);
    debugTextView.setText(String.valueOf(notificationPreference));

    sharedPreferences.edit().putString("lastUpdated", "2018-01-01T13:00+0000").apply();
    sharedPreferences.edit().putLong("synchronizationInterval", 15*60*1000).apply();
    sharedPreferences.edit().putString("synchronizationUrl", "http://192.168.2.159:5000/cameras/?").apply();
    sharedPreferences.edit().putString("area", "8.2699,50.0201,8.2978,50.0005").apply();


    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    mapView = findViewById(R.id.debug_map);

    mapView.setTilesScaledToDpi(true);
    mapView.setClickable(true);

    //enable pinch to zoom
    mapView.setBuiltInZoomControls(true);
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
    }, 50)); // delay in ms after zooming/scrolling


    debugDbSync.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        //new DbAsyncTask(getApplication(), SYNC_DB).execute();

        RequestQueue mRequestQueue;

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();

        //String url = "http://192.168.2.159:5000/cameras/?area=8.2699,50.0201,8.2978,50.0005";
        String url = sharedPreferences.getString("synchronizationAddress", "") + "/cameras/?area=8.2699,50.0201,8.2978,50.0005";

        camerasToSync = new ArrayList<SynchronizedCamera>();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                  @Override
                  public void onResponse(JSONObject response) {
                    //debugTextView.setText("Response: " + response.toString());

                    try {


                      for (int i = 0; i < response.getJSONArray("cameras").length(); i++) {
                        JSONToSynchronize = new JSONObject(String.valueOf(response.getJSONArray("cameras").get(i)));

                        cameraToAdd = new SynchronizedCamera(JSONToSynchronize.getString("image_url"),
                                JSONToSynchronize.getDouble("lat"),
                                JSONToSynchronize.getDouble("lon"),
                                JSONToSynchronize.getString("comments"),
                                JSONToSynchronize.getString("last_updated")


                        );

                        camerasToSync.add(cameraToAdd);

                        // TODO same db entry just gets appended. Check if already there. time based? value based?
                        //new checkDbAsyncTask(getApplication()).execute();


                      }


                      synchronizedCameraRepository.insert(camerasToSync);


                    } catch (Exception e) {
                      Log.i(TAG, "onResponse: " + e.toString());
                      debugTextView.setText(e.toString());

                    }


                  }
                }, new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            // TODO: Handle Errors
            debugTextView.setText(error.toString());
          }
        }
        );

        mRequestQueue.add(jsonObjectRequest);


      }
    });

    debugDbCheck.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        new DbAsyncTask(getApplication(), CHECK_DB_SIZE).execute();

        if (syncAlarmManager != null) {
          syncAlarmManager.cancel(pendingSyncIntent);

        }

      }
    });

    debugDbDelete.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        new DbAsyncTask(getApplication(), DELETE_DB).execute();

      }
    });

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
    Random rng = new Random();
    String picturesPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/";



    SurveillanceCamera surveillanceCamera1 = new SurveillanceCamera(
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99452, 8.24688,
            10.3345, 3.1414 - 3.14/10, 12.3313, 170.3332,
            "no comment",
            timestampIso8601.format(new Date(System.currentTimeMillis() - rng.nextInt(1000*60*60*24*24))),
            timestampIso8601.format(new Date(System.currentTimeMillis() + rng.nextInt(1000*60*60*24))));


    SurveillanceCamera surveillanceCamera2 = new SurveillanceCamera(
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            49.99455, 8.24705,
            10.3345, -3.1414 + 3.14/10, 12.3313, 170.3332,
            "no comment",
            timestampIso8601.format(new Date(System.currentTimeMillis() - rng.nextInt(1000*60*60*24*24))),
            timestampIso8601.format(new Date(System.currentTimeMillis() + rng.nextInt(1000*60*60*24))));


    List<SurveillanceCamera> captureListTest = Arrays.asList(surveillanceCamera1, surveillanceCamera2);

    Location testEstimate = estimateCameraLocationFromCaptures(captureListTest);

    SurveillanceCamera surveillanceCamera3 = new SurveillanceCamera(
            picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
            10, 120, 50, 140,
            testEstimate.getLatitude(), testEstimate.getLongitude(),
            10.3345, - 3.14/2, 12.3313, 170.3332,
            "no comment",
            timestampIso8601.format(new Date(System.currentTimeMillis() - rng.nextInt(1000*60*60*24*24))),
            timestampIso8601.format(new Date(System.currentTimeMillis() + rng.nextInt(1000*60*60*24))));


    allCamerasInArea = Arrays.asList(surveillanceCamera1, surveillanceCamera2, surveillanceCamera3);

    reloadMarker();


    // reoccuring task

    Synchronization.scheduleSyncIntervalJob(getApplicationContext(), null);


    debugAlarm.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        JobScheduler jobScheduler = getApplicationContext().getSystemService(JobScheduler.class);

        List<JobInfo> allJobsPending = jobScheduler.getAllPendingJobs();

        //debugTextView.setText(String.valueOf(allJobsPending.get(0).getFlexMillis()));

      }
    });




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
    private List<SynchronizedCamera> allSynchronizedCameras;
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

  public Location estimateCameraLocationFromCaptures(List<SurveillanceCamera> recordedCameras) {

    Location estimatedCameraLocation;

    SurveillanceCamera camera1 = recordedCameras.get(0);
    SurveillanceCamera camera2 = recordedCameras.get(1);

    Location camera1Location = new Location("camera1");
    Location camera2Location = new Location("camera2");

    camera1Location.setLatitude(camera1.getLatitude());
    camera1Location.setLongitude(camera1.getLongitude());

    camera2Location.setLatitude(camera2.getLatitude());
    camera2Location.setLongitude(camera2.getLongitude());

    // Approximate in 2D coordinates in meters with straight lines as headings.

    // points
    double x1;
    double y1;

    double x2;
    double y2;

    // slope
    double m1;
    double m2;

    // y intersect
    double c1;
    double c2;

    // calculate slope with cotangent
    m1 = Math.cos(camera1.getAzimuth()) / Math.sin(camera1.getAzimuth());
    m2 = Math.cos(camera2.getAzimuth()) / Math.sin(camera2.getAzimuth());


    // camera1 in approximation is positioned at (0|0)
    x1 = 0;
    y1 = 0;

    c1 = 0;


    // distance to another capture in m
    float distanceToSecondCapture = camera1Location.distanceTo(camera2Location);

    // bearing to another capture in degrees east from true north
    float bearingToSecondCapture = camera1Location.bearingTo(camera2Location);


    // approximate second point in 2D coordinates

    // 1 degree of latitude is equal to different distances depending on longitude.
    // 1 degree equals 110574 m at the equator. Doesn't change much further north/south
    y2 = (camera2.getLatitude() - camera1.getLatitude())*110574;

    // get x2 from pythagoras with distance and y distance.
    x2 = Math.sqrt(Math.pow(distanceToSecondCapture, 2) - Math.pow(y2, 2));





    c2 = (m2*-x2) - y2;


    // intersect coordinates

    double intersectX = (c2-c1) / (m1-m2);
    double intersectY = m1*intersectX + c1;


    estimatedCameraLocation = metersFromLocationToLocation(intersectY, intersectX,
            camera1.getLatitude(), camera1.getLongitude());



    return estimatedCameraLocation;

  }



  public double longitudeDegreeToMetersRatio(double latitude) {
    // in m
    int earthRadius = 6371000;

    return Math.PI/180*earthRadius*Math.cos(Math.toRadians(latitude));
  }

  public Location metersFromLocationToLocation(double metersNorth, double metersEast,
                                               double latStart, double lonStart) {

    double latDiff = metersNorth / 110574;
    double lonDiff = metersEast / longitudeDegreeToMetersRatio(latStart);

    Location updatedLocation = new Location("locationFromLocationAndMeters");

    updatedLocation.setLatitude(latStart + latDiff);
    updatedLocation.setLongitude(lonStart + lonDiff);

    return updatedLocation;

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

  private class BackgroundMarkerLoaderTask extends AsyncTask<Double, Integer, List<SurveillanceCamera>> {

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
    protected List<SurveillanceCamera> doInBackground(Double... params) {
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
          itemsToDisplay.add(new OverlayItem(String.valueOf(i), "test_camera", allCamerasInArea.get(i).getComment(), new GeoPoint(allCamerasInArea.get(i).getLatitude(), allCamerasInArea.get(i).getLongitude())));

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
    protected void onPostExecute(List<SurveillanceCamera> camerasToDisplay) {
      if (!isCancelled() && (camerasToDisplay != null)) {

        mapView.getOverlays().remove(cameraOverlay);
        mapView.invalidate();

        for (int i = 0; i < camerasToDisplay.size(); i++) {
          itemsToDisplay.add(new OverlayItem("test_camera", camerasToDisplay.get(i).getComment(), new GeoPoint(camerasToDisplay.get(i).getLatitude(), camerasToDisplay.get(i).getLongitude())));
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
                        infoEscape = infoWindow.getView().findViewById(R.id.info_escape_button);

                        File thumbnail = new File(allCamerasInArea.get(cameraIndex).getThumbnailPath());
                        Picasso.get().load(thumbnail)
                                .into(infoImage);
                        infoLatestTimestamp.setText(allCamerasInArea.get(cameraIndex).getTimestamp());
                        infoComment.setText(allCamerasInArea.get(cameraIndex).getComment());

                        infoEscape.setImageResource(R.drawable.ic_close_red_24dp);
                        infoEscape.setOnClickListener(new View.OnClickListener() {
                          @Override
                          public void onClick(View view) {
                            infoWindow.close();
                          }});
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


