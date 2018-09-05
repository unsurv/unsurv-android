package org.tensorflow.demo;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = "local_surveillance_cameras")
public class SurveillanceCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;

  @NonNull

  private String thumbnailPath;

  private String imagePath;

  //private RectF mCameraLocation;
  private int cameraLeft;
  private int cameraRight;
  private int cameraTop;
  private int cameraBottom;


  //private Location mLocation;
  private double latitude;
  private double longitude;
  private double accuracy; // radius of 68% confidence in meters

  private double azimuth;
  private double pitch;
  private double roll;

  private String comment;


  public SurveillanceCamera (String thumbnailPath, String imagePath, int cameraLeft, int cameraRight, int cameraTop, int cameraBottom, double latitude, double longitude, double accuracy, double azimuth, double pitch, double roll, String comment){

    this.thumbnailPath = thumbnailPath;
    this.imagePath = imagePath;
    this.cameraLeft = cameraLeft;
    this.cameraRight = cameraRight;
    this.cameraTop = cameraTop;
    this.cameraBottom = cameraBottom;

    this.latitude = latitude;
    this.longitude = longitude;
    this.accuracy = accuracy;

    this.azimuth = azimuth;
    this.pitch = pitch;
    this.roll = roll;

    this.comment = comment;


  }



  public int getId() {
    return id;
  }

  public String getThumbnailPath() {
    return thumbnailPath;
  }

  public String getImagePath() {
    return imagePath;
  }

  public int getCameraLeft() {
    return cameraLeft;
  }

  public int getCameraRight() {
    return cameraRight;
  }

  public int getCameraTop() {
    return cameraTop;
  }

  public int getCameraBottom() {
    return cameraBottom;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public double getAccuracy() {
    return accuracy;
  }

  public double getAzimuth() {
    return azimuth;
  }

  public double getPitch() {
    return pitch;
  }

  public double getRoll() {
    return roll;
  }

  public String getComment() {
    return comment;
  }


  public void setId(int id) {
    this.id = id;
  }

  public void setThumbnailPath(String mThumbnailPath) {
    this.thumbnailPath = mThumbnailPath;
  }

  public void setImagePath(String ImagePath) {
    this.imagePath = ImagePath;
  }

  public void setCameraLeft(int CameraLeft) {
    this.cameraLeft = CameraLeft;
  }

  public void setCameraRight(int CameraRight) {
    this.cameraRight = CameraRight;
  }

  public void setCameraTop(int CameraTop) {
    this.cameraTop = CameraTop;
  }

  public void setCameraBottom(int CameraBottom) {
    this.cameraBottom = CameraBottom;
  }

  public void setLatitude(double Latitude) {
    this.latitude = Latitude;
  }

  public void setLongitude(double Longitude) {
    this.longitude = Longitude;
  }

  public void setAccuracy(double Accuracy) {
    this.accuracy = Accuracy;
  }

  public void setAzimuth(double Azimuth) {
    this.azimuth = Azimuth;
  }

  public void setPitch(double Pitch) {
    this.pitch = Pitch;
  }

  public void setRoll(double Roll) {
    this.roll = Roll;
  }

  public void setComment(String Comment) {
    this.comment = Comment;
  }
}
