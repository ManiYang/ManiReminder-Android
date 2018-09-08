package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReminderDataBoardControl {

    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_TODO_AT_INSTANTS = 0;
    public static final int TYPE_REMINDER_IN_PERIODS = 1;
    public static final int TYPE_TODO_REPEATEDLY_IN_PERIODS = 2;

    private int remType = TYPE_UNKNOWN;
    private Instant[] instants; //for TYPE_TODO_AT_INSTANTS
    private Period[] periods;  //for TYPE_REMINDER_IN_PERIODS
    private RepetitionWithinPeriod[] repetitionPeriods; //for TYPE_TODO_REPEATEDLY_IN_PERIODS

    public ReminderDataBoardControl() {
    }

    public ReminderDataBoardControl setAsTodoAtInstants(Instant[] instants) {
        remType = TYPE_TODO_AT_INSTANTS;
        this.instants = instants;
        periods = null;
        repetitionPeriods = null;
        return this;
    }



    

    //
    public static class Time {
        private int hour; // can be >= 24
        private int minute;
        private int dayOfWeek = -1; // 1-7;  -1: not set

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

        public Time(int hour, int minute, int dayOfWeek) {
            this(hour, minute);
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                throw new RuntimeException("dayOfWeek should be 1 - 7.");
            }
            this.dayOfWeek = dayOfWeek;
        }

        public Time(String displayString) {
            try {
                String[] tokens = displayString.split(".");

                String[] hrMin = tokens[tokens.length - 1].split(":");
                hour = Integer.parseInt(hrMin[0]);
                minute = Integer.parseInt(hrMin[1]);

                if (tokens.length == 2) {
                    dayOfWeek = Integer.parseInt(tokens[0]);
                    if (dayOfWeek < 1 || dayOfWeek > 7)
                        throw new RuntimeException();
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Bad format of displayString.");
            }
        }

        // copy constructor
        public Time(Time time) {
            hour = time.hour;
            minute = time.minute;
            dayOfWeek = time.dayOfWeek;
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

        public int getDayOfWeek() {
            return dayOfWeek;
        }

        public String getDisplayString() {
            StringBuilder builder = new StringBuilder();
            if (dayOfWeek > 0) {
                builder.append(dayOfWeek).append('.');
            }
            builder.append(String.format("%02d:%02d", hour, minute));
            return builder.toString();
        }
    } // class Time

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

        public Instant setFromDisplayString(String displayString) {
            try {
                int n = displayString.length();
                type = -1;
                if (displayString.startsWith("sit")) {
                    if (displayString.endsWith("start")) {
                        type = 1;
                        String id = displayString.substring(3, n - 5);
                        sitOrEventId = Integer.parseInt(id);
                    } else if (displayString.endsWith("end")) {
                        type = 2;
                        String id = displayString.substring(3, n - 3);
                        sitOrEventId = Integer.parseInt(id);
                    }
                } else if (displayString.startsWith("event")) {
                    type = 3;
                    String id = displayString.substring(5);
                    sitOrEventId = Integer.parseInt(id);
                } else {
                    time = new Time(displayString);
                }

                if (type == -1) {
                    throw new RuntimeException();
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Bad format of displayString");
            }
            return this;
        }

        // copy constructor
        public Instant(Instant instant) {
            this.type = instant.type;
            this.sitOrEventId = instant.sitOrEventId;
            this.time = new Time(instant.time);
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

        public String getDisplayString() {
            if (type == -1) {
                throw new RuntimeException("object content not set yet");
            }
            switch (type) {
                case 1:
                    return "sit" + Integer.toString(sitOrEventId) + "start";
                case 2:
                    return "sit" + Integer.toString(sitOrEventId) + "end";
                case 3:
                    return "event" + Integer.toString(sitOrEventId);
                case 4:
                    return time.getDisplayString();
                default:
                    return "";
            }
        }
    } // class Instant

    public static class Period {
        // start:
        private Instant startInstant;
        // end (one of following):
        private boolean isSituationStartEnd = false;
        private int endTimeHr = -1; private int endTimeMinute = -1;
        private int endAfterMinutes = -1;
        // (not all combination are allowed)

        public Period() {
        }

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

        public Period setFromDisplayString(String displayString) {
            isSituationStartEnd = false;
            endAfterMinutes = -1;
            endTimeMinute = -1;

            try {
                String[] startEndTokens = displayString.split("-");
                startInstant = new Instant().setFromDisplayString(startEndTokens[0]);
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
                throw new RuntimeException("Bad format of displayString");
            }
            return this;
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
        public String getDisplayString() {
            checkDataIsSet();

            String startStr = startInstant.getDisplayString();
            String endStr;
            if (isSituationStartEnd) {
                endStr = "sitEnd";
            } else if (endAfterMinutes != -1) {
                endStr = "after" + Integer.toString(endAfterMinutes) + "m";
            } else {
                endStr = String.format("%02d:%02d", endTimeHr, endTimeMinute);
            }
            return startStr + "-" + endStr;
        }

        //
        private void checkDataIsSet() {
            if (startInstant == null) {
                throw new RuntimeException("data of this Period not set yet");
            }
        }
    } // class Period

    public static class RepetitionWithinPeriod {
        private Period enclosingPeriod;
        private int offsetMinutes;
        private int repeatEveryMinutes;

        public RepetitionWithinPeriod(Period enclosingPeriod,
                                      int repeatEveryMinutes, int offsetMinutes) {
            this.enclosingPeriod = enclosingPeriod;
            this.offsetMinutes = offsetMinutes;
            this.repeatEveryMinutes = repeatEveryMinutes;
        }

        public RepetitionWithinPeriod(String displayString) {
            try {
                String[] tokens = displayString.split(" during ");
                enclosingPeriod = new Period().setFromDisplayString(tokens[1]);

                Matcher matcher = Pattern.compile("every(\\d+)m\\.offset(\\d+)m").matcher(tokens[0]);
                repeatEveryMinutes = Integer.parseInt(matcher.group(1));
                offsetMinutes = Integer.parseInt(matcher.group(2));
            } catch (RuntimeException e) {
                throw new RuntimeException("Bad format of displayString");
            }
        }

        //
        public Period getEnclosingPeriod() {
            return new Period(enclosingPeriod);
        }

        public int getOffsetMinutes() {
            return offsetMinutes;
        }

        public int getRepeatEveryMinutes() {
            return repeatEveryMinutes;
        }

        public String getDisplayString() {
            return "every" + Integer.toString(repeatEveryMinutes)
                    + "m.offset" + Integer.toString(offsetMinutes)
                    + "m during " + enclosingPeriod.getDisplayString();
        }
    } // class RepetitionWithinPeriod

}
