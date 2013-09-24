package linder.easypass.session_list;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.widget.EditText;
import com.dropbox.sync.android.DbxPath;
import linder.easypass.session_details.SessionDetailActivity;
import linder.easypass.session_details.SessionDetailFragment;
import linder.easypass.R;

import static linder.easypass.EasyPassApplication.KEY_CACHED_PASSWD_PREFIX;
import static linder.easypass.EasyPassApplication.getPrefs;

public class SessionsListActivity extends FragmentActivity implements SessionsListFragment.Callbacks {

    private boolean mTwoPane;
    private boolean arePassCached;


    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_sessions_list );

        if( findViewById( R.id.note_detail_container ) != null ) {
            mTwoPane = true;
            ( ( SessionsListFragment ) getSupportFragmentManager().findFragmentById( R.id.note_list )
            ).setActivateOnItemClick( true );
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        arePassCached = getPrefs().getBoolean( getString( R.string.pref_key_password_caching ),
                false );
    }


    @Override
    public void onItemSelected( DbxPath path ) {
        if( arePassCached ) {
            String pass = getPrefs().getString( KEY_CACHED_PASSWD_PREFIX + path.getName(), null );
            if( pass != null ) {
                switchToDetailsActivity( path, pass );
                return;
            }
        }
        showPasswordDialog( path );
    }


    private void showPasswordDialog( final DbxPath path ) {
        // creates and show a passwod dialog
        final EditText input = new EditText( SessionsListActivity.this );
        input.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD );

        new AlertDialog.Builder( SessionsListActivity.this ).setTitle( "Password" ).setView( input )
                .setPositiveButton( "OK", new DialogInterface.OnClickListener() {

                    public void onClick( DialogInterface dialog, int whichButton ) {
                        String pass = input.getText().toString();
                        if( pass.length() > 3 ) {

                            if( arePassCached ) { // stores the passwd
                                getPrefs().edit().putString( KEY_CACHED_PASSWD_PREFIX + path
                                        .getName(), pass ).commit();
                            }

                            switchToDetailsActivity( path, pass );

                        }
                    }
                } ).setNegativeButton( "Cancel", new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int whichButton ) {
                dialog.dismiss();
            }
        } ).create().show();

    }//end showPasswordDialog


    private void switchToDetailsActivity( DbxPath path, String password ) {
        if( mTwoPane ) {
            SessionDetailFragment fragment = SessionDetailFragment.getInstance( path, password );
            getSupportFragmentManager().beginTransaction().replace( R.id.note_detail_container,
                    fragment ).commit();

        } else {
            Intent detailIntent = new Intent( this, SessionDetailActivity.class );
            detailIntent.putExtra( SessionDetailActivity.EXTRA_PATH, path.toString() );
            detailIntent.putExtra( SessionDetailActivity.EXTRA_PASS, password );
            startActivity( detailIntent );
        }
    }//end switchToDetailsActivity
}
