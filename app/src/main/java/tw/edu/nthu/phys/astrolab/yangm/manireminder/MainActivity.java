package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static boolean firstStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));
        ((TabLayout) findViewById(R.id.tabs)).setupWithViewPager(pager);

        //
        if (firstStart) {
            createNotificationChannel();
            new ReminderBoardLogic(this).onAppStart();
        }
        firstStart = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("MainActivity", "### resume");
    }

    // option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_all_reminders:
                startActivity(new Intent(this, ListActivity.class));
                return true;
            case R.id.action_view_history:
                startActivity(new Intent(this, HistoryViewActivity.class));
                return true;
            case R.id.action_fresh_restart:
                new AlertDialog.Builder(this)
                        .setTitle("Fresh Restart")
                        .setMessage("Please confirm.")
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new ReminderBoardLogic(MainActivity.this).freshRestart();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //
    private class MainPagerAdapter extends FragmentPagerAdapter {
        MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new BoardFragment();
                case 1:
                    return new SituationsEventsFragment();
                default:
                    return null;
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.board);
                case 1:
                    return getResources().getString(R.string.situations_and_events);
                default:
                    return null;
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    getResources().getString(R.string.channel_id),
                    getResources().getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getResources().getString(R.string.channel_dsecription));
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
