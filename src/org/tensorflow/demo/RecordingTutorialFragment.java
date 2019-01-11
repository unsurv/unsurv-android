package org.tensorflow.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class RecordingTutorialFragment extends android.support.v4.app.Fragment {

  public RecordingTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.recording_tutorial, container,false);

    TextView recordingTextView = rootView.findViewById(R.id.recording_tutorial_textview);
    recordingTextView.setText("Fragment Works");


    return rootView;

  }
}
