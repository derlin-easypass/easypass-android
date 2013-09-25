package linder.easypass.account_details;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.google.gson.Gson;
import linder.easypass.EasyPassApplication;
import linder.easypass.R;
import linder.easypass.models.Account;

/**
 * User: lucy
 * Date: 9/22/13
 * Version: 0.1
 */
public class AccountActivity extends FragmentActivity implements View.OnClickListener {

    public static final int EDIT_REQUEST_CODE = 9;
    public static final int SHOW_REQUEST_CODE = 8;
    public static final int NEW_REQUEST_CODE = 7;

    public static final String EXTRA_ACCOUNT_KEY = "account";
    public static final String EXTRA_ORIGINAL_ACCOUNT_NAME_kEY = "original_account_name";
    public static final String EXTRA_REQUEST_CODE_KEY = "request_code";


    Account account;
    private boolean isEditFragmentShowing;
    private int resultType = RESULT_CANCELED;
    private InputMethodManager inputMethodManager;


    interface AccountDetailsFragment {
        public void setWeakReference( Account account );

        public void updateFields();
    }

    private static AccountDetailsFragment editFragment, showFragment;
    private String originalAccountName;
    private static boolean editOnly;


    @TargetApi( 11 )
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        overrideTransition();
        setContentView( R.layout.activity_account_details );

        if( Build.VERSION.SDK_INT >= 11 ) {
            // Moving this call into a helper class avoids crashes on DalvikVM in
            // API4, which will crash if a class contains a call to an unknown method,
            // even if it is never called.
            Api11Helper.setDisplayHomeAsUpEnabled( this, true );
        }

        // for hiding keyboard
        inputMethodManager = ( InputMethodManager ) getSystemService( Activity
                .INPUT_METHOD_SERVICE );

        if( savedInstanceState == null ) {
            int requestCode = getIntent().getIntExtra( EXTRA_REQUEST_CODE_KEY, -1 );
            String serialisedAccount = getIntent().getStringExtra( Intent.EXTRA_TEXT );

            if( requestCode == -1 ) {
                Log.e( EasyPassApplication.TAG, "AccountActivity, requestCode invalid" );
                return;
            }

            if( serialisedAccount == null ) {
                account = new Account();
            } else {
                try {
                    account = new Gson().fromJson( serialisedAccount, Account.class );
                    originalAccountName = account.getNameOrDefault();
                } catch( Exception e ) {
                    Log.e( this.getLocalClassName(), "could not deserialize account extra" );
                    finish();
                }
            }

            //TODO : cancel new account
            if(requestCode == SHOW_REQUEST_CODE){
                editOnly = false;
                showShowFragment( false );
            }else if( requestCode == NEW_REQUEST_CODE  ) { //show
                editOnly = false;
                showEditFragment( false );
            }else if( requestCode == EDIT_REQUEST_CODE ) {
                editOnly = true;
                showEditFragment( false );
            } else {
                Log.e( EasyPassApplication.TAG, "AccountActivity, requestCode invalid" );
                return;
            }

        }
    }


    private void showShowFragment( boolean animate ) {
        if( showFragment == null ) {
            showFragment = new ShowAccountFragment();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if( animate ) {
            transaction.setCustomAnimations( android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right );
        }
        transaction.replace( R.id.fragment_holder, ( Fragment ) showFragment ).commit();

        //        showFragment.setWeakReference( account );
        showFragment.setWeakReference( account );
        showFragment.updateFields();
        isEditFragmentShowing = false;

    }//end showShowFragment


    private void showEditFragment( boolean animate ) {
        if( editFragment == null ) {
            editFragment = new EditAccountFragment();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if( animate ) {
            transaction.setCustomAnimations( R.anim.slide_in_right, R.anim.slide_out_left );
        }
        transaction.replace( R.id.fragment_holder, ( Fragment ) editFragment ).commit();

        editFragment.setWeakReference( account );
        editFragment.updateFields();
        isEditFragmentShowing = true;
    }//end showEditFragment


    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        if( item.getItemId() == android.R.id.home ) {
            if( isEditFragmentShowing && editOnly ) {
                showShowFragment( true );
            } else {
                onBackPressed();
            }
            return true;
        }
        return super.onOptionsItemSelected( item );
    }


    private void overrideTransition() {
        overridePendingTransition( R.anim.fade_in, R.anim.fade_out );
    }//end overrideTransition


    private void hideSoftKeyboard() {
        ( ( InputMethodManager ) getSystemService( Activity.INPUT_METHOD_SERVICE ) )
                .toggleSoftInput( InputMethodManager.SHOW_IMPLICIT, 0 );

    }


    private static class Api11Helper {
        @TargetApi( 11 )
        public static void setDisplayHomeAsUpEnabled( Activity activity, boolean value ) {
            activity.getActionBar().setDisplayHomeAsUpEnabled( true );
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overrideTransition();
    }


    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra( EXTRA_ACCOUNT_KEY, new Gson().toJson( account ) );
        data.putExtra( EXTRA_ORIGINAL_ACCOUNT_NAME_kEY, originalAccountName );
        setResult( account.getName() == null ? RESULT_CANCELED : resultType, data );

        //hideSoftKeyboard();
        //inputMethodManager.toggleSoftInput( InputMethodManager.HIDE_IMPLICIT_ONLY, 0 );

        super.finish();
        overrideTransition();
    }


    @Override
    public void onClick( View v ) {
        switch( v.getId() ) {
            case R.id.show_details_edit_button:
                showEditFragment( true );
                break;
            case R.id.show_details_back_button:
                finish();
                break;

            case R.id.edit_details_save_button:
                resultType = RESULT_OK;
                if( editOnly ) {
                    finish();
                } else {
                    showShowFragment( true );
                }
                break;
            case R.id.edit_details_cancel_button:
                //TODO : what if new mode and pressed cancel
                if( editOnly || account.getName() == null ) {
                    finish();
                } else {
//                    hideSoftKeyboard();
                    showShowFragment( true );
                }
        }
    }

}//end class
