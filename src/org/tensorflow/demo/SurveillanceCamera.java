package org.tensorflow.demo;

import android.graphics.RectF;
import android.location.Location;

import java.io.File;

public class SurveillanceCamera {

  private String mThumbnailPath;

  private String mImagePath;

  private RectF mCameraLocation;

  private Location mLocation;

  private String mComment;


  public SurveillanceCamera (String thumbnailPath, String imagePath, RectF cameraLocation, Location location, String comment){

    mThumbnailPath = thumbnailPath;
    mImagePath = imagePath;
    mCameraLocation = cameraLocation;
    mLocation = location;
    mComment = comment;
  }

  public SurveillanceCamera (String thumbnailPath, String imagePath, RectF cameraLocation, Location location){

    mThumbnailPath = thumbnailPath;
    mImagePath = imagePath;
    mCameraLocation = cameraLocation;
    mLocation = location;

  }


  public String getmThumbnailPath() {
    return mThumbnailPath;
  }

  public String getmImagePath() {
    return mImagePath;
  }

  public RectF getmCameraLocation() {
    return mCameraLocation;
  }

  public Location getmLocation() {
    return mLocation;
  }

  public String getmComment() {
    return mComment;
  }




}
