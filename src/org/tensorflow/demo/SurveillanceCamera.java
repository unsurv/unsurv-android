package org.tensorflow.demo;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.graphics.RectF;
import android.location.Location;
import android.support.annotation.NonNull;

import java.io.File;


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

  private String mComment;


  public SurveillanceCamera (String thumbnailPath, String imagePath, int cameraLeft, int cameraRight, int cameraTop, int cameraBottom, double latitude, double longitude, double accuracy, String comment){

    mThumbnailPath = thumbnailPath;
    mImagePath = imagePath;
    mCameraLeft = cameraLeft;
    mCameraRight = cameraRight;
    mCameraTop = cameraTop;
    mCameraBottom = cameraBottom;

    mLatitude = latitude;
    mLongitude = longitude;
    mAccuracy = accuracy;

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

  public String getComment() {
    return mComment;
  }


  public void setId(int id) {
    this.id = id;
  }

  public void setThumbnailPath(@NonNull String mThumbnailPath) {
    this.mThumbnailPath = mThumbnailPath;
  }

  public void setImagePath(String mImagePath) {
    this.mImagePath = mImagePath;
  }

  public void setCameraLeft(int mCameraLeft) {
    this.mCameraLeft = mCameraLeft;
  }

  public void setCameraRight(int mCameraRight) {
    this.mCameraRight = mCameraRight;
  }

  public void setCameraTop(int mCameraTop) {
    this.mCameraTop = mCameraTop;
  }

  public void setCameraBottom(int mCameraBottom) {
    this.mCameraBottom = mCameraBottom;
  }

  public void setLatitude(double mLatitude) {
    this.mLatitude = mLatitude;
  }

  public void setLongitude(double mLongitude) {
    this.mLongitude = mLongitude;
  }

  public void setAccuracy(double mAccuracy) {
    this.mAccuracy = mAccuracy;
  }

  public void setComment(String mComment) {
    this.mComment = mComment;
  }
}
