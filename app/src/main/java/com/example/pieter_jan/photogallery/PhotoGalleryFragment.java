package com.example.pieter_jan.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pieter-jan on 12/6/2016.
 */

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();

    // Challenge multiple pages
    private int lastFetchedPage = 1;
    private ThumbnailDownloader mThumbnailDownloader;


    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();


        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>(){
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                
                collapseSearchView(); //challenge

                // Alternative solution to challenge (many alternative solutions available)
//                searchView.clearFocus(); // this collapses the keyboard
//                searchView.setVisibility(View.GONE); // this hides the search button


                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange: " + newText);
                return false;
            }

            private void collapseSearchView() {
                searchItem.collapseActionView(); // collapse the action view
                View view = getActivity().getCurrentFocus(); // hide the soft keyboard
                if (view != null){
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        // Set the previously entered query in the searchview
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                updateItems();
                return true;

            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu(); // update toolbar
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());

        if (mPhotoRecyclerView != null){
            mItems.clear();
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
        }

        new FetchItemTask(query).execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mThumbnailDownloader.quit(); // this is critical! otherwise the thread will never die!
        Log.i(TAG, "Background thread destroyed");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photo_gallery,container,false);

        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));


//        //Challenge multiple pages
//        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                PhotoAdapter photoAdapter = (PhotoAdapter) recyclerView.getAdapter();
//                int lastPosition = photoAdapter.getLastBoundPosition();
//
//                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
//                int loadBufferPosition = 1;
//                if(lastPosition >= photoAdapter.getItemCount() - layoutManager.getSpanCount()- loadBufferPosition){
//                    new FetchItemTask().execute(lastFetchedPage + 1);
//                }
//
//            }
//
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//            }
//        });


        setupAdapter();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue(); // Clear the queue in case of screen rotation
    }

    /*
        Set up the adapter to the recyclerview
         */
    private void setupAdapter(){
        if(isAdded()){ // confirms that the fragment has been added to an activity
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    /*
    Class to create a background thread for the network connection, networking is not allowed on the main thread (UI thread).
     */
    public class FetchItemTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        private String mQuery;

        public FetchItemTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {

            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        /*
        Update the UI after execution, updating UI is only allowed on UI thread
         */
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();

//            // Multiple pages challenge
//            if(lastFetchedPage >1){
//                mItems.addAll(items);
//                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
//            }
//            else{
//                mItems = items;
//                setupAdapter();
//            }
//            lastFetchedPage++;
        }

    }


    /*
    ViewHolder class
     */
    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        // Used to bind the image to the viewholder
        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }

    }

    /**
     * Adapter class
     */
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        private List<GalleryItem> mGalleryItems;
        private int lastBoundPosition;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeHolder = getResources().getDrawable(R.drawable.bill_up_close);
            holder.bindDrawable(placeHolder);

            mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());

            // Challenge multiple pages
//            lastBoundPosition = position;
//            Log.i(TAG,"Last bound position is " + Integer.toString(lastBoundPosition)); // Check what the last position is in the recyclerview
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        // Challenge mutliple pages
        public int getLastBoundPosition() {
            return lastBoundPosition;
        }
    }


}
