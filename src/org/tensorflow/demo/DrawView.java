package org.tensorflow.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


@SuppressLint("AppCompatCustomView")
public class DrawView extends ImageView {

  // camera types
  public static int REGULAR_CAMERA = 0;
  public static int DOME_CAMERA = 1;

  static String TAG = "DrawView";

  String mPathToImage;
  Paint mPaint;

  // paint for corresponding camera types
  Paint mRegularPaint;
  Paint mDomePaint;

  // view height, width from display in display pixels
  int mViewWidth;
  int mViewHeight;

  // used reading touch input, value is percent of complete width/height
  float imageViewXTouchingPercent = 0;
  float imageViewYTouchingPercent = 0;

  // min value in pixels for drawing to trigger // TODO change for different display sizes
  static int TOUCH_TOLERANCE = 10;

  // touch start coordinates
  float rectXStart = 0;
  float rectYStart = 0;

  // touch stop coordinates
  float rectXStop = 0;
  float rectYStop = 0;

  // touch start coordinates in relation to the size of the view touched
  float rectXStartPercent = 0;
  float rectYStartPercent = 0;

  // touch stop coordinates in relation to the size of the view touched
  float rectXStopPercent = 0;
  float rectYStopPercent = 0;

  boolean touchFinished = false;
  boolean stopDrawing = false;
  boolean firstTimeLaunched;

  int cameraType;

  // actual size of jpg in pixels
  int trueImageWidth;
  int trueImageHeight;

  // drawn rect in img pixels
  Rect touchRectInImagePixels;

  CameraViewModel mCameraViewModel;
  Context mContext;

  // already saved rects as json array / string
  JSONArray rectArray;
  String drawnCameras;

  SurveillanceCamera mCamera;

  // tmp object used for drawing saved rects
  Rect rectFromDb;

  public Canvas mCanvas;


  public DrawView(Context context, SurveillanceCamera camera, CameraViewModel cameraViewModel) {
    super(context);

    firstTimeLaunched = true;

    mContext = context;

    mPaint = new Paint();
    mRegularPaint = new Paint();
    mDomePaint = new Paint();

    // orange
    mRegularPaint.setColor(Color.parseColor("#ff5555"));
    mRegularPaint.setStrokeWidth(10);
    mRegularPaint.setStyle(Paint.Style.STROKE);

    mDomePaint.setColor(Color.BLUE);
    mDomePaint.setStrokeWidth(10);
    mDomePaint.setStyle(Paint.Style.STROKE);

    mPathToImage = StorageUtils.TRAINING_CAPTURES_PATH + camera.getImagePath();
    mCameraViewModel = cameraViewModel;

    mCamera = camera;

    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

    BitmapFactory.decodeFile(mPathToImage, bitmapOptions);

    // Image is taken in portrait mode. outheight outputs as if in landscape mode, therefore we change values
    trueImageWidth = bitmapOptions.outHeight;
    trueImageHeight = bitmapOptions.outWidth;

    rectFromDb = new Rect();

    drawnCameras = mCamera.getDrawnRectsAsString();

    try {

      // populate drawnCameras if data already present
      if (!drawnCameras.isEmpty()){
        rectArray = new JSONArray(drawnCameras);
      } else {
        rectArray = new JSONArray();
      }

    } catch (JSONException e){
      Log.i(TAG, "jsonException: " + e.toString());
    }

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

    mCanvas = canvas;

    // view size in pixels, portrait mode is forced so width < height
    mViewWidth = getWidth();
    mViewHeight = getHeight();


    // set color for different types
    if (cameraType == StorageUtils.STANDARD_CAMERA){
      mPaint = mRegularPaint;
    }

    if (cameraType == StorageUtils.DOME_CAMERA){
      mPaint = mDomePaint;
    }

    if (!stopDrawing) {

      // draw one time after touch finished or when first creating view
      if (touchFinished || firstTimeLaunched){

        if (firstTimeLaunched) {
          // cant draw from touch when first launched, just draw rects already present
          firstTimeLaunched = false;
          drawnCameras = mCamera.getDrawnRectsAsString();

          // draw cameras from db by populating rectarray
          if (!drawnCameras.isEmpty()){
            try{
              rectArray = new JSONArray(drawnCameras);

            } catch (JSONException e){
              Log.i(TAG, "no drawn cameras present");
            }
          }

        } else {
          // touch event finished
          canvas.drawRect(rectXStart, rectYStart, rectXStop, rectYStop, mPaint);
        }

        drawCameras(canvas);
        stopDrawing = true;

      } else {
        // draw regularly while touching
        canvas.drawRect(rectXStart, rectYStart, rectXStop, rectYStop, mPaint);
        // TODO maybe only draw every xth touch movement to save some computaion
      }
    }


  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {

    // x / y in view pixel values, starting at 0/0 in top left of view and using display pixels from there on
    float x = event.getX();
    float y = event.getY();

    // touch from user in relation to view width / height
    imageViewXTouchingPercent = x / mViewWidth;
    imageViewYTouchingPercent = y / mViewHeight;

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

    // started drawing
    touchFinished = false;
    stopDrawing = false;

    rectXStart = x;
    rectYStart = y;

    rectXStartPercent = x / mViewWidth;
    rectYStartPercent = y / mViewHeight;

  }

  private void touch_move(float x, float y) {

    // distance moved while touching
    float dx = x - rectXStart;
    float dy = y - rectYStart;

    // refresh and draw new rect on screen if movement > tolerance
    if (Math.abs(dx) >= TOUCH_TOLERANCE || Math.abs(dy) >= TOUCH_TOLERANCE) {
     rectXStop = rectXStart + dx;
     rectYStop = rectYStart + dy;

     rectXStopPercent = rectXStop / mViewWidth;
     rectYStopPercent = rectYStop / mViewHeight;

     invalidate();
    }
  }

  private void touch_up() {

    touchFinished = true;

  }

  /**
   * given a percentage position of touch event, return img pixel equivalent
   * @param touchInPercent
   * @param imageAxisSize
   * @return
   */
  public int translateTouchPointsToImagePixel(float touchInPercent, int imageAxisSize){
    int pixelValue = (int) Math.floor(touchInPercent * imageAxisSize);

    if (pixelValue > imageAxisSize) {
      return imageAxisSize;
    } else {
      return pixelValue;
    }
  }

  /**
   * translates back from image pixels to percent of img axis size touched
   * @param imagePixel
   * @param imageAxisSize
   * @return
   */
  public float translateImagePixelToPercent(int imagePixel, int imageAxisSize){

    float percentInImage = (float) imagePixel / imageAxisSize;

    return percentInImage;
  }

  public void setCameraType(int cameraType) {
    this.cameraType = cameraType;
    invalidate();
  }

  /**
   * Called from button in DrawOnTrainingImageActivity. Saves one drawn rect to db.
   */
  public void saveCamera(){

    if (touchFinished){

      // x / y of touch start in image pixel value
      int imageXStartPixelTouched = translateTouchPointsToImagePixel(rectXStartPercent, trueImageWidth);
      int imageYStartPixelTouched = translateTouchPointsToImagePixel(rectYStartPercent, trueImageHeight);

      // x / y of touch stop in image pixel value
      int imageXStopPixelTouched = translateTouchPointsToImagePixel(rectXStopPercent, trueImageWidth);
      int imageYStopPixelTouched = translateTouchPointsToImagePixel(rectYStopPercent, trueImageHeight);


      // touch coordinates start in top left corner, therefore left is the smaller of both values etc.
      int left = returnSmallerInt(imageXStartPixelTouched, imageXStopPixelTouched);
      int top = returnSmallerInt(imageYStartPixelTouched, imageYStopPixelTouched);
      int right = returnBiggerInt(imageXStartPixelTouched, imageXStopPixelTouched);
      int bottom = returnBiggerInt(imageYStartPixelTouched, imageYStopPixelTouched);

      touchRectInImagePixels = new Rect(left, top, right, bottom);

      Log.i(TAG, "img pixel rect: " + touchRectInImagePixels.flattenToString());

      try {

        if (drawnCameras != null){
          rectArray = new JSONArray(drawnCameras);
        } else {
          rectArray = new JSONArray();
        }

        JSONObject singleDrawnRect = new JSONObject();

        // "cameraType":"left top right bottom"
        singleDrawnRect.put(String.valueOf(cameraType), touchRectInImagePixels.flattenToString());

        rectArray.put(singleDrawnRect);


      } catch (JSONException e){
        Log.i(TAG, "jsonException: " + e.toString());
      }

      // JSON as string to save in db
      drawnCameras = rectArray.toString();

      // updating obj and saving to db
      mCamera.setDrawnRectsAsString(drawnCameras);
      mCamera.setCameraType(cameraType);
      mCameraViewModel.update(mCamera);

    } else {
      Toast.makeText(mContext, "Please finish drawing before saving", Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * removes last drawn rect and updates view accordingly
   */
  public void undo(){

    rectArray.remove(rectArray.length() - 1);

    // update and save to db
    drawnCameras = rectArray.toString();
    mCamera.setDrawnRectsAsString(drawnCameras);
    // mCameraViewModel.update(mCamera);

    refresh();

  }

  int returnBiggerInt(int first, int last){
    if (first > last){
      return first;
    } else {
      return last;
    }
  }

  int returnSmallerInt(int first, int last){
    if (first < last){
      return first;
    } else {
      return last;
    }
  }

  public void refresh(){
    // Redraw as if just launched. Since updated SurveillanceCameraObject is queried again for drawing,
    // removed camera will not be shown.
    firstTimeLaunched = true;

    stopDrawing = false;

    invalidate();

  }

  private void drawCameras(Canvas canvas){

    for (int i=0; i < rectArray.length(); i++){

      try {
        JSONObject tmpObj = (JSONObject) rectArray.get(i);

        // key sets type of camera, see static ints for types
        if (tmpObj.keys().next().equals(String.valueOf(REGULAR_CAMERA))){
          mPaint = mRegularPaint;
        } else {
          mPaint = mDomePaint;
        }

        // acces value of JSONObj
        String rectAsStringWithImagePixel = tmpObj.getString(tmpObj.keys().next());

        // example data: "10 70 200 600" left top right bottom
        String[] splitRect = rectAsStringWithImagePixel.split(" ");

        // translate img pixel values in rect to view pixels on phone display here
        int left = (int) Math.floor(translateImagePixelToPercent(Integer.parseInt(splitRect[0]), trueImageWidth) * mViewWidth);
        int top = (int) Math.floor(translateImagePixelToPercent(Integer.parseInt(splitRect[1]), trueImageHeight) * mViewHeight);
        int right = (int) Math.floor(translateImagePixelToPercent(Integer.parseInt(splitRect[2]), trueImageWidth) * mViewWidth);
        int bottom = (int) Math.floor(translateImagePixelToPercent(Integer.parseInt(splitRect[3]), trueImageHeight) * mViewHeight);

        rectFromDb.left = left;
        rectFromDb.top = top;
        rectFromDb.right = right;
        rectFromDb.bottom = bottom;

        canvas.drawRect(rectFromDb, mPaint);


      } catch (JSONException e){
        Log.i(TAG, "jsonException: " + e.toString());
      }
    }
  }

}
