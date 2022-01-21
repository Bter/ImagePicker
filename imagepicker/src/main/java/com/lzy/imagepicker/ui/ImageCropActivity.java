package com.lzy.imagepicker.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.R;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.util.BitmapUtil;
import com.lzy.imagepicker.view.CropImageView;

import java.io.File;
import java.util.ArrayList;

public class ImageCropActivity extends ImageBaseActivity implements View.OnClickListener, CropImageView.OnBitmapSaveCompleteListener {

    private CropImageView mCropImageView;
    private Bitmap mBitmap;
    private boolean mIsSaveRectangle;
    private int mOutputX;
    private int mOutputY;
    private ArrayList<ImageItem> mImageItems;
    private ImagePicker imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        imagePicker = ImagePicker.getInstance();

        findViewById(R.id.btn_back).setOnClickListener(this);
        Button btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_ok.setText(getString(R.string.ip_complete));
        btn_ok.setOnClickListener(this);
        TextView tv_des = (TextView) findViewById(R.id.tv_des);
        tv_des.setText(getString(R.string.ip_photo_crop));
        mCropImageView = (CropImageView) findViewById(R.id.cv_crop_image);
        mCropImageView.setOnBitmapSaveCompleteListener(this);

        mOutputX = imagePicker.getOutPutX();
        mOutputY = imagePicker.getOutPutY();
        mIsSaveRectangle = imagePicker.isSaveRectangle();
        mImageItems = imagePicker.getSelectedImages();
        if(mImageItems.size() == 0){
            //https://github.com/CysionLiu/ImagePicker/pull/49/commits/f044f49bc527c1730204e36ee4cfe5d18be15131
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        Uri uri = mImageItems.get(0).uri;

        mCropImageView.setFocusStyle(imagePicker.getStyle());
        mCropImageView.setFocusWidth(imagePicker.getFocusWidth());
        mCropImageView.setFocusHeight(imagePicker.getFocusHeight());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null,options);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            options.inSampleSize = calculateInSampleSize(options, displayMetrics.widthPixels, displayMetrics.heightPixels);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //https://github.com/CysionLiu/ImagePicker/issues/70
                options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);//增加支持广色域
            }
            options.inJustDecodeBounds = false;
            mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null,options);
            if(mBitmap == null){
                //https://github.com/CysionLiu/ImagePicker/pull/49/commits/f044f49bc527c1730204e36ee4cfe5d18be15131
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            mCropImageView.setImageBitmap(mCropImageView.rotate(mBitmap, BitmapUtil.getBitmapDegree(this,mImageItems.get(0).uri)));
        } catch (Exception aE) {
            aE.printStackTrace();
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = width / reqWidth;
            } else {
                inSampleSize = height / reqHeight;
            }
        }
        return inSampleSize;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_back) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (id == R.id.btn_ok) {
            mCropImageView.saveBitmapToFile(imagePicker.getCropCacheFolder(this), mOutputX, mOutputY, mIsSaveRectangle);
        }
    }

    @Override
    public void onBitmapSaveSuccess(File file) {

        mImageItems.remove(0);
        ImageItem imageItem = new ImageItem();
        imageItem.uri = Uri.fromFile(file);
        mImageItems.add(imageItem);

        Intent intent = new Intent();
        intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, mImageItems);
        setResult(ImagePicker.RESULT_CODE_ITEMS, intent);
        finish();
    }

    @Override
    public void onBitmapSaveError(File file) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCropImageView.setOnBitmapSaveCompleteListener(null);
        if (null != mBitmap && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
