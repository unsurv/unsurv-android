package org.tensorflow.demo;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;


public class StatisticsUtils {

  static List<StatisticsMap> getCamerasPerDay(double latMin, double latMax, double lonMin, double lonMax, String startDate, String endDate, boolean getStatsFromServer, @Nullable SynchronizedCameraRepository synchronizedCameraRepository){
    List<StatisticsMap> camerasAddedByDaysFromDb = new ArrayList<>();

    if (getStatsFromServer) {
      // query server
    } else {

      if (synchronizedCameraRepository != null) {
        camerasAddedByDaysFromDb = synchronizedCameraRepository.getStatistics(latMin, latMax, lonMin, lonMax, startDate, endDate);

      }

    }

    return camerasAddedByDaysFromDb;
  }
}
