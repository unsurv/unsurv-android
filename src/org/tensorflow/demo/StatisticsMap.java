package org.tensorflow.demo;

import android.arch.persistence.room.ColumnInfo;

public class StatisticsMap {
  @ColumnInfo(name = "uploadedAt")
  public String uploadedDate;

  @ColumnInfo(name = "COUNT(uploadedAt)")
  public Integer camerasOnSpecificDay;

}
