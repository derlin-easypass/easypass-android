package linder.easypass.models;

import linder.easypass.models.DataWrapper;

/**
 * User: lucy
 * Date: 14/09/13
 * Version: 0.1
 */
public class Account {

    private Object[] account;

    private final static int NAME = 0;
    private final static int PSEUDO = 1;
    private final static int EMAIL = 2;
    private final static int PASSWORD = 3;
    private final static int NOTES = 4;


    public Account( String pseudo, String email, String name, String notes, String password ) {
        account = new Object[ 5 ];
        account[ 0 ] = name;
        account[ 1 ] = pseudo;
        account[ 2 ] = email;
        account[ 3 ] = password;
        account[ 4 ] = notes;
    }//end constructor


    public Account( Object[] obj ) {
        account = obj;
    }//end constructor


    public Account() {
       account = new Object[5];
    }


    public Object[] getRaw() {
        return account;
    }//end asArrayObject


    public boolean fieldsContains( String... patterns ) {

        for( String pattern : patterns ) {
            boolean found = false;
            pattern = pattern.toLowerCase();

            for( int i = 0; i < account.length; i++ ) {
                if( i == PASSWORD ) continue;
                if( ( ( String ) account[ i ] ).toLowerCase().contains( pattern ) ) found = true;
            }//end for

            if( !found ) return false;
        }//end for
        return true;
    }


    public String getEmail() {
        return ( String ) account[ EMAIL ];
    }


    public void setEmail( String email ) {
        account[ EMAIL ] = email;
    }


    public String getName() {
        return ( String ) account[ NAME ];
    }


    public void setName( String name ) {
        account[ NAME ] = name;
    }


    public String getNotes() {
        return ( String ) account[ NOTES ];
    }


    public void setNotes( String notes ) {
        account[ NOTES ] = notes;
    }


    public String getPassword() {
        return ( String ) account[ PASSWORD ];
    }


    public void setPassword( String password ) {
        account[ PASSWORD ] = password;
    }


    public String getPseudo() {
        return ( String ) account[ PSEUDO ];
    }


    public void setPseudo( String pseudo ) {
        account[ PSEUDO ] = pseudo;
    }


    public String getEmailOrDefault() {
        String ret = ( String ) account[ EMAIL ];
        return ret == null ? "" : ret;
    }


    public String getNameOrDefault() {
        String ret = ( String ) account[ NAME ];
        return ret == null ? "" : ret;
    }


    public String getNotesOrDefault() {
        String ret = ( String ) account[ NOTES ];
        return ret == null ? "" : ret;
    }


    public String getPasswordOrDefault() {
        String ret = ( String ) account[ PASSWORD ];
        return ret == null ? "" : ret;
    }


    public String getPseudoOrDefault() {
        String ret = ( String ) account[ PSEUDO ];
        return ret == null ? "" : ret;
    }

    @Override
    public boolean equals( Object o ) {
        if( !( o instanceof Account ) ) return false;

        Account other = ( Account ) o;
        for( int i = 0; i < DataWrapper.EP_HEADERS.length; i++ ) {

            if( account[ i ] == null ) {
                if( other.account != null ) return false;
                continue;
            }

            if( !( account[ i ] ).equals( other.account[ i ] ) ) return false;
        }//end for

        return true;
    }

}//end class
