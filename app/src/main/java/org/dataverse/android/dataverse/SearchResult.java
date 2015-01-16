package org.dataverse.android.dataverse;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchResult implements Parcelable {

    String name;
    String url;
    String imageUrl;

    public SearchResult(String name, String url, String imageUrl) {
        this.name = name;
        this.url = url;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(url);
        dest.writeString(imageUrl);
    }
}
