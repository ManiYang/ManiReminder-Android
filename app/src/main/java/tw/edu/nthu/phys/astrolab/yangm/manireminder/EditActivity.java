package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class EditActivity extends AppCompatActivity {

    public static final String EXTRA_FIELD_NAME = "field_name";
    public static final String EXTRA_INIT_DATA = "init_data";
    public static final String EXTRA_NEW_DATA = "new_data";
    public static final String EXTRA_INIT_ALL_TAGS = "init_all_tags";
    public static final String EXTRA_NEW_ALL_TAGS = "new_all_tags";
    public static final int RESULT_CODE_OK = 1;
    public static final int RESULT_CODE_CANCELED = 0;
    private String fieldName;
    EditResultHolder editResultHolder;

    public interface EditResultHolder {
        Intent getResult();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // get data from intent
        Intent intent = getIntent();
        fieldName = intent.getStringExtra(EXTRA_FIELD_NAME);
        String initData = intent.getStringExtra(EXTRA_INIT_DATA);
        String initAllTags = intent.getStringExtra(EXTRA_INIT_ALL_TAGS); // may be null

        //
        setTitle("Editing "+fieldName);
        setResult(RESULT_CODE_CANCELED);

        // set the fragment designated to fieldName
        // the fragment must implement interface EditResultHolder
        switch (fieldName) {
            case "tags":
                if (initAllTags == null) {
                    throw new RuntimeException("'initAllTags' should be given");
                }
                EditRemTagsFragment fragment = EditRemTagsFragment.newInstance(initData, initAllTags);
                setFragment(fragment);
                break;
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
                intentResult = editResultHolder.getResult();
                setResult(RESULT_CODE_OK, intentResult);
                finish();
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
