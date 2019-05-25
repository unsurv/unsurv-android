package org.tensorflow.demo;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


@Entity(tableName = "local_surveillance_cameras")
public class SurveillanceCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;

  private String thumbnailPath;

  private String imagePath;

  @Nullable
  private String externalId;

  private double latitude;
  private double longitude;

  private String comment;

  private String timestamp;
  private String timeToSync; // time where photo was taken + random delay for privacy reasons

  private boolean locationUploaded;

  private boolean uploadCompleted;


  public SurveillanceCamera(String thumbnailPath,
                       String imagePath,
                       @Nullable String externalId,
                       double latitude,
                       double longitude,
                       String comment,
                       String timestamp,
                       String timeToSync,
                       boolean locationUploaded,
                       boolean uploadCompleted){

    this.thumbnailPath = thumbnailPath;
    this.imagePath = imagePath;

    this.externalId = externalId;

    this.latitude = latitude;
    this.longitude = longitude;

    this.comment = comment;

    this.timestamp = timestamp;
    this.timeToSync = timeToSync;

    this.locationUploaded = locationUploaded;

    this.uploadCompleted = uploadCompleted;


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

  @Nullable
  public String getExternalId() {
    return externalId;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }


  public String getComment() {
    return comment;
  }

  public String getTimestamp() { return timestamp; }

  public String getTimeToSync() { return timeToSync; }

  public boolean getUploadCompleted(){
    return uploadCompleted;
  }
  public boolean getLocationUploaded(){
    return locationUploaded;
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

  public void setExternalId(@Nullable String externalId) {
    this.externalId = externalId;
  }

  public void setLatitude(double Latitude) {
    this.latitude = Latitude;
  }

  public void setLongitude(double Longitude) {
    this.longitude = Longitude;
  }

  public void setComment(String Comment) {
    this.comment = Comment;
  }

  public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

  public void setTimeToSync(String timeToSync) { this.timeToSync = timeToSync; }

  public void setLocationUploaded(boolean locationUploaded){this.locationUploaded = locationUploaded; }
  public void setUploadCompleted(boolean uploadCompleted){this.uploadCompleted = uploadCompleted; }
}
