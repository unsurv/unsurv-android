package org.unsurv;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import java.io.File;

/**
 * Lets the user pick a specific image for his captured surveillance camera.
 */
public class ChooseImageAdapter extends RecyclerView.Adapter<ChooseImageAdapter.ChooseViewHolder> {

  class ChooseViewHolder extends RecyclerView.ViewHolder {
    View itemView;

    private ChooseViewHolder(View iView){
      super(iView);
      itemView = iView;
    }

  }

  String TAG = "choose Image adapter:";
  private static String cameraCapturesPath = StorageUtils.CAMERA_CAPTURES_PATH;


  Context context;
  private String[] mFilenames;
  private LayoutInflater layoutInflater;
  private ImageView mChosenCameraImageView;
  private SurveillanceCamera mCurrentSurveillanceCamera;
  CameraRepository mCameraRepository;


  ChooseImageAdapter(Context context,
                     String[] filenames,
                     ImageView chosenCameraImageView,
                     SurveillanceCamera currentCamera,
                     CameraRepository cameraRepository){
    this.context = context;
    layoutInflater = LayoutInflater.from(this.context);
    mFilenames = filenames;
    mChosenCameraImageView = chosenCameraImageView;
    mCurrentSurveillanceCamera = currentCamera;
    mCameraRepository = cameraRepository;

  }



  @Override @NonNull
  public ChooseImageAdapter.ChooseViewHolder onCreateViewHolder(ViewGroup parent,
                                                                int viewType) {

    View itemView = layoutInflater.inflate(R.layout.camera_recyclerview_item_edit_camera, parent, false);


    return new ChooseViewHolder(itemView);

  }

  @Override
  public void onBindViewHolder(final @NonNull ChooseViewHolder holder,int i) {

    ImageView imgView = holder.itemView.findViewById(R.id.choose_image_edit_camera);
    ImageView checkmarkView = holder.itemView.findViewById(R.id.choose_image_checkmark);

    // start with small check mark invisible, currently chosen image will get a check mark later
    checkmarkView.setVisibility(View.INVISIBLE);
    String filePath;

    try {

      filePath = mFilenames[i];
      File imgFile = new File(cameraCapturesPath + filePath);

      Picasso.get().load(imgFile)
              .placeholder(R.drawable.ic_camera_alt_grey_50dp)
              .into(imgView);

      // display little check mark when reaching currently used image
      if (filePath.equals(mCurrentSurveillanceCamera.getThumbnailPath())){
        checkmarkView.setVisibility(View.VISIBLE);
      }

    } catch (Exception e) {
      Log.i(TAG, e.toString());
    }

    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        // change main ImageView to show chosen image
        String chosenImageFilePath = mFilenames[holder.getAdapterPosition()];
        File imgFile = new File(cameraCapturesPath + chosenImageFilePath);

        Picasso.get().load(imgFile)
                .placeholder(R.drawable.ic_camera_alt_grey_50dp)
                .into(mChosenCameraImageView);

        // change camera obj to reflect choice
        // this will be saved when the save button is pressed in the parent EditCameraActivity
        mCurrentSurveillanceCamera.setThumbnailPath(chosenImageFilePath);

      }

    });

  }



  @Override
  public int getItemCount() {
    return mFilenames.length;
  }
}
