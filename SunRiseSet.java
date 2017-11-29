/*
 * Copyright (C) 2017 menthan
 *
 * This program is free software: you can redistribute it and/or modify
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
package remotehub;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 *
 * @author menthan
 */
public class SunRiseSet {

    private final double breite;
    private final double laenge;
    private final int zeitzone;

    public SunRiseSet(double breite, double laenge, int zeitzone) {
        this.breite = breite;
        this.laenge = laenge;
        this.zeitzone = zeitzone;
    }

    public LocalTime getTime(String upDown) {
        final double eventTime;
        final double zenith = getZenith();
        if (upDown.equalsIgnoreCase("up") || upDown.equalsIgnoreCase("rise")) {
            eventTime = zenith - zeitdifferenz();
        } else {
            eventTime = zenith + zeitdifferenz();
        }

        int hour = (int) Math.floor(eventTime);
        int minute = (int) Math.floor(((eventTime - hour) * 100) * 3 / 5);
        return LocalTime.of(hour, minute);
    }

    public boolean dayTime() {
        final LocalTime now = LocalTime.now(ZoneId.of("Europe/Berlin"));
        return now.isAfter(getTime("Up")) && now.isBefore(getTime("Down"));
    }

    private double sonnendeklination() {
        return 0.40954 * Math.sin(0.0172 * (LocalDate.now().getDayOfYear() - 79.35));
    }

    private double zeitdifferenz() {
        final double deklination = sonnendeklination();
        final double b = breite * Math.PI / 180.0;
        final double h = -(50.0 / 60.0) * Math.PI / 180.0;
        return 12.0 * Math.acos((Math.sin(h) - Math.sin(b) * Math.sin(deklination)) / (Math.cos(b) * Math.cos(deklination))) / Math.PI;
    }

    private double zeitgleichung() {
        final int day = LocalDate.now().getDayOfYear();
        return -0.1752 * Math.sin(0.033430 * day + 0.5474) - 0.1340 * Math.sin(0.018234 * day - 0.1939);
    }

    private double getZenith() {
        return 12 - zeitgleichung() - laenge / 15.0 + zeitzone;
    }
}
