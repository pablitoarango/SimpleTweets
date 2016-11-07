package com.whospablo.simpletweets.ui.profile;


import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.whospablo.simpletweets.SimpleTweetsApplication;
import com.whospablo.simpletweets.services.TwitterClient;
import com.whospablo.simpletweets.ui.home.HomeActivity;
import com.whospablo.simpletweets.util.EndlessRecyclerViewScrollListener;
import com.whospablo.simpletweets.util.adapters.TweetsAdapter;
import com.whospablo.simpletweets.util.fragments.RecyclerFragment;
import com.whospablo.simpletweets.util.models.Tweet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by pablo_arango on 11/2/16.
 */

public class UserTimelineFragment extends RecyclerFragment implements HomeActivity.RefreshableFragment {
    private TwitterClient mClient;
    private List<Tweet> mTweets;
    private TweetsAdapter mTweetsAdapter;
    private HomeActivity.OnRefreshDoneListener mListener;
    private String user_handle;
    private boolean exclude_replies;

    public static UserTimelineFragment newInstance(String user_hadle, boolean exclude_replies) {
        UserTimelineFragment fragmentDemo = new UserTimelineFragment();
        Bundle args = new Bundle();
        args.putString("user_hadle", user_hadle);
        args.putBoolean("exclude_replies", exclude_replies);
        fragmentDemo.setArguments(args);
        return fragmentDemo;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user_handle = getArguments().getString("user_hadle", "");
        exclude_replies = getArguments().getBoolean("exclude_replies", true);
        mClient = SimpleTweetsApplication.getRestClient();
        mTweets = new ArrayList<>();
        mTweetsAdapter = new TweetsAdapter(getContext(), mTweets);
        loadRecent();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setAdapter(mTweetsAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        setLayoutManager(llm);
        getRecyclerView().addOnScrollListener(new EndlessRecyclerViewScrollListener(llm) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                loadMore();
            }
        });
    }

    private void loadMore(){
        long lastTweetid = Long.MAX_VALUE;

        if(mTweets.size()>0){
            lastTweetid = mTweets.get(mTweets.size()-1).id-1;
        }
        mClient.getUserTimelineBefore(user_handle, exclude_replies, lastTweetid, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                mTweets.addAll(Tweet.fromJSONArray(response));
                mTweetsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    private void loadRecent(){
        long mostRecentId = 1;

        if(mTweets.size()>0){
            mostRecentId = mTweets.get(0).id;
        }

        mClient.getUserTimelineSince(user_handle, exclude_replies, mostRecentId, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                List<Tweet> newTweets = Tweet.fromJSONArray(response);
                for(int i = newTweets.size()-1; i>=0; i--){
                    mTweets.add(0,newTweets.get(i));
                }
                mTweetsAdapter.notifyDataSetChanged();
                doneRefreshing();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                doneRefreshing();
            }
        });
    }


    private void doneRefreshing(){
        if(mListener!=null)
            mListener.refreshDone();
    }

    @Override
    public void refresh(HomeActivity.OnRefreshDoneListener listener) {
        mListener = listener;
        loadRecent();
    }
}
