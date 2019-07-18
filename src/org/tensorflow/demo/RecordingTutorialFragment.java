package org.tensorflow.demo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.fragment.app.Fragment;

public class RecordingTutorialFragment extends Fragment {

  private SharedPreferences sharedPreferences;

  private TutorialViewPager tutorialViewPager;

  private CheckBox automaticCaptureCheckbox;
  private CheckBox buttonCaptureCheckbox;

  public RecordingTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {


    View rootView = inflater.inflate(R.layout.recording_tutorial, container,false);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

    // standard choice is automatic capture
    sharedPreferences.edit().putBoolean("buttonCapture", false).apply();

    automaticCaptureCheckbox = rootView.findViewById(R.id.recording_tutorial_automatic_check);
    buttonCaptureCheckbox = rootView.findViewById(R.id.recording_tutorial_button_check);

    automaticCaptureCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (b) {
          sharedPreferences.edit().putBoolean("buttonCapture", false).apply();
          buttonCaptureCheckbox.setChecked(false);
        }
      }
    });

    buttonCaptureCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (b) {
          sharedPreferences.edit().putBoolean("buttonCapture", true).apply();
          automaticCaptureCheckbox.setChecked(false);
        }
      }
    });


    return rootView;

  }
}
