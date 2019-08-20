package org.unsurv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;

import java.io.File;


// TODO check if cameratype is correctly parsed from detector
// TODO change markers for map depending on type

/**
 * Allows the user to review and edit SurveillanceCamera captures, launched if user clicks on a
 * non training SurveillanceCamera in HistoryActivity
 */

public class EditCameraActivity extends AppCompatActivity {

  BottomNavigationView bottomNavigationView;

  CameraRepository cameraRepository;

  SurveillanceCamera cameraToEdit;
  SharedPreferences sharedPreferences;

  Drawable cameraMarkerIcon;

  ImageView cameraImageView;
  MapView map;
  CheckBox standardCheckBox;
  CheckBox domeCheckBox;
  CheckBox unknownCheckBox;

  TextView timestampTextView;
  TextView uploadTextView;
  TextView commentsTextView;
  Button saveButton;
  Button editButton;
  ImageButton resetMapButton;
  ImageView editLocationMarker;

  File cameraImage;
  IMapController mapController;

  RecyclerView recyclerView;
  RecyclerView.Adapter adapter;

  IconOverlay iconOverlay;

  int cameraType;

  boolean isBeingEdited = false;

  Context context;

  private static String picturesPath = StorageUtils.CAMERA_CAPTURES_PATH;


  @Override
  protected void onResume() {

    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);

    super.onResume();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_camera);

    context = this;

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    cameraImageView = findViewById(R.id.edit_camera_detail_image);
    recyclerView = findViewById(R.id.edit_camera_choose_recyclerview);
    map = findViewById(R.id.edit_camera_map);

    // check boxes for camera types
    standardCheckBox = findViewById(R.id.edit_camera_checkbox_standard);
    domeCheckBox = findViewById(R.id.edit_camera_checkbox_dome);
    unknownCheckBox = findViewById(R.id.edit_camera_checkbox_unknown);

    timestampTextView = findViewById(R.id.edit_camera_timestamp_text);
    uploadTextView = findViewById(R.id.edit_camera_upload_text);
    commentsTextView = findViewById(R.id.edit_camera_comments_text);
    saveButton = findViewById(R.id.camera_edit_save_button);
    editButton = findViewById(R.id.camera_edit_edit_button);
    resetMapButton = findViewById(R.id.edit_camera_reset_map_position);
    editLocationMarker = findViewById(R.id.edit_camera_center_marker);

    // Activity gets startet with db id in IntentExtra
    Intent startIntent = getIntent();
    int dbId = startIntent.getIntExtra("surveillanceCameraId", 0);

    cameraRepository = new CameraRepository(getApplication());


    cameraToEdit = cameraRepository.findByDbId(dbId);
    cameraType = cameraToEdit.getCameraType();
    String thumbnailPath = cameraToEdit.getThumbnailPath();
    cameraImage = new File(picturesPath + thumbnailPath);


    Picasso.get().load(cameraImage)
              .placeholder(R.drawable.ic_launcher)
              .into(cameraImageView);

    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    //example data: ""[asd.jpg, bsd.jpg]""
    String[] filenames = cameraToEdit.getCaptureFilenames()
            .replace("\"", "")
            .replace("[", "")
            .replace("]", "")
            .split(",");

    adapter = new ChooseImageAdapter(this, filenames, cameraImageView, cameraToEdit, cameraRepository);
    recyclerView.setAdapter(adapter);


    map.setTilesScaledToDpi(true);
    map.setClickable(false);
    map.setMultiTouchControls(true);

    // MAPNIK fix
    // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");
    // TODO add choice + backup strategy here
    map.setTileSource(TileSourceFactory.OpenTopo);

    final CustomZoomButtonsController zoomController = map.getZoomController();
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

    mapController = map.getController();

    double lat = cameraToEdit.getLatitude();
    double lon = cameraToEdit.getLongitude();


    // Setting starting position and zoom level.
    GeoPoint cameraLocation = new GeoPoint(lat, lon);
    mapController.setZoom(16.0);
    mapController.setCenter(cameraLocation);

    cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(this.getResources(), R.drawable.standard_camera_marker_5_dpi, 12, null);

    iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

    map.getOverlays().add(iconOverlay);

    //detailMap.getOverlays().add(cameraMarker);
    map.invalidate();

    switch (cameraType){

      case StorageUtils.STANDARD_CAMERA:
        standardCheckBox.setChecked(true);
        break;

      case StorageUtils.DOME_CAMERA:
        domeCheckBox.setChecked(true);
        break;

      case StorageUtils.UNKNOWN_CAMERA:
        unknownCheckBox.setChecked(true);
        break;


    }

    standardCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (b){
          cameraToEdit.setCameraType(StorageUtils.STANDARD_CAMERA);

          domeCheckBox.setChecked(false);
          unknownCheckBox.setChecked(false);
        }

      }
    });

    domeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (b){
          cameraToEdit.setCameraType(StorageUtils.DOME_CAMERA);
          standardCheckBox.setChecked(false);
          unknownCheckBox.setChecked(false);
        }
      }
    });

    unknownCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (b){
          cameraToEdit.setCameraType(StorageUtils.UNKNOWN_CAMERA);
          standardCheckBox.setChecked(false);
          domeCheckBox.setChecked(false);
        }

      }
    });

    String timestampAsDate = cameraToEdit.getTimestamp();

    boolean showTimestamps = sharedPreferences.getBoolean("showCaptureTimestamps", false);

    if (timestampAsDate == null || !showTimestamps){
      timestampTextView.setText(getString(R.string.timestamp_not_available));
    } else {
      timestampTextView.setText(timestampAsDate);
    }

    String uploadDate = cameraToEdit.getTimeToSync();
    uploadTextView.setText(uploadDate);


    String comment = cameraToEdit.getComment();

    if (comment.isEmpty() || comment.equals("no comment")){
      commentsTextView.setText(getString(R.string.no_comment));
    }


    editButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {


        if(isBeingEdited) {
          // stop editing
          isBeingEdited = false;
          resetMap();

        } else {
          isBeingEdited = true;

          map.getOverlays().removeAll(map.getOverlays());
          EditCameraActivity.this.map.invalidate();
          editLocationMarker.setVisibility(View.VISIBLE);
          EditCameraActivity.this.map.invalidate();
        }




      }
    });

    saveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {


        if (isBeingEdited){

          IGeoPoint center = map.getMapCenter();
          double newLat = center.getLatitude();
          double newLon = center.getLongitude();

          cameraToEdit.setLatitude(newLat);
          cameraToEdit.setLongitude(newLon);

          editLocationMarker.setVisibility(View.INVISIBLE);

          generateMarkerOverlayWithCurrentLocation();

          cameraRepository.updateCameras(cameraToEdit);
          adapter.notifyDataSetChanged();
          isBeingEdited = false;


        } else {
          cameraRepository.updateCameras(cameraToEdit);
          adapter.notifyDataSetChanged();

          resetMap();
        }

      }
    });


    resetMapButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        resetMap();
      }
    });

    // TODO listview with different images, save abort buttons, editable mapview, change upload date with + /- buttons

    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(EditCameraActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
              Intent manualCaptureIntent = new Intent(EditCameraActivity.this, ManualCaptureActivity.class);
              startActivity(manualCaptureIntent);
              return true;
            } else {
              Intent cameraIntent = new Intent(EditCameraActivity.this, DetectorActivity.class);
              startActivity(cameraIntent);
              return true;

            }

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(EditCameraActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(EditCameraActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_history).setChecked(true);



  }

  void generateMarkerOverlayWithCurrentLocation(){

    // Setting starting position and zoom level.
    GeoPoint cameraLocation = new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude());
    mapController.setZoom(16.0);
    mapController.setCenter(cameraLocation);

    iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

    map.getOverlays().add(iconOverlay);
    map.invalidate();

  }

  void resetMap(){
    if (isBeingEdited){
      isBeingEdited = false;
    }
    editLocationMarker.setVisibility(View.INVISIBLE);
    generateMarkerOverlayWithCurrentLocation();
  }


}
