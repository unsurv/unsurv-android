package org.tensorflow.demo;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;


@Entity (tableName = "synchronized_cameras")
public class SynchronizedCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;

  private String imagePath;

  private double latitude;
  private double longitude;

  private String comments;

  private String lastUpdated;


  public SynchronizedCamera (String imagePath,
                             double latitude,
                             double longitude,
                             String comments,
                             String lastUpdated) {

    this.imagePath = imagePath;
    this.latitude = latitude;
    this.longitude = longitude;
    this.comments = comments;
    this.lastUpdated = lastUpdated;
  }

  public int getId() {
    return id;
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

  public void setId(int id) {
    this.id = id;
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

}
