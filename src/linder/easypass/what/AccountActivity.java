package linder.easypass.what;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import com.google.gson.Gson;
import linder.easypass.EasyPassApplication;
import linder.easypass.R;

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
        setContentView( R.layout.activity_frame_layout );

        if( Build.VERSION.SDK_INT >= 11 ) {
            // Moving this call into a helper class avoids crashes on DalvikVM in
            // API4, which will crash if a class contains a call to an unknown method,
            // even if it is never called.
            Api11Helper.setDisplayHomeAsUpEnabled( this, true );
        }

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

            if( requestCode == NEW_REQUEST_CODE ) {
                showEditFragment();
            }else if( requestCode == SHOW_REQUEST_CODE ) { //show
                showShowFragment();
            } else if( requestCode == EDIT_REQUEST_CODE ) {
                editOnly = true;
                showEditFragment();
            } else {
                Log.e( EasyPassApplication.TAG, "AccountActivity, requestCode invalid" );
                return;
            }

        }
    }


    private void showShowFragment() {
        if( showFragment == null ) {
            showFragment = new ShowAccountFragment();
        }
        getSupportFragmentManager().beginTransaction().replace( R.id.fragment_holder, ( Fragment
                ) showFragment ).commit();

//        showFragment.setWeakReference( account );
        showFragment.setWeakReference( account );
        showFragment.updateFields();
        isEditFragmentShowing = false;

    }//end showShowFragment


    private void showEditFragment() {
        if( editFragment == null ) {
            editFragment = new EditAccountFragment();
        }
        getSupportFragmentManager().beginTransaction().replace( R.id.fragment_holder,
                ( Fragment ) editFragment ).commit();

        editFragment.setWeakReference( account );
        editFragment.updateFields();
        isEditFragmentShowing = true;
    }//end showEditFragment


    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        if( item.getItemId() == android.R.id.home ) {
            if( isEditFragmentShowing && editOnly ) {
                showShowFragment();
            } else {
                onBackPressed();
            }
            return true;
        }
        return super.onOptionsItemSelected( item );
    }


    private static class Api11Helper {
        @TargetApi( 11 )
        public static void setDisplayHomeAsUpEnabled( Activity activity, boolean value ) {
            activity.getActionBar().setDisplayHomeAsUpEnabled( true );
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra( EXTRA_ACCOUNT_KEY, new Gson().toJson( account ) );
        data.putExtra( EXTRA_ORIGINAL_ACCOUNT_NAME_kEY, originalAccountName );
//        data.putExtra( EXTRA_ACCOUNT_MODIFIED, true );
        setResult( resultType, data );
        super.finish();
    }


    @Override
    public void onClick( View v ) {
        switch( v.getId() ) {
            case R.id.show_details_edit_button:
                showEditFragment();
                break;
            case R.id.show_details_back_button:
                finish();
                break;

            case R.id.edit_details_save_button:
                resultType = RESULT_OK;
                if( editOnly ) {
                    finish();
                    return;
                }
                showShowFragment();
                break;
            case R.id.edit_details_cancel_button:
                if( editOnly ) finish();
                showShowFragment();
        }
    }

}//end class
