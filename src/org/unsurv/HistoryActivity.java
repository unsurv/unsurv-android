package org.unsurv;

import android.Manifest;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * This is one of the 4 main activities. It shows captures the user has taken and offers
 * options on what to do with them
 */
public class HistoryActivity extends AppCompatActivity {

  CameraViewModel mCameraViewModel;
  int readStoragePermission;
  int writeStoragePermission;
  RecyclerView recyclerView;

  private SharedPreferences sharedPreferences;
  private BottomNavigationView bottomNavigationView;
  private CameraListAdapter adapter;
  private Context context;


  // TODO First capture gets eaten!!
  // TODO fix upload state when syncjob is complete

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

    // clear badges for HistoryActivity when accessed
    BottomNavigationBadgeHelper.clearMenuItemBadge(bottomNavigationView, R.id.bottom_navigation_history, context);
    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);

    // redraw / refresh recyclerview items by resetting adapter
    recyclerView.setAdapter(null);
    recyclerView.setAdapter(adapter);


    super.onResume();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_history);

    context = this;

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // main view for captures
    recyclerView = findViewById(R.id.camera_recyclerview);

    // enables access to db
    mCameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

    // needed in adapter to show PopupWindows
    LayoutInflater layoutInflater = (LayoutInflater) HistoryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    adapter = new CameraListAdapter(this, getApplication(), layoutInflater, mCameraViewModel);

    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    // LiveData to immediately include new data in recyclerview
    mCameraViewModel.getAllCameras().observe(this, new Observer<List<SurveillanceCamera>>() {
      @Override
      public void onChanged(@Nullable List<SurveillanceCamera> surveillanceCameras) {
        adapter.setCameras(surveillanceCameras);
      }
    });

    androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
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

      //top actionbar with settings icon
      getMenuInflater().inflate(R.menu.actionbar, menu);

      // Removes refreshButton in ActionBar. Not needed since LiveData is used for the list.
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




