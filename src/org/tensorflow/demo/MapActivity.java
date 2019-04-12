package org.tensorflow.demo;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.res.ResourcesCompat;
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
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn;


public class MapActivity extends AppCompatActivity {
  public static final String TAG = "MapActivity";

  private MapView mapView;
  private ItemizedOverlay<OverlayItem> cameraOverlay;
  private MyLocationNewOverlay myLocationOverlay;

  private BottomNavigationView bottomNavigationView;

  private CameraViewModel cameraViewModel;

  private ArrayList<OverlayItem> itemsToDisplay = new ArrayList<>();
  private LiveData<List<SurveillanceCamera>> allCameras;
  private ImageButton myLocationButton;


  private SynchronizedCameraRepository synchronizedCameraRepository;

  private List<SynchronizedCamera> allCamerasInArea = new ArrayList<>();

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
  private View timemachineView;
  private View timeframeView;

  private TextView timeframeTextView;

  private Date timemachineMaxInterval;
  private Date currentSeekBarDate;
  private int daysBetween;

  private boolean isInitialSpinnerSelection;



  // TODO set max amount visible

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);
    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());


    mapView = findViewById(R.id.map);
    mapScrollingEnabled = true;
    isInitialSpinnerSelection = true;

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
        reloadMarker();
        closeAllInfoWindowsOn(mapView);
        return false;
      }
    }, 150)); // delay in ms after zooming/scrolling

    mapView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        reloadMarker();
        closeAllInfoWindowsOn(mapView);
      }
    });

    mapView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mapScrollingEnabled) {
          return false;
        } else {
          return true;
        }

      }
    });

    mapView.setTilesScaledToDpi(true);
    mapView.setClickable(true);

    //enable pinch to zoom
    mapView.setBuiltInZoomControls(true);
    mapView.setMultiTouchControls(true);

    mapView.setTileSource(TileSourceFactory.OpenTopo);

    final IMapController mapController = mapView.getController();

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(50.0027, 8.2771);
    mapController.setZoom(7.0);
    mapController.setCenter(startPoint);

    //get livedata from local room database
    cameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);
    allCameras = cameraViewModel.getAllCameras();

    // Refresh markers when database changes
    Observer<List<SurveillanceCamera>> localCameraObserver = new Observer<List<SurveillanceCamera>>() {
      @Override
      public void onChanged(@Nullable List<SurveillanceCamera> surveillanceCameras) {
        reloadMarker();
      }

    };
    // Set observer for LiveData
    cameraViewModel.getAllCameras().observe(this, localCameraObserver);

    // myLocationOverlay
    myLocationOverlay = new MyLocationNewOverlay(mapView);
    myLocationOverlay.enableMyLocation();
    myLocationOverlay.enableFollowLocation();
    myLocationOverlay.setDrawAccuracyEnabled(true);
    // Set
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



    timemachineButton = findViewById(R.id.map_timemachine_button);

    timemachineButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        LayoutInflater layoutInflater = (LayoutInflater) MapActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        timemachineView = findViewById(R.id.timemachine_view);
        timeframeView = findViewById(R.id.map_timeframe);

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

              if (isInitialSpinnerSelection) {
                isInitialSpinnerSelection = false;

                cal.set(2018, 1, 1);
                timemachineMaxInterval = cal.getTime();
                daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                timemachineSeekBar.invalidate();
                timemachineSeekBar.setMax(daysBetween - 1);

              } else {

                switch (i) {

                  case 0: // 7 days
                    cal.add(Calendar.DATE, -7);
                    timemachineMaxInterval = cal.getTime();
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(6);
                    reloadMarker();
                    break;


                  case 1: // 4 weeks
                    cal.add(Calendar.DATE, -28);
                    timemachineMaxInterval = cal.getTime();
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(27);
                    reloadMarker();
                    break;

                  case 2: // 3 months
                    cal.add(Calendar.MONTH, -3);
                    timemachineMaxInterval = cal.getTime();
                    daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(daysBetween - 1);
                    reloadMarker();
                    break;

                  case 3: // 6 months
                    cal.add(Calendar.MONTH, -6);
                    timemachineMaxInterval = cal.getTime();
                    daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(daysBetween - 1);
                    reloadMarker();
                    break;

                  case 4: // 1 year
                    cal.add(Calendar.MONTH, -12);
                    timemachineMaxInterval = cal.getTime();
                    daysBetween = daysBetween(timemachineMaxInterval, currentDate);
                    timemachineSeekBar.invalidate();
                    timemachineSeekBar.setMax(daysBetween - 1);
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
              timemachineSeekBar.setMax(daysBetween - 1);
              reloadMarker();



            }
          });


          timemachineSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

              timeframeTextView.setText("Progress " + String.valueOf(i));
              Calendar tempcal = Calendar.getInstance();
              tempcal.setTime(timemachineMaxInterval);
              tempcal.add(Calendar.DATE, i);
              currentSeekBarDate = tempcal.getTime();
              reloadMarker();


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
          });




        } else {
          mapLayout.removeView(timemachineView);
          mapLayout.removeView(timeframeView);
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
            Intent cameraIntent = new Intent(MapActivity.this, DetectorActivity.class);
            startActivity(cameraIntent);
            return true;

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
  public boolean onCreateOptionsMenu(Menu menu) {
    //return super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.actionbar, menu);
    return true;
  }



  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case R.id.action_settings:
        Intent settingsIntent = new Intent(MapActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

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

    if (mCurrentBackgroundMarkerLoaderTask == null) {
      // start background load
      double zoom = this.mapView.getZoomLevelDouble();
      BoundingBox world = this.mapView.getBoundingBox();

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

  void queryServer(String queryString) {

    camerasInAreaFromServer.clear();

    RequestQueue mRequestQueue;

    final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    // String url = "http://192.168.2.159:5000/cameras/?area=8.2699,50.0201,8.2978,50.0005";
    String baseURL = sharedPreferences.getString("synchronizationURL", null);

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

                    SynchronizedCamera cameraToAdd = new SynchronizedCamera(JSONToSynchronize.getString("imageURL"),
                            JSONToSynchronize.getString("id"),
                            JSONToSynchronize.getDouble("lat"),
                            JSONToSynchronize.getDouble("lon"),
                            JSONToSynchronize.getString("comments"),
                            JSONToSynchronize.getString("lastUpdated")

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
        // TODO: Handle http Errors
      }
    }
    );

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
        reloadMarker();
      }
    });

  }



  private class BackgroundMarkerLoaderTask extends AsyncTask<Double, Integer, List<SynchronizedCamera>> {

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
    protected List<SynchronizedCamera> doInBackground(Double... params) {
      FolderOverlay result = new FolderOverlay();

      List<SynchronizedCamera> allCamerasInAreaFromDb;

      try {
        if (params.length != 5)
          throw new IllegalArgumentException("expected latMin, latMax, lonMin, longMax, zoom");

        int paramNo = 0;
        double latMin = params[paramNo++];
        double latMax = params[paramNo++];
        double lonMin = params[paramNo++];
        double lonMax = params[paramNo++];

        String latMinString = String.valueOf(latMin);
        String latMaxString = String.valueOf(latMax);
        String lonMinString = String.valueOf(lonMin);
        String lonMaxString = String.valueOf(lonMax);

        String areaString =
                latMinString + ","
                + latMaxString + ","
                + lonMinString + ","
                + lonMaxString;

        if (areaString.equals(lastArea)) {
          return camerasFromLastUpdate;
        }


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

        final SharedPreferences sharedPreferences;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // TODO maybe move more or less static stuff out of background query? maybe keep to stay updated

        String offlineArea = sharedPreferences.getString("area", null); // SNWE
        String[] splitBorders = offlineArea.split(",");

        double offlineLatMin = Double.parseDouble(splitBorders[0]);
        double offlineLatMax = Double.parseDouble(splitBorders[1]);
        double offlineLonMin = Double.parseDouble(splitBorders[2]);
        double offlineLonMax = Double.parseDouble(splitBorders[3]);

        boolean offlineMode = sharedPreferences.getBoolean("offlineMode", true);
        final boolean allowServerQueries = sharedPreferences.getBoolean("allowServerQueries", false);

        boolean outsideOfflineArea = latMin < offlineLatMin ||
                latMax > offlineLatMax ||
                lonMin < offlineLonMin ||
                lonMax > offlineLonMax;

        if (!offlineMode && outsideOfflineArea) {

          runOnUiThread(new Runnable() {
            @Override
            public void run() {

              LayoutInflater layoutInflater = (LayoutInflater) MapActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
              View popupView = layoutInflater.inflate(R.layout.leaving_offline_popup, null);

              final CheckBox dontAskAgainACheckBox = popupView.findViewById(R.id.map_popup_dont_show_again_checkbox);

              Button yesButton = popupView.findViewById(R.id.map_popup_yes_button);
              Button noButton = popupView.findViewById(R.id.map_popup_no_button);

              final PopupWindow popupWindow =
                      new PopupWindow(popupView,
                      MapView.LayoutParams.WRAP_CONTENT,
                      MapView.LayoutParams.WRAP_CONTENT);


              if (!popupWindow.isShowing() && !allowServerQueries) {

                mapScrollingEnabled = false;

                popupWindow.showAtLocation(mapView, Gravity.CENTER, 0, 0);
                yesButton.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {


                    if (dontAskAgainACheckBox.isChecked()) {
                      sharedPreferences.edit().putBoolean("allowServerQueries", true).apply();
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
                    }
                    popupWindow.dismiss();
                    mapScrollingEnabled = true;


                  }
                });
              }
            }
          });


          if (allowServerQueries || allowOneServerQuery) {

            String areaQuery = "area=" + areaString;

            // TODO add start value to only update not download all everytime. need per area lastUpdated in own db

            String currentQuery = areaQuery; // add startQuery when needed

            if (!lastArea.equals(currentQuery)) {

              queryServer(currentQuery);
              lastArea = currentQuery;
            }


          }

        }

        allCamerasInAreaFromDb = synchronizedCameraRepository.getSynchronizedCamerasInArea(latMin, latMax, lonMin, lonMax);

        List<SynchronizedCamera> camerasNotInDb = new ArrayList<>();

        allCamerasInArea.clear();

        if (outsideOfflineArea) {

          Set<SynchronizedCamera> setFromDb = new HashSet<>(allCamerasInAreaFromDb);

          for (SynchronizedCamera item : camerasInAreaFromServer) {
            if (!setFromDb.contains(item)) {
              camerasNotInDb.add(item);
            }
          }

          synchronizedCameraRepository.insert(camerasNotInDb);
          allCamerasInArea.addAll(camerasNotInDb);
          camerasNotInDb.clear();

        }

        allCamerasInArea.addAll(allCamerasInAreaFromDb);

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

        mapView.getOverlays().remove(cameraOverlay);
        mapView.invalidate();

        itemsToDisplay.clear();

        timemachineView = findViewById(R.id.map_timemachine_seekbar);

        SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (timemachineView == null) {
          for (int i = 0; i < camerasToDisplay.size(); i++) {
            itemsToDisplay.add(new OverlayItem(String.valueOf(i),"test_camera", camerasToDisplay.get(i).getComments(), new GeoPoint(camerasToDisplay.get(i).getLatitude(), camerasToDisplay.get(i).getLongitude())));
          }

        } else {

          for (int i = 0; i < camerasToDisplay.size(); i++) {
            try{

              Date cameraLastUpdated = timestampIso8601.parse(camerasToDisplay.get(i).getLastUpdated());

              if (cameraLastUpdated.after(currentSeekBarDate)) {
                itemsToDisplay.add(new OverlayItem(String.valueOf(i), "test_camera", camerasToDisplay.get(i).getComments(), new GeoPoint(camerasToDisplay.get(i).getLatitude(), camerasToDisplay.get(i).getLongitude())));

              }

            } catch (ParseException parseExc) {
              Log.i(TAG, "parseException: " + camerasToDisplay.get(i).getLastUpdated());
            }

          }


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

                        File thumbnail = new File(allCamerasInArea.get(cameraIndex).getImagePath());

                        Picasso.get().load(thumbnail)
                                .into(infoImage);
                        infoLatestTimestamp.setText(allCamerasInArea.get(cameraIndex).getLastUpdated());
                        infoComment.setText(allCamerasInArea.get(cameraIndex).getComments());

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
                            MapActivity.this,
                            "Item '" + cameraItem.getTitle() + "' (index=" + index
                                    + ") got long pressed", Toast.LENGTH_LONG).show();
                    return false;
                  }
                }, getApplicationContext());
        mapView.getOverlays().add(cameraOverlay);

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

  public int daysBetween(Date d1, Date d2){
    return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
  }


}