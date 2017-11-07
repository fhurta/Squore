package com.doubleyellow.scoreboard.dialog.announcement;

import android.content.Context;
import com.doubleyellow.scoreboard.dialog.StartEndAnnouncement;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

public class EndMatchAnnouncement extends StartEndAnnouncement {
    public EndMatchAnnouncement(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }
}