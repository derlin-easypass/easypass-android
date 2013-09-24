package linder.easypass.settings;

import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.widget.Toast;
import linder.easypass.EasyPassApplication;
import linder.easypass.R;

//import com.dropbox.chooser.android.DbxChooser;

/**
 * User: lucy
 * Date: 9/15/13
 * Version: 0.1
 */
public class SettingsActivity extends PreferenceActivity {


    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // enables the home button
        getActionBar().setDisplayHomeAsUpEnabled( true );
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction();
        getFragmentManager().beginTransaction().replace( android.R.id.content,
                new SettingsFragment() ).commit();
    }


    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case android.R.id.home:
                // manages the home button - simple back button
                onBackPressed();
                return true;
        }
        return false;
    }

    /* *****************************************************************
     * inner class : Fragment
     * ****************************************************************/

    /**
     * Fragment Screen for the miscellaneous preferences
     */
    public static class SettingsFragment extends PreferenceFragment implements Preference
            .OnPreferenceClickListener {

        private Preference clearCachePref;


        @Override
        public void onCreate( Bundle savedInstanceState ) {
            super.onCreate( savedInstanceState );
            addPreferencesFromResource( R.xml.settings );
            clearCachePref = findPreference( getString( R.string.clear_cache_pref ) );
            clearCachePref.setOnPreferenceClickListener( this );
        }


        @Override
        public boolean onPreferenceClick( Preference preference ) {
            if( preference == clearCachePref ) {
                SharedPreferences prefs = EasyPassApplication.getPrefs();

                SharedPreferences.Editor editor = prefs.edit();
                String pattern = EasyPassApplication.KEY_CACHED_PASSWD_PREFIX + ".+";

                for( String key :prefs.getAll().keySet() ) {
                    if( key.matches( pattern ) ) {
                        editor.remove( key );
                    }
                }
                editor.commit();
                Toast.makeText( getActivity(), "Cache cleared", Toast.LENGTH_LONG ).show();
                return true;
            }
            return false;
        }
    }// end fragment
}//end class
