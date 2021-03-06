package com.doubleyellow.scoreboard.model;

import java.util.Map;

/**
 * Not to be confused with Squash 57, a form of squash previously called racketball.
 *
 * Losing the serve is called a side out in singles.
 * In doubles, when the first server loses the serve, it is called a handout and when the second server loses the serve, it is a side out.
 *
 * http://www.racquetballrules.us/basic-racquetball-rules/
 * http://www.teamusa.org/usa-racquetball/how-to-play/rules
 * www.teamusa.org/-/media/USA_Racquetball/Documents/Rules/USAR-Rulebook.pdf
 *
 * Each player or team is entitled to three 30-second timeouts in games to 15 and two 30-second timeouts
 in games to 11. Timeouts may not be called by either side once the service motion has begun. Calling for a timeout when
 none remain or after the service motion has begun will result in the assessment of a technical foul for delay of game. If a
 player takes more than 30 seconds for a single timeout, the referee may automatically charge any remaining timeouts, as
 needed, for any extra time taken.

 The rest period between the first two games of a match is 2 minutes. If a tiebreaker is necessary,
 the rest period between the second and third game is 5 minutes.
 *
 * A single match is made up of three games, where the first two games go until 15 points and the last game only goes until 11.
 */
public class RacquetballModel extends Model
{
    public RacquetballModel() {
        super();
        setEnglishScoring(true); // english scoring used for Racquetball
        setNrOfGamesToWinMatch(2);
        setNrOfPointsToWinGame(15);
    }

    @Override public SportType getSport() {
        return SportType.Racquetball;
    }

    @Override public Sport getSportForGame(int iGame1B) {
        return Sport.Squash;
    }

    /**
     * In racketlon the last (third) game typically only goes to 11 in stead of 15
     */
    @Override public int getNrOfPointsToWinGame() {
        int nrOfPointsToWinGame = super.getNrOfPointsToWinGame();
        int iGameNrNext1B = getGameNrInProgress();
        if ( (iGameNrNext1B == getNrOfGamesToWinMatch() * 2 - 1) && (nrOfPointsToWinGame == 15) ) {
            return 11;
        } else {
            return nrOfPointsToWinGame;
        }
    }

    //-------------------------------
    // serve side/sequence
    //-------------------------------

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        // TODO:
    }

    @Override Player determineServerForNextGame(int iGameZB, int iScoreA, int iScoreB) {
        return determineServerForNextGame_Squash(iScoreA, iScoreB);
    }

    @Override public boolean showChangeSidesMessageInGame(int iGameZB) {
        return false;
    }

    @Override DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        return m_doubleServeSequence;
    }

    @Override public String convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        return sRLInternational + sHandoutChar;
    }

    //-------------------------------
    // game/match ball
    //-------------------------------

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] paGameVictoryFor) {
        return super.isPossibleMatchBallFor_Squash_TableTennis(when, paGameVictoryFor);
    }

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        return super.calculateIsPossibleGameVictoryFor_Squash_Tabletennis(when, gameScore);
    }

    //-------------------------------
    // score
    //-------------------------------

    @Override public void changeScore(Player player) {
        super.changeScore_Squash_Racketlon(player, true, null);
    }

    @Override public String getResultShort() {
        return super.getResultShort_Squash_TableTennis();
    }

    //-------------------------------
    // JSON
    //-------------------------------

    //-------------------------------
    // conduct/appeal
    //-------------------------------

    @Override public void recordAppealAndCall(Player appealing, Call call) {
        super.recordAppealAndCall_Squash_Racketlon(appealing, call);
    }

    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) {
        super.recordConduct_Squash_Racketlon(pMisbehaving, call, conductType);
    }
}
