package org.unsurv;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;

class StorageUtils {

  static String TAG = "StorageUtils";


  final static int UNKNOWN = -1;

  // openstreetmap camera types
  final static int FIXED_CAMERA = 0;
  final static int DOME_CAMERA = 1;
  final static int PANNING_CAMERA = 2;

  // osm "surveillance" tag
  final static int AREA_UNKNOWN = 0;
  final static int AREA_OUTDOOR = 1;
  final static int AREA_PUBLIC = 2;
  final static int AREA_INDOOR = 3;
  final static int AREA_TRAFFIC = 4;

  // osm mount types
  final static int MOUNT_UNKNOWN = 0;
  final static int MOUNT_POLE = 1;
  final static int MOUNT_WALL = 2;
  final static int MOUNT_CEILING = 3;
  final static int MOUNT_STREET_LAMP = 4;

  // accessible for every app for now
  final static String SYNCHRONIZED_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/cameras/synchronized/";

  final static String CAMERA_CAPTURES_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/cameras/captures/";

  final static String TRAINING_CAPTURES_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/training/";


  /**
   * return filesize as bytes
   * @param file File object representing a file on the device
   * @return size in bytes
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


  // TODO test this
  /**
   * returns a byteSize as a String representing the size in MB with 2 decimals
   * @param byteSize number of bytes
   * @return readable String representing the size "1,78 MB" for example
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
   * @param f File
   * @return ByteArray representing the File
   * @throws IOException can't access
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
   * @param bytes ByteArray
   * @param filename filename
   * @param path directory to save file
   * @throws IOException can't be saved
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
   * @return number of deleted files
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
   * @param camera SynchronizedCamera object
   * @return number of deleted files
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
   * recursive deletion of directory, should not be used, recreates top directory afterwards
   * @param directory directory to be deleted
   * @return number of deleted files
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

  /**
   * exports captures to a csv file
   * @param camerasToExport
   * @return true if successful
   */

  static boolean exportCaptures(List<SurveillanceCamera> camerasToExport) {


    String exportPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .getAbsolutePath() + "/unsurv/export/";

    String filename = "export.txt";

    String header = "TYPE,AREA,DIRECTION,MOUNT,HEIGHT,ANGLE,THUMBNAIL,IMAGE,LAT,LON,ISTRAINING,BOXES\n";

    File exportDir = new File(exportPath);
    exportDir.mkdirs();

    File exportFile = new File(exportPath + filename);

    // delete old export file
    if (exportFile.isFile()) {
      exportFile.delete();
    }

      try {
        FileWriter writer = new FileWriter(exportPath + filename);
        writer.write(header);

        for (SurveillanceCamera camera : camerasToExport) {
          writer.write(camera.toString() + "\n");
        }

        writer.close();

      } catch (IOException ioe) {
        return false;
      }

    return true;

  }

  static boolean exportImages(List<SurveillanceCamera> camerasToExport) {

    String exportPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .getAbsolutePath() + "/unsurv/export/";

    for (SurveillanceCamera camera : camerasToExport) {

      boolean isTrainingImage = camera.getTrainingCapture();

      // TODO copy files instead of moving them
      if (isTrainingImage) {
        // for training captures the complete image is needed

        String trainingImagePath = camera.getImagePath();

        File src = new File(TRAINING_CAPTURES_PATH + trainingImagePath);
        File dest = new File(exportPath + trainingImagePath);

        src.renameTo(dest);

      } else {
        // for regular captures use detection box image
        String thumbnailPath = camera.getThumbnailPath();

        File src = new File(CAMERA_CAPTURES_PATH + thumbnailPath);
        File dest = new File(exportPath + thumbnailPath);

        src.renameTo(dest);

        // Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    }

    return true;
  }


}
