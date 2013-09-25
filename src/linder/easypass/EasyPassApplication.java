package linder.easypass;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * User: lucy
 * Date: 9/12/13
 * Version: 0.1
 */
public class EasyPassApplication extends Application {

    public static String TAG = "EasyPass";
    public static final String EP_EXTENSION = "data_ser";
    public static String CRYPTO_ALGORITHM = "aes-128-cbc";

    public static final String KEY_CACHED_PASSWD_PREFIX = "cached_passwd_";

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


}//end class
