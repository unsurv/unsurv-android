package org.tensorflow.demo;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;


import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.IconOverlay;

public class BottomAnchorIconOverlay extends org.osmdroid.views.overlay.IconOverlay {

  /** save to be called in non-gui-thread */
  public BottomAnchorIconOverlay() {
  }

  /** save to be called in non-gui-thread */
  BottomAnchorIconOverlay(IGeoPoint position, Drawable icon) {
    set(position, icon);
  }


  /**
   * Draw the icon.
   *
   * Adapted so bottom edge of icon aligns with position (Markers with a bottom shape like "V" point on the exact postion)
   */
  @Override
  public void draw(Canvas canvas, Projection pj) {
    if (mIcon == null)
      return;
    if (mPosition == null)
      return;

    //float newAnchorU = IconOverlay.ANCHOR_BOTTOM;
    float newAnchorV = IconOverlay.ANCHOR_BOTTOM;

    pj.toPixels(mPosition, mPositionPixels);
    int width = mIcon.getIntrinsicWidth();
    int height = mIcon.getIntrinsicHeight();
    Rect rect = new Rect(0, 0, width, height);
    rect.offset(-(int)(mAnchorU*width), -(int)(newAnchorV*height));
    mIcon.setBounds(rect);

    mIcon.setAlpha((int) (mAlpha * 255));

    float rotationOnScreen = (mFlat ? -mBearing : pj.getOrientation()-mBearing);
    drawAt(canvas, mIcon, mPositionPixels.x, mPositionPixels.y, false, rotationOnScreen);
  }

}
