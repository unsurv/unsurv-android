package org.tensorflow.demo;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class SurveillanceCameraAdapter extends ArrayAdapter<SurveillanceCamera> {


  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    View listItemView = convertView;

    if (listItemView == null) {
      listItemView = LayoutInflater.from(getContext()).inflate(
              R.layout.surveillance_camera_list_item, parent, false);
    }

    SurveillanceCamera currentCamera = getItem(position);

    ImageView thumbnailImageView = (ImageView) listItemView.findViewById(R.id.thumbnail_image);
    TextView latitudeTextView = (TextView) listItemView.findViewById(R.id.latitude_text_view);
    TextView longitudeTextView = (TextView) listItemView.findViewById(R.id.longitude_text_view);

    Picasso.get().load(currentCamera.getmImagePath()).into(thumbnailImageView);

    String longitude = String.valueOf(currentCamera.getmLocation().getLongitude());
    String latitude = String.valueOf(currentCamera.getmLocation().getLatitude());

    longitudeTextView.setText(longitude);
    latitudeTextView.setText(latitude);

    return listItemView;
  }

  public SurveillanceCameraAdapter(Activity context, ArrayList<SurveillanceCamera> cameras){
    super(context, 0, cameras);


  }

}
