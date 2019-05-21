/*
 * Copyright 2008-2009 Mike Reedell / LuckyCatLabs
 * Copyright 2019 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.custom.display;

import android.location.Location;

import java.util.Calendar;
import java.util.TimeZone;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Public interface for getting the various types of sunrise/sunset.
 */
public class SunriseSunsetCalculator {

    private Location location;

    private SolarEventCalculator calculator;

    /**
     * Constructs a new <code>SunriseSunsetCalculator</code> with the given <code>Location</code>
     *
     * @param location
     *            <code>Location</code> object containing the Latitude/Longitude of the location to compute
     *            the sunrise/sunset for.
     * @param timeZoneIdentifier
     *            String identifier for the timezone to compute the sunrise/sunset times in. In the form
     *            "America/New_York". Please see the zi directory under the JDK installation for supported
     *            time zones.
     */
    public SunriseSunsetCalculator(Location location, String timeZoneIdentifier) {
        this.location = location;
        this.calculator = new SolarEventCalculator(location, timeZoneIdentifier);
    }

    /**
     * Constructs a new <code>SunriseSunsetCalculator</code> with the given <code>Location</code>
     *
     * @param location
     *            <code>Location</code> object containing the Latitude/Longitude of the location to compute
     *            the sunrise/sunset for.
     * @param timeZone
     *            timezone to compute the sunrise/sunset times in.
     */
    public SunriseSunsetCalculator(Location location, TimeZone timeZone) {
        this.location = location;
        this.calculator = new SolarEventCalculator(location, timeZone);
    }

    /**
     * Returns the astronomical (108deg) sunrise for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the astronomical sunrise for.
     * @return the astronomical sunrise time in HH:MM (24-hour clock) form.
     */
    public String getAstronomicalSunriseForDate(Calendar date) {
        return calculator.computeSunriseTime(Zenith.ASTRONOMICAL, date);
    }

    /**
     * Returns the astronomical (108deg) sunrise for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the astronomical sunrise for.
     * @return the astronomical sunrise time as a Calendar
     */
    public Calendar getAstronomicalSunriseCalendarForDate(Calendar date) {
        return calculator.computeSunriseCalendar(Zenith.ASTRONOMICAL, date);
    }

    /**
     * Returns the astronomical (108deg) sunset for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the astronomical sunset for.
     * @return the astronomical sunset time in HH:MM (24-hour clock) form.
     */
    public String getAstronomicalSunsetForDate(Calendar date) {
        return calculator.computeSunsetTime(Zenith.ASTRONOMICAL, date);
    }

    /**
     * Returns the astronomical (108deg) sunset for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the astronomical sunset for.
     * @return the astronomical sunset time as a Calendar
     */
    public Calendar getAstronomicalSunsetCalendarForDate(Calendar date) {
        return calculator.computeSunsetCalendar(Zenith.ASTRONOMICAL, date);
    }

    /**
     * Returns the nautical (102deg) sunrise for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the nautical sunrise for.
     * @return the nautical sunrise time in HH:MM (24-hour clock) form.
     */
    public String getNauticalSunriseForDate(Calendar date) {
        return calculator.computeSunriseTime(Zenith.NAUTICAL, date);
    }

    /**
     * Returns the nautical (102deg) sunrise for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the nautical sunrise for.
     * @return the nautical sunrise time as a Calendar
     */
    public Calendar getNauticalSunriseCalendarForDate(Calendar date) {
        return calculator.computeSunriseCalendar(Zenith.NAUTICAL, date);
    }

    /**
     * Returns the nautical (102deg) sunset for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the nautical sunset for.
     * @return the nautical sunset time in HH:MM (24-hour clock) form.
     */
    public String getNauticalSunsetForDate(Calendar date) {
        return calculator.computeSunsetTime(Zenith.NAUTICAL, date);
    }

    /**
     * Returns the nautical (102deg) sunset for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the nautical sunset for.
     * @return the nautical sunset time as a Calendar
     */
    public Calendar getNauticalSunsetCalendarForDate(Calendar date) {
        return calculator.computeSunsetCalendar(Zenith.NAUTICAL, date);
    }

    /**
     * Returns the civil sunrise (twilight, 96deg) for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the civil sunrise for.
     * @return the civil sunrise time in HH:MM (24-hour clock) form.
     */
    public String getCivilSunriseForDate(Calendar date) {
        return calculator.computeSunriseTime(Zenith.CIVIL, date);
    }

    /**
     * Returns the civil sunrise (twilight, 96deg) for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the civil sunrise for.
     * @return the civil sunrise time as a Calendar
     */
    public Calendar getCivilSunriseCalendarForDate(Calendar date) {
        return calculator.computeSunriseCalendar(Zenith.CIVIL, date);
    }

    /**
     * Returns the civil sunset (twilight, 96deg) for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the civil sunset for.
     * @return the civil sunset time in HH:MM (24-hour clock) form.
     */
    public String getCivilSunsetForDate(Calendar date) {
        return calculator.computeSunsetTime(Zenith.CIVIL, date);
    }

    /**
     * Returns the civil sunset (twilight, 96deg) for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the civil sunset for.
     * @return the civil sunset time as a Calendar
     */
    public Calendar getCivilSunsetCalendarForDate(Calendar date) {
        return calculator.computeSunsetCalendar(Zenith.CIVIL, date);
    }

    /**
     * Returns the official sunrise (90deg 50', 90.8333deg) for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the official sunrise for.
     * @return the official sunrise time in HH:MM (24-hour clock) form.
     */
    public String getOfficialSunriseForDate(Calendar date) {
        return calculator.computeSunriseTime(Zenith.OFFICIAL, date);
    }

    /**
     * Returns the official sunrise (90deg 50', 90.8333deg) for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the official sunrise for.
     * @return the official sunrise time as a Calendar
     */
    public Calendar getOfficialSunriseCalendarForDate(Calendar date) {
        return calculator.computeSunriseCalendar(Zenith.OFFICIAL, date);
    }

    /**
     * Returns the official sunrise (90deg 50', 90.8333deg) for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the official sunset for.
     * @return the official sunset time in HH:MM (24-hour clock) form.
     */
    public String getOfficialSunsetForDate(Calendar date) {
        return calculator.computeSunsetTime(Zenith.OFFICIAL, date);
    }

    /**
     * Returns the official sunrise (90deg 50', 90.8333deg) for the given date.
     *
     * @param date
     *            <code>Calendar</code> object containing the date to compute the official sunset for.
     * @return the official sunset time as a Calendar
     */
    public Calendar getOfficialSunsetCalendarForDate(Calendar date) {
        return calculator.computeSunsetCalendar(Zenith.OFFICIAL, date);
    }

    /**
     * Computes the sunrise for an arbitrary declination.
     *
     * @param location
     *            location to compute the sunrise/sunset for.
     * @param timeZone
     *            timezone to compute the sunrise/sunset times in.
     * @param date
     *            <code>Calendar</code> object containing the date to compute the official sunset for.
     * @param degrees
     *            Angle under the horizon for which to compute sunrise. For example, "civil sunrise"
     *            corresponds to 6 degrees.
     * @return the requested sunset time as a <code>Calendar</code> object.
     */

    public static Calendar getSunrise(Location location, TimeZone timeZone, Calendar date, double degrees) {
        SolarEventCalculator solarEventCalculator = new SolarEventCalculator(location, timeZone);
        return solarEventCalculator.computeSunriseCalendar(new Zenith(90 - degrees), date);
    }

    /**
     * Computes the sunset for an arbitrary declination.
     *
     * @param location
     *            location to compute the sunrise/sunset for.
     * @param timeZone
     *            timezone to compute the sunrise/sunset times in.
     * @param date
     *            <code>Calendar</code> object containing the date to compute the official sunset for.
     * @param degrees
     *            Angle under the horizon for which to compute sunrise. For example, "civil sunset"
     *            corresponds to 6 degrees.
     * @return the requested sunset time as a <code>Calendar</code> object.
     */

    public static Calendar getSunset(Location location, TimeZone timeZone, Calendar date, double degrees) {
        SolarEventCalculator solarEventCalculator = new SolarEventCalculator(location, timeZone);
        return solarEventCalculator.computeSunsetCalendar(new Zenith(90 - degrees), date);
    }

    /**
     * Returns the location where the sunrise/sunset is calculated for.
     *
     * @return <code>Location</code> object representing the location of the computed sunrise/sunset.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Defines the solar declination used in computing the sunrise/sunset.
     */
    private static class Zenith {
        /** Astronomical sunrise/set is when the sun is 18 degrees below the horizon. */
        public static final Zenith ASTRONOMICAL = new Zenith(108);

        /** Nautical sunrise/set is when the sun is 12 degrees below the horizon. */
        public static final Zenith NAUTICAL = new Zenith(102);

        /** Civil sunrise/set (dawn/dusk) is when the sun is 6 degrees below the horizon. */
        public static final Zenith CIVIL = new Zenith(96);

        /** Official sunrise/set is when the sun is 50' below the horizon. */
        public static final Zenith OFFICIAL = new Zenith(90.8333);

        private final BigDecimal degrees;

        public Zenith(double degrees) {
            this.degrees = BigDecimal.valueOf(degrees);
        }

        public BigDecimal degrees() {
            return degrees;
        }
    }

    /**
     * Parent class of the Sunrise and Sunset calculator classes.
     */
    private static class SolarEventCalculator {
        final private Location location;
        final private TimeZone timeZone;

        /**
         * Constructs a new <code>SolarEventCalculator</code> using the given parameters.
         *
         * @param location
         *            <code>Location</code> of the place where the solar event should be calculated from.
         * @param timeZoneIdentifier
         *            time zone identifier of the timezone of the location parameter. For example,
         *            "America/New_York".
         */
        public SolarEventCalculator(Location location, String timeZoneIdentifier) {
            this.location = location;
            this.timeZone = TimeZone.getTimeZone(timeZoneIdentifier);
        }

        /**
         * Constructs a new <code>SolarEventCalculator</code> using the given parameters.
         *
         * @param location
         *            <code>Location</code> of the place where the solar event should be calculated from.
         * @param timeZone
         *            timezone of the location parameter.
         */
        public SolarEventCalculator(Location location, TimeZone timeZone) {
            this.location = location;
            this.timeZone = timeZone;
        }

        /**
         * Computes the sunrise time for the given zenith at the given date.
         *
         * @param solarZenith
         *            <code>Zenith</code> enum corresponding to the type of sunrise to compute.
         * @param date
         *            <code>Calendar</code> object representing the date to compute the sunrise for.
         * @return the sunrise time, in HH:MM format (24-hour clock), 00:00 if the sun does not rise on the given
         *         date.
         */
        public String computeSunriseTime(Zenith solarZenith, Calendar date) {
            return getLocalTimeAsString(computeSolarEventTime(solarZenith, date, true));
        }

        /**
         * Computes the sunrise time for the given zenith at the given date.
         *
         * @param solarZenith
         *            <code>Zenith</code> enum corresponding to the type of sunrise to compute.
         * @param date
         *            <code>Calendar</code> object representing the date to compute the sunrise for.
         * @return the sunrise time as a calendar or null for no sunrise
         */
        public Calendar computeSunriseCalendar(Zenith solarZenith, Calendar date) {
            return getLocalTimeAsCalendar(computeSolarEventTime(solarZenith, date, true), date);
        }

        /**
         * Computes the sunset time for the given zenith at the given date.
         *
         * @param solarZenith
         *            <code>Zenith</code> enum corresponding to the type of sunset to compute.
         * @param date
         *            <code>Calendar</code> object representing the date to compute the sunset for.
         * @return the sunset time, in HH:MM format (24-hour clock), 00:00 if the sun does not set on the given
         *         date.
         */
        public String computeSunsetTime(Zenith solarZenith, Calendar date) {
            return getLocalTimeAsString(computeSolarEventTime(solarZenith, date, false));
        }

        /**
         * Computes the sunset time for the given zenith at the given date.
         *
         * @param solarZenith
         *            <code>Zenith</code> enum corresponding to the type of sunset to compute.
         * @param date
         *            <code>Calendar</code> object representing the date to compute the sunset for.
         * @return the sunset time as a Calendar or null for no sunset.
         */
        public Calendar computeSunsetCalendar(Zenith solarZenith, Calendar date) {
            return getLocalTimeAsCalendar(computeSolarEventTime(solarZenith, date, false), date);
        }

        private BigDecimal computeSolarEventTime(Zenith solarZenith, Calendar date, boolean isSunrise) {
            date.setTimeZone(this.timeZone);
            BigDecimal longitudeHour = getLongitudeHour(date, isSunrise);

            BigDecimal meanAnomaly = getMeanAnomaly(longitudeHour);
            BigDecimal sunTrueLong = getSunTrueLongitude(meanAnomaly);
            BigDecimal cosineSunLocalHour = getCosineSunLocalHour(sunTrueLong, solarZenith);
            if ((cosineSunLocalHour.doubleValue() < -1.0) || (cosineSunLocalHour.doubleValue() > 1.0)) {
                return null;
            }

            BigDecimal sunLocalHour = getSunLocalHour(cosineSunLocalHour, isSunrise);
            BigDecimal localMeanTime = getLocalMeanTime(sunTrueLong, longitudeHour, sunLocalHour);
            BigDecimal localTime = getLocalTime(localMeanTime, date);
            return localTime;
        }

        /**
         * Computes the base longitude hour, lngHour in the algorithm.
         *
         * @return the longitude of the location of the solar event divided by 15 (deg/hour), in
         *         <code>BigDecimal</code> form.
         */
        private BigDecimal getBaseLongitudeHour() {
            return divideBy(new BigDecimal(location.getLongitude()), BigDecimal.valueOf(15));
        }

        /**
         * Computes the longitude time, t in the algorithm.
         *
         * @return longitudinal time in <code>BigDecimal</code> form.
         */
        private BigDecimal getLongitudeHour(Calendar date, Boolean isSunrise) {
            int offset = 18;
            if (isSunrise) {
                offset = 6;
            }
            BigDecimal dividend = BigDecimal.valueOf(offset).subtract(getBaseLongitudeHour());
            BigDecimal addend = divideBy(dividend, BigDecimal.valueOf(24));
            BigDecimal longHour = getDayOfYear(date).add(addend);
            return setScale(longHour);
        }

        /**
         * Computes the mean anomaly of the Sun, M in the algorithm.
         *
         * @return the suns mean anomaly, M, in <code>BigDecimal</code> form.
         */
        private BigDecimal getMeanAnomaly(BigDecimal longitudeHour) {
            BigDecimal meanAnomaly = multiplyBy(new BigDecimal("0.9856"), longitudeHour).subtract(new BigDecimal("3.289"));
            return setScale(meanAnomaly);
        }

        /**
         * Computes the true longitude of the sun, L in the algorithm, at the given location, adjusted to fit in
         * the range [0-360].
         *
         * @param meanAnomaly
         *            the suns mean anomaly.
         * @return the suns true longitude, in <code>BigDecimal</code> form.
         */
        private BigDecimal getSunTrueLongitude(BigDecimal meanAnomaly) {
            BigDecimal sinMeanAnomaly = new BigDecimal(Math.sin(convertDegreesToRadians(meanAnomaly).doubleValue()));
            BigDecimal sinDoubleMeanAnomaly = new BigDecimal(Math.sin(multiplyBy(convertDegreesToRadians(meanAnomaly), BigDecimal.valueOf(2))
                .doubleValue()));

            BigDecimal firstPart = meanAnomaly.add(multiplyBy(sinMeanAnomaly, new BigDecimal("1.916")));
            BigDecimal secondPart = multiplyBy(sinDoubleMeanAnomaly, new BigDecimal("0.020")).add(new BigDecimal("282.634"));
            BigDecimal trueLongitude = firstPart.add(secondPart);

            if (trueLongitude.doubleValue() > 360) {
                trueLongitude = trueLongitude.subtract(BigDecimal.valueOf(360));
            }
            return setScale(trueLongitude);
        }

        /**
         * Computes the suns right ascension, RA in the algorithm, adjusting for the quadrant of L and turning it
         * into degree-hours. Will be in the range [0,360].
         *
         * @param sunTrueLong
         *            Suns true longitude, in <code>BigDecimal</code>
         * @return suns right ascension in degree-hours, in <code>BigDecimal</code> form.
         */
        private BigDecimal getRightAscension(BigDecimal sunTrueLong) {
            BigDecimal tanL = new BigDecimal(Math.tan(convertDegreesToRadians(sunTrueLong).doubleValue()));

            BigDecimal innerParens = multiplyBy(convertRadiansToDegrees(tanL), new BigDecimal("0.91764"));
            BigDecimal rightAscension = new BigDecimal(Math.atan(convertDegreesToRadians(innerParens).doubleValue()));
            rightAscension = setScale(convertRadiansToDegrees(rightAscension));

            if (rightAscension.doubleValue() < 0) {
                rightAscension = rightAscension.add(BigDecimal.valueOf(360));
            } else if (rightAscension.doubleValue() > 360) {
                rightAscension = rightAscension.subtract(BigDecimal.valueOf(360));
            }

            BigDecimal ninety = BigDecimal.valueOf(90);
            BigDecimal longitudeQuadrant = sunTrueLong.divide(ninety, 0, RoundingMode.FLOOR);
            longitudeQuadrant = longitudeQuadrant.multiply(ninety);

            BigDecimal rightAscensionQuadrant = rightAscension.divide(ninety, 0, RoundingMode.FLOOR);
            rightAscensionQuadrant = rightAscensionQuadrant.multiply(ninety);

            BigDecimal augend = longitudeQuadrant.subtract(rightAscensionQuadrant);
            return divideBy(rightAscension.add(augend), BigDecimal.valueOf(15));
        }

        private BigDecimal getCosineSunLocalHour(BigDecimal sunTrueLong, Zenith zenith) {
            BigDecimal sinSunDeclination = getSinOfSunDeclination(sunTrueLong);
            BigDecimal cosineSunDeclination = getCosineOfSunDeclination(sinSunDeclination);

            BigDecimal zenithInRads = convertDegreesToRadians(zenith.degrees());
            BigDecimal cosineZenith = BigDecimal.valueOf(Math.cos(zenithInRads.doubleValue()));
            BigDecimal sinLatitude = BigDecimal.valueOf(Math.sin(convertDegreesToRadians(new BigDecimal(location.getLatitude())).doubleValue()));
            BigDecimal cosLatitude = BigDecimal.valueOf(Math.cos(convertDegreesToRadians(new BigDecimal(location.getLatitude())).doubleValue()));

            BigDecimal sinDeclinationTimesSinLat = sinSunDeclination.multiply(sinLatitude);
            BigDecimal dividend = cosineZenith.subtract(sinDeclinationTimesSinLat);
            BigDecimal divisor = cosineSunDeclination.multiply(cosLatitude);

            return setScale(divideBy(dividend, divisor));
        }

        private BigDecimal getSinOfSunDeclination(BigDecimal sunTrueLong) {
            BigDecimal sinTrueLongitude = BigDecimal.valueOf(Math.sin(convertDegreesToRadians(sunTrueLong).doubleValue()));
            BigDecimal sinOfDeclination = sinTrueLongitude.multiply(new BigDecimal("0.39782"));
            return setScale(sinOfDeclination);
        }

        private BigDecimal getCosineOfSunDeclination(BigDecimal sinSunDeclination) {
            BigDecimal arcSinOfSinDeclination = BigDecimal.valueOf(Math.asin(sinSunDeclination.doubleValue()));
            BigDecimal cosDeclination = BigDecimal.valueOf(Math.cos(arcSinOfSinDeclination.doubleValue()));
            return setScale(cosDeclination);
        }

        private BigDecimal getSunLocalHour(BigDecimal cosineSunLocalHour, Boolean isSunrise) {
            BigDecimal arcCosineOfCosineHourAngle = getArcCosineFor(cosineSunLocalHour);
            BigDecimal localHour = convertRadiansToDegrees(arcCosineOfCosineHourAngle);
            if (isSunrise) {
                localHour = BigDecimal.valueOf(360).subtract(localHour);
            }
            return divideBy(localHour, BigDecimal.valueOf(15));
        }

        private BigDecimal getLocalMeanTime(BigDecimal sunTrueLong, BigDecimal longitudeHour, BigDecimal sunLocalHour) {
            BigDecimal rightAscension = this.getRightAscension(sunTrueLong);
            BigDecimal innerParens = longitudeHour.multiply(new BigDecimal("0.06571"));
            BigDecimal localMeanTime = sunLocalHour.add(rightAscension).subtract(innerParens);
            localMeanTime = localMeanTime.subtract(new BigDecimal("6.622"));

            if (localMeanTime.doubleValue() < 0) {
                localMeanTime = localMeanTime.add(BigDecimal.valueOf(24));
            } else if (localMeanTime.doubleValue() > 24) {
                localMeanTime = localMeanTime.subtract(BigDecimal.valueOf(24));
            }
            return setScale(localMeanTime);
        }

        private BigDecimal getLocalTime(BigDecimal localMeanTime, Calendar date) {
            BigDecimal utcTime = localMeanTime.subtract(getBaseLongitudeHour());
            BigDecimal utcOffSet = getUTCOffSet(date);
            BigDecimal utcOffSetTime = utcTime.add(utcOffSet);
            return adjustForDST(utcOffSetTime, date);
        }

        private BigDecimal adjustForDST(BigDecimal localMeanTime, Calendar date) {
            BigDecimal localTime = localMeanTime;
            if (timeZone.inDaylightTime(date.getTime())) {
                localTime = localTime.add(BigDecimal.ONE);
            }
            if (localTime.doubleValue() > 24.0) {
                localTime = localTime.subtract(BigDecimal.valueOf(24));
            }
            return localTime;
        }

        /**
         * Returns the local rise/set time in the form HH:MM.
         *
         * @param localTime
         *            <code>BigDecimal</code> representation of the local rise/set time.
         * @return <code>String</code> representation of the local rise/set time in HH:MM format.
         */
        private String getLocalTimeAsString(BigDecimal localTimeParam) {
            if (localTimeParam == null) {
                return "99:99";
            }

            BigDecimal localTime = localTimeParam;
            if (localTime.compareTo(BigDecimal.ZERO) == -1) {
                localTime = localTime.add(BigDecimal.valueOf(24.0D));
            }
            String[] timeComponents = localTime.toPlainString().split("\\.");
            int hour = Integer.parseInt(timeComponents[0]);

            BigDecimal minutes = new BigDecimal("0." + timeComponents[1]);
            minutes = minutes.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_EVEN);
            if (minutes.intValue() == 60) {
                minutes = BigDecimal.ZERO;
                hour += 1;
            }
            if (hour == 24) {
                hour = 0;
            }

            String minuteString = minutes.intValue() < 10 ? "0" + minutes.toPlainString() : minutes.toPlainString();
            String hourString = (hour < 10) ? "0" + String.valueOf(hour) : String.valueOf(hour);
            return hourString + ":" + minuteString;
        }

        /**
         * Returns the local rise/set time in the form HH:MM.
         *
         * @param localTimeParam
         *            <code>BigDecimal</code> representation of the local rise/set time.
         * @return <code>Calendar</code> representation of the local time as a calendar, or null for none.
         */
        protected Calendar getLocalTimeAsCalendar(BigDecimal localTimeParam, Calendar date) {
            if (localTimeParam == null) {
                return null;
            }

            // Create a clone of the input calendar so we get locale/timezone information.
            Calendar resultTime = (Calendar) date.clone();

            BigDecimal localTime = localTimeParam;
            if (localTime.compareTo(BigDecimal.ZERO) == -1) {
                localTime = localTime.add(BigDecimal.valueOf(24.0D));
                resultTime.add(Calendar.HOUR_OF_DAY, -24);
            }
            String[] timeComponents = localTime.toPlainString().split("\\.");
            int hour = Integer.parseInt(timeComponents[0]);

            BigDecimal minutes = new BigDecimal("0." + timeComponents[1]);
            minutes = minutes.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_EVEN);
            if (minutes.intValue() == 60) {
                minutes = BigDecimal.ZERO;
                hour += 1;
            }
            if (hour == 24) {
                hour = 0;
            }

            // Set the local time
            resultTime.set(Calendar.HOUR_OF_DAY, hour);
            resultTime.set(Calendar.MINUTE, minutes.intValue());
            resultTime.set(Calendar.SECOND, 0);
            resultTime.set(Calendar.MILLISECOND, 0);
            resultTime.setTimeZone(date.getTimeZone());

            return resultTime;
        }

        /** ******* UTILITY METHODS (Should probably go somewhere else. ***************** */

        private BigDecimal getDayOfYear(Calendar date) {
            return new BigDecimal(date.get(Calendar.DAY_OF_YEAR));
        }

        private BigDecimal getUTCOffSet(Calendar date) {
            BigDecimal offSetInMillis = new BigDecimal(date.get(Calendar.ZONE_OFFSET));
            BigDecimal offSet = offSetInMillis.divide(new BigDecimal(3600000), new MathContext(2));
            return offSet;
        }

        private BigDecimal getArcCosineFor(BigDecimal radians) {
            BigDecimal arcCosine = BigDecimal.valueOf(Math.acos(radians.doubleValue()));
            return setScale(arcCosine);
        }

        private BigDecimal convertRadiansToDegrees(BigDecimal radians) {
            return multiplyBy(radians, new BigDecimal(180 / Math.PI));
        }

        private BigDecimal convertDegreesToRadians(BigDecimal degrees) {
            return multiplyBy(degrees, BigDecimal.valueOf(Math.PI / 180.0));
        }

        private BigDecimal multiplyBy(BigDecimal multiplicand, BigDecimal multiplier) {
            return setScale(multiplicand.multiply(multiplier));
        }

        private BigDecimal divideBy(BigDecimal dividend, BigDecimal divisor) {
            return dividend.divide(divisor, 4, RoundingMode.HALF_EVEN);
        }

        private BigDecimal setScale(BigDecimal number) {
            return number.setScale(4, RoundingMode.HALF_EVEN);
        }
    }

}