package org.tensorflow.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import java.io.File;


public class ChooseImageAdapter extends RecyclerView.Adapter<ChooseImageAdapter.ChooseViewHolder> {

  class ChooseViewHolder extends RecyclerView.ViewHolder {
    View itemView;

    private ChooseViewHolder(View iView){
      super(iView);
      itemView = iView;
    }

  }

  private static String TAG = "choose Image adapter:";
  private static String imagesPath = StorageUtils.CAPTURES_PATH;


  private Context ctx;
  private String[] mFilenames;
  private LayoutInflater layoutInflater;
  private ImageView mChosenCameraImageView;
  private SurveillanceCamera mCurrentSurveillanceCamera;
  private CameraRepository mCameraRepository;


  ChooseImageAdapter(Context context,
                     String[] filenames,
                     ImageView chosenCameraImageView,
                     SurveillanceCamera currentCamera,
                     CameraRepository cameraRepository){
    ctx = context;
    layoutInflater = LayoutInflater.from(ctx);
    mFilenames = filenames;
    mChosenCameraImageView = chosenCameraImageView;
    mCurrentSurveillanceCamera = currentCamera;
    mCameraRepository = cameraRepository;

  }



  @Override
  public ChooseImageAdapter.ChooseViewHolder onCreateViewHolder(ViewGroup parent,
                                                                int viewType) {
    // TODO display only one checkmark, need to pass big imageview in constructor

    View itemView = layoutInflater.inflate(R.layout.camera_recyclerview_item_edit_camera, parent, false);


    return new ChooseViewHolder(itemView);

  }

  @Override
  public void onBindViewHolder(final @NonNull ChooseViewHolder holder,int i) {

    ImageView imgView = holder.itemView.findViewById(R.id.choose_image_edit_camera);
    ImageView checkmarkView = holder.itemView.findViewById(R.id.choose_image_checkmark);
    checkmarkView.setVisibility(View.INVISIBLE);
    String filePath;

    try {

      filePath = mFilenames[i];
      File imgFile = new File(imagesPath + filePath);

      Picasso.get().load(imgFile)
              .placeholder(R.drawable.ic_launcher)
              .into(imgView);

      if (filePath.equals(mCurrentSurveillanceCamera.getThumbnailPath())){
        checkmarkView.setVisibility(View.VISIBLE);
      }

    } catch (Exception e) {
      Log.i(TAG, e.toString());
    }

    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        String chosenImageFilePath = mFilenames[holder.getAdapterPosition()];
        File imgFile = new File(imagesPath + chosenImageFilePath);

        Picasso.get().load(imgFile)
                .placeholder(R.drawable.ic_launcher)
                .into(mChosenCameraImageView);

        mCurrentSurveillanceCamera.setThumbnailPath(chosenImageFilePath);

      }

    });

  }



  @Override
  public int getItemCount() {
    return mFilenames.length;
  }
}
