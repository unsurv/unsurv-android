package org.unsurv;

import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
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

/**
 * Used to draw rectangles on TrainingImages to train the Tensorflow mobilenet ssd model.
 * Drawn rectangles will be converted to corresponding xml files suitable for imglabel on the server.
 */

public class DrawOnTrainingImageActivity extends AppCompatActivity {

  private static String TAG = "DrawOnTrainingImage";

  ImageButton addRegularCameraButton;
  ImageButton addDomeCameraButton;
  ImageButton saveButton;
  ImageButton undoButton;

  String pathToImage;
  CameraRepository cameraRepository;

  private int cameraType;
  private File imageFile;
  private CameraViewModel cameraViewModel;
  private SurveillanceCamera currentTrainingCamera;

  private Context context;

  private SharedPreferences sharedPreferences;


  BottomNavigationView bottomNavigationView;


  private  DrawView drawView;

  @Override
  protected void onResume() {

    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);

    super.onResume();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_draw_on_image);

    context = this;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    // activity is launched with intent and TrainingImage dbId in IntentExtra
    Intent intent = getIntent();
    int dbId = intent.getIntExtra("surveillanceCameraId", 0);


    cameraRepository = new CameraRepository(getApplication());
    cameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

    currentTrainingCamera = cameraRepository.findByDbId(dbId);

    cameraType = StorageUtils.FIXED_CAMERA;

    pathToImage = StorageUtils.TRAINING_CAPTURES_PATH + currentTrainingCamera.getImagePath();

    imageFile = new File(pathToImage);

    final RelativeLayout parentRelativeLayout = findViewById(R.id.drawing_relative);

    final ViewTreeObserver viewTreeObserver = parentRelativeLayout.getViewTreeObserver();

    // when view visibility changes i.e. when view gets drawn.
    // create custom DrawView with correct image
    viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {

        drawView = new DrawView(context, currentTrainingCamera, cameraViewModel);

        // force drawView to be 4:3
        int parentLayoutWidth = parentRelativeLayout.getWidth();
        int drawViewHeight = (int) Math.ceil(4/3.0 * parentLayoutWidth);

        // effectively "match parent" for layout_width and 1.33 * width for layout_height for a total 4:3 format
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(parentLayoutWidth, drawViewHeight);

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        parentRelativeLayout.addView(drawView, 0, layoutParams);

        Picasso.get().load(imageFile).into(drawView);


        // start drawing 0.5 sec after capture to give db some time to save data
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            drawView.refresh();
          }
        }, 500);

        // just do this once
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

        cameraType = StorageUtils.FIXED_CAMERA;
        drawView.setCameraType(cameraType);

      }
    });

    addDomeCameraButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        cameraType = StorageUtils.DOME_CAMERA;
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

    androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(DrawOnTrainingImageActivity.this, HistoryActivity.class);
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
    menu.removeItem(item.getItemId());

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
