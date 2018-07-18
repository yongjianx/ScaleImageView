package views;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

/**
 * Created by skyworthclub on 2018/7/17.
 */

public class ScaleImageView extends AppCompatImageView {

    private ScaleImageViewAttacher attacher;

    public ScaleImageView(Context context){
        this(context,null);
    }
    public ScaleImageView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }
    public ScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        attacher = new ScaleImageViewAttacher(this);
        //设置矩阵放大缩小
        setScaleType(ScaleType.MATRIX);
    }

}
