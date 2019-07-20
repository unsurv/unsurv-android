package org.tensorflow.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Helper for setting and removing badges from BottomNavigationView.
 * Badges function as a notification if new cameras are available.
 * They get removed once the corresponding activity is launched.
 */

class BottomNavigationBadgeHelper {


  static void setBadges(BottomNavigationView bottomNavigationView, Context context, int historyBadgeCount, int mapBadgeCount){

    SharedPreferences sharedPreferences;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    sharedPreferences.edit().putInt("bottomNavigationHistoryBadgeCount", historyBadgeCount).apply();
    sharedPreferences.edit().putInt("bottomNavigationMapBadgeCount", mapBadgeCount).apply();

    if (historyBadgeCount == 0){
      bottomNavigationView.removeBadge(R.id.bottom_navigation_history);
    } else {
      bottomNavigationView.showBadge(R.id.bottom_navigation_history).setNumber(historyBadgeCount);
    }

    if (mapBadgeCount == 0){
      bottomNavigationView.removeBadge(R.id.bottom_navigation_map);
    } else {
      bottomNavigationView.showBadge(R.id.bottom_navigation_map).setNumber(mapBadgeCount);
    }

  }

  static void setHistoryBadge(BottomNavigationView bottomNavigationView, Context context, int historyBadgeCount){

    SharedPreferences sharedPreferences;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    sharedPreferences.edit().putInt("bottomNavigationHistoryBadgeCount", historyBadgeCount).apply();

    if (historyBadgeCount == 0){
      bottomNavigationView.removeBadge(R.id.bottom_navigation_history);
    } else {
      bottomNavigationView.showBadge(R.id.bottom_navigation_history).setNumber(historyBadgeCount);
    }

  }


  static void setMapBadge(BottomNavigationView bottomNavigationView, Context context, int mapBadgeCount){

    SharedPreferences sharedPreferences;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    sharedPreferences.edit().putInt("bottomNavigationMapBadgeCount", mapBadgeCount).apply();

    if (mapBadgeCount == 0){
      bottomNavigationView.removeBadge(R.id.bottom_navigation_map);
    } else {
      bottomNavigationView.showBadge(R.id.bottom_navigation_map).setNumber(mapBadgeCount);
    }

  }



  static void setBadgesFromSharedPreferences(BottomNavigationView bottomNavigationView, Context context){

    SharedPreferences sharedPreferences;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    int historyCount = sharedPreferences.getInt("bottomNavigationHistoryBadgeCount", 0);
    int mapCount = sharedPreferences.getInt("bottomNavigationMapBadgeCount", 0);

    if (historyCount != 0){
      bottomNavigationView.showBadge(R.id.bottom_navigation_history).setNumber(historyCount);
    } else {
      bottomNavigationView.removeBadge(R.id.bottom_navigation_history);
    }

    if (mapCount != 0){
      bottomNavigationView.showBadge(R.id.bottom_navigation_map).setNumber(mapCount);
    } else {
      bottomNavigationView.removeBadge(R.id.bottom_navigation_map);
    }

  }

  static void incrementBadge(BottomNavigationView bottomNavigationView, Context context, int menuItemId, int amount){

    SharedPreferences sharedPreferences;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    int badgeCount = 0;

    switch (menuItemId){

      case R.id.bottom_navigation_history:

        badgeCount = sharedPreferences.getInt("bottomNavigationHistoryBadgeCount", 0);
        badgeCount += amount;
        sharedPreferences.edit().putInt("bottomNavigationHistoryBadgeCount", badgeCount).apply();

        break;


      case R.id.bottom_navigation_map:

        badgeCount = sharedPreferences.getInt("bottomNavigationMapBadgeCount", 0);
        badgeCount += amount;
        sharedPreferences.edit().putInt("bottomNavigationMapBadgeCount", badgeCount).apply();

        break;
    }

    bottomNavigationView.removeBadge(menuItemId);

    if (badgeCount != 0){
      bottomNavigationView.showBadge(menuItemId).setNumber(badgeCount);
    }

  }

  static void clearMenuItemBadge(BottomNavigationView bottomNavigationView, int menuItemId, Context context){

    SharedPreferences sharedPreferences;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    switch (menuItemId){

      case R.id.bottom_navigation_history:
        sharedPreferences.edit().putInt("bottomNavigationHistoryBadgeCount", 0).apply();
        break;

      case R.id.bottom_navigation_map:
        sharedPreferences.edit().putInt("bottomNavigationMapBadgeCount", 0).apply();
        break;
    }

    bottomNavigationView.removeBadge(menuItemId);
  }



  static void clearAllBadges(BottomNavigationView bottomNavigationView){

    int[] bottomNavigationIds = new int[]{
            R.id.bottom_navigation_history,
            R.id.bottom_navigation_camera,
            R.id.bottom_navigation_map,
            R.id.bottom_navigation_stats};


    for (int id : bottomNavigationIds){

      bottomNavigationView.removeBadge(id);

    }

  }

}
