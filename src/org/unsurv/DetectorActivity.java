/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.unsurv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;

import org.json.JSONArray;
import org.unsurv.OverlayView.DrawCallback;
import org.unsurv.env.BorderedText;
import org.unsurv.env.ImageUtils;
import org.unsurv.env.Logger;
import org.unsurv.tracking.MultiBoxTracker;

// MY CHANGES
import java.io.FileOutputStream;

import static android.content.ContentValues.TAG;

// TODO ASK for location permission, do in tutorial
// TODO remove bboxes when confidence moves below min confidence
// TODO abortcurrent capture method

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener, SensorEventListener  {


  private static boolean startDebugMode = false;

  private static final Logger LOGGER = new Logger();
  // Configuration values for the prepackaged multibox model.
  private static final int MB_INPUT_SIZE = 224;
  private static final int MB_IMAGE_MEAN = 128;
  private static final float MB_IMAGE_STD = 128;
  private static final String MB_INPUT_NAME = "ResizeBilinear";
  private static final String MB_OUTPUT_LOCATIONS_NAME = "output_locations/Reshape";
  private static final String MB_OUTPUT_SCORES_NAME = "output_scores/Reshape";
  private static final String MB_MODEL_FILE = "file:///android_asset/multibox_model.pb";
  private static final String MB_LOCATION_FILE =
          "file:///android_asset/multibox_location_priors.txt";
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final String TF_OD_API_MODEL_FILE =
          "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

  // Configuration values for tiny-yolo-voc. Note that the graph is not included with TensorFlow and
  // must be manually placed in the assets/ directory by the user.
  // Graphs and models downloaded from http://pjreddie.com/darknet/yolo/ may be converted e.g. via
  // DarkFlow (https://github.com/thtrieu/darkflow). Sample command:
  // ./flow --model cfg/tiny-yolo-voc.cfg --load bin/tiny-yolo-voc.weights --savepb --verbalise
  private static final String YOLO_MODEL_FILE = "file:///android_asset/graph-tiny-yolo-voc.pb";
  private static final int YOLO_INPUT_SIZE = 416;
  private static final String YOLO_INPUT_NAME = "input";
  private static final String YOLO_OUTPUT_NAMES = "output";
  private static final int YOLO_BLOCK_SIZE = 32;

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.  Optionally use legacy Multibox (trained using an older version of the API)
  // or YOLO.
  private enum DetectorMode {
    TF_OD_API, MULTIBOX, YOLO;
  }
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;

  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

  private static final float MINIMUM_CONFIDENCE_MULTIBOX = 0.1f;
  private static final float MINIMUM_CONFIDENCE_YOLO = 0.25f;


  private static final boolean MAINTAIN_ASPECT = MODE == DetectorMode.YOLO;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;

  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private byte[] luminanceCopy;

  private BorderedText borderedText;

  private String picturesPath = StorageUtils.CAMERA_CAPTURES_PATH;
  private static long timeLastPictureTaken = 0;
  private static final int DELAY_BETWEEN_CAPTURES = 500;

  Location currentBestLocation;
  private double cameraLatitude;
  private double cameraLongitude;
  private double cameraAccuracy;
  AsyncLocationGetter gpsLocation = new AsyncLocationGetter(DetectorActivity.this);

  private SensorManager mSensorManager;
  private Sensor accelerometer;
  private Sensor magneticField;

  private float[] mAccelerometerReading = new float[3];
  private float[] mMagnetometerReading = new float[3];

  private float[] mRotationMatrix = new float[9];
  private float[] mOrientationAngles = new float[3];

  private double azimuth;
  private double pitch;
  private double roll;



  private ImageView locationStatusView;
  private ImageView photoStatusView;

  private final int STATUS_RED = 0;
  private final int STATUS_GREEN = 1;

  private int photoStatus = STATUS_GREEN;

  private Boolean buttonCaptureEnabled;

  final File pictureDirectory = new File(picturesPath);

  private Boolean captureButtonIsBeingHeld = false;

  private ArrayList<CameraCapture> pooledCameraCaptures = new ArrayList<>();

  private long timePoolCaptureStarted = 0;
  private int poolDurationInMillis = 5000; //TODO add setting to set duration.
  private int delayBetweenPoolsInMillis = 10000; //TODO add setting to set duration.

  private Long currentTime;

  private Boolean isCapturing = false;

  Button manualCameraCapture;
  Button trainingCameraCapture;
  ImageButton toOrganize;

  private SharedPreferences sharedPreferences;

  private LocationManager locationManager;
  private MyLocationListener locationListener;
  private Location currentLocation;

  BottomNavigationView bottomNavigationView;
  Context context;




  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(null);

    startDebugMode = false;

    if (startDebugMode) {
      Intent debugIntent = new Intent(DetectorActivity.this, DebugActivity.class);
      startActivity(debugIntent);
    }

    context = this;

    locationListener = new MyLocationListener();


    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // if tutorial not completed start tutorial activity
    if (!sharedPreferences.getBoolean("tutorialCompleted", false)) {
      Intent tutorialIntent = new Intent(DetectorActivity.this, TutorialActivity.class);
      startActivity(tutorialIntent);
    }


    if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
      Intent manualCaptureIntent = new Intent(DetectorActivity.this, ManualCaptureActivity.class);
      startActivity(manualCaptureIntent);
    }

    if (sharedPreferences.getBoolean("alwaysEnableTrainingCapture", false)) {
      Intent trainingCaptureIntent = new Intent(DetectorActivity.this, CaptureTrainingImageActivity.class);
      startActivity(trainingCaptureIntent);
    }



    manualCameraCapture = findViewById(R.id.manual_capture_button);
    trainingCameraCapture = findViewById(R.id.training_capture_button);
    toOrganize = findViewById(R.id.detector_to_grid_button);

    manualCameraCapture.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent manualCaptureIntent = new Intent(DetectorActivity.this, ManualCaptureActivity.class);
        startActivity(manualCaptureIntent);
      }
    });

    trainingCameraCapture.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent trainingCaptureIntent = new Intent(DetectorActivity.this, CaptureTrainingImageActivity.class);
        startActivity(trainingCaptureIntent);
      }
    });

    toOrganize.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent organizeIntent = new Intent(DetectorActivity.this, OrganizeActivity.class);
        startActivity(organizeIntent);
      }
    });

    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


    bottomNavigationView = findViewById(R.id.navigation);

    // Handle bottom navigation bar clicks
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(DetectorActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:

            if (sharedPreferences.getBoolean("alwaysEnableManualCapture", false)) {
              Intent manualCaptureIntent = new Intent(DetectorActivity.this, ManualCaptureActivity.class);
              startActivity(manualCaptureIntent);
              return true;
            } else {
              Intent cameraIntent = new Intent(DetectorActivity.this, DetectorActivity.class);
              startActivity(cameraIntent);
              return true;
            }


          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(DetectorActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(DetectorActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_camera).setChecked(true);


    buttonCaptureEnabled = sharedPreferences.getBoolean("buttonCapture", false);

    // Create folder structure in storage/.../Pictures/
    pictureDirectory.mkdirs();

    photoStatusView = findViewById(R.id.photo_status_view);

  }



  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //return super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.actionbar, menu);

    // No need for a refresh Button in a capture activity.
    MenuItem refreshItem = menu.findItem(R.id.action_refresh);
    refreshItem.setVisible(false);
    return true;
  }


  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // Do something here if sensor accuracy changes.
    // You must implement this callback in your code.
  }

  @Override
  public void onResume() {

    // Get updates from the accelerometer and magnetometer at a constant rate.
    // To make batch operations more efficient and reduce power consumption,
    // provide support for delaying updates to the application.
    //
    // In this example, the sensor reporting delay is small enough such that
    // the application receives an update before the system checks the sensor
    // readings again.

    if (accelerometer != null) {
      mSensorManager.registerListener(this, accelerometer,
              SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    if (magneticField != null) {
      mSensorManager.registerListener(this, magneticField,
              SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    gpsLocation.execute();

    BottomNavigationBadgeHelper.setBadgesFromSharedPreferences(bottomNavigationView, context);


    super.onResume();


  }

  @Override
  public void onPause() {
    super.onPause();

    // Don't receive any more updates from either sensor.
    gpsLocation.cancel(true);
    locationManager.removeUpdates(locationListener);
    mSensorManager.unregisterListener(this);
  }

  @Override
  public synchronized void onDestroy() {
    super.onDestroy();
  }

  // Get readings from accelerometer and magnetometer. To simplify calculations,
  // consider storing these readings as unit vectors.
  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      System.arraycopy(event.values, 0, mAccelerometerReading,
              0, mAccelerometerReading.length);
    } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
      System.arraycopy(event.values, 0, mMagnetometerReading,
              0, mMagnetometerReading.length);
    }
  }

  // Compute the three orientation angles based on the most recent readings from
  // the device's accelerometer and magnetometer.
  public void updateOrientationAngles() {
    // Update rotation matrix, which is needed to update orientation angles.
    SensorManager.getRotationMatrix(mRotationMatrix, null,
            mAccelerometerReading, mMagnetometerReading);

    // "mRotationMatrix" now has up-to-date information.

    SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

    // "mOrientationAngles" now has up-to-date information.
  }

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;
    if (MODE == DetectorMode.YOLO) {
      detector =
          TensorFlowYoloDetector.create(
              getAssets(),
              YOLO_MODEL_FILE,
              YOLO_INPUT_SIZE,
              YOLO_INPUT_NAME,
              YOLO_OUTPUT_NAMES,
              YOLO_BLOCK_SIZE);
      cropSize = YOLO_INPUT_SIZE;
    } else if (MODE == DetectorMode.MULTIBOX) {
      detector =
          TensorFlowMultiBoxDetector.create(
              getAssets(),
              MB_MODEL_FILE,
              MB_LOCATION_FILE,
              MB_IMAGE_MEAN,
              MB_IMAGE_STD,
              MB_INPUT_NAME,
              MB_OUTPUT_LOCATIONS_NAME,
              MB_OUTPUT_SCORES_NAME);
      cropSize = MB_INPUT_SIZE;
    } else {
      try {
        detector = TensorFlowObjectDetectionAPIModel.create(
            getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        cropSize = TF_OD_API_INPUT_SIZE;
      } catch (final IOException e) {
        LOGGER.e("Exception initializing classifier!", e);
        Toast toast =
            Toast.makeText(
                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
        toast.show();
        finish();
      }
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            if (!isDebug()) {
              return;
            }
            final Bitmap copy = cropCopyBitmap;
            if (copy == null) {
              return;
            }

            final int backgroundColor = Color.argb(100, 0, 0, 0);
            canvas.drawColor(backgroundColor);

            final Matrix matrix = new Matrix();
            final float scaleFactor = 2;
            matrix.postScale(scaleFactor, scaleFactor);
            matrix.postTranslate(
                canvas.getWidth() - copy.getWidth() * scaleFactor,
                canvas.getHeight() - copy.getHeight() * scaleFactor);
            canvas.drawBitmap(copy, matrix, new Paint());

            final Vector<String> lines = new Vector<>();
            if (detector != null) {
              final String statString = detector.getStatString();
              final String[] statLines = statString.split("\n");
              lines.addAll(Arrays.asList(statLines));

            }
            lines.add("");

            lines.add("Frame: " + previewWidth + "x" + previewHeight);
            lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
            lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
            lines.add("Rotation: " + sensorOrientation);
            lines.add("Inference time: " + lastProcessingTimeMs + "ms");

            borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
          }
        });
  }

  OverlayView trackingOverlay;

  @Override
  protected void processImage() {

    updateOrientationAngles();

    azimuth = mOrientationAngles[0];
    pitch = mOrientationAngles[1];
    roll = mOrientationAngles[2];

    ++timestamp;
    final long currTimestamp = timestamp;
    byte[] originalLuminance = getLuminance();
    tracker.onFrame(
        previewWidth,
        previewHeight,
        getLuminanceStride(),
        sensorOrientation,
        originalLuminance,
        timestamp);
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    if (luminanceCopy == null) {
      luminanceCopy = new byte[originalLuminance.length];
    }
    System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
              case MULTIBOX:
                minimumConfidence = MINIMUM_CONFIDENCE_MULTIBOX;
                break;
              case YOLO:
                minimumConfidence = MINIMUM_CONFIDENCE_YOLO;
                break;
            }

            final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<>();


            currentTime = System.currentTimeMillis();

            // TODO add single capture mode


            if (!buttonCaptureEnabled) {

              // Abort in time after duration completed and before delay completed
              if (timePoolCaptureStarted + delayBetweenPoolsInMillis > currentTime &&
                      timePoolCaptureStarted + poolDurationInMillis < currentTime) {

                isCapturing = false;
                // End pooling, analyze pool and clear list.
                if (!pooledCameraCaptures.isEmpty()) {

                  /*
                  CameraCapture cameraCapture1 = new CameraCapture(99.9f,
                          "190754878_thumbnail.jpg", "190754878.jpg",
                          10, 120, 50, 140,
                          49.99452, 8.24688,
                          10.3345, 0 + 3.14/18, 12.3313, 170.3332);

                  CameraCapture cameraCapture2 = new CameraCapture(99.9f,
                          "190754878_thumbnail.jpg", "190754878.jpg",
                          10, 120, 50, 140,
                          49.99455, 8.24715,
                          10.3345, 0 + 3.14/36, 12.3313, 170.3332);

                  CameraCapture cameraCapture3 = new CameraCapture(99.9f,
                          "190754878_thumbnail.jpg", "190754878.jpg",
                          10, 120, 50, 140,
                          49.99458, 8.24735,
                          10.3345, 0 - 3.14/36, 12.3313, 170.3332);

                  CameraCapture cameraCapture4 = new CameraCapture(99.9f,
                          "190754878_thumbnail.jpg", "190754878.jpg",
                          10, 120, 50, 140,
                          49.99455, 8.24750,
                          10.3345, 0 - 3.14/18, 12.3313, 170.3332);

                  pooledCameraCaptures.add(cameraCapture1);
                  pooledCameraCaptures.add(cameraCapture2);
                  pooledCameraCaptures.add(cameraCapture3);
                  pooledCameraCaptures.add(cameraCapture4);
                   */

                  processCapturePool(pooledCameraCaptures);
                  pooledCameraCaptures.clear();
                }

              }

              else if (timePoolCaptureStarted + delayBetweenPoolsInMillis < currentTime) {
                // Set new startpoint if old startpoint is longer than delayBetweenPoolsinMillis ago.
                // This starts a new pool.
                timePoolCaptureStarted = currentTime;
              }

              else if (timePoolCaptureStarted + poolDurationInMillis > currentTime) {
                // after pool started but before pool ended
                isCapturing = true;
              }


            } else {
              // If Button is held, enable pooling
              captureButtonIsBeingHeld = isCapturing;
            }



            for (final Classifier.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);



                //TODO move this to processImage()
                // Change photoStatusView depending on delay. photoStatusView starts as green from xml.
                runOnUiThread(new Runnable(){
                  @Override
                  public void run(){
                    if (isCapturing) {
                      photoStatusView.setImageResource(R.drawable.ic_camera_alt_green_24dp);
                      photoStatus = STATUS_GREEN;
                    }

                    if (!isCapturing) {
                      photoStatusView.setImageResource(R.drawable.ic_camera_alt_red_24dp);
                      photoStatus = STATUS_RED;

                    }
                  }
                });

                if (isCapturing) {
                  processSurveillanceCameraCapture(result, location, pictureDirectory);
                }

                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
              }
            }



            tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
            trackingOverlay.postInvalidate();

            requestRender();
            computingDetection = false;
          }
        });
  }




  void processSurveillanceCameraCapture(Classifier.Recognition result, RectF location, File pictureDirectory) {

    HashMap<String, Integer> typeMap = new HashMap<>();
    typeMap.put("surveillance camera", 0);
    typeMap.put("dome camera", 1);

    if (result.getConfidence() > 0.95 && currentTime -
            timeLastPictureTaken > DELAY_BETWEEN_CAPTURES && isCapturing) {


      LOGGER.i("TOOK PICTURE -------------------------------------- ");

      FileOutputStream out = null;
      FileOutputStream thumbnailOut = null;

      try {
        Matrix turnMatrix = new Matrix();
        turnMatrix.postRotate(90);

        long currentTime = System.currentTimeMillis();

        String imageFilename = currentTime + ".jpg";
        String thumbnailFilename = currentTime + "_thumbnail.jpg";

        // Create files, for now with timestamp as name
        File outputFile = new File(pictureDirectory, imageFilename);
        File thumbnailFile = new File(pictureDirectory, thumbnailFilename);

        out = new FileOutputStream(outputFile);
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

        thumbnailOut = new FileOutputStream(thumbnailFile);

        // Get detection edges in px values.
        int xThumbnail = Math.round(location.left);
        int yThumbnail =  Math.round(location.top);
        int widthThumbnail = Math.round(location.width());
        int heightThumbnail = Math.round(location.height());

        Bitmap thumbnail = Bitmap.createBitmap(croppedBitmap, xThumbnail, yThumbnail, widthThumbnail, heightThumbnail);
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, thumbnailOut);

        int cameraLeft = Math.round(location.left);
        int cameraRight = Math.round(location.right);
        int cameraTop = Math.round(location.top);
        int cameraBottom = Math.round(location.bottom);
        if (currentBestLocation != null){
          cameraLatitude = currentBestLocation.getLatitude();
          cameraLongitude = currentBestLocation.getLongitude();
          cameraAccuracy = currentBestLocation.getAccuracy();

        }

        //updateOrientationAngles();
        // rad to degree
        //azimuth = mOrientationAngles[0]*(180/Math.PI);
        //pitch = mOrientationAngles[1]*(180/Math.PI);
        //roll = mOrientationAngles[2]*(180/Math.PI);

        SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

        CameraCapture currentCamera = new CameraCapture(
                typeMap.get(result.getTitle()),
                result.getConfidence(),
                thumbnailFilename,
                imageFilename,
                cameraLeft, cameraRight, cameraTop, cameraBottom,
                cameraLatitude, cameraLongitude, cameraAccuracy,
                azimuth, pitch, roll

        );

        Log.d(TAG, "datetimestamp: " + timestampIso8601.format(new Date(currentTime)) + " cameraTitle: " + result.getTitle());

        pooledCameraCaptures.add(currentCamera);


        if (gpsLocation.getStatus() != AsyncTask.Status.RUNNING){
          AsyncLocationGetter gpsLocation = new AsyncLocationGetter(DetectorActivity.this);
          gpsLocation.execute();
        }


      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        timeLastPictureTaken = System.currentTimeMillis();
        try {
          if (out != null) {
            out.close();
          }

          if (thumbnailOut != null) {
            thumbnailOut.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }

      }

    }
  }

  void processCapturePool(List<CameraCapture> cameraPool){
    // Intersects all capture headings to find true position of a surveillance camera.
    // Adds a new SurveillanceCamera to database when done processing.

    // repository to get db access
    CameraViewModel cameraViewModel = new CameraViewModel(getApplication());

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    List<Location> allIntersectsfromCaptures = new ArrayList<>();

    CameraCapture biggestConfidence = cameraPool.get(0); // is sorted

    JSONArray allCaptureFilenames = new JSONArray();

    // filter for one type

    SparseIntArray occurencesPerType = new SparseIntArray(3);

    int standardCount = 0;
    int domeCount = 0;
    int unknownCount = 0;
    occurencesPerType.put(StorageUtils.FIXED_CAMERA, standardCount);
    occurencesPerType.put(StorageUtils.DOME_CAMERA, domeCount);
    occurencesPerType.put(StorageUtils.PANNING_CAMERA, unknownCount);

    for (CameraCapture capture : cameraPool){

      switch (capture.getCameraType()){

        case StorageUtils.FIXED_CAMERA:
          standardCount++;
          occurencesPerType.put(StorageUtils.FIXED_CAMERA, standardCount);
          break;

        case StorageUtils.DOME_CAMERA:
          domeCount++;
          occurencesPerType.put(StorageUtils.DOME_CAMERA, domeCount);
          break;

        case StorageUtils.PANNING_CAMERA:
          unknownCount++;
          occurencesPerType.put(StorageUtils.PANNING_CAMERA, unknownCount);
          break;

      }

    }

    int maxCount = Collections.max(Arrays.asList(standardCount, domeCount, unknownCount));

    int mostCommonTypeInPool = occurencesPerType.indexOfValue(maxCount); // index in Map = type of camera


    // Get Capture with biggest confidence for thumbnail/picture.
    for (int k=0; k < cameraPool.size(); k++) {
      allCaptureFilenames.put(cameraPool.get(k).getThumbnailPath());
      // TODO fix ChooseImageAdapter to only show thumbnail images while all images are added here
      // allCaptureFilenames.put(cameraPool.get(k).getImagePath());

      if (cameraPool.get(k).getConfidence() > biggestConfidence.getConfidence() && cameraPool.get(k).getCameraType() == mostCommonTypeInPool) {
        biggestConfidence = cameraPool.get(k);
      }
    }

    // Get all combinations. Loop through all except last.
    for (int i=0; i < cameraPool.size() - 1; i++) {

      CameraCapture firstCaptureToIntersect = cameraPool.get(i);


      // Combine every ith element with remaining ones.
      for (int j=i+1; j < cameraPool.size(); j++){

        CameraCapture secondCaptureToIntersect = cameraPool.get(j);

        Location intersect = firstCaptureToIntersect.intersectWith(secondCaptureToIntersect);

        if (intersect != null) {
          allIntersectsfromCaptures.add(intersect);
        }
      }
    }

    try{

      Location intersectReference = allIntersectsfromCaptures.get(0);
      List<Pair<Double, Double>> intersectsInCoordinates = LocationUtils.transferLocationsTo2dCoordinates(allIntersectsfromCaptures);

      Location cameraEstimate = LocationUtils.approximateCameraPosition(intersectsInCoordinates, intersectReference);


      SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
      timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

      Random random = new Random();

      long minDelay = Long.parseLong(sharedPreferences.getString("minUploadDelay", "21600000")); // 6 h
      long maxDelay = Long.parseLong(sharedPreferences.getString("maxUploadDelay", "259200000")); // 3 d

      long timeframe = maxDelay - minDelay;

      long randomDelay = Math.round(timeframe * random.nextDouble()); // minDelay < x < maxDelay

      currentTime = System.currentTimeMillis();

      boolean useTimestamp = sharedPreferences.getBoolean("enableCaptureTimestamps", false);
      String captureFilenames = allCaptureFilenames.toString();

      if (useTimestamp) {

        cameraViewModel.insert(new SurveillanceCamera(
                mostCommonTypeInPool,
                0,
                -1,
                0,
                5,
                15,
                biggestConfidence.getThumbnailPath(),
                biggestConfidence.getImagePath(),
                null,
                cameraEstimate.getLatitude(),
                cameraEstimate.getLongitude(),
                sharedPreferences.getString("comment", ""),
                timestampIso8601.format(new Date(currentTime)),
                timestampIso8601.format(new Date(currentTime + randomDelay)),
                false,
                false,
                false,
                false,
                "",
                captureFilenames

        ));

        Toast.makeText(this, "successfully captured camera", Toast.LENGTH_SHORT).show();
        BottomNavigationBadgeHelper.incrementBadge(bottomNavigationView, context, R.id.bottom_navigation_history, 1);

      } else {

        cameraViewModel.insert(new SurveillanceCamera(
                mostCommonTypeInPool,
                0,
                -1,
                0,
                5,
                15,
                biggestConfidence.getThumbnailPath(),
                biggestConfidence.getImagePath(),
                null,
                cameraEstimate.getLatitude(),
                cameraEstimate.getLongitude(),
                sharedPreferences.getString("comment", ""),
                null,
                timestampIso8601.format(new Date(currentTime + randomDelay)),
                false,
                false,
                false,
                false,
                "",
                captureFilenames

        ));

        Toast.makeText(this, "successfully captured camera", Toast.LENGTH_SHORT).show();
        BottomNavigationBadgeHelper.incrementBadge(bottomNavigationView, context, R.id.bottom_navigation_history, 1);

      }

    } catch (Exception e){
      Log.i(TAG, "failed to create a capture pool");
    }

  }


  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onSetDebug(final boolean debug) {
    detector.enableStatLogging(debug);
  }


  private class AsyncLocationGetter extends AsyncTask<Void, Integer, Location> {

    /**
     * Retrieves user location and updates currentBestLocation if sensor data is
     * more accurate / recent.
     *
     * @param params none.
     * @return Location.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */

    //TODO put location handling in own class, maybe use osmdroid implementation

    private final Context mContext;


    String locationProvider = LocationManager.GPS_PROVIDER;

    @Override
    protected void onPreExecute() {
      locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

      try{
        locationManager.requestLocationUpdates(locationProvider, 1000, 0, locationListener);
      }
      catch (SecurityException se) {
        Log.d(TAG, "getLocation: SECURITY ERROR", se);
      }
    }

    @Override
    protected Location doInBackground(Void... voids) {
      return null;
    }

    @Override
    protected void onPostExecute(Location location) {
      super.onPostExecute(location);
      currentBestLocation = location;


    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);

    }

    private AsyncLocationGetter(Context context) {
      this.mContext = context;

    }

  }


  public class MyLocationListener implements LocationListener{

    @Override
    public void onLocationChanged(Location location) {
      if (currentBestLocation == null) {
        currentBestLocation = currentLocation;
      }

      try {
        currentLocation = location;
        final float locationAccuracy = location.getAccuracy();

        locationStatusView = findViewById(R.id.location_status_view);
        photoStatusView = findViewById(R.id.photo_status_view);

        runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                          // Displays location accuracy (radius with 68 % confidence) for debugging purposes.
                          // locationDebugTextView.setText("GPS Accuracy:\n" + String.valueOf(locationAccuracy));
                          // locationDebugTextView.setTextSize(10);


                          if (locationAccuracy > 15) {
                            locationStatusView.setImageResource(R.drawable.ic_my_location_red_24dp);
                          } else if (locationAccuracy < 15 && locationAccuracy > 3 ) {
                            locationStatusView.setImageResource(R.drawable.ic_my_location_orange_24dp);
                          } else if (locationAccuracy < 5) {
                            locationStatusView.setImageResource(R.drawable.ic_my_location_green_24dp);
                          }

                        }
                      }
        );

        Log.i(TAG, "onLocationChanged:\n" + location.getLatitude() + "\n" + location.getLongitude() + "\n" + location.getAccuracy());

        // Compare best location vs current location.
        if (isBetterLocation(currentLocation, currentBestLocation)) {
          currentBestLocation = currentLocation;
        }

      }
      catch (Exception e){
        Log.d(TAG, "onLocationChanged: Location getting Error" , e);
        Toast.makeText(getApplicationContext(),"Unable to get Location", Toast.LENGTH_LONG).show();
      }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      Log.i("onStatusChanged", "onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider) {
      Log.i("onProviderEnabled", "onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
      Log.i("OnProviderDisabled", "OnProviderDisabled");
    }
  }

  private boolean isBetterLocation(Location location, Location currentBestLocation){
    if (currentBestLocation == null) {
      // A new location is always better than no location
      return true;
    }

    // Check whether the new location fix is newer or older
    long timeDelta = location.getTime() - currentBestLocation.getTime();
    boolean isNewer = timeDelta > 0;

    // Check whether the new location fix is more or less accurate
    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
    boolean isLessAccurate = accuracyDelta > 0;
    boolean isMoreAccurate = accuracyDelta < 0;
    boolean isSignificantlyLessAccurate = accuracyDelta > 50;


    // Determine location quality using a combination of timeliness and accuracy
    if (isMoreAccurate) {
      return true;
    } else if (isNewer && !isLessAccurate) {
      return true;
    } else if (isNewer && !isSignificantlyLessAccurate) {
      return true;
    }
    return false;

  }

}
