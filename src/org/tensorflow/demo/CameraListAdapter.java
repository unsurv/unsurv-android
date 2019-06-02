package org.tensorflow.demo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CameraListAdapter extends RecyclerView.Adapter<CameraListAdapter.CameraViewHolder> {


  class CameraViewHolder extends RecyclerView.ViewHolder {
    private final ImageView thumbnailImageView;
    private final TextView latitudeTextView;
    private final TextView longitudeTextView;
    private final ImageButton deleteButton;
    private final ImageButton uploadButton;






    private CameraViewHolder(View itemView) {
      super(itemView);
      thumbnailImageView = itemView.findViewById(R.id.thumbnail_image);
      latitudeTextView = itemView.findViewById(R.id.history_item_text_view_1);
      longitudeTextView = itemView.findViewById(R.id.history_item_text_view_2);
      deleteButton = itemView.findViewById(R.id.history_item_delete_button);
      uploadButton = itemView.findViewById(R.id.history_item_upload_button);

    }

  }

  private final LayoutInflater mInflater;
  private final LinearLayout mHistoryDetails;

  private List<SurveillanceCamera> mSurveillanceCameras;

  private String picturesPath = SynchronizationUtils.picturesPath;

  private final CameraRepository cameraRepository;
  private final SharedPreferences sharedPreferences;

  CameraListAdapter(Context context, LinearLayout detailLinearLayout, Application application) {
    mInflater = LayoutInflater.from(context);
    mHistoryDetails = detailLinearLayout;
    cameraRepository = new CameraRepository(application);
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);


  }

  @Override
  public CameraViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View itemView = mInflater.inflate(R.layout.camera_recyclerview_item, parent, false);



    return new CameraViewHolder(itemView);
  }


  @Override
  public void onBindViewHolder(final CameraViewHolder holder, int position) {

    if (mSurveillanceCameras != null) {
      final SurveillanceCamera current = mSurveillanceCameras.get(position);

      String mLatitude = String.valueOf(current.getLatitude());
      String mLongitude = String.valueOf(current.getLongitude());
      File mThumbnailPicture = new File(picturesPath + current.getThumbnailPath());

      String mComment = current.getComment();

      // holder.thumbnailImageView.
      holder.latitudeTextView.setText(mLatitude);
      holder.longitudeTextView.setText(mLongitude);

      holder.deleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

          cameraRepository.deleteCameras(current);

        }
      });

      holder.uploadButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {


          SynchronizationUtils.uploadSurveillanceCamera(Collections.singletonList(current), "http://192.168.178.137:5000/", sharedPreferences, cameraRepository);

        }
      });



      Picasso.get().load(mThumbnailPicture)
              .placeholder(R.drawable.ic_launcher)
              .into(holder.thumbnailImageView);

      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Log.i("holder onClick:", "clicked position: " + holder.getAdapterPosition());


          int currentPosition = holder.getAdapterPosition();
          SurveillanceCamera currentCamera = mSurveillanceCameras.get(currentPosition);

          MapView detailMap = mHistoryDetails.findViewById(R.id.history_detail_map);
          TextView detailTimestamp = mHistoryDetails.findViewById(R.id.history_detail_timestamp);
          TextView detailUpload = mHistoryDetails.findViewById(R.id.history_detail_upload);
          ImageView detailImage = mHistoryDetails.findViewById(R.id.history_detail_image);

          File cameraImage = new File(picturesPath + currentCamera.getThumbnailPath());

          Picasso.get().load(cameraImage)
                  .placeholder(R.drawable.ic_launcher)
                  .into(detailImage);


          final IMapController mapController = detailMap.getController();

          double lat = mSurveillanceCameras.get(currentPosition).getLatitude();
          double lon = mSurveillanceCameras.get(currentPosition).getLongitude();

          // Setting starting position and zoom level.
          GeoPoint cameraLocation = new GeoPoint(lat, lon);
          mapController.setZoom(15.0);
          mapController.setCenter(cameraLocation);

          String timestamp = mSurveillanceCameras.get(currentPosition).getTimestamp();

          if (timestamp != null) {
            detailTimestamp.setText(timestamp);

          } else {
            detailTimestamp.setText(" \"Enable Capture Timestamps\" in Settings");
          }

          detailUpload.setText(mSurveillanceCameras.get(currentPosition).getTimeToSync());

          Drawable cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(view.getContext().getResources(), R.drawable.standard_camera_marker_5_dpi, 12, null);

          Marker cameraMarker = new Marker(detailMap);
          cameraMarker.setPosition(cameraLocation);
          cameraMarker.setIcon(cameraMarkerIcon);
          cameraMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
              return true;
            }
          });


          detailMap.getOverlays().add(cameraMarker);


        }
      });

    } else {
      // Covers the case of data not being ready yet.

    }
  }


  void setCameras(List<SurveillanceCamera> cameras){
    mSurveillanceCameras = cameras;
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    if (mSurveillanceCameras != null)
      return mSurveillanceCameras.size();
    else return 0;
  }


}
