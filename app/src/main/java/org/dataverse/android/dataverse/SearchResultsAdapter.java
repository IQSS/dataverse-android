package org.dataverse.android.dataverse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SearchResultsAdapter extends ArrayAdapter<SearchResult> {
    public SearchResultsAdapter(Context context, List<SearchResult> objects) {
        super(context, R.layout.search_result, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View searchResultView = layoutInflater.inflate(R.layout.search_result, parent, false);
        SearchResult searchResult = getItem(position);
        TextView searchResultTextView = (TextView) searchResultView.findViewById(R.id.searchResultTextView);
        searchResultTextView.setText(searchResult.getName());
        ImageView searchResultImageView = (ImageView) searchResultView.findViewById(R.id.searchResultImageView);
        new DownloadImageTask(searchResultImageView).execute(searchResult.getImageUrl());
        return searchResultView;
    }

    /**
     * This class is inspired by
     * http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
     * but it would be good to consider
     * http://developer.android.com/training/displaying-bitmaps/index.html
     */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        ImageView imageView;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... params) {
            String urlToFetch = params[0];
            Bitmap bitmap = null;
            InputStream inputStream = null;
            try {
                inputStream = new java.net.URL(urlToFetch).openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

}
