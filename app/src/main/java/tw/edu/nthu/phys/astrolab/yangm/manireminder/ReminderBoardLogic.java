package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReminderBoardLogic {

    private static final int PERIOD_ID_START_FOR_NON_SIT_START_END = 100;
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
        ReminderBehaviorReader reader = new ReminderBehaviorReader();
        reader.addRemindersInvolvingSituations(sitIds);

        // deal with model-1 reminders
        for (int i=0; i<reader.remModel1Behaviors.size(); i++) {
            int remId = reader.remModel1Behaviors.keyAt(i);
            ReminderDataBehavior.Instant[] instants =
                    reader.remModel1Behaviors.valueAt(i).getInstants();
            if (areSitsStartsInInstants(sitIds, instants)) {
                helper1.addReminderToOpen(remId);
            }
        }

        // deal with model-2,3 reminders
        List<List<Integer>> remsStartedPeriodIds = UtilStorage.getRemindersStartedPeriodIds(
                context, UtilGeneral.getKeysOfSparseArray(reader.remModel23Behaviors));

        for (int i=0; i<reader.remModel23Behaviors.size(); i++) {
            int remId = reader.remModel23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = reader.remModel23Behaviors.valueAt(i);
            List<Integer> startedPeriodIds = remsStartedPeriodIds.get(i);

            // determine periods to start
            List<Integer> periodIndexesToStart =
                    getPeriodIndexesToStartOnSitsStart(behavior.getPeriods(), sitIds);

            //
            helper1.handleModel23Reminder(remId, behavior, startedPeriodIds,
                    periodIndexesToStart, new ArrayList<Integer>());
        }

        helper1.takeEffect(at, reader.remModel23Behaviors);
    }

    public void stopSituations(Set<Integer> sitIds, Calendar at) {
        if (sitIds.isEmpty())
            return;

        Helper1 helper1 = new Helper1();

        // get involved reminders
        ReminderBehaviorReader reader = new ReminderBehaviorReader();
        reader.addRemindersInvolvingSituations(sitIds);

        // deal with model-1 reminders
        for (int i = 0; i < reader.remModel1Behaviors.size(); i++) {
            int remId = reader.remModel1Behaviors.keyAt(i);
            ReminderDataBehavior.Instant[] instants =
                    reader.remModel1Behaviors.valueAt(i).getInstants();
            if (areSitsEndsInInstants(sitIds, instants)) {
                helper1.addReminderToOpen(remId);
            }
        }

        // deal with model-2,3 reminders
        List<List<Integer>> remsStartedPeriodIds = UtilStorage.getRemindersStartedPeriodIds(
                context, UtilGeneral.getKeysOfSparseArray(reader.remModel23Behaviors));

        for (int i=0; i<reader.remModel23Behaviors.size(); i++) {
            int remId = reader.remModel23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = reader.remModel23Behaviors.valueAt(i);
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

        helper1.takeEffect(at, reader.remModel23Behaviors);
    }

    public void triggerEvent(int eventId, Calendar at) {
        Helper1 helper1 = new Helper1();

        // get involved reminders
        ReminderBehaviorReader reader = new ReminderBehaviorReader();
        reader.addRemindersInvolvingEvent(eventId);

        // deal with model-1 reminders
        for (int i=0; i<reader.remModel1Behaviors.size(); i++) {
            int remId = reader.remModel1Behaviors.keyAt(i);
            ReminderDataBehavior.Instant[] instants =
                    reader.remModel1Behaviors.valueAt(i).getInstants();
            if (isEventInInstants(eventId, instants)) {
                helper1.addReminderToOpen(remId);
            }
        }

        // deal with model-2,3 reminders
        List<List<Integer>> remsStartedPeriodIds = UtilStorage.getRemindersStartedPeriodIds(
                context, UtilGeneral.getKeysOfSparseArray(reader.remModel23Behaviors));

        for (int i=0; i<reader.remModel23Behaviors.size(); i++) {
            int remId = reader.remModel23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = reader.remModel23Behaviors.valueAt(i);
            List<Integer> startedPeriodIds = remsStartedPeriodIds.get(i);

            // determine periods to start
            List<Integer> periodIndexesToStart =
                    getPeriodIndexesToStartOnEvent(behavior.getPeriods(), eventId);

            //
            helper1.handleModel23Reminder(remId, behavior, startedPeriodIds,
                    periodIndexesToStart, new ArrayList<Integer>());
        }

        helper1.takeEffect(at, reader.remModel23Behaviors);
    }

    public void performScheduledActions(List<ScheduleAction> actions) {
        Helper1 helper1 = new Helper1();

        List<ScheduleAction> periodStartStopActions = new ArrayList<>();
        List<ScheduleAction> model3RemReschedules = new ArrayList<>();
        boolean doMainReschedule = false;
        long millisMin = -1, millisMax = -1;
        for (ScheduleAction action: actions) {

            long millis = action.getTime().getTimeInMillis();
            if (millisMin == -1) {
                millisMin = millis;
                millisMax = millis;
            } else {
                if (millis < millisMin)
                    millisMin = millis;
                else if (millis > millisMax)
                    millisMax = millis;
            }

            switch (action.getType()) {
                case ScheduleAction.TYPE_REMINDER_M1_OPEN:
                case ScheduleAction.TYPE_REMINDER_M3_OPEN:
                    helper1.addReminderToOpen(action.getReminderId());
                    break;

                case ScheduleAction.TYPE_PERIOD_START:
                case ScheduleAction.TYPE_PERIOD_STOP:
                    periodStartStopActions.add(action);
                    break;

                case ScheduleAction.TYPE_RESCHEDULE_M3_REMINDER_REPEATS:
                    model3RemReschedules.add(action);
                    break;

                case ScheduleAction.TYPE_MAIN_RESCHEDULE:
                    if (doMainReschedule){
                        throw new RuntimeException("main reschedule required more than once");
                    }
                    helper1.performMainReschedule();
                    doMainReschedule = true;
                    break;
            }
        }

        // treat all actions as at the same time
        long timeDisperseRange = millisMax - millisMin;
        Log.v("ReminderBoardLogic", "### timeDisperseRange = "+timeDisperseRange);
        if (timeDisperseRange > 1000) {
            Log.w("ReminderBoardLogic", "### timeDisperseRange > 1 sec");
        }

        Calendar at = Calendar.getInstance();
        at.setTimeInMillis(millisMax);

        // get involved reminders for periodStartStopActions and for model3RemReschedules
        List<Integer> remIdsPeriodStartStop = new ArrayList<>();
        for (ScheduleAction action: periodStartStopActions) {
            remIdsPeriodStartStop.add(action.getReminderId());
        }

        List<Integer> remM3IdsToReschedule = new ArrayList<>();
        List<Calendar> remM3RepeatStartedAt = new ArrayList<>();
        for (ScheduleAction action: model3RemReschedules) {
            int remId = action.getReminderId();
            if (remM3IdsToReschedule.contains(remId)) {
                throw new RuntimeException("reschedule the same model-3 reminder more than once");
            }
            remM3IdsToReschedule.add(remId);
            remM3RepeatStartedAt.add(action.getRepeatStartAt());
        }

        // get needed reminder behavior data
        ReminderBehaviorReader reader = new ReminderBehaviorReader();
        if (doMainReschedule) {
            reader.addRemindersInvolvingTimeInInstant();
        }
        reader.addReminders(UtilGeneral.setUnion(remIdsPeriodStartStop, remM3IdsToReschedule));

        // deal with periodStartStopActions
        List<List<Integer>> remsStartedPeriodIds =
                UtilStorage.getRemindersStartedPeriodIds(context, remIdsPeriodStartStop);

        for (int i=0; i<remIdsPeriodStartStop.size(); i++) {
            int remId = remIdsPeriodStartStop.get(i);
            List<Integer> startedPeriodIds = remsStartedPeriodIds.get(i);

            ReminderDataBehavior behavior = reader.remModel23Behaviors.get(remId);
            if (behavior == null) {
                throw new RuntimeException("`remId` not found in reader.remModel23Behaviors");
            }
            if (behavior.getRemType() != 2 && behavior.getRemType() != 3) {
                throw new RuntimeException("reminder is not model-2 or model-3");
            }

            // get periods to start / stop
            List<Integer> periodIndexesToStart = new ArrayList<>();
            List<Integer> periodIdsToStop = new ArrayList<>();
            for (ScheduleAction action: periodStartStopActions) {
                if (action.getReminderId() != remId)
                    continue;
                switch (action.getType()) {
                    case ScheduleAction.TYPE_PERIOD_START:
                        periodIndexesToStart.add(action.getPeriodIndex());
                        break;
                    case ScheduleAction.TYPE_PERIOD_STOP:
                        periodIdsToStop.add(action.getPeriodId());
                        break;
                }
            }

            helper1.handleModel23Reminder(remId, behavior, startedPeriodIds,
                    periodIndexesToStart, periodIdsToStop);
        }

        // model-3 reminder reschedule
        helper1.addRemindersToRescheduleRepeats(remM3IdsToReschedule, remM3RepeatStartedAt);

        //
        helper1.takeEffect(at, reader.remModel23Behaviors, reader.remModel1Behaviors);
    }

    public void onAppStart() {
        Log.v("logic", "app start");
        boolean haveScheduledActions =
                UtilStorage.getRowCountInTable(context, MainDbHelper.TABLE_SCHEDULED_ACTIONS) > 0;
        if (haveScheduledActions) {
            // check that exactly one of the scheduled actions is main reschedule
            if (UtilStorage.countMainSchedulingInScheduledActions(context) != 1) {
                throw new RuntimeException("more than one main reschedule found");
            }
        } else {
            // schedule model-1 reminder opening, period start, and main reschedule
            Log.v("logic", "app run first time after installation");
            ReminderBehaviorReader reader = new ReminderBehaviorReader();
            reader.addRemindersInvolvingTimeInInstant();
            List<ScheduleAction> actions = performMainRescheduling(Calendar.getInstance(),
                    reader.remModel1Behaviors, reader.remModel23Behaviors);
            scheduleNewActions(actions);
        }
    }

    public void onDeviceBootCompleted() {
        // perform "fresh start"

        Log.v("logic", "on device boot complete");
        List<ScheduleAction> actionsToSchedule = new ArrayList<>();

        Calendar now = Calendar.getInstance();
        ReminderBehaviorReader reader = new ReminderBehaviorReader();

        // do main reschedule at now
        reader.addRemindersInvolvingTimeInInstant();
        actionsToSchedule.addAll(performMainRescheduling(
                now, reader.remModel1Behaviors, reader.remModel23Behaviors));

        // re-determine started periods
        List<Integer[]> startedPeriods = new ArrayList<>(); //{rem-id, period-id, period-index}
        List<Calendar> startedPeriodsStartTime = new ArrayList<>();
        SparseArray<List<Integer>> remsStartedPeriodIds = new SparseArray<>(); //duplicate data
        Set<Integer> remM2IdsToOpen = new HashSet<>();
        SparseArray<Calendar> remM3StartedTime = new SparseArray<>();

        reader.addModel23Reminders();
        List<UtilStorage.HistoryRecord> historyRecords =
                UtilStorage.getHistoryRecords(context, null);
        List<Integer> startedSitIds = UtilStorage.getStartedSituations(context);

        List<Integer> periodIndexesToStart = new ArrayList<>(); //used only temporarily
        int minPeriodIdNonSitStartEnd = PERIOD_ID_START_FOR_NON_SIT_START_END;
        for (int i=0; i<reader.remModel23Behaviors.size(); i++) {
            int remId = reader.remModel23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = reader.remModel23Behaviors.valueAt(i);
            ReminderDataBehavior.Period[] periods = behavior.getPeriods();

            periodIndexesToStart.clear();
            Calendar earliestStartTime = Calendar.getInstance();
            for (int p=0; p<periods.length; p++) {
                // find started periods
                List<Calendar> startedTimes = getPeriodStartedTime(now, periods[p],
                        historyRecords, startedSitIds);
                for (Calendar startedTime: startedTimes) {
                    periodIndexesToStart.add(p);
                    startedPeriodsStartTime.add(startedTime);

                    if (startedTime.compareTo(earliestStartTime) < 0) {
                        earliestStartTime = startedTime;
                    }
                }
            }

            if (!periodIndexesToStart.isEmpty()) {
                List<Integer> periodIds = createIdsForPeriodsToStart(
                        periodIndexesToStart, minPeriodIdNonSitStartEnd, behavior);
                for (int j = 0; j < periodIds.size(); j++) {
                    int periodId = periodIds.get(j);
                    startedPeriods.add(
                            new Integer[]{remId, periodId, periodIndexesToStart.get(j)});
                    if (periodId >= minPeriodIdNonSitStartEnd)
                        minPeriodIdNonSitStartEnd++;
                }
                remsStartedPeriodIds.append(remId, periodIds);

                //
                if (behavior.isReminderInPeriod()) {
                    remM2IdsToOpen.add(remId);
                } else {
                    remM3StartedTime.append(remId, earliestStartTime);
                }
            }
        }

        // overwrite started periods to DB
        UtilStorage.writeStartedPeriods(context, remsStartedPeriodIds);

        // schedule ending actions of started periods
        List<Integer[]> list = new ArrayList<>();
        for (int i=0; i<startedPeriods.size(); i++) {
            list.clear();
            list.add(startedPeriods.get(i));
            actionsToSchedule.addAll(schedulePeriodsEndings(
                    list, reader.remModel23Behaviors, startedPeriodsStartTime.get(i)));
        }

        // update the list of opened reminders (model-2 reminders: open only those in
        // `remM2IdsToOpen`, close the others; model-1,3 reminders stay opened/closed)
        Set<Integer> remIdsToRemoveFromOpened = new HashSet<>();
        SparseBooleanArray openedRemsOld = UtilStorage.getOpenedReminders(context);
        for (int i=0; i<openedRemsOld.size(); i++) {
            int remId = openedRemsOld.keyAt(i);

            int index = reader.remModel23Behaviors.indexOfKey(remId);
                    //(note that reader.remModel23Behaviors contains all model-2,3 reminders)
            if (index >= 0 && reader.remModel23Behaviors.valueAt(index).isReminderInPeriod()) {
                //(remId is of model 2)
                if (!remM2IdsToOpen.contains(remId)) {
                    remIdsToRemoveFromOpened.add(remId);
                }
            }
        }
        UtilStorage.removeOpenedReminders(context, remIdsToRemoveFromOpened);
        UtilStorage.addOpenedReminders(context, remM2IdsToOpen);

        // do rescheduling for started model-3 reminders from now
        actionsToSchedule.addAll(
                rescheduleReminderRepeats(remM3StartedTime, reader.remModel23Behaviors, now));

        //
        scheduleNewActions(actionsToSchedule);

        //
        if (!remM2IdsToOpen.isEmpty()) {
            issueNotification();
        }
        Log.v("logic", "fresh start done");
    }

    public void onReminderRemove(int remId) {
        // todo....
    }

    public void onNewReminder(int remId) {
        // todo....

    }

    public void onReminderBehaviorUpdate(int remId) {
        // todo....
    }

    public void onSystemTimeChange() {
        // ...
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////

    private class Helper1 {

        private Set<Integer> remindersToOpen;
        private Set<Integer> remindersToClose;
        private Set<Integer> remindersToStartRepeating;
        private Set<Integer> remindersToStopRepeating;
        private List<Integer[]> periodsToScheduleEnding; //element: {rem-id, period-id, period-index}
        private SparseArray<Set<Integer>> remsNewStartedPeriodIds;
        private boolean doMainReschedule;
        private SparseArray<Calendar> remM3ToRescheduleRepeatStartTime;

        Helper1() {
            remindersToOpen = new HashSet<>();
            remindersToClose = new HashSet<>();
            remindersToStartRepeating = new HashSet<>();
            remindersToStopRepeating = new HashSet<>();
            remM3ToRescheduleRepeatStartTime = new SparseArray<>();
            periodsToScheduleEnding = new ArrayList<>();
            remsNewStartedPeriodIds = new SparseArray<>();
        }

        private void performMainReschedule() {
            doMainReschedule = true;
        }

        private void addReminderToOpen(int remId) {
            remindersToOpen.add(remId);
        }

        private void addRemindersToRescheduleRepeats(List<Integer> remIds,
                                                     List<Calendar> repeatStartedAt) {
            for (int i=0; i<remIds.size(); i++) {
                remM3ToRescheduleRepeatStartTime.append(remIds.get(i), repeatStartedAt.get(i));
            }
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

        private void takeEffect(Calendar at, SparseArray<ReminderDataBehavior> remM23Behaviors) {
            takeEffect(at, remM23Behaviors, null);
        }

        private void takeEffect(Calendar at, SparseArray<ReminderDataBehavior> remM23Behaviors,
                                SparseArray<ReminderDataBehavior> remM1Behaviors) {
            // start repeating reminders (get reminders to open and actions to schedule)
            RemindersToOpenAndScheduleActions data =
                    startRepeatingReminders(remindersToStartRepeating, remM23Behaviors, at);
            remindersToOpen.addAll(data.remindersToOpen);
            List<ScheduleAction> scheduleActions = data.scheduleActions;

            // open/close reminders
            updateBoard(remindersToOpen, remindersToClose);

            // update started periods of reminders in database
            UtilStorage.updateRemindersStartedPeriodIds(context, remsNewStartedPeriodIds);

            // get actions of endings of newly started periods
            scheduleActions.addAll(
                    schedulePeriodsEndings(periodsToScheduleEnding, remM23Behaviors, at));

            // reschedule repeats and next reschedule for model-3 reminders
            for (int remId: remindersToStopRepeating) {
                if (remM3ToRescheduleRepeatStartTime.indexOfKey(remId) >= 0) {
                    remM3ToRescheduleRepeatStartTime.remove(remId);
                }
            }
            scheduleActions.addAll(rescheduleReminderRepeats(
                    remM3ToRescheduleRepeatStartTime, remM23Behaviors, at));

            // main reschedule
            if (doMainReschedule) {
                if (remM1Behaviors == null) {
                    throw new RuntimeException("remM1Behaviors not given");
                }
                scheduleActions.addAll(performMainRescheduling(at, remM1Behaviors, remM23Behaviors));
            }

            // schedule actions
            scheduleNewActions(scheduleActions);

            // stop repeating reminders
            stopRepeatingReminders(remindersToStopRepeating, remM23Behaviors);
        }
    }

    //// private tasks ////////////////////////////////////////////////////////////////////////////

    /**
     * Will consider only reminders given in remM1Behaviors and remM23Behaviors. */
    private List<ScheduleAction> performMainRescheduling(Calendar from,
                                         SparseArray<ReminderDataBehavior> remM1Behaviors,
                                         SparseArray<ReminderDataBehavior> remM23Behaviors) {
        List<ScheduleAction> actions = new ArrayList<>();

        final int TAU = context.getResources().getInteger(R.integer.tau_main_reschedule);
        Calendar to = (Calendar) from.clone();
        to.add(Calendar.MINUTE, TAU);
        to.set(Calendar.SECOND, 0);
        to.set(Calendar.MILLISECOND, 0);

        // schedule opening times of model-1 reminders
        for (int i=0; i<remM1Behaviors.size(); i++) {
            int remId = remM1Behaviors.keyAt(i);

            List<Calendar> openAt = new ArrayList<>();
            ReminderDataBehavior behavior = remM1Behaviors.valueAt(i);
            if (!behavior.isTodoAtInstants()) {
                throw new RuntimeException("reminder is not of model-1");
            }
            ReminderDataBehavior.Instant[] instants = behavior.getInstants();
            for (ReminderDataBehavior.Instant instant: instants) {
                Calendar timeFound = getInstantTimeWithin(instant, from, to); //(from to]
                if (timeFound != null && !openAt.contains(timeFound)) {
                    openAt.add(timeFound);
                }
            }

            for (Calendar t: openAt) {
                actions.add(new ScheduleAction().setAsModel1ReminderOpen(t, remId));
            }
        }

        // schedule starts of periods of model-2,3 reminders
        for (int i=0; i<remM23Behaviors.size(); i++) {
            int remId = remM23Behaviors.keyAt(i);

            ReminderDataBehavior behavior = remM23Behaviors.valueAt(i);
            if (!behavior.isReminderInPeriod() && !behavior.isTodoRepeatedlyInPeriod()) {
                throw new RuntimeException("reminder is not of model-2 or 3");
            }
            ReminderDataBehavior.Period[] periods = behavior.getPeriods();
            for (int p=0; p<periods.length; p++) {
                ReminderDataBehavior.Instant instant = periods[p].getStartInstant();
                Calendar timeFound = getInstantTimeWithin(instant, from, to); //(from, to]
                if (timeFound != null) {
                    actions.add(new ScheduleAction().setAsPeriodStart(timeFound, remId, p));
                }
            }
        }

        // schedule next main reschedule
        actions.add(new ScheduleAction().setAsMainReschedule(to));

        //
        return actions;
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

            ReminderDataBehavior behavior = remindersBehaviors.get(remId);
            if (behavior == null) {
                throw new RuntimeException("`remId` not found in remindersBehaviors");
            }
            ReminderDataBehavior.Period period = behavior.getPeriods()[periodIndex];

            Calendar endTime = (Calendar) periodStartAt.clone();
            if (period.isEndingAfterDuration()) {
                endTime.add(Calendar.MINUTE, period.getDurationMinutes());
                if (endTime.get(Calendar.SECOND) >= 30) {
                    endTime.add(Calendar.MINUTE, 1);
                }
                endTime.set(Calendar.SECOND, 0);
                endTime.set(Calendar.MILLISECOND, 0);
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
                endTime.set(Calendar.MILLISECOND, 0);
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

    private RemindersToOpenAndScheduleActions startRepeatingReminders(
                Set<Integer> reminderIds, SparseArray<ReminderDataBehavior> reminderBehaviors,
                Calendar at) {
        Set<Integer> remindersToOpen = new HashSet<>();
        List<ScheduleAction> scheduleActions = new ArrayList<>();

        for (int remId: reminderIds) {
            ReminderDataBehavior behavior = reminderBehaviors.get(remId);
            if (behavior == null) {
                throw new RuntimeException("`remId` not found in reminderBehaviors");
            }
            if (behavior.getRemType() != 3) {
                throw new RuntimeException("reminder is not of model-3");
            }
            int repeatOffset = behavior.getRepeatOffsetMinutes();
            int repeatEvery = behavior.getRepeatEveryMinutes();

            // open reminder
            if (repeatOffset == 0) {
                remindersToOpen.add(remId);
            }

            // schedule reminder repeats and reschedule
            scheduleActions.addAll(
                    scheduleReminderRepeatsAndNextReschedule(remId, at, at, repeatEvery, repeatOffset));
        }
        return new RemindersToOpenAndScheduleActions(remindersToOpen, scheduleActions);
    }

    private List<ScheduleAction> rescheduleReminderRepeats(
                SparseArray<Calendar> remIdToRepeatStartTime,
                SparseArray<ReminderDataBehavior> reminderBehaviors, Calendar from) {
        List<ScheduleAction> actions = new ArrayList<>();
        for (int i=0; i<remIdToRepeatStartTime.size(); i++) {
            int remId = remIdToRepeatStartTime.keyAt(i);
            Calendar repeatStartTime = remIdToRepeatStartTime.valueAt(i);

            ReminderDataBehavior behavior = reminderBehaviors.get(remId);
            if (behavior == null) {
                throw new RuntimeException("`remId` not found in reminderBehaviors");
            }
            if (behavior.getRemType() != 3) {
                throw new RuntimeException("reminder is not of model-3");
            }

            List<ScheduleAction> moreActions = scheduleReminderRepeatsAndNextReschedule(
                    remId, from, repeatStartTime,
                    behavior.getRepeatEveryMinutes(), behavior.getRepeatOffsetMinutes());
            actions.addAll(moreActions);
        }
        return actions;
    }

    private List<ScheduleAction> scheduleReminderRepeatsAndNextReschedule(
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
        if (rescheduleTime.get(Calendar.SECOND) > 0 || rescheduleTime.get(Calendar.MILLISECOND) > 0) {
            rescheduleTime.add(Calendar.MINUTE, 1);
        }
        rescheduleTime.set(Calendar.SECOND, 0);
        rescheduleTime.set(Calendar.MILLISECOND, 0);
        actions.add(new ScheduleAction()
                .setAsRescheduleModel3ReminderRepeats(rescheduleTime, remId, repeatStartedAt));

        return actions;
    }

    private void updateBoard(Set<Integer> remindersToOpen, Set<Integer> remindersToClose) {
        Set<Integer> intersect = UtilGeneral.setIntersection(remindersToOpen, remindersToClose);
        if (!intersect.isEmpty()) {
            throw new RuntimeException("reminder(s) to open and close at the same time");
        }

        // update the list of opened reminders
        UtilStorage.removeOpenedReminders(context, remindersToClose);
        UtilStorage.addOpenedReminders(context, remindersToOpen);

        // local broadcast
        Intent intent = new Intent(context.getResources().getString(R.string.action_update_board));
        boolean received = LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // issue notification
        if (!remindersToOpen.isEmpty()) {
            issueNotification();
        }
    }

    private void issueNotification() {
        Intent intentOnUserTap = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intentOnUserTap)
                .getPendingIntent(1, PendingIntent.FLAG_ONE_SHOT);

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                context.getResources().getString(R.string.channel_id));
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("New Reminder")
                .setContentText("You have new reminders.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setSound(notificationSound, AudioManager.STREAM_NOTIFICATION)
                .setLights(Color.GREEN,500, 500);
        int notificationId = context.getResources().getInteger(R.integer.notification_id);
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private void scheduleNewActions(List<ScheduleAction> actions) {
        if (actions.isEmpty()) {
            return;
        }
        if (actions.size() == 1) {
            scheduleAlarmWithActions(actions, actions.get(0).getTime());
            return;
        }

        // sort by time
        Collections.sort(actions, new Comparator<ScheduleAction>() {
            @Override
            public int compare(ScheduleAction o1, ScheduleAction o2) {
                return o1.getTime().compareTo(o2.getTime());
            }
        });

        // divide into groups, each group having actions scheduled at the same time
        List<List<ScheduleAction>> actionGroups = new ArrayList<>();
        actionGroups.add(new ArrayList<ScheduleAction>());
        actionGroups.get(0).add(actions.get(0));
        int g = 0;
        Calendar groupTime = actionGroups.get(0).get(0).getTime();
        for (int i=1; i<actions.size(); i++) {
            ScheduleAction action = actions.get(i);
            if (action.getTime().compareTo(groupTime) == 0) {
                actionGroups.get(g).add(action);
            } else {
                actionGroups.add(new ArrayList<ScheduleAction>());
                g++;
                actionGroups.get(g).add(action);
                groupTime = action.getTime();
            }
        }

        // attach each group to an alarm
        for (g=0; g<actionGroups.size(); g++) {
            scheduleAlarmWithActions(actionGroups.get(g), actionGroups.get(g).get(0).getTime());
        }
    }

    private void scheduleAlarmWithActions(List<ScheduleAction> actions, Calendar scheduleAt) {
        // Ideally, all the `actions` have the same scheduled time which equal `scheduleAt`.
        // Anyway, they will be attached to an alarm scheduled at `scheduleAt`.

        if (actions.isEmpty()) {
            return;
        }

        // get a new alarm id
        int alarmId = UtilStorage.readSharedPrefInt(
                context, UtilStorage.KEY_NEW_ALARM_ID, 0);

        // create PendingIntent to attach to alarm
        Intent intentActions = new Intent(context, AlarmReceiver.class)
                .setAction(context.getResources().getString(R.string.action_scheduled_actions))
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, alarmId, intentActions, PendingIntent.FLAG_ONE_SHOT);

        // set alarm using alarmId
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, scheduleAt.getTimeInMillis(), pendingIntent);

        // add the actions (with alarmId) to DB
        UtilStorage.addScheduledActions(context, alarmId, actions);

        // increment new alarm id
        UtilStorage.writeSharedPrefInt(context, UtilStorage.KEY_NEW_ALARM_ID, alarmId+1);

        // [log]
        for (ScheduleAction action: actions) {
            Log.v("logic", String.format("scheduled action: alarm-id %d, %s", alarmId,
                    action.getDisplayString()));
        }
    }

    private void stopRepeatingReminders(Set<Integer> reminderIds,
                                        SparseArray<ReminderDataBehavior> remBehaviors) {
        if (reminderIds.isEmpty()) {
            return;
        }

        // get alarm id's with an action to be canceled
        Set<Integer> affectedAlarmIds = new HashSet<>();

        List<Integer> args = new ArrayList<>();
        args.add(ScheduleAction.TYPE_REMINDER_M3_OPEN);
        args.add(ScheduleAction.TYPE_RESCHEDULE_M3_REMINDER_REPEATS);
        args.addAll(reminderIds);

        SQLiteDatabase db = UtilStorage.getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SCHEDULED_ACTIONS, new String[] {"alarm_id"},
                "type IN (?,?) AND "
                        + "reminder_id IN ("+UtilStorage.placeHolders(reminderIds.size())+")",
                UtilGeneral.toStringArray(args),
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            affectedAlarmIds.add(cursor.getInt(0));
        }
        cursor.close();

        //
        for (int alarmId: affectedAlarmIds) { // for each affected scheduled alarm
            // get new list of actions
            List<ScheduleAction> actions = UtilStorage.getScheduledActions(context, alarmId);
            List<ScheduleAction> newActionList = new ArrayList<>();
            for (ScheduleAction action: actions) {
                if (action.getType() != ScheduleAction.TYPE_REMINDER_M3_OPEN
                    && action.getType() != ScheduleAction.TYPE_RESCHEDULE_M3_REMINDER_REPEATS) {
                    newActionList.add(action);
                }
            }

            // cancel alarm
            Intent intentActions = new Intent(context, AlarmReceiver.class)
                    .setAction(context.getResources().getString(R.string.action_scheduled_actions));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, alarmId, intentActions, PendingIntent.FLAG_ONE_SHOT);
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(pendingIntent);
            Log.v("logic", "canceled alarm "+alarmId);

            // delete records
            UtilStorage.removeScheduledActions(context, alarmId);

            // schedule new alarm with actions in `newActionList`
            if (!newActionList.isEmpty()) {
                scheduleNewActions(newActionList);
            }
        }
    }

    //// private tools ////////////////////////////////////////////////////////////////////////////

    private class ReminderBehaviorReader {

        private SparseArray<ReminderDataBehavior> remModel1Behaviors = new SparseArray<>();
        private SparseArray<ReminderDataBehavior> remModel23Behaviors = new SparseArray<>();

        private void clear() {
            remModel1Behaviors.clear();
            remModel23Behaviors.clear();
        }

        /**
         * @param sitIds cannot be empty */
        private void addRemindersInvolvingSituations(Set<Integer> sitIds) {
            if (sitIds.isEmpty()) {
                throw new RuntimeException("sitIds is empty");
            }
            Cursor cursor = queryRemInvolvingSituations(sitIds);
            List<SparseArray<ReminderDataBehavior>> list = groupByModelFromCursor(cursor);
            cursor.close();
            UtilGeneral.sparseArrayPutAll(remModel1Behaviors, list.get(1));
            UtilGeneral.sparseArrayPutAll(remModel23Behaviors, list.get(2));
        }

        private void addRemindersInvolvingEvent(int eventId) {
            Cursor cursor = queryRemInvolvingEvent(eventId);
            List<SparseArray<ReminderDataBehavior>> list = groupByModelFromCursor(cursor);
            cursor.close();
            UtilGeneral.sparseArrayPutAll(remModel1Behaviors, list.get(1));
            UtilGeneral.sparseArrayPutAll(remModel23Behaviors, list.get(2));
        }

        private void addRemindersInvolvingTimeInInstant() {
            Cursor cursor = queryRemInvolvingTimeInInstant();
            List<SparseArray<ReminderDataBehavior>> list = groupByModelFromCursor(cursor);
            cursor.close();
            UtilGeneral.sparseArrayPutAll(remModel1Behaviors, list.get(1));
            UtilGeneral.sparseArrayPutAll(remModel23Behaviors, list.get(2));
        }

        private void addModel23Reminders() {
            Cursor cursor = queryModel23Reminders();
            List<SparseArray<ReminderDataBehavior>> list = groupByModelFromCursor(cursor);
            cursor.close();
            UtilGeneral.sparseArrayPutAll(remModel23Behaviors, list.get(2));
        }

        private void addReminders(Set<Integer> remIds) {
            Cursor cursor = queryReminders(remIds);
            List<SparseArray<ReminderDataBehavior>> list = groupByModelFromCursor(cursor);
            cursor.close();
            UtilGeneral.sparseArrayPutAll(remModel1Behaviors, list.get(1));
            UtilGeneral.sparseArrayPutAll(remModel23Behaviors, list.get(2));
        }

        // low-level methods //
        private Cursor queryRemInvolvingSituations(Set<Integer> sitIds) {
            // returns a cursor containing columns (reminder-id, model, behavior-settings), or null
            // if sitIds is empty

            if (sitIds.isEmpty())
                return null;

            StringBuilder builderWhere = new StringBuilder();
            List<String> whereArgs = new ArrayList<>();
            boolean first = true;
            for (int id : sitIds) {
                if (!first)
                    builderWhere.append(" OR ");
                builderWhere.append("involved_sits LIKE ?");
                whereArgs.add("%," + id + ",%");
                first = false;
            }
            return db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                    new String[]{"_id", "type", "behavior_settings"},
                    builderWhere.toString(), whereArgs.toArray(new String[0]),
                    null, null, null);
        }

        private Cursor queryRemInvolvingEvent(int eventId) {
            // returns a cursor containing columns (reminder-id, model, behavior-settings)
            return db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                    new String[]{"_id", "type", "behavior_settings"},
                    "involved_events LIKE ?", new String[]{"%," + eventId + ",%"},
                    null, null, null);
        }

        private Cursor queryRemInvolvingTimeInInstant() {
            // returns a cursor containing columns (reminder-id, model, behavior-settings)
            return db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                    new String[]{"_id", "type", "behavior_settings"},
                    "involve_time_in_start_instant = ?", new String[] {"1"},
                    null, null, null);
        }

        private Cursor queryModel23Reminders() {
            return db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                    new String[]{"_id", "type", "behavior_settings"},
                    "type IN (?,?)", new String[] {"2", "3"},
                    null, null, null);
        }

        private Cursor queryReminders(Set<Integer> remIds) {
            // returns a cursor containing columns (reminder-id, model, behavior-settings)
            return db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                    new String[]{"_id", "type", "behavior_settings"},
                    "_id IN ("+UtilStorage.placeHolders(remIds.size())+")",
                    UtilGeneral.toStringArray(remIds),
                    null, null, null);
        }

        private List<SparseArray<ReminderDataBehavior>> groupByModelFromCursor(Cursor cursor) {
            // returned list --- [0]: model-0 reminders,  [1]: model-1 reminders,
            //                   [2]: model-2,3 reminders
            // `cursor` is not closed.
            List<SparseArray<ReminderDataBehavior>> arrays = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
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
    }

    /**
     * If `instant` is a Time having a time point within (beginExclusive, end], return the
     * time point. Otherwise, return null.
     * (end - beginExclusive) must < 1 day  */
    private Calendar getInstantTimeWithin(ReminderDataBehavior.Instant instant,
                                          Calendar beginExclusive, Calendar end) {
        if (!instant.isTime()) {
            return null;
        }

        // round beginExclusive to next minute --> `from`
        Calendar from = (Calendar) beginExclusive.clone();
        from.add(Calendar.MINUTE, 1);
        from.set(Calendar.SECOND, 0);
        from.set(Calendar.MILLISECOND, 0);

        // round `end` to minute --> `to`
        Calendar to = (Calendar) end.clone();
        to.set(Calendar.SECOND, 0);
        to.set(Calendar.MILLISECOND, 0);

        //
        long dt = to.getTimeInMillis() - from.getTimeInMillis();
        if (dt >= 86400000) {
            throw new RuntimeException("to - from >= 1 day");
        } else if (dt < 0) {
            return null;
        }

        //
        List<int[]> dates = new ArrayList<>(); //{year, month ,day-of-month}
        List<int[]> intervals = new ArrayList<>(); //{day-of-week, start-hr, start-min, end-hr, end-min}
        if (to.get(Calendar.DAY_OF_MONTH) == from.get(Calendar.DAY_OF_MONTH)) {
            int dw = from.get(Calendar.DAY_OF_WEEK) - 1;
            if (dw == 0)
                dw = 7;
            int hr0 = from.get(Calendar.HOUR_OF_DAY);
            int min0 = from.get(Calendar.MINUTE);
            int hr1 = to.get(Calendar.HOUR_OF_DAY);
            int min1 = to.get(Calendar.MINUTE);
            intervals.add(new int[] {dw, hr0, min0, hr1, min1});
            dates.add(new int[] {from.get(Calendar.YEAR),
                    from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH)});
        } else {
            // [from, to] is across two days
            // 1st interval
            int dw = from.get(Calendar.DAY_OF_WEEK) - 1;
            int hr0 = from.get(Calendar.HOUR_OF_DAY);
            int min0 = from.get(Calendar.MINUTE);
            int hr1 = 23;
            int min1 = 59;
            intervals.add(new int[] {dw, hr0, min0, hr1, min1});
            dates.add(new int[] {from.get(Calendar.YEAR),
                    from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH)});

            // 2nd interval
            dw = to.get(Calendar.DAY_OF_WEEK) - 1;
            hr0 = 0;
            min0 = 0;
            hr1 = to.get(Calendar.HOUR_OF_DAY);
            min1 = to.get(Calendar.MINUTE);
            intervals.add(new int[] {dw, hr0, min0, hr1, min1});
            dates.add(new int[] {to.get(Calendar.YEAR),
                    to.get(Calendar.MONTH), to.get(Calendar.DAY_OF_MONTH)});
        }

        //
        ReminderDataBehavior.Time t = instant.getTime();
        if (t.isNextDay()) {
            throw new RuntimeException("instant is a Time on next day");
        }
        for (int i=0; i<intervals.size(); i++) {
            int[] interval = intervals.get(i);

            int dw = interval[0];
            if (t.hasDayOfWeek(dw)) {
                int startMinNo = interval[1] * 60 + interval[2];
                int minNo = t.getHour() * 60 + t.getMinute();
                if (minNo >= startMinNo) {
                    int endMinNo = interval[3] * 60 + interval[4];
                    if (minNo <= endMinNo) { // `t` is in `interval`
                        int[] date = dates.get(i);
                        return new GregorianCalendar(
                                date[0], date[1], date[2], t.getHour(), t.getMinute());
                    }
                }
            }
        }
        return null;
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
        int newPeriodIdMinForNonSitStartEnd = startedPeriodIds.isEmpty() ?
                PERIOD_ID_START_FOR_NON_SIT_START_END
                : Math.max(PERIOD_ID_START_FOR_NON_SIT_START_END,
                        Collections.max(startedPeriodIds) + 1);
        return createIdsForPeriodsToStart(
                periodIndexesToStart, newPeriodIdMinForNonSitStartEnd, behavior);
    }

    /** see above */
    private List<Integer> createIdsForPeriodsToStart(List<Integer> periodIndexesToStart,
                                                     int newPeriodIdMinForNonSitStartEnd,
                                                     ReminderDataBehavior behavior) {
        List<Integer> periodIdsToStart = new ArrayList<>();
        for (int i=0; i<periodIndexesToStart.size(); i++) {
            int periodIndex = periodIndexesToStart.get(i);
            int periodId;
            if (behavior.getPeriods()[periodIndex].isSituationStartEnd()) {
                periodId = -periodIndex; // id <= 0 for sit. start-end
            } else {
                periodId = newPeriodIdMinForNonSitStartEnd;
                newPeriodIdMinForNonSitStartEnd++;
            }
            periodIdsToStart.add(periodId);
        }
        return periodIdsToStart;
    }

    /**
     * Determine whether `period` is started at `at`, given the history records and list of
     * started situations.
     * If yes, return the start time(s) of the period's instantiation(s) (there can be more than
     * one instantiations). Otherwise, return an empty list.
     * @param historyRecords must be in descending order of time */
    private List<Calendar> getPeriodStartedTime(Calendar at, ReminderDataBehavior.Period period,
                                                List<UtilStorage.HistoryRecord> historyRecords,
                                                List<Integer> startedSituations) {
        List<Calendar> periodStartTimes = new ArrayList<>();
        HistoryRecordsFinder histRecordsFinder = new HistoryRecordsFinder(historyRecords);

        if (period.isSituationStartEnd()) {
            int sitId = period.getStartInstant().getSituationId();
            if (startedSituations.contains(sitId)) {
                // try to find start of situation `sitId` in `historyRecords`
                Calendar t = histRecordsFinder.findLatestSituationStart(sitId);
                if (t == null) {
                    // not found in history: return N days ago
                    final int DAYS_MAX = context.getResources()
                            .getInteger(R.integer.history_record_days_max);
                    t = (Calendar) at.clone();
                    t.add(Calendar.DAY_OF_MONTH, -DAYS_MAX);
                }
                periodStartTimes.add(t);
            }
            return periodStartTimes;
        }
        else if (period.isTimeRange()) {
            int atDayOfWeek = at.get(Calendar.DAY_OF_WEEK) - 1;
            int prevDayOfWeek = atDayOfWeek == 0 ? 6 : (atDayOfWeek - 1);
            int atMinuteNumber = at.get(Calendar.HOUR_OF_DAY) * 60  + at.get(Calendar.MINUTE);

            ReminderDataBehavior.Time startTime = period.getStartInstant().getTime();

            Calendar periodStartTimeOnAtDay = (Calendar) at.clone();
            periodStartTimeOnAtDay.set(Calendar.HOUR_OF_DAY, startTime.getHour());
            periodStartTimeOnAtDay.set(Calendar.MINUTE, startTime.getMinute());
            periodStartTimeOnAtDay.set(Calendar.SECOND, 0);
            periodStartTimeOnAtDay.set(Calendar.MILLISECOND, 0);

            int[] endHrMin = period.getEndHrMin();
            if (endHrMin[0] >= 24) {
                if (startTime.hasDayOfWeek(atDayOfWeek)) {
                    if (startTime.getMinuteNumber() <= atMinuteNumber) {
                        periodStartTimes.add(periodStartTimeOnAtDay);
                    }
                }
                if (startTime.hasDayOfWeek(prevDayOfWeek)) {
                    if (atMinuteNumber < endHrMin[0] * 60 + endHrMin[1]) {
                        Calendar t = (Calendar) periodStartTimeOnAtDay.clone();
                        t.add(Calendar.DAY_OF_MONTH, -1);
                        periodStartTimes.add(t);
                    }
                }
            } else { //(endHrMin[0] < 24)
                if (startTime.hasDayOfWeek(atDayOfWeek)) {
                    if (startTime.getMinuteNumber() <= atMinuteNumber
                        && atMinuteNumber < endHrMin[0] * 60 + endHrMin[1]) {
                        periodStartTimes.add(periodStartTimeOnAtDay);
                    }
                }
            }
            return periodStartTimes;
        }
        else {  //(ending after duration)
            int duration = period.getDurationMinutes();
            Calendar from = (Calendar) at.clone();
            from.add(Calendar.MINUTE, -duration);

            ReminderDataBehavior.Instant instant = period.getStartInstant();
            if (instant.isSituationStart()) {
                periodStartTimes.addAll(
                        histRecordsFinder.findSituationStartInTimeRange(
                                instant.getSituationId(), from, at));
            } else if (instant.isSituationEnd()) {
                periodStartTimes.addAll(
                        histRecordsFinder.findSituationEndInTimeRange(
                                instant.getSituationId(), from, at));
            } else if (instant.isEvent()) {
                periodStartTimes.addAll(
                        histRecordsFinder.findEventInTimeRange(
                                instant.getSituationId(), from, at));
            } else { // (instant is Time)
                Calendar tf = (Calendar) at.clone();
                while (tf.compareTo(from) > 0) {
                    Calendar ti = (Calendar) tf.clone();
                    ti.add(Calendar.HOUR_OF_DAY, -12);
                    if (ti.compareTo(from) < 0) {
                        ti = from;
                    }
                    Calendar tInstant = getInstantTimeWithin(instant, ti, tf);
                    if (tInstant != null) {
                        periodStartTimes.add(tInstant);
                    }

                    //
                    tf.add(Calendar.HOUR_OF_DAY, -12);
                }
            }
            return periodStartTimes;
        }
    }

    private class HistoryRecordsFinder {
        private List<UtilStorage.HistoryRecord> historyRecords;

        /**
         * @param historyRecords must be in descending order of time */
        HistoryRecordsFinder(List<UtilStorage.HistoryRecord> historyRecords) {
            this.historyRecords = historyRecords;
        }

        /**
         * @return null if not found  */
        private Calendar findLatestSituationStart(int sitId) {
            for (int i = 0; i < historyRecords.size(); i++) {
                UtilStorage.HistoryRecord record = historyRecords.get(i);
                if (record.isStartOfSituationWithId(sitId)) {
                    return record.getTime();
                }
            }
            return null;
        }

        private List<Calendar> findSituationStartInTimeRange(int sitId,
                                                             Calendar fromExclusive, Calendar to) {
            return this.findRecordsInTimeRange(
                    UtilStorage.HIST_TYPE_SIT_START, sitId, fromExclusive, to);
        }

        private List<Calendar> findSituationEndInTimeRange(int sitId,
                                                           Calendar fromExclusive, Calendar to) {
            return this.findRecordsInTimeRange(
                    UtilStorage.HIST_TYPE_SIT_END, sitId, fromExclusive, to);
        }

        private List<Calendar> findEventInTimeRange(int eventId,
                                                    Calendar fromExclusive, Calendar to) {
            return this.findRecordsInTimeRange(
                    UtilStorage.HIST_TYPE_EVENT, eventId, fromExclusive, to);
        }

        private List<Calendar> findRecordsInTimeRange(int type, int sitOrEventId,
                                                      Calendar fromExclusive, Calendar to) {
            List<Calendar> sitStartTimes = new ArrayList<>();
            for (int i = 0; i < historyRecords.size(); i++) {
                UtilStorage.HistoryRecord record = historyRecords.get(i);
                Calendar recTime = record.getTime();
                if (recTime.compareTo(fromExclusive) <= 0) {
                    break;
                }

                if (record.getSitOrEventId() == sitOrEventId && record.getType() == type) {
                    if (recTime.compareTo(to) <= 0) {
                        sitStartTimes.add(recTime);
                    }
                }
            }
            return sitStartTimes;
        }
    }

}
