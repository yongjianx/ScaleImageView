# ScaleImageView
**android图片缩放库，支持单击、双击、长按、拖拽、多点触控缩放**\
<br>
基于-[PhotoView](https://github.com/chrisbanes/PhotoView)实现，并在`PhotoView`的基础上做了改进，比如允许图片偏离x方向边界，手指抬起时回弹。\
为方便大家学习，我仓库上的[PhotoView](https://github.com/yongjianx/PhotoView)源码我作了详细注释.
## 使用方法：
将以下内容添加到根目录下的`build.gradle`(**注意**：不是`module:app`下的`build.gradle`）
```gdb
allprojects {
	repositories {
        maven { url "https://jitpack.io" }
    }
}
```
然后将库依赖添加到`module:app`目录下的`build.gradle`
```gdb
dependencies {
    implementation 'com.github.yongjianx:ScaleImageView:v1.0'
}
```
## 使用示例
### 单张图片使用示例
定义`xml`文件
```xml
<views.ScaleImageView
        android:id="@+id/imageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/colorBackground"/>
```
在`activity`中
```
imageView =  (ScaleImageView) findViewById(R.id.imageView);
        try {
            getBitmapForImgResourse(this, R.drawable.image, imageView);
        }
        catch (IOException e){
            e.printStackTrace();
        }
```
`getBitmapForImgResourse(this, R.drawable.image, imageView);`**加载大图片时防止OOM**\
也可以使用图片加载框架--[Glide](https://github.com/bumptech/glide/releases)
### ViewPager多张图片浏览
有一些ViewGroups（利用`onInterceptTouchEvent()`)的ViewGroups）在将ScaleImageView放入其中时可能会抛出`IllegalArgumentException`异常，比如**ViewPager**。为了防止此异常,请使用`自定义ViewPager`捕获异常.
```java
public class MyViewPager extends ViewPager {

    public MyViewPager(Context context) {
        super(context);
    }

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }
}
```
然后, 在`PagerAdapter`的`instantiateItem()`方法中添加以下内容
```
ScaleImageView view = new ScaleImageView(container.getContext());
view.setIsViewPager(true);//标识ViewGroup为ViewPager,处理滑动冲突
```
最后，在`activity`中设置`ViewPager`的适配器
```
viewPager = (MyViewPager) findViewById(R.id.viewPager);
viewPager.setAdapter(new ViewPagerAdapter());
```
具体可参考`app/src/main/java/com/example/skyworthclub/imagescale/ViewPagerActivity.java`的实现.
## 关于*API*
```
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

//标识ViewGroup为ViewPager,处理滑动冲突
public void setIsViewPager(boolean isViewPager){
    attacher.setIsViewPager(isViewPager);
}
```


