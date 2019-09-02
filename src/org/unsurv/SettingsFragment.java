package org.unsurv;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.util.List;


/**
 * SettingsFragment containing the actual PreferenceObjects
 */
public class SettingsFragment extends PreferenceFragmentCompat {

  Context context;

  private Preference clearSynchronizedImages;
  private Preference clearCapturedImages;
  private Preference clearTrainingImages;

  private SharedPreferences sharedPreferences;

  private final static int DELETE_SYNCHRONIZED_CAMERAS = 0;
  private final static int DELETE_SURVEILLANCE_CAMERAS = 1;
  private final static int DELETE_TRAINING_CAMERAS = 2;

  private List<SynchronizedCamera> synchronizedCameras;
  private List<SurveillanceCamera> surveillanceCameras;

  private CameraRepository cameraRepository;
  private SynchronizedCameraRepository synchronizedCameraRepository;

  private BroadcastReceiver br;
  private IntentFilter intentFilter;
  private LocalBroadcastManager localBroadcastManager;

  // TODO grey out synchronitaion category if offline mode selected

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Preference showLicences;
    Preference synchronizeNow;

    cameraRepository = new CameraRepository(getActivity().getApplication());
    synchronizedCameraRepository = new SynchronizedCameraRepository(getActivity().getApplication());

    context = getContext();
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    localBroadcastManager = LocalBroadcastManager.getInstance(context);


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

        displayPopUpBeforeDeleting("Do you want to delete all downloaded images?", synchronizedMbTwoDecimals , DELETE_SYNCHRONIZED_CAMERAS, context);


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

        displayPopUpBeforeDeleting("Do you want to delete all captured images?", capturedMbTwoDecimals , DELETE_SURVEILLANCE_CAMERAS, context);

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

        displayPopUpBeforeDeleting("Do you want to delete all training images?\nThis will remove all training data without uploading.", trainingMbTwoDecimals , DELETE_TRAINING_CAMERAS, context);

        return true;

      }
    });

    // starts an activity that displays all the licences of used libraries / codebase
    showLicences = findPreference("showLicences");

    if (showLicences != null) {
      showLicences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

          Intent licencesIntent = new Intent(context, LicencesActivity.class);
          startActivity(licencesIntent);

          return true;
        }
      });
    }


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

          if (SynchronizationUtils.isApiKeyExpired(sharedPreferences, context)) {

            br = new BroadcastReceiver() {
              @Override
              public void onReceive(Context context, Intent intent) {
                SynchronizationUtils.downloadCamerasFromServer(
                        baseURL,
                        "area=" + homeArea,
                        sharedPreferences,
                        true,
                        null,
                        synchronizedCameraRepository);

                localBroadcastManager.unregisterReceiver(br);
              }
            };

            intentFilter = new IntentFilter("org.unsurv.API_KEY_CHANGED");

            localBroadcastManager.registerReceiver(br, intentFilter);

            SynchronizationUtils.refreshApiKeyIfExpired(sharedPreferences, context);


          } else {


            SynchronizationUtils.downloadCamerasFromServer(
                    baseURL,
                    "area=" + homeArea,
                    sharedPreferences,
                    true,
                    null,
                    synchronizedCameraRepository);
          }

          return true;
        }
      });
    }



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


                  case(DELETE_SURVEILLANCE_CAMERAS):

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


                  case(DELETE_TRAINING_CAMERAS):

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
                refreshSizes();

              }
            })
            .setNegativeButton("No", null)
            .show();

  }


  /**
   * refreshes sizes displayed to user
   */
  private void refreshSizes(){

    long synchronizedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.SYNCHRONIZED_PATH));
    clearSynchronizedImages.setTitle("Delete downloaded images: " + StorageUtils.convertByteSizeToMBTwoDecimals(synchronizedImagesSize) + " MB");

    long capturedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.CAMERA_CAPTURES_PATH));
    clearCapturedImages.setTitle("Delete capture images: " + StorageUtils.convertByteSizeToMBTwoDecimals(capturedImagesSize) + " MB");

    long trainingImagesSize = StorageUtils.getFileSize(new File(StorageUtils.TRAINING_CAPTURES_PATH));
    clearTrainingImages.setTitle("Delete training images: " + StorageUtils.convertByteSizeToMBTwoDecimals(trainingImagesSize) + " MB");

  }

}
