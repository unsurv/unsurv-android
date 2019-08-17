package org.unsurv;

import androidx.room.ColumnInfo;

public class StatisticsMap {
  @ColumnInfo(name = "uploadedAt")
  public String uploadedDate;

  @ColumnInfo(name = "COUNT(uploadedAt)")
  public Integer camerasOnSpecificDay;

}
