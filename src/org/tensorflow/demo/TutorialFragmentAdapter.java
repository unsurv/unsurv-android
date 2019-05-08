package org.tensorflow.demo;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class TutorialFragmentAdapter extends FragmentPagerAdapter {

  private Context mContext;

  TutorialFragmentAdapter(Context context, FragmentManager fragmentManager){
    super(fragmentManager);
    mContext = context;
  }

  @Override
  public Fragment getItem(int position) {

    switch (position) {
      case 0:
        return new MapTutorialFragment();
      case 1:
        return new RecordingTutorialFragment();
      case 2:
        return new SynchronizationTutorialFragment();

    }

    return null;

  }


  @Override
  public int getCount() {
    return 3;
  }
}
