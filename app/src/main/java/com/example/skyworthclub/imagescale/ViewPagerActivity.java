package com.example.skyworthclub.imagescale;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;

import views.MyViewPager;
import views.ScaleImageView;

public class ViewPagerActivity extends AppCompatActivity {
    private static final String TAG = "ViewPagerActivity";
    private MyViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        viewPager = (MyViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPagerAdapter());

    }

    public static class ViewPagerAdapter extends PagerAdapter{
        private static final int[] MDRAWABLES = {R.drawable.image, R.drawable.image1, R.drawable.image,
                R.drawable.image1, R.drawable.image, R.drawable.image1};

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            ScaleImageView view = new ScaleImageView(container.getContext());
            view.setIsViewPager(true);//标识ViewGroup为ViewPager,处理滑动冲突
            try {
                SampleActivity.getBitmapForImgResourse(container.getContext(), MDRAWABLES[position], view);
            }
            catch (IOException e){
                e.printStackTrace();
            }

            container.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getCount() {
            return MDRAWABLES.length;
        }
    }

}
