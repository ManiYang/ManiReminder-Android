package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.SparseArray;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReminderBoardLogic {

    private Context context;
    private SQLiteDatabase db;

    public ReminderBoardLogic(Context context) {
        try {
            db = new MainDbHelper(context).getWritableDatabase();
        } catch (SQLiteException e) {
            Toast.makeText(context, "Database unavailable", Toast.LENGTH_LONG).show();
            throw new RuntimeException("Database unavailable");
        }
        this.context = context;
    }

    public void startSituations(Set<Integer> sitIds, Calendar at) {
        if (sitIds.isEmpty())
            return;

        Helper1 helper1 = new Helper1();

        // get involved reminders
        Cursor cursor = readRemBehaviorInvolvingSituations(sitIds); //(rem-id, model, behavior)

        // get reminders (and their behavior data) of model 1 and those of model 2 & 3
        List<SparseArray<ReminderDataBehavior>> arrays = groupByModelFromCursor(cursor);
        cursor.close();
        SparseArray<ReminderDataBehavior> remM1Behaviors = arrays.get(1);
        SparseArray<ReminderDataBehavior> remM23Behaviors = arrays.get(2);

        // deal with model-1 reminders
        for (int i=0; i<remM1Behaviors.size(); i++) {
            int remId = remM1Behaviors.keyAt(i);
            ReminderDataBehavior.Instant[] instants = remM1Behaviors.valueAt(i).getInstants();
            if (areSitsStartsInInstants(sitIds, instants)) {
                helper1.addReminderToOpen(remId);
            }
        }

        // deal with model-2,3 reminders
        List<List<Integer>> remsStartedPeriodIds = UtilStorage.getRemindersStartedPeriodIds(
                context, UtilGeneral.getKeysOfSparseArray(remM23Behaviors));

        for (int i=0; i<remM23Behaviors.size(); i++) {
            int remId = remM23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = remM23Behaviors.valueAt(i);
            List<Integer> startedPeriodIds = remsStartedPeriodIds.get(i);

            // determine periods to start
            List<Integer> periodIndexesToStart =
                    getPeriodIndexesToStartOnSitsStart(behavior.getPeriods(), sitIds);

            //
            helper1.handleModel23Reminder(remId, behavior, startedPeriodIds,
                    periodIndexesToStart, new ArrayList<Integer>());
        }

        helper1.takeEffect(at, remM23Behaviors);
    }



    public void stopSituations(Set<Integer> sitIds, Calendar at) {
        if (sitIds.isEmpty())
            return;

        Helper1 helper1 = new Helper1();

        // get involved reminders
        Cursor cursor = readRemBehaviorInvolvingSituations(sitIds); //(rem-id, model, behavior)

        // get reminders (and their behavior data) of model 1 and those of model 2 & 3
        List<SparseArray<ReminderDataBehavior>> arrays = groupByModelFromCursor(cursor);
        cursor.close();
        SparseArray<ReminderDataBehavior> remM1Behaviors = arrays.get(1);
        SparseArray<ReminderDataBehavior> remM23Behaviors = arrays.get(2);

        // deal with model-1 reminders
        for (int i = 0; i < remM1Behaviors.size(); i++) {
            int remId = remM1Behaviors.keyAt(i);
            ReminderDataBehavior.Instant[] instants = remM1Behaviors.valueAt(i).getInstants();
            if (areSitsEndsInInstants(sitIds, instants)) {
                helper1.addReminderToOpen(remId);
            }
        }

        // deal with model-2,3 reminders
        List<List<Integer>> remsStartedPeriodIds = UtilStorage.getRemindersStartedPeriodIds(
                context, UtilGeneral.getKeysOfSparseArray(remM23Behaviors));

        for (int i=0; i<remM23Behaviors.size(); i++) {
            int remId = remM23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = remM23Behaviors.valueAt(i);
            List<Integer> startedPeriodIds = remsStartedPeriodIds.get(i);

            // determine periods to start
            List<Integer> periodIndexesToStart =
                    getPeriodIndexesToStartOnSitsEnd(behavior.getPeriods(), sitIds);

            // determine periods to stop
            List<Integer> periodIndexesToEnd =
                    getPeriodIndexesToEndOnSitsEnd(behavior.getPeriods(), sitIds);

            // get id's of periods to stop
            List<Integer> periodIdsToStop = new ArrayList<>();
            for (int periodIndex: periodIndexesToEnd) {
                periodIdsToStop.add(-periodIndex);
            }

            //
            helper1.handleModel23Reminder(remId, behavior,
                    startedPeriodIds, periodIndexesToStart, periodIdsToStop);
        }

        helper1.takeEffect(at, remM23Behaviors);
    }

    public void triggerEvent(int eventId, Calendar at) {
        Helper1 helper1 = new Helper1();

        // get involved reminders
        Cursor cursor = readRemBehaviorInvolvingEvent(eventId); //(rem-id, model, behavior)

        // get reminders (and their behavior data) of model 1 and those of model 2 & 3
        List<SparseArray<ReminderDataBehavior>> arrays = groupByModelFromCursor(cursor);
        cursor.close();
        SparseArray<ReminderDataBehavior> remM1Behaviors = arrays.get(1);
        SparseArray<ReminderDataBehavior> remM23Behaviors = arrays.get(2);

        // deal with model-1 reminders
        for (int i=0; i<remM1Behaviors.size(); i++) {
            int remId = remM1Behaviors.keyAt(i);
            ReminderDataBehavior.Instant[] instants = remM1Behaviors.valueAt(i).getInstants();
            if (isEventInInstants(eventId, instants)) {
                helper1.addReminderToOpen(remId);
            }
        }

        // deal with model-2,3 reminders
        List<List<Integer>> remsStartedPeriodIds = UtilStorage.getRemindersStartedPeriodIds(
                context, UtilGeneral.getKeysOfSparseArray(remM23Behaviors));

        for (int i=0; i<remM23Behaviors.size(); i++) {
            int remId = remM23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = remM23Behaviors.valueAt(i);
            List<Integer> startedPeriodIds = remsStartedPeriodIds.get(i);

            // determine periods to start
            List<Integer> periodIndexesToStart =
                    getPeriodIndexesToStartOnEvent(behavior.getPeriods(), eventId);

            //
            helper1.handleModel23Reminder(remId, behavior, startedPeriodIds,
                    periodIndexesToStart, new ArrayList<Integer>());
        }

        helper1.takeEffect(at, remM23Behaviors);
    }

    public void performScheduleActions(ScheduleAction[] actions) {
        // todo.....

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private class Helper1 {

        private Set<Integer> remindersToOpen;
        private Set<Integer> remindersToClose;
        private Set<Integer> remindersToStartRepeating;
        private Set<Integer> remindersToStopRepeating;
        private List<Integer[]> periodsToScheduleEnding; //element: {rem-id, period-id, period-index}
        private SparseArray<Set<Integer>> remsNewStartedPeriodIds;

        Helper1() {
            remindersToOpen = new HashSet<>();
            remindersToClose = new HashSet<>();
            remindersToStartRepeating = new HashSet<>();
            remindersToStopRepeating = new HashSet<>();
            periodsToScheduleEnding = new ArrayList<>();
            remsNewStartedPeriodIds = new SparseArray<>();
        }

        private void addReminderToOpen(int remId) {
            remindersToOpen.add(remId);
        }

        private void handleModel23Reminder(int remId, ReminderDataBehavior behavior,
                                           List<Integer> startedPeriodIds,
                                           List<Integer> periodIndexesToStart,
                                           List<Integer> periodIdsToStop) {
            if (behavior.getRemType() != 2 && behavior.getRemType() != 3) {
                throw new RuntimeException("reminder is not of model 2 or 3");
            }
            if (!UtilGeneral.setDifference(periodIdsToStop, startedPeriodIds).isEmpty()) {
                throw new RuntimeException("stopping a period that is not already started");
            }

            //
            List<Integer> periodIdsToStart =
                    createIdsForPeriodsToStart(periodIndexesToStart, startedPeriodIds, behavior);
            if (UtilGeneral.haveNonemptyIntersection(periodIdsToStart, startedPeriodIds)) {
                throw new RuntimeException("starting already started period(s)");
            }

            //
            Set<Integer> newStartedPeriodIds = new HashSet<>(startedPeriodIds);
            newStartedPeriodIds.addAll(periodIdsToStart);
            newStartedPeriodIds.removeAll(periodIdsToStop);

            // open/close or start/stop repeating
            if (startedPeriodIds.isEmpty() && !newStartedPeriodIds.isEmpty()) {
                if (behavior.getRemType() == 2)
                    remindersToOpen.add(remId);
                else
                    remindersToStartRepeating.add(remId);
            } else if (!startedPeriodIds.isEmpty() && newStartedPeriodIds.isEmpty()) {
                if (behavior.getRemType() == 2)
                    remindersToClose.add(remId);
                else
                    remindersToStopRepeating.add(remId);
            }

            // to schedule endings for periods-to-start whose end conditions are time/duration
            for (int j = 0; j < periodIndexesToStart.size(); j++) {
                int periodIndex = periodIndexesToStart.get(j);
                int periodId = periodIdsToStart.get(j);
                if (periodId >= 100) {
                    periodsToScheduleEnding.add(new Integer[]{remId, periodId, periodIndex});
                }
            }

            // to update the list of started periods
            if (!periodIdsToStart.isEmpty() || !periodIdsToStop.isEmpty()) {
                remsNewStartedPeriodIds.append(remId, newStartedPeriodIds);
            }
        }

        private void takeEffect(Calendar at, SparseArray<ReminderDataBehavior> remBehaviors) {
            // start repeating reminders (get reminders to open and actions to schedule)
            RemindersToOpenAndScheduleActions data =
                    startRepeatingReminders(remindersToStartRepeating, remBehaviors, at);
            remindersToOpen.addAll(data.remindersToOpen);
            List<ScheduleAction> scheduleActions = data.scheduleActions;

            // open/close reminders
            updateBoard(remindersToOpen, remindersToClose);

            // update started periods of reminders
            UtilStorage.updateRemindersStartedPeriodIds(context, remsNewStartedPeriodIds);

            // get actions of endings of newly started periods
            scheduleActions.addAll(
                    schedulePeriodsEndings(periodsToScheduleEnding, remBehaviors, at));

            // schedule actions
            scheduleNewActions(scheduleActions);

            // stop repeating reminders
            stopRepeatingReminders(remindersToStopRepeating);
        }
    }

    //// private tasks ////////////////////////////////////////////////////////////////////////////
    /**
     * @param remindersPeriods: Each element must be {reminder-id, period-id, period-index}  */
    private List<ScheduleAction> schedulePeriodsEndings(
            List<Integer[]> remindersPeriods,
            SparseArray<ReminderDataBehavior> remindersBehaviors, Calendar periodStartAt) {
        List<ScheduleAction> actions = new ArrayList<>();

        for (Integer[] remIdPeriodId: remindersPeriods) {
            int remId = remIdPeriodId[0];
            int periodId = remIdPeriodId[1];
            int periodIndex = remIdPeriodId[2];
            ReminderDataBehavior.Period period =
                    remindersBehaviors.get(remId).getPeriods()[periodIndex];

            Calendar endTime = (Calendar) periodStartAt.clone();
            if (period.isEndingAfterDuration()) {
                endTime.add(Calendar.MINUTE, period.getDurationMinutes());
            } else if (period.isTimeRange()) {
                int[] hrMin = period.getEndHrMin();

                int hr = hrMin[0];
                if (hr >= 24) {
                    endTime.add(Calendar.DAY_OF_MONTH, 1);
                    hr -= 24;
                }
                endTime.set(Calendar.HOUR, hr);
                endTime.set(Calendar.MINUTE, hrMin[1]);
                endTime.set(Calendar.SECOND, 0);
            }
            actions.add(new ScheduleAction().setAsPeriodStop(endTime, remId, periodId));
        }
        return actions;
    }

    private class RemindersToOpenAndScheduleActions {
        private Set<Integer> remindersToOpen;
        private List<ScheduleAction> scheduleActions;

        RemindersToOpenAndScheduleActions(Set<Integer> remindersToOpen,
                                          List<ScheduleAction> scheduleActions) {
            this.remindersToOpen = remindersToOpen;
            this.scheduleActions = scheduleActions;
        }
    }

    private void updateBoard(Set<Integer> remindersToOpen, Set<Integer> remindersToClose) {
        //[temp]
        //String text = UtilGeneral.joinIntegerList(",", new ArrayList<>(remindersToOpen));
        //Toast.makeText(context, "reminders to open: "+text, Toast.LENGTH_LONG).show();

        // todo ...


    }

    private void scheduleNewActions(List<ScheduleAction> actions) {
        // todo .....
    }

    private void stopRepeatingReminders(Set<Integer> reminderIds) {
        // todo .....

    }

    //// private tools ////////////////////////////////////////////////////////////////////////////
    /**
     * @param reminderBehaviors must include all of reminders in `reminderIds` */
    private RemindersToOpenAndScheduleActions startRepeatingReminders(
            Set<Integer> reminderIds, SparseArray<ReminderDataBehavior> reminderBehaviors,
            Calendar at) {
        Set<Integer> remindersToOpen = new HashSet<>();
        List<ScheduleAction> scheduleActions = new ArrayList<>();

        for (int remId: reminderIds) {
            ReminderDataBehavior behavior = reminderBehaviors.get(remId);
            int repeatOffset = behavior.getRepeatOffsetMinutes();
            int repeatEvery = behavior.getRepeatEveryMinutes();

            // open reminder
            if (repeatOffset == 0) {
                remindersToOpen.add(remId);
            }

            // schedule reminder repeats and reschedule
            scheduleActions.addAll(
                    scheduleReminderRepeatsAndReschedule(remId, at, at, repeatEvery, repeatOffset));
        }

        return new RemindersToOpenAndScheduleActions(remindersToOpen, scheduleActions);
    }

    private List<ScheduleAction> scheduleReminderRepeatsAndReschedule(
            int remId, Calendar from, Calendar repeatStartedAt, int repeatEvery, int repeatOffset) {
        List<ScheduleAction> actions = new ArrayList<>();

        // schedule repeats within (from, from+tau]
        int tau = context.getResources().getInteger(R.integer.tau_m3_reschedule);

        float dt1 = UtilDatetime.timeDifference(from, repeatStartedAt) - repeatOffset;
        int n1 = (int) Math.floor(dt1/repeatEvery) + 1;
        int n2 = (int) Math.floor((dt1 + tau)/repeatEvery);
        if (n2 >= n1 && n2 >= 0) {
            if (n1 < 0)
                n1 = 0;
            for (int n=n1; n<=n2; n++) {
                Calendar t = UtilDatetime.addMinutes(
                        repeatStartedAt, repeatOffset + n*repeatEvery);
                actions.add(new ScheduleAction().setAsModel3ReminderOpen(t, remId));
            }
        }

        // schedule reschedule
        Calendar rescheduleTime = UtilDatetime.addMinutes(from, tau);
        actions.add(new ScheduleAction()
                .setAsRescheduleModel3ReminderRepeats(rescheduleTime, remId, repeatStartedAt));

        return actions;
    }

    /**
     * @param cursor must have columns (rem-id, model, behavior)
     * @return [0]: model-0 reminders,  [1]: model-1 reminders,  [2]: model-2,3 reminders
     * `cursor` is not closed. */
    private List<SparseArray<ReminderDataBehavior>> groupByModelFromCursor(Cursor cursor) {
        List<SparseArray<ReminderDataBehavior>> arrays = new ArrayList<>();
        for (int i=0; i<3; i++) {
            arrays.add(new SparseArray<ReminderDataBehavior>());
        }

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            int remId = cursor.getInt(0);
            int model = cursor.getInt(1);
            ReminderDataBehavior behavior = new ReminderDataBehavior()
                    .setFromStringRepresentation(cursor.getString(2));

            if (model == 0 || model == 1) {
                arrays.get(model).append(remId, behavior);
            } else if (model == 2 || model == 3) {
                arrays.get(2).append(remId, behavior);
            }
        }

        return arrays;
    }

    private boolean areSitsStartsInInstants(Set<Integer> sitIds,
                                            ReminderDataBehavior.Instant[] instants) {
        for (ReminderDataBehavior.Instant instant: instants) {
            if (instant.isSituationStart() && sitIds.contains(instant.getSituationId()))
                return true;
        }
        return false;
    }

    private boolean areSitsEndsInInstants(Set<Integer> sitIds,
                                          ReminderDataBehavior.Instant[] instants) {
        for (ReminderDataBehavior.Instant instant: instants) {
            if (instant.isSituationEnd() && sitIds.contains(instant.getSituationId()))
                return true;
        }
        return false;
    }

    private boolean isEventInInstants(int eventId, ReminderDataBehavior.Instant[] instants) {
        for (ReminderDataBehavior.Instant instant: instants) {
            if (instant.isEvent() && instant.getEventId() == eventId)
                return true;
        }
        return false;
    }

    /** returns {i: periods[i] gets started when all of `sitIds` start} */
    private List<Integer> getPeriodIndexesToStartOnSitsStart(ReminderDataBehavior.Period[] periods,
                                                             Set<Integer> sitIds) {
        List<Integer> periodIndexesToStart = new ArrayList<>();
        for (int i=0; i<periods.length; i++) {
            ReminderDataBehavior.Instant instant = periods[i].getStartInstant();
            if (instant.isSituationStart() && sitIds.contains(instant.getSituationId()))
                periodIndexesToStart.add(i);
        }
        return periodIndexesToStart;
    }

    /** returns {i: periods[i] gets started when all of `sitIds` end} */
    private List<Integer> getPeriodIndexesToStartOnSitsEnd(ReminderDataBehavior.Period[] periods,
                                                           Set<Integer> sitIds) {
        List<Integer> periodIndexesToStart = new ArrayList<>();
        for (int i=0; i<periods.length; i++) {
            ReminderDataBehavior.Instant instant = periods[i].getStartInstant();
            if (instant.isSituationEnd() && sitIds.contains(instant.getSituationId()))
                periodIndexesToStart.add(i);
        }
        return periodIndexesToStart;
    }

    /** returns {i: periods[i] gets stopped when all of `sitIds` end} */
    private List<Integer> getPeriodIndexesToEndOnSitsEnd(ReminderDataBehavior.Period[] periods,
                                                         Set<Integer> sitIds) {
        List<Integer> periodIndexesToEnd = new ArrayList<>();
        for (int i=0; i<periods.length; i++) {
            if (periods[i].isSituationStartEnd() &&
                    sitIds.contains(periods[i].getStartInstant().getSituationId())) {
                periodIndexesToEnd.add(i);
            }
        }
        return periodIndexesToEnd;
    }

    /** returns {i: periods[i] gets started when `eventId` happens} */
    private List<Integer> getPeriodIndexesToStartOnEvent(ReminderDataBehavior.Period[] periods,
                                                         int eventId) {
        List<Integer> periodIndexesToStart = new ArrayList<>();
        for (int i=0; i<periods.length; i++) {
            ReminderDataBehavior.Instant instant = periods[i].getStartInstant();
            if (instant.isEvent() && instant.getEventId() == eventId)
                periodIndexesToStart.add(i);
        }
        return periodIndexesToStart;
    }

    /**
     * Assign an period ID to every period in `periodIndexesToStart`.
     * If the period is a situation start-end, its ID will be (period index)*(-1).
     * For the other kinds of periods, the ID will be >= 100, will be unique to each period
     * instantiation, and will be distinct from all started period instantiations.  */
    private List<Integer> createIdsForPeriodsToStart(List<Integer> periodIndexesToStart,
                                                     List<Integer> startedPeriodIds,
                                                     ReminderDataBehavior behavior) {
        List<Integer> periodIdsToStart = new ArrayList<>();
        int newPeriodIdMin = startedPeriodIds.isEmpty() ?
                100 : Math.max(100, Collections.max(startedPeriodIds) + 1);
        for (int i=0; i<periodIndexesToStart.size(); i++) {
            int periodIndex = periodIndexesToStart.get(i);
            int periodId;
            if (behavior.getPeriods()[periodIndex].isSituationStartEnd()) {
                periodId = -periodIndex; // id <= 0 for sit. start-end
            } else {
                periodId = newPeriodIdMin;
                newPeriodIdMin++;
            }
            periodIdsToStart.add(periodId);
        }
        return periodIdsToStart;
    }

    /**
     * @return cursor containing columns (reminder-id, model, behavior-settings), or null if sitIds
     *         is empty
     * Remember to close the cursor. */
    private Cursor readRemBehaviorInvolvingSituations(Set<Integer> sitIds) {
        if (sitIds.isEmpty())
            return null;

        StringBuilder builderWhere = new StringBuilder();
        List<String > whereArgs = new ArrayList<>();
        boolean first = true;
        for (int id: sitIds) {
            if (!first)
                builderWhere.append(" OR ");
            builderWhere.append("involved_sits LIKE ?");
            whereArgs.add("%," + id + ",%");
            first = false;
        }

        return db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                new String[] {"_id", "type", "behavior_settings"},
                builderWhere.toString(), whereArgs.toArray(new String[0]),
                null, null, null);
    }

    /**
     * @return cursor containing columns (reminder-id, model, behavior-settings)
     * Remember to close the cursor. */
    private Cursor readRemBehaviorInvolvingEvent(int eventId) {
        return db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                new String[] {"_id", "type", "behavior_settings"},
                "involved_events LIKE ?", new String[] {"%,"+eventId+",%"},
                null, null, null);
    }
}
