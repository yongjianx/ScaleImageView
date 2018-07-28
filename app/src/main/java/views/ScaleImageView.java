package views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import utils.ScaleImageViewAttacher;

/**
 * Created by skyworthclub on 2018/7/17.
 */

public class ScaleImageView extends AppCompatImageView {
    private static final String TAG = "ScaleImageView";
    private ScaleImageViewAttacher attacher;

    public ScaleImageView(Context context){
        this(context,null);
    }
    public ScaleImageView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }
    public ScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        attacher = new ScaleImageViewAttacher(this);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
    }

    /**
     * 其他方法内部都是通过调用此方法实现
     * @param drawable
     */
    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
    }

    @Override
    public void setImageIcon(@Nullable Icon icon) {
        super.setImageIcon(icon);
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        super.setImageURI(uri);
    }

    //设置初始图片的尺寸，不调用此方法时初始值由初始加载图片时计算出来
    public void setInitScale(float initScale){
        attacher.setInitScale(initScale);
    }

    public void setMidScale(float midScale){
        attacher.setMidScale(midScale);
    }
    //设置缩放图片的最大尺寸，默认为3*初始图片的尺寸
    public void setMaxScale(float maxScale){
        attacher.setMaxScale(maxScale);
    }

    //设置图片的多点触控时的最小尺寸，默认为0.5*初始图片的尺寸
    public void setMinOverstep(float minOverstep){
        attacher.setMinOverstep(minOverstep);
    }

    //设置图片的多点触控时的最大尺寸，默认为4*初始图片的尺寸
    public void setMaxOverstep(float maxOverstep){
        attacher.setMaxOverstep(maxOverstep);
    }

    //设置图片的双击缩放时持续时间
    public void setZoomDuration(int duration){
        attacher.setZoomDuration(duration);
    }

    //图片被拖动超出x方向边界，MOTIONEVENT.ACTION_UP事件图片回弹的持续时间
    public void setTranslateDuration(int duration){
        attacher.setTranslateDuration(duration);
    }

    //图片长按监听事件
    public void setOnLongClickListener(OnLongClickListener listener){
        attacher.setOnLongClickListener(listener);
    }

    //图片点击监听事件
    public void setOnClickListener(OnClickListener listener){
        attacher.setOnClickListener(listener);
    }

    public void setIsViewPager(boolean isViewPager){
        attacher.setIsViewPager(isViewPager);
    }

}
