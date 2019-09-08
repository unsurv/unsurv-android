package org.unsurv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.fragment.app.Fragment;

/**
 * This Fragment is part of the tutorial the user sees when first launching the app.
 * It shows how and when the app synchronizes with the database server. The user can choose
 * different intervals or no interval depending on their preference.
 */
public class SynchronizationTutorialFragment extends Fragment {

  private SharedPreferences sharedPreferences;

  private Switch offlineModeSwitch;
  private LinearLayout intervalChoiceLayout;
  private LinearLayout minDelayChoiceLayout;
  private LinearLayout maxDelayChoiceLayout;
  private long intervalSpinnerFactor;
  private long minDelaySpinnerFactor;
  private long maxDelaySpinnerFactor;
  private int intervalUserEntry;
  private int minDelayUserEntry;
  private int maxDelayUserEntry;
  private String minDelay;
  private String maxDelay;
  private String interval;

  private TutorialViewPager tutorialViewPager;

  private EditText userIntervalChoice;
  private EditText userMinDelayChoice;
  private EditText userMaxDelayChoice;

  private Spinner userDurationChoiceSpinner;
  private Spinner minDelaySpinner;
  private Spinner maxDelaySpinner;


  public SynchronizationTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    final View rootView = inflater.inflate(R.layout.tutorial_synchronization, container,false);

    tutorialViewPager = getActivity().findViewById(R.id.tutorial_viewpager);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

    offlineModeSwitch = rootView.findViewById(R.id.sync_tutorial_offline_switch);

    intervalChoiceLayout = rootView.findViewById(R.id.sync_tutorial_interval_view);
    minDelayChoiceLayout = rootView.findViewById(R.id.sync_tutorial_min_delay_view);
    maxDelayChoiceLayout = rootView.findViewById(R.id.sync_tutorial_max_delay_view);

    userIntervalChoice = rootView.findViewById(R.id.sync_tutorial_user_interval);
    userMinDelayChoice = rootView.findViewById(R.id.sync_tutorial_user_min_delay);
    userMaxDelayChoice = rootView.findViewById(R.id.sync_tutorial_user_max_delay);

    userDurationChoiceSpinner = rootView.findViewById(R.id.sync_tutorial_interval_spinner);
    minDelaySpinner = rootView.findViewById(R.id.sync_tutorial_min_delay_spinner);
    maxDelaySpinner = rootView.findViewById(R.id.sync_tutorial_max_delay_spinner);


    offlineModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
          sharedPreferences.edit().putBoolean("offlineMode", true).apply();

          // half transparent + unclickable when offline mode is chosen
          intervalChoiceLayout.setAlpha(0.5f);
          minDelayChoiceLayout.setAlpha(0.5f);
          maxDelayChoiceLayout.setAlpha(0.5f);

          userIntervalChoice.setFocusable(false);
          userMinDelayChoice.setFocusable(false);
          userMaxDelayChoice.setFocusable(false);
          userIntervalChoice.setCursorVisible(false);
          userMinDelayChoice.setCursorVisible(false);
          userMaxDelayChoice.setCursorVisible(false);

          userDurationChoiceSpinner.setEnabled(false);
          minDelaySpinner.setEnabled(false);
          maxDelaySpinner.setEnabled(false);


        } else {
          sharedPreferences.edit().putBoolean("offlineMode", false).apply();

          intervalChoiceLayout.setAlpha(1);
          minDelayChoiceLayout.setAlpha(1);
          maxDelayChoiceLayout.setAlpha(1);

          userIntervalChoice.setFocusableInTouchMode(true);
          userMinDelayChoice.setFocusableInTouchMode(true);
          userMaxDelayChoice.setFocusableInTouchMode(true);
          userIntervalChoice.setCursorVisible(true);
          userMinDelayChoice.setCursorVisible(true);
          userMaxDelayChoice.setCursorVisible(true);

          userDurationChoiceSpinner.setEnabled(true);
          minDelaySpinner.setEnabled(true);
          maxDelaySpinner.setEnabled(true);

        }
      }
    });




    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getContext(),
            R.array.sync_tutorial_interval, android.R.layout.simple_spinner_item);
    // Specify the layout to use when the list of choices appears
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    // Apply the adapter to the spinners
    userDurationChoiceSpinner.setAdapter(adapter);
    minDelaySpinner.setAdapter(adapter);
    maxDelaySpinner.setAdapter(adapter);

    userDurationChoiceSpinner.setSelection(1); // days
    minDelaySpinner.setSelection(0); // hours
    maxDelaySpinner.setSelection(1); // days


    userDurationChoiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        switch (i) {
          case 0:
            // hour[s]
            intervalSpinnerFactor = 1000*60*60;
            intervalUserEntry = Integer.valueOf(userIntervalChoice.getText().toString());
            interval = String.valueOf(intervalUserEntry * intervalSpinnerFactor);

            // synchronizationInterval gets multiplied by 1000 before being used
            // sharedPreferences doesnt like Long types
            sharedPreferences.edit().putString("synchronizationInterval", interval).apply();
            break;

          case 1:
            // day[s]
            intervalSpinnerFactor = 1000*60*60*24;
            intervalUserEntry = Integer.valueOf(userIntervalChoice.getText().toString());
            interval = String.valueOf(intervalUserEntry * intervalSpinnerFactor);

            sharedPreferences.edit().putString("synchronizationInterval", interval).apply();
            break;

          case 2:
            // week[s]
            intervalSpinnerFactor = 1000*60*60*24*7;
            intervalUserEntry = Integer.valueOf(userIntervalChoice.getText().toString());
            interval = String.valueOf(intervalUserEntry * intervalSpinnerFactor);

            sharedPreferences.edit().putString("synchronizationInterval", interval).apply();
            break;
        }

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });


    minDelaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        switch (i) {
          case 0:
            // hour[s]
            minDelaySpinnerFactor = 1000*60*60;
            minDelayUserEntry = Integer.valueOf(userMinDelayChoice.getText().toString());
            minDelay = String.valueOf(minDelayUserEntry * minDelaySpinnerFactor);

            // synchronizationInterval gets multiplied by 1000 before being used
            // sharedPreferences doesnt like Long types
            sharedPreferences.edit().putString("minUploadDelay", minDelay).apply();
            break;

          case 1:
            // day[s]
            minDelaySpinnerFactor = 1000*60*60*24;
            minDelayUserEntry = Integer.valueOf(userMinDelayChoice.getText().toString());
            minDelay = String.valueOf(minDelayUserEntry * minDelaySpinnerFactor);

            sharedPreferences.edit().putString("minUploadDelay", minDelay).apply();
            break;

          case 2:
            // week[s]
            minDelaySpinnerFactor = 1000*60*60*24*7;
            minDelayUserEntry = Integer.valueOf(userMinDelayChoice.getText().toString());
            minDelay = String.valueOf(minDelayUserEntry * minDelaySpinnerFactor);

            sharedPreferences.edit().putString("minUploadDelay", minDelay).apply();
            break;
        }

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });


    maxDelaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        switch (i) {
          case 0:
            // hour[s]
            maxDelaySpinnerFactor = 1000*60*60;
            maxDelayUserEntry = Integer.valueOf(userMaxDelayChoice.getText().toString());
            maxDelay = String.valueOf(maxDelayUserEntry * maxDelaySpinnerFactor);

            // synchronizationInterval gets multiplied by 1000 before being used
            // sharedPreferences doesnt like Long types
            sharedPreferences.edit().putString("maxUploadDelay", maxDelay).apply();
            break;

          case 1:
            // day[s]
            maxDelaySpinnerFactor = 1000*60*60*24;
            maxDelayUserEntry = Integer.valueOf(userMaxDelayChoice.getText().toString());
            maxDelay = String.valueOf(maxDelayUserEntry * maxDelaySpinnerFactor);

            sharedPreferences.edit().putString("maxUploadDelay", maxDelay).apply();
            break;

          case 2:
            // week[s]
            maxDelaySpinnerFactor = 1000*60*60*24*7;
            maxDelayUserEntry = Integer.valueOf(userMaxDelayChoice.getText().toString());
            maxDelay = String.valueOf(maxDelayUserEntry * maxDelaySpinnerFactor);

            sharedPreferences.edit().putString("maxUploadDelay", maxDelay).apply();
            break;
        }

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });


    Button saveSynchronizationSettings = rootView.findViewById(R.id.sync_tutorial_save_button);

    saveSynchronizationSettings.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        saveToSharedPreferences();
      }
    });


    return rootView;

  }

  private void saveToSharedPreferences(){

    sharedPreferences.edit().putBoolean("offlineMode", offlineModeSwitch.isChecked()).apply();

    intervalUserEntry = Integer.valueOf(userIntervalChoice.getText().toString());
    interval = String.valueOf(intervalUserEntry * intervalSpinnerFactor);
    sharedPreferences.edit().putString("synchronizationInterval", interval).apply();

    minDelayUserEntry = Integer.valueOf(userMinDelayChoice.getText().toString());
    minDelay = String.valueOf(minDelayUserEntry * minDelaySpinnerFactor);
    sharedPreferences.edit().putString("minUploadDelay", minDelay).apply();

    maxDelayUserEntry = Integer.valueOf(userMaxDelayChoice.getText().toString());
    maxDelay = String.valueOf(maxDelayUserEntry * maxDelaySpinnerFactor);
    sharedPreferences.edit().putString("maxUploadDelay", maxDelay).apply();
  }
}
