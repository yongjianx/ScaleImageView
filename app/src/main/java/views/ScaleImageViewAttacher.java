package views;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

/**
 * Created by skyworthclub on 2018/7/17.
 */

public class ScaleImageViewAttacher implements View.OnTouchListener{
    private static final String TAG = "ScaleImageViewAttacher";

    private static final float DEFAULT_MAX_SCALE = 3.0f;
    private static final float DEFAULT_MID_SCALE = 1.75f;
    private static final float DEFAULT_MIN_SCALE = 1.0f;
    private static final int DEFAULT_ZOOM_DURATION = 200;

    //默认缩放的值
    private float mInitScale = DEFAULT_MIN_SCALE;
    //双击放大的中间值
    private float mMidScale = DEFAULT_MID_SCALE;
    //放大到最大的值
    private float mMaxScale = DEFAULT_MAX_SCALE;
    //缩放动画持续时间
    private int mZoomDuration = DEFAULT_ZOOM_DURATION;

    //拖动能识别的最小距离
    private float MTouchSlop;
    //拖动时判断
    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    //Matrix是一个3*3的矩阵，通过矩阵执行对图像的平移，旋转，缩放，斜切等操作
    private Matrix mMatrix;
    private ImageView mImageView;

    //自定义属性的值
    private boolean mIsScaleImage;
    private boolean mIsScaleToValue;

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0)
                mImageView.setImageMatrix(mMatrix);
        }
    };

    /**
     * 公共构造器
     * @param imageView
     */
    public ScaleImageViewAttacher(ImageView imageView){
        mImageView = imageView;
        mImageView.setOnTouchListener(this);
        mMatrix = new Matrix();
        //获取ViewConfiguration的最小滑动距离
        MTouchSlop = ViewConfiguration.get(imageView.getContext()).getScaledTouchSlop();

        //双击放大与缩小
        mGestureDetector = new GestureDetector(mImageView.getContext(), new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                //获取点击的当前坐标
                float x = e.getX();
                float y = e.getY();

                //获取当前缩放值
                float scale = getScale();
                Log.e(TAG, scale+" ");
                /*
                 * 在Runnable中由于计算机储存float类型时会采用四舍五入方法，导致最终的mTarget放大缩小倍数不是完全
                 * 精确到指定的值，所以如果当前的放大缩小倍数与初始值的绝对值误差小于0.06时即认为本次的放大缩小倍数
                 * 就是初始的倍数
                 */
                if (Math.abs(scale-mInitScale) < 0.06)
                    scale = mInitScale;
                //当前缩放值大于初使的缩放值，对其进行缩小操作
                if (scale > mInitScale){
//                    mMatrix.setScale(mInitScale, mInitScale, x, y);
//                    mImageView.setImageMatrix(mMatrix);
                    new Thread(new AutoScaleRunnable(mInitScale, x, y)).start();
                }else {
                    //当前的缩放值小于等于初始的缩放值，对其进行放大的操作
//                    mMatrix.setScale(mMaxScale, mMaxScale, x, y);
//                    mImageView.setImageMatrix(mMatrix);
                    new Thread(new AutoScaleRunnable(mMaxScale, x, y)).start();
                }
                return true;
            }
        });

        mScaleGestureDetector = new ScaleGestureDetector(mImageView.getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Log.e(TAG, "onScale: come in..");
                //获取当前缩放比例
                float scale = getScale();
                //获取缩放因子
                float scaleFactor = detector.getScaleFactor();

                if (mImageView.getDrawable() == null)
                    return true;
                if ((scale<mMaxScale && scaleFactor>1.0f) || (scale>mInitScale && scaleFactor<1.0f)){
                    if (scale*scaleFactor < mInitScale)
                        scaleFactor = mInitScale / scale;
                    if (scale*scaleFactor > mMaxScale)
                        scaleFactor = mMaxScale / scale;
                }

                //缩放
                mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                checkBorderAndCenter();
                mImageView.setImageMatrix(mMatrix);

                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });

    }



    //上一次触碰的中心点
    private float mLastX;
    private float mLastY;
    //上一次触控的点数
    private int mLastPointerCount;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //将事件传递给手势检测的onTouchEvent处理
        if (mGestureDetector.onTouchEvent(event)){
            return true;
        }
        mScaleGestureDetector.onTouchEvent(event);

        float x = 0;
        float y = 0;
        //触摸点的个数
        int pointerCount = event.getPointerCount();
        for (int i=0; i<pointerCount; i++){
            x += event.getX(i);
            y += event.getY(i);
        }
        //得到多个触摸点的x与y均值
        x = x / pointerCount;
        y = y / pointerCount;
        if (mLastPointerCount != pointerCount) {
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                //父控件不拦截TouchEcent事件
                mImageView.getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_MOVE:
                //偏移量
                float dx = x - mLastX;
                float dy = y - mLastY;

                if (isMoveAction(dx, dy)){
                    if (mImageView.getDrawable() != null){

                        mMatrix.postTranslate(dx, dy);
                        checkBorderAndCenter();
                        mImageView.setImageMatrix(mMatrix);
                    }
                }

                mLastX = x;
                mLastY = y;
                break;

            case MotionEvent.ACTION_UP:
                mLastPointerCount = 0;

                break;
        }
        return true;
    }

    /**
     * @param dx x方向偏移量
     * @param dy
     * @return
     */
    //判断是否足以触发MOVE事件
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx * dx + dy * dy) > MTouchSlop;
    }

    /**
     * @return 获取当前图片的缩放值
     */
    private float getScale(){
        final float[] floats = new float[9];
        mMatrix.getValues(floats);
        //获取当前的x方向缩放值
        return floats[mMatrix.MSCALE_X];
    }

    /**
     * 检查图片边界是否超出控件边界
     * @return
     */
    private boolean checkBorder(){
        //得到放大或者缩小后的图片的宽与高
        final RectF matrixRectF = getMatrixRectF();
        //得到控件的宽与高
        final int width = mImageView.getWidth();
        final int height = mImageView.getHeight();

        if (matrixRectF.width() >= width) {
            //当图片距控件左边的距离left>0那说明图片距离左边出现的空白
            if (matrixRectF.left > 0) {
                return true;
            }
            //当图片距控件右边的距离right小于控件的width的时候，说明图片距离控件的右面会出现空白，需要将图片向右面移动
            if (matrixRectF.right < width) {
                return true;
            }
        }

        if (matrixRectF.height() >= height) {
            if (matrixRectF.top > 0) {
                return true;
            }
            if (matrixRectF.bottom < height) {
                return true;
            }
        }

        return false;
    }

    //进行边界控制的逻辑
    private void checkBorderAndCenter() {
        float deltaX = getDelta(0);
        float deltaY = getDelta(1);

        mMatrix.postTranslate(deltaX, deltaY);
    }

    private float getDelta(int flag){
        //得到放大或者缩小后的图片的宽与高
        final RectF matrixRectF = getMatrixRectF();
        float delta = 0;
        //得到控件的宽与高
        final int width = mImageView.getWidth();
        final int height = mImageView.getHeight();
        switch (flag){
            case 0:
                if (matrixRectF.width() >= width) {
                    //当图片距控件左边的距离left>0那说明图片距离左边出现的空白，需要将图片向左面移动
                    if (matrixRectF.left > 0) {
                        delta = -matrixRectF.left;
                    }
                    //当图片距控件右边的距离right小于控件的width的时候，说明图片距离控件的右面会出现空白，需要将图片向右面移动
                    if (matrixRectF.right < width) {
                        delta = width - matrixRectF.right;
                    }
                }
                else
                    delta = width / 2 - matrixRectF.left - matrixRectF.width() / 2;
                break;

            case 1:
                if (matrixRectF.height() >= height) {
                    if (matrixRectF.top > 0) {
                        delta = -matrixRectF.top;
                    }
                    if (matrixRectF.bottom < height) {
                        delta = height - matrixRectF.bottom;
                    }
                }
                else
                    delta = height / 2 - matrixRectF.top - matrixRectF.height() / 2;
                break;

            default:
                 break;
        }

        return delta;
    }

    //获得图片缩放后的宽高,以及top,bottom,left,right
    private RectF getMatrixRectF(){
        Matrix matrix = mMatrix;
        RectF rectf = new RectF();
        Drawable d = mImageView.getDrawable();
        if (d != null) {
            rectf.set(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
            //将matrix矩阵表示的图片转换为用top,left,bottom,right的矩形表示
            matrix.mapRect(rectf);
        }
        return rectf;
    }

    public class AutoScaleRunnable implements Runnable {
        //目标缩放倍数
        private float mTargetScale;
        //缩放的中心点
        private float mX;
        private float mY;
        //缩放的梯度
        private static final float BIGGET = 1.07F;
        private static final float SMALL = 0.93F;
        private float mTempScale;

        /**
         * @param mTargetScale
         * @param x
         * @param y
         */
        public AutoScaleRunnable(float mTargetScale, float x, float y){
            this.mTargetScale = mTargetScale;
            mX = x;
            mY = y;
            //依据当前的缩放值设置将要进行的缩放比例操作
            if (getScale() < mTargetScale)
                mTempScale = BIGGET;
            else
                mTempScale = SMALL;
        }

        @Override
        public void run() {
            mMatrix.postScale(mTempScale, mTempScale, mX, mY);
            checkBorderAndCenter();
            mHandler.sendEmptyMessage(0);
//            mImageView.setImageMatrix(mMatrix);

            //获取当前缩放值
            float currentScale = getScale();
            if ((mTempScale>1.0&&currentScale<mTargetScale) || (mTempScale<1.0&&currentScale>mTargetScale)){
                mImageView.postDelayed(this, 10);
            }else{
                float scale = mTargetScale / currentScale;
                mMatrix.postScale(scale, scale, mX, mY);
                checkBorderAndCenter();
                mHandler.sendEmptyMessage(0);
//                mImageView.setImageMatrix(mMatrix);
                Log.e(TAG, "AutoScaleRunnabel end...");
            }
        }
    }

    public class AutoTranslateRunnable implements Runnable{
        private float mTargetX, mTargetY;
        private float tempX = 2f;
        private float tempY = 2f;

        public AutoTranslateRunnable(float targetX, float targetY){
            mTargetX = targetX;
            mTargetY = targetY;
            if (targetX < 0)
                tempX = -tempX;
            if (targetY < 0)
                tempY = -tempY;
        }

        @Override
        public void run() {
            mMatrix.postTranslate(tempX, tempY);
            mImageView.setImageMatrix(mMatrix);
            if (checkBorder())
                mImageView.postDelayed(this, 1);
            Log.e(TAG, "AutoTranslateRunnabe end...");
        }
    }
}
