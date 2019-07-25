package org.tensorflow.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.widget.Toast;

import java.io.File;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat {

  Context context;

  private Preference clearSynchronizedImages;
  private Preference clearCapturedImages;
  private Preference clearTrainingImages;
  private Preference showLicences;

  // TODO grey out synchronitaion category if offline mode selected

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    final CameraRepository cameraRepository = new CameraRepository(getActivity().getApplication());
    final SynchronizedCameraRepository synchronizedCameraRepository = new SynchronizedCameraRepository(getActivity().getApplication());

    context = getContext();


    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preferences, rootKey);

    EditTextPreference asd = findPreference("synchronizationUrl");


    // clears storage for sychronized camera images
    clearSynchronizedImages = findPreference("clear_synchronized");

    // filesize in bytes for synchronized camera image diretory
    final long synchronizedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.SYNCHRONIZED_PATH));

    // examples 1,22 or 12,32
    final String synchronizedMbTwoDecimals = StorageUtils.convertByteSizeToMBTwoDecimals(synchronizedImagesSize);

    // set size as preference title for user to see
    clearSynchronizedImages.setTitle("Delete downloaded images: " + StorageUtils.convertByteSizeToMBTwoDecimals(synchronizedImagesSize) + " MB");

    clearSynchronizedImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        List<SynchronizedCamera> allSynchronizedCameras = synchronizedCameraRepository.getAllSynchronizedCameras(true);

        // update cameras
        for (SynchronizedCamera camera : allSynchronizedCameras){
          camera.setImagePath(null);
          synchronizedCameraRepository.update(camera);
        }

        displayPopUpBeforeDeleting("Do you want to delete all downloaded images?", synchronizedMbTwoDecimals , StorageUtils.SYNCHRONIZED_PATH, context);


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

        List<SurveillanceCamera> cameras = cameraRepository.getAllCameras();

        for (SurveillanceCamera camera : cameras){
          // camera is a "normal" capture
          if (!camera.getTrainingCapture()){
            camera.setThumbnailPath(null);
            camera.setImagePath(null);
            cameraRepository.updateCameras(camera);
          }
        }

        displayPopUpBeforeDeleting("Do you want to delete all captured images?", capturedMbTwoDecimals , StorageUtils.CAMERA_CAPTURES_PATH, context);

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
        List<SurveillanceCamera> cameras = cameraRepository.getAllCameras();


        for (SurveillanceCamera camera : cameras){
          // camera is a training capture
          if (camera.getTrainingCapture()){
            // training capture without an image is useless, delete
            cameraRepository.deleteCamera(camera);
          }
        }

        displayPopUpBeforeDeleting("Do you want to delete all training images?\nThis will remove all training data without uploading.", trainingMbTwoDecimals , StorageUtils.TRAINING_CAPTURES_PATH, context);

        return true;

      }
    });

    showLicences = findPreference("show_licences");

    showLicences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        Intent licencesIntent = new Intent(context, LicencesActivity.class);
        startActivity(licencesIntent);

        return true;
      }
    });


  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);




  }


  private void displayPopUpBeforeDeleting(String message, final String deleteSizeInBytes, final String pathToClear, final Context context){

    new AlertDialog.Builder(context)
            .setTitle("Are you sure?")
            .setMessage(message)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                StorageUtils.deleteAllFilesInDirectory(pathToClear);
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
  void refreshSizes(){

    long synchronizedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.SYNCHRONIZED_PATH));
    clearSynchronizedImages.setTitle("Delete downloaded images: " + StorageUtils.convertByteSizeToMBTwoDecimals(synchronizedImagesSize) + " MB");

    long capturedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.CAMERA_CAPTURES_PATH));
    clearCapturedImages.setTitle("Delete capture images: " + StorageUtils.convertByteSizeToMBTwoDecimals(capturedImagesSize) + " MB");

    long trainingImagesSize = StorageUtils.getFileSize(new File(StorageUtils.TRAINING_CAPTURES_PATH));
    clearTrainingImages.setTitle("Delete training images: " + StorageUtils.convertByteSizeToMBTwoDecimals(trainingImagesSize) + " MB");

  }

}
