package org.tensorflow.demo;

import android.Manifest;
import android.app.Application;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

  private CameraViewModel mCameraViewModel;
  private int readStoragePermission;
  private int writeStoragePermission;
  private SharedPreferences sharedPreferences;
  private BottomNavigationView bottomNavigationView;
  private RecyclerView recyclerView;
  private CameraListAdapter adapter;



  @Override
  protected void onResume() {

    // check permissions on resume
    readStoragePermission = ContextCompat.checkSelfPermission(HistoryActivity.this,
            Manifest.permission.READ_EXTERNAL_STORAGE);
    writeStoragePermission = ContextCompat.checkSelfPermission(HistoryActivity.this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);


    List<String> permissionList = new ArrayList<>();

    if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    // transfer list to string array for requestPermissions method
    String[] neededPermissions = permissionList.toArray(new String[0]);

    if (!permissionList.isEmpty()) {
      ActivityCompat.requestPermissions(HistoryActivity.this, neededPermissions, 0);
    }

    super.onResume();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_history);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // small map at top-right to show camera location
    MapView detailMap = findViewById(R.id.history_detail_map);

    detailMap.setTilesScaledToDpi(true);
    detailMap.setClickable(false);
    detailMap.setMultiTouchControls(false);

    // MAPNIK fix
    // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");
    // TODO add choice + backup strategy here
    detailMap.setTileSource(TileSourceFactory.OpenTopo);

    final CustomZoomButtonsController zoomController = detailMap.getZoomController();
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

    final IMapController mapController = detailMap.getController();

    String homeZone = sharedPreferences.getString("area", null);

    String[] coordinates = homeZone.split(",");

    double latMin = Double.valueOf(coordinates[0]);
    double latMax = Double.valueOf(coordinates[1]);
    double lonMin = Double.valueOf(coordinates[2]);
    double lonMax = Double.valueOf(coordinates[3]);

    // homezone center as start if no camera is chosen yet
    double centerLat = (latMin + latMax) / 2;
    double centerLon = (lonMin + lonMax) / 2;

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(centerLat, centerLon);
    mapController.setZoom(10.0);
    mapController.setCenter(startPoint);

    recyclerView = (RecyclerView) findViewById(R.id.camera_recyclerview);
    mCameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

    // needed in adapter to show PopupWindows
    LayoutInflater layoutInflater = (LayoutInflater) HistoryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    final LinearLayout rootView = findViewById(R.id.history_detail_linear);
    adapter = new CameraListAdapter(this, rootView, getApplication(), layoutInflater, mCameraViewModel);

    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    // LiveData
    mCameraViewModel.getAllCameras().observe(this, new Observer<List<SurveillanceCamera>>() {
      @Override
      public void onChanged(@Nullable List<SurveillanceCamera> surveillanceCameras) {
        adapter.setCameras(surveillanceCameras);
      }
    });

    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(HistoryActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
              Intent manualCaptureIntent = new Intent(HistoryActivity.this, ManualCaptureActivity.class);
              startActivity(manualCaptureIntent);
              return true;
            } else {
              Intent cameraIntent = new Intent(HistoryActivity.this, DetectorActivity.class);
              startActivity(cameraIntent);
              return true;

            }

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(HistoryActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(HistoryActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_history).setChecked(true);


  }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      //return super.onCreateOptionsMenu(menu);
      getMenuInflater().inflate(R.menu.actionbar, menu);

      // Removes refreshButton in ActionBar. Not needed since LiveData is used for the List.
      MenuItem item = menu.findItem(R.id.action_refresh);
      item.setVisible(false);


      return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {

        case R.id.action_settings:
          Intent settingsIntent = new Intent(HistoryActivity.this, SettingsActivity.class);
          startActivity(settingsIntent);

          return true;

        case R.id.action_training:
          Intent trainingCaptureIntent = new Intent(HistoryActivity.this, CaptureTrainingImageActivity.class);
          startActivity(trainingCaptureIntent);



        default:
          // Fall back on standard behaviour when user choice not recognized.
          return super.onOptionsItemSelected(item);
      }
    }

}




