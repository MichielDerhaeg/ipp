/*
 * Copyright (C) 2014-2015 ULYSSIS VZW
 *
 * This file is part of i++.
 * 
 * i++ is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Affero General Public License
 * as published by the Free Software Foundation. No other versions apply.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.ulyssis.ipp.control.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;

@JsonTypeName("Correction")
public class CorrectionCommand extends Command {
    private final int teamNb;
    private final int correction;

    /**
     * Create a CorrectionCommand
     * @param teamNb
     *        The team number to correct
     * @param correction
     *        The correction in nb. of laps. Positive adds laps,
     *        negative removes laps.
     */
    public CorrectionCommand(int teamNb, int correction) {
        super();
        this.teamNb = teamNb;
        this.correction = correction;
    }

    public CorrectionCommand(Instant time, int teamNb, int correction) {
        super(time);
        this.teamNb = teamNb;
        this.correction = correction;
    }

    @JsonCreator
    private CorrectionCommand(@JsonProperty("commandId") String commandId,
                              @JsonProperty("time") Instant time,
                              @JsonProperty("teamNb") int teamNb,
                              @JsonProperty("correction") int correction) {
        super(commandId, time);
        this.teamNb = teamNb;
        this.correction = correction;
    }

    public int getTeamNb() {
        return teamNb;
    }

    public int getCorrection() {
        return correction;
    }
}
