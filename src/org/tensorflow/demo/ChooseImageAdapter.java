package org.tensorflow.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

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
  private static String picturesPath = SynchronizationUtils.PICTURES_PATH;


  ChooseImageAdapter(Context context, String[] filenames){
    ctx = context;
    layoutInflater = LayoutInflater.from(ctx);
    this.filenames = filenames;

  }

  private Context ctx;
  private String[] filenames;
  private LayoutInflater layoutInflater;



  @Override
  public ChooseImageAdapter.ChooseViewHolder onCreateViewHolder(ViewGroup parent,
                                                                int viewType) {
    // TODO display only one checkmark, need to pass big imageview in constructor

    View itemView = layoutInflater.inflate(R.layout.camera_listview_item_edit_camera, parent, false);


    return new ChooseViewHolder(itemView);

  }

  @Override
  public void onBindViewHolder(@NonNull ChooseViewHolder holder, int i) {

    ImageView imgView = holder.itemView.findViewById(R.id.choose_image_edit_camera);

    String filePath;

    try {

      filePath = filenames[i];
      File imgFile = new File(picturesPath + filePath);

      Picasso.get().load(imgFile)
              .placeholder(R.drawable.ic_launcher)
              .into(imgView);
    } catch (Exception e) {
      Log.i(TAG, e.toString());
    }

  }



  @Override
  public int getItemCount() {
    return filenames.length;
  }
}
