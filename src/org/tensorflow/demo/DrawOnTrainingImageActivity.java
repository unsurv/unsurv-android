package org.tensorflow.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import java.io.File;

public class DrawOnTrainingImageActivity extends AppCompatActivity {

  private static String TAG = "DrawOnTrainingImage";

  private ImageButton addRegularCameraButton;
  private ImageButton addDomeCameraButton;
  private ImageButton saveButton;
  private ImageButton undoButton;

  private int cameraType;
  private String pathToImage;
  private File imageFile;
  private CameraRepository cameraRepository;
  private SurveillanceCamera currentTrainingCamera;


  private  DrawView drawView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_draw_on_image);

    Intent intent = getIntent();
    long dbId = intent.getLongExtra("surveillanceCameraId", 0);

    cameraRepository = new CameraRepository(getApplication());

    currentTrainingCamera = cameraRepository.findByDbId(dbId);

    cameraType = DrawView.REGULAR_CAMERA;

    final RelativeLayout drawingRelativeLayout = findViewById(R.id.drawing_relative);

    pathToImage = SynchronizationUtils.TRAINING_IMAGES_PATH + currentTrainingCamera.getId() + ".jpg";

    imageFile = new File(pathToImage);

    drawView = new DrawView(this, pathToImage, cameraType);

    drawingRelativeLayout.addView(drawView, 0);

    Picasso.get().load(imageFile).into(drawView);

    addRegularCameraButton = findViewById(R.id.add_regular_camera_button);
    addDomeCameraButton = findViewById(R.id.add_dome_camera_button);
    saveButton = findViewById(R.id.drawing_save_button);
    undoButton = findViewById(R.id.drawing_undo);


    addRegularCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        cameraType = DrawView.REGULAR_CAMERA;
        drawView.setCameraType(cameraType);

      }
    });

    addDomeCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        cameraType = DrawView.DOME_CAMERA;
        drawView.setCameraType(cameraType);

      }
    });

    saveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        drawView.saveCamera();
      }
    });

    undoButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        drawView.undo();
      }
    });


  }
}
