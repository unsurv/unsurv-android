package org.unsurv;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;

import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
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
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Allows the user to review and edit SurveillanceCamera captures, launched if user clicks on a
 * non training SurveillanceCamera in HistoryActivity
 */

public class OrganizeActivity extends AppCompatActivity {

  BottomNavigationView bottomNavigationView;

  SharedPreferences sharedPreferences;
  CameraRepository cameraRepository;

  boolean offlineMode;

  List<SurveillanceCamera> cameras;
  List<OverlayItem> overlayItemsToDisplay;
  ItemizedIconOverlay<OverlayItem> itemizedIconOverlay;

  MapView map;
  IMapController mapController;
  OverlayManager overlayManager;

  GeoPoint centerMap;
  double standardZoom;

  List<Polyline> lines = new ArrayList<>();

  EditText centerLat;
  EditText centerLon;
  EditText gridLength;
  EditText gridHeight;
  EditText gridRows;
  EditText gridColumns;

  Button resetButton;
  Button drawButton;
  Button backButton;

  Context context;
  Resources resources;


  @Override
  protected void onResume() {

    String oldLat = sharedPreferences.getString("gridCenterLat", "");
    String oldLon = sharedPreferences.getString("gridCenterLon", "");
    String oldZoom = sharedPreferences.getString("gridZoom", "");

    if (!oldLat.isEmpty() && !oldLon.isEmpty() && !oldZoom.isEmpty()) {
      centerMap = new GeoPoint(Double.parseDouble(oldLat), Double.parseDouble(oldLon));
      mapController.setZoom(Double.parseDouble(oldZoom));
      mapController.setCenter(centerMap);
    }

    cameras = cameraRepository.getAllCameras();

    //sharedPreferences.edit().putBoolean("offlineMode", true).apply();
    // offlineMode = sharedPreferences.getBoolean("offlineMode", true);


    deleteMarkers();
    populateWithMarkers();

    super.onResume();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_organize);

    context = this;
    resources = context.getResources();

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    cameraRepository = new CameraRepository(getApplication());

    centerLat = findViewById(R.id.organize_center_lat_edit);
    centerLon = findViewById(R.id.organize_center_lon_edit);

    // default grid is 1000m x 1000m, 5 rows 5 columns
    gridLength = findViewById(R.id.organize_length_edit);
    gridLength.setText("3000");

    gridHeight = findViewById(R.id.organize_height_edit);
    gridHeight.setText("3000");

    gridRows = findViewById(R.id.organize_rows_edit);
    gridRows.setText("5");

    gridColumns = findViewById(R.id.organize_columns_edit);
    gridColumns.setText("5");

    resetButton = findViewById(R.id.organize_reset);
    drawButton = findViewById(R.id.organize_draw);
    backButton = findViewById(R.id.organize_back);

    overlayItemsToDisplay = new ArrayList<>();

    map = findViewById(R.id.organize_camera_map);

    mapController = map.getController();
    overlayManager = map.getOverlayManager();

    CopyrightOverlay copyrightOverlay = new CopyrightOverlay(context);
    overlayManager.add(copyrightOverlay);

    map.setTilesScaledToDpi(true);
    map.setClickable(false);
    map.setMultiTouchControls(true);

    offlineMode = true;


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
                        10,
                        15,
                        256,
                        ".png",
                        new String[]{""}
                ));

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


    // remove big + and - buttons at the bottom of the map
    final CustomZoomButtonsController zoomController = map.getZoomController();
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);


    standardZoom = 5.0;
    sharedPreferences.edit().putString("gridZoom", String.valueOf(standardZoom)).apply();

    String oldLat = sharedPreferences.getString("gridCenterLat", "");
    String oldLon = sharedPreferences.getString("gridCenterLon", "");
    String oldZoom = sharedPreferences.getString("gridZoom", "");



    if (!oldLat.isEmpty() && !oldLon.isEmpty() && !oldZoom.isEmpty()) {
      centerMap = new GeoPoint(Double.parseDouble(oldLat), Double.parseDouble(oldLon));
      mapController.setZoom(Double.parseDouble(oldZoom));
      mapController.setCenter(centerMap);
    } else {
      // Setting starting position and zoom level.
      centerMap = new GeoPoint(49.9955, 8.2856);
      mapController.setZoom(13.0);
      mapController.setCenter(centerMap);
    }



    // refresh values after 200ms delay
    map.addMapListener(new DelayedMapListener(new MapListener() {
      @Override
      public boolean onScroll(ScrollEvent event) {
        IGeoPoint centerAfterScroll = map.getMapCenter();

        centerMap = new GeoPoint(centerAfterScroll);
        sharedPreferences.edit().putString("gridCenterLat", String.valueOf(centerMap.getLatitude())).apply();
        sharedPreferences.edit().putString("gridCenterLon", String.valueOf(centerMap.getLongitude())).apply();


        centerLat.setText(String.valueOf(centerAfterScroll.getLatitude()));
        centerLon.setText(String.valueOf(centerAfterScroll.getLongitude()));

        return false;

      }

      @Override
      public boolean onZoom(ZoomEvent event) {

        sharedPreferences.edit().putString("gridZoom", String.valueOf(map.getZoomLevelDouble())).apply();


        return false;

      }
    }, 200));



    resetButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {


        new AlertDialog.Builder(context)
                .setTitle("Clear Data?")
                .setMessage("Do you want to clear this data?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {

                    sharedPreferences.edit().remove("gridCenterLat").apply();
                    sharedPreferences.edit().remove("gridCenterLon").apply();
                    sharedPreferences.edit().remove("gridZoom").apply();

                    deleteGrid();

                    centerMap = new GeoPoint(50.972, 10.107);
                    mapController.setZoom(standardZoom);
                    mapController.setCenter(centerMap);
                    redrawMap();

                    Toast.makeText(context, "Successfully cleared data.", Toast.LENGTH_LONG).show();
                  }
                })
                .setNegativeButton("No", null)
                .show();

      }
    });

    drawButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        int length = Integer.parseInt(gridLength.getText().toString());
        int height = Integer.parseInt(gridHeight.getText().toString());

        int rows = Integer.parseInt(gridRows.getText().toString());
        int columns = Integer.parseInt(gridColumns.getText().toString());

        deleteGrid();
        drawGrid(centerMap.getLatitude(), centerMap.getLongitude(), length, height, rows, columns);


      }
    });

    backButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
          Intent manualCaptureIntent = new Intent(OrganizeActivity.this, ManualCaptureActivity.class);
          startActivity(manualCaptureIntent);
        } else {
          Intent cameraIntent = new Intent(OrganizeActivity.this, DetectorActivity.class);
          startActivity(cameraIntent);

        }


      }
    });



    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(OrganizeActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
              Intent manualCaptureIntent = new Intent(OrganizeActivity.this, ManualCaptureActivity.class);
              startActivity(manualCaptureIntent);
              return true;
            } else {
              Intent cameraIntent = new Intent(OrganizeActivity.this, DetectorActivity.class);
              startActivity(cameraIntent);
              return true;

            }

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(OrganizeActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(OrganizeActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_map).setChecked(true);

  }


  void drawGrid(double centerLat, double centerLon, int length, int height, int rows, int columns){

    // outer rectangle of grid

    // left edge is center lon - length/2
    GeoPoint topLeft = new GeoPoint(LocationUtils.getNewLocation(
            centerLat, centerLon, height/2d, -length/2d));

    GeoPoint topRight = new GeoPoint(LocationUtils.getNewLocation(
            centerLat, centerLon, height/2d, length/2d));

    GeoPoint bottomRight = new GeoPoint(LocationUtils.getNewLocation(
            centerLat, centerLon, -height/2d, length/2d));

    GeoPoint bottomLeft = new GeoPoint(LocationUtils.getNewLocation(
            centerLat, centerLon, -height/2d, -length/2d));

    List<GeoPoint> outerRect = new ArrayList<>(Arrays.asList(topLeft, topRight, bottomRight, bottomLeft, topLeft));

    drawLine(outerRect);

    // stepsize of dividing lines
    int partHeight = height / rows;
    int partLength = length / columns;

    GeoPoint tmpRowStartpoint;
    GeoPoint tmpRowEndpoint;

    // one "step" south from topleft, that's the top point of the first divider line for rows
    tmpRowStartpoint = new GeoPoint(LocationUtils.getNewLocation(
            topLeft.getLatitude(), topLeft.getLongitude(), -partHeight, 0));

    // rows , ex: we need 4 lines to divide into 5 parts
    for (int i = 0; i < rows - 1; i++) {

      tmpRowEndpoint = new GeoPoint(LocationUtils.getNewLocation(
              tmpRowStartpoint.getLatitude(), tmpRowStartpoint.getLongitude(), 0, length));

      List<GeoPoint> gridLinePoints = new ArrayList<>(Arrays.asList(tmpRowStartpoint, tmpRowEndpoint));

      drawLine(gridLinePoints);

      // new startpoint is one "step" south of previous startpoint
      tmpRowStartpoint = new GeoPoint(LocationUtils.getNewLocation(
              tmpRowStartpoint.getLatitude(), tmpRowStartpoint.getLongitude(), -partHeight, 0));

    }

    GeoPoint tmpColStartpoint;
    GeoPoint tmpColEndpoint;

    tmpColStartpoint = new GeoPoint(LocationUtils.getNewLocation(
            topLeft.getLatitude(), topLeft.getLongitude(), 0, partLength));

    // columns
    for (int j = 0; j < columns - 1; j++) {

      tmpColEndpoint = new GeoPoint(LocationUtils.getNewLocation(
              tmpColStartpoint.getLatitude(), tmpColStartpoint.getLongitude(), -height, 0));

      List<GeoPoint> gridLinePoints = new ArrayList<>(Arrays.asList(tmpColStartpoint, tmpColEndpoint));

      drawLine(gridLinePoints);

      // new startpoint is one "step" east of previous startpoint
      tmpColStartpoint = new GeoPoint(LocationUtils.getNewLocation(
              tmpColStartpoint.getLatitude(), tmpColStartpoint.getLongitude(), 0, partLength));

    }


  }

  void drawLine(List<GeoPoint> geoPoints){

    Polyline polyline = new Polyline();

    polyline.setPoints(geoPoints);

    int hotPink = Color.argb(127, 255, 0, 255);

    polyline.setColor(hotPink);

    lines.add(polyline);

    overlayManager.add(polyline);

    redrawMap();

  }

  void redrawMap() {
    map.invalidate();
  }

  void deleteGrid() {
    overlayManager.removeAll(lines);
    redrawMap();
  }


  // use different Markers for different types when moving marker bug is fixed
  void populateWithMarkers() {
    List<SurveillanceCamera> filteredCameras = new ArrayList<>();

    // TODO create db call for non training captures
    // filter for non training captures
    for (SurveillanceCamera camera : cameras) {
      if (!camera.getTrainingCapture()) {
        filteredCameras.add(camera);
      }
    }

    for (SurveillanceCamera nonTrainingCapture : filteredCameras) {

      GeoPoint geoPoint = new GeoPoint(
              nonTrainingCapture.getLatitude(),
              nonTrainingCapture.getLongitude());

      OverlayItem marker = new OverlayItem("placeholder", "placeholder", geoPoint);


      overlayItemsToDisplay.add(marker);

    }

    Drawable cameraMarkerIcon = getDrawable(R.drawable.simple_marker_5dpi);

    itemizedIconOverlay = new ItemizedIconOverlay<>(overlayItemsToDisplay,
            cameraMarkerIcon,
            new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {


      @Override
      public boolean onItemSingleTapUp(int index, OverlayItem item) {
        Toast.makeText(context, "id:" + index +
                        ", lat:" + item.getPoint().getLatitude() +
                        ", lon:" + item.getPoint().getLongitude(),
                Toast.LENGTH_LONG).show();

        return false;
      }

      @Override
      public boolean onItemLongPress(int index, OverlayItem item) {
        return false;
      }

      }, context);

    overlayManager.add(itemizedIconOverlay);


  }

  void deleteMarkers(){
    overlayManager.remove(itemizedIconOverlay);
    redrawMap();
  }

}
