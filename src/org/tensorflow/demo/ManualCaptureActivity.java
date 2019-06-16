package org.tensorflow.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ManualCaptureActivity extends AppCompatActivity {

  private BottomNavigationView bottomNavigationView;
  private SharedPreferences sharedPreferences;

  private MapView mapView;

  private ImageButton manualSaveButton;

  private CameraRepository cameraRepository;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_manual_capture);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    cameraRepository = new CameraRepository(getApplication());


    mapView = findViewById(R.id.manual_capture_map);

    manualSaveButton = findViewById(R.id.manual_save_button);

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

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(centerLat, centerLon);
    mapController.setZoom(10.0);
    mapController.setCenter(startPoint);


    manualSaveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        IGeoPoint center = mapView.getMapCenter();

        SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        long currentTime = System.currentTimeMillis();

        SurveillanceCamera manualCamera = new SurveillanceCamera(
                null,
                null,
                null,
                center.getLatitude(),
                center.getLongitude(),
                "",
                timestampIso8601.format(new Date(currentTime)),
                SynchronizationUtils.getSynchronizationDateWithRandomDelay(currentTime, sharedPreferences),
                false,
                false,
                true

        );

        cameraRepository.insert(manualCamera);


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
            Intent cameraIntent = new Intent(ManualCaptureActivity.this, DetectorActivity.class);
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

    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_camera).setChecked(true);


  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //return super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.actionbar, menu);

    // No need for a refresh Button in a capture activity.
    MenuItem item = menu.findItem(R.id.action_refresh);
    item.setVisible(false);

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

      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }





}
