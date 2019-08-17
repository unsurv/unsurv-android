package org.unsurv;

import androidx.room.ColumnInfo;

/**
 * Will be used in the future to minimize network usage when querying server for info
 * outside of homezone.
 */

public class SlimSynchronizedCamera {
  @ColumnInfo(name = "latitude")
  public double latitude;

  @ColumnInfo(name = "longitude")
  public double longitude;

  @ColumnInfo(name = "externalID")
  public String externalID;


  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public String getExternalID() {
    return externalID;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public void setExternalID(String externalID) {
    this.externalID = externalID;
  }
}
