package linder.easypass.what;

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
import linder.easypass.EasyPassApplication;
import linder.easypass.R;

import java.lang.ref.WeakReference;

import static linder.easypass.what.AccountActivity.AccountDetailsFragment;

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
        View view = inflater.inflate( R.layout.account_edit_details_layout, null );

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
        updateFields();
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
    public void updateFields() {
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
        account.setName( editName.getText().toString() );
        account.setPseudo( editPseudo.getText().toString() );
        account.setEmail( editEmail.getText().toString() );
        account.setPassword( editPass.getText().toString() );
        account.setNotes( editNotes.getText().toString() );
        ( ( View.OnClickListener ) getActivity() ).onClick( v );
    }
}//end class
