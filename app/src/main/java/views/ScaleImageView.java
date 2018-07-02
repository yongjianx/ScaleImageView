package views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

import com.example.skyworthclub.imagescale.R;

/**
 * Created by skyworthclub on 2018/6/30.
 */

public class ScaleImageView extends ImageView implements OnGlobalLayoutListener, View.OnTouchListener, OnScaleGestureListener {
    //双击放大与缩小
    private GestureDetector mGestureDetector;
    //多点触控放大缩小
    private ScaleGestureDetector mScaleGestureDector;
    //Matrix是一个3*3的矩阵，通过矩阵执行对图像的平移，旋转，缩放，斜切等操作
    private Matrix mMatrix;
    private boolean mIsScaleImage;
    private boolean mIsScaleToValue;

    //初始化缩放的值
    private float mInitScale;
    //双击放大到达的值
//    private float mMidScale;
    //放大到最大的值
    private float mMaxScale;

    //拖动能识别的最小距离
    private float MTouchSlop;

    //拖动时判断
    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    public ScaleImageView(Context context){
        this(context,null);
    }
    public ScaleImageView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }
    public ScaleImageView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        //设置矩阵放大缩小
        setScaleType(ScaleType.MATRIX);
        setOnTouchListener(this);

        //获取ViewConfiguration的最小滑动距离
        MTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        //获取所有的自定义属性和样式
        TypedArray typedArray = context.getTheme().obtainStyledAttributes
                (attrs, R.styleable.ScaleImageView, defStyleAttr, 0);
        //获取自定义属性的个数
        int indexCount = typedArray.getIndexCount();
        //获取相关设定的值
        for (int i=0; i<indexCount; i++){
            int attr = typedArray.getIndex(i);
            switch (attr){
                case R.styleable.ScaleImageView_isScaleImage:
                    mIsScaleImage = typedArray.getBoolean(attr, false);
                    break;
                case R.styleable.ScaleImageView_isScaleToValue:
                    mIsScaleToValue = typedArray.getBoolean(attr, false);
                    break;
                case R.styleable.ScaleImageView_maxScale:
                    mMaxScale = typedArray.getFloat(attr, 3.0f);
                    break;
                    default:
                        break;
            }
        }

        mMatrix = new Matrix();
        //多点触控放大缩小
        mScaleGestureDector = new ScaleGestureDetector(context, this);
        //双击放大缩小
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                //获取点击的当前坐标
                float x = e.getX();
                float y = e.getY();

                //获取缩放值
                float scale = getScale();
                /*
                 * 在Runnable中由于计算机储存float类型时会采用四舍五入方法，导致最终的mTarget放大缩小倍数不是完全
                 * 精确到指定的值，所以如果当前的放大缩小倍数与初始值的绝对值误差小于0.06时即认为本次的放大缩小倍数
                 * 就是初始的倍数
                 */
                if (Math.abs(scale-mInitScale) < 0.06)
                    scale = mInitScale;
                //当前缩放值大于初使的缩放值，对其进行缩小操作
                if (scale > mInitScale){
//                    mMatrix.postScale(initScale/5.0f, initScale/5.0f, x, y);
//                    setImageMatrix(mMatrix);
                    new Thread(new AutoScaleRunnable(mInitScale, x, y)).start();
                }else {
                    //当前的缩放值小于等于初始的缩放值，对其进行放大的操作
//                    mMatrix.postScale(mInitScale*5.0f, mInitScale*5.0f, x, y);
//                    setImageMatrix(mMatrix);
                    new Thread(new AutoScaleRunnable(mMaxScale, x, y)).start();
                }
                return true;
            }
        });
    }

    /**
     * 当此view附加到窗体上时调用该方法
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //注册OnGlobalLayoutListener
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * 当此view从窗体上消除时调用该方法
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //注销OnGlobalLayoutListener
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    /**
     * 获取ImageView加载完成的图片
     */
    private boolean mOnce = false;
    @Override
    public void onGlobalLayout() {
        //由于onGlobalLayout可能会被调用多次,我们使用一个标志mOnce来判断是否已经调用
        if(!mOnce){
            //获取控件宽高
            final int width = getWidth();
            final int height = getHeight();

            //得到图片，并获取宽与高
            final Drawable drawable = getDrawable();
            if (drawable == null)
                return;
            //获取图片宽高
            final int intrinsicWidth = drawable.getIntrinsicWidth();
            final int intrinsicHeight = drawable.getIntrinsicHeight();

            //设置缩放比例
            float scale = 1.0f;

            //如果图片宽度大于控件宽度，图片高度小于控件高度  图片缩小
            if(intrinsicWidth>width && intrinsicHeight<height)
                scale = intrinsicWidth*1.0f / width;
            //如果图片高度大于控件高度，图片宽度小于控件宽度  图片缩小
            if (intrinsicHeight>height && intrinsicWidth<width)
                scale = intrinsicHeight*1.0f / height;
            //如果图片的宽与高都大于控件的宽与高
            if (intrinsicHeight > height && intrinsicWidth > width)
                scale = Math.min(width * 1.0f / intrinsicWidth, height * 1.0f / intrinsicHeight);
            //如果图片的宽与高都小于控件的宽与高
            if (mIsScaleImage && (intrinsicHeight < height && intrinsicWidth < width))
                scale = Math.min(width * 1.0f / intrinsicWidth, height * 1.0f / intrinsicHeight);

            //获取初始化时图片需要进行缩放的值
            final int moveX = width/2 - intrinsicWidth/2;
            final int moveY = height/2 - intrinsicHeight/2;

            mMatrix.postTranslate(moveX, moveY);
            mMatrix.postScale(scale, scale, width/2, height/2);
            setImageMatrix(mMatrix);

            //设置默认缩放倍数
            mInitScale = scale;
            //如果没有设置指定放大倍数就设置为默认值
            if(!mIsScaleToValue){
//                mMidScale = scale*3.5f;
                mMaxScale = scale*3.0f;
            }
            mOnce = true;
        }
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        //获取当前缩放比例
        float scale = getScale();
        //获取缩放因子
        float scaleFactor = detector.getScaleFactor();

        if (getDrawable() == null)
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
        setImageMatrix(mMatrix);

        return false;
    }

    /**
     * 缩放开始时调用此方法
     * @param detector
     * @return
     */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    /**
     * 缩放结束时调用此方法
     * @param detector
     */
    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    //上一次的中心点
    private float mLastX;
    private float mLastY;
    //判断是否可拖动
    private boolean mIsCanDrag;
    //上一次触控的点数
    private int mLastPointerCount;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //将事件传递给手势检测的onTouchEvent处理
        if (mGestureDetector.onTouchEvent(event)){
            return true;
        }
        //多点触控放大缩小
        mScaleGestureDector.onTouchEvent(event);

        float x = 0;
        float y = 0;
        //触摸点的个数
        int pointerCount = event.getPointerCount();
        Log.e("触控的点数when Touch：", pointerCount+" "+ mLastPointerCount);
        for (int i=0; i<pointerCount; i++){
            x += event.getX(i);
            y += event.getY(i);
        }
        //得到多个触摸点的x与y均值
        x = x / pointerCount;
        y = y / pointerCount;

        if (mLastPointerCount != pointerCount) {
            mIsCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;

        RectF rectF = getMatrixRectF();
        /**
         * 此View在ViewPager中使用时,图片放大后自由移动的事件会与
         * ViewPager的左右切换的事件发生冲突,导致图片放大后如果左右
         * 移动时不能自由移动图片,而是使ViewPager切换图片.这是由于事
         * 件分发时外层的优先级比内层的高,使用下列判断可以解决
         */
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (rectF.width()>getWidth() || rectF.height() > getHeight())
                    getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (rectF.width()>getWidth() || rectF.height() > getHeight())
                    getParent().requestDisallowInterceptTouchEvent(true);

                //偏移量
                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!mIsCanDrag)
                    mIsCanDrag = isMoveAction(dx, dy);
                if (mIsCanDrag){
                    if (getDrawable() != null){
                        isCheckLeftAndRight = true;
                        isCheckTopAndBottom = true;

//                        //如果宽度小于控件的宽度,不允许横向移动
//                        if(rectF.width() < getWidth()){
//                            isCheckLeftAndRight = false;
//                            dx = 0;
//                        }
//                        //如果高度小于控件的高度,不允许纵向移动
//                        if (rectF.height() < getHeight()){
//                            isCheckTopAndBottom = false;
//                            dy = 0;
//                        }

                        mMatrix.postTranslate(dx, dy);
                        checkBorderAndCenter();
                        setImageMatrix(mMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_UP:
                mLastPointerCount = 0;
                Log.e("触控的点数Action_up：", " "+ mLastPointerCount);
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

    //当自由移动时进行边界检查,防止留白
    private void checkBorderWhenTranslate() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectF.top > 0 && isCheckTopAndBottom) {
            deltaY = -rectF.top;
        }
        if (rectF.bottom < height && isCheckTopAndBottom) {
            deltaY = height - rectF.bottom;
        }
        if (rectF.left > 0 && isCheckLeftAndRight) {
            deltaX = -rectF.left;
        }
        if (rectF.right < width && isCheckLeftAndRight) {
            deltaX = width -rectF.right;
        }

        mMatrix.postTranslate(deltaX,deltaY);
    }

    /**
     * @return 获取当前图片的缩放值
     */
    public float getScale(){
        final float[] floats = new float[9];
        mMatrix.getValues(floats);
        //获取当前的x方向缩放值
        return floats[mMatrix.MSCALE_X];
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
            setImageMatrix(mMatrix);

            //获取当前缩放值
            float currentScale = getScale();
            if ((mTempScale>1.0&&currentScale<mTargetScale) || (mTempScale<1.0&&currentScale>mTargetScale)){
                postDelayed(this, 16);
            }else{
                float scale = mTargetScale / currentScale;
                Log.e("未正常：", " "+getScale()+" "+scale);
                mMatrix.postScale(scale, scale, mX, mY);
                checkBorderAndCenter();
                setImageMatrix(mMatrix);
                Log.e("Runnanle:", "exit!");
            }
        }
    }


    //进行边界控制的逻辑
    private void checkBorderAndCenter() {
        //得到放大或者缩小后的图片的宽与高
        final RectF matrixRectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;
        //得到控件的宽与高
        final int width = getWidth();
        final int height = getHeight();

        if (matrixRectF.width() >= width) {
            //当图片距控件左边的距离left>0那说明图片距离左边出现的空白，需要将图片向左面移动
            if (matrixRectF.left > 0) {
                deltaX = -matrixRectF.left;
            }
            //当图片距控件右边的距离right小于控件的width的时候，说明图片距离控件的右面会出现空白，需要将图片向右面移动
            if (matrixRectF.right < width) {
                deltaX = width - matrixRectF.right;
            }
        }

        if (matrixRectF.height() >= height) {
            if (matrixRectF.top > 0) {
                deltaY = -matrixRectF.top;
            }
            if (matrixRectF.bottom < height) {
                deltaY = height - matrixRectF.bottom;
            }
        }

        //如果宽度与高度小于控件的宽与高，则让图片居中
        if (matrixRectF.width() < width) {
            deltaX = width / 2 - matrixRectF.left - matrixRectF.width() / 2;
        }
        if (matrixRectF.height() < height) {
            deltaY = height / 2 - matrixRectF.top - matrixRectF.height() / 2;
        }

        mMatrix.postTranslate(deltaX, deltaY);
    }

    //获得图片缩放后的宽高,以及top,bottom,left,right
    private RectF getMatrixRectF(){
        Matrix matrix = mMatrix;
        RectF rectf = new RectF();
        Drawable d = getDrawable();
        if (d != null) {
            rectf.set(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
            matrix.mapRect(rectf);
        }
        return rectf;
    }

}
