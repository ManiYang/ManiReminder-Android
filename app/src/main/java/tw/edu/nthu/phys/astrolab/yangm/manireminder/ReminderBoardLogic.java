package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReminderBoardLogic {

    private SQLiteDatabase db;
    private Context context;

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

        // [temp]
        cursor.moveToPosition(-1);
        List<Integer> remIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            remIds.add(cursor.getInt(0));
        }
        String remIdsStr = UtilGeneral.joinIntegerList(",", remIds);
        Toast.makeText(context, "involved reminders: "+remIdsStr, Toast.LENGTH_LONG).show();


    }

    public void stopSituations(Set<Integer> sitIds) {


    }

    public void triggerEvent(int eventId) {


    }

    ////
    /**
     * @return cursor containing columns (reminder-id, model, behavior-settings), or null if sitIds
     *         is empty */
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
     * @return cursor containing columns (reminder-id, model, behavior-settings) */
    private Cursor readRemBehaviorInvolvingEvent(int eventId) {
        return db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                new String[] {"_id", "type", "behavior_settings"},
                "involved_events LIKE ?", new String[] {"%,"+eventId+",%"},
                null, null, null);
    }


}
