package com.lzy.imagepicker.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.lzy.imagepicker.DataHolder;
import com.lzy.imagepicker.ImageDataSource;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.R;
import com.lzy.imagepicker.adapter.ImageFolderAdapter;
import com.lzy.imagepicker.adapter.ImageRecyclerAdapter;
import com.lzy.imagepicker.adapter.ImageRecyclerAdapter.OnImageItemClickListener;
import com.lzy.imagepicker.bean.ImageFolder;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.util.Utils;
import com.lzy.imagepicker.view.FolderPopUpWindow;
import com.lzy.imagepicker.view.GridSpacingItemDecoration;
import com.lzy.imagepicker.view.SuperCheckBox;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class ImageGridActivity extends ImageBaseActivity implements ImageDataSource.OnImagesLoadedListener, OnImageItemClickListener, ImagePicker.OnImageSelectedListener, View.OnClickListener {

    public static final int REQUEST_PERMISSION_STORAGE = 0x01;
    public static final int REQUEST_PERMISSION_CAMERA = 0x02;
    public static final String EXTRAS_TAKE_PICKERS = "TAKE";
    public static final String EXTRAS_IMAGES = "IMAGES";
    public static final String EXTRA_DIRECT_BACK_APP = "DirectBackApp";

    private ImagePicker imagePicker;

    private View mFooterBar;
    private Button mBtnOk;
    private View mllDir;
    private TextView mtvDir;
    private TextView mBtnPre;
    private ImageFolderAdapter mImageFolderAdapter;
    private FolderPopUpWindow mFolderPopupWindow;
    private List<ImageFolder> mImageFolders;
    private boolean directPhoto = false;
    private RecyclerView mRecyclerView;
    private ImageRecyclerAdapter mRecyclerAdapter;
    private SuperCheckBox mCbOrigin;
    private boolean directBack2App = true;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        directPhoto = savedInstanceState.getBoolean(EXTRAS_TAKE_PICKERS, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRAS_TAKE_PICKERS, directPhoto);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_grid);
        imagePicker = ImagePicker.getInstance();
        imagePicker.clear();
        imagePicker.addOnImageSelectedListener(this);
        if (imagePicker.getSelectLimit() == 0 || imagePicker.getSelectLimit() == 1) {
            imagePicker.setSelectLimit(1);
            imagePicker.setMultiMode(false);
        }

        Intent data = getIntent();
        if (data != null && data.getExtras() != null) {
            directPhoto = data.getBooleanExtra(EXTRAS_TAKE_PICKERS, false);
            directBack2App = data.getBooleanExtra(EXTRA_DIRECT_BACK_APP, true);
            if (directPhoto) {
                checkToCapture();
            }
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(EXTRAS_IMAGES);
            imagePicker.setSelectedImages(images);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);

        findViewById(R.id.btn_back).setOnClickListener(this);
        mBtnOk = (Button) findViewById(R.id.btn_ok);
        mBtnOk.setOnClickListener(this);
        mBtnPre = (TextView) findViewById(R.id.btn_preview);
        mBtnPre.setOnClickListener(this);
        mCbOrigin = (SuperCheckBox) findViewById(R.id.cb_origin);
        mFooterBar = findViewById(R.id.footer_bar);
        mllDir = findViewById(R.id.ll_dir);
        mllDir.setOnClickListener(this);
        mtvDir = (TextView) findViewById(R.id.tv_dir);
        if (imagePicker.isMultiMode()) {
            mBtnOk.setVisibility(View.VISIBLE);
            mBtnPre.setVisibility(View.VISIBLE);
        } else {
            mBtnOk.setVisibility(View.GONE);
            mBtnPre.setVisibility(View.GONE);
        }

        mImageFolderAdapter = new ImageFolderAdapter(this, null);
        mRecyclerAdapter = new ImageRecyclerAdapter(this, null);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        mRecyclerView.setAdapter(mRecyclerAdapter);
        onImageSelected(0, null, false);
        mCbOrigin.setChecked(imagePicker.isOrigin());

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if(Build.VERSION.SDK_INT >= 33 && Utils.getAppTargetSdkVersion(this) >= 33){
                permission = Manifest.permission.READ_MEDIA_IMAGES;
            }
            if (checkPermission(permission)) {
                new ImageDataSource(this, null, this);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_PERMISSION_STORAGE);
            }
        } else {
            new ImageDataSource(this, null, this);
        }
        mCbOrigin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                imagePicker.setOrigin(isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCbOrigin.setChecked(imagePicker.isOrigin());
    }

    private void checkToCapture() {
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if(Build.VERSION.SDK_INT >= 33 && Utils.getAppTargetSdkVersion(this) >= 33){
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        }

        if (!(checkPermission(Manifest.permission.CAMERA)) || !checkPermission(permission)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                    permission}, ImageGridActivity.REQUEST_PERMISSION_CAMERA);
        } else {
            imagePicker.takePicture(this, ImagePicker.REQUEST_CODE_TAKE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new ImageDataSource(this, null, this);
            } else {
                showToast(getString(R.string.ip_str_no_permission));
            }
        } else if (requestCode == REQUEST_PERMISSION_CAMERA) {
            boolean denied = false;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true;
                }
            }
            if (!denied) {
                imagePicker.takePicture(this, ImagePicker.REQUEST_CODE_TAKE);
            } else {
                showToast(getString(R.string.ip_str_no_camera_permission));
            }
        }
    }

    @Override
    protected void onDestroy() {
        imagePicker.removeOnImageSelectedListener(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_ok) {
            Intent intent = new Intent();
            intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
            setResult(ImagePicker.RESULT_CODE_ITEMS, intent);
            finish();
        } else if (id == R.id.ll_dir) {
            if (mImageFolders == null) {
                return;
            }
            createPopupFolderList();
            mImageFolderAdapter.refreshData(mImageFolders);
            if (mFolderPopupWindow.isShowing()) {
                mFolderPopupWindow.dismiss();
            } else {
                mFolderPopupWindow.showAtLocation(mFooterBar, Gravity.NO_GRAVITY, 0, 0);
                int index = mImageFolderAdapter.getSelectIndex();
                index = index == 0 ? index : index - 1;
                mFolderPopupWindow.setSelection(index);
            }
        } else if (id == R.id.btn_preview) {
            Intent intent = new Intent(ImageGridActivity.this, ImagePreviewActivity.class);
            intent.putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, 0);
            intent.putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, imagePicker.getSelectedImages());
            intent.putExtra(ImagePicker.EXTRA_FROM_ITEMS, true);
            startActivityForResult(intent, ImagePicker.REQUEST_CODE_PREVIEW);
        } else if (id == R.id.btn_back) {
            finish();
        }
    }

    private void createPopupFolderList() {
        mFolderPopupWindow = new FolderPopUpWindow(this, mImageFolderAdapter);
        mFolderPopupWindow.setOnItemClickListener(new FolderPopUpWindow.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                mImageFolderAdapter.setSelectIndex(position);
                imagePicker.setCurrentImageFolderPosition(position);
                mFolderPopupWindow.dismiss();
                ImageFolder imageFolder = (ImageFolder) adapterView.getAdapter().getItem(position);
                if (null != imageFolder) {
                    mRecyclerAdapter.refreshData(imageFolder.images);
                    mtvDir.setText(imageFolder.name);
                }
            }
        });
        mFolderPopupWindow.setMargin(mFooterBar.getHeight());
    }

    @Override
    public void onImagesLoaded(List<ImageFolder> imageFolders) {
        this.mImageFolders = imageFolders;
        imagePicker.setImageFolders(imageFolders);
        if (imageFolders.size() == 0) {
            mRecyclerAdapter.refreshData(null);
        } else {
            mRecyclerAdapter.refreshData(imageFolders.get(0).images);
        }
        mRecyclerAdapter.setOnImageItemClickListener(this);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        if (mRecyclerView.getItemDecorationCount() < 1) {
            mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3, Utils.dp2px(this, 2), false));
        }
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mImageFolderAdapter.refreshData(imageFolders);
    }

    @Override
    public void onImageItemClick(View view, ImageItem imageItem, int position) {
        position = imagePicker.isShowCamera() ? position - 1 : position;
        if (imagePicker.isMultiMode()) {
            Intent intent = new Intent(ImageGridActivity.this, ImagePreviewActivity.class);
            intent.putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, position);

            DataHolder.getInstance().save(DataHolder.DH_CURRENT_IMAGE_FOLDER_ITEMS, imagePicker.getCurrentImageFolderItems());
            startActivityForResult(intent, ImagePicker.REQUEST_CODE_PREVIEW);
        } else {
            imagePicker.clearSelectedImages();
            imagePicker.addSelectedImageItem(position, imagePicker.getCurrentImageFolderItems().get(position), true);
            if (imagePicker.isFreeCrop) {
                Intent intent = new Intent(ImageGridActivity.this, FreeCropActivity.class);
                startActivityForResult(intent, ImagePicker.REQUEST_CODE_CROP);
            } else if (imagePicker.isCrop()) {
                Intent intent = new Intent(ImageGridActivity.this, ImageCropActivity.class);
                startActivityForResult(intent, ImagePicker.REQUEST_CODE_CROP);
            } else {
                Intent intent = new Intent();
                intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
                setResult(ImagePicker.RESULT_CODE_ITEMS, intent);
                finish();
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    @Override
    public void onImageSelected(int position, ImageItem item, boolean isAdd) {
        if (imagePicker.getSelectImageCount() > 0) {
            mBtnOk.setText(getString(R.string.ip_select_complete, imagePicker.getSelectImageCount(), imagePicker.getSelectLimit()));
            mBtnOk.setEnabled(true);
            mBtnPre.setEnabled(true);
            mBtnPre.setText(getResources().getString(R.string.ip_preview_count, imagePicker.getSelectImageCount()));
            mBtnPre.setTextColor(ContextCompat.getColor(this, R.color.ip_text_primary_inverted));
            mBtnOk.setTextColor(ContextCompat.getColor(this, R.color.ip_text_primary_inverted));
        } else {
            mBtnOk.setText(getString(R.string.ip_complete));
            mBtnOk.setEnabled(imagePicker.isMultiMode());
            mBtnPre.setEnabled(false);
            mBtnPre.setText(getResources().getString(R.string.ip_preview));
            mBtnPre.setTextColor(ContextCompat.getColor(this, R.color.ip_text_secondary_inverted));
//            mBtnOk.setTextColor(ContextCompat.getColor(this, R.color.ip_text_secondary_inverted));
            mBtnOk.setTextColor(ContextCompat.getColor(this, R.color.ip_text_primary_inverted));
        }
        for (int i = imagePicker.isShowCamera() ? 1 : 0; i < mRecyclerAdapter.getItemCount(); i++) {
            if (mRecyclerAdapter.getItem(i).uri != null && mRecyclerAdapter.getItem(i).uri.equals(item.uri)) {
                mRecyclerAdapter.notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getExtras() != null) {
            if (resultCode == ImagePicker.RESULT_CODE_BACK) {
            } else {
                if (data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS) == null) {
                } else {
                    setResult(ImagePicker.RESULT_CODE_ITEMS, data);
                }
                finish();
            }
        } else {
            if (requestCode == ImagePicker.REQUEST_CODE_TAKE) {
                if(resultCode == RESULT_OK) {
                    ImageItem imageItem = new ImageItem();
                    imageItem.uri = imagePicker.getUri();
                    if (!imagePicker.isMultiMode()) {
                        if (imagePicker.isFreeCrop) {
                            imagePicker.clearSelectedImages();
                            imagePicker.addSelectedImageItem(0, imageItem, true);
                            Intent intent = new Intent(ImageGridActivity.this, FreeCropActivity.class);
                            startActivityForResult(intent, ImagePicker.REQUEST_CODE_CROP);
                            return;
                        } else if (imagePicker.isCrop()) {
                            imagePicker.clearSelectedImages();
                            imagePicker.addSelectedImageItem(0, imageItem, true);
                            Intent intent = new Intent(ImageGridActivity.this, ImageCropActivity.class);
                            startActivityForResult(intent, ImagePicker.REQUEST_CODE_CROP);
                            return;
                        }
                    }
                    imagePicker.addSelectedImageItem(0, imageItem, true);
                    Intent intent = new Intent();
                    intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
                    setResult(ImagePicker.RESULT_CODE_ITEMS, intent);
                    finish();
                }else{
                    if(imagePicker.getUri() != null) {
                        //https://github.com/CysionLiu/ImagePicker/issues/74
                        int result = getContentResolver().delete(imagePicker.getUri(), null, null);
                        Log.i("DEBUG",">>>>>>>>>>>>>>>>result = " + result);
                    }
                    if(directPhoto && directBack2App){
                        finish();
                    }
                }
            } else if (directPhoto) {
                finish();
            }
        }
    }

}