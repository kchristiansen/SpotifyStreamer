package com.example.android.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistTopTracksFragment extends Fragment {

    public ArtistTopTracksFragment() {
    }

    TrackInfoAdapter mTopTrackAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_top_tracks, container, false);

        ArrayList<TrackInfo> trackItems = new ArrayList<>();

        mTopTrackAdapter = new TrackInfoAdapter(getActivity(), trackItems);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_top_tracks);
        listView.setAdapter(mTopTrackAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Context context = getActivity();
                int duration = Toast.LENGTH_SHORT;
                String text = ((TrackInfo)mTopTrackAdapter.getItem(position)).trackName;
                Toast toast = Toast.makeText(context, "Track name: " + text, duration);
                toast.show();
            }
        });
        return rootView;
    }

    public class TrackInfo{
        String albumName;
        String trackName;
        String albumUrl;
        Bitmap albumArt;
    }

    public class TrackInfoAdapter extends BaseAdapter {
        ArrayList<TrackInfo> mTrackItems;
        Context mContext;

        public TrackInfoAdapter(Context context, ArrayList<TrackInfo> trackItems) {
            mTrackItems=trackItems;
            mContext=context;
        }

        @Override
        public int getCount() {
            if(mTrackItems!=null){
                return mTrackItems.size();
            }
            return 0;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mTrackItems!=null && mTrackItems.size()>position){
                return mTrackItems.get(position);
            }
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(mTrackItems!=null && mTrackItems.size()>position){
                LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                if(convertView==null)
                {
                    convertView = mInflater.inflate(R.layout.list_item_track, null);
                }

                TrackInfo trackInfo = mTrackItems.get(position);

                ((TextView) convertView.findViewById(R.id.album_name)).setText(trackInfo.albumName);
                ((TextView) convertView.findViewById(R.id.track_name)).setText(trackInfo.trackName);
                if(trackInfo.albumArt==null) {
                    ((ImageView)convertView.findViewById(R.id.album_image)).setImageResource(R.mipmap.default_artist_image);
                    DownloadImageTask imageTask = new DownloadImageTask(trackInfo.albumArt, this);
                    imageTask.execute(trackInfo.albumUrl);
                }

            }
            return null;
        }

    }

    public class TopTrackTask extends AsyncTask<String,Void,ArrayList<TrackInfo>>{
        private final String LOG_TAG = TopTrackTask.class.getSimpleName();
        @Override
        protected ArrayList<TrackInfo> doInBackground(String... params) {
            final String SPOTIFY_BASE_URL = "https://api.spotify.com/v1/artists/"+ params[0] +"/top-tracks";
            Uri builtUri = Uri.parse(SPOTIFY_BASE_URL).buildUpon()
                    .appendQueryParameter("country", "US")
                    .build();

            String spotifyJsonStr = JsonDataFetch.fetchJson(builtUri);

            try{
                return getTopTrackInfoFromJson(spotifyJsonStr);
            }
            catch(JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<TrackInfo> trackInfos) {
            super.onPostExecute(trackInfos);
        }

        protected ArrayList<TrackInfo> getTopTrackInfoFromJson(String spotifyJsonStr) throws JSONException{
            final String NAME = "name";
            final String IMAGES = "images";
            final String TRACKS = "tracks";
            final String ITEMS = "items";
            final String ALBUM = "album";
            final String SPOTIFY_IMAGE_HEIGHT = "height";
            final String SPOTIFY_IMAGE_URL = "url";

            JSONArray tracks = new JSONObject(spotifyJsonStr).getJSONArray(TRACKS);
            ArrayList<TrackInfo> topTracks = new ArrayList<>();

            for(int i=0;i< tracks.length();i++){
                JSONObject track = tracks.getJSONObject(i);
                TrackInfo info = new TrackInfo();
                info.trackName = track.getString(NAME);

                JSONObject album = track.getJSONObject(ALBUM);

                info.albumName = album.getString(NAME);

                JSONArray imageArray = album.getJSONArray(IMAGES);
                int minHeight=1000;
                int imageIndex=imageArray.length()-1;

                for(int j=0;j<imageArray.length();j++){
                    int height = imageArray.getJSONObject(j).getInt(SPOTIFY_IMAGE_HEIGHT);
                    String url = imageArray.getJSONObject(j).getString(SPOTIFY_IMAGE_URL);
                    if(height<minHeight && url!=null){
                        minHeight=height;
                        imageIndex = j;
                    }
                }

                if(imageIndex>0) {
                    info.albumUrl = imageArray.getJSONObject(imageIndex).getString(SPOTIFY_IMAGE_URL);
                }
                topTracks.add(info);
            }
            return topTracks;
        }
    }
}
