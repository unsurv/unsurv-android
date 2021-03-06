package org.unsurv;

import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper class for statistics functions
 */
class StatisticsUtils {

  final static String TAG = "StatisticsUtils";

  /**
   * Method to get datapoints per day from db
   *
   * @param latMin latmin
   * @param latMax latmax
   * @param lonMin lonmin
   * @param lonMax lonmax
   * @param startDate yyyy-mm-dd
   * @param endDate yyyy-mm-dd
   * @param synchronizedCameraRepository synchronizedCameraRepository object
   * @return List<StatisticsMap> with per day statistics from db
   */
  static List<StatisticsMap> getCamerasPerDayFromDb(double latMin, double latMax, double lonMin, double lonMax, Date startDate, Date endDate, SynchronizedCameraRepository synchronizedCameraRepository){
    String TAG = "StatisticsUtils";
    List<StatisticsMap> camerasAddedByDaysFromDb;
    List<StatisticsMap> filteredStatsByDays =  new ArrayList<>();

    // List<HashMap<Date, Integer>> convertedStatsList = new ArrayList<>();

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    // format does add time(HH:mm:ss) and timezone in string. account for that craziness
    String dbStartString = timestampIso8601.format(startDate).split(" ")[0];
    String dbEndString = timestampIso8601.format(endDate).split(" ")[0];


      if (synchronizedCameraRepository != null) {
        camerasAddedByDaysFromDb = synchronizedCameraRepository.getPerDayStatistics(latMin, latMax, lonMin, lonMax, dbStartString, dbEndString);


        for (StatisticsMap item : camerasAddedByDaysFromDb) {

          try {
            Date statisticsDate = timestampIso8601.parse(item.uploadedDate);

            // TODO Date has no TimeZone but Date to String methods apply timezone? test if buggy

            if ((startDate.before(statisticsDate) || startDate.equals(statisticsDate)) &&
                    (endDate.after(statisticsDate) || endDate.equals(statisticsDate))
            ) {


              filteredStatsByDays.add(item);
            }

          } catch (ParseException parseExc) {
            Log.i(TAG, parseExc.toString());

          }

        }
      }

    return filteredStatsByDays;
  }

  /**
   * returns amount of cameras in timeframe
   * @param startDate yyyy-MM-dd start
   * @param endDate yyyy-MM-dd end
   * @param synchronizedCameraRepository synchronizedCameraRepository object
   * @return int: how many cameras have been added in the given timeframe
   */
  static int getTotalCamerasAddedInTimeframeFromDb(Date startDate, Date endDate, SynchronizedCameraRepository synchronizedCameraRepository){

    try {
      SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
      timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

      String dbStartString = timestampIso8601.format(startDate).split(" ")[0];
      String dbEndString = timestampIso8601.format(endDate).split(" ")[0];

      return synchronizedCameraRepository.getAmountInTimeframe(dbStartString, dbEndString);

    } catch (Exception e) {
      Log.i(TAG, e.toString());
      return 0;
    }

  }

  static int totalCamerasInDb(SynchronizedCameraRepository synchronizedCameraRepository){
    return synchronizedCameraRepository.getTotalAmountInDb();
  }

  static int totalCamerasCapturedOnDevice(CameraRepository cameraRepository){
    return cameraRepository.getCamerasAddedByUserCount();
  }

}
