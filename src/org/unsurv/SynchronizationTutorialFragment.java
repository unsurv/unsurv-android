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
  private LinearLayout intervalChoice;
  private long spinnerFactor;
  private int userEntry;
  private long intervalDuration;
  private String interval;

  public SynchronizationTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    final View rootView = inflater.inflate(R.layout.synchronization_tutorial, container,false);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

    Switch offlineModeSwitch = rootView.findViewById(R.id.sync_tutorial_offline_switch);

    intervalChoice = rootView.findViewById(R.id.sync_tutorial_interval_view);

    offlineModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
          sharedPreferences.edit().putBoolean("offlineMode", true).apply();

          // half transparent + unclickable when offline mode is chosen
          intervalChoice.setAlpha(0.5f);
          intervalChoice.setClickable(false);


        } else {
          sharedPreferences.edit().putBoolean("offlineMode", false).apply();
          intervalChoice.setAlpha(1);
          intervalChoice.setClickable(true);

        }
      }
    });


    EditText userIntervalChoice = rootView.findViewById(R.id.sync_tutorial_user_interval);

    userEntry = Integer.valueOf(userIntervalChoice.getText().toString());

    Spinner userDurationChoice = rootView.findViewById(R.id.sync_tutorial_spinner);

    spinnerFactor = 1000*60*60*24; // ms to day conversion factor as default

    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getContext(),
            R.array.sync_tutorial_interval, android.R.layout.simple_spinner_item);
    // Specify the layout to use when the list of choices appears
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    // Apply the adapter to the spinners
    userDurationChoice.setAdapter(adapter);


    userDurationChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        switch (i) {
          case 0:
            // hour[s]
            spinnerFactor = 1000*60*60;
            intervalDuration = userEntry * spinnerFactor;

            interval = String.valueOf(intervalDuration);
            sharedPreferences.edit().putString("synchronizationInterval", interval).apply();
            break;

          case 1:
            // day[s]
            spinnerFactor = 1000*60*60*24;
            intervalDuration = userEntry * spinnerFactor;

            interval = String.valueOf(intervalDuration);
            sharedPreferences.edit().putString("synchronizationInterval", interval).apply();
            break;

          case 2:
            // week[s]
            spinnerFactor = 1000*60*60*24*7;
            intervalDuration = userEntry * spinnerFactor;

            interval = String.valueOf(intervalDuration);
            sharedPreferences.edit().putString("synchronizationInterval", interval).apply();
            break;
        }

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });




    return rootView;

  }
}
