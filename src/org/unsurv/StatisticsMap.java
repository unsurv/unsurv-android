package org.unsurv;

import androidx.room.ColumnInfo;

/**
 * Used as a return from a db query that represents datapoints per day
 *
 * 2019-01-01 13
 *
 */
class StatisticsMap {
  @ColumnInfo(name = "uploadedAt")
  String uploadedDate;

  @ColumnInfo(name = "COUNT(uploadedAt)")
  Integer camerasOnSpecificDay;


  // TODO add toString method
}
