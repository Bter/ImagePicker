package com.lzy.imagepicker.bean;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class ImageItem implements Serializable, Parcelable {

    public String name;
    public long size;
    public int width;
    public int height;
    public String mimeType;
    public long addTime;
    public Uri uri;



    @Override
    public boolean equals(Object o) {
        if (o instanceof ImageItem) {
            ImageItem item = (ImageItem) o;
            if(uri!=null){
                //https://github.com/CysionLiu/ImagePicker/issues/78
                return this.uri.equals(item.uri);
            }
            return super.equals(o);
        }

        return super.equals(o);
    }


    @NonNull
    @Override
    public String toString() {
        return "name: "+name+
                ", size: "+size+
                ", width: "+width+
                ", uri: "+uri.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeLong(this.size);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeString(this.mimeType);
        dest.writeLong(this.addTime);
        dest.writeParcelable(uri,0);
    }

    public ImageItem() {
    }

    protected ImageItem(Parcel in) {
        this.name = in.readString();
        this.size = in.readLong();
        this.width = in.readInt();
        this.height = in.readInt();
        this.mimeType = in.readString();
        this.addTime = in.readLong();
        this.uri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Parcelable.Creator<ImageItem> CREATOR = new Parcelable.Creator<ImageItem>() {
        @Override
        public ImageItem createFromParcel(Parcel source) {
            return new ImageItem(source);
        }

        @Override
        public ImageItem[] newArray(int size) {
            return new ImageItem[size];
        }
    };
}
