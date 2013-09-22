package linder.easypass.what;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.google.gson.Gson;
import linder.easypass.EasyPassApplication;
import linder.easypass.R;

/**
 * User: lucy
 * Date: 9/21/13
 * Version: 0.1
 */
public class ShowAccountActivity extends Activity implements CompoundButton
        .OnCheckedChangeListener, View.OnClickListener {

    public static final String EXTRA_ACCOUNT_KEY = "account";

    private TextView nameView, pseudoView, emailView, notesView, passView;
    private Account account;
    final PasswordTransformationMethod transform = new PasswordTransformationMethod();

    private int resultType;


    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.account_details_layout );
        nameView = ( TextView ) findViewById( R.id.details_name );
        pseudoView = ( TextView ) findViewById( R.id.details_pseudo );
        emailView = ( TextView ) findViewById( R.id.details_email );
        notesView = ( TextView ) findViewById( R.id.details_notes );
        passView = ( TextView ) findViewById( R.id.details_password );

        findViewById( R.id.details_edit_button ).setOnClickListener( this );
        findViewById( R.id.details_back_button ).setOnClickListener( this );

        if( Build.VERSION.SDK_INT >= 11 ) {
            // Moving this call into a helper class avoids crashes on DalvikVM in
            // API4, which will crash if a class contains a call to an unknown method,
            // even if it is never called.
            Api11Helper.setDisplayHomeAsUpEnabled( this, true );
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();

        if( extras == null ) {
            Log.e( EasyPassApplication.TAG, "show details activity : missing account extra" );
            finish();
        } else {
            // Get data via the key
            String serialisedAccount = extras.getString( Intent.EXTRA_TEXT );
            if( serialisedAccount != null ) {
                try {
                    account = new Gson().fromJson( serialisedAccount, Account.class );
                } catch( Exception e ) {
                    Log.e( this.getLocalClassName(), "could not deserialize account extra" );
                    finish();
                }
            }
        }
        setTitle( account.getNameOrDefault() );

        // sets the values
        nameView.setText( account.getNameOrDefault() );
        pseudoView.setText( account.getPseudoOrDefault() );
        emailView.setText( account.getEmailOrDefault() );
        notesView.setText( account.getNotesOrDefault() );
        passView.setText( account.getPasswordOrDefault() );

        // password : hide it by default
        final String hiddenPass = account.getPassword() == null ? "" : "***";
        passView.setText( hiddenPass );

        // adds a listener for the "show password" checkbox
        ( ( CheckBox ) findViewById( R.id.details_show_password ) ).
                setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                        passView.setText( isChecked ? account.getPasswordOrDefault() : hiddenPass );
                    }
                } );

    }


    @Override
    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
        passView.setTransformationMethod( isChecked ? null : transform );
    }


    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        if( item.getItemId() == android.R.id.home ) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }


    @Override
    public void onClick( View v ) {
        switch( v.getId() ) {
            case R.id.details_edit_button:
                resultType = Activity.RESULT_FIRST_USER;
                finish();
            case R.id.details_back_button:
                resultType = Activity.RESULT_OK;
                finish();
        }
    }


    @Override
    public void finish() {
        Intent data = new Intent();
        if( resultType == RESULT_FIRST_USER ) { // the edit button was pressed,
        // asks the details fragment to launch edit activity
            data.putExtra( EXTRA_ACCOUNT_KEY, new Gson().toJson( account ) );
        }
        setResult( resultType, data );
        super.finish();
    }


    private static class Api11Helper {
        @TargetApi( 11 )
        public static void setDisplayHomeAsUpEnabled( Activity activity, boolean value ) {
            activity.getActionBar().setDisplayHomeAsUpEnabled( true );
        }
    }
}//end class
