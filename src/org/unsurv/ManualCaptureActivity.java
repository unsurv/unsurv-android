package org.unsurv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

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

  private MapView mapView;

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

    super.onResume();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_manual_capture);

    context = this;

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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

    // MAPNIK fix
    // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");

    // TODO add choice + backup strategy here
    mapView.setTileSource(TileSourceFactory.OpenTopo);

    IMapController mapController = mapView.getController();

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
