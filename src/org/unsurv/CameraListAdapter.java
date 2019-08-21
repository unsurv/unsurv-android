package org.unsurv;

import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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


    private CameraViewHolder(View itemView) {
      super(itemView);
      cameraTypeBar = itemView.findViewById(R.id.type_bar);
      thumbnailImageView = itemView.findViewById(R.id.thumbnail_image);
      topTextViewInItem = itemView.findViewById(R.id.history_item_text_view_top);
      bottomTextViewInItem = itemView.findViewById(R.id.history_item_text_view_bottom);
      deleteButton = itemView.findViewById(R.id.history_item_delete_button);
      uploadButton = itemView.findViewById(R.id.history_item_upload_button);

    }

  }

  private final LayoutInflater mInflater;

  private List<SurveillanceCamera> mSurveillanceCameras;

  private final CameraRepository cameraRepository;
  private final SharedPreferences sharedPreferences;
  private final LayoutInflater layoutInflater;
  private final Context ctx;
  private CameraViewModel cameraViewModel;

  private BroadcastReceiver br;
  private IntentFilter intentFilter;
  private LocalBroadcastManager localBroadcastManager;


  CameraListAdapter(Context context, Application application, LayoutInflater layoutInflater, CameraViewModel cameraViewModel) {
    mInflater = LayoutInflater.from(context);
    cameraRepository = new CameraRepository(application);
    this.cameraViewModel = cameraViewModel;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    this.layoutInflater = layoutInflater;
    ctx = context;

    localBroadcastManager = LocalBroadcastManager.getInstance(ctx);


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

          case StorageUtils.UNKNOWN_CAMERA:
            holder.cameraTypeBar.setBackgroundColor(Color.parseColor("#9101b5")); // purple
        }

      }


      String uploadDate = current.getTimeToSync();

      String mComment = current.getComment();

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


            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

            View dontAskAgainLinearLayout = layoutInflater.inflate(R.layout.alert_dialog_dont_ask_again, null);
            final CheckBox dontAskAgainCheckBox = dontAskAgainLinearLayout.findViewById(R.id.delete_popup_dont_show_again_checkbox);
            alertDialogBuilder.setView(dontAskAgainLinearLayout);

            alertDialogBuilder.setTitle("Do you want to permanently delete this camera?");

            alertDialogBuilder.setMessage(null);

            alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                if (dontAskAgainCheckBox.isChecked()) {
                  sharedPreferences.edit().putBoolean("quickDeleteCameras", true).apply();
                }

                cameraRepository.deleteCamera(current);
                notifyItemRemoved(holder.getAdapterPosition());
              }
            });

            alertDialogBuilder.setNegativeButton("No", null);
            alertDialogBuilder.show();

          }

        }
      });

      holder.uploadButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

          if (!currentCameraUploadComplete) {

            final String baseUrl = sharedPreferences.getString("synchronizationUrl", null);

            // If api key is expired set up a LocalBroadCastReceiver to start the upload
            // as soon as a new api key has been acquired. Then start getting a new API key.
            if (SynchronizationUtils.isApiKeyExpired(sharedPreferences, ctx)){

              br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                  SynchronizationUtils.uploadSurveillanceCamera(Collections.singletonList(current), baseUrl, sharedPreferences, cameraViewModel, null, false);

                  localBroadcastManager.unregisterReceiver(br);
                }
              };

              intentFilter = new IntentFilter("org.unsurv.API_KEY_CHANGED");

              localBroadcastManager.registerReceiver(br, intentFilter);
            } else {
              // API key is not expired, just upload
              SynchronizationUtils.uploadSurveillanceCamera(Collections.singletonList(current), baseUrl, sharedPreferences, cameraViewModel, null, false);

            }


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

  private void displayPopUpBeforeDeleting(String message, final String deleteSizeInBytes, final String pathToClear, final Context context){

    new AlertDialog.Builder(context)
            .setTitle("Are you sure?")
            .setMessage(message)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                StorageUtils.deleteAllFilesInDirectory(pathToClear);
                Toast.makeText(context, "Freed up " + deleteSizeInBytes + " MB of storage.", Toast.LENGTH_SHORT).show();

              }
            })
            .setNegativeButton("No", null)
            .show();

  }


}
