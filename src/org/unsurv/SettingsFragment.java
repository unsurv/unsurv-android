package org.unsurv;

import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * SettingsFragment containing the actual PreferenceObjects
 */
public class SettingsFragment extends PreferenceFragmentCompat {

  private static String TAG = "SettingsFragment";

  private Context ctx;

  private Preference clearSynchronizedImages;
  private Preference clearCapturedImages;
  private Preference clearTrainingImages;
  private Preference exportCapturesData;
  private Preference exportTrainingData;
  private Preference databaseSize;

  private SharedPreferences sharedPreferences;

  private final static int DELETE_SYNCHRONIZED_CAMERAS = 0;
  private final static int DELETE_SURVEILLANCE_CAMERAS = 1;
  private final static int DELETE_TRAINING_CAMERAS = 2;

  private List<SynchronizedCamera> synchronizedCameras;
  private List<SurveillanceCamera> surveillanceCameras;

  private CameraRepository cameraRepository;
  private SynchronizedCameraRepository synchronizedCameraRepository;

  private BroadcastReceiver apiKeyBroadcastListener;
  private BroadcastReceiver synchronizationBroadcastListener;
  private IntentFilter intentFilter;
  private IntentFilter synchronizationIntentFilter;
  private LocalBroadcastManager localBroadcastManager;

  // TODO grey out synchronitaion category if offline mode selected

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Preference showLicences;
    Preference synchronizeNow;
    Preference startSynchronizing;
    Preference stopSynchronizing;
    ListPreference synchronizationInterval;
    Preference restartTutorial;

    cameraRepository = new CameraRepository(getActivity().getApplication());
    synchronizedCameraRepository = new SynchronizedCameraRepository(getActivity().getApplication());

    ctx = getContext();
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);

    localBroadcastManager = LocalBroadcastManager.getInstance(ctx);

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preferences, rootKey);

    // clears storage for sychronized camera images
    clearSynchronizedImages = findPreference("clear_synchronized");

    // filesize in bytes for synchronized camera image diretory
    final long synchronizedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.SYNCHRONIZED_PATH));

    // examples 1,22 or 12,32
    final String synchronizedMbTwoDecimals = StorageUtils.convertByteSizeToMBTwoDecimals(synchronizedImagesSize);

    // set size as preference title for user to see
    clearSynchronizedImages.setTitle("Delete downloaded images: " + StorageUtils.convertByteSizeToMBTwoDecimals(synchronizedImagesSize) + " MB");

    // delete files but not db entries of chosen type
    // db entries are still useful without an image
    clearSynchronizedImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        synchronizedCameras = synchronizedCameraRepository.getAllSynchronizedCameras(true);

        displayPopUpBeforeDeleting("Do you want to delete all downloaded images?", synchronizedMbTwoDecimals , DELETE_SYNCHRONIZED_CAMERAS, ctx);


        return true;
      }
    });

    // same procedure as clearSynchronizedImages

    clearCapturedImages = findPreference("clear_captures");

    final long capturedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.CAMERA_CAPTURES_PATH));

    final String capturedMbTwoDecimals = StorageUtils.convertByteSizeToMBTwoDecimals(capturedImagesSize);

    clearCapturedImages.setTitle("Delete capture images: " + StorageUtils.convertByteSizeToMBTwoDecimals(capturedImagesSize) + " MB");


    clearCapturedImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        surveillanceCameras = cameraRepository.getAllCameras();

        displayPopUpBeforeDeleting("Do you want to delete all captured images?", capturedMbTwoDecimals , DELETE_SURVEILLANCE_CAMERAS, ctx);

        return true;

      }
    });

    // same procedure as clearSynchronizedImages, except training captures without an image are useless, so they get deleted

    clearTrainingImages = findPreference("clear_training");

    final long trainingImagesSize = StorageUtils.getFileSize(new File(StorageUtils.TRAINING_CAPTURES_PATH));

    final String trainingMbTwoDecimals = StorageUtils.convertByteSizeToMBTwoDecimals(trainingImagesSize);
    clearTrainingImages.setTitle("Delete training images: " + trainingMbTwoDecimals + " MB");

    clearTrainingImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
         surveillanceCameras = cameraRepository.getAllCameras();

        displayPopUpBeforeDeleting("Do you want to delete all training images?\nThis will remove all training data without uploading.", trainingMbTwoDecimals , DELETE_TRAINING_CAMERAS, ctx);

        return true;

      }
    });


    exportCapturesData = findPreference("export_data");

    exportCapturesData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        List<SurveillanceCamera> camerasToExport = cameraRepository.getAllCameras();

        List<SurveillanceCamera> filteredCameras = new ArrayList<>();

        for (SurveillanceCamera camera: camerasToExport) {
          if (!camera.getTrainingCapture()){
            filteredCameras.add(camera);
          }
        }

        // ~~~~~~~~~~~~~ pasta time

        if (StorageUtils.exportCaptures(filteredCameras)){
          if(StorageUtils.exportImages(filteredCameras, true)) {
            Toast.makeText(ctx, "Successfully exported data to /Pictures/unsurv/export/captures/", Toast.LENGTH_LONG).show();
          }
        }

        return true;
      }
    });


    exportTrainingData = findPreference("export_data_training");

    exportTrainingData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        List<SurveillanceCamera> camerasToExport = cameraRepository.getAllCameras();

        List<SurveillanceCamera> filteredCameras = new ArrayList<>();

        for (SurveillanceCamera camera: camerasToExport) {
          if (camera.getTrainingCapture()){
            filteredCameras.add(camera);
          }
        }

        if (StorageUtils.exportTraining(filteredCameras)){
          if(StorageUtils.exportImages(filteredCameras, false)) {
            Toast.makeText(ctx, "Successfully exported data to /Pictures/unsurv/export/training/", Toast.LENGTH_LONG).show();
          }
        }

        return true;
      }
    });

    // starts an activity that displays all the licences of used libraries / codebase
    showLicences = findPreference("showLicences");

    if (showLicences != null) {
      showLicences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

          Intent licencesIntent = new Intent(ctx, LicencesActivity.class);
          startActivity(licencesIntent);

          return true;
        }
      });
    }

    // displays current db size

    databaseSize = findPreference("databaseSize");
    int dbSize = synchronizedCameraRepository.getTotalAmountInDb();

    databaseSize.setTitle(getResources().getQuantityString(R.plurals.cameras_in_database, dbSize, dbSize));

    // listen for synchronization events and update title when a synchronization has taken place
    synchronizationBroadcastListener = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        refreshDbSize();
      }
    };

    synchronizationIntentFilter = new IntentFilter("org.unsurv.SYNCHRONIZATION_SUCCESSFUL");

    localBroadcastManager.registerReceiver(synchronizationBroadcastListener, synchronizationIntentFilter);

    // TODO rename and rework to rebuild database
    // starts a manual synchronization
    synchronizeNow = findPreference("synchronizeNow");

    if (synchronizeNow != null) {
      synchronizeNow.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

          final String baseURL = sharedPreferences.getString("synchronizationUrl", null);
          final String homeArea = sharedPreferences.getString("area", null);

          // If api key is expired set up a LocalBroadCastReceiver to start the synchronization
          // as soon as a new api key has been acquired. Then start getting a new API key.

          SynchronizationUtils.downloadCamerasFromServer(
                  baseURL,
                  homeArea,
                  true,
                  synchronizedCameraRepository,
                  sharedPreferences,
                  ctx);



          return true;
        }
      });
    }


    startSynchronizing = findPreference("startSynchronizing");

    startSynchronizing.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        try {

          JobScheduler jobScheduler = getActivity().getApplicationContext().getSystemService(JobScheduler.class);

          JobInfo syncJob = jobScheduler.getPendingJob(0);

          if (syncJob == null){
            SynchronizationUtils.scheduleSyncIntervalJob(getActivity().getApplicationContext(), null, 0);
            Toast.makeText(ctx, "Successfully started recurring synchronization.", Toast.LENGTH_SHORT).show();

          }

        } catch (NullPointerException npe){
          Log.i(TAG, "npe in startSynchronizing " + npe.toString());
        }

        return true;
      }
    });


    stopSynchronizing = findPreference("stopSynchronizing");

    stopSynchronizing.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        try {

          JobScheduler jobScheduler = getActivity().getApplicationContext().getSystemService(JobScheduler.class);

          JobInfo syncJob = jobScheduler.getPendingJob(0);

          if (syncJob != null){
            jobScheduler.cancel(syncJob.getId());
            Toast.makeText(ctx, "Successfully stopped recurring synchronization.", Toast.LENGTH_SHORT).show();

          }

        } catch (NullPointerException npe){
          Log.i(TAG, "npe in stopSynchronizing " + npe.toString());
        }

        return true;
      }
    });


    synchronizationInterval = findPreference("synchronizationInterval");
    synchronizationInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, final Object newValue) {

        JobScheduler jobScheduler = getActivity().getApplicationContext().getSystemService(JobScheduler.class);

        JobInfo syncJob = jobScheduler.getPendingJob(0);

        if (syncJob != null){
          jobScheduler.cancel(syncJob.getId());
          SynchronizationUtils.scheduleSyncIntervalJob(getActivity().getApplicationContext(), null, Long.parseLong((String) newValue));

          Toast.makeText(ctx, "Successfully restarted recurring synchronization with new interval.", Toast.LENGTH_LONG).show();

        } else {

          new AlertDialog.Builder(ctx)
                  .setTitle("No synchronization job found")
                  .setMessage("Do you want to start a recurring synchronization job with the chosen interval now?")
                  .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                      SynchronizationUtils.scheduleSyncIntervalJob(getActivity().getApplicationContext(), null, Long.parseLong((String) newValue));
                      Toast.makeText(ctx, "Successfully started recurring synchronization with new interval.", Toast.LENGTH_LONG).show();
                    }
                  })
                  .setNegativeButton("No", null)
                  .show();

        }

        return true;
      }
    });


    restartTutorial = findPreference("restartTutorial");

    restartTutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {


        Intent tutorialIntent = new Intent(ctx, TutorialActivity.class);
        startActivity(tutorialIntent);


        return true;
      }
    });


  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

  }


  private void displayPopUpBeforeDeleting(String message, final String deleteSizeInBytes, final int mode, final Context context){

    new AlertDialog.Builder(context)
            .setTitle("Are you sure?")
            .setMessage(message)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                // StorageUtils.deleteAllFilesInDirectory(pathToClear);

                switch (mode){

                  case (DELETE_SYNCHRONIZED_CAMERAS):

                    // update cameras
                    for (SynchronizedCamera camera : synchronizedCameras){
                      StorageUtils.deleteImagesForSynchronizedCamera(camera);
                      camera.setImagePath(null);
                      synchronizedCameraRepository.update(camera);
                    }
                    break;


                  case (DELETE_SURVEILLANCE_CAMERAS):

                    for (SurveillanceCamera camera : surveillanceCameras){
                      // camera is a "normal" capture
                      // regular captures without images are still useful for position data
                      // user can always delete captures completely in HistoryActivity
                      if (!camera.getTrainingCapture()){

                        StorageUtils.deleteImagesForCamera(camera);
                        camera.setThumbnailPath(null);
                        camera.setImagePath(null);
                        camera.setManualCapture(true);
                        cameraRepository.updateCameras(camera);
                      }
                    }
                    break;


                  case (DELETE_TRAINING_CAMERAS):

                    for (SurveillanceCamera camera : surveillanceCameras){
                      // camera is a training capture
                      if (camera.getTrainingCapture()){
                        // training capture without an image is useless, therefore delete
                        cameraRepository.deleteCamera(camera);
                      }
                    }

                    break;
                }

                Toast.makeText(context, "Freed up " + deleteSizeInBytes + " MB of storage.", Toast.LENGTH_SHORT).show();
                refreshStorageSizes();

              }
            })
            .setNegativeButton("No", null)
            .show();

  }


  /**
   * refreshes sizes displayed to user
   */
  private void refreshStorageSizes(){

    long synchronizedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.SYNCHRONIZED_PATH));
    clearSynchronizedImages.setTitle("Delete downloaded images: " + StorageUtils.convertByteSizeToMBTwoDecimals(synchronizedImagesSize) + " MB");

    long capturedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.CAMERA_CAPTURES_PATH));
    clearCapturedImages.setTitle("Delete capture images: " + StorageUtils.convertByteSizeToMBTwoDecimals(capturedImagesSize) + " MB");

    long trainingImagesSize = StorageUtils.getFileSize(new File(StorageUtils.TRAINING_CAPTURES_PATH));
    clearTrainingImages.setTitle("Delete training images: " + StorageUtils.convertByteSizeToMBTwoDecimals(trainingImagesSize) + " MB");

  }

  private void refreshDbSize(){

    databaseSize = findPreference("databaseSize");
    int dbSize = synchronizedCameraRepository.getTotalAmountInDb();

    databaseSize.setTitle(getResources().getQuantityString(R.plurals.cameras_in_database, dbSize, dbSize));
    Toast.makeText(ctx, "Successfully synchronized " + dbSize + " cameras." , Toast.LENGTH_LONG).show();
  }

}
