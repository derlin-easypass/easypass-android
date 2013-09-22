package linder.easypass;

import android.content.ClipData;
import android.content.Context;

public class Util {
    private static final int sdk = android.os.Build.VERSION.SDK_INT;

    public static String stripExtension( String extension, String filename ) {
        extension = "." + extension;
        if( filename.endsWith( extension ) ) {
            return filename.substring( 0, filename.length() - extension.length() );
        }
        return filename;
    }

    public static void copyToClipBoard( Context context, String label, String copyText ) {
        if( sdk < android.os.Build.VERSION_CODES.HONEYCOMB ) {
            android.text.ClipboardManager clipboard = ( android.text.ClipboardManager )
                    context.getSystemService( Context.CLIPBOARD_SERVICE );
            clipboard.setText( copyText );
        } else {
            android.content.ClipboardManager clipboard = ( android.content.ClipboardManager )
                    context.getSystemService( Context.CLIPBOARD_SERVICE );
            clipboard.setPrimaryClip( ClipData.newPlainText( label, copyText ) );
        }
    }
}
