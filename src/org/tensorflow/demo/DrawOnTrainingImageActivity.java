package org.tensorflow.demo;

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

  private int cameraType;
  private String pathToImage;
  private File imageFile;

  private  DrawView regularDrawView;
  private  DrawView domeDrawView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_draw_on_image);

    cameraType = DrawView.REGULAR_CAMERA;

    final RelativeLayout drawingRelativeLayout = findViewById(R.id.drawing_relative);

    pathToImage = SynchronizationUtils.TRAINING_IMAGES_PATH + "asd.jpg";

    imageFile = new File(pathToImage);

    final DrawView drawView = new DrawView(this, pathToImage, cameraType);

    drawingRelativeLayout.addView(drawView, 0);

    Picasso.get().load(imageFile).into(drawView);

    addRegularCameraButton = findViewById(R.id.add_regular_camera_button);
    addDomeCameraButton = findViewById(R.id.add_dome_camera_button);

    regularDrawView = new DrawView(DrawOnTrainingImageActivity.this, pathToImage, DrawView.REGULAR_CAMERA);
    domeDrawView = new DrawView(DrawOnTrainingImageActivity.this, pathToImage, DrawView.DOME_CAMERA);

    addRegularCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        cameraType = DrawView.REGULAR_CAMERA;
        drawingRelativeLayout.removeView(drawView);
        drawingRelativeLayout.removeView(regularDrawView);
        drawingRelativeLayout.removeView(domeDrawView);

        drawingRelativeLayout.addView(regularDrawView, 0);

        Picasso.get().load(imageFile).into(regularDrawView);

      }
    });

    addDomeCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        cameraType = DrawView.DOME_CAMERA;
        drawingRelativeLayout.removeView(drawView);
        drawingRelativeLayout.removeView(regularDrawView);
        drawingRelativeLayout.removeView(domeDrawView);

        drawingRelativeLayout.addView(domeDrawView, 0);

        Picasso.get().load(imageFile).into(domeDrawView);
      }
    });





  }
}
