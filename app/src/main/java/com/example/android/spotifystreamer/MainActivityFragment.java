package com.example.android.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    ArtistInfoAdapter mArtistAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ArrayList<ArtistInfo> artistItems = new ArrayList<>();

        mArtistAdapter = new ArtistInfoAdapter(getActivity(), artistItems);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_artists);
        listView.setAdapter(mArtistAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Context context = getActivity();
                int duration = Toast.LENGTH_SHORT;
                String text = ((ArtistInfo)mArtistAdapter.getItem(position)).name;
                Toast toast = Toast.makeText(context, "Artist name: " + text, duration);
                toast.show();
            }
        });
        final Button searchButton = (Button) rootView.findViewById(R.id.search_button);
        final TextView textView = (TextView) rootView.findViewById(R.id.edit_box);
        searchButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getActivity();
                CharSequence searchTerm = textView.getText();
                updateArtistList(searchTerm.toString());
                InputMethodManager inputManager = (InputMethodManager)context
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(textView.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }
        });
        return rootView;
    }


    public void updateArtistList(String searchParam){
        new FetchArtistTask().execute(searchParam);
    }

    public class ArtistInfoAdapter extends BaseAdapter{
        ArrayList<ArtistInfo> artistInfos;
        Context context;

        public ArtistInfoAdapter(Context context, ArrayList<ArtistInfo> artistInfos){
            this.context = context;
            this.artistInfos = artistInfos;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if(artistInfos==null) return null;
            return artistInfos.get(position);
        }

        @Override
        public int getCount() {
            if(artistInfos==null) return 0;
            return artistInfos.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if(convertView==null){
                convertView = mInflater.inflate(R.layout.list_item_artist, null);
            }

            ArtistInfo artistInfo = artistInfos.get(position);

            ((TextView) convertView.findViewById(R.id.artist_name)).setText(artistInfo.name);

            if(artistInfo.image==null) {
                ((ImageView) convertView.findViewById(R.id.artist_image)).setImageResource(R.mipmap.default_artist_image);

                DownloadImageTask imageTask = new DownloadImageTask(artistInfo.image, this);
                imageTask.execute(artistInfo.imageUrl);
            }
            else{
                ((ImageView) convertView.findViewById(R.id.artist_image)).setImageBitmap(artistInfo.image);
            }

            return convertView;
        }

        public void clear() {
            if(artistInfos==null) {
                artistInfos = new ArrayList<ArtistInfo>();
            }
            else {
                artistInfos.clear();
            }
        }
        public void add(ArtistInfo info){
            if(artistInfos==null) {
                artistInfos = new ArrayList<ArtistInfo>();
            }
            artistInfos.add(info);
        }
    }

    public class ArtistInfo {
        public String id;
        public String name;
        public String imageUrl;
        public Bitmap image;
    }

    public class FetchArtistTask extends AsyncTask<String,Void,ArrayList<ArtistInfo>> {
        private final String LOG_TAG = FetchArtistTask.class.getSimpleName();
        @Override
        protected ArrayList<ArtistInfo> doInBackground(String... params) {
            final String SPOTIFY_BASE_URL = "https://api.spotify.com/v1/search";
            Uri builtUri = Uri.parse(SPOTIFY_BASE_URL).buildUpon()
                    .appendQueryParameter("q", params[0])
                    .appendQueryParameter("type", "artist")
                    .build();

            String spotifyJsonStr = JsonDataFetch.fetchJson(builtUri);

            try{
                return getArtistInfoFromJson(spotifyJsonStr);
            }
            catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(),e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<ArtistInfo> artistInfos) {
            if(artistInfos!=null){
                mArtistAdapter.clear();
                for(ArtistInfo info :artistInfos){
                    mArtistAdapter.add(info);
                }
                mArtistAdapter.notifyDataSetChanged();
            }
        }

        private ArrayList<ArtistInfo> getArtistInfoFromJson(String spotifyJsonStr) throws JSONException {
            final String SPOTIFY_ARTISTS = "artists";
            final String SPOTIFY_ITEMS = "items";
            final String SPOTIFY_ID = "id";
            final String SPOTIFY_IMAGES = "images";
            final String SPOTIFY_NAME = "name";
            final String SPOTIFY_IMAGE_HEIGHT = "height";
            final String SPOTIFY_IMAGE_URL = "url";

            JSONObject artistJson = new JSONObject(spotifyJsonStr).getJSONObject(SPOTIFY_ARTISTS);
            JSONArray artistArray = artistJson.getJSONArray(SPOTIFY_ITEMS);

            ArrayList<ArtistInfo> result = new ArrayList<ArtistInfo>();
            for(int i=0; i< artistArray.length();i++){
                ArtistInfo info = new ArtistInfo();

                JSONObject artist = artistArray.getJSONObject(i);
                info.id = artist.getString(SPOTIFY_ID);
                info.name = artist.getString(SPOTIFY_NAME);
                JSONArray imageArray = artist.getJSONArray(SPOTIFY_IMAGES);
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
                    info.imageUrl = imageArray.getJSONObject(imageIndex).getString(SPOTIFY_IMAGE_URL);
                }
                result.add(info);
            }
            return result;
        }
    }

}

