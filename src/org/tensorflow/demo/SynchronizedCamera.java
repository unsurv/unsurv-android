package org.tensorflow.demo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.Nullable;


@Entity (tableName = "synchronized_cameras")
public class SynchronizedCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;

  private String externalID;

  private int type;

  private String imagePath;

  private double latitude;
  private double longitude;

  private String comments;

  private String lastUpdated;
  private String uploadedAt;

  private boolean manualCapture;


  public SynchronizedCamera (@Nullable String imagePath,
                             String externalID,
                             int type,
                             double latitude,
                             double longitude,
                             String comments,
                             String lastUpdated,
                             String uploadedAt,
                             boolean manualCapture) {

    this.imagePath = imagePath;
    this.externalID = externalID;
    this.type = type;
    this.latitude = latitude;
    this.longitude = longitude;
    this.comments = comments;
    this.lastUpdated = lastUpdated;
    this.uploadedAt = uploadedAt;
    this.manualCapture = manualCapture;
  }

  public int getId() {
    return id;
  }

  public String getExternalID() {
    return externalID;
  }

  public int getType() {
    return type;
  }

  public String getImagePath() {
    return imagePath;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public String getComments() {
    return comments;
  }

  public String getLastUpdated() {
    return lastUpdated;
  }

  public String getUploadedAt() {
    return uploadedAt;
  }

  public boolean getManualCapture(){return manualCapture;}

  public void setId(int id) {
    this.id = id;
  }

  public void setExternalID(String externalID) {
    this.externalID = externalID;
  }

  public void setType(int type) {
    this.type = type;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public void setLastUpdated(String lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public void setUploadedAt(String uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public void setManualCapture(boolean manualCapture){this.manualCapture = manualCapture;}
}
