package org.tensorflow.demo;

import android.os.Environment;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

class StorageUtils {

  // accessible for every app for now
  static String PICTURES_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/cameras/";
  static String TRAINING_IMAGES_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/training/";

  // TODO add 3rd option  sync, captures, training

  static long getFileSize(final File file) {
    if (file == null || !file.exists())
      return 0;
    if (!file.isDirectory())
      return file.length();
    final List<File> dirs = new LinkedList<>();
    dirs.add(file);
    long result = 0;
    while (!dirs.isEmpty()) {
      final File dir = dirs.remove(0);
      if (!dir.exists())
        continue;
      final File[] listFiles = dir.listFiles();
      if (listFiles == null || listFiles.length == 0)
        continue;
      for (final File child : listFiles) {
        result += child.length();
        if (child.isDirectory())
          dirs.add(child);
      }
    }
    return result;
  }


  static String convertByteSizeToMB(long byteSize){

    String tmp = String.valueOf(byteSize / 10000);

    String MbInByteSize = tmp.substring(0, tmp.length() - 2);
    String twoDecimals = tmp.substring(tmp.length() - 2);
    return MbInByteSize + "," + twoDecimals;
  }

}
