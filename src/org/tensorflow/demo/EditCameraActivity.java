package org.tensorflow.demo;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;

import java.io.File;


public class EditCameraActivity extends AppCompatActivity {

  CameraRepository cameraRepository;
  CameraViewModel cameraViewModel;

  SurveillanceCamera cameraToEdit;
  SharedPreferences sharedPreferences;

  LinearLayout parentLayout;
  ImageView cameraImageView;
  MapView map;
  TextView timestampTextView;
  TextView uploadTextView;
  TextView commentsTextView;
  File cameraImage;

  IconOverlay iconOverlay;

  int cameraType;
  boolean cameraIsTrainingImage;

  private static String picturesPath = SynchronizationUtils.PICTURES_PATH;
  private static String trainingPath = SynchronizationUtils.TRAINING_IMAGES_PATH;


  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_camera);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    parentLayout = findViewById(R.id.edit_camera_container);
    cameraImageView = findViewById(R.id.edit_camera_detail_image);
    map = findViewById(R.id.edit_camera_map);
    timestampTextView = findViewById(R.id.edit_camera_timestamp_text);
    uploadTextView = findViewById(R.id.edit_camera_upload_text);
    commentsTextView = findViewById(R.id.edit_camera_comments_text);

    Intent startIntent = getIntent();

    int dbId = startIntent.getIntExtra("surveillanceCameraId", 0);

    cameraRepository = new CameraRepository(getApplication());
    cameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

    cameraToEdit = cameraRepository.findByDbId(dbId);

    cameraType = cameraToEdit.getCameraType();
    cameraIsTrainingImage = cameraToEdit.getTrainingCapture();





    if (cameraIsTrainingImage){
      parentLayout.removeView(map);


      cameraImage = new File(trainingPath + cameraToEdit.getImagePath());

      Picasso.get().load(cameraImage)
              .placeholder(R.drawable.ic_launcher)
              .into(cameraImageView);

    } else {

      cameraImage = new File(picturesPath + cameraToEdit.getThumbnailPath());

      Picasso.get().load(cameraImage)
              .placeholder(R.drawable.ic_launcher)
              .into(cameraImageView);

      map.setTilesScaledToDpi(true);
      map.setClickable(false);
      map.setMultiTouchControls(true);

      // MAPNIK fix
      // Configuration.getInstance().setUserAgentValue("github-unsurv-unsurv-android");
      // TODO add choice + backup strategy here
      map.setTileSource(TileSourceFactory.OpenTopo);

      final CustomZoomButtonsController zoomController = map.getZoomController();
      zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

      final IMapController mapController = map.getController();

      double lat = cameraToEdit.getLatitude();
      double lon = cameraToEdit.getLongitude();


      // Setting starting position and zoom level.
      GeoPoint cameraLocation = new GeoPoint(lat, lon);
      mapController.setZoom(16.0);
      mapController.setCenter(cameraLocation);

      Drawable cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(this.getResources(), R.drawable.standard_camera_marker_5_dpi, 12, null);


      iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

      map.getOverlays().add(iconOverlay);

      //detailMap.getOverlays().add(cameraMarker);
      map.invalidate();
    }



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




    // TODO listview with different images, save abort buttons, editable mapview, change upload date with + /- buttons



  }


}
