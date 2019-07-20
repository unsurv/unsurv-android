package org.tensorflow.demo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
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

import java.io.File;
import java.util.Collections;
import java.util.List;


/**
 * Used in HistoryActivity to display captures made by users.
 */

public class CameraListAdapter extends RecyclerView.Adapter<CameraListAdapter.CameraViewHolder> {


  class CameraViewHolder extends RecyclerView.ViewHolder {

    // small bar on lefthand side of view for each item in recyclerview
    private final View cameraTypeBar;

    private final ImageView thumbnailImageView;
    private final TextView topTextViewInItem;
    private final TextView bottomTextViewInItem;

    private final ImageButton deleteButton;
    private final ImageButton uploadButton;

    private final LinearLayout detailLinearLayout;



    private CameraViewHolder(View itemView) {
      super(itemView);
      cameraTypeBar = itemView.findViewById(R.id.type_bar);
      thumbnailImageView = itemView.findViewById(R.id.thumbnail_image);
      topTextViewInItem = itemView.findViewById(R.id.history_item_text_view_top);
      bottomTextViewInItem = itemView.findViewById(R.id.history_item_text_view_bottom);
      deleteButton = itemView.findViewById(R.id.history_item_delete_button);
      uploadButton = itemView.findViewById(R.id.history_item_upload_button);
      detailLinearLayout = itemView.findViewById(R.id.history_item_linear_layout);

    }

  }

  private final LayoutInflater mInflater;

  private List<SurveillanceCamera> mSurveillanceCameras;

  private final CameraRepository cameraRepository;
  private final SharedPreferences sharedPreferences;
  private final LayoutInflater layoutInflater;
  private final Context ctx;
  private CameraViewModel cameraViewModel;


  CameraListAdapter(Context context, Application application, LayoutInflater layoutInflater, CameraViewModel cameraViewModel) {
    mInflater = LayoutInflater.from(context);
    cameraRepository = new CameraRepository(application);
    this.cameraViewModel = cameraViewModel;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    this.layoutInflater = layoutInflater;
    ctx = context;

  }

  @Override
  public CameraViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View itemView = mInflater.inflate(R.layout.camera_recyclerview_item_history, parent, false);

    return new CameraViewHolder(itemView);
  }


  @Override
  public void onBindViewHolder(final CameraViewHolder holder, int position) {

    if (mSurveillanceCameras != null) {
      final SurveillanceCamera current = mSurveillanceCameras.get(position);

      final boolean currentCameraUploadComplete = current.getUploadCompleted();

      File imageFile;
      final boolean trainingCapture = current.getTrainingCapture();
      int cameraType = current.getCameraType();

      if (trainingCapture){
        // camera is a training image not a capture with obj detection
        imageFile = new File(StorageUtils.TRAINING_CAPTURES_PATH + current.getImagePath());

        //holder.detailLinearLayout.setBackgroundColor(Color.GRAY);

        holder.cameraTypeBar.setBackgroundColor(Color.GREEN);

      } else {

        // not a training capture, use correct storage path
        imageFile = new File(StorageUtils.CAMERA_CAPTURES_PATH + current.getThumbnailPath());

        switch (cameraType){
          case StorageUtils.STANDARD_CAMERA:
            holder.cameraTypeBar.setBackgroundColor(Color.parseColor("#ff5555")); // red

            break;

          case StorageUtils.DOME_CAMERA:
            holder.cameraTypeBar.setBackgroundColor(Color.BLUE);

            break;
        }

      }


      String uploadDate = current.getTimeToSync();

      String mComment = current.getComment();

      // holder.thumbnailImageView.

      holder.bottomTextViewInItem.setText(uploadDate);


      Picasso.get().load(imageFile)
              .placeholder(R.drawable.ic_launcher)
              .into(holder.thumbnailImageView);


      if (currentCameraUploadComplete){
        holder.uploadButton.setImageResource(R.drawable.ic_file_upload_green_24dp);
        holder.uploadButton.setClickable(false);
      } else {
        holder.uploadButton.setImageResource(R.drawable.ic_file_upload_grey_24dp);
      }

      holder.deleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

          // TODO move to AlertDialog
          // create popupView to ask user if he wants to delete camera
          // saves to SharedPreference if "don't ask again" checkbox ticked

          boolean quickDeleteCameras = sharedPreferences.getBoolean("quickDeleteCameras", false);

          if (quickDeleteCameras){

            cameraRepository.deleteCamera(current);
            notifyItemRemoved(holder.getAdapterPosition());

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

                  holder.thumbnailImageView.setVisibility(View.INVISIBLE);

                  if (dontAskAgainACheckBox.isChecked()) {
                    sharedPreferences.edit().putBoolean("quickDeleteCameras", true).apply();
                  }
                  cameraRepository.deleteCamera(current);
                  popupWindow.dismiss();
                  notifyItemRemoved(holder.getAdapterPosition());


                }
              });


              noButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

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

            SynchronizationUtils.uploadSurveillanceCamera(Collections.singletonList(current), "http://192.168.178.137:5000/", sharedPreferences, cameraViewModel, null, false);
          } else {
            Toast.makeText(ctx, "Camera has already been uploaded.", Toast.LENGTH_SHORT).show();
          }

        }
      });



      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

          //opens EditCameraActivity if regular capture, DrawOnTrainingImageActivity if item is a training capture.

          Log.i("holder onClick:", "clicked position: " + holder.getAdapterPosition());

          int currentPosition = holder.getAdapterPosition();
          SurveillanceCamera currentCamera = mSurveillanceCameras.get(currentPosition);


          if (trainingCapture) {
            // camera is a training image not a capture with obj detection

            // if delete was last action image view is invisible
            holder.thumbnailImageView.setVisibility(View.VISIBLE);

            Intent drawOnImageIntent = new Intent(ctx, DrawOnTrainingImageActivity.class);

            drawOnImageIntent.putExtra("surveillanceCameraId", currentCamera.getId());

            ctx.startActivity(drawOnImageIntent);


          } else {
            // camera is captured via obj detection

            // if delete was last action image view is invisible
            holder.thumbnailImageView.setVisibility(View.VISIBLE);

            Intent editCameraIntent = new Intent(ctx, EditCameraActivity.class);

            editCameraIntent.putExtra("surveillanceCameraId", currentCamera.getId());

            ctx.startActivity(editCameraIntent);

          }

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
