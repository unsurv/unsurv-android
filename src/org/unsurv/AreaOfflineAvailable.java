package org.unsurv;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Database class to store areas that have been visited outside the users "home zone". It is meant
 * ease load on the server.
 *
 * - saves areas where server has been queried for and cameras have been downloaded
 * - when user moves map to same area x mins after last query, dont query server again
 */

@Entity(tableName = "offline_areas")
public class AreaOfflineAvailable {

  @PrimaryKey(autoGenerate = true)
  private int id;

  private double latMin;
  private double latMax;
  private double lonMin;
  private double lonMax;

  private String lastUpdated;

  AreaOfflineAvailable (double latMin,
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

  public int getId() {
    return id;
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

  public void setId(int id) {
    this.id = id;
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
