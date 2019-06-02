package org.tensorflow.demo;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

  private CameraViewModel mCameraViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_history);

    MapView detailMap = findViewById(R.id.history_detail_map);
    detailMap.setTilesScaledToDpi(true);
    detailMap.setClickable(false);
    detailMap.setMultiTouchControls(false);
    detailMap.setTileSource(TileSourceFactory.OpenTopo);

    final CustomZoomButtonsController zoomController = detailMap.getZoomController();
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

    final IMapController mapController = detailMap.getController();

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(50.0027, 8.2771);
    mapController.setZoom(15.0);
    mapController.setCenter(startPoint);


    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.camera_recyclerview);

    LayoutInflater layoutInflater = (LayoutInflater) HistoryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    final LinearLayout rootView = findViewById(R.id.history_detail_linear);
    final CameraListAdapter adapter = new CameraListAdapter(this, rootView, getApplication());

    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    mCameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

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
            Intent cameraIntent = new Intent(HistoryActivity.this, DetectorActivity.class);
            startActivity(cameraIntent);
            return true;

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
    return true;
  }



  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case R.id.action_settings:
        Intent settingsIntent = new Intent(HistoryActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

        return true;



      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }

  private BottomNavigationView bottomNavigationView;
}




