package org.tensorflow.demo;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = "local_surveillance_cameras")
public class SurveillanceCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;

  private String thumbnailPath;

  private String imagePath;

  private double latitude;
  private double longitude;

  private String comment;

  private String timestamp;
  private String timeToSync; // time where photo was taken + random delay for privacy reasons


  public SurveillanceCamera(String thumbnailPath,
                       String imagePath,
                       double latitude,
                       double longitude,

                       String comment,
                       String timestamp,
                       String timeToSync){

    this.thumbnailPath = thumbnailPath;
    this.imagePath = imagePath;

    this.latitude = latitude;
    this.longitude = longitude;

    this.comment = comment;

    this.timestamp = timestamp;
    this.timeToSync = timeToSync;


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

  public void setId(int id) {
    this.id = id;
  }

  public void setThumbnailPath(String mThumbnailPath) {
    this.thumbnailPath = mThumbnailPath;
  }

  public void setImagePath(String ImagePath) {
    this.imagePath = ImagePath;
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
}
