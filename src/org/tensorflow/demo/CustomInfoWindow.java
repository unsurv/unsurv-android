package org.tensorflow.demo;


import android.os.Environment;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.io.File;

public class CustomInfoWindow extends MarkerInfoWindow {

  private ImageView infoImage;
  private TextView infoLatestTimestamp;
  private TextView infoComment;


  private String picturesPath;


  public CustomInfoWindow( final MapView mapView) {
    super(R.layout.camera_info_bubble, mapView);


    infoImage = mView.findViewById(R.id.bubble_camera_image);
    infoLatestTimestamp = mView.findViewById(R.id.bubble_title);
    infoComment = mView.findViewById(R.id.bubble_description);

    picturesPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/";

  }

  @Override
  public void onOpen(Object item) {
    super.onOpen(item);

    Marker marker = (Marker) item;


    SynchronizedCamera selectedCamera = (SynchronizedCamera) marker.getRelatedObject();

    File thumbnail = new File(
            picturesPath + selectedCamera.getImagePath());

    Picasso.get().load(thumbnail).into(infoImage);


    infoLatestTimestamp.setText(selectedCamera.getLastUpdated());
    infoComment.setText(selectedCamera.getComments());

  }
}