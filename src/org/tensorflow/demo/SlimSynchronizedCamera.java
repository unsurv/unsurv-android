package org.tensorflow.demo;

import androidx.room.ColumnInfo;

public class SlimSynchronizedCamera {
  @ColumnInfo(name = "latitude")
  public double latitude;

  @ColumnInfo(name = "longitude")
  public double longitude;

  @ColumnInfo(name = "externalID")
  public String externalID;
}
