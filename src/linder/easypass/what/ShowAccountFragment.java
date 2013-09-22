package linder.easypass.what;

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

import java.lang.ref.WeakReference;

import static linder.easypass.what.AccountActivity.AccountDetailsFragment;

/**
 * User: lucy
 * Date: 9/21/13
 * Version: 0.1
 */
public class ShowAccountFragment extends Fragment implements CompoundButton
        .OnCheckedChangeListener, AccountDetailsFragment {

    private TextView nameView, pseudoView, emailView, notesView, passView;
    private CheckBox checkBox;
    final PasswordTransformationMethod transform = new PasswordTransformationMethod();
    private WeakReference<Account> accountRef;


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        View view = inflater.inflate( R.layout.account_details_layout, null );
        nameView = ( TextView ) view.findViewById( R.id.details_name );
        pseudoView = ( TextView ) view.findViewById( R.id.details_pseudo );
        emailView = ( TextView ) view.findViewById( R.id.details_email );
        notesView = ( TextView ) view.findViewById( R.id.details_notes );
        passView = ( TextView ) view.findViewById( R.id.details_password );
        checkBox = ( CheckBox ) view.findViewById( R.id.details_show_password );
        view.findViewById( R.id.show_details_edit_button ).setOnClickListener( ( View.OnClickListener ) getActivity() );
        view.findViewById( R.id.show_details_back_button ).setOnClickListener( ( View.OnClickListener ) getActivity() );
        updateFields();
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
    public void updateFields() {
        if(accountRef == null) return;
        Account account = accountRef.get();
        if(account == null || getActivity() == null) return;

        getActivity().setTitle( account.getNameOrDefault() );

        // sets the values
        nameView.setText( account.getNameOrDefault() );
        pseudoView.setText( account.getPseudoOrDefault() );
        emailView.setText( account.getEmailOrDefault() );
        notesView.setText( account.getNotesOrDefault() );
        passView.setText( account.getPasswordOrDefault() );

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
}//end class
