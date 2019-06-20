package org.tensorflow.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;


@SuppressLint("AppCompatCustomView")
public class DrawView extends ImageView {

  public static int REGULAR_CAMERA = 0;
  public static int DOME_CAMERA = 1;

  static String TAG = "DrawView";

  String mPathToImage;
  Paint mPaint;
  int mViewWidth;
  int mViewHeight;

  float imageXTouchingPercent = 0;
  float imageYTouchingPercent = 0;

  static int TOUCH_TOLERANCE = 10;

  // touch start coordinates
  float rectXStart = 0;
  float rectYStart = 0;

  // touch stop coordinates
  float rectXStop = 0;
  float rectYStop = 0;

  boolean touchFinished = false;
  boolean stopDrawing = false;



  public DrawView(Context context, String pathToImage, int cameraType) {
    super(context);

    mPaint = new Paint();

    if (cameraType == REGULAR_CAMERA){
      mPaint.setColor(Color.GREEN);
      mPaint.setStrokeWidth(10);
      mPaint.setStyle(Paint.Style.STROKE);
    }

    if (cameraType == DOME_CAMERA){
      mPaint.setColor(Color.BLUE);
      mPaint.setStrokeWidth(10);
      mPaint.setStyle(Paint.Style.STROKE);
    }



    mPathToImage = pathToImage;

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

    mViewWidth = getWidth();
    mViewHeight = getHeight();

    if (!stopDrawing) {

      // draw one last time after touch finished
      if (touchFinished){
        canvas.drawRect(rectXStart, rectYStart, rectXStop, rectYStop, mPaint);
        stopDrawing = true;

        // reset starting points
        rectXStart = 0;
        rectYStart = 0;

        rectXStop = 0;
        rectYStop = 0;

      } else {
        // draw regularly while touching
        canvas.drawRect(rectXStart, rectYStart, rectXStop, rectYStop, mPaint);
      }
    }


  }

  // TODO intent.putextra with file path to start activity

  // TODO add 2 buttons in activity (regular / dome) + 3rd save button, one is always active, if other button is pressed:
  // TODO remove drawview, create new drawview from db values with picture + rect of different color

  // TODO use percentage * true size to get pixel value for db storage

  // TODO in drawactivity setup surveillance camera obj with array of int values for cameras for each camera class
  // TODO in give drawview already marked cameras in constructor, draw rect in corresponding colors
  // TODO in drawactivity create method for touching rect (truepixelToTouchxyConversion method needed) and undo button to delete rects, update db after

  @Override
  public boolean onTouchEvent(MotionEvent event) {


    float x = event.getX();
    float y = event.getY();

    // touch from user in relation to view width / height
    imageXTouchingPercent = x / mViewWidth;
    imageYTouchingPercent = y / mViewHeight;

    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

    BitmapFactory.decodeFile(mPathToImage, bitmapOptions);

    int trueImageWidth = bitmapOptions.outWidth;
    int trueImageHeight = bitmapOptions.outHeight;


    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        touch_start(x, y);
        break;
      case MotionEvent.ACTION_MOVE:
        touch_move(x, y);
        break;
      case MotionEvent.ACTION_UP:
        touch_up();
        break;
    }

    return true;

  }

  private void touch_start(float x, float y) {

    touchFinished = false;
    stopDrawing = false;

    rectXStart = x;
    rectYStart = y;
  }

  private void touch_move(float x, float y) {
    float dx = x - rectXStart;
    float dy = y - rectYStart;

    if (Math.abs(dx) >= TOUCH_TOLERANCE || Math.abs(dy) >= TOUCH_TOLERANCE) {
     rectXStop = rectXStart + dx;
     rectYStop = rectYStart + dy;
     invalidate();
    }
  }

  private void touch_up() {

    touchFinished = true;

  }
}
