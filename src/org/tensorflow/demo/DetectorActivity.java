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

package org.tensorflow.demo;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.tracking.MultiBoxTracker;
import org.tensorflow.demo.R; // Explicit import needed for internal Google builds.

// MY CHANGES
import java.io.FileOutputStream;
import android.os.Environment;

import static android.content.ContentValues.TAG;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(null);
    //surveillanceCameras.add(new SurveillanceCamera(picturesPath + "/nsurv/227312830_thumbnail.jpg", picturesPath + "/nsurv/227312830.jpg", new RectF(1.22f, 1.44f, 1.62f, 1.61f), new Location("")));
    //SurveillanceCamera surveillanceCamera = new SurveillanceCamera(picturesPath + "/73457629_thumbnail.jpg", picturesPath + "/nsurv//73457629.jpg", 10, 20, 0, 40, 50.0005, 8.2832, 10.3345, "no comment");
    cameraRoomDatabase = CameraRoomDatabase.getDatabase(this);

  }

  private CameraRoomDatabase cameraRoomDatabase;


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

  // MY CHANGES
  private SurveillanceCamera currentCamera;
  private static String picturesPath = Environment.getExternalStoragePublicDirectory(
  Environment.DIRECTORY_PICTURES).getAbsolutePath();
  private static long TIME_LAST_PICTURE_TAKEN = SystemClock.uptimeMillis();
  private static final int DELAY_BETWEEN_CAPTURES = 5000;

  Location currentBestLocation;
  private Location currentLocation;
  private double cameraLatitude;
  private double cameraLongitude;
  private double cameraAccuracy;
  private String locationProvider = LocationManager.GPS_PROVIDER;
  AsyncLocationGetter gpsLocation = new AsyncLocationGetter(DetectorActivity.this);

  ArrayList<SurveillanceCamera> surveillanceCameras = new ArrayList<>();






  // CHANGES END



  // MY CHANGES




  // END CHANGES





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

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
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

            final Vector<String> lines = new Vector<String>();
            if (detector != null) {
              final String statString = detector.getStatString();
              final String[] statLines = statString.split("\n");
              for (final String line : statLines) {
                lines.add(line);
              }
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

            // MY CHANGES



            // CHANGES END



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
                new LinkedList<Classifier.Recognition>();

            Button toHistoryButton = (Button) findViewById(R.id.to_history);
            toHistoryButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                Intent historyIntent = new Intent(DetectorActivity.this, HistoryActivity.class);
                startActivity(historyIntent);
              }
            });


            for (final Classifier.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                // MY CHANGES
                LOGGER.i("Box Coordinates: " + location.toShortString() + "\nConfidence: " + result.getConfidence());
                File pictureDirectory = new File(picturesPath + "/nsurv/");
                pictureDirectory.mkdirs();

                File outputFile = new File(pictureDirectory, SystemClock.uptimeMillis() + ".jpg");
                File thumbnailFile = new File(pictureDirectory, SystemClock.uptimeMillis() + "_thumbnail.jpg");

                if (result.getConfidence() > 0.95 && SystemClock.uptimeMillis() -
                        TIME_LAST_PICTURE_TAKEN > DELAY_BETWEEN_CAPTURES) {

                  LOGGER.i("TOOK PICTURE -------------------------------------- ");

                  FileOutputStream out = null;
                  FileOutputStream thumbnailOut;

                  try {
                    Matrix turnMatrix = new Matrix();
                    turnMatrix.postRotate(90);

                    out = new FileOutputStream(outputFile);

                    int xThumbnail = Math.round(location.left);
                    int yThumbnail =  Math.round(location.top);
                    int widthThumbnail = Math.round(location.width());
                    int heightThumbnail = Math.round(location.height());


                    LOGGER.i(String.format("function coords %s %s %s %s", xThumbnail, yThumbnail, widthThumbnail, heightThumbnail));

                    // TODO FIX THUMBNAIL! POSS CAUSES: ORIENTATION? xy width height? USE CROPPED BITMAP!!!

                    //Bitmap inputPicture = Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

                    // Bitmap thumbnail = Bitmap.createBitmap(rgbFrameBitmap, xThumbnail, yThumbnail, widthThumbnail, heightThumbnail);
                    thumbnailOut = new FileOutputStream(thumbnailFile);
                    Bitmap thumbnail = Bitmap.createBitmap(croppedBitmap, xThumbnail, yThumbnail, widthThumbnail, heightThumbnail);
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, thumbnailOut);

                    int cameraLeft = Math.round(location.left);
                    int cameraRight = Math.round(location.right);
                    int cameraTop = Math.round(location.top);
                    int cameraBottom = Math.round(location.bottom);
                    if (currentBestLocation != null){
                      cameraLatitude = currentBestLocation.getLatitude();
                      cameraLongitude = currentBestLocation.getLongitude();
                      cameraAccuracy = currentBestLocation.getAccuracy();

                    }

                    currentCamera = new SurveillanceCamera(thumbnailFile.getPath(), outputFile.getPath(), cameraLeft, cameraRight, cameraTop, cameraBottom, cameraLatitude, cameraLongitude, cameraAccuracy, "no comment");
                    surveillanceCameras.add(currentCamera);

                    cameraRoomDatabase.surveillanceCameraDao().insert(currentCamera);


                    if (gpsLocation.getStatus() != AsyncTask.Status.RUNNING){
                      AsyncLocationGetter gpsLocation = new AsyncLocationGetter(DetectorActivity.this);
                      gpsLocation.execute();
                    }


                    // END CHANGES



                  } catch (Exception e) {
                    e.printStackTrace();
                  } finally {
                    TIME_LAST_PICTURE_TAKEN = SystemClock.uptimeMillis();
                    try {
                      if (out != null) {
                        out.close();
                      }
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }

                }

                // CHNAGES END


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

    private final Context mContext;
    private Location currentLocation;

    private String locationProvider = LocationManager.GPS_PROVIDER;

    private LocationManager locationManager;
    private myLocationListener locationListener;
    TextView locationUpdater;


    @Override
    protected void onPreExecute() {
      locationListener = new myLocationListener();
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
      if (currentLocation != null){
        locationUpdater.setText(String.format(Locale.GERMANY ,"lat: %f \nlong: %f \nacc: %f", currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getAccuracy()));
      }


    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);

    }

    private AsyncLocationGetter(Context context) {
      this.mContext = context;

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

    private class myLocationListener implements LocationListener{

      @Override
      public void onLocationChanged(Location location) {
        if (currentBestLocation == null) {
          currentBestLocation = currentLocation;
        }

        try {

          currentLocation = location;

          Log.i(TAG, "onLocationChanged:\n" + location.getLatitude() + "\n" + location.getLongitude());

          // compare best location vs current location
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



  }



}
