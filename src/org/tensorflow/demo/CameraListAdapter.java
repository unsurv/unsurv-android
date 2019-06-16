package org.tensorflow.demo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
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

  private IconOverlay iconOverlay;

  private String picturesPath = SynchronizationUtils.PICTURES_PATH;

  private final CameraRepository cameraRepository;
  private final SharedPreferences sharedPreferences;
  private final LayoutInflater layoutInflater;
  private final Context ctx;


  CameraListAdapter(Context context, LinearLayout detailLinearLayout, Application application, LayoutInflater layoutInflater) {
    mInflater = LayoutInflater.from(context);
    mHistoryDetails = detailLinearLayout;
    cameraRepository = new CameraRepository(application);
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    this.layoutInflater = layoutInflater;
    ctx = context;


  }

  @Override
  public CameraViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View itemView = mInflater.inflate(R.layout.camera_recyclerview_item, parent, false);



    return new CameraViewHolder(itemView);
  }


  @Override
  public void onBindViewHolder(final CameraViewHolder holder, final int position) {

    if (mSurveillanceCameras != null) {
      final SurveillanceCamera current = mSurveillanceCameras.get(position);

      final boolean currentCameraUploadComplete = current.getUploadCompleted();

      String mLatitude = String.valueOf(current.getLatitude());
      String mLongitude = String.valueOf(current.getLongitude());
      File mThumbnailPicture = new File(picturesPath + current.getThumbnailPath());

      String mComment = current.getComment();

      // holder.thumbnailImageView.
      holder.latitudeTextView.setText(mLatitude);
      holder.longitudeTextView.setText(mLongitude);

      if (currentCameraUploadComplete){
        holder.uploadButton.setImageResource(R.drawable.ic_file_upload_green_24dp);
        holder.uploadButton.setClickable(false);
      }

      holder.deleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {


          // create popupView to ask user if he wants to delete camera
          // saves preference if checkbox ticked

          boolean quickDeleteCameras = sharedPreferences.getBoolean("quickDeleteCameras", false);

          if (quickDeleteCameras){

            cameraRepository.deleteCameras(current);
            notifyItemRemoved(position);

          } else {

            View popupView = layoutInflater.inflate(R.layout.delete_camera_popup, null);

            final CheckBox dontAskAgainACheckBox = popupView.findViewById(R.id.delete_popup_dont_show_again_checkbox);

            Button yesButton = popupView.findViewById(R.id.delete_popup_yes_button);
            Button noButton = popupView.findViewById(R.id.delete_popup_no_button);

            final PopupWindow popupWindow =
                    new PopupWindow(popupView,
                            RecyclerView.LayoutParams.WRAP_CONTENT,
                            RecyclerView.LayoutParams.WRAP_CONTENT);

            if (!popupWindow.isShowing()) {


              popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

              yesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                  if (dontAskAgainACheckBox.isChecked()) {
                    sharedPreferences.edit().putBoolean("quickDeleteCameras", true).apply();
                  }
                  cameraRepository.deleteCameras(current);
                  popupWindow.dismiss();
                  notifyItemRemoved(position);


                }
              });


              noButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                  if (dontAskAgainACheckBox.isChecked()) {
                    sharedPreferences.edit().putBoolean("allowServerQueries", false).apply();
                    sharedPreferences.edit().putBoolean("askForConnections", false).apply();

                  }
                  popupWindow.dismiss();

                }
              });
            }

          }



        }
      });

      holder.uploadButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

          if (!currentCameraUploadComplete) {

            SynchronizationUtils.uploadSurveillanceCamera(Collections.singletonList(current), "http://192.168.178.137:5000/", sharedPreferences, cameraRepository);
            holder.uploadButton.setImageResource(R.drawable.ic_file_upload_green_24dp);

          } else {
            Toast.makeText(ctx, "Camera has already been uploaded.", Toast.LENGTH_SHORT).show();
          }

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

          detailMap.getOverlays().remove(iconOverlay);

          final IMapController mapController = detailMap.getController();

          double lat = mSurveillanceCameras.get(currentPosition).getLatitude();
          double lon = mSurveillanceCameras.get(currentPosition).getLongitude();

          // Setting starting position and zoom level.
          GeoPoint cameraLocation = new GeoPoint(lat, lon);
          mapController.setZoom(16.0);
          mapController.setCenter(cameraLocation);

          String timestamp = mSurveillanceCameras.get(currentPosition).getTimestamp();

          if (timestamp != null) {
            detailTimestamp.setText(timestamp);

          } else {
            detailTimestamp.setText(" \"Enable Capture Timestamps\" in Settings");
          }

          detailUpload.setText(mSurveillanceCameras.get(currentPosition).getTimeToSync());

          Drawable cameraMarkerIcon = ResourcesCompat.getDrawableForDensity(view.getContext().getResources(), R.drawable.standard_camera_marker_5_dpi, 12, null);


          iconOverlay = new BottomAnchorIconOverlay(cameraLocation, cameraMarkerIcon);

          detailMap.getOverlays().add(iconOverlay);

          //detailMap.getOverlays().add(cameraMarker);
          detailMap.invalidate();


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
