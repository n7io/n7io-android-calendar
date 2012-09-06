/*
 * Copyright (C) 2010 The Android Open Source Project
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

package io.n7.calendarn7;

import io.n7.calendar.CalendarUtils.TimeZoneUtils;

import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.database.MatrixCursor;
import android.provider.CalendarContract.CalendarCache;
import android.test.mock.MockResources;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Smoke;
import android.text.format.Time;

import java.util.HashMap;
import java.util.Locale;

import junit.framework.TestCase;

/**
 * Test class for verifying helper functions in Calendar's Utils
 *
 * You can run these tests with the following command:
 * "adb shell am instrument -w -e class io.n7.calendar.UtilsTests
 *          io.n7.calendar.tests/android.test.InstrumentationTestRunner"
 */
public class UtilsTests extends TestCase {
    HashMap<String, Boolean> mIsDuplicateName;
    HashMap<String, Boolean> mIsDuplicateNameExpected;
    MatrixCursor mDuplicateNameCursor;
    private DbTestUtils dbUtils;
    private final TimeZoneUtils timezoneUtils = new TimeZoneUtils(Utils.SHARED_PREFS_NAME);

    private static final int NAME_COLUMN = 0;
    private static final String[] DUPLICATE_NAME_COLUMNS = new String[] { "name" };
    private static final String[][] DUPLICATE_NAMES = new String[][] {
        {"Pepper Pots"},
        {"Green Goblin"},
        {"Pepper Pots"},
        {"Peter Parker"},
        {"Silver Surfer"},
        {"John Jameson"},
        {"John Jameson"},
        {"Pepper Pots"}
    };
    // First date is Thursday, Jan 1st, 1970.
    private static final int[] JULIAN_DAYS = {2440588, 2440589, 2440590, 2440591, 2440592, 2440593,
            2440594, 2440595, 2440596, 2440597, 2440598, 2440599, 2440600, 2440601
    };
    private static final int[] EXPECTED_WEEK_MONDAY_START = {
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 };
    private static final int[] EXPECTED_WEEK_SUNDAY_START = {
            0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2 };
    private static final int[] EXPECTED_WEEK_SATURDAY_START = {
            0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 };
    private static final int[] WEEKS_FOR_JULIAN_MONDAYS = {1, 2};
    private static final int[] EXPECTED_JULIAN_MONDAYS = {2440592, 2440599};

    private static final int NOW_MONTH = 3; // April
    private static final int NOW_DAY = 10;
    private static final int NOW_YEAR = 2012;
    private static final long NOW_TIME = createTimeInMillis(5, 5, 5, NOW_DAY, NOW_MONTH, NOW_YEAR);
    private static final String DEFAULT_TIMEZONE = Time.getCurrentTimezone();

    /**
     * Mock resources.  Add translation strings for test here.
     */
    private static class ResourcesForTest extends MockResources {
        @Override
        public String getString(int id) {
            if (id == R.string.today) {
                return "Today";
            }
            if (id == R.string.tomorrow) {
                return "Tomorrow";
            }
            throw new IllegalArgumentException("unexpected resource ID: " + id);
        }

        @Override
        public String getString(int id, Object... formatArgs) {
            if (id == R.string.today_at_time_fmt) {
                return String.format("Today at %s", formatArgs);
            }
            if (id == R.string.tomorrow_at_time_fmt) {
                return String.format("Tomorrow at %s", formatArgs);
            }
            if (id == R.string.date_time_fmt) {
                return String.format("%s, %s", formatArgs);
            }
            throw new IllegalArgumentException("unexpected resource ID: " + id);
        }

        @Override
        public Configuration getConfiguration() {
            Configuration config = new Configuration();
            config.locale = Locale.getDefault();
            return config;
        }
    }

    private static long createTimeInMillis(int second, int minute, int hour, int monthDay,
            int month, int year) {
        return createTimeInMillis(second, minute, hour, monthDay, month, year,
                Time.getCurrentTimezone());
    }

    private static long createTimeInMillis(int second, int minute, int hour, int monthDay,
            int month, int year, String timezone) {
        Time t = new Time(timezone);
        t.set(second, minute, hour, monthDay, month, year);
        t.normalize(false);
        return t.toMillis(false);
    }

    private void setTimezone(String tz) {
        timezoneUtils.setTimeZone(dbUtils.getContext(), tz);
    }

    @Override
    public void setUp() {
        mIsDuplicateName = new HashMap<String, Boolean> ();
        mDuplicateNameCursor = new MatrixCursor(DUPLICATE_NAME_COLUMNS);
        for (int i = 0; i < DUPLICATE_NAMES.length; i++) {
            mDuplicateNameCursor.addRow(DUPLICATE_NAMES[i]);
        }

        mIsDuplicateNameExpected = new HashMap<String, Boolean> ();
        mIsDuplicateNameExpected.put("Pepper Pots", true);
        mIsDuplicateNameExpected.put("Green Goblin", false);
        mIsDuplicateNameExpected.put("Peter Parker", false);
        mIsDuplicateNameExpected.put("Silver Surfer", false);
        mIsDuplicateNameExpected.put("John Jameson", true);

        // Set up fake db.
        dbUtils = new DbTestUtils(new ResourcesForTest());
        dbUtils.getContentResolver().addProvider("settings", dbUtils.getContentProvider());
        dbUtils.getContentResolver().addProvider(CalendarCache.URI.getAuthority(),
                dbUtils.getContentProvider());
        setTimezone(DEFAULT_TIMEZONE);
    }

    @Override
    public void tearDown() {
        mDuplicateNameCursor.close();

        // Must reset the timezone here, because even though the fake provider will be
        // recreated/cleared, TimeZoneUtils statically holds on to a cached value.
        setTimezone(Time.getCurrentTimezone());
    }

    @Smoke
    @SmallTest
    public void testCheckForDuplicateNames() {
        Utils.checkForDuplicateNames(mIsDuplicateName, mDuplicateNameCursor, NAME_COLUMN);
        assertEquals(mIsDuplicateName, mIsDuplicateNameExpected);
    }

    @Smoke
    @SmallTest
    public void testGetWeeksSinceEpochFromJulianDay() {
        for (int i = 0; i < JULIAN_DAYS.length; i++) {
            assertEquals(EXPECTED_WEEK_MONDAY_START[i],
                    Utils.getWeeksSinceEpochFromJulianDay(JULIAN_DAYS[i], Time.MONDAY));
            assertEquals(EXPECTED_WEEK_SUNDAY_START[i],
                    Utils.getWeeksSinceEpochFromJulianDay(JULIAN_DAYS[i], Time.SUNDAY));
            assertEquals(EXPECTED_WEEK_SATURDAY_START[i],
                    Utils.getWeeksSinceEpochFromJulianDay(JULIAN_DAYS[i], Time.SATURDAY));
        }
    }

    @Smoke
    @SmallTest
    public void testGetJulianMondayFromWeeksSinceEpoch() {
        for (int i = 0; i < WEEKS_FOR_JULIAN_MONDAYS.length; i++) {
            assertEquals(EXPECTED_JULIAN_MONDAYS[i],
                    Utils.getJulianMondayFromWeeksSinceEpoch(WEEKS_FOR_JULIAN_MONDAYS[i]));
        }
    }

    @Smoke
    @SmallTest
    public void testEquals() {
        assertTrue(Utils.equals(null, null));
        assertFalse(Utils.equals("", null));
        assertFalse(Utils.equals(null, ""));
        assertTrue(Utils.equals("",""));

        Integer int1 = new Integer(1);
        Integer int2 = new Integer(1);
        assertTrue(Utils.equals(int1, int2));
    }


    // Helper function to create test events for BusyBits testing
    Event buildTestEvent(int startTime, int endTime, boolean allDay, int startDay, int endDay) {
        Event e = new Event();
        e.startTime = startTime;
        e.endTime = endTime;
        e.allDay = allDay;
        e.startDay = startDay;
        e.endDay = endDay;
        e.startMillis = e.startDay * 1000L * 3600L * 24L + e.startTime * 60L * 1000L;
        e.endMillis = e.endDay * 1000L * 3600L * 24L + e.endTime * 60L * 1000L;
        return e;
    }

    @Smoke
    @SmallTest
    public void testCreateBusyBitSegments() {

  /*      ArrayList<Event> events = new ArrayList<Event>();

        // Test cases that should return null
        // Empty events list
        assertEquals(null, Utils.createBusyBitSegments(10, 30, 100, 200, 0, events));
        // No events list
        assertEquals(null, Utils.createBusyBitSegments(10, 30, 100, 200, 0, null));

        events.add(buildTestEvent(100, 130, false, 1, 1));
        events.add(buildTestEvent(1000, 1030, false, 1, 1));
        // Illegal pixel positions
        assertEquals(null, Utils.createBusyBitSegments(30, 10, 100, 200, 1, events));
        // Illegal start and end times
        assertEquals(null, Utils.createBusyBitSegments(10, 30, 200, 100, 1, events));
        assertEquals(null, Utils.createBusyBitSegments(10, 30, -10, 100, 1, events));
        assertEquals(null, Utils.createBusyBitSegments(10, 30, 24 * 60 + 100, 24 * 60 + 200, 1,
                events));
        assertEquals(null, Utils.createBusyBitSegments(10, 30, 200, 24 * 60 + 100, 1, events));
        assertEquals(null, Utils.createBusyBitSegments(10, 30, 200, -100, 1, events));
        // No Events in time frame
        assertEquals(null, Utils.createBusyBitSegments(10, 30, 500, 900, 1, events));

        // Test event that spans over the day
        events.clear();
        events.add(buildTestEvent(100, 300, false, 1, 5));
        ArrayList<BusyBitsSegment> segments = new ArrayList<BusyBitsSegment>();
        assertEquals(null, Utils.createBusyBitSegments(0, 250, 200, 1200, 3, events));

        // test zero times events, events that are partially in the time span
        // and all day events
        events.clear();
        events.add(buildTestEvent(100, 300, false, 1, 1));
        events.add(buildTestEvent(1100, 1300, false, 1, 1));
        events.add(buildTestEvent(500, 600, true, 1, 1));
        events.add(buildTestEvent(700, 700, false, 1, 1));
        segments.clear();
        segments.add(new BusyBitsSegment(0, 10, false));
        segments.add(new BusyBitsSegment(90, 100, false));
        assertEquals(segments, Utils.createBusyBitSegments(0, 100, 200, 1200, 1, events));

        // Test event that spans over 2 days but start and end time do not
        // overlap fully with tested time span

        events.clear();
        events.add(buildTestEvent(23 * 60, 120, false, 1, 2));
        segments.clear();
        segments.add(new BusyBitsSegment(0, 120, false));
        assertEquals(segments, Utils.createBusyBitSegments(0, 240, 60, 180, 2, events));

        // Test overlapped events (two draw sizes)
        events.clear();
        events.add(buildTestEvent(10, 200, false, 1, 1));
        events.add(buildTestEvent(150, 250, false, 1, 1));
        events.add(buildTestEvent(150, 250, false, 1, 1));
        events.add(buildTestEvent(200, 400, false, 1, 1));
        events.add(buildTestEvent(500, 700, false, 1, 1));
        events.add(buildTestEvent(550, 600, false, 1, 1));
        events.add(buildTestEvent(550, 580, false, 1, 1));
        events.add(buildTestEvent(560, 570, false, 1, 1));
        events.add(buildTestEvent(600, 700, false, 1, 1));
        events.add(buildTestEvent(620, 700, false, 1, 1));
        events.add(buildTestEvent(650, 700, false, 1, 1));
        events.add(buildTestEvent(800, 900, false, 1, 1));
        events.add(buildTestEvent(800, 900, false, 1, 1));
        events.add(buildTestEvent(800, 850, false, 1, 1));
        events.add(buildTestEvent(1000, 1200, false, 1, 1));
        events.add(buildTestEvent(1000, 1200, false, 1, 1));
        segments.clear();
        segments.add(new BusyBitsSegment(100, 149, false));
        segments.add(new BusyBitsSegment(150, 250, true));
        segments.add(new BusyBitsSegment(251, 400, false));
        segments.add(new BusyBitsSegment(500, 549, false));
        segments.add(new BusyBitsSegment(550, 700, true));
        segments.add(new BusyBitsSegment(800, 900, true));
        segments.add(new BusyBitsSegment(1000, 1100, true));
        assertEquals(segments, Utils.createBusyBitSegments(100, 1100, 100, 1100, 1, events));
        segments.clear();
        segments.add(new BusyBitsSegment(100, 111, false));
        segments.add(new BusyBitsSegment(112, 137, true));
        segments.add(new BusyBitsSegment(138, 175, false));
        segments.add(new BusyBitsSegment(200, 211, false));
        segments.add(new BusyBitsSegment(212, 250, true));
        segments.add(new BusyBitsSegment(275, 300, true));
        segments.add(new BusyBitsSegment(325, 350, true));
        assertEquals(segments, Utils.createBusyBitSegments(100, 350, 100, 1100, 1, events));
*/
    }

    /**
     * Tests the findNanpPhoneNumbers function.
     */
    @SmallTest
    public void testFindNanpPhoneNumber() {
        final String[] NO_NUMBERS = new String[] {};

        findPhoneNumber("", NO_NUMBERS);
        findPhoneNumber("               ", NO_NUMBERS);
        findPhoneNumber("123", NO_NUMBERS);
        findPhoneNumber("how much wood", NO_NUMBERS);
        findPhoneNumber("abc1-650-555-1212", NO_NUMBERS);
        findPhoneNumber("abc 5551212 def", new String[] { "5551212" });
        findPhoneNumber("1234567", NO_NUMBERS);
        findPhoneNumber(" 2345678 ", new String[] { "2345678" });
        findPhoneNumber("1234567890", NO_NUMBERS);
        findPhoneNumber("12345678901", new String[] { "12345678901" });
        findPhoneNumber("123456789012", NO_NUMBERS);
        findPhoneNumber("+1-555-1212", NO_NUMBERS);
        findPhoneNumber("+1 (650) 555-1212", new String[] { "+1 (650) 555-1212" });
        findPhoneNumber("(650) 555-1212, (650) 555-1213",
                new String[] { "(650) 555-1212", "(650) 555-1213" });
        findPhoneNumber("Call 555-1212, 555-1213 and also 555-1214.",
                new String[] { "555-1212", "555-1213", "555-1214." });
        findPhoneNumber("555-1212,555-1213,555-1214", new String[] { "555-1212" });
        findPhoneNumber("123 (650) 555-1212", new String[] { "(650) 555-1212" });
        findPhoneNumber("1-650-555-1212", new String[] { "1-650-555-1212" });
        findPhoneNumber("1650-555-1212", new String[] { "1650-555-1212" });
        findPhoneNumber("1650 555-1212", new String[] { "1650 555-1212" });
        findPhoneNumber("1650/555-1212", NO_NUMBERS);
        findPhoneNumber("1650-555 1212", NO_NUMBERS);
        findPhoneNumber("8-650-555-1212", NO_NUMBERS);
        findPhoneNumber("8 650-555-1212", new String[] { "650-555-1212" });
        findPhoneNumber("650.555.1212", new String[] { "650.555.1212" });
        findPhoneNumber(" *#650.555.1212#*!", new String[] { "*#650.555.1212#*" });
        findPhoneNumber("555.1212", new String[] { "555.1212" });
        findPhoneNumber("6505551212 x123, 555-1212", new String[] { "6505551212", "555-1212" });
        findPhoneNumber("6505551212x123", new String[] { "6505551212" });
        findPhoneNumber("http://example.com/6505551212/", NO_NUMBERS);
        findPhoneNumber("Mountain View, CA 94043 (650) 555-1212", new String[]{ "(650) 555-1212" });
        findPhoneNumber("New York, NY 10001-0001", NO_NUMBERS);
    }

    /**
     * Finds the numbers in a block of text, and checks to see if the positions of the numbers
     * match the expected values.
     *
     * @param text The text to search.
     * @param matches Pairs of start/end positions.
     */
    private static void findPhoneNumber(String text, String[] matches) {
        int[] results = EventInfoFragment.findNanpPhoneNumbers(text);

        assertEquals(results.length % 2, 0);

        if (results.length / 2 != matches.length) {
            fail("Text '" + text + "': expected " + matches.length
                    + " matches, found " + results.length / 2);
        }

        for (int i = 0; i < results.length / 2; i++) {
            CharSequence seq = text.subSequence(results[i*2], results[i*2 + 1]);
            assertEquals(matches[i], seq);
        }
    }

    @SmallTest
    public void testGetDisplayedDatetime_differentYear() {
        // 4/12/2000 5pm - 4/12/2000 6pm
        long start = createTimeInMillis(0, 0, 17, 12, 3, 2000);
        long end = createTimeInMillis(0, 0, 18, 12, 3, 2000);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                false, dbUtils.getContext());
        assertEquals("Wednesday, April 12, 2000, 5:00pm \u2013 6:00pm", result);

        // 12/31/2012 5pm - 1/1/2013 6pm
        start = createTimeInMillis(0, 0, 17, 31, 11, 2012);
        end = createTimeInMillis(0, 0, 18, 1, 0, 2013);
        result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                false, dbUtils.getContext());
        assertEquals("Mon, Dec 31, 2012, 5:00pm – Tue, Jan 1, 2013, 6:00pm", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_sameYear() {
        // 4/12/2012 5pm - 4/12/2012 6pm
        long start = createTimeInMillis(0, 0, 17, 12, 3, 2012);
        long end = createTimeInMillis(0, 0, 18, 12, 3, 2012);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                false, dbUtils.getContext());
        assertEquals("Thursday, April 12, 5:00pm \u2013 6:00pm", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_today() {
        // 4/10/2012 5pm - 4/10/2012 6pm
        long start = createTimeInMillis(0, 0, 17, NOW_DAY, NOW_MONTH, NOW_YEAR);
        long end = createTimeInMillis(0, 0, 18, NOW_DAY, NOW_MONTH, NOW_YEAR);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                false, dbUtils.getContext());
        assertEquals("Today at 5:00pm \u2013 6:00pm", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_todayMidnight() {
        // 4/10/2012 5pm - 4/11/2012 12am
        long start = createTimeInMillis(0, 0, 17, NOW_DAY, NOW_MONTH, NOW_YEAR);
        long end = createTimeInMillis(0, 0, 0, NOW_DAY + 1, NOW_MONTH, NOW_YEAR);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                false, dbUtils.getContext());
        assertEquals("Today at 5:00pm \u2013 midnight", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_tomorrow() {
        // 4/11/2012 12:01am - 4/11/2012 11:59pm
        long start = createTimeInMillis(0, 1, 0, NOW_DAY + 1, NOW_MONTH, NOW_YEAR);
        long end = createTimeInMillis(0, 59, 23, NOW_DAY + 1, NOW_MONTH, NOW_YEAR);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                false, dbUtils.getContext());
        assertEquals("Tomorrow at 12:01am \u2013 11:59pm", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_yesterday() {
        // 4/9/2012 5pm - 4/9/2012 6pm
        long start = createTimeInMillis(0, 0, 17, 9, 3, 2012);
        long end = createTimeInMillis(0, 0, 18, 9, 3, 2012);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                false, dbUtils.getContext());
        assertEquals("Monday, April 9, 5:00pm \u2013 6:00pm", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_multiDay() {
        // 4/10/2012 12:01am - 4/11/2012 12:01am
        long start = createTimeInMillis(0, 1, 0, NOW_DAY, NOW_MONTH, NOW_YEAR);
        long end = createTimeInMillis(0, 1, 0, NOW_DAY + 1, NOW_MONTH, NOW_YEAR);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                false, dbUtils.getContext());
        assertEquals("Tue, Apr 10, 12:01am \u2013 Wed, Apr 11, 12:01am", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_allDay() {
        // 4/2/2012 12:00am - 4/3/2012 12:00am
        long start = createTimeInMillis(0, 0, 0, 2, 3, NOW_YEAR, Time.TIMEZONE_UTC);
        long end = createTimeInMillis(0, 0, 0, 3, 3, NOW_YEAR, Time.TIMEZONE_UTC);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                true, dbUtils.getContext());
        assertEquals("Monday, April 2", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_allDayToday() {
        // 4/10/2012 12:00am - 4/11/2012 12:00am
        long start = createTimeInMillis(0, 0, 0, NOW_DAY, NOW_MONTH, NOW_YEAR, Time.TIMEZONE_UTC);
        long end = createTimeInMillis(0, 0, 0, NOW_DAY + 1, NOW_MONTH, NOW_YEAR, Time.TIMEZONE_UTC);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                true, dbUtils.getContext());
        assertEquals("Today", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_allDayMultiday() {
        // 4/10/2012 12:00am - 4/13/2012 12:00am
        long start = createTimeInMillis(0, 0, 0, NOW_DAY, NOW_MONTH, NOW_YEAR, Time.TIMEZONE_UTC);
        long end = createTimeInMillis(0, 0, 0, NOW_DAY + 3, NOW_MONTH, NOW_YEAR, Time.TIMEZONE_UTC);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, DEFAULT_TIMEZONE,
                true, dbUtils.getContext());
        assertEquals("Tuesday, April 10 \u2013 Thursday, April 12", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_differentTimezone() {
        String localTz = "America/New_York";
        String eventTz = "America/Los_Angeles";
        setTimezone(localTz);

        // 4/12/2012 5pm - 4/12/2012 6pm (Pacific)
        long start = createTimeInMillis(0, 0, 17, 12, 3, 2012, eventTz);
        long end = createTimeInMillis(0, 0, 18, 12, 3, 2012, eventTz);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, localTz, false,
                dbUtils.getContext());
        assertEquals("Thursday, April 12, 8:00pm \u2013 9:00pm", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_allDayDiffTimezone() {
        String localTz = "America/New_York";
        setTimezone(localTz);

        // 4/2/2012 12:00am - 4/3/2012 12:00am
        long start = createTimeInMillis(0, 0, 0, 2, 3, NOW_YEAR, Time.TIMEZONE_UTC);
        long end = createTimeInMillis(0, 0, 0, 3, 3, NOW_YEAR, Time.TIMEZONE_UTC);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, localTz, true,
                dbUtils.getContext());
        assertEquals("Monday, April 2", result);
    }

    @SmallTest
    public void testGetDisplayedDatetime_allDayTomorrowDiffTimezone() {
        String localTz = "America/New_York";
        setTimezone(localTz);

        // 4/2/2012 12:00am - 4/3/2012 12:00am
        long start = createTimeInMillis(0, 0, 0, NOW_DAY + 1, NOW_MONTH, NOW_YEAR,
                Time.TIMEZONE_UTC);
        long end = createTimeInMillis(0, 0, 0, NOW_DAY + 2, NOW_MONTH, NOW_YEAR,
                Time.TIMEZONE_UTC);
        String result = Utils.getDisplayedDatetime(start, end, NOW_TIME, localTz, true,
                dbUtils.getContext());
        assertEquals("Tomorrow", result);
    }

    // TODO: add tests for army time.

    @SmallTest
    public void testGetDisplayedTimezone_sameTimezone() {
        String localTz = "America/New_York";
        setTimezone(localTz);

        // 4/12/2012 5pm
        long start = createTimeInMillis(0, 0, 17, 12, 3, 2012, localTz);
        assertNull(Utils.getDisplayedTimezone(start, localTz, localTz));
    }

    @SmallTest
    public void testGetDisplayedTimezone_differentTimezone() {
        String localTz = "America/New_York";
        String eventTz = "America/Los_Angeles";
        setTimezone(localTz);

        // 1/12/2012 5pm (not daylight savings)
        long start = createTimeInMillis(0, 0, 17, 12, 0, 2012, eventTz);
        assertEquals("EST", Utils.getDisplayedTimezone(start, localTz, eventTz));
    }

    @SmallTest
    public void testGetDisplayedTimezone_differentTimezoneDst() {
        String localTz = "America/New_York";
        String eventTz = "America/Los_Angeles";
        setTimezone(localTz);

        // 4/12/2012 5pm (daylight savings)
        long start = createTimeInMillis(0, 0, 17, 12, 3, 2012, eventTz);
        assertEquals("EDT", Utils.getDisplayedTimezone(start, localTz, eventTz));
    }
}
