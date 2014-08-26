package linder.easypass.account_details;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import linder.easypass.R;
import linder.easypass.models.Account;

import java.lang.ref.WeakReference;

import static linder.easypass.account_details.AccountActivity.AccountDetailsFragment;

/**
 * User: lucy
 * Date: 9/21/13
 * Version: 0.1
 */
public class ShowAccountFragment extends Fragment implements CompoundButton
        .OnCheckedChangeListener, AccountDetailsFragment {

    private TextView nameView, pseudoView, emailView, notesView, passView, metadataView;
    private CheckBox checkBox;
    final PasswordTransformationMethod transform = new PasswordTransformationMethod();
    private WeakReference<Account> accountRef;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        View view = inflater.inflate( R.layout.fragment_show_account_details, null );
        nameView = ( TextView ) view.findViewById( R.id.details_name );
        pseudoView = ( TextView ) view.findViewById( R.id.details_pseudo );
        emailView = ( TextView ) view.findViewById( R.id.details_email );
        notesView = ( TextView ) view.findViewById( R.id.details_notes );
        passView = ( TextView ) view.findViewById( R.id.details_password );
        metadataView = ( TextView ) view.findViewById( R.id.details_metadata );
        checkBox = ( CheckBox ) view.findViewById( R.id.details_show_password );
        view.findViewById( R.id.show_details_edit_button ).setOnClickListener( ( View
                .OnClickListener ) getActivity() );
        view.findViewById( R.id.show_details_back_button ).setOnClickListener( ( View
                .OnClickListener ) getActivity() );

        return view;
    }


    @Override
    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
        passView.setTransformationMethod( isChecked ? null : transform );
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
        if( nameView == null || getActivity() == null || accountRef == null ) return;
        Account account = accountRef.get();
        if( account == null ) return;

        getActivity().setTitle( account.getNameOrDefault() );

        // sets the values
        nameView.setText( account.getNameOrDefault() );
        pseudoView.setText( account.getPseudoOrDefault() );
        emailView.setText( account.getEmailOrDefault() );
        notesView.setText( account.getNotesOrDefault() );
        passView.setText( account.getPasswordOrDefault() );
        metadataView.setText( metaText(account) );

        // password : hide it by default
        final String pass = account.getPasswordOrDefault();
        final String hiddenPass = pass.isEmpty() ? "" : "***";
        passView.setText( hiddenPass );

        // adds a listener for the "show password" checkbox
        checkBox.
                setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                        passView.setText( isChecked ? pass : hiddenPass );
                    }
                } );
    }


    private String metaText( Account account ){
        return String.format( "Created [%s]\nLast updated [%s]", account.getCreatDate(), account.getModifDate() );
    }
}//end class
