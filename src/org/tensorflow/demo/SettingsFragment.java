package org.tensorflow.demo;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends PreferenceFragment {

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final CameraRepository cameraRepository = new CameraRepository(getActivity().getApplication());
    final SynchronizedCameraRepository synchronizedCameraRepository = new SynchronizedCameraRepository(getActivity().getApplication());


    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);

    // clears storage for sychronized camera images
    final Preference clearSynchronizedImages = findPreference("clear_synchronized");

    // filesize in bytes for synchronized camera image diretory
    final long synchronizedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.SYNCHRONIZED_PATH));

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

        StorageUtils.deleteAllFilesInDirectory(StorageUtils.SYNCHRONIZED_PATH);

        long newDirSize = StorageUtils.getFileSize(new File(StorageUtils.SYNCHRONIZED_PATH));

        long diff = synchronizedImagesSize - newDirSize;

        Toast.makeText(getContext(), "Freed up " + StorageUtils.convertByteSizeToMBTwoDecimals(diff) + " MB of storage.", Toast.LENGTH_SHORT).show();

        clearSynchronizedImages.setTitle("Delete downloaded images: " + StorageUtils.convertByteSizeToMBTwoDecimals(newDirSize) + " MB");

        return true;
      }
    });

    // same procedure as clearSynchronizedImages

    final Preference clearCapturedImages = findPreference("clear_captures");

    final long capturedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.CAPTURES_PATH));

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

        StorageUtils.deleteAllFilesInDirectory(StorageUtils.CAPTURES_PATH);

        long newDirSize = StorageUtils.getFileSize(new File(StorageUtils.CAPTURES_PATH));

        long diff = capturedImagesSize - newDirSize;

        Toast.makeText(getContext(), "Freed up " + StorageUtils.convertByteSizeToMBTwoDecimals(diff) + " MB of storage.", Toast.LENGTH_SHORT).show();

        clearCapturedImages.setTitle("Delete capture images: " + StorageUtils.convertByteSizeToMBTwoDecimals(newDirSize) + " MB");

        return true;

      }
    });

    // same procedure as clearSynchronizedImages, except training captures without an image are useless, so they get deleted

    final Preference clearTrainingImages = findPreference("clear_training");

    final long trainingImagesSize = StorageUtils.getFileSize(new File(StorageUtils.TRAINING_IMAGES_PATH));

    clearTrainingImages.setTitle("Delete training images: " + StorageUtils.convertByteSizeToMBTwoDecimals(trainingImagesSize) + " MB");

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

        StorageUtils.deleteAllFilesInDirectory(StorageUtils.TRAINING_IMAGES_PATH);

        long newDirSize = StorageUtils.getFileSize(new File(StorageUtils.TRAINING_IMAGES_PATH));

        long diff = trainingImagesSize - newDirSize;

        Toast.makeText(getContext(), "Freed up " + StorageUtils.convertByteSizeToMBTwoDecimals(diff) + " MB of storage.", Toast.LENGTH_SHORT).show();

        clearTrainingImages.setTitle("Delete training images: " + StorageUtils.convertByteSizeToMBTwoDecimals(newDirSize) + " MB");

        return true;

      }
    });



    // TODO add popup before clearing,
    // TODO grey out synchronitaion category if offline mode selected



  }
}
