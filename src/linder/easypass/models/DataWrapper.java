package linder.easypass.models;

import java.util.*;

/**
 * User: lucy
 * Date: 14/09/13
 * Version: 0.1
 */
public class DataWrapper {
    private List<Account> data;
    private String sessionName, password;

    public static final String[] EP_HEADERS = new String[]{ "Account name", "Pseudo", "Email",
            "Password", "Notes" };

    public DataWrapper( List<Object[]> data, String sessionName, String password ) {
        this.sessionName = sessionName;
        this.password = password;
        if( data != null ) setData( data );

    }


    public void setData( Object o ) {
        List<Map<String,String>> data = ( List<Map<String,String>> ) o;
        this.data = new ArrayList<Account>();
        for( Map<String,String> obj : data ) {
            this.data.add( new Account( obj ) );
        }//end for
    }


    public List<String> getAccountNames() {
        List<String> names = new ArrayList<String>();
        if( data == null ) return names;

        for( Account session : data ) {
            names.add( session.getNameOrDefault() );
        }//end for
        Collections.sort( names );
        return names;
    }//end getAccountNames


    public Account getAccount( String name ) {
        for( Account account : data ) {
            if( account.getNameOrDefault().equals( name ) ) return account;
        }//end for
        return null;
    }//end getAccount


    public boolean replaceAccount( Account oldAccount, Account newAccount ) {
        if( oldAccount.equals( newAccount ) ) return false;
        // update meta-data
        newAccount.setCreatDate( oldAccount.getCreatDate() );
        newAccount.updateModifDate();

        data.remove( oldAccount );
        data.add( newAccount );
        return true;
    }


    public List<Map<String,String>> getRawData() {
        List<Map<String,String>> array = new ArrayList<Map<String, String>>(  );

        for( Account account : data ) {
            array.add( account.getRaw() );
        }//end for

        return array;
    }//end getArrayOfObjects


    public List<Account> getData() {
        return data;
    }


    public String getPassword() {
        return password;
    }


    public String getSessionName() {
        return sessionName;
    }


    public void removeAccount( String accountName ) {
        Account account = getAccount( accountName );
        if( account != null ) data.remove( account );
    }


    public void addAccount( Account account ) {
        data.add( account );
    }
}//end class
