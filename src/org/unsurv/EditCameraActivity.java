package org.unsurv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.IconOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


// TODO add angle to bottom
// TODO NEW MARKERS
// TODO auto open this activity after capture

/**
 * Allows the user to review and edit SurveillanceCamera captures, launched if user clicks on a
 * non training SurveillanceCamera in HistoryActivity
 */

public class EditCameraActivity extends AppCompatActivity {

  BottomNavigationView bottomNavigationView;

  CameraRepository cameraRepository;

  SurveillanceCamera cameraToEdit;
  SharedPreferences sharedPreferences;
  boolean offlineMode;

  Drawable cameraMarkerIcon;

  ImageView detailCameraImageView;
  MapView map;

  TextView timestampTextView;
  TextView uploadTextView;

  Spinner cameraTypeSpinner;

  SeekBar directionSeekBar;
  TextView directionTextView;

  Spinner areaSpinner;

  SeekBar heightSeekBar;
  TextView heightTextView;

  Spinner mountSpinner;

  SeekBar angleSeekBar;
  TextView angleTextView;

  Button saveButton;
  Button editButton;
  ImageButton resetMapButton;
  ImageButton takePictureButton;
  ImageView editLocationMarker;

  File cameraImage;
  IMapController mapController;

  RecyclerView recyclerView;
  RecyclerView.Adapter adapter;

  IconOverlay iconOverlay;
  GeoPoint cameraLocation;

  Polyline line = new Polyline();
  Polygon polygon = new Polygon();

  int cameraType;

  boolean isBeingEdited = false;

  Context context;
  Resources resources;

  private static String captureImagePath = StorageUtils.CAMERA_CAPTURES_PATH;


  @Override
  protected void onResume() {

    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);

    offlineMode = sharedPreferences.getBoolean("offlineMode", true);


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
                map.setTileProvider(tileProvider);

                map.setTileSource(new XYTileSource(
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
      map.setTileSource(TileSourceFactory.OpenTopo);
    }

    super.onResume();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_camera);

    context = this;
    resources = context.getResources();

    cameraRepository = new CameraRepository(getApplication());

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    offlineMode = sharedPreferences.getBoolean("offlineMode", true);

    detailCameraImageView = findViewById(R.id.edit_camera_detail_image);
    recyclerView = findViewById(R.id.edit_camera_choose_recyclerview);
    map = findViewById(R.id.edit_camera_map);

    CopyrightOverlay copyrightOverlay = new CopyrightOverlay(context);
    map.getOverlays().add(copyrightOverlay);

    // user edit area
    cameraTypeSpinner = findViewById(R.id.edit_camera_type_selection);

    directionSeekBar = findViewById(R.id.edit_camera_direction_seekbar);
    directionSeekBar.setMax(360);
    directionTextView = findViewById(R.id.edit_camera_direction_text);

    areaSpinner = findViewById(R.id.edit_camera_area_selection);

    heightSeekBar = findViewById(R.id.edit_camera_height_seekbar);
    heightSeekBar.setMax(20);
    heightTextView = findViewById(R.id.edit_camera_height_text);

    angleSeekBar = findViewById(R.id.edit_camera_angle_seekbar);
    angleSeekBar.setMax(75); // start at 15 deg + 75 steps. maximum = 90
    angleTextView = findViewById(R.id.edit_camera_angle_text);

    mountSpinner = findViewById(R.id.edit_camera_mount_selection);

    timestampTextView = findViewById(R.id.edit_camera_timestamp_text);
    uploadTextView = findViewById(R.id.edit_camera_upload_text);

    saveButton = findViewById(R.id.edit_camera_save_button);
    editButton = findViewById(R.id.edit_camera_edit_button);
    resetMapButton = findViewById(R.id.edit_camera_reset_map_position);
    editLocationMarker = findViewById(R.id.edit_camera_center_marker);
    takePictureButton = findViewById(R.id.edit_camera_take_picture_button);

    // Activity gets started with db id in IntentExtra, get id
    Intent startIntent = getIntent();
    int dbId = startIntent.getIntExtra("surveillanceCameraId", 0);


    cameraToEdit = cameraRepository.findByDbId(dbId);

    List<SurveillanceCamera> allCams = cameraRepository.getAllCameras();
    cameraType = cameraToEdit.getCameraType();
    String thumbnailPath = cameraToEdit.getThumbnailPath();
    cameraImage = new File(captureImagePath + thumbnailPath);

    if (thumbnailPath == null || thumbnailPath.isEmpty()) {

      detailCameraImageView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          dispatchTakePictureIntent();
        }
      });
    }

    takePictureButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        dispatchTakePictureIntent();
      }
    });



    Picasso.get().load(cameraImage)
            .placeholder(R.drawable.ic_camera_alt_grey_50dp)
            .into(detailCameraImageView);

    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    //example data: ""[asd.jpg, bsd.jpg]""
    String[] filenames = cameraToEdit.getThumbnailFilesAsStringArray();

    adapter = new ChooseImageAdapter(this, filenames, detailCameraImageView, cameraToEdit, cameraRepository);
    recyclerView.setAdapter(adapter);


    map.setTilesScaledToDpi(true);
    map.setClickable(false);
    map.setMultiTouchControls(true);



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

    int hotPink = Color.argb(127, 255, 0, 255);
    polygon.setFillColor(hotPink);
    polygon.setStrokeColor(hotPink);

    drawCameraArea(cameraLocation,
            cameraToEdit.getDirection(),
            cameraToEdit.getHeight(),
            cameraToEdit.getAngle(),
            cameraToEdit.getCameraType());


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

        cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(resources,
                StorageUtils.chooseMarker(i, cameraToEdit.getArea()), 12, null);

        map.getOverlays().remove(iconOverlay);

        iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

        // reactivate all edits
        directionSeekBar.setEnabled(true);
        angleSeekBar.setEnabled(true);

        drawCameraArea(cameraLocation,
                cameraToEdit.getDirection(),
                cameraToEdit.getHeight(),
                cameraToEdit.getAngle(),
                cameraToEdit.getCameraType());

        List<Overlay> asdf = map.getOverlays();
        map.getOverlays().add(iconOverlay);
        map.invalidate();


      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });

    final int cameraDirection = cameraToEdit.getDirection();


    directionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        directionTextView.setText(String.valueOf(i));

        // TODO draw area or line on map to represent direction
        // TODO different area shapes for fixed dome panning

        drawCameraArea(
                new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude()),
                i,
                cameraToEdit.getHeight(),
                cameraToEdit.getAngle(),
                cameraToEdit.getCameraType());

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

    if (cameraDirection != -1) {
      directionTextView.setText(String.valueOf(cameraDirection));
      directionSeekBar.setProgress(cameraDirection);
    } else {
      directionTextView.setText("?");
      directionSeekBar.setProgress(0);
    }


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

        cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(resources,
                StorageUtils.chooseMarker(cameraToEdit.getCameraType(), i), 12, null);

        map.getOverlays().remove(iconOverlay);
        List<Overlay> asdf = map.getOverlays();

        iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

        // reactivate all edits
        directionSeekBar.setEnabled(true);
        angleSeekBar.setEnabled(true);

        drawCameraArea(cameraLocation,
                cameraToEdit.getDirection(),
                cameraToEdit.getHeight(),
                cameraToEdit.getAngle(),
                cameraToEdit.getCameraType());

        map.getOverlays().add(iconOverlay);
        map.invalidate();


      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });


    int cameraHeight = cameraToEdit.getHeight();

    heightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        heightTextView.setText(String.valueOf(i));

        // TODO draw area or line on map to represent direction
        // TODO different area shapes for fixed dome panning

        drawCameraArea(
                new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude()),
                cameraToEdit.getDirection(),
                i,
                cameraToEdit.getAngle(),
                cameraToEdit.getCameraType());
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

    if (cameraHeight != -1) {
      heightTextView.setText(String.valueOf(cameraHeight));
      heightSeekBar.setProgress(cameraHeight);
    } else {
      heightTextView.setText("?");
      heightSeekBar.setProgress(0);
    }


    int cameraAngle = cameraToEdit.getAngle();

    angleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        angleTextView.setText(String.valueOf(i + 15));

        drawCameraArea(
                new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude()),
                cameraToEdit.getDirection(),
                cameraToEdit.getHeight(),
                i + 15, // + 15 because we cant set seekbarmin in API 24
                cameraToEdit.getCameraType());

      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        cameraToEdit.setAngle(progress + 15);

      }
    });

    if (cameraAngle != -1) {
      angleTextView.setText(String.valueOf(cameraAngle));

      // seekbar has only 0-75 range, cameraAngle from 15 -90
      angleSeekBar.setProgress(cameraAngle - 15);
    } else {
      angleTextView.setText("?");
      angleSeekBar.setProgress(0);
    }


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

    if (timestampAsDate == null || !showTimestamps) {
      timestampTextView.setText(getString(R.string.timestamp_not_available));
    } else {
      timestampTextView.setText(timestampAsDate);
    }

    String uploadDate = cameraToEdit.getTimeToSync();
    uploadTextView.setText(uploadDate);


    editButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        if (isBeingEdited) {
          // stop editing
          isBeingEdited = false;
          resetMap();

        } else {
          isBeingEdited = true;

          map.getOverlays().removeAll(map.getOverlays());
          EditCameraActivity.this.map.invalidate();

          Picasso.get().load(
                  StorageUtils.chooseMarker(cameraToEdit.getCameraType(), cameraToEdit.getArea()))
                  .into(editLocationMarker);

          editLocationMarker.setVisibility(View.VISIBLE);

        }

      }
    });

    saveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        if (isBeingEdited) {

          IGeoPoint center = map.getMapCenter();
          double newLat = center.getLatitude();
          double newLon = center.getLongitude();

          cameraToEdit.setLatitude(newLat);
          cameraToEdit.setLongitude(newLon);

          editLocationMarker.setVisibility(View.INVISIBLE);

          generateMarkerOverlayWithCurrentLocation();

          drawCameraArea(new GeoPoint(newLat, newLon),
                  cameraToEdit.getDirection(),
                  cameraToEdit.getHeight(),
                  cameraToEdit.getAngle(),
                  cameraToEdit.getCameraType());

          cameraRepository.updateCameras(cameraToEdit);
          adapter.notifyDataSetChanged();
          isBeingEdited = false;


        } else {

          // set direction / angle to unknown if type DOME
          if (cameraToEdit.getCameraType() == StorageUtils.DOME_CAMERA) {
            cameraToEdit.setDirection(-1); // unknown
            cameraToEdit.setAngle(-1); // unknown

          }
          cameraRepository.updateCameras(cameraToEdit);
          adapter.notifyDataSetChanged();

          resetMap();


        }


        if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
          Intent manualCaptureIntent = new Intent(EditCameraActivity.this, ManualCaptureActivity.class);
          startActivity(manualCaptureIntent);

        } else {
          Intent cameraIntent = new Intent(EditCameraActivity.this, DetectorActivity.class);
          startActivity(cameraIntent);


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

        switch (item.getItemId()) {

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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

      long currentTime = System.currentTimeMillis();

      String thumbnailFilename = currentTime + "_thumbnail.jpg";

      Bundle extras = data.getExtras();
      Bitmap imageBitmap = (Bitmap) extras.get("data");

      Matrix turnMatrix = new Matrix();
      turnMatrix.postRotate(90);

      Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
              imageBitmap.getWidth(),
              imageBitmap.getHeight(),
              turnMatrix, true);

      try {

        JSONArray allCaptureFilenames;
        String previousFilenames = cameraToEdit.getCaptureFilenames();

        if (!previousFilenames.isEmpty()) {

          allCaptureFilenames = new JSONArray(cameraToEdit.getCaptureFilenames());

        } else {

          allCaptureFilenames = new JSONArray();

        }

        allCaptureFilenames.put(thumbnailFilename);
        cameraToEdit.setCaptureFilenames(allCaptureFilenames.toString());

        cameraToEdit.setThumbnailPath(thumbnailFilename + ".jpg");

        StorageUtils.saveBitmap(rotatedBitmap, StorageUtils.CAMERA_CAPTURES_PATH, thumbnailFilename);

      } catch (JSONException jse) {
        Log.i("EditCamera: ",  jse.toString());
      }




      detailCameraImageView.setImageBitmap(rotatedBitmap);
      cameraRepository.updateCameras(cameraToEdit);


      String[] filenames = cameraToEdit.getThumbnailFilesAsStringArray();

      recyclerView.setAdapter(null);

      adapter = new ChooseImageAdapter(this, filenames, detailCameraImageView, cameraToEdit, cameraRepository);
      recyclerView.setAdapter(adapter);

      Toast.makeText(context, "Successfully added new image and updated camera.",
              Toast.LENGTH_LONG).show();
    }

    super.onActivityResult(requestCode, resultCode, data);
  }


  void generateMarkerOverlayWithCurrentLocation() {

    // Setting starting position and zoom level.
    GeoPoint cameraLocation = new GeoPoint(cameraToEdit.getLatitude(), cameraToEdit.getLongitude());
    mapController.setZoom(16.0);
    mapController.setCenter(cameraLocation);

    map.getOverlays().remove(iconOverlay);

    iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

    map.getOverlays().add(iconOverlay);
    map.invalidate();

  }

  void resetMap() {
    if (isBeingEdited) {
      isBeingEdited = false;
    }
    editLocationMarker.setVisibility(View.INVISIBLE);
    generateMarkerOverlayWithCurrentLocation();
  }


  void drawCameraArea(GeoPoint currentPos, int direction, int height, int horizontalAngle, int cameraType) {

    int baseViewDistance = 15; // in m

    // if height entered by user
    if (height >= 0) {
      // TODO use formula from surveillance under surveillance https://sunders.uber.space
      // add 30% viewdistance per meter of height

      double heightFactor = 1 + (0.3 * height);
      baseViewDistance *= heightFactor;
    }

    if (horizontalAngle != -1) {
      // TODO use formula from surveillance under surveillance https://sunders.uber.space

      // about the same as SurveillanceUnderSurveillance https://sunders.uber.space
      double angleFactor = Math.pow(25f / horizontalAngle, 2) * 0.4;
      baseViewDistance *= angleFactor;
    }

    // remove old drawings
    map.getOverlayManager().remove(polygon);
    map.getOverlayManager().remove(line);

    List<GeoPoint> geoPoints;

    if (cameraType == StorageUtils.FIXED_CAMERA || cameraType == StorageUtils.PANNING_CAMERA) {

      int viewAngle;

      // calculate geopoints for triangle

      double startLat = currentPos.getLatitude();
      double startLon = currentPos.getLongitude();

      geoPoints = new ArrayList<>();


      if (cameraType == StorageUtils.FIXED_CAMERA) {
        viewAngle = 60; // fixed camera

        // triangle sides compass direction
        int direction1 = direction - viewAngle / 2;
        int direction2 = direction + viewAngle / 2;

        // in meters, simulate a 2d coordinate system, known values are: hyp length, and inside angles
        double xDiff1 = Math.cos(Math.toRadians(90 - direction1)) * baseViewDistance;
        double yDiff1 = Math.sin(Math.toRadians(90 - direction1)) * baseViewDistance;

        double xDiff2 = Math.cos(Math.toRadians(90 - direction2)) * baseViewDistance;
        double yDiff2 = Math.sin(Math.toRadians(90 - direction2)) * baseViewDistance;


        Location endpoint1 = LocationUtils.getNewLocation(startLat, startLon, yDiff1, xDiff1);
        Location endpoint2 = LocationUtils.getNewLocation(startLat, startLon, yDiff2, xDiff2);



        geoPoints.add(new GeoPoint(startLat, startLon));
        geoPoints.add(new GeoPoint(endpoint1.getLatitude(), endpoint1.getLongitude()));
        geoPoints.add(new GeoPoint(endpoint2.getLatitude(), endpoint2.getLongitude()));


      } else {
        viewAngle = 120; // panning camera

        // using two 60 degree cones instead of one 120 cone

        // triangle sides compass direction
        int direction1 = direction - viewAngle / 2;
        int direction2 = direction;
        int direction3 = direction + viewAngle / 2;


        // in meters, simulate a 2d coordinate system, known values are: hyp length, and inside angles
        double xDiff1 = Math.cos(Math.toRadians(90 - direction1)) * baseViewDistance;
        double yDiff1 = Math.sin(Math.toRadians(90 - direction1)) * baseViewDistance;

        double xDiff2 = Math.cos(Math.toRadians(90 - direction2)) * baseViewDistance;
        double yDiff2 = Math.sin(Math.toRadians(90 - direction2)) * baseViewDistance;

        double xDiff3 = Math.cos(Math.toRadians(90 - direction3)) * baseViewDistance;
        double yDiff3 = Math.sin(Math.toRadians(90 - direction3)) * baseViewDistance;


        Location endpoint1 = LocationUtils.getNewLocation(startLat, startLon, yDiff1, xDiff1);
        Location endpoint2 = LocationUtils.getNewLocation(startLat, startLon, yDiff2, xDiff2);
        Location endpoint3 = LocationUtils.getNewLocation(startLat, startLon, yDiff3, xDiff3);

        geoPoints.add(new GeoPoint(startLat, startLon));
        geoPoints.add(new GeoPoint(endpoint1.getLatitude(), endpoint1.getLongitude()));
        geoPoints.add(new GeoPoint(endpoint2.getLatitude(), endpoint2.getLongitude()));
        geoPoints.add(new GeoPoint(endpoint3.getLatitude(), endpoint3.getLongitude()));


      }





    } else {

      // circle for dome cameras
      geoPoints = Polygon.pointsAsCircle(currentPos, height * 7);

    }

    polygon.setPoints(geoPoints);
    map.getOverlayManager().add(polygon);
    EditCameraActivity.this.map.invalidate();

  }

  static final int REQUEST_IMAGE_CAPTURE = 1;

  private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }



  }



}
