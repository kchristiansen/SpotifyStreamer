package com.example.android.spotifystreamer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;

import java.io.InputStream;
import java.net.URL;

/**
 * Created by kchristiansen on 7/2/15.
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    Bitmap mBitmap;
    BaseAdapter mBaseAdapter;

    public DownloadImageTask(Bitmap bitmap, BaseAdapter baseAdapter) {
        this.mBitmap = bitmap;
        this.mBaseAdapter = baseAdapter;
    }

    @Override
    protected Bitmap doInBackground(String... urls) {
        if (urls == null) return null;
        String urldisplay = urls[0];
        if (urldisplay == null) return null;
        Bitmap mIcon = null;
        try {
            InputStream in = new URL(urldisplay).openStream();
            mIcon = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", "Error downloading image... " + urldisplay);
            e.printStackTrace();
        }
        return mIcon;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            this.mBitmap = bitmap;
            mBaseAdapter.notifyDataSetChanged();
        }
    }

}
