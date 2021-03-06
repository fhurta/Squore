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

package com.doubleyellow.scoreboard.prefs;

/**
 * Keys (values before the = sign) that may be used in the configuration
 * value of the preference 'feedPostUrls'
 */
public enum URLsKeys {
    Name,
    FeedPlayers,
    FeedMatches,
    /** an URL where match result should be posted to */
    PostResult,
    /**
     * Should contain a value of enum PostDataPreference
     */
    PostData,
    ValidFrom,
    ValidTo,
    /* None, Basic, ... */
    Authentication,
    Organization,
    Country,
        CountryCode,
    Region,

    /* Json config feed keys */
    config,
        avatarBaseURL,
        expandGroup,
        Format,
        skipMatchSettings,
}
