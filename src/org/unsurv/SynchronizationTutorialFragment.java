package org.unsurv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
  private LinearLayout userServerChoiceLayout;
  private long intervalSpinnerFactor;
  private long minDelaySpinnerFactor;
  private long maxDelaySpinnerFactor;
  private int intervalUserEntry;
  private int minDelayUserEntry;
  private int maxDelayUserEntry;
  private String minDelay;
  private String maxDelay;
  private String interval;
  private String serverUrl;

  private TutorialViewPager tutorialViewPager;

  private EditText userIntervalChoice;
  private EditText userMinDelayChoice;
  private EditText userMaxDelayChoice;
  private EditText userServerUrlChoice;

  private Spinner userDurationChoiceSpinner;
  private Spinner minDelaySpinner;
  private Spinner maxDelaySpinner;

  private Context context;

  private SynchronizedCameraRepository synchronizedCameraRepository;

  private LocalBroadcastManager localBroadcastManager;
  private IntentFilter intentFilter;
  private BroadcastReceiver br;


  public SynchronizationTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    final View rootView = inflater.inflate(R.layout.tutorial_synchronization, container,false);

    context = getContext();

    tutorialViewPager = getActivity().findViewById(R.id.tutorial_viewpager);

    localBroadcastManager = LocalBroadcastManager.getInstance(context);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

    offlineModeSwitch = rootView.findViewById(R.id.sync_tutorial_offline_switch);

    userServerUrlChoice = rootView.findViewById(R.id.sync_tutorial_user_server_url);

    intervalChoiceLayout = rootView.findViewById(R.id.sync_tutorial_interval_view);
    minDelayChoiceLayout = rootView.findViewById(R.id.sync_tutorial_min_delay_view);
    maxDelayChoiceLayout = rootView.findViewById(R.id.sync_tutorial_max_delay_view);
    userServerChoiceLayout = rootView.findViewById(R.id.sync_tutorial_server_view);

    userIntervalChoice = rootView.findViewById(R.id.sync_tutorial_user_interval);
    userMinDelayChoice = rootView.findViewById(R.id.sync_tutorial_user_min_delay);
    userMaxDelayChoice = rootView.findViewById(R.id.sync_tutorial_user_max_delay);

    userDurationChoiceSpinner = rootView.findViewById(R.id.sync_tutorial_interval_spinner);
    minDelaySpinner = rootView.findViewById(R.id.sync_tutorial_min_delay_spinner);
    maxDelaySpinner = rootView.findViewById(R.id.sync_tutorial_max_delay_spinner);

    sharedPreferences.edit().putBoolean("offlineMode", false).apply();

    offlineModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
          sharedPreferences.edit().putBoolean("offlineMode", true).apply();

          // half transparent + unclickable when offline mode is chosen
          intervalChoiceLayout.setAlpha(0.5f);
          minDelayChoiceLayout.setAlpha(0.5f);
          maxDelayChoiceLayout.setAlpha(0.5f);
          userServerChoiceLayout.setAlpha(0.5f);

          userIntervalChoice.setFocusable(false);
          userMinDelayChoice.setFocusable(false);
          userMaxDelayChoice.setFocusable(false);
          userServerUrlChoice.setFocusable(false);

          userIntervalChoice.setCursorVisible(false);
          userMinDelayChoice.setCursorVisible(false);
          userMaxDelayChoice.setCursorVisible(false);
          userServerUrlChoice.setCursorVisible(false);

          userDurationChoiceSpinner.setEnabled(false);
          minDelaySpinner.setEnabled(false);
          maxDelaySpinner.setEnabled(false);


        } else {
          sharedPreferences.edit().putBoolean("offlineMode", false).apply();

          intervalChoiceLayout.setAlpha(1);
          minDelayChoiceLayout.setAlpha(1);
          maxDelayChoiceLayout.setAlpha(1);
          userServerChoiceLayout.setAlpha(1);

          userIntervalChoice.setFocusableInTouchMode(true);
          userMinDelayChoice.setFocusableInTouchMode(true);
          userMaxDelayChoice.setFocusableInTouchMode(true);
          userServerUrlChoice.setFocusable(true);
          userIntervalChoice.setCursorVisible(true);
          userMinDelayChoice.setCursorVisible(true);
          userMaxDelayChoice.setCursorVisible(true);
          userServerUrlChoice.setCursorVisible(true);

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
        saveEntriesToSharedPreferences();
        sharedPreferences.edit().putBoolean("tutorialCompleted", true).apply();

        // disables clustering until a bug in library has been fixed
        sharedPreferences.edit().putBoolean("clusteringEnabled", false).apply();

        // check for offline mode and assume it's on
        if (!sharedPreferences.getBoolean("offlineMode", true)) {

          synchronizedCameraRepository = new SynchronizedCameraRepository(getActivity().getApplication());


          if (SynchronizationUtils.isApiKeyExpiredOrMissing(sharedPreferences)){

            br = new BroadcastReceiver() {
              @Override
              public void onReceive(Context context, Intent intent) {
                String baseURL = userServerUrlChoice.getText().toString();
                String homeArea = sharedPreferences.getString("area", null);


                SynchronizationUtils.downloadCamerasFromServer(
                        baseURL,
                        "area=" + homeArea,
                        sharedPreferences,
                        true,
                        null,
                        synchronizedCameraRepository,
                        context);
              }
            };

            intentFilter = new IntentFilter("org.unsurv.API_KEY_CHANGED");

            localBroadcastManager.registerReceiver(br, intentFilter);

            SynchronizationUtils.getAPIkey(context, sharedPreferences);

          } else {
            // api key is present and not expired
            String baseURL = sharedPreferences.getString("synchronizationUrl", null);
            String homeArea = sharedPreferences.getString("area", null);


            SynchronizationUtils.downloadCamerasFromServer(
                    baseURL,
                    "area=" + homeArea,
                    sharedPreferences,
                    true,
                    null,
                    synchronizedCameraRepository,
                    context);
          }

        } // TODO use local file included with app to populate db

        Intent tutorialFinishedIntent = new Intent(getActivity(), DetectorActivity.class);
        startActivity(tutorialFinishedIntent);


      }
    });


    return rootView;

  }

  private void saveEntriesToSharedPreferences(){

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

    serverUrl = userServerUrlChoice.getText().toString();
    sharedPreferences.edit().putString("synchronizationUrl", serverUrl).apply();
  }
}
