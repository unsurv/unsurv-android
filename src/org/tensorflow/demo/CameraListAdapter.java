package org.tensorflow.demo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;


public class CameraListAdapter extends RecyclerView.Adapter<CameraListAdapter.CameraViewHolder> {

  class CameraViewHolder extends RecyclerView.ViewHolder {
    private final ImageView thumbnailImageView;
    private final TextView latitudeTextView;
    private final TextView longitudeTextView;

    private CameraViewHolder(View itemView) {
      super(itemView);
      thumbnailImageView = (ImageView) itemView.findViewById(R.id.thumbnail_image);
      latitudeTextView = (TextView) itemView.findViewById(R.id.latitude_text_view);
      longitudeTextView = (TextView) itemView.findViewById(R.id.longitude_text_view);
    }

  }

  private final LayoutInflater mInflater;
  private List<SurveillanceCamera> mSurveillanceCameras;

  CameraListAdapter(Context context) {mInflater = LayoutInflater.from(context); }

  @Override
  public CameraViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = mInflater.inflate(R.layout.camera_recyclerview_item, parent, false);
    return new CameraViewHolder(itemView);
  }


  @Override
  public void onBindViewHolder(CameraViewHolder holder, int position) {



    if (mSurveillanceCameras != null) {
      SurveillanceCamera current = mSurveillanceCameras.get(position);

      String mLatitude = String.valueOf(current.getLatitude());
      String mLongitude = String.valueOf(current.getLongitude());
      File mThumbnailPicture = new File(current.getThumbnailPath());

      String mComment = current.getComment();

      // holder.thumbnailImageView.
      holder.latitudeTextView.setText(mLatitude);
      holder.longitudeTextView.setText(mLongitude);
      Picasso.get().load(mThumbnailPicture)
              .placeholder(R.drawable.ic_launcher)
              .into(holder.thumbnailImageView);
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
