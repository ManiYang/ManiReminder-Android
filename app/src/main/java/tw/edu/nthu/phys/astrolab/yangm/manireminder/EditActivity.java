package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class EditActivity extends AppCompatActivity {

    public static final String EXTRA_FIELD_NAME = "field_name";
    public static final String EXTRA_INIT_DATA = "init_data";
    public static final String EXTRA_NEW_DATA = "new_data";
    public static final int RESULT_CODE_OK = 1;
    public static final int RESULT_CODE_CANCELED = 0;
    private String fieldName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        Intent intent = getIntent();
        fieldName = intent.getStringExtra(EXTRA_FIELD_NAME);
        String initData = intent.getStringExtra(EXTRA_INIT_DATA);
        ((TextView) findViewById(R.id.field_name)).setText(fieldName);
        ((TextView) findViewById(R.id.init_data)).setText(initData);

        setResult(RESULT_CODE_CANCELED);
    }

    public void onButtonClick(View v) {
        if (v.getId() == R.id.button_done) {
            Intent newData = new Intent()
                    .putExtra(EXTRA_FIELD_NAME, fieldName)
                    .putExtra(EXTRA_NEW_DATA, "new data...");
            setResult(RESULT_CODE_OK, newData);
        }
        finish();
    }
}
