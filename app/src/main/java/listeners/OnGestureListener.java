package listeners;

/**
 * 拖动、快速滑动、多点触控缩放监听器
 * Created by skyworthclub on 2018/7/20.
 */

public interface OnGestureListener {

    void onDrag(float dx, float dy);

    void onFling(float startX, float startY, float velocityX, float velocityY);

    void onScale(float scaleFactor, float focusX, float focusY);
}
