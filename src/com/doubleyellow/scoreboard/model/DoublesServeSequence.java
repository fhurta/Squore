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

package com.doubleyellow.scoreboard.model;

public enum DoublesServeSequence {
    /** in a official doubles match start serving in an 'out' state */
    //BXYA,
    /** same as previous only starting in 'in' state */
    //ABXY,
    /** alternating. Looks the official sequence if reading http://www.worldsquash.org/ws/wp-content/uploads/2014/11/150101_Doubles-Rules-Final.pdf */
    //AXBY,
    /** e.g. one in front/back is always serving */
    //AXAX,

    /**
     * Encoded serve sequences
     * To determine what happens take 2 character sequence from infinate string
     * Start + Repeat + Repeat + Repeat
     *
     * Second character says who needs to serve: first (i,I) or second (o,O) player of a team
     * If first character is lowercase than team needs to change, if first character is uppercase stay within same team
     **/
    A2B1B2_then_A1A2B1B2("o", "Io"  ), //  2_12_12_12_12_12 = BXY then ABXY = BXYA
    A1B1B2_then_A1A2B1B2("i", "Io"  ), //  1_12_12_12_12_12 = AXY then ABXY
    A1A2B1B2            ("" , "Io"  ), // 12_12_12_12_12_12 =               = ABXY
    A1B1A1B1            ("" , "i"   ), // 1_1_1_1_1_1_1_1_1 =               = AXAX
    A1B1A2B2            ("" , "io"  ), // 1_1_2_2_1_1_2_2_1 =               = AXBY

    /** just to have a value if we are not playing doubles */
    NA("", ""),
    ;
    private String sStart;
    private String sRepeat;
    DoublesServeSequence(String sStart, String sRepeat) {
        this.sStart  = sStart;
        this.sRepeat = sRepeat;
    }
    /** should not be called for 'semi-handout to winner of last point' at start of new game
     * return 1 for in  (first player of same team should serve next)
     * return 2 for out (second player should serve)
     **/
    public boolean switchTeam(DoublesServe io, boolean bUseStart) {
        String sRet = get2SequentialChars(io, bUseStart);
        String sFirstChar = sRet.substring(0,1);
        return sFirstChar.equals(sFirstChar.toLowerCase());
    }
    public DoublesServe playerToServe(DoublesServe io, boolean bUseStart) {
        String sRet = get2SequentialChars(io, bUseStart);
        String sSecondChar = sRet.substring(1,2);
        DoublesServe doublesServe = DoublesServe.NA;
        try {
            doublesServe = DoublesServe.valueOf(sSecondChar.toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doublesServe;
    }

    private String get2SequentialChars(DoublesServe io, boolean bUseStart) {
        String sCars = (bUseStart?sStart:"") + sRepeat + sRepeat;
        int iIdx = sCars.toUpperCase().indexOf(io.toString());
        if ( iIdx >= 0 ) {
            return sCars.substring(iIdx, iIdx+2);
        } else {
            return "_" + sCars.substring(0,1);
        }
    }
/*
    public boolean switchTeamBack(DoublesServe io, boolean bUseStart) {
        String sRet = get2SequentialCharsBackwards(io, bUseStart);
        String sFirstChar = sRet.substring(0,1);
        return sFirstChar.equals(sFirstChar.toLowerCase()) == false;
    }
    public DoublesServe playerToServeBack(DoublesServe io, boolean bUseStart) {
        String sRet = get2SequentialCharsBackwards(io, bUseStart);
        String sSecondChar = sRet.substring(1,2);
        return DoublesServe.valueOf(sSecondChar.toUpperCase()).getOther();
    }
    private String get2SequentialCharsBackwards(DoublesServe io, boolean bUseStart) {
        String sCars = sRepeat + sRepeat;
        if ( bUseStart && StringUtil.isNotEmpty(sStart) ) {
            sCars = sStart + sRepeat;
        }
        int iIdx = sCars.toUpperCase().lastIndexOf(io.toString());
        if ( iIdx >= 1 ) {
            return sCars.substring(iIdx-1, iIdx+1);
        } else {
            return "_" + sCars.substring(0,1);
        }
    }
*/
}
