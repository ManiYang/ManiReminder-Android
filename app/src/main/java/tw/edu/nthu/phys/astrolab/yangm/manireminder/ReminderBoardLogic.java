package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.SparseArray;
import android.widget.Toast;

import java.util.ArrayList;
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

    public void startSituations(Set<Integer> sitIds) {
        if (sitIds.isEmpty())
            return;

        Set<Integer> remindersToOpen = new HashSet<>();
        Set<Integer> remindersToStartRepeating = new HashSet<>();
        List<Integer[]> reminderPeriodsToScheduleEnding = new ArrayList<>();

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
        List<Integer[]> remsStartedPeriods = UtilStorage.getRemindersStartedPeriods(
                context, UtilGeneral.getKeysOfSparseArray(remM23Behaviors));

        SparseArray<Set<Integer>> remsNewStartedPeriods = new SparseArray<>();
        for (int i=0; i<remM23Behaviors.size(); i++) {
            int remId = remM23Behaviors.keyAt(i);
            ReminderDataBehavior behavior = remM23Behaviors.valueAt(i);
            Integer[] startedPeriods = remsStartedPeriods.get(i);

            // determine periods to start
            Set<Integer> periodsToStart = UtilGeneral.setDifference(
                    getPeriodsToStartOnSitsStart(behavior.getPeriods(), sitIds), startedPeriods);

            // to open reminder or start repeating it
            if (startedPeriods.length == 0 && !periodsToStart.isEmpty()) {
                if (behavior.getRemType() == 2) {
                    // open reminder
                    remindersToOpen.add(remId);
                } else {
                    // start repeating reminder
                    remindersToStartRepeating.add(remId);
                }
            }

            // to schedule actions for periods-to-start whose end condition is time/duration
            for (int period: periodsToStart) {
                reminderPeriodsToScheduleEnding.add(new Integer[] {remId, period});
            }

            // to update started periods in database
            if (!periodsToStart.isEmpty()) {
                remsNewStartedPeriods.append(remId,
                        UtilGeneral.setUnion(periodsToStart, startedPeriods));
            }
        }

        // start repeating reminders
        // remindersToStartRepeating
        // todo...



        // open reminders
        updateBoard(remindersToOpen, new HashSet<Integer>());

        // update started periods of reminders
        UtilStorage.updateRemindersStartedPeriods(context, remsNewStartedPeriods);

        // schedule endings of newly started periods
        // reminderPeriodsToScheduleEnding
        // todo...




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
    private Set<Integer> getPeriodsToStartOnSitsStart(ReminderDataBehavior.Period[] periods,
                                                      Set<Integer> sitIds) {
        Set<Integer> periodsToStart = new HashSet<>();
        for (int i=0; i<periods.length; i++) {
            ReminderDataBehavior.Instant instant = periods[i].getStartInstant();
            if (instant.isSituationStart() && sitIds.contains(instant.getSituationId()))
                periodsToStart.add(i);
        }
        return periodsToStart;
    }




    public void stopSituations(Set<Integer> sitIds) {
        if (sitIds.isEmpty())
            return;

        Set<Integer> remindersToOpen = new HashSet<>();

        Cursor cursor = readRemBehaviorInvolvingSituations(sitIds); //(rem-id, model, behavior)
        ReminderDataBehavior behavior = null;
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

    public void triggerEvent(int eventId) {
        Set<Integer> remindersToOpen = new HashSet<>();
        Cursor cursor = readRemBehaviorInvolvingEvent(eventId); //(rem-id, model, behavior)
        ReminderDataBehavior behavior = null;
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
