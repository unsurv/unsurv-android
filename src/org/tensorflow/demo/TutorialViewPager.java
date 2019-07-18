package org.tensorflow.demo;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class TutorialViewPager extends ViewPager {

  private boolean fragmentScrollingEnabled;

  public TutorialViewPager (Context context, AttributeSet attributeSet) {

    super(context, attributeSet);
    this.fragmentScrollingEnabled = true;

  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {

    if (fragmentScrollingEnabled) {
      return super.onTouchEvent(ev);
    }

    return false;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (fragmentScrollingEnabled) {
      return super.onInterceptTouchEvent(ev);
    }

    return false;
  }


  public void setFragmentScrollingEnabled(boolean scrolling) {
    this.fragmentScrollingEnabled = scrolling;
  }


}
