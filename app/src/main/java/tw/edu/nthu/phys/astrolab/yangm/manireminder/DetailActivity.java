package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_ID = "reminder_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //
        int reminderId = getIntent().getIntExtra(EXTRA_REMINDER_ID, -99);
        DetailFragment detailFragment =
                (DetailFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_detail);
        detailFragment.setReminderId(reminderId);
        Log.v("DetailActivity", "### reminder ID: "+Integer.toString(reminderId));
    }
}
