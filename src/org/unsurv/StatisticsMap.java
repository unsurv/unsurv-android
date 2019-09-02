package org.unsurv;

import androidx.room.ColumnInfo;

/**
 * Used as a return from a db query that represents datapoints per day
 *
 * 2019-01-01 13
 *
 */
public class StatisticsMap {
  @ColumnInfo(name = "uploadedAt")
  public String uploadedDate;

  @ColumnInfo(name = "COUNT(uploadedAt)")
  public Integer camerasOnSpecificDay;


  // TODO add toString method
}
