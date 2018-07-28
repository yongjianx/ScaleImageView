package utils;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import listeners.OnGestureListener;

/**
 * 拖动、快速滑动、多点触控缩放手势检测
 * Created by skyworthclub on 2018/7/20.
 */

public class OnGestureDetector {
    private static final String TAG = "OnGestureDetector";
    private static final int INVALID_POINTER_ID = -1;

    private int mActivePointerId = INVALID_POINTER_ID;//无效手指id
    private int mActivePointerIndex = 0;//手指索引值

    private float mLastTouchX;//上一次触碰点坐标
    private float mLastTouchY;

    private boolean mIsDragging;
    //拖动能识别的最小距离
    private float mTouchSlop;
    //拖动能识别的最小速度
    private float mMinVelocity;
    private VelocityTracker mVelocityTracker;

    private ScaleGestureDetector mScaleGestureDetector;
    private OnGestureListener mOnGestureListener;

    public OnGestureDetector(Context context, final OnGestureListener listener){
        mOnGestureListener = listener;

        //获取拖动时能识别的最小速度
        mMinVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
        //获取拖动时能识别的最小距离
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor))
                    return false;

                listener.onScale(scaleFactor, detector.getFocusX(), detector.getFocusY());//回调多点触控缩放函数
                //返回false，detector认为本次缩放尚未结束，再次计算缩放因子时仍以上次结束时为基准
                //返回true，detector认为本次缩放已结束，计算缩放因子以1为基准
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;//查看源码，onScaleBegin()需返回true才会调用onScale()函数
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
    }

    public boolean onTouchEvent(MotionEvent event){
        mScaleGestureDetector.onTouchEvent(event);
        return processTouchEvent(event);
    }

    private boolean processTouchEvent(MotionEvent event){
        //getAction获得的int值是由pointer的index值和事件类型值组合而成的
        //getActionWithMasked则只返回事件的类型值
        //getAction() & ACTION_POINTER_INDEX_MASK就获得了pointer的id,等同于getActionIndex函数;
        //getAction()& ACTION_MASK就获得了pointer的事件类型，等同于getActionMasked函数
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                //获取第一根手指的id
                mActivePointerId = event.getPointerId(0);

                //手势速度追踪
                if (mVelocityTracker == null)
                    mVelocityTracker = VelocityTracker.obtain();
                //单击情况
                if (mVelocityTracker != null)
                    mVelocityTracker.addMovement(event);

                mLastTouchX = getActiveX(event);
                mLastTouchY = getActiveY(event);
                mIsDragging = false;
                break;

            case MotionEvent.ACTION_MOVE://move处理是否拖动
                final float x = getActiveX(event);
                final float y = getActiveY(event);
                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;
                //判断手势滑动距离是否足以触发ACTION_MOVE事件
                if (!mIsDragging)
                    mIsDragging = isMoveAction(dx, dy);

                if (mIsDragging){
                    if (event.getPointerCount() == 1)//手指数为1时才能拖动
                        mOnGestureListener.onDrag(dx, dy);
                    mLastTouchX = x;
                    mLastTouchY = y;

                    if (mVelocityTracker != null)
                        mVelocityTracker.addMovement(event);
                }
                break;

            case MotionEvent.ACTION_UP://手指抬起时处理是否快速滑动
                //当前活动的手指设置为无效状态
                mActivePointerId = INVALID_POINTER_ID;
                if (mIsDragging){
                    if (mVelocityTracker != null){
                        mLastTouchX = getActiveX(event);
                        mLastTouchY = getActiveY(event);

                        //设置时间单位为1s
                        mVelocityTracker.computeCurrentVelocity(1000);
                        //x,y轴的速度，单位：像素/s
                        final float vX = mVelocityTracker.getXVelocity();
                        final float vY = mVelocityTracker.getYVelocity();

                        //判断是否触发快速滑动事件
                        if (isFlingAction(vX, vY))
                            mOnGestureListener.onFling(mLastTouchX, mLastTouchY, -vX, -vY);
                    }
                }
                //回收mVelocityTracker
                if (mVelocityTracker != null){
                    mVelocityTracker.clear();
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER_ID;
                //回收mVelocityTracker
                if (mVelocityTracker != null){
                    mVelocityTracker.clear();
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP://多点触控中，手指抬起处理手指id的切换问题
                //获取某一根手指抬起时的索引
                int pointerIndex = event.getActionIndex();
                //根据索引获取id
                int pointerId = event.getPointerId(pointerIndex);
                //如果是抬起的是第一根手指,即正在滑动的手指
                if (pointerId == mActivePointerId){
                    //那么对应获取第二点
                    final int newPointerIndex = pointerId == 0? 1 : 0;
                    //将id指向第二根手指
                    mActivePointerId = event.getPointerId(newPointerIndex);
                    //获取第二根手指的当前坐标
                    mLastTouchX = event.getX(newPointerIndex);
                    mLastTouchY = event.getY(newPointerIndex);
                }
                break;
        }
        //根据id将索引指向后抬起的手指
        mActivePointerIndex = event.
                findPointerIndex(mActivePointerId != INVALID_POINTER_ID? mActivePointerId : 0);
//        Log.e(TAG, "mActivePointerIndex= "+mActivePointerIndex);

        return true;
    }

    private float getActiveX(MotionEvent event){
        //mActivePointerIndex为手指的索引,根据当前手指的索引获取坐标
        try {
            return event.getX(mActivePointerIndex);
        }
        catch (Exception e){
            return event.getX();
        }

    }

    private float getActiveY(MotionEvent event){
        try{
            return event.getY(mActivePointerIndex);
        }
        catch (Exception e){
            return event.getY();
        }
    }

    public boolean isScaling() {
        return mScaleGestureDetector.isInProgress();
    }

    public boolean isDragging() {
        return mIsDragging;
    }

    /**
     * 判断是否足以触发MOVE事件
     * @param dx x方向偏移量
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }

    /**
     * 判断是否足以触发OnFling事件
     * @param vX
     * @param vY
     * @return
     */
    private boolean isFlingAction(float vX, float vY){
        return Math.max(Math.abs(vX), Math.abs(vY)) >= mMinVelocity;
    }
}
