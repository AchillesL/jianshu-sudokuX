package com.example.sudokux;

import android.os.Parcel;
import android.os.Parcelable;

class LocTextInfo implements Parcelable {
    public int locX, locY;
    public int number;

    public LocTextInfo(int locX, int locY, int number) {
        this.locX = locX;
        this.locY = locY;
        this.number = number;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.locX);
        dest.writeInt(this.locY);
        dest.writeInt(this.number);
    }

    protected LocTextInfo(Parcel in) {
        this.locX = in.readInt();
        this.locY = in.readInt();
        this.number = in.readInt();
    }

    public static final Parcelable.Creator<LocTextInfo> CREATOR = new Parcelable.Creator<LocTextInfo>() {
        @Override
        public LocTextInfo createFromParcel(Parcel source) {
            return new LocTextInfo(source);
        }

        @Override
        public LocTextInfo[] newArray(int size) {
            return new LocTextInfo[size];
        }
    };
}