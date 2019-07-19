package org.tensorflow.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationNotificationHelper {


  static void setBottomNaviagtionNotifications(BottomNavigationView bottomNavigationView, Context context, int historyBadgeCount, int mapBadgeCount){

    SharedPreferences sharedPreferences;
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    sharedPreferences.edit().putInt("bottomNavigationHistoryBadgeCount", historyBadgeCount).apply();
    sharedPreferences.edit().putInt("bottomNavigationMapBadgeCount", mapBadgeCount).apply();

    bottomNavigationView.showBadge(R.id.bottom_navigation_history).setNumber(historyBadgeCount);
    bottomNavigationView.showBadge(R.id.bottom_navigation_map).setNumber(mapBadgeCount);

  }

  static void incrementBadge(BottomNavigationView bottomNavigationView, int menuItemId, int amount){

    int current;

    try {
       current = bottomNavigationView.getBadge(menuItemId).getNumber();

    } catch (NullPointerException e){
      current = 0;
    }

    bottomNavigationView.removeBadge(menuItemId);

    current += amount;

    bottomNavigationView.showBadge(menuItemId).setNumber(current);

  }

  static void clearMenuItemBadge(BottomNavigationView bottomNavigationView, int menuItemId){
    bottomNavigationView.removeBadge(menuItemId);
  }



  static void clearAllBottomNavigationNotifications(BottomNavigationView bottomNavigationView){

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
