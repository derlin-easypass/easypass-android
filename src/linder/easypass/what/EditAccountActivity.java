package linder.easypass.what;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import com.google.gson.Gson;
import linder.easypass.R;

import static linder.easypass.EasyPassApplication.logd;

/**
 * User: lucy
 * Date: 9/21/13
 * Version: 0.1
 */
public class EditAccountActivity extends Activity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    public static final String EXTRA_ACCOUNT_KEY = "account";
    public static final String EXTRA_ORIGINAL_ACCOUNT_NAME = "original_account_name";

    private EditText editName, editPseudo, editEmail, editNotes, editPass;
    private Account account;
    private String originalAccountName;
    final PasswordTransformationMethod transform = new PasswordTransformationMethod();

    private int resultType;


    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.account_edit_details_layout );

        editName = ( EditText ) findViewById( R.id.details_name );
        logd( "editdialog : account = " + account );

        editPseudo = ( EditText ) findViewById( R.id.details_pseudo );
        editEmail = ( EditText ) findViewById( R.id.details_email );
        editNotes = ( EditText ) findViewById( R.id.details_notes );
        editPass = ( EditText ) findViewById( R.id.details_password );
        ( ( CheckBox ) findViewById( R.id.details_show_password ) ).
                setOnCheckedChangeListener( this );

        findViewById( R.id.edit_details_cancel_button ).setOnClickListener( this );
        findViewById( R.id.edit_details_save_button ).setOnClickListener( this );


    }


    @Override
    protected void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();

        if( extras == null ) {
            account = new Account();
        } else {
            // Get data via the key
            String serialisedAccount = extras.getString( Intent.EXTRA_TEXT );
            if( serialisedAccount != null ) {
                try {
                    account = new Gson().fromJson( serialisedAccount, Account.class );
                    originalAccountName = account.getNameOrDefault();
                } catch( Exception e ) {
                    Log.e( this.getLocalClassName(), "could not deserialize account extra" );
                    finish();
                }
            }
        }
        editName.setText( account.getNameOrDefault() );
        editPseudo.setText( account.getPseudoOrDefault() );
        editEmail.setText( account.getEmailOrDefault() );
        editNotes.setText( account.getNotesOrDefault() );
        editPass.setText( account.getPasswordOrDefault() );
        editPass.setTransformationMethod( transform );

        setTitle( "Edit " + originalAccountName );
    }


    @Override
    public void onClick( View v ) {

        switch( v.getId() ) {
            case R.id.edit_details_cancel_button:
                resultType = RESULT_CANCELED;
                break;

            case R.id.edit_details_save_button:

                account.setName( editName.getText().toString() );
                account.setPseudo( editPseudo.getText().toString() );
                account.setEmail( editEmail.getText().toString() );
                account.setPassword( editPass.getText().toString() );
                account.setNotes( editNotes.getText().toString() );

                resultType = RESULT_OK;
                break;
        }

        finish();
    }


    @Override
    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
        editPass.setTransformationMethod( isChecked ? null : transform );
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        if( resultType == RESULT_OK)
        data.putExtra( EXTRA_ACCOUNT_KEY, new Gson().toJson( account ) );
        data.putExtra( EXTRA_ORIGINAL_ACCOUNT_NAME, originalAccountName );
        setResult( resultType, data );
        super.finish();
    }
}//end class
