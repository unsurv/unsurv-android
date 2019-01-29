package org.tensorflow.demo;

import android.location.Location;


public class CameraCapture {

  private int id;

  private float confidence;

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



  CameraCapture(float confidence,
                       String thumbnailPath,
                       String imagePath,
                       int cameraLeft,
                       int cameraRight,
                       int cameraTop,
                       int cameraBottom,
                       double latitude,
                       double longitude,
                       double accuracy,
                       double azimuth,
                       double pitch,
                       double roll
                       ){

    this.confidence = confidence;
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



  }

  Location intersectWith(CameraCapture secondCapture) {

    Location estimatedCameraLocation;
    Location camera1Location = new Location("camera1");
    Location camera2Location = new Location("camera2");

    camera1Location.setLatitude(latitude);
    camera1Location.setLongitude(longitude);

    camera2Location.setLatitude(secondCapture.getLatitude());
    camera2Location.setLongitude(secondCapture.getLongitude());

    // Approximate in 2D coordinates in meters with straight lines as headings.

    // points
    double x1;
    double y1;

    double x2;
    double y2;

    // slope
    double m1;
    double m2;

    // y intersect
    double c1;
    double c2;

    // calculate slope with cotangent
    m1 = Math.cos(azimuth) / Math.sin(azimuth);
    m2 = Math.cos(secondCapture.getAzimuth()) / Math.sin(secondCapture.getAzimuth());


    // camera1 in approximation is positioned at (0|0)
    x1 = 0;
    y1 = 0;

    c1 = 0;


    // distance to another capture in m
    float distanceToSecondCapture = camera1Location.distanceTo(camera2Location);

    // bearing to another capture in degrees east from true north
    float bearingToSecondCapture = camera1Location.bearingTo(camera2Location);


    // approximate second point in 2D coordinates

    // 1 degree of latitude equals 110574 m at the equator. Doesn't change much further north/south
    y2 = (secondCapture.getLatitude() - this.getLatitude())*110574;

    // get x2 from pythagoras with distance and y distance.
    x2 = Math.sqrt(Math.pow(distanceToSecondCapture, 2) - Math.pow(y2, 2));

    // y-axis intersect
    c2 = (m2*-x2) - y2;

    // intersect coordinates

    double intersectX = (c2-c1) / (m1-m2);
    double intersectY = m1*intersectX + c1;

    estimatedCameraLocation = LocationUtils.getNewLocation(latitude, longitude, intersectY, intersectX);

    return estimatedCameraLocation;

  }






  public int getId() {
    return id;
  }

  public float getConfidence() {
    return confidence;
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

  public void setId(int id) {
    this.id = id;
  }

  public void setConfidence(float confidence) {
    this.confidence = confidence;
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

}
