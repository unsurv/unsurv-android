package org.tensorflow.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CameraListAdapter extends RecyclerView.Adapter<CameraListAdapter.CameraViewHolder> {


  class CameraViewHolder extends RecyclerView.ViewHolder {
    private final ImageView thumbnailImageView;
    private final TextView latitudeTextView;
    private final TextView longitudeTextView;





    private CameraViewHolder(View itemView) {
      super(itemView);
      thumbnailImageView = itemView.findViewById(R.id.thumbnail_image);
      latitudeTextView = itemView.findViewById(R.id.latitude_text_view);
      longitudeTextView = itemView.findViewById(R.id.longitude_text_view);
    }

  }

  private final LayoutInflater mInflater;
  private final LinearLayout mHistoryDetails;

  private List<SurveillanceCamera> mSurveillanceCameras;

  private String picturesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/";

  CameraListAdapter(Context context, LinearLayout detailLinearLayout) {
    mInflater = LayoutInflater.from(context);
    mHistoryDetails = detailLinearLayout;}

  @Override
  public CameraViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View itemView = mInflater.inflate(R.layout.camera_recyclerview_item, parent, false);



    return new CameraViewHolder(itemView);
  }


  @Override
  public void onBindViewHolder(final CameraViewHolder holder, int position) {

    if (mSurveillanceCameras != null) {
      SurveillanceCamera current = mSurveillanceCameras.get(position);

      String mLatitude = String.valueOf(current.getLatitude());
      String mLongitude = String.valueOf(current.getLongitude());
      File mThumbnailPicture = new File(picturesPath + current.getThumbnailPath());

      String mComment = current.getComment();

      // holder.thumbnailImageView.
      holder.latitudeTextView.setText(mLatitude);
      holder.longitudeTextView.setText(mLongitude);
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
