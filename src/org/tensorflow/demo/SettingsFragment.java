package org.tensorflow.demo;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.annotation.Nullable;

import java.io.File;

public class SettingsFragment extends PreferenceFragment {

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);

    // TODO onclick to clear individual stuff

    Preference clearSynchronizedImages = findPreference("clear_synchronized");

    long synchronizedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.SYNCHRONIZED_PATH));

    clearSynchronizedImages.setTitle("Delete downloaded images: " + StorageUtils.convertByteSizeToMB(synchronizedImagesSize) + " MB");


    Preference clearCapturedImages = findPreference("clear_captures");

    long capturedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.CAPTURES_PATH));

    clearCapturedImages.setTitle("Delete capture images: " + StorageUtils.convertByteSizeToMB(capturedImagesSize) + " MB");


    Preference clearTrainingImages = findPreference("clear_training");

    long trainingImagesSize = StorageUtils.getFileSize(new File(StorageUtils.TRAINING_IMAGES_PATH));

    clearTrainingImages.setTitle("Delete training images: " + StorageUtils.convertByteSizeToMB(trainingImagesSize) + " MB");



    // TODO add popup before clearing
    // TODO grey out synchronitaion category if offline mode selected



  }
}
