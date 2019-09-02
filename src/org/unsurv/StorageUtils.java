package org.unsurv;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

class StorageUtils {

  static String TAG = "StorageUtils";

  final static int STANDARD_CAMERA = 0;
  final static int DOME_CAMERA = 1;
  final static int UNKNOWN_CAMERA = 2;

  // accessible for every app for now
  final static String SYNCHRONIZED_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/cameras/synchronized/";

  final static String CAMERA_CAPTURES_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/cameras/captures/";

  final static String TRAINING_CAPTURES_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/training/";


  /**
   * return filesize as bytes
   * @param file
   * @return
   */
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


  /**
   * returns a byteSize as a String representing the size in MB with 2 decimals
   * @param byteSize
   * @return
   */
  static String convertByteSizeToMBTwoDecimals(long byteSize){

    String tmp = String.valueOf(byteSize / 10000);

    // if less than 10 kB
    if (byteSize < 10000) {
      return "0,00";
    }

    String MbInByteSize = tmp.substring(0, tmp.length() - 2);
    String twoDecimals = tmp.substring(tmp.length() - 2);

    if (MbInByteSize.isEmpty()){
      return "0," + twoDecimals;
    } else {
      return MbInByteSize + "," + twoDecimals;
    }

  }

  /**
   * returns a bytearray from file, used for encryption
   * @param f
   * @return
   * @throws IOException
   */
  static byte[] readFileToBytes(File f) throws IOException {
    int size = (int) f.length();
    byte[] bytes = new byte[size];
    byte[] tmpBuff = new byte[size];
    FileInputStream fis = new FileInputStream(f);

    try {

      // tries to read file in one go
      int read = fis.read(bytes, 0, size);

      // if file is too big
      if (read < size) {
        int remain = size - read;
        // reads and appends to read from tmpBuff until file is read
        while (remain > 0) {
          read = fis.read(tmpBuff, 0, remain);
          System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
          remain -= read;
        }
      }
    }  catch (IOException e){
      throw e;
    } finally {
      fis.close();
    }

    return bytes;
  }

  /**
   * save bytearray to file
   * @param bytes
   * @param filename
   * @param path
   * @throws IOException
   */
  static void saveBytesToFile(byte[] bytes, String filename, String path) throws IOException {

    File directoryCheck = new File(path);
    if (!directoryCheck.exists()){
      directoryCheck.mkdirs();
    }

    File file = new File(path + filename);

    FileOutputStream fileOutputStream = new FileOutputStream(file.getPath());

    fileOutputStream.write(bytes);
    fileOutputStream.close();
  }

  /**
   * Deletes saved files for a single SurveillanceCamera
   * @param camera SurveillanceCamera object
   * @return
   */
  static int deleteImagesForCamera(SurveillanceCamera camera){

    File imageFile;
    File thumbnailFile;
    File multipleCaptureFile;
    String[] multipleCapturesFilenames;

    int deletedFiles = 0;

    // gets type of camera and deletes corresponding files
    try {
      if (camera.getTrainingCapture()){
        imageFile = new File(TRAINING_CAPTURES_PATH + camera.getImagePath());
        if (imageFile.delete()){
          deletedFiles++;
        }
      } else {
        imageFile = new File(CAMERA_CAPTURES_PATH + camera.getImagePath());
        if (imageFile.delete()){
          deletedFiles++;
        }
      }
    } catch (Exception e){
      Log.i(TAG, "deleteImages:" + e);
    }


    try {
      thumbnailFile = new File(CAMERA_CAPTURES_PATH + camera.getThumbnailPath());
      if (thumbnailFile.delete()){
        deletedFiles++;
      }
    } catch (Exception e){
      Log.i(TAG, "deleteImages:" + e);
    }

    // gets all captures and deletes files
    try {
      multipleCapturesFilenames = camera.getCaptureFilenames()
              .replace("\"", "")
              .replace("[", "")
              .replace("]", "")
              .split(",");

      for (String path : multipleCapturesFilenames){
        multipleCaptureFile = new File(CAMERA_CAPTURES_PATH + path);
        if (multipleCaptureFile.delete()){
          deletedFiles++;
        }
      }

    } catch (Exception e){
      Log.i(TAG, "deleteImages:" + e);
    }

    return deletedFiles;
  }


  /**
   * Deletes saved files for a single SynchronizedCamera
   * @param camera
   * @return
   */
  static int deleteImagesForSynchronizedCamera(SynchronizedCamera camera){

    File imageFile;
    int deletedFiles = 0;

    try {

      imageFile = new File(SYNCHRONIZED_PATH + camera.getImagePath());
      if (imageFile.delete()){
        deletedFiles++;
      }
    } catch (Exception e){
      Log.i(TAG, "deleteImages:" + e);
    }

    Log.i(TAG, "deleted " + deletedFiles + "images");

    return deletedFiles;
  }

  /**
   * recursive deletion of directory, should not use
   * @param directory
   * @return
   */
  static long deleteAllFilesInDirectory(String directory){

    // recursive delete a good idea? TODO visit again
    File directoryFile = new File(directory);

    final List<File> dirs = new LinkedList<>();
    dirs.add(directoryFile);
    long result = 0;
    while (!dirs.isEmpty()) {
      final File dir = dirs.remove(0);
      if (!dir.exists())
        continue;
      final File[] listFiles = dir.listFiles();
      if (listFiles == null || listFiles.length == 0)
        continue;
      for (final File child : listFiles) {
        child.delete();
        result++;
        if (child.isDirectory())
          dirs.add(child);
      }
    }

    File recreateDir = new File(directory);
    recreateDir.mkdirs();

    return result;
  }

}
