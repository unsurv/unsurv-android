package org.tensorflow.demo;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ListView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

  private CameraViewModel mCameraViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_history);


    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.camera_recyclerview);
    final CameraListAdapter adapter = new CameraListAdapter(this);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    mCameraViewModel = ViewModelProviders.of(this).get(CameraViewModel.class);

    mCameraViewModel.getAllCameras().observe(this, new Observer<List<SurveillanceCamera>>() {
      @Override
      public void onChanged(@Nullable List<SurveillanceCamera> surveillanceCameras) {
        adapter.setCameras(surveillanceCameras);
      }
    });

  }





}
