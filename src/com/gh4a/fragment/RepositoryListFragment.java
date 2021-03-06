/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class RepositoryListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<Repository>>, OnItemClickListener, OnScrollListener {

    private String mLogin;
    private String mUserType;
    private String mRepoType;
    private ListView mListView;
    private RepositoryAdapter mAdapter;
    private PageIterator<Repository> mDataIterator;
    private boolean isLoadMore;
    private boolean isLoadCompleted;
    private TextView mLoadingView;
    
    public static RepositoryListFragment newInstance(String login, String userType, String repoType) {
        RepositoryListFragment f = new RepositoryListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putString(Constants.User.USER_TYPE, userType);
        args.putString(Constants.Repository.REPO_TYPE, repoType);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mUserType = getArguments().getString(Constants.User.USER_TYPE);
        mRepoType = getArguments().getString(Constants.Repository.REPO_TYPE);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        LayoutInflater vi = getSherlockActivity().getLayoutInflater();
        mLoadingView = (TextView) vi.inflate(R.layout.row_simple, null);
        mLoadingView.setText("Loading...");
        mLoadingView.setTextColor(Color.parseColor("#0099cc"));
        
        mAdapter = new RepositoryAdapter(getSherlockActivity(), new ArrayList<Repository>());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (!isLoadCompleted) {
            loadData();
            
            if (getLoaderManager().getLoader(0) == null) {
                getLoaderManager().initLoader(0, null, this);
            }
            else {
                getLoaderManager().restartLoader(0, null, this);
            }
            getLoaderManager().getLoader(0).forceLoad();
        }
    }

    public void loadData() {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        RepositoryService repoService = new RepositoryService(client);
        
        Map<String, String> filterData = new HashMap<String, String>();
        if ("sources".equals(mRepoType) || "forks".equals(mRepoType)) {
            filterData.put("type", "all");
        }
        else {
            filterData.put("type", mRepoType);
        }
        
        if (mLogin.equals(app.getAuthLogin())) {
            mDataIterator = repoService.pageRepositories(filterData, 100);
        }
        else if (Constants.User.USER_TYPE_ORG.equals(mUserType)) {
            mDataIterator = repoService.pageOrgRepositories(mLogin, new HashMap<String, String>());
        }
        else {
            mDataIterator = repoService.pageRepositories(mLogin, filterData, 100);
        }
    }
    
    private void fillData(List<Repository> repositories) {
        if (repositories != null && !repositories.isEmpty()) {
            if (mListView.getFooterViewsCount() == 0) {
                mListView.addFooterView(mLoadingView);
                mListView.setAdapter(mAdapter);
            }
            if (isLoadMore) {
                if ("sources".equals(mRepoType) || "forks".equals(mRepoType)) {
                    for (Repository repository : repositories) {
                        if ("sources".equals(mRepoType) && !repository.isFork()) {
                            mAdapter.add(repository);
                        }
                        else if ("forks".equals(mRepoType) && repository.isFork()) {
                            mAdapter.add(repository);
                        }
                    }
                }
                else {
                    mAdapter.addAll(mAdapter.getCount(), repositories);  
                }
                mAdapter.notifyDataSetChanged();
            }
            else {
                mAdapter.clear();
                if ("sources".equals(mRepoType) || "forks".equals(mRepoType)) {
                    for (Repository repository : repositories) {
                        if ("sources".equals(mRepoType) && !repository.isFork()) {
                            mAdapter.add(repository);
                        }
                        else if ("forks".equals(mRepoType) && repository.isFork()) {
                            mAdapter.add(repository);
                        }
                    }
                }
                else {
                    mAdapter.addAll(repositories);
                }
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(0);
            }
        }
        else {
            mListView.removeFooterView(mLoadingView);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Gh4Application context = ((BaseSherlockFragmentActivity) getActivity()).getApplicationContext();
        Repository repository = (Repository) adapterView.getAdapter().getItem(position);
        
        Intent intent = new Intent()
                .setClass(getActivity(), RepositoryActivity.class);
        Bundle data = context.populateRepository(repository);
        intent.putExtra(Constants.DATA_BUNDLE, data);
        startActivity(intent);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {

        boolean loadMore = firstVisible + visibleCount >= totalCount;

        if(loadMore) {
            if (getLoaderManager().getLoader(0) != null
                    && isLoadCompleted) {
                isLoadMore = true;
                isLoadCompleted = false;
                getLoaderManager().getLoader(0).forceLoad();
            }
        }
    }
    
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}
    
    @Override
    public Loader<List<Repository>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<Repository>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<Repository>> loader, List<Repository> repositories) {
        isLoadCompleted = true;
        hideLoading();
        fillData(repositories);
    }

    @Override
    public void onLoaderReset(Loader<List<Repository>> arg0) {
        // TODO Auto-generated method stub
        
    }
}