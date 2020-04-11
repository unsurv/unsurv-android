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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
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

  Spinner cameraTypeSpinner;

  SeekBar directionSeekBar;
  TextView directionTextView;

  Spinner areaSpinner;

  SeekBar heightSeekBar;
  TextView heightTextView;

  Spinner mountSpinner;

  Button saveButton;
  Button editButton;
  ImageButton resetMapButton;
  ImageView editLocationMarker;

  File cameraImage;
  IMapController mapController;

  RecyclerView recyclerView;
  RecyclerView.Adapter adapter;

  IconOverlay iconOverlay;
  GeoPoint cameraLocation;

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


    cameraTypeSpinner = findViewById(R.id.edit_camera_type_selection);

    directionSeekBar = findViewById(R.id.edit_camera_direction_seekbar);
    directionSeekBar.setMax(360);
    directionTextView = findViewById(R.id.edit_camera_direction_text);

    areaSpinner = findViewById(R.id.edit_camera_area_selection);

    heightSeekBar = findViewById(R.id.edit_camera_height_seekbar);
    heightSeekBar.setMax(20);
    heightTextView = findViewById(R.id.edit_camera_height_text);

    mountSpinner = findViewById(R.id.edit_camera_mount_selection);

    timestampTextView = findViewById(R.id.edit_camera_timestamp_text);
    uploadTextView = findViewById(R.id.edit_camera_upload_text);


    saveButton = findViewById(R.id.camera_edit_save_button);
    editButton = findViewById(R.id.camera_edit_edit_button);
    resetMapButton = findViewById(R.id.edit_camera_reset_map_position);
    editLocationMarker = findViewById(R.id.edit_camera_center_marker);

    // Activity gets startet with db id in IntentExtra, get id
    Intent startIntent = getIntent();
    int dbId = startIntent.getIntExtra("surveillanceCameraId", 0);

    cameraRepository = new CameraRepository(getApplication());


    cameraToEdit = cameraRepository.findByDbId(dbId);
    cameraType = cameraToEdit.getCameraType();
    String thumbnailPath = cameraToEdit.getThumbnailPath();
    cameraImage = new File(picturesPath + thumbnailPath);


    Picasso.get().load(cameraImage)
              .placeholder(R.drawable.ic_camera_alt_grey_50dp)
              .into(cameraImageView);

    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    //example data: ""[asd.jpg, bsd.jpg]""
    String[] filenames = cameraToEdit.getThumbnailFiles();

    adapter = new ChooseImageAdapter(this, filenames, cameraImageView, cameraToEdit, cameraRepository);
    recyclerView.setAdapter(adapter);


    map.setTilesScaledToDpi(true);
    map.setClickable(false);
    map.setMultiTouchControls(true);

    // MAPNIK fix
    // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");
    // TODO add choice + backup strategy here
    map.setTileSource(TileSourceFactory.OpenTopo);

    // remove big + and - buttons at the bottom of the map
    final CustomZoomButtonsController zoomController = map.getZoomController();
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

    mapController = map.getController();

    double lat = cameraToEdit.getLatitude();
    double lon = cameraToEdit.getLongitude();


    // Setting starting position and zoom level.
    cameraLocation = new GeoPoint(lat, lon);
    mapController.setZoom(16.0);
    mapController.setCenter(cameraLocation);


    // Spinner for camera type selection

    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(getApplicationContext(),
            R.array.edit_camera_type, android.R.layout.simple_spinner_item);

    // Specify the layout to use when the list of choices appears
    typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    // Apply the adapter to the spinners
    cameraTypeSpinner.setAdapter(typeAdapter);

    cameraTypeSpinner.setSelection(cameraType);

    cameraTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        ((TextView) adapterView.getChildAt(0))
                .setTextColor(getResources().getColor(R.color.textWhite, null));

        cameraToEdit.setCameraType(i);

        switch (i){
          case StorageUtils.FIXED_CAMERA:

            map.getOverlays().remove(iconOverlay);
            map.invalidate();

            cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(context.getResources(),
                    R.drawable.standard_camera_marker_5_dpi, 12, null);
            iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

            map.getOverlays().add(iconOverlay);
            map.invalidate();
            break;

          case StorageUtils.DOME_CAMERA:

            map.getOverlays().remove(iconOverlay);
            map.invalidate();

            cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(context.getResources(),
                    R.drawable.dome_camera_marker_5_dpi, 12, null);
            iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

            map.getOverlays().add(iconOverlay);
            map.invalidate();
            break;

          case StorageUtils.PANNING_CAMERA:

            map.getOverlays().remove(iconOverlay);
            map.invalidate();

            cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(context.getResources(),
                    R.drawable.unknown_camera_marker_5dpi, 12, null);
            iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

            map.getOverlays().add(iconOverlay);
            map.invalidate();
            break;
        }

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });

    int cameraDirection = cameraToEdit.getDirection();

    if (cameraDirection != -1) {
      directionTextView.setText(String.valueOf(cameraDirection));
      directionSeekBar.setProgress(cameraDirection);
    } else {
      directionTextView.setText("?");
      directionSeekBar.setProgress(0);
    }


    directionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        directionTextView.setText(String.valueOf(i));

        // TODO draw area or line on map to represent direction
        // TODO different area shapes for fixed dome panning
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        cameraToEdit.setDirection(progress);
      }
    });


    int area = cameraToEdit.getArea();

    // Spinner for camera type selection

    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> areaAdapter = ArrayAdapter.createFromResource(getApplicationContext(),
            R.array.edit_camera_area, android.R.layout.simple_spinner_item);

    // Specify the layout to use when the list of choices appears
    areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    // Apply the adapter to the spinners
    areaSpinner.setAdapter(areaAdapter);

    areaSpinner.setSelection(area);

    areaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        ((TextView) adapterView.getChildAt(0))
                .setTextColor(getResources().getColor(R.color.textWhite, null));

        cameraToEdit.setArea(i);

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });


    int cameraHeight = cameraToEdit.getHeight();

    if (cameraHeight != -1) {
      heightTextView.setText(String.valueOf(cameraHeight));
      heightSeekBar.setProgress(cameraHeight);
    } else {
      heightTextView.setText("?");
      heightSeekBar.setProgress(0);
    }

    heightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        heightTextView.setText(String.valueOf(i));

        // TODO draw area or line on map to represent direction
        // TODO different area shapes for fixed dome panning
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        cameraToEdit.setHeight(progress);
      }
    });


    int mount = cameraToEdit.getMount();

    // Spinner for camera type selection

    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> mountAdapter = ArrayAdapter.createFromResource(getApplicationContext(),
            R.array.edit_camera_mount, android.R.layout.simple_spinner_item);

    // Specify the layout to use when the list of choices appears
    areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    // Apply the adapter to the spinners
    mountSpinner.setAdapter(mountAdapter);

    mountSpinner.setSelection(mount);

    mountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        ((TextView) adapterView.getChildAt(0))
                .setTextColor(getResources().getColor(R.color.textWhite, null));

        cameraToEdit.setMount(i);

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

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

          switch (cameraToEdit.getCameraType()) {
            case StorageUtils.FIXED_CAMERA:

              Picasso.get().load(R.drawable.standard_camera_marker_5_dpi)
                      .into(editLocationMarker);
              break;

            case StorageUtils.DOME_CAMERA:

              Picasso.get().load(R.drawable.dome_camera_marker_5_dpi)
                      .into(editLocationMarker);
              break;

            case StorageUtils.PANNING_CAMERA:

              Picasso.get().load(R.drawable.unknown_camera_marker_5dpi)
                      .into(editLocationMarker);
              break;

          }

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
