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

        Set<Integer> remindersToOpen = new HashSet<>();
        Set<Integer> remindersToStartRepeating = new HashSet<>();
        List<Integer[]> periodsToScheduleEnding =
                new ArrayList<>(); //each element: [reminder-id, period-id, period-index]

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
                remindersToOpen.add(remId);
            }
        }

        // deal with model-2,3 reminders
        List<List<Integer>> remsStartedPeriodIds = UtilStorage.getRemindersStartedPeriodIds(
                context, UtilGeneral.getKeysOfSparseArray(remM23Behaviors));

        SparseArray<Set<Integer>> remsNewStartedPeriodIds = new SparseArray<>();
        for (int i=0; i<remM23Behaviors.size(); i++) {
            int remId = remM23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = remM23Behaviors.valueAt(i);
            List<Integer> startedPeriodIds = remsStartedPeriodIds.get(i);

            // determine periods to start
            List<Integer> periodIndexesToStart =
                    getPeriodIndexesToStartOnSitsStart(behavior.getPeriods(), sitIds);

            // to open reminder or start repeating it
            if (startedPeriodIds.isEmpty() && !periodIndexesToStart.isEmpty()) {
                if (behavior.getRemType() == 2) {
                    remindersToOpen.add(remId);
                } else {
                    remindersToStartRepeating.add(remId);
                }
            }

            // create id's of periods to start
            List<Integer> periodIdsToStart =
                    createIdsForPeriodsToStart(periodIndexesToStart, startedPeriodIds, behavior);

            // to schedule actions for periods-to-start whose end condition is time/duration
            for (int j=0; j<periodIndexesToStart.size(); j++) {
                int periodIndex = periodIndexesToStart.get(j);
                int periodId = periodIdsToStart.get(j);
                if (periodId >= 100) {
                    periodsToScheduleEnding.add(new Integer[] {remId, periodId, periodIndex});
                }
            }

            // to update started periods in database
            if (!periodIdsToStart.isEmpty()) {
                remsNewStartedPeriodIds.append(remId,
                        UtilGeneral.setUnion(periodIdsToStart, startedPeriodIds));
            }
        }

        // start repeating reminders (get reminders to open and actions to schedule)
        RemindersToOpenAndScheduleActions data =
                startRepeatingReminders(remindersToStartRepeating, remM23Behaviors, at);
        remindersToOpen.addAll(data.remindersToOpen);
        List<ScheduleAction> scheduleActions = data.scheduleActions;

        // open reminders
        updateBoard(remindersToOpen, new HashSet<Integer>());

        // update started periods of reminders
        UtilStorage.updateRemindersStartedPeriodIds(context, remsNewStartedPeriodIds);

        // get schedule actions of endings of newly started periods
        scheduleActions.addAll(
                schedulePeriodsEndings(periodsToScheduleEnding, remM23Behaviors, at));

        // schedule actions
        //scheduleActions
    }

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

        public RemindersToOpenAndScheduleActions(Set<Integer> remindersToOpen,
                                                 List<ScheduleAction> scheduleActions) {
            this.remindersToOpen = remindersToOpen;
            this.scheduleActions = scheduleActions;
        }
    }

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

    private List<ScheduleAction> scheduleReminderRepeatsAndReschedule(int remId,
            Calendar from, Calendar repeatStartedAt, int repeatEvery, int repeatOffset) {
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

    boolean areSitsStartsInInstants(Set<Integer> sitIds, ReminderDataBehavior.Instant[] instants) {
        for (ReminderDataBehavior.Instant instant: instants) {
            if (instant.isSituationStart() && sitIds.contains(instant.getSituationId()))
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








    public void stopSituations(Set<Integer> sitIds, Calendar at) {
        if (sitIds.isEmpty())
            return;

        Set<Integer> remindersToOpen = new HashSet<>();

        Cursor cursor = readRemBehaviorInvolvingSituations(sitIds); //(rem-id, model, behavior)
        ReminderDataBehavior behavior;
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) { //for each reminder
            int remId = cursor.getInt(0);
            int model = cursor.getInt(1);
            behavior = new ReminderDataBehavior()
                    .setFromStringRepresentation(cursor.getString(2));

            if (model == 0) //(should not happen, though)
                continue;
            if (model == 1) { //to-do at instants
                boolean triggerReminder = false;
                ReminderDataBehavior.Instant[] instants = behavior.getInstants();
                for (ReminderDataBehavior.Instant instant: instants) {
                    if (instant.isSituationEnd() && sitIds.contains(instant.getSituationId())) {
                        triggerReminder = true;
                        break;
                    }
                }
                if (triggerReminder) {
                    remindersToOpen.add(remId);
                }
            } else {
                //...........



            }
        }
        cursor.close();
    }

    public void triggerEvent(int eventId, Calendar at) {
        Set<Integer> remindersToOpen = new HashSet<>();
        Cursor cursor = readRemBehaviorInvolvingEvent(eventId); //(rem-id, model, behavior)
        ReminderDataBehavior behavior;
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) { //for each reminder
            int remId = cursor.getInt(0);
            int model = cursor.getInt(1);
            behavior = new ReminderDataBehavior()
                    .setFromStringRepresentation(cursor.getString(2));

            if (model == 0) //(should not happen, though)
                continue;
            if (model == 1) { //to-do at instants
                boolean triggerReminder = false;
                ReminderDataBehavior.Instant[] instants = behavior.getInstants();
                for (ReminderDataBehavior.Instant instant: instants) {
                    if (instant.isEvent() && instant.getEventId() == eventId) {
                        triggerReminder = true;
                        break;
                    }
                }
                if (triggerReminder) {
                    remindersToOpen.add(remId);
                }
            } else {
                //...........


            }
        }
        cursor.close();
    }

    public void performScheduleActions(ScheduleAction[] actions) {
        Set<Integer> remindersToOpen = new HashSet<>();



    }

    //
    private void updateBoard(Set<Integer> remindersToOpen, Set<Integer> remindersToClose) {
        //[temp]
        //String text = UtilGeneral.joinIntegerList(",", new ArrayList<>(remindersToOpen));
        //Toast.makeText(context, "reminders to open: "+text, Toast.LENGTH_LONG).show();

        // todo ...


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
