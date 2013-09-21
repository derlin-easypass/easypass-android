package linder.easypass;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * User: lucy
 * Date: 9/12/13
 * Version: 0.1
 */
public class EasyPassApplication extends Application {

    public static String TAG = "EasyPass";
    public static final String EP_EXTENSION = ".data_ser";
    public static String CRYPTO_ALGORITHM = "aes-128-cbc";

    public static final String KEY_CACHED_PASSWD_PREFIX = "cached_passwd_";
    public static final String KEY_OPENED_SESSION = "opened_session";


    private static Context context;
    private static SharedPreferences prefs;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        prefs = PreferenceManager
                .getDefaultSharedPreferences( context );
    }

    public static Context getContext() {
        return context;
    }


    public static SharedPreferences getPrefs() {
        return prefs;
    }

    /* *****************************************************************
     * other
     * ****************************************************************/


    public static boolean isNetworkAvaiable() {
        return ( ( ConnectivityManager ) context.getSystemService( Context.CONNECTIVITY_SERVICE )
        ).getActiveNetworkInfo() != null;
    }


    public static void showToast( String msg ) {
        Toast error = Toast.makeText( context, msg, Toast.LENGTH_LONG );
        error.show();
    }


    public static void logd( String msg ) {
        Log.d( TAG, msg );
    }//end logd


    public static void logd( String msg, Exception e ) {
        Log.d( TAG, msg, e );
    }//end logd


}//end class
