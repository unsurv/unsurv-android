package org.unsurv;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.Nullable;

/**
 * Object representing a local camera capture by the user. Either manual, via object detection or
 * a training capture
 */

@Entity(tableName = "local_surveillance_cameras")
public class SurveillanceCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;

  private int cameraType;

  private String thumbnailPath;

  @Nullable
  private String imagePath;

  @Nullable
  private String externalId;

  private double latitude;
  private double longitude;

  private String comment;

  private String timestamp; // can be disabled in settings
  private String timeToSync; // time where photo was taken + random delay for privacy reasons

  // status
  private boolean locationUploaded;
  private boolean uploadCompleted;
  private boolean manualCapture;
  private boolean trainingCapture;

  // rectangles drawn in DrawOnTrainingImageActivities, only used when object is a training capture
  private String drawnRectsAsString;
  // all images associated with the object
  private String captureFilenames;


  public SurveillanceCamera(int cameraType,
                            @Nullable String thumbnailPath,
                            @Nullable String imagePath,
                            @Nullable String externalId,
                            double latitude,
                            double longitude,
                            @Nullable String comment,
                            @Nullable String timestamp,
                            String timeToSync,
                            boolean locationUploaded,
                            boolean uploadCompleted,
                            boolean manualCapture,
                            boolean trainingCapture,
                            String drawnRectsAsString,
                            String captureFilenames){

    this.cameraType = cameraType;
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

    this.manualCapture = manualCapture;

    this.trainingCapture = trainingCapture;
    this.drawnRectsAsString = drawnRectsAsString;
    this.captureFilenames = captureFilenames;


  }



  public int getId() {
    return id;
  }

  public int getCameraType(){return cameraType;}

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
  public boolean getManualCapture(){
    return manualCapture;
  }

  public boolean getTrainingCapture(){
    return trainingCapture;
  }

  public String getDrawnRectsAsString(){
    return drawnRectsAsString;
  }

  public String getCaptureFilenames() {
    return captureFilenames;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setCameraType(int type){this.cameraType = type;}

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
  public void setManualCapture(boolean manualCapture){this.manualCapture = manualCapture; }
  public void setTrainingCapture(boolean trainingCapture){this.trainingCapture = trainingCapture; }

  public void setDrawnRectsAsString(String drawnRectsAsString) {
    this.drawnRectsAsString = drawnRectsAsString;
  }

  public void setCaptureFilenames(String captureFilenames) {
    this.captureFilenames = captureFilenames;
  }

//TODO add delete function to delete all Files connected to specific camera

}
