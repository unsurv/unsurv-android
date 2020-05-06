package org.unsurv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Allows the user to manually create a SurveillanceCamera without having to use the camera and
 * expose his/her actions
 */

// TODO save position of last aquired camera and start from there


public class ManualCaptureActivity extends AppCompatActivity {

  private BottomNavigationView bottomNavigationView;
  private SharedPreferences sharedPreferences;

  boolean offlineMode;

  private MapView mapView;
  IMapController mapController;
  GeoPoint centerMap;

  ImageButton manualSaveButton;
  ImageButton addStandardCameraButton;
  ImageButton addDomeCameraButton;
  ImageButton addUnknownCameraButton;
  ImageButton manualToGrid;

  ImageView marker;

  private CameraRepository cameraRepository;

  int cameraType;

  Context context;


  @Override
  protected void onResume() {

    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);

    String oldLat = sharedPreferences.getString("manualCenterLat", "");
    String oldLon = sharedPreferences.getString("manualCenterLon", "");
    String oldZoom = sharedPreferences.getString("manualZoom", "");

    if (!oldLat.isEmpty() && !oldLon.isEmpty() && !oldZoom.isEmpty()) {

      centerMap = new GeoPoint(Double.parseDouble(oldLat), Double.parseDouble(oldLon));
      mapController.setZoom(Double.parseDouble(oldZoom));
      mapController.setCenter(centerMap);

    }


    super.onResume();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_manual_capture);

    context = this;

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    offlineMode = sharedPreferences.getBoolean("offlineMode", true);

    cameraRepository = new CameraRepository(getApplication());

    // choose standard type as default
    cameraType = StorageUtils.FIXED_CAMERA;

    mapView = findViewById(R.id.manual_capture_map);

    marker = findViewById(R.id.manual_capture_marker);

    manualSaveButton = findViewById(R.id.manual_save_button);
    addStandardCameraButton = findViewById(R.id.manual_capture_add_standard_camera_button);
    addDomeCameraButton = findViewById(R.id.manual_capture_add_dome_camera_button);
    addUnknownCameraButton = findViewById(R.id.manual_capture_add_unknown_camera_button);
    manualToGrid = findViewById(R.id.manual_to_grid);

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

    mapController = mapView.getController();

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


    // refresh values after 200ms delay
    mapView.addMapListener(new DelayedMapListener(new MapListener() {
      @Override
      public boolean onScroll(ScrollEvent event) {
        IGeoPoint centerAfterScroll = mapView.getMapCenter();


        centerMap = new GeoPoint(centerAfterScroll);
        sharedPreferences.edit().putString("manualCenterLat", String.valueOf(centerMap.getLatitude())).apply();
        sharedPreferences.edit().putString("manualCenterLon", String.valueOf(centerMap.getLongitude())).apply();


        // onZoom triggers only when there is absolutely no movement too, save zoom level here too
        Double zoomLevel = mapView.getZoomLevelDouble();
        sharedPreferences.edit().putString("manualZoom", String.valueOf(zoomLevel)).apply();

        return false;

      }

      @Override
      public boolean onZoom(ZoomEvent event) {

        Double zoomLevel = mapView.getZoomLevelDouble();
        sharedPreferences.edit().putString("manualZoom", String.valueOf(zoomLevel)).apply();

        return false;

      }
    }, 200));



    // add different types depending on user choice
    addStandardCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        cameraType = StorageUtils.FIXED_CAMERA;
        marker.setImageDrawable(getDrawable(R.drawable.standard_camera_marker_5_dpi));
      }
    });

    addDomeCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        cameraType = StorageUtils.DOME_CAMERA;
        marker.setImageDrawable(getDrawable(R.drawable.dome_camera_marker_5_dpi));

      }
    });

    addUnknownCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        cameraType = StorageUtils.PANNING_CAMERA;
        marker.setImageDrawable(getDrawable(R.drawable.unknown_camera_marker_5dpi));

      }
    });


    manualSaveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        IGeoPoint center = mapView.getMapCenter();

        SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        long currentTime = System.currentTimeMillis();

        String currentDateOrNull;

        if (sharedPreferences.getBoolean("showCaptureTimestamps", false)){
          currentDateOrNull = timestampIso8601.format(new Date(currentTime));
        } else {
          currentDateOrNull = null;
        }

        // when editing camera location, a new marker is created in the center of the map
        // get center coordinates when save button is pressed
        SurveillanceCamera manualCamera = new SurveillanceCamera(
                cameraType,
                0,
                -1,
                0,
                5,
                15,
                null,
                null,
                null,
                center.getLatitude(),
                center.getLongitude(),
                "",
                currentDateOrNull,
                SynchronizationUtils.getSynchronizationDateWithRandomDelay(currentTime, sharedPreferences),
                false,
                false,
                true,
                false,
                "",
                ""

        );

        cameraRepository.insert(manualCamera);

        // create small notification for history activity
        BottomNavigationBadgeHelper.incrementBadge(bottomNavigationView, context, R.id.bottom_navigation_history, 1);


      }
    });

    manualToGrid.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent organizeIntent = new Intent(ManualCaptureActivity.this, OrganizeActivity.class);
        startActivity(organizeIntent);
      }
    });



    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(ManualCaptureActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            Intent cameraIntent = new Intent(ManualCaptureActivity.this, ManualCaptureActivity.class);
            startActivity(cameraIntent);
            return true;

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(ManualCaptureActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(ManualCaptureActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

    androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_camera).setChecked(true);


  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //return super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.actionbar, menu);

    // No need for a refresh Button in a capture activity.
    MenuItem refreshItem = menu.findItem(R.id.action_refresh);
    refreshItem.setVisible(false);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      // settings Button
      case R.id.action_settings:
        Intent settingsIntent = new Intent(ManualCaptureActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

        return true;

      case R.id.action_training:
        Intent trainingCaptureIntent = new Intent(ManualCaptureActivity.this, CaptureTrainingImageActivity.class);
        startActivity(trainingCaptureIntent);

        return true;

      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }





}
