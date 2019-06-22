package org.tensorflow.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
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

  private SharedPreferences sharedPreferences;


  private BottomNavigationView bottomNavigationView;


  private  DrawView drawView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_draw_on_image);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    Intent intent = getIntent();
    int dbId = intent.getIntExtra("surveillanceCameraId", 0);

    cameraRepository = new CameraRepository(getApplication());

    currentTrainingCamera = cameraRepository.findByDbId(dbId);

    cameraType = DrawView.REGULAR_CAMERA;

    final RelativeLayout drawingRelativeLayout = findViewById(R.id.drawing_relative);

    pathToImage = SynchronizationUtils.TRAINING_IMAGES_PATH + currentTrainingCamera.getImagePath();

    imageFile = new File(pathToImage);

    drawView = new DrawView(this, currentTrainingCamera, cameraRepository);

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


    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);






    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(DrawOnTrainingImageActivity.this, DrawOnTrainingImageActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
              Intent manualCaptureIntent = new Intent(DrawOnTrainingImageActivity.this, ManualCaptureActivity.class);
              startActivity(manualCaptureIntent);
              return true;
            } else {
              Intent cameraIntent = new Intent(DrawOnTrainingImageActivity.this, DetectorActivity.class);
              startActivity(cameraIntent);
              return true;

            }

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(DrawOnTrainingImageActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(DrawOnTrainingImageActivity.this, StatisticsActivity.class);
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

    // Removes refreshButton in ActionBar. Not needed since LiveData is used for the List.
    MenuItem item = menu.findItem(R.id.action_refresh);


    return true;
  }



  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case R.id.action_settings:
        Intent settingsIntent = new Intent(DrawOnTrainingImageActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

        return true;


      case R.id.action_refresh:
        drawView.refresh();
        return true;

      case R.id.action_training:
        Intent trainingCaptureIntent = new Intent(DrawOnTrainingImageActivity.this, CaptureTrainingImageActivity.class);
        startActivity(trainingCaptureIntent);

        return true;



      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }

}
