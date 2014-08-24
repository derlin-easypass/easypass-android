package linder.easypass.models;

import java.util.HashMap;
import java.util.Map;

/**
 * User: lucy
 * Date: 14/09/13
 * Version: 0.1
 */
public class Account {

    private Map<String,String> account;

    private final static String NAME = "name";
    private final static String PSEUDO = "pseudo";
    private final static String EMAIL = "email";
    private final static String PASSWORD = "password";
    private final static String NOTES = "notes";
    private final static String CREAT_DATE = "creation date";
    private final static String MODIF_DATE = "modification date";

    private static  String[] COMPARABLE_HEADERS = new String[]{NAME, PSEUDO, EMAIL, PASSWORD, NOTES};


    public Account( String pseudo, String email, String name, String notes, String password ) {
        account = new HashMap<String, String>(  );
        account.put( NAME, name);
        account.put( PSEUDO, pseudo );
        account.put( EMAIL, email );
        account.put( PASSWORD, password );
        account.put( NOTES, notes );
    }//end constructor


    public Account( Map<String,String> obj ) {
        account = obj;
    }//end constructor


    public Account() {
       this("", "", "", "", "");
    }


    public Map<String,String> getRaw() {
        return account;
    }//end asArrayObject


    public boolean fieldsContains( String... patterns ) {

        for( String pattern : patterns ) {
            boolean found = false;
            pattern = pattern.toLowerCase();

            for( String i : account.keySet() ){
                if( PASSWORD.equals( i ) ) continue;
                if( account.get( i ).toLowerCase().contains( pattern ) ) found = true;
            }//end for

            if( !found ) return false;
        }//end for
        return true;
    }


    public String getEmail() {
        return account.get( EMAIL );
    }


    public void setEmail( String email ) {
        account.put(  EMAIL, email);
    }


    public String getName() {
        return account.get( NAME );
    }


    public void setName( String name ) {
        account.put(  NAME, name);
    }


    public String getNotes() {
        return account.get( NOTES );
    }


    public void setNotes( String notes ) {
        account.put(  NOTES , notes);
    }


    public String getPassword() {
        return account.get( PASSWORD );
    }


    public void setPassword( String password ) {
        account.put( PASSWORD, password);
    }


    public String getPseudo() {
        return account.get( PSEUDO );
    }


    public void setPseudo( String pseudo ) {
        account.put(  PSEUDO, pseudo);
    }


    public String getEmailOrDefault() {
        String ret = getEmail();
        return ret == null ? "" : ret;
    }


    public String getNameOrDefault() {
        String ret = getName();
        return ret == null ? "" : ret;
    }


    public String getNotesOrDefault() {
        String ret = getNotes();
        return ret == null ? "" : ret;
    }


    public String getPasswordOrDefault() {
        String ret = getPassword();
        return ret == null ? "" : ret;
    }


    public String getPseudoOrDefault() {
        String ret = getPseudo();
        return ret == null ? "" : ret;
    }

    @Override
    public boolean equals( Object o ) {
        if( !( o instanceof Account ) ) return false;

        Account other = ( Account ) o;
        for( String i : COMPARABLE_HEADERS){
            if( !account.get( i ).equals( other.account.get(i) )  ) {
                return false;
            }
        }//end for

        return true;
    }

}//end class
