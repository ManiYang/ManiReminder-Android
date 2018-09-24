package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReminderDataBehavior {

    public static final int TYPE_NO_BOARD_CONTROL = 0;
    public static final int TYPE_TODO_AT_INSTANTS = 1;
    public static final int TYPE_REMINDER_IN_PERIOD = 2;
    public static final int TYPE_TODO_REPETITIVE_IN_PERIOD = 3;

    private int remType;
    private Instant[] instants; //for TYPE_TODO_AT_INSTANTS
    private Period[] periods;   //for TYPE_REMINDER_IN_PERIOD & TYPE_TODO_REPETITIVE_IN_PERIOD
                                //(will take the union of the periods)
    private int repeatEveryMinutes;    //for TYPE_TODO_REPETITIVE_IN_PERIOD
    private int repeatOffsetMinutes;   //for TYPE_TODO_REPETITIVE_IN_PERIOD

    public ReminderDataBehavior() {
        remType = TYPE_NO_BOARD_CONTROL;
    }

    public ReminderDataBehavior setAsTodoAtInstants(Instant[] instants) {
        if (instants.length == 0)
            throw new RuntimeException("`instants` is empty");

        this.remType = TYPE_TODO_AT_INSTANTS;
        this.instants = instants;
        this.periods = null;
        this.repeatEveryMinutes = -1;
        this.repeatOffsetMinutes = -1;
        return this;
    }

    public ReminderDataBehavior setAsTodoAtInstants(String[] instantsDisplayStrings,
                                                    SparseArray<String> allSituations,
                                                    SparseArray<String> allEvents) {
        if (instantsDisplayStrings.length == 0)
            throw new RuntimeException("instantsDisplayStrings is empty");

        Instant[] instants = new Instant[instantsDisplayStrings.length];
        for (int i=0; i<instants.length; i++) {
            instants[i] = new Instant().setFromDisplayString(
                    instantsDisplayStrings[i], allSituations, allEvents);
        }
        setAsTodoAtInstants(instants);
        return this;
    }

    public ReminderDataBehavior setAsReminderInPeriod(Period[] periods) {
        if (periods.length == 0)
            throw new RuntimeException("`periods` is empty");

        this.remType = TYPE_REMINDER_IN_PERIOD;
        this.instants = null;
        this.periods = periods; //will take the union
        this.repeatEveryMinutes = -1;
        this.repeatOffsetMinutes = -1;
        return this;
    }

    public ReminderDataBehavior setAsReminderInPeriod(String[] periodsDisplayStrings,
                                                      SparseArray<String> allSituations,
                                                      SparseArray<String> allEvents) {
        if (periodsDisplayStrings.length == 0)
            throw new RuntimeException("periodsDisplayStrings is empty");

        Period[] periods = new Period[periodsDisplayStrings.length];
        for (int i=0; i<periods.length; i++) {
            periods[i] = new Period().setFromDisplayString(
                    periodsDisplayStrings[i], allSituations, allEvents);
        }
        setAsReminderInPeriod(periods);
        return this;
    }

    public ReminderDataBehavior setAsTodoRepeatedlyInPeriod(
            Period[] periods, int repeatEveryMinutes, int repeatOffsetMinutes) {
        if (periods.length == 0)
            throw new RuntimeException("`periods` is empty");
        if (repeatEveryMinutes <= 0) {
            throw new RuntimeException("repeatEveryMinutes should be > 0");
        }
        if (repeatOffsetMinutes < 0) {
            throw new RuntimeException("repeatOffsetMinutes should be >= 0");
        }

        this.remType = TYPE_TODO_REPETITIVE_IN_PERIOD;
        this.instants = null;
        this.periods = periods;
        this.repeatEveryMinutes = repeatEveryMinutes;
        this.repeatOffsetMinutes = repeatOffsetMinutes;
        return this;
    }

    public ReminderDataBehavior setAsTodoRepeatedlyInPeriod(
            String[] periodsDisplayStrings, int repeatEveryMinutes, int repeatOffsetMinutes,
            SparseArray<String> allSituations, SparseArray<String> allEvents) {
        if (periodsDisplayStrings.length == 0)
            throw new RuntimeException("periodsDisplayStrings is empty");
        if (repeatEveryMinutes <= 0) {
            throw new RuntimeException("repeatEveryMinutes should be > 0");
        }
        if (repeatOffsetMinutes < 0) {
            throw new RuntimeException("repeatOffsetMinutes should be >= 0");
        }

        Period[] periods = new Period[periodsDisplayStrings.length];
        for (int i=0; i<periods.length; i++) {
            periods[i] = new Period().setFromDisplayString(
                    periodsDisplayStrings[i], allSituations, allEvents);
        }
        setAsTodoRepeatedlyInPeriod(periods, repeatEveryMinutes, repeatOffsetMinutes);
        return this;
    }

    public ReminderDataBehavior setFromStringRepresentation(String stringRepresentation) {
        setFromString(stringRepresentation, false, null, null);
        return this;
    }

    public ReminderDataBehavior setFromDisplayString(String displayString,
                                                     SparseArray<String> allSituations,
                                                     SparseArray<String> allEvents) {
        setFromString(displayString, true, allSituations, allEvents);
        return this;
    }

    private void setFromString(String string, boolean isDisplayString,
                               @Nullable SparseArray<String> allSituations,
                               @Nullable SparseArray<String> allEvents) {
        if (isDisplayString) {
            if (allSituations == null || allEvents == null) {
                throw new RuntimeException("allSituations & allEvents should not be null "
                        +"as they may be needed");
            }
        }

        this.instants = null;
        this.periods = null;
        try {
            if (string.trim().isEmpty()) {
                remType = TYPE_NO_BOARD_CONTROL;
            } else if (string.contains(" in ")) {
                String[] tokens = string.split(" in ");

                Matcher matcher = Pattern.compile("every(\\d+)m\\.offset(\\d+)m").matcher(tokens[0]);
                if (!matcher.find()) {
                    throw new RuntimeException();
                }
                repeatEveryMinutes = Integer.parseInt(matcher.group(1));
                repeatOffsetMinutes = Integer.parseInt(matcher.group(2));

                Log.v("ReminderDataBehavior",
                        String.format("### repeatEveryMinutes=%d, repeatOffsetMinutes=%d",
                                repeatEveryMinutes, repeatOffsetMinutes));

                if (tokens[1].trim().isEmpty()) {
                    throw new RuntimeException();
                }
                String[] periodStrings = tokens[1].split(", ");
                periods = new Period[periodStrings.length];
                if (isDisplayString) {
                    for (int i = 0; i < periods.length; i++) {
                        Log.v("ReminderDataBehavior", "### periodString: "+periodStrings[i]);
                        periods[i] = new Period().setFromDisplayString(
                                periodStrings[i].trim(), allSituations, allEvents);
                    }
                } else {
                    for (int i = 0; i < periods.length; i++) {
                        periods[i] = new Period().setFromStringRepresentation(periodStrings[i].trim());
                    }
                }
                remType = TYPE_TODO_REPETITIVE_IN_PERIOD;
                Log.v("ReminderDataBehavior",
                        "### successfully set as todo-repetitively-during-periods");
            } else {
                String[] tokens = string.split(", ");
                if (tokens[0].contains("-")) {
                    periods = new Period[tokens.length];
                    if (isDisplayString) {
                        for (int i = 0; i < periods.length; i++) {
                            periods[i] = new Period().setFromDisplayString(
                                    tokens[i].trim(), allSituations, allEvents);
                        }
                    } else {
                        for (int i = 0; i < periods.length; i++) {
                            periods[i] = new Period().setFromStringRepresentation(tokens[i].trim());
                        }
                    }
                    remType = TYPE_REMINDER_IN_PERIOD;
                } else {
                    instants = new Instant[tokens.length];
                    if (isDisplayString) {
                        for (int i = 0; i < instants.length; i++) {
                            instants[i] = new Instant().setFromDisplayString(
                                    tokens[i].trim(), allSituations, allEvents);
                        }
                    } else {
                        for (int i = 0; i < instants.length; i++) {
                            instants[i] = new Instant().setFromStringRepresentation(tokens[i].trim());
                        }
                    }
                    remType = TYPE_TODO_AT_INSTANTS;
                }
            }
        } catch (RuntimeException e) {
            Log.v("ReminderDataBehavior", "### bad string: \""+string+"\"");
            throw new RuntimeException("Bad format of string");
        }
    }

    //
    public int getRemType() {
        return remType;
    }

    public boolean hasNoBoardControl() {
        return remType == TYPE_NO_BOARD_CONTROL;
    }

    public boolean isTodoAtInstants() {
        return remType == TYPE_TODO_AT_INSTANTS;
    }

    public boolean isReminderInPeriod() {
        return remType == TYPE_REMINDER_IN_PERIOD;
    }

    public boolean isTodoRepeatedlyInPeriod() {
        return remType == TYPE_TODO_REPETITIVE_IN_PERIOD;
    }

    /** Don't modify the returned object */
    public Instant[] getInstants() {
        if (remType != TYPE_TODO_AT_INSTANTS) {
            throw new RuntimeException("this is not todo-at-instants");
        }
        return instants;
    }

    /** Don't modify the returned object */
    public Period[] getPeriods() {
        if (remType != TYPE_REMINDER_IN_PERIOD && remType != TYPE_TODO_REPETITIVE_IN_PERIOD) {
            throw new RuntimeException(
                    "this is neither reminder-in-period nor todo-repetitive-in-period");
        }
        return periods;
    }

    public int getRepeatEveryMinutes() {
        if (remType != TYPE_TODO_REPETITIVE_IN_PERIOD) {
            throw new RuntimeException("this is not todo-repetitive-in-period");
        }
        return repeatEveryMinutes;
    }

    public int getRepeatOffsetMinutes() {
        if (remType != TYPE_TODO_REPETITIVE_IN_PERIOD) {
            throw new RuntimeException("this is not todo-repetitive-in-period");
        }
        return repeatOffsetMinutes;
    }

    public String getStringRepresentation() {
        return getString(false, null, null);
    }

    public String getDisplayString(SparseArray<String> allSituations, SparseArray<String> allEvents) {
        return getString(true, allSituations, allEvents);
    }

    /**
     * @param forDisplay -- true: get display string,  false: get string representation
     * allSituations and allEvents can be null if `forDisplay` = false (for getting string
     * representation) */
    private String getString(boolean forDisplay,
                             SparseArray<String> allSituations, SparseArray<String> allEvents) {
        if (forDisplay) {
            if (allSituations == null || allEvents == null) {
                throw new RuntimeException("allSituations and allEvents should not be null "
                        +"as they may be needed");
            }
        }

        StringBuilder builder = new StringBuilder();
        switch (remType) {
            case TYPE_NO_BOARD_CONTROL:
                return "";

            case TYPE_TODO_AT_INSTANTS:
                if (instants != null) {
                    for (int i=0; i<instants.length; i++) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        if (forDisplay) {
                            builder.append(instants[i].getDisplayString(allSituations, allEvents));
                        } else {
                            builder.append(instants[i].getStringRepresentation());
                        }
                    }
                }
                return builder.toString();

            case TYPE_REMINDER_IN_PERIOD:
                if (periods != null) {
                    for (int i=0; i<periods.length; i++) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        if (forDisplay) {
                            builder.append(periods[i].getDisplayString(allSituations, allEvents));
                        } else {
                            builder.append(periods[i].getStringRepresentation());
                        }
                    }
                }
                return builder.toString();

            case TYPE_TODO_REPETITIVE_IN_PERIOD:
                builder.append("every").append(repeatEveryMinutes)
                        .append("m.offset").append(repeatOffsetMinutes).append("m in ");
                if (periods != null) {
                    for (int i=0; i<periods.length; i++) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        if (forDisplay) {
                            builder.append(periods[i].getDisplayString(allSituations, allEvents));
                        } else {
                            builder.append(periods[i].getStringRepresentation());
                        }
                    }
                }
                return builder.toString();

            default:
                return "unknown";
        }
    }

    public List<Integer> getInvolvedSituationIds() {
        List<Integer> ids = new ArrayList<>();
        if (remType == 1) {
            for (Instant instant: instants) {
                if (instant.isSituationStart() || instant.isSituationEnd())
                    ids.add(instant.getSituationId());
            }
        } else if (remType == 2 || remType == 3) {
            for (Period period: periods) {
                Instant startInst = period.getStartInstant();
                if (startInst.isSituationStart() || startInst.isSituationEnd())
                    ids.add(startInst.getSituationId());
            }
        }
        return ids;
    }

    public List<Integer> getInvolvedEventIds() {
        List<Integer> ids = new ArrayList<>();
        if (remType == 1) {
            for (Instant instant: instants) {
                if (instant.isEvent())
                    ids.add(instant.getEventId());
            }
        } else if (remType == 2 || remType == 3) {
            for (Period period : periods) {
                Instant startInst = period.getStartInstant();
                if (startInst.isEvent())
                    ids.add(startInst.getEventId());
            }
        }
        return ids;
    }

    public boolean involvesTimeInStartInstant() {
        if (remType == 1) {
            for (Instant instant: instants) {
                if (instant.isTime())
                    return true;
            }
        } else if (remType == 2 || remType == 3) {
            for (Period period : periods) {
                if (period.getStartInstant().isTime())
                    return true;
            }
        }
        return false;
    }


    //// inner classes ////

    /*  A Time is  <days-of-week-selection>.<hr>:<min>  */
    public static class Time {
        private int hour; // can be >= 24
        private int minute;
        private byte dayOfWeekSelection = -1; //binary 1111 1111
        public static final String[] DAY_SYMBOLS = {"Su","M","T","W","R","F","Sa","Su"};

        public Time(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
            if (hour < 0) {
                throw new RuntimeException("'hour' should be >= 0");
            }
            if (minute < 0 || minute > 59) {
                throw new RuntimeException("'minute' should be 0 - 59");
            }
        }

        /* daysOfWeek[i] -- 0,7: Sun, 1: Mon, 2: Tue, ..., 6: Sat */
        public Time(int hour, int minute, int[] daysOfWeek) {
            this(hour, minute);
            dayOfWeekSelection = 0;
            for (int dw: daysOfWeek) {
                if (dw < 0 || dw > 7) {
                    throw new RuntimeException("dayOfWeek should be 0 - 7.");
                }
                if (dw == 0 || dw == 7)
                    dayOfWeekSelection |= ((1 << 7) | 1);
                else
                    dayOfWeekSelection |= (1 << dw);
            }
        }

        public Time(String displayString) {
            try {
                String[] tokens = displayString.split("\\.");

                if (tokens[0].equals("M~Su"))
                    dayOfWeekSelection = -1;
                else {
                    dayOfWeekSelection = 0;
                    for (int i=0; i<7; i++) {
                        if (tokens[0].contains(DAY_SYMBOLS[i])) {
                            dayOfWeekSelection |= (1 << i);
                            if (i == 0)
                                dayOfWeekSelection |= (1 << 7);
                        }
                    }
                }

                String[] hrMin = tokens[1].split(":");
                hour = Integer.parseInt(hrMin[0]);
                minute = Integer.parseInt(hrMin[1]);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw new RuntimeException("Bad format of displayString.");
            }
        }

        // copy constructor
        public Time(Time time) {
            hour = time.hour;
            minute = time.minute;
            dayOfWeekSelection = time.dayOfWeekSelection;
        }

        public boolean isNextDay() {
            return hour >= 24;
        }

        public int getHour() {
            return hour;
        }

        public int getMinute() {
            return minute;
        }

        public int getMinuteNumber() {
            return hour * 60 + minute;
        }

        public List<Integer> getDaysOfWeek(boolean sundayIs0) {
            List<Integer> list = new ArrayList<>();
            if (sundayIs0 && hasDayOfWeek(0))
                list.add(0);
            for (int d=1; d<7; d++) {
                if (hasDayOfWeek(d))
                    list.add(d);
            }
            if (!sundayIs0 && hasDayOfWeek(7))
                list.add(7);
            return list;
        }

        public boolean hasDayOfWeek(int d) {
            return (dayOfWeekSelection & (1 << d)) != 0;
        }

        public String getDisplayString() {
            StringBuilder builder = new StringBuilder();

            if (dayOfWeekSelection == -1)
                builder.append("M~Su");
            else {
                for (int d=1; d<=7; d++) {
                    if (hasDayOfWeek(d))
                        builder.append(DAY_SYMBOLS[d]);
                }
            }
            builder.append(String.format(Locale.US, ".%d:%02d", hour, minute));
            return builder.toString();
        }

        public String getDaysOfWeekDisplayString(String sep) {
            StringBuilder builder = new StringBuilder();
            if (dayOfWeekSelection == -1)
                builder.append("M ~ Su");
            else {
                boolean first = true;
                for (int d=1; d<=7; d++) {
                    if (hasDayOfWeek(d)) {
                        if (!first) {
                            builder.append(sep);
                        }
                        builder.append(DAY_SYMBOLS[d]);
                        first = false;
                    }
                }
            }
            return builder.toString();
        }
    } // class Time


    /*  An Instant can be
            sit<id>start
            sit<id>end
            event<id>
            <Time>     */
    public static class Instant {
        private int type; //1: situation start,  2: situation end,  3: event,  4: time
        private int sitOrEventId;
        private Time time;

        public Instant() {
            type = -1;
        }

        public Instant setAsSituationStart(int situationId) {
            type = 1;
            sitOrEventId = situationId;
            return this;
        }

        public Instant setAsSituationEnd(int situationId) {
            type = 2;
            sitOrEventId = situationId;
            return this;
        }

        public Instant setAsEvent(int eventId) {
            type = 3;
            sitOrEventId = eventId;
            return this;
        }

        public Instant setAsTime(Time time) {
            type = 4;
            this.time = time;
            return this;
        }

        public Instant setFromStringRepresentation(String stringRepresentation) {
            setFromString(stringRepresentation,
                    false, null, null);
            return this;
        }

        public Instant setFromDisplayString(String displayString,
                                            SparseArray<String> allSituations,
                                            SparseArray<String> allEvents) {
            setFromString(displayString, true, allSituations, allEvents);
            return this;
        }

        private void setFromString(String string, boolean isDisplayString,
                                   SparseArray<String> allSituations,
                                   SparseArray<String> allEvents) {
            if (isDisplayString) {
                if (allSituations == null || allEvents == null) {
                    throw new RuntimeException("allSituations & allEvents should not be null "
                            + "as they may be needed.");
                }
            }

            try {
                time = new Time(string);
                type = 4;
                return;
            } catch (RuntimeException e) {
                time = null;
                type = -1;
            }

            try {
                String substr = "";
                if (string.startsWith("sit")) {
                    if (string.endsWith("start")) {
                        substr = string.substring(3, string.length() - 5);
                        type = 1;
                    } else if (string.endsWith("end")) {
                        substr = string.substring(3, string.length() - 3);
                        type = 2;
                    }
                } else if (string.startsWith("event")) {
                    substr = string.substring(5);
                    type = 3;
                }

                if (type == -1) {
                    throw new RuntimeException();
                }


                if (isDisplayString) {
                    if (!substr.startsWith("[") || !substr.endsWith("]")) {
                        throw new RuntimeException();
                    }
                    String name = substr.substring(1, substr.length()-1);

                    sitOrEventId = (type != 3) ?
                            UtilGeneral.searchSparseStringArrayByValue(allSituations, name)
                            : UtilGeneral.searchSparseStringArrayByValue(allEvents, name);
                    if (sitOrEventId == -1) {
                        throw new RuntimeException();
                    }
                } else {
                    sitOrEventId = Integer.parseInt(substr);
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Bad format of `string`");
            }
        }

        // copy constructor
        public Instant(Instant instant) {
            this.type = instant.type;
            this.sitOrEventId = instant.sitOrEventId;
            if (instant.isTime())
                this.time = new Time(instant.time);
            else
                this.time = null;
        }

        //
        public boolean isSituationStart() {
            return type == 1;
        }

        public boolean isSituationEnd() {
            return type == 2;
        }

        public boolean isEvent() {
            return type == 3;
        }

        public boolean isTime() {
            return type == 4;
        }

        public int getSituationId() {
            if (type != 1 && type != 2) {
                throw new RuntimeException("not situation start/end");
            }
            return sitOrEventId;
        }

        public int getEventId() {
            if (type != 3) {
                throw new RuntimeException("not an event");
            }
            return sitOrEventId;
        }

        public Time getTime() { // return a copy
            if (type != 4) {
                throw new RuntimeException("not a Time");
            }
            return new Time(time);
        }

        public String getStringRepresentation() {
            if (type == -1) {
                throw new RuntimeException("object content not set yet");
            }
            switch (type) {
                case 1:
                    return String.format(Locale.US, "sit%dstart", sitOrEventId);
                case 2:
                    return String.format(Locale.US, "sit%dend", sitOrEventId);
                case 3:
                    return String.format(Locale.US, "event%d", sitOrEventId);
                case 4:
                    return time.getDisplayString();
                default:
                    return "";
            }
        }

        public String getDisplayString(@Nullable SparseArray<String> allSituations,
                                       @Nullable SparseArray<String> allEvents) {
            if (type == -1) {
                throw new RuntimeException("object content not set yet");
            } else if (type == 1 || type == 2) {
                if (allSituations == null) {
                    throw new RuntimeException("allSituations should not be null as it is needed");
                }
            } else if (type == 3) {
                if (allEvents == null) {
                    throw new RuntimeException("allEvent should not be null as it is needed");
                }
            }

            switch (type) {
                case 1:
                    return String.format(Locale.US, "sit[%s]start", allSituations.get(sitOrEventId));
                case 2:
                    return String.format(Locale.US, "sit[%s]end", allSituations.get(sitOrEventId));
                case 3:
                    return String.format(Locale.US, "event[%s]", allEvents.get(sitOrEventId));
                case 4:
                    return time.getDisplayString();
                default:
                    return "";
            }
        }
    } // class Instant


    /*  A Period can be
            sit<id>start-sitEnd
            <Instant>-after<n>m
            <Time>-<hr>:<min>    ( <hr>:<min> must be later than the time in <Time> )  */
    public static class Period {
        // start:
        private Instant startInstant;
        // end (one of following):
        private boolean isSituationStartEnd = false;
        private int endTimeHr = -1; private int endTimeMinute = -1;
        private int endAfterMinutes = -1;
        // (not all combination are allowed)

        public Period() {}

        public Period setAsSituationStartEnd(int situationId) {
            startInstant = new Instant().setAsSituationStart(situationId);
            isSituationStartEnd = true;
            endTimeHr = -1;
            endAfterMinutes = -1;
            return this;
        }

        public Period setAsTimeRange(Time startTime, int endHr, int endMinute) {
            if (startTime.isNextDay()) {
                throw new RuntimeException("startTime cannot be > 23:59");
            }
            if ((endHr - startTime.getHour())*60 + endMinute - startTime.getMinute() <= 0) {
                throw new RuntimeException("end time should be later than start time");
            }
            this.startInstant = new Instant().setAsTime(startTime);
            this.isSituationStartEnd = false;
            this.endTimeHr = endHr;
            this.endTimeMinute = endMinute;
            this.endAfterMinutes = -1;
            return this;
        }

        public Period setWithDuration(Instant startInstant, int endAfterMinutes) {
            if (startInstant.isTime()) {
                if (startInstant.getTime().isNextDay()) {
                    throw new RuntimeException("start time cannot be > 23:59");
                }
            }
            if (endAfterMinutes <= 0) {
                throw new RuntimeException("endAfterMinutes should be > 0");
            }
            this.startInstant = startInstant;
            this.isSituationStartEnd = false;
            this.endTimeHr = -1;
            this.endAfterMinutes = endAfterMinutes;
            return this;
        }

        public Period setFromStringRepresentation(String stringRepresentation) {
            setFromString(stringRepresentation,
                    false, null, null);
            return this;
        }

        public Period setFromDisplayString(String displayString,
                                           SparseArray<String> allSituations,
                                           SparseArray<String> allEvents) {
            setFromString(displayString, true, allSituations, allEvents);
            return this;
        }

        private void setFromString(String string, boolean isDisplayString,
                                   SparseArray<String> allSituations,
                                   SparseArray<String> allEvents) {
            if (isDisplayString) {
                if (allSituations == null || allEvents == null) {
                    throw new RuntimeException("allSituations & allEvents should not be null "
                            + "as they may be needed.");
                }
            }

            isSituationStartEnd = false;
            endAfterMinutes = -1;
            endTimeMinute = -1;

            try {
                String[] startEndTokens = string.split("-");

                // startInstant
                if (isDisplayString) {
                    startInstant = new Instant().setFromDisplayString(
                            startEndTokens[0], allSituations, allEvents);
                } else {
                    startInstant = new Instant().setFromStringRepresentation(startEndTokens[0]);
                }

                // end condition
                String endStr = startEndTokens[1];
                boolean done = false;
                if (endStr.equals("sitEnd")) {
                    if (startInstant.isSituationStart()) {
                        isSituationStartEnd = true;
                        done = true;
                    }
                } else if (endStr.startsWith("after") && endStr.endsWith("m")) {
                    String s = endStr.substring(5, endStr.length() - 1);
                    endAfterMinutes = Integer.parseInt(s);
                    if (endAfterMinutes > 0) {
                        done = true;
                    }
                } else if (endStr.contains(":")) {
                    String[] hrMin = endStr.split(":");
                    endTimeHr = Integer.parseInt(hrMin[0]);
                    endTimeMinute = Integer.parseInt(hrMin[1]);
                    if (startInstant.isTime()) {
                        Time startTime = startInstant.getTime();
                        if ((endTimeHr - startTime.getHour())*60
                                + endTimeMinute - startTime.getMinute() > 0) {
                            done = true;
                        }
                    }
                }

                if (!done) {
                    throw new RuntimeException();
                }
            } catch (RuntimeException e) {
                Log.v("ReminderDataBehavior", "### bad string: "+string);
                throw new RuntimeException("Bad format of string representation or displayString");
            }
        }

        // copy constructor
        public Period(Period period) {
            this.startInstant = new Instant(period.startInstant);
            this.isSituationStartEnd = period.isSituationStartEnd;
            this.endTimeHr = period.endTimeHr;
            this.endTimeMinute = period.endTimeMinute;
            this.endAfterMinutes = period.endAfterMinutes;
        }

        // in the following 3 methods, only one returns true:
        public boolean isSituationStartEnd() {
            checkDataIsSet();
            return isSituationStartEnd;
        }

        public boolean isTimeRange() {
            checkDataIsSet();
            return endTimeHr != -1;
        }

        public boolean isEndingAfterDuration() {
            // If true, startInstant can be an Instant of any type.
            checkDataIsSet();
            return endAfterMinutes != -1;
        }

        //
        public Instant getStartInstant() { // return a copy
            checkDataIsSet();
            return new Instant(startInstant);
        }

        public int getDurationMinutes() {
            checkDataIsSet();
            if (endAfterMinutes == -1) {
                throw new RuntimeException("this Period is not set with a duration");
            }
            return endAfterMinutes;
        }

        public int[] getEndHrMin() {
            checkDataIsSet();
            if (endTimeHr == -1) {
                throw new RuntimeException("this Period is not a time range");
            }
            return new int[] {endTimeHr, endTimeMinute};
        }

        //
        public String getDisplayString(SparseArray<String> allSituations,
                                       SparseArray<String> allEvents) {
            checkDataIsSet();
            String startStr = startInstant.getDisplayString(allSituations, allEvents);
            return startStr + "-" + getEndString();
        }

        public String getStringRepresentation() {
            checkDataIsSet();
            String startStr = startInstant.getStringRepresentation();
            return startStr + "-" + getEndString();
        }

        private String getEndString() {
            String endStr;
            if (isSituationStartEnd) {
                endStr = "sitEnd";
            } else if (endAfterMinutes != -1) {
                endStr = String.format(Locale.US, "after%dm", endAfterMinutes);
            } else {
                endStr = String.format(Locale.US, "%02d:%02d", endTimeHr, endTimeMinute);
            }
            return endStr;
        }

        //
        private void checkDataIsSet() {
            if (startInstant == null) {
                throw new RuntimeException("data of this Period not set yet");
            }
        }
    } // class Period
}
