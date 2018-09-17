package org.tensorflow.demo;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

  private BottomNavigationView bottomNavigationView;
  private CameraRoomDatabase cameraDb;
  private List<Pair<String, Integer>> intervalSizes;
  private List<BarEntry> barEntries;
  private List<Entry> lineEntries;
  private AsyncCamerasInTimeframe mAsyncCamerasInTimeframe;
  private BarChart statisticsBarChart;
  private LineChart statisticsLineChart;

  // Operating modes for AsyncTask to fetch data. Needs more detail for further graphs.
  private final int ASYNC_BAR_MODE = 0;
  private final int ASYNC_LINE_MODE = 1;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_statistics);
    cameraDb = CameraRoomDatabase.getDatabase(getApplicationContext());

    Spinner timeframeBarSpinner = findViewById(R.id.statistics_bar_spinner);
    Spinner timeframeLineSpinner = findViewById(R.id.statistics_line_spinner);


    statisticsBarChart = findViewById(R.id.statistics_barchart);

    statisticsBarChart.setPinchZoom(false);
    statisticsBarChart.setDoubleTapToZoomEnabled(false);
    statisticsBarChart.setScaleXEnabled(false);
    statisticsBarChart.setScaleYEnabled(false);

    XAxis barXAxis = statisticsBarChart.getXAxis();
    YAxis barYAxis = statisticsBarChart.getAxisLeft();

    // Disable right-side axis.
    statisticsBarChart.getAxisRight().setEnabled(false);

    barXAxis.setTextColor(Color.WHITE);
    barYAxis.setTextColor(Color.WHITE);

    barXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);


    statisticsLineChart = findViewById(R.id.statistics_linechart);

    XAxis lineXAxis = statisticsLineChart.getXAxis();
    YAxis lineYAxis = statisticsLineChart.getAxisLeft();

    lineXAxis.setTextColor(Color.WHITE);
    lineYAxis.setTextColor(Color.WHITE);
    lineXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);


    barEntries = new ArrayList<BarEntry>();
    lineEntries = new ArrayList<Entry>();

    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.statistics_timeframes, android.R.layout.simple_spinner_item);
    // Specify the layout to use when the list of choices appears
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    // Apply the adapter to the spinners
    timeframeBarSpinner.setAdapter(adapter);
    timeframeLineSpinner.setAdapter(adapter);

    timeframeBarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        // Listen for item selected in spinner. AsyncTask input is (operating mode, time, ... , time).
        switch (i) {
          case 0:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute("BAR", "-3 days", "-2 days", "-1 day");
            break;

          case 1:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "BAR", "-7 days", "-6 days", "-5 days", "-4 day", "-3 days", "-2 days", "-1 day");
            break;

            case 2:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "BAR", "-14 days", "-13 days", "-12 days", "-11 days", "-10 days", "-9 days",
                    "-8 days", "-7 days", "-6 days", "-5 days", "-4 days", "-3 days", "-2 days", "-1 day");
            break;

          case 3:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "BAR", "-28 days", "-21 days", "-14 days", "-7 days");
            break;

          case 4:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "BAR", "-3 months", "-2 months", "-1 month");
            break;

          case 5:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "BAR", "-12 months", "-11 months", "-10 months", "-9 months", "-8 months",
                    "-8 months", "-7 months", "-6 months", "-5 months", "-4 months", "-3 months",
                    "-2 months", "-1 month");
            break;
        }

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });



    timeframeLineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        switch (i) {
          case 0:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute("LINE", "-3 days", "-2 days", "-1 day");
            break;

          case 1:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "LINE", "-7 days", "-6 days", "-5 days", "-4 day", "-3 days", "-2 days", "-1 day");
            break;

          case 2:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "LINE", "-14 days", "-13 days", "-12 days", "-11 days", "-10 days", "-9 days",
                    "-8 days", "-7 days", "-6 days", "-5 days", "-4 days", "-3 days", "-2 days", "-1 day");
            break;

          case 3:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "LINE", "-28 days", "-21 days", "-14 days", "-7 days");
            break;

          case 4:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "LINE", "-3 months", "-2 months", "-1 month1");
            break;

          case 5:
            mAsyncCamerasInTimeframe = new AsyncCamerasInTimeframe();
            mAsyncCamerasInTimeframe.execute(
                    "LINE", "-12 months", "-11 months", "-10 months", "-9 months", "-8 months",
                    "-8 months", "-7 months", "-6 months", "-5 months", "-4 months", "-3 months",
                    "-2 months", "-1 month");
            break;

        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {
      }
    });


    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(StatisticsActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            Intent cameraIntent = new Intent(StatisticsActivity.this, DetectorActivity.class);
            startActivity(cameraIntent);
            return true;

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(StatisticsActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(StatisticsActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }
        return false;
      }
    });

    bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_stats).setChecked(true);
  }

  // Helper methods for querying database.
  private List<SurveillanceCamera> getCamerasInTimeframe(String sqlliteTimeStartpoint, String sqlliteTimeEndpoint) {
    return cameraDb.surveillanceCameraDao().getCamerasAddedInTimeframe(sqlliteTimeStartpoint, sqlliteTimeEndpoint);
  }

  private int getTotalCamerasUpTo(String sqliteTime) {
    return cameraDb.surveillanceCameraDao().getTotalCamerasUpTo(sqliteTime);
  }


  private class AsyncCamerasInTimeframe extends AsyncTask<String, Integer, List<Pair<Integer, Integer>>> {
    /**
     * Fetches data for graphs from database in background task.
     *
     * @param strings ("MODE", "sqltime",..., "sqltime").
     *                modes = "BAR", "LINE"
     *                sqltime see https://www.sqlite.org/lang_datefunc.html
     * @return list of xy value pairs where first Pair describes mode again:
     * (Pair(MODE, MODE), Pair(xValue0, yValue0), Pair(xValue1, yValue1), ...).
     * @see #onPreExecute()
     * @see #onPostExecute iterates through Pairs and populates graphs depending on mode.
     * @see #publishProgress
     */

    @Override
    protected List<Pair<Integer, Integer>> doInBackground(String... strings) {
      List<Pair<Integer, Integer>> outputPairs = new ArrayList<>();
      List<String> parameters = Arrays.asList(strings);

      // Fetching data depending on mode. Different modes are for different graphs.
      if (parameters.get(0).equals("BAR")) {
        outputPairs.add(Pair.create(ASYNC_BAR_MODE, ASYNC_BAR_MODE));
        // TODO Add query in CameraDao with sql COUNT operator.
        for (int j = 1; j < parameters.size(); j++) {
          if (j < parameters.size() - 1) {
            outputPairs.add(Pair.create(j, getCamerasInTimeframe(parameters.get(j), parameters.get(j + 1)).size()));
          } else {
            outputPairs.add(Pair.create(j, getCamerasInTimeframe(parameters.get(j), "-1 second").size()));

          }

        }
      } else if (parameters.get(0).equals("LINE")) {
        outputPairs.add(Pair.create(ASYNC_LINE_MODE, ASYNC_LINE_MODE));

        int currentTotalCameras = getTotalCamerasUpTo(parameters.get(1));

        for (int k = 1; k < parameters.size(); k++) {

          if (k < parameters.size() - 1) {
            outputPairs.add(Pair.create(k, currentTotalCameras + getCamerasInTimeframe(parameters.get(k), parameters.get(k + 1)).size()));
            currentTotalCameras += getCamerasInTimeframe(parameters.get(k), parameters.get(k + 1)).size();
          } else {
            outputPairs.add(Pair.create(k, currentTotalCameras + getCamerasInTimeframe(parameters.get(k), "-1 second").size()));
            currentTotalCameras += currentTotalCameras + getCamerasInTimeframe(parameters.get(k), "-1 second").size();

          }
        }
        }

      return outputPairs;
    }

    @Override
    protected void onPostExecute(List<Pair<Integer, Integer>> pairs) {
      super.onPostExecute(pairs);

      // Adds output from querying to corresponding graphs and refreshes them.
      if (pairs.get(0).first == ASYNC_BAR_MODE && pairs.get(0).second == ASYNC_BAR_MODE){
        barEntries.clear();

        for (int i = 1; i < pairs.size(); i++) {

          barEntries.add(new BarEntry(pairs.get(i).first, pairs.get(i).second));

        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        BarData barData = new BarData(barDataSet);
        barData.setValueTextColor(Color.WHITE);
        statisticsBarChart.setData(barData);
        // refresh
        statisticsBarChart.invalidate();

      } else if (pairs.get(0).first == ASYNC_LINE_MODE && pairs.get(0).second == ASYNC_LINE_MODE) {
        lineEntries.clear();

        for (int i = 1; i < pairs.size(); i++) {

          lineEntries.add(new Entry(pairs.get(i).first, pairs.get(i).second));

        }

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "");
        LineData lineData = new LineData(lineDataSet);
        lineData.setValueTextColor(Color.WHITE);
        statisticsLineChart.setData(lineData);
        // refresh
        statisticsLineChart.invalidate();

      }
    }
  }

}