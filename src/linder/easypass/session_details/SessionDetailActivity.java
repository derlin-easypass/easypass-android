package linder.easypass.session_details;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import com.dropbox.sync.android.DbxPath;
import linder.easypass.R;
import linder.easypass.session_list.SessionsListActivity;

public class SessionDetailActivity extends FragmentActivity {

    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_PASS = "pass";


    @TargetApi( 11 )
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_session_detail );

        if( Build.VERSION.SDK_INT >= 11 ) {
            // Moving this call into a helper class avoids crashes on DalvikVM in
            // API4, which will crash if a class contains a call to an unknown method,
            // even if it is never called.
            Api11Helper.setDisplayHomeAsUpEnabled( this, true );
        }

        if( savedInstanceState == null ) {
            String path = getIntent().getStringExtra( EXTRA_PATH );
            String pass = getIntent().getStringExtra( EXTRA_PASS );
            SessionDetailFragment fragment = SessionDetailFragment.getInstance( new DbxPath( path
            ), pass );
            getSupportFragmentManager().beginTransaction().add( R.id.note_detail_container,
                    fragment ).commit();
        }
    }


    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        if( item.getItemId() == android.R.id.home ) {
            NavUtils.navigateUpTo( this, new Intent( this, SessionsListActivity.class ) );
            return true;
        }

        return super.onOptionsItemSelected( item );
    }


    private static class Api11Helper {
        @TargetApi( 11 )
        public static void setDisplayHomeAsUpEnabled( SessionDetailActivity activity, boolean value ) {
            activity.getActionBar().setDisplayHomeAsUpEnabled( true );
        }
    }
}
