package com.example.pieter_jan.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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


    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemTask().execute();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photo_gallery,container,false);

        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));


        //Challenge multiple pages
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                PhotoAdapter photoAdapter = (PhotoAdapter) recyclerView.getAdapter();
                int lastPosition = photoAdapter.getLastBoundPosition();

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                int loadBufferPosition = 1;
                if(lastPosition >= photoAdapter.getItemCount() - layoutManager.getSpanCount()- loadBufferPosition){
                    new FetchItemTask().execute(lastFetchedPage + 1);
                }

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });


        setupAdapter();

        return view;
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

    @Override
    protected List<GalleryItem> doInBackground(Integer... params) {
        return new FlickrFetchr().fetchItems(lastFetchedPage);
    }

        /*
        Update the UI after execution, updating UI is only allowed on UI thread
         */
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
//            mItems = items;
//            setupAdapter();

            // Multiple pages challenge
            if(lastFetchedPage >1){
                mItems.addAll(items);
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            }
            else{
                mItems = items;
                setupAdapter();
            }
            lastFetchedPage++;
        }

        }


    /*
    ViewHolder class
     */
    private class PhotoHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView;
        }

        // Used to bind the adapter to the viewholder
        public void bindGalleryItem(GalleryItem item){
            mTitleTextView.setText(item.toString());
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
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);

            lastBoundPosition = position;
            Log.i(TAG,"Last bound position is " + Integer.toString(lastBoundPosition)); // Check what the last position is in the recyclerview
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
