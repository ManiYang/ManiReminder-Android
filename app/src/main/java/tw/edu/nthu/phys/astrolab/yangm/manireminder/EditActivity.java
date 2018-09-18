package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

public class EditActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_TITLE = "reminder_title";
    public static final String EXTRA_FIELD_NAME = "field_name";
    public static final String EXTRA_INIT_DATA = "init_data";
    public static final String EXTRA_NEW_DATA = "new_data";
    public static final String EXTRA_INIT_ALL_TAGS = "init_all_tags";
    public static final String EXTRA_NEW_ALL_TAGS = "new_all_tags";
    public static final String EXTRA_INIT_ALL_SITUATIONS = "init_all_sits";
    public static final String EXTRA_NEW_ALL_SITUATIONS = "new_all_sits";
    public static final String EXTRA_INIT_ALL_EVENTS = "init_all_events";
    public static final String EXTRA_NEW_ALL_EVENTS = "new_all_events";
    public static final int RESULT_CODE_OK = 1;
    public static final int RESULT_CODE_CANCELED = 0;
    private String fieldName;
    EditResultHolder editResultHolder;

    public interface EditResultHolder {
        boolean validateData();
        Intent getResult();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // get data from intent
        Intent intent = getIntent();
        String remTitle = intent.getStringExtra(EXTRA_REMINDER_TITLE);
        fieldName = intent.getStringExtra(EXTRA_FIELD_NAME);
        String initData = intent.getStringExtra(EXTRA_INIT_DATA);
        String initAllTags = intent.getStringExtra(EXTRA_INIT_ALL_TAGS); // may be null
        String initAllSits = intent.getStringExtra(EXTRA_INIT_ALL_SITUATIONS); // may be null
        String initAllEvents = intent.getStringExtra(EXTRA_INIT_ALL_EVENTS); // may be null

        //
        setTitle("Editing "+fieldName);
        setResult(RESULT_CODE_CANCELED);
        ((TextView) findViewById(R.id.reminder_title)).setText(remTitle);

        // set the fragment designated to fieldName
        // the fragment must implement interface EditResultHolder
        switch (fieldName) {
            case "tags": {
                if (initAllTags == null) {
                    throw new RuntimeException("'initAllTags' should be given");
                }
                EditRemTagsFragment fragment = EditRemTagsFragment.newInstance(initData, initAllTags);
                setFragment(fragment);
                break;
            }
            case "behavior": {
                if (initAllSits == null || initAllEvents == null) {
                    throw new RuntimeException("initAllSits & initAllEvents should be given");
                }
                EditRemBehaviorFragment fragment =
                        EditRemBehaviorFragment.newInstance(initData, initAllSits, initAllEvents);
                setFragment(fragment);
            }
        }
    }

    void setFragment(Fragment fragment) {
        try {
            editResultHolder = (EditResultHolder) fragment;
        } catch (ClassCastException e) {
            throw new RuntimeException(fragment.toString()+" should implement EditResultHolder");
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intentResult;
        switch (item.getItemId()) {
            case R.id.action_done:
                if (editResultHolder.validateData()) {
                    intentResult = editResultHolder.getResult();
                    setResult(RESULT_CODE_OK, intentResult);
                    finish();
                }
                return true;

            case R.id.action_cancel:
                setResult(RESULT_CODE_CANCELED);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
