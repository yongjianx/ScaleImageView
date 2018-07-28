package utils;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.OverScroller;

import listeners.OnGestureListener;

/**
 * Created by skyworthclub on 2018/7/17.
 */

public class ScaleImageViewAttacher implements View.OnTouchListener, View.OnLayoutChangeListener{
    private static final String TAG = "ScaleImageViewAttacher";

    //双击缩放系数
//    private static final float DEFAULT_MIN_SCALE = 1.0f;
//    private static final float DEFAULT_MID_SCALE = 1.75f;
//    private static final float DEFAULT_MAX_SCALE = 3.0f;
    //多点触控缩放时默认的最小最大系数
//    private static final float DEFAULT_MIN_OVERSTEP = 0.3f;
//    private static final float DEFAULT_MAX_OVERSTEP = 4.0f;
    //双击缩放时持续时间
    private static final int DEFAULT_ZOOM_DURATION = 200;
    //超出边界时平移恢复持续时间
    private static final int DEFAULT_TRANSLATE_DURATION = 100;

    private static final int EDGE_NONE = -1;//图片两边都不在边缘内
    private static final int EDGE_LEFT = 0;//图片左边显示在View的左边缘内
    private static final int EDGE_RIGHT = 1;//图片右边显示在View的右边缘内
    private static final int EDGE_BOTH = 2;//图片两边都在边缘内
    private int mScrollEdge = EDGE_BOTH;//两边边缘

    //默认缩放的值
    private float mInitScale = 0;
    //双击放大的中间值
    private float mMidScale = 0;
    //放大到最大的值
    private float mMaxScale = 0;
    //多点触控缩放时默认的最小最大系数
    private float mMinOverstep = 0;
    private float mMaxOVerstep = 0;

    //缩放动画持续时间
    private int mZoomDuration = DEFAULT_ZOOM_DURATION;
    //图片被拖动超出x方向边界，MOTIONEVENT.ACTION_UP事件图片回弹的持续时间
    private int mTranslateDuration = DEFAULT_TRANSLATE_DURATION;

    //Matrix是一个3*3的矩阵，通过矩阵执行对图像的平移，旋转，缩放，斜切等操作
    private Matrix mMatrix;
    private ImageView mImageView;

    //点击，长按事件监听器
    private View.OnClickListener mOnClickListener;
    private View.OnLongClickListener mOnLongClickListener;

    //父控件为ViewPager
    private boolean mIsViewPager;
//    private int viewPagerPosition;//如果父容器是ViewPager,viewPagerPosition为viewPager页面索引

    //快速滑动事件
    private AutoFlingRunnable mAutoFlingRunnable;

    //双击手势检测
    private GestureDetector mGestureDetector;
    //拖拽、快速滑动、多点触控缩放手势检测
    private OnGestureDetector mOnGeatureDetector;

    private OnGestureListener mOnGestureListner = new OnGestureListener() {
        @Override
        public void onDrag(float dx, float dy) {
            Log.e(TAG,"onDrag()...");

            boolean flag = true;//true，表示checkBorder(flag)只检查y方向的边界
            if (mIsViewPager){//表示当前父容器为ViewPager
                if (mScrollEdge == EDGE_BOTH || (mScrollEdge == EDGE_LEFT && dx>1f) ||
                        (mScrollEdge == EDGE_RIGHT && dx<-1f)){
                    mImageView.getParent().requestDisallowInterceptTouchEvent(false);
                    return;
                }
                else {
                    mImageView.getParent().requestDisallowInterceptTouchEvent(true);
                }
                flag = false;//false，当前父容器为ViewPager,表示checkBorder(flag)同时检查x,y方向的边界
            }

            mMatrix.postTranslate(dx, dy);
            if (checkBorder(flag))//边界检查
                mImageView.setImageMatrix(mMatrix);
//            checkBorderAndCenter();
        }

        @Override
        public void onFling(float startX, float startY, float velocityX, float velocityY) {

            Log.e(TAG, "onFling()...");
            if (mAutoFlingRunnable == null)
                mAutoFlingRunnable = new AutoFlingRunnable(mImageView.getContext());

            mAutoFlingRunnable.fling(getImageViewWidth(), getImageViewHeight(), (int) velocityX, (int) velocityY);
            new Thread(mAutoFlingRunnable).start();
        }

        @Override
        public void onScale(float scaleFactor, float focusX, float focusY) {
            Log.e(TAG, "onScale()...");
            if (mImageView.getDrawable() == null)
                return;

            final float scale = getScale();
            //多点触控缩放时，条件一：还能继续放大；条件二：还能继续缩小
            if ((scale< mMaxOVerstep && scaleFactor>1.0f) || (scale>mMinOverstep && scaleFactor<1.0f)){
                mMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                checkBorderAndCenter();
            }
        }
    };

    //UI线程更新ImageView的显示
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0:
                    mImageView.setImageMatrix(mMatrix);
                    break;
                    default:
                        break;
            }
        }
    };

    /**
     * 公共构造器
     * @param imageView
     */
    public ScaleImageViewAttacher(ImageView imageView){
        mImageView = imageView;
        mImageView.setScaleType(ImageView.ScaleType.MATRIX);//设置ImageView的缩放类型为Matrix
        mImageView.setOnTouchListener(this);
        mImageView.addOnLayoutChangeListener(this);
        mMatrix = new Matrix();

        //拖动，多点触控缩放
        mOnGeatureDetector = new OnGestureDetector(mImageView.getContext(), mOnGestureListner);
        //长按、单击、双击
        mGestureDetector = new GestureDetector(mImageView.getContext(), new GestureDetector.SimpleOnGestureListener(){
            @Override
            public void onLongPress(MotionEvent e) {
                Log.e(TAG, "onLongPress()...");
                if (mOnLongClickListener != null)
                    mOnLongClickListener.onLongClick(mImageView);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.e(TAG, "onSingleTapConfirmed()...");
                if (mOnClickListener != null)
                    mOnClickListener.onClick(mImageView);
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.e(TAG, "onDoubleTap()...");
                //获取点击的当前坐标
                float x = e.getX();
                float y = e.getY();

                //获取当前缩放值
                float scale = getScale();
                /*
                 * 由于计算机储存float类型时会采用四舍五入方法，导致最终的放大缩小倍数不是完全精确到指定的值，
                 * 所以如果当前的放大缩小倍数与初始值的绝对值误差小于0.02时即认为本次的放大缩小倍数就是初始的倍数
                 */
                if (Math.abs(scale - mInitScale) < 0.02)
                    scale = mInitScale;

                //当前缩放值大于初使的缩放值，对其进行缩小操作
                if (scale > mInitScale){
                    new Thread(new AutoScaleRunnable(x, y, getScale(), mInitScale)).start();
                }else {
                    //当前的缩放值小于等于初始的缩放值，对其进行放大的操作
                    new Thread(new AutoScaleRunnable(x, y, getScale(), mMaxScale)).start();
                }
                return true;
            }
        });
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            Log.e(TAG, "onLayouChange()");
           initScale();
           //根据initScale()计算出来的mInitScale初始化默认的mMidScale，mMaxScale，mMinOverstep，mMaxOVerstep
           if (mMidScale == 0)
               mMidScale = (float) 1.5*mInitScale;
           if (mMaxScale == 0)
               mMaxScale = 3*mInitScale;
           if (mMaxOVerstep == 0)
               mMaxOVerstep = 4*mInitScale;
           if (mMinOverstep == 0)
               mMinOverstep = (float) 0.5*mInitScale;
        }
    }

    /**
     * 初始化图片的缩放系数
     */
    public void initScale(){
        Drawable drawable = mImageView.getDrawable();
        if (drawable == null) {
            return;
        }

        final float width = getImageViewWidth();//控件宽高
        final float height = getImageViewHeight();

        final float intrinsicWidth = drawable.getIntrinsicWidth();//获取drawable固有宽高
        final float intrinsicHeight = drawable.getIntrinsicHeight();

        Log.e(TAG,"initScale()  mInitScale="+mInitScale);
        if (mInitScale == 0) {//没有通过setInitScale()方法设置mInitScale,根据原图大小缩放来计算

            mInitScale = 1.0f;
            //如果图片宽度大于控件宽度，图片高度小于控件高度  图片缩小
            if (intrinsicWidth > width && intrinsicHeight < height)
                mInitScale = width * 1.0f / intrinsicWidth;
                //如果图片高度大于控件高度，图片宽度小于控件宽度  图片缩小
            else if (intrinsicHeight > height && intrinsicWidth < width)
                mInitScale = height * 1.0f / intrinsicHeight;
                //如果图片的宽与高都大于控件的宽与高
            else if (intrinsicHeight > height && intrinsicWidth > width)
                mInitScale = Math.min(width * 1.0f / intrinsicWidth, height * 1.0f / intrinsicHeight);
                //如果图片的宽与高都小于控件的宽与高
            else if (intrinsicHeight < height && intrinsicWidth < width)
                mInitScale = Math.min(width * 1.0f / intrinsicWidth, height * 1.0f / intrinsicHeight);
        }

        //获取初始化时图片需要进行移动的值，移动到视图中心
        final float moveX = width/2 - intrinsicWidth/2;
        final float moveY = height/2 - intrinsicHeight/2;

        mMatrix.postTranslate(moveX, moveY);
        mMatrix.postScale(mInitScale, mInitScale, width/2, height/2);
        mImageView.setImageMatrix(mMatrix);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                 if (mImageView.getParent() != null)
                    //父控件不拦截TouchEcent事件
                    mImageView.getParent().requestDisallowInterceptTouchEvent(true);

                cancelFling();//取消快速滑动事件
                break;

            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_UP:
                final RectF rectF = getMatrixRectF();
                final float imageViewWidth = getImageViewWidth();

                float currentScale = getScale();
                if (Math.abs(currentScale - mInitScale) < 0.02) {//float的精度问题
                    currentScale = mInitScale;
                }
                //如果当前图片大小<初始图片大小，缩放恢复到图片初始大小
                if (currentScale < mInitScale){
                    new Thread(new AutoScaleRunnable(rectF.centerX(), rectF.centerY(), currentScale, mInitScale)).start();
                }
                else if (currentScale > mMaxScale){//如果当前图片大小>图片最大尺寸，缩放恢复到图片最大尺寸
                    Point point = getCenter(event);//多点触控，计算中心点
                    new Thread(new AutoScaleRunnable(point.x, point.y, currentScale, mMaxScale)).start();
                }

                if (!mIsViewPager)//父控件不是ViewPager时才允许不检查x方向边界
                    //如果图片被拖动偏离x方向的边界
                    if ((rectF.left>0 && Math.abs(rectF.left)>0.2) ||
                            (rectF.right<imageViewWidth && Math.abs(rectF.right-imageViewWidth)>0.2)) {

                        final float startX, targetX;
                        //图片宽度大于ImageView控件宽度
                        if (rectF.width() > imageViewWidth) {
                            if (rectF.left > 0) {//图片被拖到了右侧
                                startX = rectF.left;
                                targetX = 0;
                            } else {
                                startX = rectF.right;
                                targetX = imageViewWidth;
                            }
                        }
                        //图片宽度小于ImageView控件宽度
                        else {
                            if (rectF.left > (imageViewWidth-rectF.right)) {//图片被拖到了右侧
                                startX = rectF.left;
                                targetX = imageViewWidth/2 - rectF.width()/2;
                            }
                            else {//图片被拖到了左侧
                                startX = rectF.right;
                                targetX = imageViewWidth/2 + rectF.width()/2;
                            }
                        }

                        new Thread(new AutoTranslateRunnable(startX, targetX)).start();
                        return true;
                    }

//                checkBorderAndCenter();
                break;
        }

        //将事件传递给单击，双击，长按手势检测的onTouchEvent处理
        if (mGestureDetector.onTouchEvent(event)){
            return true;
        }
        //将事件传递给拖拽，快速滑动，多点触控缩放手势检测的onTouchEvent处理
        mOnGeatureDetector.onTouchEvent(event);
        return true;
    }

    /**
     * 多点触控，取中心点
     * @param event
     * @return
     */
    private Point getCenter(MotionEvent event){
        Point point = new Point();
        float x = 0;
        float y = 0;
        //触摸点的个数
        int pointerCount = event.getPointerCount();
        for (int i=0; i<pointerCount; i++){
            x += event.getX(i);
            y += event.getY(i);
        }
        //得到多个触摸点的x与y均值,即中心点
        x = x / pointerCount;
        y = y / pointerCount;

        point.x = (int) x;
        point.y = (int) y;
        return point;
    }

    /**
     * @return 获取当前图片的缩放值
     */
    private float getScale(){
        final float[] floats = new float[9];
        mMatrix.getValues(floats);
        //获取当前的x方向缩放值(x，y方向缩放值相同)
        return floats[mMatrix.MSCALE_X];
    }

    /**
     * 检查图片边界是否超出控件边界
     * @param isDrag 是否为拖拽事件
     * @return
     */
    private boolean checkBorder(boolean isDrag){
        //得到放大或者缩小后的图片的宽与高
        final RectF matrixRectF = getMatrixRectF();
        if (matrixRectF == null)
            return false;

        float deltaX = 0;
        float deltaY = 0;
        //得到控件的宽与高
        final int width = getImageViewWidth();
        final int height = getImageViewHeight();

        if (matrixRectF.width() <= width) {//如果宽度与高度小于控件的宽与高，则让图片居中
            deltaX = width / 2 - matrixRectF.left - matrixRectF.width() / 2;
            mScrollEdge = EDGE_BOTH;
        }
        //当图片距控件左边的距离left>0那说明图片距离左边出现的空白，需要将图片向左面移动
        else if (matrixRectF.left > 0) {
            deltaX = -matrixRectF.left;
            mScrollEdge = EDGE_LEFT;
        }
        //当图片距控件右边的距离right小于控件的width的时候，说明图片距离控件的右面会出现空白，需要将图片向右面移动
        else if (matrixRectF.right < width) {
            deltaX = width - matrixRectF.right;
            mScrollEdge = EDGE_RIGHT;
        }
        else {
            mScrollEdge = EDGE_NONE;
        }

        if (matrixRectF.height() <= height) {
            deltaY = height / 2 - matrixRectF.top - matrixRectF.height() / 2;
        }
        else if (matrixRectF.top > 0) {
            deltaY = -matrixRectF.top;
        }
        else if (matrixRectF.bottom < height) {
            deltaY = height - matrixRectF.bottom;
        }

        if (isDrag)//如果是拖拽行为，检查边界并且只修正y方向的偏移量
            mMatrix.postTranslate(0, deltaY);
        else {
            mMatrix.postTranslate(deltaX, deltaY);//否则，同时检查x,y方向边界
        }
        return true;
    }

    //进行边界控制的逻辑
    private void checkBorderAndCenter() {
        if (checkBorder(false))
            mImageView.setImageMatrix(mMatrix);
    }

    //ImageView控件放置Image宽度
    private int getImageViewWidth(){
        return mImageView.getWidth()-mImageView.getPaddingLeft()-mImageView.getPaddingRight();
    }

    //ImageView控件放置Image高度
    private int getImageViewHeight(){
        return mImageView.getHeight()-mImageView.getPaddingTop()-mImageView.getPaddingBottom();
    }

    //获得转换为RectF格式的图片尺寸
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

    //取消快速滑动行为
    private void cancelFling(){
        if (mAutoFlingRunnable != null){
            mAutoFlingRunnable.cancelFling();
            mAutoFlingRunnable = null;
        }
    }

    public class AutoScaleRunnable implements Runnable {
        //插值方式--加速减速插值器
        private Interpolator mInterpolator;
        //缩放中心点
        private final float mFocalX, mFocalY;
        private final long mStartTime;//开始时间
        private final float mZoomStart, mZoomEnd;

        /**
         * @param mFocalX
         * @param mFocalY
         * @param mZoomStart
         * @param mZoomEnd
         */
        public AutoScaleRunnable(float mFocalX, float mFocalY, float mZoomStart, float mZoomEnd){
            this.mFocalX = mFocalX;
            this.mFocalY = mFocalY;
            this.mZoomStart = mZoomStart;
            this.mZoomEnd = mZoomEnd;
            mStartTime = System.currentTimeMillis();//获取当前开始时间
            //插值方式--加速减速插值器
            mInterpolator = new AccelerateDecelerateInterpolator();
        }

        @Override
        public void run() {
            Log.e(TAG, "AutoScaleRunnable start...");

            final float t = interpolate();
            //根据插值，获取当前时间的缩放值
            float scale = mZoomStart + t * (mZoomEnd - mZoomStart);
            //获取缩放比，大于1表示在放大，小于1表缩小
            float deltaScale = scale / getScale();

            mMatrix.postScale(deltaScale, deltaScale, mFocalX, mFocalY);
            if (checkBorder(false))
                mHandler.sendEmptyMessage(0);

            if (t < 1f)//还没有缩放完成，继续缩放
                mImageView.postDelayed(this, 10);
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration;
            t = Math.min(1f, t);
            t = mInterpolator.getInterpolation(t);
            return t;
        }
    }

    public class AutoFlingRunnable implements Runnable{
        private final OverScroller mScroller;
        //图片当前左上角的坐标值
        private float mCurrentX;
        private float mCurrentY;

        public AutoFlingRunnable(Context context){
            mScroller = new OverScroller(context);
        }

        public void cancelFling(){
            mScroller.forceFinished(true);//停止滑动
        }

        public void fling(int viewWidth, int viewHeight, int vX, int vY){
            RectF rectF = getMatrixRectF();
            if (rectF == null)
                return;

            final float imageViewWidth = getImageViewWidth();
            final float imageViewHeight = getImageViewHeight();

            //x,y方向最小最大的偏移量
            int minX, maxX, minY, maxY;
            //滚动起始点X坐标
            final int startX = Math.round(-rectF.left);
            if (rectF.width() > imageViewWidth) {
                minX = 0;
                maxX = Math.round(rectF.width() - imageViewWidth);
            }
            else{
                minX = maxX = startX;
            }

            //滚动起始点Y坐标
            final int startY = Math.round(-rectF.top);
            if (rectF.height() > imageViewHeight){
                minY = 0;
                maxY = Math.round(rectF.height() - imageViewHeight);
            }
            else{
                minY = maxY = startY;
            }

            mCurrentX = startX;
            mCurrentY = startY;

            //fling()函数会根据velocityX, velocityY的值大小计算滑动距离
            if (maxX != startX || maxY != startY){
                mScroller.fling(startX, startY, vX, vY, minX, maxX, minY, maxY, 0, 0);
            }
        }

        @Override
        public void run() {
            if (mScroller.isFinished())
                return;
            Log.e(TAG, "AutoFlingRunnable start...");

            if (mScroller.computeScrollOffset()){
                //返回当前滚动X方向的偏移,返回值:距离原点(ImageView控件左上角)X方向的绝对值
                final int x = mScroller.getCurrX();
                final int y = mScroller.getCurrY();

                //postTranslate()是以累加方式应用到变换矩阵上，所以要mCurrentX-x
                mMatrix.postTranslate(mCurrentX-x, mCurrentY-y);
                mCurrentX = x;
                mCurrentY = y;

                if (checkBorder(true))//快速滑动事件，属于拖动事件，允许x方向超出ImageView控件宽度
                    mHandler.sendEmptyMessage(0);

                mImageView.postDelayed(this, 10);
            }
        }
    }

    public class AutoTranslateRunnable implements Runnable{
        //插值方式
        private Interpolator mInterpolator;
        private final float mStartX;
        private final float mTargetX;
        //记录上一次平移到达的位置
        private float mLastX;
        private final long mStartTime;

        public AutoTranslateRunnable(float startX, float targetX){
            mStartX = startX;
            mTargetX = targetX;
            mStartTime = System.currentTimeMillis();
            //线性插值器
            mInterpolator = new LinearInterpolator();
        }

        @Override
        public void run() {
            Log.e(TAG, "AutoTranslateRunnable start...");

            float t = interpolate();
            //到达的x的位置
            final float x = t * (mTargetX - mStartX);
            //偏移量
            final float delta = x - mLastX;
            mMatrix.postTranslate(delta, 0);
            //记录上一次已经到达的x的位置
            mLastX = x;
            mHandler.sendEmptyMessage(0);

            if (t < 1f)
                mImageView.postDelayed(this, 10);
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / mTranslateDuration;
            t = Math.min(1f, t);
            t = mInterpolator.getInterpolation(t);
            return t;
        }
    }

    public void setInitScale(float initScale){
        mInitScale = initScale;
    }

    public void setMidScale(float midScale){
        mMidScale = midScale;
    }

    public void setMaxScale(float maxScale){
        mMaxScale = maxScale;
    }

    public void setMinOverstep(float minOverstep){
        mMinOverstep = minOverstep;
    }

    public void setMaxOverstep(float maxOverstep){
        mMaxOVerstep = maxOverstep;
    }

    public void setZoomDuration(int duration){
        mZoomDuration = duration;
    }

    public void setTranslateDuration(int duration){
        mTranslateDuration = duration;
    }

    public void setOnLongClickListener(View.OnLongClickListener listener){
        mOnLongClickListener = listener;
    }

    public void setOnClickListener(View.OnClickListener listener){
        mOnClickListener = listener;
    }

    public void setIsViewPager(boolean isViewPager){
        mIsViewPager = isViewPager;
    }

}
