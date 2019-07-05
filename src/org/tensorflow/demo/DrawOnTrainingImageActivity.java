package org.tensorflow.demo;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
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
  private CameraViewModel cameraViewModel;
  private SurveillanceCamera currentTrainingCamera;

  private Context context;

  private SharedPreferences sharedPreferences;


  private BottomNavigationView bottomNavigationView;


  private  DrawView drawView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_draw_on_image);

    context = this;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    Intent intent = getIntent();
    int dbId = intent.getIntExtra("surveillanceCameraId", 0);

    cameraRepository = new CameraRepository(getApplication());
    cameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

    currentTrainingCamera = cameraRepository.findByDbId(dbId);

    cameraType = DrawView.REGULAR_CAMERA;

    pathToImage = SynchronizationUtils.TRAINING_IMAGES_PATH + currentTrainingCamera.getImagePath();

    imageFile = new File(pathToImage);

    final RelativeLayout parentRelativeLayout = findViewById(R.id.drawing_relative);

    final ViewTreeObserver viewTreeObserver = parentRelativeLayout.getViewTreeObserver();

    viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {

        drawView = new DrawView(context, currentTrainingCamera, cameraViewModel);

        // force drawView to be 4:3
        int parentLayoutWidth = parentRelativeLayout.getWidth();

        int drawViewHeight = (int) Math.ceil(4/3.0 * parentLayoutWidth);

        // effectively "match parent" for layout_width and 1.33 * width for height for a total 4:3 format
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(parentLayoutWidth, drawViewHeight);

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        parentRelativeLayout.addView(drawView, 0, layoutParams);

        Picasso.get().load(imageFile).into(drawView);


        // start drawing activity 0.5 sec after capture to give db some time to save data
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            drawView.refresh();
          }
        }, 500);

        parentRelativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

      }
    });

    addRegularCameraButton = findViewById(R.id.add_regular_camera_button);
    addDomeCameraButton = findViewById(R.id.add_dome_camera_button);
    saveButton = findViewById(R.id.drawing_save_button);
    undoButton = findViewById(R.id.drawing_undo);


    addRegularCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        cameraType = SynchronizationUtils.STANDARD_CAMERA;
        drawView.setCameraType(cameraType);

      }
    });

    addDomeCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        cameraType = SynchronizationUtils.DOME_CAMERA;
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
