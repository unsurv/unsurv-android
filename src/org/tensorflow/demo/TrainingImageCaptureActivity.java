package org.tensorflow.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrainingImageCaptureActivity extends AppCompatActivity {
  private BottomNavigationView bottomNavigationView;
  private static String TAG = "TrainingImageCapture";

  private TextureView textureView;
  private Size previewsize;
  private Size jpgSizes[]=null;

  private CameraDevice cameraDevice;
  private CaptureRequest.Builder previewBuilder;
  private CameraCaptureSession previewSession;

  private ImageView capture;

  private int readStoragePermission;
  private int writeStoragePermission;
  private int cameraPermission;


  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_training_image_capture);
    textureView = findViewById(R.id.training_texture_view);
    capture = findViewById(R.id.training_capture);

    textureView.setSurfaceTextureListener(surfaceTextureListener);

    capture.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getCapture();
      }
    });



    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(TrainingImageCaptureActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            Intent cameraIntent = new Intent(TrainingImageCaptureActivity.this, DetectorActivity.class);
            startActivity(cameraIntent);
            return true;

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(TrainingImageCaptureActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(TrainingImageCaptureActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_camera).setChecked(true);
  }

  @Override
  protected void onResume() {
    super.onResume();

    Toast.makeText(getApplicationContext(),
            "Capture and mark surveillancecameras to improve accuracy",
            Toast.LENGTH_LONG).show();

    readStoragePermission = ContextCompat.checkSelfPermission(TrainingImageCaptureActivity.this,
            Manifest.permission.READ_EXTERNAL_STORAGE);
    writeStoragePermission = ContextCompat.checkSelfPermission(TrainingImageCaptureActivity.this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);
    cameraPermission = ContextCompat.checkSelfPermission(TrainingImageCaptureActivity.this,
            Manifest.permission.CAMERA);


    List<String> permissionList = new ArrayList<>();

    if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
      permissionList.add(Manifest.permission.CAMERA);
    }



    String[] neededPermissions = permissionList.toArray(new String[0]);

    if (!permissionList.isEmpty()) {
      ActivityCompat.requestPermissions(TrainingImageCaptureActivity.this, neededPermissions, 1);
    }


    if (textureView.isAvailable()) {
      openCamera();
    }


  }

  void getCapture() {

    if (cameraDevice == null) {
      return;
    }

    CameraManager manager = (CameraManager)getSystemService(CAMERA_SERVICE);

    try {
      for (String cameraId : manager.getCameraIdList()) {
        CameraCharacteristics characteristics
                = manager.getCameraCharacteristics(cameraId);

        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

        if (facing != null && facing ==
                CameraCharacteristics.LENS_FACING_FRONT) {
          continue;

        } else if (facing != null && facing ==
                CameraCharacteristics.LENS_FACING_BACK) {

          StreamConfigurationMap map = characteristics.get(
                  CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
          if (map == null) {
            continue;
          }

          jpgSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                  .getOutputSizes(ImageFormat.JPEG);

          int width = 640;
          int height = 480;

          if (jpgSizes != null && jpgSizes.length != 0) {
            for (Size size : jpgSizes) {
              if (size.getWidth() == 1024 && size.getHeight() == 768) {
                width = 1024;
                height = 768;
              }
            }
          }

          ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

          List<Surface> outputSurfaces = new ArrayList<>(2);
          outputSurfaces.add(imageReader.getSurface());
          outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

          final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
          captureBuilder.addTarget(imageReader.getSurface());
          captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

          int rotation = getWindowManager().getDefaultDisplay().getRotation();
          captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);

          ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
              Image img;
              try {
                img = imageReader.acquireLatestImage();
                ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                SynchronizationUtils.saveBytesToFile(bytes, "trainingimg.jpg", SynchronizationUtils.TRAINING_IMAGES_PATH);


              } catch (Exception e){
                Log.i(TAG, "onImageAvailable: " + e.toString());
              }

            }
          };

          HandlerThread handlerThread = new HandlerThread("takeImage");
          handlerThread.start();

          final Handler handler = new Handler((handlerThread.getLooper()));

          imageReader.setOnImageAvailableListener(imageAvailableListener, handler);

          final CameraCaptureSession.CaptureCallback  previewSSession = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
              super.onCaptureStarted(session, request, timestamp, frameNumber);
            }
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
              super.onCaptureCompleted(session, request, result);
              startCamera();
            }
          };

          cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

              try{
                cameraCaptureSession.capture(captureBuilder.build(), previewSSession, handler);

              } catch (Exception e){
                Log.i(TAG, "captureSession: " + e.toString());
              }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

            }
          }, handler);

        }


      }
    } catch (CameraAccessException e){
      Log.i(TAG, "camera access exception");
    }


  }





  private void openCamera() {

    Activity activity = TrainingImageCaptureActivity.this;
    CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

    try
    {
      String cameraId = manager.getCameraIdList()[0];
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
      StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      previewsize = map.getOutputSizes(SurfaceTexture.class)[0];

      if (ContextCompat.checkSelfPermission(TrainingImageCaptureActivity.this,
              Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
        manager.openCamera(cameraId, stateCallback, null);

      }


    }catch (Exception e) {
      Log.i(TAG, "exception on openCamera");
    }
  }

  private TextureView.SurfaceTextureListener surfaceTextureListener=new TextureView.SurfaceTextureListener() {
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
      openCamera();
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
      return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
  };

  private CameraDevice.StateCallback stateCallback=new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice camera) {
      cameraDevice=camera;
      startCamera();
    }
    @Override
    public void onDisconnected(CameraDevice camera) {
    }
    @Override
    public void onError(CameraDevice camera, int error) {
    }
  };



  void startCamera(){

    if ( cameraDevice == null || !textureView.isAvailable() || previewsize == null){
      return;
    }

    SurfaceTexture texture=textureView.getSurfaceTexture();

    if (texture == null) {
      return;
    }

    texture.setDefaultBufferSize(previewsize.getWidth(), previewsize.getHeight());
    Surface surface = new Surface(texture);

    try{
      previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

    } catch (Exception e){
      Log.i(TAG, "previewBuilder" + e.toString());
    }

    previewBuilder.addTarget(surface);

    try {
      cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
          previewSession=session;
          getChangedPreview();
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
      },null);
    }catch (Exception e){
    }
  }


  void getChangedPreview(){

    if (cameraDevice == null)
    {
      return;
    }
    previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    HandlerThread thread = new HandlerThread("changed Preview");
    thread.start();
    Handler handler = new Handler(thread.getLooper());
    try{
      previewSession.setRepeatingRequest(previewBuilder.build(), null, handler);
    } catch (Exception e){
      Log.i(TAG, "previewSessionSetRepeatingRequest" + e.toString());
    }
  }






}
