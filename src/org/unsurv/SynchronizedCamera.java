package org.unsurv;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.Nullable;

/**
 * Object representing a camera that has been downloaded from the database server.
 */
@Entity (tableName = "synchronized_cameras")
public class SynchronizedCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;

  private String externalID;

  private int type; // fixed, dome, panning
  private int area; // unknown, outdoor, public, indoor, traffic
  private int mount; // unknown, pole, wall, ceiling, streetlamp
  private int direction; // unknown, pole, wall, ceiling, streetlamp
  private int height; // 0 = unknown, in meters editable by user
  private int angle; // -1 = unknown,  15 to 90 Degree from horizontal

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
                             int area,
                             int direction,
                             int mount,
                             int height,
                             int angle,
                             double latitude,
                             double longitude,
                             String comments,
                             String lastUpdated,
                             String uploadedAt,
                             boolean manualCapture) {

    this.imagePath = imagePath;
    this.externalID = externalID;

    this.type = type;
    this.area = area;
    this.direction = direction;
    this.mount = mount;
    this.height = height;
    this.angle = angle;
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

  public int getArea() {
    return area;
  }

  public int getDirection() {
    return direction;
  }

  public int getMount() {
    return mount;
  }

  public int getHeight() {
    return height;
  }

  public int getAngle() {
    return angle;
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

  public void setArea(int area) {
    this.area = area;
  }

  public void setDirection(int direction) {
    this.direction = direction;
  }

  public void setMount(int mount) {
    this.mount = mount;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public void setAngle(int angle) {
    this.angle = angle;
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
