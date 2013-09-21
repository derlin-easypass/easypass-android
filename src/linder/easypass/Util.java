package linder.easypass;

public class Util {

    public static String stripExtension( String extension, String filename ) {
        extension = "." + extension;
        if( filename.endsWith( extension ) ) {
            return filename.substring( 0, filename.length() - extension.length() );
        }
        return filename;
    }
}
