package org.tensorflow.demo;

import android.support.annotation.Nullable;
import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class StatisticsUtils {

  static List<HashMap<Date, Integer>> getCamerasPerDayFromDb(double latMin, double latMax, double lonMin, double lonMax, String startDate, String endDate, @Nullable SynchronizedCameraRepository synchronizedCameraRepository){
    String TAG = "StatisticsUtils";
    List<StatisticsMap> camerasAddedByDaysFromDb = new ArrayList<>();
    List<HashMap<Date, Integer>> convertedStatsList = new ArrayList<>();

      if (synchronizedCameraRepository != null) {
        camerasAddedByDaysFromDb = synchronizedCameraRepository.getStatistics(latMin, latMax, lonMin, lonMax, startDate, endDate);

        SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (StatisticsMap item : camerasAddedByDaysFromDb) {
          try {
            Date statisticsDate = timestampIso8601.parse(item.uploadedDate);

            // TODO Date has no TimeZone but Date to String methods apply timezone? test if buggy
            Date functionStartDate = timestampIso8601.parse(startDate);
            Date functionEndDate = timestampIso8601.parse(endDate);

            if ((functionStartDate.before(statisticsDate) || functionStartDate.equals(statisticsDate)) &&
                    (functionEndDate.after(statisticsDate) || functionEndDate.equals(statisticsDate))
            ) {

              HashMap<Date, Integer> convertedStatisticsMap = new HashMap<>();
              convertedStatisticsMap.put(statisticsDate, item.camerasOnSpecificDay);

              convertedStatsList.add(convertedStatisticsMap);
            }



          } catch (ParseException parseExc) {
            Log.i(TAG, parseExc.toString());

          }
        }

      }

    return convertedStatsList;
  }
}
