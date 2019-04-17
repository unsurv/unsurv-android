package org.tensorflow.demo;


import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "offline_areas")
public class AreaOfflineAvailable {

  @PrimaryKey(autoGenerate = true)
  int id;

  private double latMin;
  private double latMax;
  private double lonMin;
  private double lonMax;

  private String lastUpdated;

  public AreaOfflineAvailable (double latMin,
                               double latMax,
                               double lonMin,
                               double lonMax,
                               String lastUpdated) {
    this.latMin = latMin;
    this.latMax = latMax;
    this.lonMin = lonMin;
    this.lonMax = lonMax;
    this.lastUpdated = lastUpdated;

  }

  public double getLatMin() {
    return latMin;
  }

  public double getLatMax() {
    return latMax;
  }

  public double getLonMin() {
    return lonMin;
  }

  public double getLonMax() {
    return lonMax;
  }

  public String getLastUpdated() {
    return lastUpdated;
  }


  public void setLatMin(double latMin) {
    this.latMin = latMin;
  }

  public void setLatMax(double latMax) {
    this.latMax = latMax;
  }

  public void setLonMin(double lonMin) {
    this.lonMin = lonMin;
  }

  public void setLonMax(double lonMax) {
    this.lonMax = lonMax;
  }

  public void setLastUpdated(String lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
