package linder.easypass;

import android.content.Context;
import com.dropbox.sync.android.DbxAccountManager;

public final class NotesAppConfig {
    private NotesAppConfig() {
    }


    public static final String appKey = "wb46jdsrtgwsi2h";
    public static final String appSecret = "03l4njmsjyhqoto";


    public static DbxAccountManager getAccountManager( Context context ) {
        return DbxAccountManager.getInstance( context.getApplicationContext(), appKey, appSecret );
    }
}
