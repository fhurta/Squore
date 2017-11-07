package com.doubleyellow.scoreboard.prefs;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.*;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.dialog.ButtonUpdater;
import com.doubleyellow.util.*;
import com.doubleyellow.android.util.ColorUtil;

import java.io.File;
import java.io.StringReader;
import java.util.*;

/**
 * Provides read/write method for preferences that are color related.
 */
public class ColorPrefs
{
    private static final int ICON_SIZE = 100;  // IH 20140327

    public enum ColorTarget {
        backgroundColor,
        mainTextColor,

        actionBarBackgroundColor,

        timerButtonBackgroundColor,
        timerButtonBackgroundColor_Warn,
        playerButtonBackgroundColor,
        playerButtonTextColor,

        speakButtonBackgroundColor,
        scoreButtonBackgroundColor,
        scoreButtonTextColor,

        tossButtonBackgroundColor,
        shareButtonBackgroundColor,
        serveButtonBackgroundColor,
        serveButtonTextColor,

        historyBackgroundColor,
        historyTextColor,

        black,
        darkest,
        middlest,
        lightest,
        white,
    }

    private static final String TAG = "SB." + ColorPrefs.class.getSimpleName();

    public static final int COLOR_BLACK = Color.parseColor("#000000");
    public static final int COLOR_WHITE = Color.parseColor("#FFFFFF");

    private static Map<String, String>       mColorScheme;
    private static Map<ColorTarget, Integer> mTarget2Color;
    private static Map<String, Long>         mRGB2Distance2White;
    private static Map<String, Long>         mRGBDistance2Black;
    private static SparseArray<Long>         mColor2Distance2White;
    public  static SparseArray<Long>         mColor2Distance2Black;
    public  static SparseArray<Long>         mColor2Distance2Darker;

    public static File getFile(Context context) {
        File fCacheOld = new File(context.getCacheDir(), PreferenceKeys.colorSchema.toString());
        File fFileNew  = new File(context.getFilesDir(), PreferenceKeys.colorSchema.toString() + ".json");

        // temporary code to move customize colors stored in cache directory to files directory
        if ( fCacheOld.exists() && fFileNew.exists() == false ) {
            fCacheOld.renameTo(fFileNew);
        }
        return fFileNew;
    }

    public static void reset(Context context) {
        File file = getFile(context);
        if ( file.exists() ) {
            file.delete();
        }
    }

    public static void clearColorCache() {
        mTarget2Color.clear();
    }

    /** reconstruct both mColorScheme, and mTarget2Color */
    public static Map<ColorTarget, Integer> getTarget2colorMapping(Context context) {
        if ( MapUtil.isNotEmpty(mTarget2Color) ) { return mTarget2Color; }

        mColorScheme = getColorScheme(context);

        mRGBDistance2Black    = new HashMap<String, Long>();
        mRGB2Distance2White   = new HashMap<String, Long>();
        mColor2Distance2Black = new SparseArray<Long>();
        mColor2Distance2White = new SparseArray<Long>();
        mTarget2Color         = new HashMap<ColorTarget, Integer>();

        if ( MapUtil.isEmpty(mColorScheme)) { return mTarget2Color;  }

        String sLightest = null;
        String sMiddlest = null;
        String sDarkest  = null;
        if ( mColorScheme != null ) {
            Map<String, String> mDefaultsForZeroAnd9 = new HashMap<String, String>();
            mDefaultsForZeroAnd9.put(DefKey.Color0.toString(), "#000000");
            mDefaultsForZeroAnd9.put(DefKey.Color9.toString(), "#FFFFFF");

            MapUtil.putAllOnlyNew(mDefaultsForZeroAnd9, mColorScheme);

            List<String> keys = new ArrayList<String>(mColorScheme.keySet());
            Collections.sort(keys);
            for(String key: keys) {
                if ( key.startsWith("Color") == false) { continue; } // e.g. the 'name'

                String sColor = fixColorString(mColorScheme.get(key));
                long iDeltaW = ColorUtil.getDistance2White(sColor);
                long iDeltaB = ColorUtil.getDistance2Black(sColor);
                int  iColor  = 0;
                try {
                    iColor = Color.parseColor(sColor);
                } catch (Exception e) {
                    // invalid color specified
                    iColor = COLOR_BLACK;
                }
                if ( iColor == COLOR_BLACK || iColor == COLOR_WHITE ) continue;

                mRGBDistance2Black   .put(sColor, iDeltaB);
                mColor2Distance2Black.put(iColor, iDeltaB);
                mRGB2Distance2White  .put(sColor, iDeltaW);
                mColor2Distance2White.put(iColor, iDeltaW);
            }
        }
        String[] keys   = context.getResources().getStringArray(R.array.ColorKeys);
        String[] values = getColorStringValues(keys);
        Map<String, String> mKey2Color = MapUtil.mapFromArrays(keys, values);

        //boolean bTextColorDynamically = PreferenceValues.getBoolean(PreferenceKeys.textColorDynamically, context, R.bool.textColorDynamically_default);
        DetermineTextColor determineTextColor = PreferenceValues.getTextColorDetermination(context);
        List<String> lMinMaxKeys2White = MapUtil.findKeysWithMinMax(mRGB2Distance2White);
        List<String> lMinMaxKeys2Black = MapUtil.findKeysWithMinMax(mRGBDistance2Black);
        sLightest = lMinMaxKeys2White.get(0);
        sDarkest  = lMinMaxKeys2Black.get(0);

        String sColorPrevTarget = null;
        for(ColorTarget colorTarget: ColorTarget.values() ) { // this loop asumes that background color and text color of the same 'element' are directly next to each other in the enumeration
            String sColorKey = getColor(colorTarget, context);
            String sColor    = fixColorString(mKey2Color.get(sColorKey));
            int    iColor    = Color.parseColor(sColor);

            if ( determineTextColor.equals(DetermineTextColor.Manual)==false && colorTarget.toString().endsWith("TextColor")) {
                boolean bChooseFromPalette = determineTextColor.equals(DetermineTextColor.AutoChooseFromPalette);
                // calculate an appropriate text color
                Long iDeltaW = mRGB2Distance2White.get(sColorPrevTarget);
                Long iDeltaB = mRGBDistance2Black .get(sColorPrevTarget);
                if ( iDeltaW == null ) {
                    iDeltaW = ColorUtil.getColorDistance("#FFFFFF", sColorPrevTarget);
                }
                if ( iDeltaB == null ) {
                    iDeltaB = ColorUtil.getColorDistance("#000000", sColorPrevTarget);
                }
                // background color was the previous one in our iteration
                if ( (iDeltaW != null) && (iDeltaB!=null) ) {
                    if (iDeltaW < iDeltaB) {
                        // background color is closer to white than to black
                        mTarget2Color.put(colorTarget, COLOR_BLACK);
                        Long iDeltaDarkest = ColorUtil.getColorDistance(sColorPrevTarget, sDarkest);
                        if (bChooseFromPalette) {
                            if (iDeltaDarkest > (iDeltaB / 2)) {
                                // darkest is good enough
                                mTarget2Color.put(colorTarget, Color.parseColor(sDarkest));
                            }
                        }
                    } else {
                        // background color is closer to black than to white
                        mTarget2Color.put(colorTarget, COLOR_WHITE);
                        Long iDeltaLightest = ColorUtil.getColorDistance(sColorPrevTarget, sLightest);
                        if (bChooseFromPalette) {
                            if (iDeltaLightest > (iDeltaW / 2) ) {
                                // lightest is good enough
                                mTarget2Color.put(colorTarget, Color.parseColor(sLightest));
                            }
                        }
/*
                        if ( sColorPrevTarget.equalsIgnoreCase("#FFFFFF") ) {
                            mTarget2Color.put(colorTarget, Color.parseColor(sDarkest));
                        } else if ( sColorPrevTarget.equalsIgnoreCase("#000000") ) {
                            mTarget2Color.put(colorTarget, Color.parseColor(sLightest));
                        } else {
                            mTarget2Color.put(colorTarget, COLOR_WHITE);
                        }
*/
                    }
                }
            } else {
                mTarget2Color.put(colorTarget, iColor);
                sColorPrevTarget = sColor;
            }

            ButtonUpdater.setPlayerColor(mTarget2Color.get(ColorTarget.playerButtonBackgroundColor));
        }

/*
        boolean bBaseBackgroundColorOnPaletteColors = false;
        if ( bBaseBackgroundColorOnPaletteColors ) {
            if ( bTextColorDynamically ) {
                // choose white or black dynamically (overwriting values already in the map)
                Long iMinDistance2White = mRGB2Distance2White.get(sLightest);
                Long iMinDistance2Black = mRGBDistance2Black .get(sDarkest);
                if ( (iMinDistance2White != null) && (iMinDistance2Black!=null) && iMinDistance2White < iMinDistance2Black ) {
                    //lightest color is 'closer' to white than darkest color is to black
                    mTarget2Color.put(ColorTarget.backgroundColor, COLOR_BLACK);
                    mTarget2Color.put(ColorTarget.mainTextColor  , Color.parseColor(sLightest));
                } else {
                    mTarget2Color.put(ColorTarget.backgroundColor, COLOR_WHITE);
                    mTarget2Color.put(ColorTarget.mainTextColor  , Color.parseColor(sDarkest));
                }
            }
        } else {
            // determine mainTextColor
            Integer iBgColorChoosen = mTarget2Color.get(ColorTarget.backgroundColor);
            String sColor    = fixColorString(mKey2Color.get(ColorTarget.backgroundColor));
        }
*/

        mRGB2Distance2White.remove(sLightest);
        mRGB2Distance2White.remove(sDarkest);
        if ( MapUtil.isNotEmpty(mRGB2Distance2White) ) {
            sMiddlest = mRGB2Distance2White.keySet().iterator().next();
        } else {
            sMiddlest = sDarkest;
        }

        int iLightest = Color.parseColor(sLightest);
        int iMiddlest = Color.parseColor(sMiddlest);
        int iDarkest  = Color.parseColor(sDarkest);

        mTarget2Color.put(ColorTarget.actionBarBackgroundColor, Color.parseColor(sDarkest) );
        mTarget2Color.put(ColorTarget.darkest , iDarkest);
        mTarget2Color.put(ColorTarget.middlest, iMiddlest);
        mTarget2Color.put(ColorTarget.lightest, iLightest);
        mTarget2Color.put(ColorTarget.black   , COLOR_BLACK);
        mTarget2Color.put(ColorTarget.white   , COLOR_WHITE);

        // not yet customizable
        mTarget2Color.put(ColorTarget.speakButtonBackgroundColor, mTarget2Color.get(ColorTarget.scoreButtonBackgroundColor));
        mTarget2Color.put(ColorTarget.tossButtonBackgroundColor , mTarget2Color.get(ColorTarget.serveButtonBackgroundColor));
        mTarget2Color.put(ColorTarget.shareButtonBackgroundColor, mTarget2Color.get(ColorTarget.serveButtonBackgroundColor));
        mTarget2Color.put(ColorTarget.timerButtonBackgroundColor     , mTarget2Color.get(ColorTarget.playerButtonBackgroundColor));
        mTarget2Color.put(ColorTarget.timerButtonBackgroundColor_Warn, mTarget2Color.get(ColorTarget.serveButtonBackgroundColor));

        mColor2Distance2Darker = new SparseArray<Long>();
        mColor2Distance2Darker.put(iLightest, ColorUtil.getColorDistance(sLightest, sMiddlest));
        mColor2Distance2Darker.put(iMiddlest, ColorUtil.getColorDistance(sMiddlest, sDarkest));

        Integer clActionBar = iDarkest;
        long l = ColorUtil.getDistance2Black(ColorUtil.getRGBString(clActionBar));
        if ( l > (ColorUtil.maxColorDistince/4) || mTarget2Color.get(ColorTarget.backgroundColor).equals(COLOR_BLACK) ) {
            clActionBar = COLOR_BLACK;
        }
        mTarget2Color.put(ColorTarget.actionBarBackgroundColor, clActionBar);

        return mTarget2Color;
    }

    public static void initDefaultColors(Map<ColorPrefs.ColorTarget, Integer> mColors) {
        mColors.put(ColorTarget.backgroundColor                 , R.color.dy_white );
        mColors.put(ColorTarget.mainTextColor                   , R.color.dy_dark  );
        mColors.put(ColorTarget.actionBarBackgroundColor        , R.color.dy_dark  );

        mColors.put(ColorTarget.speakButtonBackgroundColor      , R.color.dy_dark  );
        mColors.put(ColorTarget.scoreButtonBackgroundColor      , R.color.dy_dark  );
        mColors.put(ColorTarget.scoreButtonTextColor            , R.color.dy_yellow);

        mColors.put(ColorTarget.tossButtonBackgroundColor       , R.color.dy_dark  );
        mColors.put(ColorTarget.serveButtonBackgroundColor      , R.color.dy_dark  );
        mColors.put(ColorTarget.shareButtonBackgroundColor      , R.color.dy_dark  );
        mColors.put(ColorTarget.serveButtonTextColor            , R.color.dy_yellow);

        mColors.put(ColorTarget.timerButtonBackgroundColor      , R.color.dy_dark  );
        mColors.put(ColorTarget.timerButtonBackgroundColor_Warn , R.color.dy_dark  );
        mColors.put(ColorTarget.playerButtonBackgroundColor     , R.color.dy_dark  );
        mColors.put(ColorTarget.playerButtonTextColor           , R.color.dy_yellow);

        mColors.put(ColorTarget.historyBackgroundColor          , R.color.dy_yellow);
        mColors.put(ColorTarget.historyTextColor                , R.color.dy_dark  );

        ButtonUpdater.setPlayerColor(mColors.get(ColorTarget.playerButtonBackgroundColor));
    }

    private static Map<String, String> getColorScheme(Context context)
    {
        String overwritten = PreferenceValues.getOverwritten(PreferenceKeys.colorSchema);
        if ( StringUtil.isNotEmpty(overwritten) ) {
            try {
                overwritten = overwritten.replaceAll("\\|", "\n");
                Properties p = new Properties();
                p.load(new StringReader(overwritten));
                HashMap<String, String> map = new HashMap<String, String>();
                for ( Map.Entry e: p.entrySet() ) {
                    map.put(e.getKey().toString(), e.getValue().toString());
                }
                return map;
            } catch (Exception e) {
            }
        }

        int iUserSelectedColorSchemeName = PreferenceValues.getInteger(PreferenceKeys.colorSchema, context, 0);
        File file = getFile(context);
        PersistedListOfMaps plomColors = new PersistedListOfMaps(file, null);

        if ( file.exists() == false ) {
            // initial colors
            plomColors = initialColors(context, file);
        }

        List<Map<String,String>> list = null;
        if ( plomColors.read() ) {
            if ( new_colors_have_been_added == false ) {
                try {
                    int viewedChangelogVersion = PreferenceValues.getInteger(PreferenceKeys.viewedChangelogVersion, context, 0);
                    PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                    if ( (viewedChangelogVersion != packageInfo.versionCode) && (viewedChangelogVersion != 0) ) {
                        // new colors
                        String[] lDefaultClrs = Brand.getColorsArray(context);
                        String[] lHeaders     = StringUtil.singleCharacterSplit(lDefaultClrs[0]);
                        String[] lNewColors   = context.getResources().getStringArray(R.array.colorSchema_new);
                        PersistedListOfMaps.appendEntries(plomColors, Arrays.asList(lHeaders), lNewColors, iUserSelectedColorSchemeName + 1);
                    }
                    new_colors_have_been_added = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            list = plomColors.getContent();
        }

        if ( ListUtil.isEmpty(list) ) {
            // user deleted all colors palette entries!?
            plomColors = initialColors(context, file);
            list = plomColors.getContent();
        }

        // IH 20151210: added to prevent an IndexOutOfBoundsException
        if ( (iUserSelectedColorSchemeName < 0) || (iUserSelectedColorSchemeName >= ListUtil.size(list))) {
            iUserSelectedColorSchemeName = iUserSelectedColorSchemeName % ListUtil.size(list);
        }

        return list.get(iUserSelectedColorSchemeName);
    }
    private static boolean new_colors_have_been_added = false;

/*
    public static boolean activeColorSchema(Context context, int iResIdOfREColorSchemeName) {
        if ( iResIdOfREColorSchemeName == 0 ) { return false; }
        return activeColorSchema(context, context.getString( iResIdOfREColorSchemeName));
    }
    public static boolean activeColorSchema(Context context, String sREColorSchemeName) {
        String[] sColorStrings = Brand.getColorsArray(context);
        for(int i=1; i < sColorStrings.length; i++) { // we start at 1 (0 is the 'header')
            String sColorString = sColorStrings[i];
            if ( sColorString.matches(sREColorSchemeName) ) {
                RWValues.setNumber(PreferenceKeys.colorSchema, context, i-1);
                Log.d(TAG, String.format("Setting colorschema to %s (%s)", i-1, sColorString));
                return true;
            }
        }
        Log.w(TAG, String.format("Not setting colorschema to (%s)", sREColorSchemeName));
        return false;
    }
*/

    public static String getColorPrefsAsCss(Context context) {
        Map<ColorTarget, Integer> target2colorMapping = getTarget2colorMapping(context);
        StringBuilder sbCss = new StringBuilder(256);
      //appendColorStyleForKeys("body"         , target2colorMapping, sbCss, ColorTarget.backgroundColor            , ColorTarget.mainTextColor );
        appendColorStyleForKeys(".serveButton" , target2colorMapping, sbCss, ColorTarget.serveButtonBackgroundColor , ColorTarget.serveButtonTextColor);
        appendColorStyleForKeys(".scoreButton" , target2colorMapping, sbCss, ColorTarget.scoreButtonBackgroundColor , ColorTarget.scoreButtonTextColor);
        appendColorStyleForKeys(".playerButton", target2colorMapping, sbCss, ColorTarget.playerButtonBackgroundColor, ColorTarget.playerButtonTextColor);
        return sbCss.toString();
    }

    private static void appendColorStyleForKeys(String sStyleName, Map<ColorTarget, Integer> target2colorMapping, StringBuilder sbCss, ColorTarget ctBack, ColorTarget ctText) {
        sbCss.append(sStyleName).append(" {\n");
        sbCss.append(" background-color:").append(ColorUtil.getRGBString(target2colorMapping.get(ctBack))).append(";\n");
        sbCss.append(" color:"           ).append(ColorUtil.getRGBString(target2colorMapping.get(ctText))).append(";\n");
        sbCss.append("}\n");
        // append the inverse variation
        if ( sStyleName.startsWith(".") && sStyleName.endsWith("Inv") == false ) {
            appendColorStyleForKeys(sStyleName + "Inv", target2colorMapping, sbCss, ctText, ctBack);
        }
    }

    static CharSequence[] setListPreferenceEntries(ListPreference lp, CharSequence[] values, ColorTarget colorTarget) {
        if ( lp == null ) {
            //Log.d(TAG, "No gui element for " + colorTarget);
            return values;
        }

        if ( values == null ) {
            CharSequence[] keys = lp.getEntryValues();
            values = getColorStringValues(keys);
        }

        lp.setEntries    (values);
        //lp.setEntryValues(keys);
        return values;
    }

    static String[] getColorStringValues(CharSequence[] keys) {
        String[] values = new String[keys.length];
        int i=0;
        if ( mColorScheme != null ) {
            for(CharSequence key: keys ) {
                String sKey = key.toString();
                if ( sKey.startsWith("Color") == false) { continue; } // e.g. the 'name'
                values[i] = mColorScheme.get(sKey);
                i++;
            }
        }
        return values;
    }

    private static PersistedListOfMaps initialColors(Context context, File file) {
        String[] lInitialColors  = Brand.getColorsArray(context);
        return PersistedListOfMaps.loadFromArray(file, lInitialColors);
    }

    private static String fixColorString(String sColor) {
        if ( sColor.startsWith("#") ) {
            sColor = sColor.substring(1);
        }
        if ( sColor.matches("#?[a-fA-F0-9]{6}") == false ) {
            // remove invalid RGB color values
            sColor = sColor.replaceAll("[^a-fA-F0-9]", "");
        }
        if ( sColor.length() > 6 ) {
            sColor = sColor.substring(0,6);
        } else if ( sColor.length() < 6 ) {
            sColor = StringUtil.lpad(sColor, '0', 6);
        }
        if ( StringUtil.length(sColor) == 6 && sColor.startsWith("#")==false ) {
            sColor = "#" + sColor;
        }
        return sColor;
    }



    static void setColorSchemaIcon(Preference lColorSchema) {
        if ( lColorSchema == null ) { return; }
        if ( mTarget2Color == null ) { return; }
        int iColorL = mTarget2Color.get(ColorTarget.lightest);
        int iColorM = mTarget2Color.get(ColorTarget.middlest);
        int iColorD = mTarget2Color.get(ColorTarget.darkest);

        GradientDrawable icon = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{iColorL, iColorL, iColorM, iColorM, iColorD, iColorD});
        icon.setSize(ICON_SIZE, ICON_SIZE);
        lColorSchema.setIcon(icon);
    }

    static void setColorTargetIcons(Preferences.SettingsFragment fragment) {
        for(ColorTarget colorTarget: ColorTarget.values() ) {
            Preference preference = fragment.findPreference(colorTarget);
            if ( preference == null) { continue; }
            setColorTargetPreferenceIcon(colorTarget, null, preference);
        }
    }
    static void setColorTargetIcons(PreferenceScreen screen) {
        for(ColorTarget colorTarget: ColorTarget.values() ) {
            String key = colorTarget.toString();
            Preference preference = screen.findPreference(key);
            if ( preference == null ) { continue; }
            setColorTargetPreferenceIcon(colorTarget, null, preference);
        }
    }

    static void setColorTargetPreferenceIcon(ColorTarget colorTarget, String sColor, Preference preference) {
        if ( mTarget2Color == null ) { return; }
        try {
            ListPreference listPreference = (ListPreference) preference;
            //ColorTarget colorTarget = ColorTarget.valueOf(key);
            if ( sColor != null ) {
                mTarget2Color.put(colorTarget, Color.parseColor(sColor));
            }
            int iColor = mTarget2Color.get(colorTarget);

            GradientDrawable icon = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{iColor, iColor});
            icon.setSize(ICON_SIZE, ICON_SIZE);
            listPreference.setIcon(icon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setColors(Activity activity, View view) {
        if ( mTarget2Color == null ) { return; }

        setColors(view, null);
      //boolean bSwitchActionBarColor = false;
        if ( /*bSwitchActionBarColor &&*/ (activity!=null) ) {
            Integer color = mTarget2Color.get(ColorTarget.actionBarBackgroundColor);
            if ( activity instanceof XActivity ) {
                XActivity xActivity = (XActivity) activity;
                xActivity.setActiorBarBGColor(color);
            } else {
                ActionBar actionBar = activity.getActionBar();
                if (actionBar != null) {
                    if (color != null) {
                        actionBar.setBackgroundDrawable(new ColorDrawable(color));
                    }
                }
            }
        }
        if ( view != null ) {
            Integer iMainBgColor = mTarget2Color.get(ColorTarget.backgroundColor);
            view.setBackgroundColor(iMainBgColor);
        }
    }

    public static void setColors(View view, Tags parentTagContains) {
        if ( mTarget2Color == null ) { return; }

        if ( view instanceof ViewGroup ) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                setColor(v, parentTagContains);
            }
        } else {
            setColor(view, parentTagContains);
        }
    }

    public enum Tags {
        header,
        item,

        mainBackgroundColor,
        mainTextColor,
        noBackgroundColor,
    }

    public static void setColor(View v) {
        setColor(v, null);
    }
    private static void setColor(View v, Tags parentTagContains) {
        if ( mTarget2Color == null ) { return; }
        if ( v == null ) { return; }

        String tag = String.valueOf(v.getTag());
        boolean bHasItemTag   = tag.contains(Tags.item.toString())  ;
        boolean bHasHeaderTag = tag.contains(Tags.header.toString());
        if ( bHasHeaderTag == false && bHasItemTag == false ) {
            bHasItemTag   = Tags.item  .equals(parentTagContains);
            bHasHeaderTag = Tags.header.equals(parentTagContains);
        }
        if (v instanceof TextView) {
            TextView textView = (TextView) v;
/*
            if (textView instanceof AutoCompleteTextView) {
                setBackground(textView, mTarget2Color.get(ColorTarget.playerButtonBackgroundColor));
                //v.setBackgroundColor(mTarget2Color.get(ColorTarget.playerButtonBackgroundColor));
                textView.setTextColor(mTarget2Color.get(ColorTarget.playerButtonTextColor));
                //Log.w(TAG, "Not setting color for " + textView.getClass().getName() + " for now...");
                return;
            }
*/
            if (bHasHeaderTag) {
                // e.g for a list view header
                setBackground(textView, mTarget2Color.get(ColorTarget.scoreButtonBackgroundColor), tag);
                textView.setTextColor(mTarget2Color.get(ColorTarget.scoreButtonTextColor));
            } else {
                boolean bHasMainTextColorTag = tag.contains(Tags.mainTextColor.toString());
                if (bHasItemTag) {
                    // e.g for a list view item
                    setBackground(textView, mTarget2Color.get(ColorTarget.playerButtonBackgroundColor), tag);
                    textView.setTextColor(mTarget2Color.get(ColorTarget.playerButtonTextColor));
                } else {
                    if ( textView instanceof EditText ) {
                        setBackground(textView, mTarget2Color.get(ColorTarget.playerButtonBackgroundColor), tag);
                        textView.setTextColor(mTarget2Color.get(ColorTarget.playerButtonTextColor));
                    } else if (v instanceof Button && (v instanceof CheckBox == false)) {
                        Button button = (Button) v;
                        setBackground(button, mTarget2Color.get(ColorTarget.scoreButtonBackgroundColor), tag);
                        button.setTextColor(mTarget2Color.get(ColorTarget.scoreButtonTextColor));
                    } else if (v instanceof CheckBox) {
                        CheckBox button = (CheckBox) v;
                        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                            // make only the border of the checkbox a different color (mainly to prevent white on white)
                            Drawable drawable = button.getButtonDrawable();
                            ColorFilter colorFilter = new PorterDuffColorFilter(mTarget2Color.get(ColorTarget.playerButtonBackgroundColor), PorterDuff.Mode.SRC_ATOP);
                            drawable.setColorFilter(colorFilter);

                            button.setTextColor(mTarget2Color.get(ColorTarget.mainTextColor));
                        } else {
                            setBackground(button, mTarget2Color.get(ColorTarget.playerButtonBackgroundColor), tag);
                            button.setTextColor(mTarget2Color.get(ColorTarget.playerButtonTextColor));
                        }
                    } else {
                        setBackground(textView, mTarget2Color.get(ColorTarget.backgroundColor), tag);
                        textView.setTextColor(mTarget2Color.get(ColorTarget.mainTextColor));
                    }
                    if ( bHasMainTextColorTag ) {
                        textView.setTextColor(mTarget2Color.get(ColorTarget.mainTextColor));
                    }
                }
            }
        } else if ( v instanceof ImageButton ) {
            ImageButton imageButton = (ImageButton) v;
            if ( bHasHeaderTag ) {
                // e.g for a list view header
                //imageButton.setBackgroundColor(mTarget2Color.get(ColorTarget.scoreButtonBackgroundColor));
                setBackground(imageButton, mTarget2Color.get(ColorTarget.scoreButtonBackgroundColor), tag);
            } else {
                //imageButton.setBackgroundColor(mTarget2Color.get(ColorTarget.playerButtonBackgroundColor));
                setBackground(imageButton, mTarget2Color.get(ColorTarget.playerButtonBackgroundColor), tag);
            }
        } else if ( v instanceof Spinner) {
            Spinner spinner = (Spinner) v;
            spinner.setBackgroundColor(mTarget2Color.get(ColorTarget.playerButtonBackgroundColor));
        } else if ( v instanceof NumberPicker) {
            NumberPicker numberPicker = (NumberPicker) v;
            numberPicker.setBackgroundColor(mTarget2Color.get(ColorTarget.playerButtonBackgroundColor));
        } else if ( v instanceof ExpandableListView) {
            //ExpandableListView lv = (ExpandableListView) v;
            // each item must receive colors when expanding
        } else if ( v instanceof ViewGroup ) {
            ViewGroup vgChild = (ViewGroup) v;
            if ( bHasHeaderTag ) {
                // e.g for a list view header relative layout
                setBackground(vgChild, mTarget2Color.get(ColorTarget.scoreButtonBackgroundColor), tag);
            } else if (bHasItemTag) {
                // e.g for a composite list view item
                setBackground(vgChild, mTarget2Color.get(ColorTarget.playerButtonBackgroundColor), tag);
            } else if ( tag.contains(Tags.mainBackgroundColor.toString())) {
                setBackground(vgChild, mTarget2Color.get(ColorTarget.backgroundColor), tag);
            }
/*
            if ( v instanceof android.support.v4.view.ViewPager) {
                android.support.v4.view.ViewPager viewPager = (ViewPager) v;
                viewPager.sets
            }
*/
            setColors(vgChild, bHasHeaderTag?Tags.header : (bHasItemTag?Tags.item : null));
        } else {
            // skipping
            //Log.d(TAG, "Not setting color for " + v.getClass() + " " + v.getId());
        }
    }


    private static void setBackground(View v, int iColor, String sTag) {
        boolean bNoBgColor = sTag.contains(Tags.noBackgroundColor.toString());
        if ( bNoBgColor ) {
            //Log.d(TAG, "Skipping " + v + " for bg color based on tag " + sTag);
            return;
        }

        Drawable background = v.getBackground();
        if ( background != null ) {
            // better to 'maintain' rounded corners for buttons
            ColorFilter colorFilter = new PorterDuffColorFilter(iColor, PorterDuff.Mode.SRC_ATOP);
            background.setColorFilter(colorFilter);
        } else {
            // TODO: ?? there was no background before: don't set one: assume it has been set on parent or not
            v.setBackgroundColor(iColor);
        }
    }

    /** if the key contains 'background', color0 will server as default, else color9 serves as default */
    private static String getColor(ColorTarget key, Context context) {
        // reload all defaults
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sColorTargetKey = key.toString();
        String sDefault = sColorTargetKey.toLowerCase().contains("background")?DefKey.Color0.toString():DefKey.Color9.toString();
        String sColorKey = prefs.getString(sColorTargetKey, sDefault);
        if ( StringUtil.isEmpty(sColorKey) ) {
            sColorKey = sDefault;
        }
        return sColorKey;
    }

    private enum DefKey {
        /** default for the text */
        Color0,
        /** default for the background */
        Color9,
    }
}