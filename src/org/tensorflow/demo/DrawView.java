package org.tensorflow.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.widget.ImageViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.io.File;

@SuppressLint("AppCompatCustomView")
public class DrawView extends ImageView {

  static String TAG = "DrawView";

  String mPathToImage;
  Paint mPaint;
  Rect mRect;
  //int[] mViewXYStart = new int[2];
  int mViewWidth;
  int mViewHeight;

  float imageXTouchingPercent = 0;
  float imageYTouchingPercent = 0;

  static int TOUCH_TOLERANCE = 10;

  float rectXStart = 0;
  float rectYStart = 0;

  float rectXStop = 0;
  float rectYStop = 0;

  boolean drawFinished = false;



  public DrawView(Context context, String pathToImage) {
    super(context);

    mPaint = new Paint();

    mPaint.setColor(Color.BLUE);
    mPaint.setStrokeWidth(7);
    mPaint.setStyle(Paint.Style.STROKE);

    mPathToImage = pathToImage;

    mRect = new Rect();

  }

  public DrawView(Context context, AttributeSet attr){
    super(context, attr);

  }

  public DrawView(Context context, AttributeSet attr, int number){
    super(context, attr, number);

  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    //getLocationOnScreen(mViewXYStart);
    mViewWidth = getWidth();
    mViewHeight = getHeight();

    if (rectXStop != 0 || rectYStop != 0) {
      canvas.drawRect(rectXStart, rectYStart, rectXStop, rectYStop, mPaint);
    }


  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {


    float x = event.getX();
    float y = event.getY();

    mViewWidth = getWidth();
    mViewHeight = getHeight();

    // touch from user in relation to view width / height
    imageXTouchingPercent = x / mViewWidth;
    imageYTouchingPercent = y / mViewHeight;

    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

    BitmapFactory.decodeFile(mPathToImage, bitmapOptions);

    int trueImageWidth = bitmapOptions.outWidth;
    int trueImageHeight = bitmapOptions.outHeight;


    Log.i(TAG, String.valueOf(imageXTouchingPercent));
    Log.i(TAG, String.valueOf(imageYTouchingPercent));

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        touch_start(x, y);
        invalidate();
        break;
      case MotionEvent.ACTION_MOVE:
        touch_move(x, y);
        invalidate();
        break;
      case MotionEvent.ACTION_UP:
        touch_up();
        break;
    }

    return true;

  }

  private void touch_start(float x, float y) {
    //mPath.reset();
    if (drawFinished) {

    }

    rectXStart = x;
    rectYStart = y;
  }

  private void touch_move(float x, float y) {
    float dx = x - rectXStart;
    float dy = y - rectYStart;

    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
     rectXStop = rectXStart + dx;
     rectYStop = rectYStart + dy;
    }
  }

  private void touch_up() {

    drawFinished = true;

  }
}
