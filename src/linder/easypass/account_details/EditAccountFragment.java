package linder.easypass.account_details;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import linder.easypass.EasyPassApplication;
import linder.easypass.R;
import linder.easypass.models.Account;

import java.lang.ref.WeakReference;

import static linder.easypass.account_details.AccountActivity.AccountDetailsFragment;

/**
 * User: lucy
 * Date: 9/21/13
 * Version: 0.1
 */
public class EditAccountFragment extends Fragment implements CompoundButton
        .OnCheckedChangeListener, AccountDetailsFragment, View.OnClickListener {

    private EditText editName, editPseudo, editEmail, editNotes, editPass;
    final PasswordTransformationMethod transform = new PasswordTransformationMethod();

    private WeakReference<Account> accountRef;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // inflates and sets the dialog's content view
        View view = inflater.inflate( R.layout.fragment_edit_account_details, null );

        editName = ( EditText ) view.findViewById( R.id.details_name );
        editPseudo = ( EditText ) view.findViewById( R.id.details_pseudo );
        editEmail = ( EditText ) view.findViewById( R.id.details_email );
        editNotes = ( EditText ) view.findViewById( R.id.details_notes );
        editPass = ( EditText ) view.findViewById( R.id.details_password );
        ( ( CheckBox ) view.findViewById( R.id.details_show_password ) ).
                setOnCheckedChangeListener( this );

        view.findViewById( R.id.edit_details_cancel_button ).setOnClickListener( ( View
                .OnClickListener ) getActivity() );
        view.findViewById( R.id.edit_details_save_button ).setOnClickListener( this );

        return view;
    }


    @Override
    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
        editPass.setTransformationMethod( isChecked ? null : transform );
    }

    @Override
    public void setWeakReference( Account account ) {
        accountRef = new WeakReference<Account>( account );
    }


    @Override
    public void onResume(){
        super.onResume();
        updateFields();
    }


    public void updateFields() {
        // be sure that onCreate was called and that we have an account
        // to avoid null pointer exceptions
        if( editName == null || getActivity() == null || accountRef == null ) return;

        Account account = accountRef.get();
        if( account == null || getActivity() == null ) return;
        editName.setText( account.getNameOrDefault() );
        editPseudo.setText( account.getPseudoOrDefault() );
        editEmail.setText( account.getEmailOrDefault() );
        editNotes.setText( account.getNotesOrDefault() );
        editPass.setText( account.getPasswordOrDefault() );
        editPass.setTransformationMethod( transform );
        getActivity().setTitle( "Edit " + account.getNameOrDefault() );
    }


    @Override
    public void onClick( View v ) {
        Account account = accountRef.get();
        if( account == null ) {
            Log.e( EasyPassApplication.TAG, "account is null in editaccount fragment onclick" );
            return;
        }

        String name = editName.getText().toString();
        if( name == null || name.isEmpty()){
            Toast.makeText( getActivity(), "The account name cannot be empty",
                    Toast.LENGTH_LONG ).show();
            return;
        }
        account.setName( name );
        account.setPseudo( editPseudo.getText().toString() );
        account.setEmail( editEmail.getText().toString() );
        account.setPassword( editPass.getText().toString() );
        account.setNotes( editNotes.getText().toString() );
        ( ( View.OnClickListener ) getActivity() ).onClick( v );
    }
}//end class
