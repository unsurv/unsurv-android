package org.unsurv;


import android.content.SharedPreferences;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.io.File;
import java.util.Collections;

/**
 * Used when clustering in MapActivity is used.
 * If user clicks on a marker this window is used to show additional data.
 * Currently clustering is disabled
 */

public class CustomInfoWindow extends MarkerInfoWindow {

  private ImageView infoImage;
  private TextView infoLatestTimestamp;
  private TextView infoComment;


  private String picturesPath;

  private SharedPreferences sharedPreferences;


  public CustomInfoWindow(final MapView mapView, SharedPreferences sharedPreferences) {
    super(R.layout.camera_info_bubble, mapView);

    this.sharedPreferences = sharedPreferences;
    infoImage = mView.findViewById(R.id.bubble_camera_image);
    infoLatestTimestamp = mView.findViewById(R.id.bubble_title);
    infoComment = mView.findViewById(R.id.bubble_description);

    picturesPath = StorageUtils.SYNCHRONIZED_PATH;

  }

  @Override
  public void onOpen(Object item) {
    super.onOpen(item);

    Marker marker = (Marker) item;


    final SynchronizedCamera selectedCamera = (SynchronizedCamera) marker.getRelatedObject();

    File thumbnail = new File(
            picturesPath + selectedCamera.getImagePath());

    Picasso.get()
            .load(thumbnail)
            .placeholder(R.drawable.ic_file_download_grey_48dp)
            .into(infoImage);

    infoImage.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        String baseUrl = sharedPreferences.getString("synchronizationURL", null);
        SynchronizationUtils.downloadImagesFromServer(
                baseUrl,
                Collections.singletonList(selectedCamera),
                sharedPreferences);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {

            File updatedThumbnail = new File(picturesPath + selectedCamera.getImagePath());

            infoImage.setImageDrawable(null);
            Picasso.get().load(updatedThumbnail)
                    .placeholder(R.drawable.ic_file_download_grey_48dp)
                    .into(infoImage);

          }
        }, 500);


      }
    });


    infoLatestTimestamp.setText(selectedCamera.getLastUpdated());
    infoComment.setText(selectedCamera.getComments());

  }
}