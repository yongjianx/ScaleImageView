package com.example.skyworthclub.imagescale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

import views.ScaleImageView;

public class SampleActivity extends AppCompatActivity {
    private ScaleImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        imageView =  (ScaleImageView) findViewById(R.id.imageView);
        try {
            getBitmapForImgResourse(this, R.drawable.image, imageView);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 大图片处理机制
     * 利用Bitmap 转存 R图片
     * @param mContext
     * @param imgId
     * @param mImageView
     * @throws IOException
     */
    public static void getBitmapForImgResourse(Context mContext, int imgId, ImageView mImageView) throws IOException {
        InputStream is = mContext.getResources().openRawResource(imgId);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inSampleSize = 1;
        final Bitmap btp = BitmapFactory.decodeStream(is, null, options);
        mImageView.setImageBitmap(btp);
        is.close();
    }

}
