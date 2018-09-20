package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
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

        Cursor cursor = readRemBehaviorInvolvingSituations(sitIds); //(rem-id, model, behavior)

        Set<Integer> remindersToOpen = new HashSet<>();


        /// deal with model-1 reminders, and find model-2,3 reminders
        List<Integer> model23RemIds = new ArrayList<>();
        List<ReminderDataBehavior> model23RemBehaviors = new ArrayList<>();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) { //for each reminder
            int remId = cursor.getInt(0);
            int model = cursor.getInt(1);
            ReminderDataBehavior behavior = new ReminderDataBehavior()
                    .setFromStringRepresentation(cursor.getString(2));

            if (model == 0) //(should not happen, though)
                continue;
            if (model == 1) { //to-do at instants
                boolean triggerReminder = false;
                ReminderDataBehavior.Instant[] instants = behavior.getInstants();
                for (ReminderDataBehavior.Instant instant: instants) {
                    if (instant.isSituationStart() && sitIds.contains(instant.getSituationId())) {
                        triggerReminder = true;
                        break;
                    }
                }
                if (triggerReminder) {
                    remindersToOpen.add(remId);
                }
            } else {
                // model-2,3 reminders
                model23RemIds.add(remId);
                model23RemBehaviors.add(behavior);
            }
        }
        cursor.close();

        /// deal with model-2,3 reminders, and find reminders whose list of started periods gets updated
        if (model23RemIds.isEmpty())
            return;

        List<Integer[]> model23RemStartedPeriods =
                UtilStorage.getRemindersStartedPeriods(context, model23RemIds);

        List<Integer> remIdsToUpdateStartedPeriods = new ArrayList<>();
        List<Integer[]> remsNewStartedPeriods = new ArrayList<>();
        for (int i=0; i<model23RemIds.size(); i++) {
            int id = model23RemIds.get(i);
            Integer[] startedPeriods = model23RemStartedPeriods.get(i);
            ReminderDataBehavior behavior = model23RemBehaviors.get(i);
            Set<Integer> periodsToStart = getPeriodsToStartOnSitsStart(behavior.getPeriods(), sitIds);

            if (startedPeriods.length == 0 && !periodsToStart.isEmpty()) {
                if (behavior.getRemType() == 2) { // remind during periods
                    // open reminder
                    remindersToOpen.add(id);
                } else if (behavior.getRemType() == 3) { // to-do repetitively during periods
                    // todo: start repeating reminder

                }




            }

            // todo: schedule actions for newly started periods whose end condition is not situation end



            // reminders which need updating started periods
            if (!periodsToStart.isEmpty()) { //can be more strict................

                Set<Integer> newStartedPeriods = new HashSet<>(Arrays.asList(startedPeriods));
                newStartedPeriods.addAll(periodsToStart);

                remIdsToUpdateStartedPeriods.add(id);
                remsNewStartedPeriods.add(newStartedPeriods.toArray(new Integer[0]));
            }
        }

        /// update started periods of reminders
        UtilStorage.updateRemindersStartedPeriods(
                context, remIdsToUpdateStartedPeriods, remsNewStartedPeriods);
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

    public void actionsByTime() {

    }

    /*
    public void openModel13RemindersByTime(Set<Integer> remIds) {

    }

    public void startReminderPeriodsByTime(List<Integer> remIds, List<Integer> periodIndexes) {

    }

    public void stopReminderPeriodsByTime(List<Integer> remIds, List<Integer> periodIndexes) {

    }
*/

    ////
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
