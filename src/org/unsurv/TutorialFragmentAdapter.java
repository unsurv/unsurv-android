package org.unsurv;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * Adapter that handles tutorial fragments.
 */
public class TutorialFragmentAdapter extends FragmentPagerAdapter {

  private Context mContext;

  TutorialFragmentAdapter(Context context, FragmentManager fragmentManager){
    super(fragmentManager);
    mContext = context;
  }

  @NonNull
  @Override
  public Fragment getItem(int position) {

    switch (position) {
      case 0:
        return new PermissionTutorialFragment();
      case 1:
        return new RecordingTutorialFragment();
      case 2:
        return new MapTutorialFragment();
      case 3:
        return new SynchronizationTutorialFragment();

    }

    return new MapTutorialFragment();

  }


  @Override
  public int getCount() {
    return 4;
  }
}
