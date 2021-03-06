/*
 * Copyright (C) 2017  Iddo Hoeve
 *
 * Squore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.doubleyellow.scoreboard.feed;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.*;
import android.widget.*;

import com.doubleyellow.android.util.SearchEL;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.scoreboard.view.ExpandableListUtil;
import com.doubleyellow.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Activity that allows the user to select a item (in this case a feed) from an internet feed
 * and add this feed the the list of feeds this app can choose from with FeedMatchSelector
 */
public class FeedFeedSelector extends XActivity implements MenuHandler
{
    private static final String TAG = "SB." + FeedFeedSelector.class.getSimpleName();

    private ListView           listView           = null;
    private ExpandableListView expListView        = null;
    private SearchView         etFilter           = null;

    private ShowTypesAdapter   loadTypesAdapter = null;
    //private SimpleELAdapter    loadFeedAdapter  = null;
    private ShowFeedsAdapter   showFeedsAdapter = null;

    //---------------------------------------------------------
    // Progress message
    //---------------------------------------------------------
    private ProgressDialog progressDialog = null;
    private void showProgress(int iResId) {
        if ( progressDialog == null ) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(iResId));
        try {
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void hideProgress() {
        if ( m_bUseSwipeContainer ) {
            m_srlListView   .setRefreshing(false);
            m_srlExpListView.setRefreshing(false);
        }
        if ( progressDialog != null ) {
            try {
                progressDialog.cancel(); // this fails after a rotate of orientation
                progressDialog.dismiss();
            } catch (Exception e) {
                progressDialog = null;
                //e.printStackTrace();
            }
        }
    }

    private static final boolean m_bUseSwipeContainer = true;
    private SwipeRefreshLayout m_srlListView    = null; // should only contain a single listview/gridview
    private SwipeRefreshLayout m_srlExpListView = null;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        etFilter = new SearchView(this);
        etFilter.setIconifiedByDefault(false);
        etFilter.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        etFilter.setVisibility(View.GONE);

        listView    = new ListView(this);
        expListView = new ExpandableListView(this);
        ll.addView(etFilter, 0);
        if ( m_bUseSwipeContainer ) {
            m_srlListView    = new SwipeRefreshLayout(this);
            m_srlExpListView = new SwipeRefreshLayout(this);
            m_srlListView   .addView(listView);
            m_srlExpListView.addView(expListView);
            final SwipeRefreshLayout.OnRefreshListener listener = new SwipeRefreshLayout.OnRefreshListener() {
                @Override public void onRefresh() {
                    FeedFeedSelector.this.handleMenuItem(R.id.refresh);
                }
            };
            m_srlListView   .setOnRefreshListener(listener);
            m_srlExpListView.setOnRefreshListener(listener);
            ll.addView(m_srlListView);
            ll.addView(m_srlExpListView);
        } else {
            ll.addView(expListView);
            ll.addView(listView);
        }
        changeStatus(Status.LoadingTypes);

        super.setContentView(ll);

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));

        loadTypesAdapter = new ShowTypesAdapter(this, childClicker, childClicker);
        listView.setAdapter(loadTypesAdapter);

        //loadFeedAdapter = new LoadFeedAdapter(this, getString(R.string.fetching_data));
        //expListView.setAdapter(loadFeedAdapter);
        expListView.setOnGroupExpandListener  (groupStatusRecaller);
        expListView.setOnGroupCollapseListener(groupStatusRecaller);
        expListView.setOnChildClickListener   (childClicker);
        expListView.setOnItemLongClickListener(childClicker);
        //expListView.setVisibility(View.GONE);

        ColorPrefs.setColors(this, expListView);
        ColorPrefs.setColors(this, etFilter);
    }

    @Override public boolean handleMenuItem(int menuItemId, Object... ctx) {
        switch (menuItemId) {
            case android.R.id.home: // contains a 'back arrow' so treat it like the back button
                onBackPressed();
                break;
            case R.id.close:
            case R.id.cmd_cancel:
                finish();
                return true;
            case R.id.refresh:
                switch (m_status) {
                    case SelectType:
                        loadTypesAdapter.load(false);
                        break;
                    case SelectFeed:
                        showFeedsAdapter.load(false);
                        break;
                }

                //expListView.setAdapter(loadFeedAdapter);
                //loadFeedAdapter.load(false);
                return true;
            case R.id.expand_all:
                ExpandableListUtil.expandAll(expListView);
				return true;
            case R.id.collapse_all:
                ExpandableListUtil.collapseAll(expListView);
				return true;
            case R.id.filter:
                initFiltering();
                return true;
        }
        return false;
    }

    private Status m_status = Status.SelectType;
    void changeStatus(Status statusNew) {
        boolean bShowFilter = statusNew.equals(Status.SelectFeed);
        ViewUtil.setMenuItemsVisibility(menu, new int[]{R.id.filter}, bShowFilter);

        switch (statusNew) {
            case LoadingTypes:
                showProgress(R.string.loading);
                childClicker.setDisabled(true);
                if ( m_bUseSwipeContainer ) {
                    m_srlListView   .setVisibility(View.VISIBLE);
                    m_srlExpListView.setVisibility(View.GONE);
                } else {
                    listView   .setVisibility(View.VISIBLE);
                    expListView.setVisibility(View.GONE);
                }
                break;
            case LoadingFeeds:
                showProgress(R.string.loading);
                childClicker.setDisabled(true);
                if ( m_bUseSwipeContainer ) {
                    m_srlExpListView.setVisibility(View.VISIBLE);
                    m_srlListView   .setVisibility(View.GONE);
                } else {
                    expListView  .setVisibility(View.VISIBLE);
                    listView     .setVisibility(View.GONE);
                }
                break;
            case SelectFeed:
                childClicker.setDisabled(false);
                hideProgress();
                break;
            case SelectType:
                ExpandableListUtil.expandAllOrFirst(expListView, 20);
                childClicker.setDisabled(false);
                hideProgress();
                break;
        }

        m_status = statusNew;
    }

    void postLoad(String sRoot, List<String> lGroupsWithActive) {
        // adapter is now loaded, let the gui know
        expListView.setAdapter(showFeedsAdapter);
        showFeedsAdapter.notifyDataSetChanged();

        this.childClicker.setDisabled(false);
        groupStatusRecaller.setMode(sRoot + "." + m_status);
        int iExpandedAfterRestore = ExpandableListUtil.restoreStatus(expListView, groupStatusRecaller);
        if ( iExpandedAfterRestore <= 0 ) {
            ExpandableListUtil.collapseAll(expListView);
            if ( ListUtil.isNotEmpty(lGroupsWithActive) /*&& ListUtil.size(lGroupsWithActive) < 4*/ ) {
                ExpandableListUtil.expandGroups(expListView, lGroupsWithActive);
            } else {
                ExpandableListUtil.expandAllOrFirst(expListView, 4);
            }
        }

        changeStatus(com.doubleyellow.scoreboard.feed.Status.SelectFeed);
    }

    private Menu menu = null;
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feedselector, menu);
        this.menu = menu;

        ViewUtil.setMenuItemsVisibility(menu, new int[]{R.id.filter}, m_status.equals(Status.SelectFeed));

        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        handleMenuItem(item.getItemId());
        return true;
    }

    private GroupStatusRecaller groupStatusRecaller = GroupStatusRecaller.getInstance(FeedFeedSelector.class.getSimpleName());
    private ChildClicker childClicker = new ChildClicker();

    private class ChildClicker implements ExpandableListView.OnChildClickListener, AdapterView.OnItemLongClickListener, View.OnClickListener, View.OnLongClickListener
    {
        private boolean bDisabled = false;
        void setDisabled(boolean b) {
            this.bDisabled = b;
        }

        @Override public void onClick(View v) {
          //Log.d(TAG, "Implement onClick");
            String sName = (String) v.getTag();

            JSONArray array = loadTypesAdapter.joRoot.optJSONArray(sName);
            if ( JsonUtil.isNotEmpty(array) ) {
                PreferenceValues.addFeedTypeToMyList(FeedFeedSelector.this, sName);
                changeStatus(Status.LoadingFeeds);
                showFeedsAdapter = new ShowFeedsAdapter(FeedFeedSelector.this, FeedFeedSelector.this.getLayoutInflater(), array, sName);
            } else {
                Toast.makeText(FeedFeedSelector.this, "No feeds in " + sName + " ... yet...", Toast.LENGTH_SHORT).show();
            }
        }

        @Override public boolean onLongClick(View v) {
          //Log.d(TAG, "Implement onLongClick");
            String sName = (String) v.getTag();
            JSONArray jsonArray = loadTypesAdapter.joRoot.optJSONArray(sName);
            AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(FeedFeedSelector.this);
            StringBuilder sb = new StringBuilder();
            sb.append(sName).append(" (").append(jsonArray.length()).append(")").append("\n\n");

            JSONObject joType = loadTypesAdapter.joMetaData.optJSONObject(sName);
            for( FeedKeys key: FeedKeys.values() ) {
                if ( key.equals(FeedKeys.Image    ) ) { continue; }
                if ( key.equals(FeedKeys.BGColor  ) ) { continue; }
                if ( key.equals(FeedKeys.TextColor) ) { continue; }
                String sValue = joType.optString(key.toString());
                if ( StringUtil.isNotEmpty(sValue) ) {
                    sb.append(key).append(": ").append(sValue).append("\n\n");
                }
            }
            ab.setMessage(sb.toString())
                    .setIcon(R.drawable.ic_action_web_site)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true; // so that onclick is not also triggered
        }

        @Override public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            if ( this.bDisabled ) {
                return false;
            }
            switch (m_status) {
                case SelectType:
/*
                    String sName             =             loadFeedAdapter.getGroup(groupPosition).toString();
                    showFeedsAdapter.m_feeds = (JSONArray) loadFeedAdapter.getObject(groupPosition, childPosition);
                    if (showFeedsAdapter.m_feeds == null) {
                        return false;
                    }
                    changeStatus(Status.LoadingFeeds);
                    loadFeedAdapter.clear();
                    loadFeedAdapter.addItem(getString(R.string.loading), sName);
                    loadFeedAdapter.notifyDataSetChanged();
                    ExpandableListUtil.expandFirst(expListView);

                    showFeedsAdapter = new ShowFeedsAdapter(FeedFeedSelector.this, getLayoutInflater(), sName);
*/
                    return false;
                case SelectFeed:
                    JSONObject joFeed = (JSONObject) showFeedsAdapter.getObject(groupPosition, childPosition);
                    if (joFeed == null) {
                        return false;
                    }
                    Map<URLsKeys, String> newEntry = new HashMap<URLsKeys, String>();
                    for ( URLsKeys key : URLsKeys.values() ) {
                        String sValue = joFeed.optString(key.toString());
                        if (StringUtil.isNotEmpty(sValue)) {
                            newEntry.put(key, sValue);
                        }
                    }
                    String sNewURL = newEntry.get(URLsKeys.FeedMatches);
                    sNewURL = URLFeedTask.prefixWithBaseIfRequired(sNewURL);
                    Map<String, String> existingUrls2Name = PreferenceValues.getFeedPostDetailMap(FeedFeedSelector.this, URLsKeys.FeedMatches, URLsKeys.Name, true);
                    if ( existingUrls2Name.containsKey(sNewURL) ) {
                        String sMsg = getString(R.string.feed_x_already_exist_with_name_y, sNewURL, existingUrls2Name.get(sNewURL));
                        ScoreBoard.dialogWithOkOnly(FeedFeedSelector.this, sMsg);
                        return false;
                    }

                    PreferenceValues.addOrReplaceNewFeedURL(FeedFeedSelector.this, newEntry, true, true);

                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long id) {
            switch (m_status) {
                case SelectType:
                    return false;
                case SelectFeed:
                    Object itemAtPosition = adapterView.getItemAtPosition(i);
                    if (itemAtPosition instanceof JSONObject) {
                        JSONObject joFeed = (JSONObject) itemAtPosition;
                        StringBuilder sb = new StringBuilder(256);
                        for (URLsKeys key : URLsKeys.values()) {
                            String sValue = joFeed.optString(key.toString());
                            if (StringUtil.isNotEmpty(sValue)) {
                                sb.append(StringUtil.capitalize(key)).append(" : ").append(sValue).append("\n\n");
                            }
                        }

                        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(FeedFeedSelector.this);
                        ab.setMessage(sb.toString())
                                .setIcon(R.drawable.ic_action_web_site)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        }
    }

    @Override public void onBackPressed() {
        if ( etFilter.getVisibility() == View.VISIBLE ) {
            //etFilter.setVisibility(View.GONE);
            searchEL.toggleSearchViewVisibility();
            return;
        }
        switch (m_status) {
            case SelectType:
                //just leave the activity
                super.onBackPressed();
                break;
            case SelectFeed:
                changeStatus(Status.LoadingTypes);

                if ( m_bUseSwipeContainer ) {
                    m_srlListView   .setVisibility(View.VISIBLE);
                    m_srlExpListView.setVisibility(View.GONE);
                } else {
                    listView   .setVisibility(View.VISIBLE);
                    expListView.setVisibility(View.GONE);
                }
                listView.setAdapter(loadTypesAdapter);
                loadTypesAdapter.notifyDataSetChanged();

                groupStatusRecaller.setMode(m_status.toString());
                int iExpandedAfterRestore = ExpandableListUtil.restoreStatus(expListView, groupStatusRecaller);
                if ( iExpandedAfterRestore <= 0 ) {
                    ExpandableListUtil.collapseAll(expListView);
                }
                changeStatus(Status.SelectType);

                break;
        }
    }

    // ------------------------------------------
    // Filtering
    // ------------------------------------------

    private SearchEL searchEL = null;
    private void initFiltering() {
        if ( searchEL != null ) {
            searchEL.toggleSearchViewVisibility();
            return;
        }

        searchEL = new SearchEL(this, showFeedsAdapter, etFilter, expListView, true);
    }

}
