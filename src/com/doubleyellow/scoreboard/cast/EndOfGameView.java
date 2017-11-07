package com.doubleyellow.scoreboard.cast;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.history.GameGraphView;
import com.doubleyellow.scoreboard.history.MatchGameScoresView;
import com.doubleyellow.scoreboard.main.SBTimerView;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TimerView;
import com.doubleyellow.scoreboard.timer.TimerViewContainer;
import com.doubleyellow.scoreboard.vico.IBoard;

import java.util.Map;

public class EndOfGameView implements TimerViewContainer
{
            ViewGroup          root;
    private Context            context     = null;
    private IBoard             iBoard      = null;
    private TextView           txtTimer    = null;
            boolean            bIsShowing  = false;
    private TimerView          timerView   = null;
    private Model              matchModel  = null;

    public EndOfGameView(Context context, IBoard iBoard, Model model) {
        this.context    = context;
        this.iBoard     = iBoard;
        this.matchModel = model;
    }
    void setModel(Model model) {
        this.matchModel = model;
    }

    @Override public TimerView getTimerView() {
        if ( timerView == null ) {
            if ( txtTimer != null ) {
                timerView = new SBTimerView(txtTimer, null, context, iBoard);
            }
        }
        return timerView;
    }
    public void show(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        root = (ViewGroup) inflater.inflate(R.layout.presentation_end_of_game, parent);
        show();
    }
    public void show(Activity screen) {
        screen.setContentView(R.layout.presentation_end_of_game);
        root = (ViewGroup) screen.findViewById(android.R.id.content);
        show();
    }
    void show(PresentationScreen screen) {
        screen.setContentView(R.layout.presentation_end_of_game);
        root = (ViewGroup) screen.findViewById(android.R.id.content);
        show();
    }
    private void show() {
        if ( root == null ) { return; }

        bIsShowing = true;

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        ColorUtil.setBackground(root, mColors.get(ColorPrefs.ColorTarget.black));

        // enforce re-create of timerview
        Timer.removeTimerView(timerView);
        timerView = null;

                            txtTimer = (TextView           ) root.findViewById(R.id.peog_timer     );
        GameGraphView       ggv      = (GameGraphView      ) root.findViewById(R.id.peog_gamegraph );
        MatchGameScoresView mgsv     = (MatchGameScoresView) root.findViewById(R.id.peog_gamescores);
        if ( ggv == null ) {
            ggv = ViewUtil.getFirstView(root, GameGraphView.class);
        }

        if ( ggv != null ) {
            ggv.showGame(matchModel, matchModel.getNrOfFinishedGames());
        }
        if ( mgsv != null ) {
            mgsv.setProperties( mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor       )
                              , mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor )
                              , mColors.get(ColorPrefs.ColorTarget.serveButtonBackgroundColor )
                              , mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor)
            );
            mgsv.update(matchModel, Player.A);
        }
        if ( txtTimer !=null ) {
            if ( matchModel.matchHasEnded() ) {
                txtTimer.setVisibility(View.INVISIBLE); // not GONE, so logo layout remains the same
            } else {
                TimerView timerView = this.getTimerView();
                Timer.addTimerView(timerView);
            }
        }

        for(int iResId: Brand.imageViewIds ) {
            ImageView ivBrandLogo = (ImageView) root.findViewById(iResId);
            if (ivBrandLogo != null) {
                if (iResId != Brand.getImageViewResId()) {
                    ivBrandLogo.setVisibility(View.GONE);
                    continue;
                }
                int iResIdLogo = Brand.getLogoResId(); // the splash screen logo
                if ( iResIdLogo != 0 ) {
                    ivBrandLogo.setImageResource(iResIdLogo);
                    //ivBrandLogo.setBackgroundColor(Brand.getBgColor(screen.getContext()));
                    ivBrandLogo.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                    ivBrandLogo.setVisibility(View.VISIBLE);
                    ivBrandLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                } else {
                    ivBrandLogo.setVisibility(View.INVISIBLE);
                }
            }
        }
        bIsShowing = true;
    }
}