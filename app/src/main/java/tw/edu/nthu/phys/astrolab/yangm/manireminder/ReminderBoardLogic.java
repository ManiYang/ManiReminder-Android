package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.widget.Toast;

import java.util.Set;

public class ReminderBoardLogic {

    private SQLiteDatabase db;

    public ReminderBoardLogic(Context context) {
        try {
            db = new MainDbHelper(context).getWritableDatabase();
        } catch (SQLiteException e) {
            Toast.makeText(context, "Database unavailable", Toast.LENGTH_LONG).show();
            throw new RuntimeException("Database unavailable");
        }
    }

    public void startSituations(Set<Integer> sitIds) {

    }

    public void stopSituations(Set<Integer> sitIds) {

    }

    public void triggerEvent(int eventId) {

    }

    ////////////
    private Cursor readRemBehaviorInvolvingSituations(Set<Integer> sitIds) {
        return null;

    }

    private Cursor readRemBehaviorInvolvingEvent(int eventId) {
        return null;
    }


}
