package com.lzy.imagepicker;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.lzy.imagepicker.bean.ImageFolder;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.util.BitmapUtil;
import com.lzy.imagepicker.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;


public class ImageDataSource implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ALL = 0;
    public static final int LOADER_CATEGORY = 1;
    private final String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media._ID,
    };
    private final String[] IMAGE_PROJECTION_Q = {
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.ORIENTATION
    };
    private String[] mCurProjection = null;

    private FragmentActivity activity;
    private OnImagesLoadedListener loadedListener;
    private ArrayList<ImageFolder> imageFolders = new ArrayList<>();
    private int mLoadedCount = 0;


    public ImageDataSource(FragmentActivity activity, String path, OnImagesLoadedListener loadedListener) {
        this.activity = activity;
        this.loadedListener = loadedListener;
        mLoadedCount = 0;

        LoaderManager loaderManager = activity.getSupportLoaderManager();
        if (path == null) {
            loaderManager.initLoader(LOADER_ALL, null, this);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("path", path);
            loaderManager.initLoader(LOADER_CATEGORY, bundle, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
            mCurProjection = IMAGE_PROJECTION;
        }else{
            mCurProjection  =IMAGE_PROJECTION_Q;
        }
        if (id == LOADER_ALL)
            cursorLoader = new CursorLoader(activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mCurProjection, null, null, mCurProjection[6] + " DESC");
        if (id == LOADER_CATEGORY)
            cursorLoader = new CursorLoader(activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mCurProjection, mCurProjection[1] + " like '%" + args.getString("path") + "%'", null, mCurProjection[6] + " DESC");

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() == 0) {
            return;
        }
        if (mLoadedCount == data.getCount()) {
            return;
        }
        imageFolders.clear();
        mLoadedCount = data.getCount();
        ArrayList<ImageItem> allImages = new ArrayList<>();
        while (data.moveToNext()) {
            String imageName = data.getString(data.getColumnIndexOrThrow(mCurProjection[0]));
            String relativePath = data.getString(data.getColumnIndexOrThrow(mCurProjection[1]));
            long imageSize = data.getLong(data.getColumnIndexOrThrow(mCurProjection[2]));
            int imageWidth = data.getInt(data.getColumnIndexOrThrow(mCurProjection[3]));
            int imageHeight = data.getInt(data.getColumnIndexOrThrow(mCurProjection[4]));
            String imageMimeType = data.getString(data.getColumnIndexOrThrow(mCurProjection[5]));
            long imageAddTime = data.getLong(data.getColumnIndexOrThrow(mCurProjection[6]));
            long id = data.getLong(data.getColumnIndexOrThrow(mCurProjection[7]));
            Uri uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
            );
            ImageItem imageItem = new ImageItem();
            imageItem.name = imageName;
            imageItem.uri = uri;
            imageItem.size = imageSize;
            imageItem.width = imageWidth;
            imageItem.height = imageHeight;
            imageItem.mimeType = imageMimeType;
            imageItem.addTime = imageAddTime;
            allImages.add(imageItem);
            ImageFolder imageFolder = new ImageFolder();
            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
                File imageFile = new File(relativePath);
                File imageParentFile = imageFile.getParentFile();
                if(imageParentFile == null){
                    //https://github.com/CysionLiu/ImagePicker/pull/49/commits/ad737d5ad5e3f09fb1410b49181c6da48b4bae69
                    continue;
                }
                imageFolder.name =imageParentFile.getName() ;
                imageFolder.path = imageParentFile.getAbsolutePath();
            }else{
                String tname = "";
                if(relativePath==null){
                    relativePath="";
                }
                if(relativePath.length()>0){
                    tname = relativePath.substring(0,relativePath.length()-1);
                }
                if (TextUtils.isEmpty(tname)) {
                    tname = "sdcard";
                }
                if(tname.indexOf('/')>=0&&tname.lastIndexOf('/')<tname.length()){
                    tname = tname.substring(tname.lastIndexOf('/')+1);
                }
                imageFolder.name =tname ;
                imageFolder.path = relativePath;
            }
            Utils.innerLog(imageFolder.path);
            if (!imageFolders.contains(imageFolder)) {
                ArrayList<ImageItem> images = new ArrayList<>();
                images.add(imageItem);
                imageFolder.cover = imageItem;
                imageFolder.images = images;
                imageFolders.add(imageFolder);
            } else {
                imageFolders.get(imageFolders.indexOf(imageFolder)).images.add(imageItem);
            }
        }
        if (data.getCount() > 0 && allImages.size() > 0) {
            ImageFolder allImagesFolder = new ImageFolder();
            allImagesFolder.name = activity.getResources().getString(R.string.ip_all_images);
            allImagesFolder.cover = allImages.get(0);
            allImagesFolder.images = allImages;
            allImagesFolder.path ="/";
            imageFolders.add(0, allImagesFolder);
        }
        ImagePicker.getInstance().setImageFolders(imageFolders);
        loadedListener.onImagesLoaded(imageFolders);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        System.out.println("--------");
    }

    public interface OnImagesLoadedListener {
        void onImagesLoaded(List<ImageFolder> imageFolders);
    }
}
