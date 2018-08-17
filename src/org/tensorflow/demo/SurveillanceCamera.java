package org.tensorflow.demo;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = "local_surveillance_cameras")
public class SurveillanceCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;

  @NonNull

  private String mThumbnailPath;

  private String mImagePath;

  //private RectF mCameraLocation;
  private int mCameraLeft;
  private int mCameraRight;
  private int mCameraTop;
  private int mCameraBottom;


  //private Location mLocation;
  private double mLatitude;
  private double mLongitude;
  private double mAccuracy; // radius of 68% confidence in meters

  private double mAzimuth;
  private double mPitch;
  private double mRoll;

  private String mComment;


  public SurveillanceCamera (String thumbnailPath, String imagePath, int cameraLeft, int cameraRight, int cameraTop, int cameraBottom, double latitude, double longitude, double accuracy, double azimuth, double pitch, double roll, String comment){

    mThumbnailPath = thumbnailPath;
    mImagePath = imagePath;
    mCameraLeft = cameraLeft;
    mCameraRight = cameraRight;
    mCameraTop = cameraTop;
    mCameraBottom = cameraBottom;

    mLatitude = latitude;
    mLongitude = longitude;
    mAccuracy = accuracy;

    mAzimuth = azimuth;
    mPitch = pitch;
    mRoll = roll;

    mComment = comment;
  }



  public int getId() {
    return id;
  }

  public String getThumbnailPath() {
    return mThumbnailPath;
  }

  public String getImagePath() {
    return mImagePath;
  }

  public int getCameraLeft() {
    return mCameraLeft;
  }

  public int getCameraRight() {
    return mCameraRight;
  }

  public int getCameraTop() {
    return mCameraTop;
  }

  public int getCameraBottom() {
    return mCameraBottom;
  }

  public double getLatitude() {
    return mLatitude;
  }

  public double getLongitude() {
    return mLongitude;
  }

  public double getAccuracy() {
    return mAccuracy;
  }

  public double getAzimuth() {
    return mAzimuth;
  }

  public double getPitch() {
    return mPitch;
  }

  public double getRoll() {
    return mRoll;
  }

  public String getComment() {
    return mComment;
  }


  public void setId(int id) {
    this.id = id;
  }

  public void setThumbnailPath(@NonNull String mThumbnailPath) {
    this.mThumbnailPath = mThumbnailPath;
  }

  public void setImagePath(String ImagePath) {
    this.mImagePath = ImagePath;
  }

  public void setCameraLeft(int CameraLeft) {
    this.mCameraLeft = CameraLeft;
  }

  public void setCameraRight(int CameraRight) {
    this.mCameraRight = CameraRight;
  }

  public void setCameraTop(int CameraTop) {
    this.mCameraTop = CameraTop;
  }

  public void setCameraBottom(int CameraBottom) {
    this.mCameraBottom = CameraBottom;
  }

  public void setLatitude(double Latitude) {
    this.mLatitude = Latitude;
  }

  public void setLongitude(double Longitude) {
    this.mLongitude = Longitude;
  }

  public void setAccuracy(double Accuracy) {
    this.mAccuracy = Accuracy;
  }

  public void setAzimuth(double Azimuth) {
    this.mAzimuth = Azimuth;
  }

  public void setPitch(double Pitch) {
    this.mPitch = Pitch;
  }

  public void setRoll(double Roll) {
    this.mRoll = Roll;
  }

  public void setComment(String Comment) {
    this.mComment = Comment;
  }
}
