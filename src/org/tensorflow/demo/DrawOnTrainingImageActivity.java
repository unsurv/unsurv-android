package org.tensorflow.demo;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;

import java.io.File;

public class DrawOnTrainingImageActivity extends AppCompatActivity {

  private static String TAG = "DrawOnTrainingImage";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_draw_on_image);

    //ImageView trainingImageView = findViewById(R.id.training_image_drawing);

    LinearLayout drawingLinearLayout = findViewById(R.id.drawing_linear);

    String pathToImage = SynchronizationUtils.TRAINING_IMAGES_PATH + "asd.jpg";

    File imageFile = new File(pathToImage);

    DrawView drawView = new DrawView(this, pathToImage);

    drawingLinearLayout.addView(drawView);

    Picasso.get().load(imageFile).into(drawView);

  }
}
