package org.tensorflow.demo;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;


public class EditCameraActivity extends AppCompatActivity {

  private CameraRepository cameraRepository;
  private CameraViewModel cameraViewModel;

  private SurveillanceCamera cameraToEdit;

  private TextView timestampTextView;
  private TextView uploadTextView;
  private TextView commentsTextView;


  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_camera);

    timestampTextView = findViewById(R.id.timestamp_edit_camera);
    uploadTextView = findViewById(R.id.upload_edit_camera);
    commentsTextView = findViewById(R.id.comments_edit_camera);

    Intent startIntent = getIntent();

    int dbId = startIntent.getIntExtra("surveillanceCameraId", 0);

    cameraRepository = new CameraRepository(getApplication());
    cameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

    cameraToEdit = cameraRepository.findByDbId(dbId);


    // TODO listview with different images, save abort buttons, editable mapview, change upload date with + /- buttons



  }


}
