package org.tensorflow.demo;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import java.io.File;

public class SettingsFragment extends PreferenceFragment {

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);

    Preference clearSynchronizedImages = findPreference("clear_synchronized");

    long synchronizedImagesSize = StorageUtils.getFileSize(new File(StorageUtils.PICTURES_PATH));

    clearSynchronizedImages.setTitle("size: " + StorageUtils.convertByteSizeToMB(synchronizedImagesSize) + " MB");

    // TODO add popup before clearing



  }
}
