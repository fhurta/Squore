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

package com.doubleyellow.scoreboard.match;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.*;

import com.doubleyellow.android.view.CountryTextView;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.android.view.EnumSpinner;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.scoreboard.view.PlayerTextView;
import com.doubleyellow.scoreboard.view.PreferenceACTextView;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;
import com.doubleyellow.view.NextFocusDownListener;
import com.doubleyellow.view.SBRelativeLayout;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * MatchView used by both the Match activity and the MatchFragment (MatchTabbed).
 * Used to define the match format.
 */
public class MatchView extends SBRelativeLayout
{
    private NewMatchLayout m_layout = NewMatchLayout.AllFields;

    public MatchView(Context context, boolean bIsDoubles, Model model, NewMatchLayout layout) {
        super(context);
        m_layout = layout;
        init(bIsDoubles, model);
        if ( hideElementsBasedOnSport() == false ) {
            initExpandCollapse();
        }
    }

    void setReferees(String sName, String sMarker) {
        txtRefereeName.setText(sName);
        txtMarkerName .setText(sMarker);
    }

    /** introduced to be able to quickly turn it off during development */
    private boolean requestFocusFor(View v) {
        //if ( true ) { return false; }
        if ( v == null ) {
            return false;
        }
        return v.requestFocus();
    }

    void setEvent(String sName, String sDivision, String sRound, String sLocation, String sCourt, String sSourceID) {
        boolean bFocusOnIfEmpty = false;
        txtEventName.setText(sName);
        if ( ViewUtil.areAllNonEmpty(txtEventName) ) {
            if (bFocusOnIfEmpty) requestFocusFor(txtEventDivision);
        }
        txtEventDivision.setText(sDivision);
        txtEventDivision.useLastValueAsDefault(StringUtil.isEmpty(sName)); // only fill division with previous if name was also not specified
        if ( ViewUtil.areAllNonEmpty(txtEventName, txtEventDivision) ) {
            if (bFocusOnIfEmpty) requestFocusFor(txtEventRound);
        }
        txtEventRound.setText(sRound);
        txtEventRound.useLastValueAsDefault(StringUtil.isEmpty(sName)); // only fill round with previous if name was also not specified
        if ( ViewUtil.areAllNonEmpty(txtEventName, txtEventDivision, txtEventRound) ) {
            if (bFocusOnIfEmpty) requestFocusFor(txtEventLocation);
        }
        txtEventLocation.setText(sLocation);
        txtEventLocation.useLastValueAsDefault(StringUtil.isEmpty(sName)); // only fill location with previous if name was also not specified
        if ( ViewUtil.areAllNonEmpty(txtEventName, txtEventDivision, txtEventRound, txtEventLocation) ) {
            requestFocusFor(txtPlayerA);
        }
        if ( (ViewUtil.areAllEmpty(txtEventName, txtEventDivision, txtEventRound, txtEventLocation) == false) && bEventCollapsed) {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 /* 15 */ ) {
                findViewById(R.id.lblEvent).callOnClick(); // to expand the parent area by default
                bEventCollapsed = true;
            }
        }

        if ( txtCourt != null ) {
            txtCourt.useLastValueAsDefault(StringUtil.isEmpty(sName)); // only fill court with previous if name was also not specified
            txtCourt.setText(sCourt);
        }
        if ( txtEventID != null ) {
            txtEventID.setText(sSourceID);
            int visibility = StringUtil.isEmpty(sSourceID) ? GONE : VISIBLE;
            txtEventID.setVisibility(visibility);
        }
    }

    //private static final String[] sExpandedCollapsed = {"\u2303", "\u2304"};
    private static final String[] sExpandedCollapsed = {"\u2191", "\u2193"}; // up & down
    //private static final String[] sExpandedCollapsed = {"\u21A5", "\u21A7"}; // arrow with a bar
    //private static final String[] sExpandedCollapsed = {"\u2B06", "\u2B07"}; // pretty fat arrow (maybe a little TO in your face)

    private boolean bEventCollapsed     = false;
    private boolean bClubsCollapsed     = false;
    private boolean bCountriesCollapsed = false;
    private void initExpandCollapse() {
        final int[] iToggleClubs = {R.id.match_clubA, R.id.match_clubB};
        bClubsCollapsed = (ViewUtil.editTextsAreAllEmpty(this, iToggleClubs));
        ViewUtil.installExpandCollapse(this, R.id.lblClubs, iToggleClubs, (bClubsCollapsed ? GONE : VISIBLE), sExpandedCollapsed);

        final int[] iToggleCountries = {R.id.match_countryA, R.id.match_countryB};
        bCountriesCollapsed = (ViewUtil.editTextsAreAllEmpty(this, iToggleCountries));
        ViewUtil.installExpandCollapse(this, R.id.lblCountries, iToggleCountries, (bCountriesCollapsed ? GONE : VISIBLE), sExpandedCollapsed);

        final int[] iToggleEventViews = {R.id.ll_match_event_texts, R.id.ll_match_event_texts1, R.id.ll_match_event_texts2};
        bEventCollapsed = (ViewUtil.editTextsAreAllEmpty(this, iToggleEventViews));
        ViewUtil.installExpandCollapse(this, R.id.lblEvent, iToggleEventViews, (bEventCollapsed ? GONE : VISIBLE), sExpandedCollapsed);

        final int[] iToggleRefViews = {R.id.match_referee, R.id.match_marker, R.id.ll_AnnouncementLanguage};
        final boolean bRefTxtsAreEmpty = ViewUtil.editTextsAreAllEmpty(this, iToggleRefViews);
        final boolean bLanguageDeviates = PreferenceValues.announcementLanguageDeviates(getContext());
        int iInitialState = bRefTxtsAreEmpty && (bLanguageDeviates == false) ? GONE : VISIBLE;
        ViewUtil.installExpandCollapse(this, R.id.lblReferee, iToggleRefViews, iInitialState, sExpandedCollapsed);

        List<Integer> lToggleFormatViews = new ArrayList<>();
        lToggleFormatViews.add(R.id.llPoints);
        lToggleFormatViews.add(R.id.llTieBreakFormat);
        lToggleFormatViews.add(R.id.llPauseDuration);
        lToggleFormatViews.add(R.id.llScoringType);
        lToggleFormatViews.add(R.id.llLiveScore);
        if ( m_bIsDoubles ) {
            lToggleFormatViews.add(R.id.ll_doubleServeSequence);
        }
        SportType sportType = Brand.getSport();
        switch (sportType) {
            case Tabletennis:
                lToggleFormatViews.add(R.id.llNumberOfServesPerPlayer);
                break;
            case Racketlon:
                lToggleFormatViews.add(R.id.llDisciplineStart);
                lToggleFormatViews.remove(R.id.llTieBreakFormat);
                break;
            case Squash:
                lToggleFormatViews.add(R.id.useHandInHandOutScoring);
                lToggleFormatViews.add(R.id.llHandicapFormat);
                break;
        }
/*
        int[] iToggleFormatViews = new int[] {R.id.llPoints, R.id.llTieBreakFormat, R.id.llHandicapFormat, R.id.useHandInHandOutScoring, R.id.llPauseDuration, R.id.llDisciplineStart, R.id.llNumberOfServesPerPlayer, R.id.llScoringType, R.id.llLiveScore};
        if ( m_bIsDoubles ) {
            iToggleFormatViews = new int[]   {R.id.llPoints, R.id.llTieBreakFormat, R.id.llHandicapFormat, R.id.useHandInHandOutScoring, R.id.llPauseDuration, R.id.llDisciplineStart, R.id.llNumberOfServesPerPlayer, R.id.llScoringType, R.id.llLiveScore, R.id.ll_doubleServeSequence};
        }
*/
        ViewUtil.installExpandCollapse(this, R.id.lblFormat, lToggleFormatViews, VISIBLE, sExpandedCollapsed);
    }

    private boolean hideElementsBasedOnSport() {
        // hide some elements base on sport
        SportType sportType = Brand.getSport();
        boolean bTrueGoneFalseInvisible = ViewUtil.isPortraitOrientation(getContext()) || m_layout.equals(NewMatchLayout.Simple); // in portrait each element is on a single line, layout is not screwed up when GONE is used. In landscape layout IS screwed up
        switch (sportType) {
            case Tabletennis:
                ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                        , R.id.llHandicapFormat
                        , R.id.llDisciplineStart
                        , R.id.llScoringType
                        , R.id.match_marker
                        , R.id.ll_AnnouncementLanguage
                );
                return true;
            case Racketlon:
                ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                        , R.id.lblMatch_BestOf, R.id.spNumberOfGamesToWin
                        , R.id.llHandicapFormat
                        , R.id.llTieBreakFormat
                        , R.id.llScoringType
                        , R.id.llNumberOfServesPerPlayer
                        , R.id.ll_doubleServeSequence
                        , R.id.match_marker
                        , R.id.ll_AnnouncementLanguage
                );
                return true;
            case Squash:
                ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                        , R.id.llDisciplineStart
                        , R.id.llNumberOfServesPerPlayer
                );
                return false;
            case Racquetball:
                ViewUtil.hideViewsForEver(this
                        , R.id.llHandicapFormat
                        , R.id.llDisciplineStart
                        , R.id.llNumberOfServesPerPlayer
                      //, R.id.llScoringType
                );
                return false;
        }
        return false;
    }

    void setPlayers(String sA, String sB, String sCountryA, String sCountryB, String sAvatarA, String sAvatarB, String sClubA, String sClubB) {
        int iNames = 0;
        if ( StringUtil.isNotEmpty(sA) ) {
            if ( m_bIsDoubles ) {
                String[] saNames = sA.split("/");
                txtPlayerA.setText(saNames[0]);
                if ( saNames.length > 1 ) {
                    txtPlayerA2.setText(saNames[1]);
                }
            } else {
                txtPlayerA.setText(sA);
            }
            iNames++;
        }
        if ( StringUtil.isNotEmpty(sB) ) {
            if ( m_bIsDoubles ) {
                String[] saNames = sB.split("/");
                txtPlayerB.setText(saNames[0]);
                if ( saNames.length > 1 ) {
                    txtPlayerB2.setText(saNames[1]);
                }
            } else {
                txtPlayerB.setText(sB);
            }
            iNames++;
        }

        if ( (txtCountryA != null) && (txtCountryB != null) ) {
            if ( StringUtil.isNotEmpty(sCountryA) ) {
                txtCountryA.setText(sCountryA);
                if ( txtCountryA instanceof CountryTextView) {
                    CountryTextView textView = (CountryTextView) this.txtCountryA;
                    textView.setCountryCode(sCountryA);
                }
                PreferenceValues.downloadImage(getContext(), null, sCountryA);
            }
            if ( StringUtil.isNotEmpty(sCountryB) ) {
                txtCountryB.setText(sCountryB);
                if ( txtCountryB instanceof CountryTextView) {
                    CountryTextView textView = (CountryTextView) this.txtCountryB;
                    textView.setCountryCode(sCountryB);
                }
                PreferenceValues.downloadImage(getContext(), null, sCountryB);
            }
            if ( StringUtil.areAllEmpty(sCountryA, sCountryB) ) {
                if ( PreferenceValues.useCountries(getContext()) == false ) {
                    ViewUtil.hideViews(this, R.id.ll_match_countries);
                }
            } else {
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 /* 15 */ ) {
                    if ( bCountriesCollapsed ) {
                        findViewById(R.id.lblCountries).callOnClick(); // to expand the parent area by default
                        bCountriesCollapsed = false;
                    }
                }
            }
        }

        saAvatars[Player.A.ordinal()] = sAvatarA;
        saAvatars[Player.B.ordinal()] = sAvatarB;
        for(Player p: Player.values() ) {
            if ( StringUtil.isNotEmpty(saAvatars[p.ordinal()]) ) {
                PreferenceValues.downloadAvatar(getContext(), null, saAvatars[p.ordinal()]);
            }
        }

        if ( (txtClubA != null) && (txtClubB!=null) ) {
            if ( StringUtil.areAllEmpty(sClubA, sClubB) == false ) {
                if ( StringUtil.isNotEmpty(sClubA) ) {
                    txtClubA.setText(sClubA);
                }
                if ( StringUtil.isNotEmpty(sClubB) ) {
                    txtClubB.setText(sClubB);
                }
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 /* 15 */ ) {
                    if ( bClubsCollapsed ) {
                        findViewById(R.id.lblClubs).callOnClick(); // to expand the parent area by default
                        bClubsCollapsed = false;
                    }
                }
            }
        }

        // try setting the focus to something else than the playertextview elements to prevent keyboard to pop-up if names
        // are already specified
        if ( iNames == 2 ) {
            View viewById = findViewById(R.id.spNumberOfGamesToWin);
            if ( viewById != null ) {
                viewById.setFocusable(true);
                viewById.setFocusableInTouchMode(true);
                requestFocusFor(viewById);
            }
        }
    }
  //private NumberPicker         npGameEndScore = null;
    private Spinner              spGameEndScore; // e.g. used in landscape but not in portrait
    private Spinner              spNumberOfGamesToWin;
    private Spinner              spNumberOfServesPerPlayer;
    private Spinner              spTieBreakFormat;
    private Spinner              spHandicap;
    private Spinner              spDisciplineStart;
    private Spinner              spPauseDuration;
    private Spinner              spAnnouncementLanguage;
    private Spinner              spDoublesServeSequence;
    private CheckBox             cbUseEnglishScoring;
    private CheckBox             cbUseLiveScoring;
    private PreferenceACTextView txtRefereeName;
    private PreferenceACTextView txtMarkerName;
    private PreferenceACTextView txtEventName;
    private PreferenceACTextView txtEventDivision;
    private PreferenceACTextView txtEventRound;
    private PreferenceACTextView txtEventLocation;
    private PreferenceACTextView txtCourt;
    private TextView             txtEventID;
    private EditText             txtPlayerA;  // singles and doubles
    private EditText             txtPlayerA2; // doubles
    private EditText             txtPlayerB;  // singles and doubles
    private EditText             txtPlayerB2; // doubles
    private TextView             txtCountryA;
    private TextView             txtCountryB;
    private String[]             saAvatars = new String[2];
    private PreferenceACTextView txtClubA;
    private PreferenceACTextView txtClubB;

/*
    private TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if ((actionId == EditorInfo.IME_ACTION_SEARCH) || (actionId == EditorInfo.IME_ACTION_DONE) ) {
                // close a possible open autocomplete
                if ( v instanceof AutoCompleteTextView ) {
                    AutoCompleteTextView acv = (AutoCompleteTextView) v;
                    if ( acv.isPopupShowing() ) {
                        acv.dismissDropDown();
                    }
                }
            }
            return false;
        }
    };
*/

    private TextView[] tvsPlayers   = null;
    private TextView[] tvsCountries = null;
    private TextView[] tvsClubs     = null;
    private View[]     tvsReferee   = null;
    private TextView[] tvsEvent     = null;

    boolean clearEventFields() {
        return clearFields(tvsEvent);
    }

    boolean clearRefereeFields() {
        return clearFields(tvsReferee);
    }

    boolean clearAllFields() {
        clearPlayerFields();
        clearClubFields();
        clearCountryFields();
        clearEventFields();
        clearRefereeFields();
        return true;
    }
    boolean clearPlayerFields() {
        return clearFields(tvsPlayers);
    }

    boolean clearCountryFields() {
        return clearFields(tvsCountries);
    }
    boolean clearClubFields() {
        return clearFields(tvsClubs);
    }

    private static int iForceTextSize = 0;
    private static ArrayAdapter<String> getStringArrayAdapter(Context context, List<String> list, TextView tvRefTxtSize) {
        if ( tvRefTxtSize != null ) {
            iForceTextSize = (int) tvRefTxtSize.getTextSize();
        }
        return EnumSpinner.getStringArrayAdapter(context, list, iForceTextSize);
    }
    private <T extends Enum<T>> void initEnumSpinner(Spinner spinner, Class<T> clazz, T value, T excludeValue, int iResourceDisplayValues) {
        EnumSpinner.init(spinner, getContext(), clazz, value, excludeValue, iResourceDisplayValues, iForceTextSize);
    }
    private static void setTextSizeFromBoard(View v) {
/*
        if ( (v instanceof TextView) && (iForceTextSize > 0) ) {
            TextView tv = (TextView) v;
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, iForceTextSize);
        }
*/
    }

    private void initTextViews(View[] tvs) {
        for ( View v:tvs ) {
            NextFocusDownListener.mimicPreSdk19(this, v);
            //v.setOnEditorActionListener(onEditorActionListener);

            setTextSizeFromBoard(v);
        }
    }

    private boolean clearFields(View[] tvs) {
        if ( tvs == null ) {
            return false;
        }
        for(View v:tvs) {
            if ( v instanceof TextView ) {
                TextView tv = (TextView) v;
                ViewUtil.emptyField(tv);
                if (v instanceof PreferenceACTextView) {
                    PreferenceACTextView v1 = (PreferenceACTextView) v;
                    v1.useLastValueAsDefault(false);
                    v1.getTextAndPersist(true);
                }
            }
        }
        return true;
    }

    private boolean m_bIsDoubles = false;
    private Model   m_model      = null;
    public void init(boolean bIsDoubles, Model model) {
        Context context = getContext();

        //iForceTextSize = IBoard.iTxtSizePx_FinishedGameScores;

        m_bIsDoubles = bIsDoubles; // attrs.getAttributeBooleanValue(APPLICATION_NS, "isDoubles", false);
        m_model = model;

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // wrap it into a scroll view so that user with small screens and large fonts.... yada yada
        ScrollView sv = new ScrollView(context);
        inflater.inflate(m_layout.getLayoutResId(), sv, true);
        this.addView(sv);

        List<Integer> lViewsToHide = new ArrayList<Integer>();
        if ( bIsDoubles ) {
            lViewsToHide.add(R.id.ll_match_singles);
        } else {
            lViewsToHide.add(R.id.ll_match_doubles);
            lViewsToHide.add(R.id.ll_doubleServeSequence);
        }
        ViewUtil.hideViews(this, lViewsToHide);

        // get references for late use
        txtEventName     = (PreferenceACTextView) findViewById(R.id.match_event);
        txtEventDivision = (PreferenceACTextView) findViewById(R.id.match_division);
        txtEventRound    = (PreferenceACTextView) findViewById(R.id.match_round);
        txtEventLocation = (PreferenceACTextView) findViewById(R.id.match_location);
        txtEventID       = (TextView)             findViewById(R.id.match_id);
        txtCourt         = (PreferenceACTextView) findViewById(R.id.match_court);
        tvsEvent = new TextView[]{txtEventName,txtEventDivision,txtEventRound,txtEventLocation, txtCourt};

        txtRefereeName   = (PreferenceACTextView) findViewById(R.id.match_referee);
        txtMarkerName    = (PreferenceACTextView) findViewById(R.id.match_marker);
        spAnnouncementLanguage = (Spinner) findViewById(R.id.spAnnouncementLanguage);
        tvsReferee = new View[]{txtRefereeName,txtMarkerName, spAnnouncementLanguage};

        if ( bIsDoubles ) {
            txtPlayerA  = (EditText) findViewById(R.id.match_playerA1);
            txtPlayerA2 = (EditText) findViewById(R.id.match_playerA2);
            txtPlayerB  = (EditText) findViewById(R.id.match_playerB1);
            txtPlayerB2 = (EditText) findViewById(R.id.match_playerB2);
            tvsPlayers  = new TextView[]{txtPlayerA,txtPlayerA2,txtPlayerB,txtPlayerB2};
            clearPlayerFields();
        } else {
            txtPlayerA = (EditText) findViewById(R.id.match_playerA);
            txtPlayerB = (EditText) findViewById(R.id.match_playerB);
            tvsPlayers = new TextView[]{txtPlayerA,txtPlayerB};
            clearPlayerFields();
        }
        if ( txtPlayerA instanceof PlayerTextView ) {
            PlayerTextView p = (PlayerTextView) txtPlayerA;
            p.addSibling((PlayerTextView) txtPlayerB);
            if ( bIsDoubles ) {
                p.addSibling((PlayerTextView) txtPlayerA2);
                p.addSibling((PlayerTextView) txtPlayerB2);
            }
        }

        txtCountryA  = (EditText)             findViewById(R.id.match_countryA);
        txtCountryB  = (EditText)             findViewById(R.id.match_countryB);
        txtClubA     = (PreferenceACTextView) findViewById(R.id.match_clubA);
        txtClubB     = (PreferenceACTextView) findViewById(R.id.match_clubB);
        tvsCountries = new TextView[]{txtCountryA,txtCountryB};
        tvsClubs     = new TextView[]{txtClubA,txtClubB};
        initCountries(context, txtCountryA, txtPlayerA, bIsDoubles);
        initCountries(context, txtCountryB, txtPlayerB, bIsDoubles);
        initClubs(context, txtClubA, txtPlayerA);
        initClubs(context, txtClubB, txtPlayerB);

        initTextViews(tvsEvent);
        initTextViews(tvsReferee);
        initTextViews(tvsPlayers);
        //initTextViews(tvsCountries);
        //initTextViews(tvsClubs);
        initForEasyClearingFields();

        // optionally get player names passed in from a match selection
        requestFocusFor(txtPlayerA); // does not work if focus was impossible (.e.g not the initial tab in MatchTabbed)

        // take values from preferences as default values
        int iGameEndPref        = Model.UNDEFINED_VALUE;
        int iNrOfGamesToWinPref = Model.UNDEFINED_VALUE;
        if ( m_model != null ) {
            iGameEndPref        = m_model.getNrOfPointsToWinGame();
            iNrOfGamesToWinPref = m_model.getNrOfGamesToWinMatch();
        }
        if ( iGameEndPref == Model.UNDEFINED_VALUE ) {
            iGameEndPref = PreferenceValues.numberOfPointsToWinGame(context);
        }
        if ( iNrOfGamesToWinPref == Model.UNDEFINED_VALUE ) {
            iNrOfGamesToWinPref = PreferenceValues.numberOfGamesToWinMatch(context);
        }
/*
        npGameEndScore = null; // (NumberPicker) findViewById(R.id.npGameEndScore);
        if ( npGameEndScore != null ) {
            npGameEndScore.setMinValue(2);
            npGameEndScore.setMaxValue(Math.max(21, iGameEndPref));
            npGameEndScore.setValue(iGameEndPref);
            npGameEndScore.setWrapSelectorWheel(false);
        }
*/

        spGameEndScore = (Spinner) findViewById(R.id.spGameEndScore);
        initGameEndScore(context, spGameEndScore, iGameEndPref, 2, txtPlayerA);

        int max = Math.max(iNrOfGamesToWinPref, 11);
/*
        npNumberOfGamesToWin = (NumberPicker) findViewById(R.id.npNumberOfGamesToWin);

        if ( npNumberOfGamesToWin != null ) {
            String[] saValues = new String[max];
            for(int iIdx=0; iIdx < max;iIdx++ ) {
                saValues[iIdx] = "" + ((iIdx+1) * 2 - 1);
            }
            npNumberOfGamesToWin.setMinValue(0);
            npNumberOfGamesToWin.setMaxValue(saValues.length - 1);
            npNumberOfGamesToWin.setDisplayedValues(saValues);
            npNumberOfGamesToWin.setValue(iNrOfGamesToWinPref-1);
            npNumberOfGamesToWin.setWrapSelectorWheel(false);
        }
*/

        spNumberOfGamesToWin = (Spinner) findViewById(R.id.spNumberOfGamesToWin);
        initNumberOfGamesToWin(context, spNumberOfGamesToWin, iNrOfGamesToWinPref, max, txtPlayerA);

        spNumberOfServesPerPlayer = (Spinner) findViewById(R.id.spNumberOfServesPerPlayer);
        initNumberOfServesPerPlayer(context, spNumberOfServesPerPlayer, PreferenceValues.numberOfServesPerPlayer(context), 1, 5, txtPlayerA);
        {
            TieBreakFormat tbfPref = PreferenceValues.getTiebreakFormat(context);
            spTieBreakFormat = (Spinner) findViewById(R.id.spTieBreakFormat);
            if ( spTieBreakFormat != null ) {
                if ( spTieBreakFormat instanceof EnumSpinner ) {
                    EnumSpinner<TieBreakFormat> sp = (EnumSpinner<TieBreakFormat>) spTieBreakFormat;
                    sp.setSelected(tbfPref);
                } else {
                    initEnumSpinner(spTieBreakFormat, TieBreakFormat.class, tbfPref, null, R.array.tiebreakFormatDisplayValues);
/*
                    String[] sDisplayValues = getResources().getStringArray(R.array.tiebreakFormatDisplayValues);
                    List<String> list = new ArrayList<String>();
                    int iSelectedIndex = 0;
                    int iIdx = -1;
                    for (TieBreakFormat tbf : TieBreakFormat.values()) {
                        iIdx++;
                        if (tbf.equals(tbfPref)) {
                            iSelectedIndex = iIdx;
                        }
                        String sDisplayValue = sDisplayValues.length > iIdx ? sDisplayValues[iIdx] : StringUtil.capitalize(tbf);
                        list.add(sDisplayValue);
                    }
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item, list);
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spTieBreakFormat.setAdapter(dataAdapter);
                    spTieBreakFormat.setSelection(iSelectedIndex);
*/
                }
            }
        }
        {
            spPauseDuration = (Spinner) findViewById(R.id.spPauseDuration);
            initPauseDuration(context, spPauseDuration, txtPlayerA);
        }
        {
            if ( PreferenceValues.useOfficialAnnouncementsFeature(context).equals(Feature.DoNotUse) ) {
                ViewUtil.hideViewsForEver(this, R.id.ll_AnnouncementLanguage);
            } else {
                spAnnouncementLanguage = (Spinner) findViewById(R.id.spAnnouncementLanguage);
                if (spAnnouncementLanguage != null) {
                    AnnouncementLanguage language = PreferenceValues.officialAnnouncementsLanguage(context);
                    if (spAnnouncementLanguage instanceof EnumSpinner) {
                        EnumSpinner<AnnouncementLanguage> sp = (EnumSpinner<AnnouncementLanguage>) spAnnouncementLanguage;
                        sp.setSelected(language);
                    } else {
                        initEnumSpinner(spAnnouncementLanguage, AnnouncementLanguage.class, language, null, 0);
                    }
                }
            }
        }
        {
            HandicapFormat hdcPref = PreferenceValues.getHandicapFormat(context);
            spHandicap = (Spinner) findViewById(R.id.spHandicapFormat);
            if ( spHandicap != null ) {
                if ( spHandicap instanceof EnumSpinner ) {
                    EnumSpinner<HandicapFormat> sp = (EnumSpinner<HandicapFormat>) spHandicap;
                    sp.setSelected(hdcPref);
                } else {
                    initEnumSpinner(spHandicap, HandicapFormat.class, hdcPref, null, R.array.handicapFormatDisplayValues);
                }
            }
        }
        {
            EnumSet<Sport> sports = PreferenceValues.getDisciplineSequence(context);
            spDisciplineStart = (Spinner) findViewById(R.id.spDisciplineStart);
            Sport first = sports.iterator().next();
            if ( spDisciplineStart != null ) {
                if ( spDisciplineStart instanceof EnumSpinner ) {
                    EnumSpinner<Sport> sp = (EnumSpinner<Sport>) spDisciplineStart;
                    sp.setSelected(first);
                } else {
                    initEnumSpinner(spDisciplineStart, Sport.class, first, null, 0);
                }
            }
        }

        if ( bIsDoubles ) {
            DoublesServeSequence dssPref = PreferenceValues.getDoublesServeSequence(context);
            spDoublesServeSequence = (Spinner) findViewById(R.id.spDoublesServeSequence);
            if ( spDoublesServeSequence != null ) {
                if ( spDoublesServeSequence instanceof EnumSpinner ) {
                    EnumSpinner<DoublesServeSequence> sp = (EnumSpinner<DoublesServeSequence>) spDoublesServeSequence;
                    sp.setSelected(dssPref); // TODO: set selected
                } else {
                    initEnumSpinner(spDoublesServeSequence, DoublesServeSequence.class, dssPref, DoublesServeSequence.NA, R.array.doublesServeSequence);
/*
                    String[] sDisplayValues = getResources().getStringArray(R.array.doublesServeSequence);
                    List<String> list = new ArrayList<String>();
                    int iSelectedIndex = 0;
                    int iIdx = -1;
                    for (DoublesServeSequence dss : DoublesServeSequence.values()) {
                        if (dss.equals(DoublesServeSequence.NA)) {
                            continue;
                        } // NA must be the last in the enumeration
                        iIdx++;
                        if (dss.equals(dssPref)) {
                            iSelectedIndex = iIdx;
                        }
                        String sDisplayValue = sDisplayValues.length > iIdx ? sDisplayValues[iIdx] : dss.toString();
                        list.add(sDisplayValue);
                    }
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item, list);
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spDoublesServeSequence.setAdapter(dataAdapter);
                    spDoublesServeSequence.setSelection(iSelectedIndex);
*/
                }
            }
        }

        cbUseLiveScoring = (CheckBox) findViewById(R.id.cbUseLivescore);
        if ( cbUseLiveScoring != null ) {
            ShareMatchPrefs forLiveScore = PreferenceValues.isConfiguredForLiveScore(context);
            cbUseLiveScoring.setChecked(forLiveScore!=null);
        }

        cbUseEnglishScoring = (CheckBox) findViewById(R.id.useHandInHandOutScoring);
        cbUseEnglishScoring.setChecked(PreferenceValues.useHandInHandOutScoring(context));
    }

    public static void initPauseDuration(Context context, Spinner spPauseDuration, TextView tvRefTxtSize) {
        if ( spPauseDuration == null ) { return; }

        List<String> lValues = Preferences.syncAndClean_pauseBetweenGamesValues(context);
        if ( ListUtil.size(lValues) <= 1 ) {
            // do not show if there is only one value to select
            ViewParent parent = spPauseDuration.getParent();
            if ( parent instanceof ViewGroup) {
                ((ViewGroup) parent).setVisibility(GONE);
            }
            //findViewById(R.id.llPauseDuration).setVisibility(GONE);
        } else {
            int iPauseDuration = PreferenceValues.getPauseDuration(context);
            ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, lValues, tvRefTxtSize);
            spPauseDuration.setAdapter(dataAdapter);
            spPauseDuration.setSelection(lValues.indexOf(String.valueOf(iPauseDuration)));
            setTextSizeFromBoard(spPauseDuration);
        }
    }

    private static void initClubs(final Context context, final PreferenceACTextView spClub, final TextView txtPlayer) {
        if ( spClub == null ) { return; }
        if ( txtPlayer != null ) {
            spClub.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    if ( (v instanceof AutoCompleteTextView) && (hasFocus == false) && ViewUtil.areAllEmpty(txtPlayer)) {
                        AutoCompleteTextView actv = (AutoCompleteTextView) v;
                        txtPlayer.setText(actv.getText());
                    }
                }
            });
        }
    }

    private static void initCountries(final Context context, final TextView spCountry, final TextView txtPlayer, boolean bIsDoubles) {
        if ( spCountry == null ) { return; }
        if ( spCountry instanceof CountryTextView ) {
            int iSuggestAfterAtLeast = PreferenceValues.numberOfCharactersBeforeAutocompleteCountry(context);

            // tmp: fix old default from 3 to 1
            int iRunCount = PreferenceValues.getRunCount(context, PreferenceKeys.numberOfCharactersBeforeAutocompleteCountry);
            if ( (iRunCount <= 1) && (iSuggestAfterAtLeast == 3) /* Old default */ ) {
                iSuggestAfterAtLeast = 1;
                RWValues.setNumber(PreferenceKeys.numberOfCharactersBeforeAutocompleteCountry, context, iSuggestAfterAtLeast);
            }

            final CountryTextView ac = (CountryTextView) spCountry;
            ac.setThreshold(iSuggestAfterAtLeast);
            ac.setAutoCompleteLayoutResourceId(R.layout.expandable_match_selector_item);
            if ( txtPlayer != null ) {
                ac.addListener(new CountryTextView.Listener() {
                    @Override public void onSelected(String sCode, String sCountryName) {
                        if ( ViewUtil.areAllEmpty(txtPlayer) ) {
                            txtPlayer.setText(sCountryName);
                        }
                    }
                });

                if ( PreferenceValues.useCountries(context) /*&& (bIsDoubles == false)*/ ) {
                    // fill country if player is selected from autocomplete list with country code in it
                    if (txtPlayer instanceof PlayerTextView) {
                        final PlayerTextView ptv = (PlayerTextView) txtPlayer;
                        ptv.addListener(new CountryCodeCopier(ac));
                    }
                }
            }
            ac.addListener(new CountryTextView.Listener() {
                @Override public void onSelected(String sCode, String sCountryName) {
                    PreferenceValues.downloadImage(context, null, sCode);
                }
            });
        }
    }

    private static class CountryCodeCopier implements PlayerTextView.Listener {
        private CountryTextView ac = null;
        CountryCodeCopier(CountryTextView ac) {
            this.ac = ac;
        }
        @Override public void onSelected(String sName, PlayerTextView ptv) {
            if ( sName.matches(CountryTextView.S_EXTRACT_COUNTRYCODE_REGEXP) ) {
                String sCountryCode = sName.replaceAll(CountryTextView.S_EXTRACT_COUNTRYCODE_REGEXP, "$2");
                if ( ViewUtil.areAllEmpty(ac) && StringUtil.isNotEmpty(sCountryCode) ) {
                    ac.setCountryCode(sCountryCode);
                    ptv.setText(sName.replaceAll(CountryTextView.S_EXTRACT_COUNTRYCODE_REGEXP, "$1"));
                }
            }
        }
    }
    /** Invoked from EditFormat as well */
    public static void initGameEndScore(Context context, final Spinner spGameEndScore, int iGameEndPref, int iValueOfset, TextView refTxtSize) {
        if ( spGameEndScore == null ) { return; }

        List<String> list = new ArrayList<String>();
        int iSelectedIndex = 0;
        for (int iIdx = 0; iIdx <= Math.max(19, iGameEndPref); iIdx++) {
            int iValue = iIdx + iValueOfset;
            if (iValue == iGameEndPref) {
                iSelectedIndex = iIdx;
            }
            list.add(" " + iValue + " ");
        }
        final String sMORE = context.getResources().getString(R.string.uc_more);
        list.add(sMORE);
        final ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, list, refTxtSize);
        spGameEndScore.setAdapter(dataAdapter);
        spGameEndScore.setSelection(iSelectedIndex);
        spGameEndScore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spGameEndScore) {
                    Object selectedItem = spGameEndScore.getSelectedItem();
                    if ( selectedItem.toString().equals(sMORE)) {
                        dataAdapter.remove(sMORE); // add it again after numbers have been added
                        for (int i = position+2; i <= position + 101; i++) {
                            dataAdapter.add("" + i);
                        }
                        dataAdapter.add(sMORE);
                        dataAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        setTextSizeFromBoard(spGameEndScore);
    }

    /** Invoked from EditFormat as well */
    public static void initNumberOfGamesToWin(Context context, Spinner spNumberOfGamesToWin, int iNrOfGamesToWinPref, int max, TextView refTxtSize) {
        if ( spNumberOfGamesToWin == null ) { return; }
        List<String> list = new ArrayList<String>();
        int iSelectedIndex = 0;
        for ( int iIdx=0; iIdx < max; iIdx++ ) {
            int iNrOfGamesToWin = (iIdx + 1);
            int iBestOf = iNrOfGamesToWin * 2 - 1;
            if ( iNrOfGamesToWin == iNrOfGamesToWinPref) {
                iSelectedIndex = iIdx;
            }
            list.add("" + iBestOf);
        }
        ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, list, refTxtSize);
        spNumberOfGamesToWin.setAdapter(dataAdapter);
        spNumberOfGamesToWin.setSelection(iSelectedIndex);
        setTextSizeFromBoard(spNumberOfGamesToWin);
    }

    public static void initNumberOfServesPerPlayer(Context context, Spinner sp, int iCurrent, int iMin, int max, TextView txtRefTxtSize) {
        if ( sp == null ) { return; }
        List<String> list = new ArrayList<String>();
        int iSelectedIndex = 0;
        for ( int iIdx=iMin; iIdx <= max; iIdx++ ) {
            if ( iIdx == iCurrent) {
                iSelectedIndex = iIdx-iMin;
            }
            list.add("" + iIdx);
        }
        ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, list, txtRefTxtSize);
        sp.setAdapter(dataAdapter);
        sp.setSelection(iSelectedIndex);
        setTextSizeFromBoard(sp);
    }

    private void initForEasyClearingFields() {
        TextView lblPlayersD = (TextView) findViewById(R.id.lblPlayersD);
        if ( lblPlayersD != null ) {
            lblPlayersD.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearPlayerFields();
                    return true;
                }
            });
        }
        TextView lblPlayersS = (TextView) findViewById(R.id.lblPlayersS);
        if ( lblPlayersS != null ) {
            lblPlayersS.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearPlayerFields();
                    return true;
                }
            });
        }
        TextView lblCountries = (TextView) findViewById(R.id.lblCountries);
        if ( lblCountries != null ) {
            lblCountries.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearCountryFields();
                    return true;
                }
            });
        }
        TextView lblClubs = (TextView) findViewById(R.id.lblClubs);
        if ( lblClubs != null ) {
            lblClubs.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearClubFields();
                    return true;
                }
            });
        }
        TextView lblEvent = (TextView) findViewById(R.id.lblEvent);
        if ( lblEvent != null ) {
            lblEvent.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearEventFields();
                    return true;
                }
            });
        }
        TextView lblRef = (TextView) findViewById(R.id.lblReferee);
        if ( lblRef != null ) {
            lblRef.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearRefereeFields();
                    return true;
                }
            });
        }
    }

    DoublesServeSequence getDoublesServeSequence() {
        if ( (spDoublesServeSequence != null) && (spDoublesServeSequence.getVisibility() != View.GONE) ) {
            return DoublesServeSequence.values()[spDoublesServeSequence.getSelectedItemPosition()];
        }
        return DoublesServeSequence.NA;
    }

    /** uses Model instead of MatchDetails */
    Intent getIntent(String sSource, String sSourceID, boolean bBackPressed) {
        TextView[] textViews = {txtPlayerA, txtPlayerB};
        int msg_enter_player_names = R.string.msg_enter_both_player_names;
        if ( m_bIsDoubles ) {
            textViews = new TextView[] {txtPlayerA, txtPlayerA2, txtPlayerB, txtPlayerB2};
            msg_enter_player_names = R.string.msg_enter_all_player_names;
        } else {
            // use club names as player names if player names are not entered
            TextView[] tvClubs = { txtClubA, txtClubB };
            if ( ViewUtil.areAllNonEmpty(tvClubs) && ViewUtil.areAllEmpty(textViews) ) {
                for(int i=0; i < tvClubs.length; i++) {
                    textViews[i].setText(tvClubs[i].getText());
                }
            }
        }
        for ( TextView txt: textViews ) {
            if ( txt == null ) { continue; }
            if ( txt.getText().toString().trim().length() == 0 ){
                //txt.setError(getString(msg_enter_player_names)); // To aggressive
                txt.setHint(msg_enter_player_names);
                if ( bBackPressed == false ) {
                    Toast.makeText(getContext(), msg_enter_player_names, Toast.LENGTH_SHORT).show();
                }
                return null;
            } else {
                txt.setError(null);
                txt.setHint(null);
            }
        }
        // get values from either a NumberPicker or Spinner element
        int iNrOfPoints2Win = PreferenceValues.numberOfPointsToWinGame(getContext());
/*
        if ( npGameEndScore != null ) {
            iNrOfPoints2Win = npGameEndScore.getValue();
        } else
*/
        if ( spGameEndScore != null ) {
            iNrOfPoints2Win = Integer.parseInt(spGameEndScore.getSelectedItem().toString().trim());
        }

        int iNrOfGamesToWinMatch = PreferenceValues.numberOfGamesToWinMatch(getContext()); // TODO: test for both spinner and numberpicker
        /*if ( npNumberOfGamesToWin != null ) {
            iNrOfGamesToWinMatch = (npNumberOfGamesToWin.getValue() + 1);
        } else*/
        if ( spNumberOfGamesToWin != null ) {
            iNrOfGamesToWinMatch = (Integer.parseInt(spNumberOfGamesToWin.getSelectedItem().toString()) + 1) / 2;
        }
        Intent intent = new Intent();
        //intent.putExtra(MatchDetails.class.getSimpleName(), getBundle(sSource, iNrOfPoints2Win, iNrOfGamesToWinMatch)); // this is read by ScoreBoard.onActivityResult
        Model  model = getModel(sSource, sSourceID, iNrOfPoints2Win, iNrOfGamesToWinMatch);
        String sJson = model.toJsonString(null);
        intent.putExtra(Model.class.getSimpleName(), sJson); // this is read by ScoreBoard.onActivityResult
        return intent;
    }

    private Model getModel(String sSource, String sSourceID, int iNrOfPoints2Win, int iNrOfGamesToWinMatch) {
        Model m = ModelFactory.getTemp();
        if ( m_bIsDoubles == false ) {
            m.setPlayerName(Player.A, txtPlayerA.getText().toString());
            m.setPlayerName(Player.B, txtPlayerB.getText().toString());
        }
        if ( (txtCountryA != null) && (txtCountryB != null) ) {
            String sCountryA = txtCountryA.getText().toString();
            String sCountryB = txtCountryB.getText().toString();
            if ( txtCountryA instanceof CountryTextView && txtCountryB instanceof CountryTextView) {
                String sCCA = ((CountryTextView)txtCountryA).getCountryCode();
                String sCCB = ((CountryTextView)txtCountryB).getCountryCode();
                if ( StringUtil.isNotEmpty(sCCA) ) {
                    sCountryA = sCCA;
                }
                if ( StringUtil.isNotEmpty(sCCB) ) {
                    sCountryB = sCCB;
                }
            }
            m.setPlayerCountry(Player.A, sCountryA);
            m.setPlayerCountry(Player.B, sCountryB);
        }
        m.setPlayerAvatar(Player.A, saAvatars[Player.A.ordinal()]);
        m.setPlayerAvatar(Player.B, saAvatars[Player.B.ordinal()]);

        if ( (txtClubA != null) && (txtClubB != null) ) {
            m.setPlayerClub(Player.A, txtClubA.getTextAndPersist().toString());
            m.setPlayerClub(Player.B, txtClubB.getTextAndPersist().toString());
        }
        if ( txtEventName != null ) {
            m.setEvent( txtEventName    .getTextAndPersist().toString()
                      , txtEventDivision.getTextAndPersist().toString()
                      , txtEventRound   .getTextAndPersist().toString()
                      , txtEventLocation.getTextAndPersist().toString()
            );
        }
        if ( txtRefereeName != null ) {
            m.setReferees( txtRefereeName.getTextAndPersist().toString()
                         , txtMarkerName .getTextAndPersist().toString());
        }
        m.setNrOfPointsToWinGame(iNrOfPoints2Win);
        m.setNrOfGamesToWinMatch(iNrOfGamesToWinMatch);
        if ( txtCourt != null ) {
            m.setCourt(txtCourt.getTextAndPersist().toString());
        }
        if ( StringUtil.isNotEmpty(sSource) ) {
            m.setSource(sSource, sSourceID);
        }

        if ( (spTieBreakFormat != null) && (spTieBreakFormat.getVisibility() != View.GONE) && ( spTieBreakFormat.getSelectedItemPosition() != -1) ) {
            TieBreakFormat tbf = TieBreakFormat.values()[spTieBreakFormat.getSelectedItemPosition()];
            m.setTiebreakFormat(tbf);
        }

        if ( (spHandicap != null) && (spHandicap.getVisibility() != View.GONE) && (spHandicap.getSelectedItemPosition() != -1) ) {
            HandicapFormat hdc = HandicapFormat.values()[spHandicap.getSelectedItemPosition()];
            m.setHandicapFormat(hdc);
        }
        if ( Brand.isRacketlon() && spDisciplineStart != null ) {
            RacketlonModel rm = (RacketlonModel) m;
            Sport sport = Sport.values()[spDisciplineStart.getSelectedItemPosition()];
            if ( sport.equals(Sport.Tabletennis) == false ) {
                rm.setDiscipline(0, sport);
            }
        }
        if ( Brand.isTabletennis() && (spNumberOfServesPerPlayer != null) ) {
            int iNrOfServesPerPlayer = Integer.parseInt(spNumberOfServesPerPlayer.getSelectedItem().toString());
            m.setNrOfServesPerPlayer(iNrOfServesPerPlayer);
        }
        if ( (spAnnouncementLanguage != null) && (spAnnouncementLanguage.getVisibility() != View.GONE) && (spAnnouncementLanguage.getSelectedItemPosition() != -1) ) {
            AnnouncementLanguage languageNew = AnnouncementLanguage.values()[spAnnouncementLanguage.getSelectedItemPosition()];
            PreferenceValues.setAnnouncementLanguage(languageNew, getContext());
        }
        if ( (spPauseDuration != null) && (spPauseDuration.getVisibility() != View.GONE) ) {
            String sDuration = (String) spPauseDuration.getSelectedItem();
            if ( StringUtil.isNotEmpty(sDuration) ) {
                int iDuration = Integer.parseInt(sDuration);
                int iCurrentPrefValue = PreferenceValues.getPauseDuration(getContext());
                if ( iDuration != iCurrentPrefValue) {
                    PreferenceValues.setNumber(PreferenceKeys.timerPauseBetweenGames, getContext(), iDuration);
                }
            }
        }
        m.setEnglishScoring((cbUseEnglishScoring != null) && cbUseEnglishScoring.isChecked());
        if ( (cbUseLiveScoring != null) && cbUseLiveScoring.isChecked() ) {
            PreferenceValues.initForLiveScoring(getContext(), false);
        } else {
            PreferenceValues.initForNoLiveScoring(getContext());
        }

        // for doubles
        if ( m_bIsDoubles ) {
            if ( (spDoublesServeSequence != null) && (spDoublesServeSequence.getVisibility() != View.GONE) && Brand.isSquash() ) {
                DoublesServeSequence dss = DoublesServeSequence.values()[spDoublesServeSequence.getSelectedItemPosition()];
                SquashModel squashModel = (SquashModel) m;
                squashModel.setDoublesServeSequence(dss);
            }
            if ( (txtPlayerA2 != null) && (txtPlayerB2 != null) ) {
                m.setPlayerName(Player.A, txtPlayerA.getText() + "/" + txtPlayerA2.getText());
                m.setPlayerName(Player.B, txtPlayerB.getText() + "/" + txtPlayerB2.getText());
            }
        }
        return m;
    }
}
