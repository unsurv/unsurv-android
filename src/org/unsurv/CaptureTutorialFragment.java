package org.unsurv;

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


/**
 * This Fragment is part of the tutorial the user sees when first launching the app.
 * It shows the different methods that Surveillance cameras can be captured with this app
 */
public class CaptureTutorialFragment extends Fragment {

  private SharedPreferences sharedPreferences;

  // private TutorialViewPager tutorialViewPager;



  public CaptureTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {


    View rootView = inflater.inflate(R.layout.tutorial_capture, container,false);

    // sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

    return rootView;

  }
}
