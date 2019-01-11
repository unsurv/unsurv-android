
package org.tensorflow.demo;

        import android.os.Bundle;
        import android.support.annotation.Nullable;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.TextView;

public class MapTutorialFragment extends android.support.v4.app.Fragment {

  public MapTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.map_tutorial, container,false);

    TextView recordingTextView = rootView.findViewById(R.id.map_tutorial_textview);
    recordingTextView.setText("Fragment Works");


    return rootView;

  }
}
