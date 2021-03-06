package linder.easypass.session_details;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import com.dropbox.sync.android.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.woozzu.android.widget.IndexableListView;
import linder.easypass.R;
import linder.easypass.account_details.AccountActivity;
import linder.easypass.misc.TextWatcherAdapter;
import linder.easypass.misc.Util;
import linder.easypass.models.Account;
import linder.easypass.models.DataWrapper;
import linder.easypass.models.DboxConfig;
import linder.easypass.models.JsonManager;
import linder.easypass.settings.SettingsActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import static linder.easypass.EasyPassApplication.TAG;
import static linder.easypass.EasyPassApplication.showToast;
import static linder.easypass.account_details.AccountActivity.*;

public class SessionDetailFragment extends Fragment{

    // keys to the bundle's extras
    private static final String BUNDLE_ARG_FILE_PATH = "path";
    private static final String BUNDLE_ARG_PASSWD = "password";

    // algo for the deserialisation of data
    private static final String CRYPTO_ALGORITHM = "aes-128-cbc";
    // request codes for startActivityForResult calls
    private static final int ACCOUNT_ACTIVITY_REQUEST_CODE = 23;
    private static final int ACCOUNT_ACTIVITY_NEW_REQUEST_CODE = 20;

    // context menu items
    private static final int MENU_COPY_PASS = 0;
    private static final int MENU_COPY_PSEUDO = 1;
    private static final int MENU_SHOW_DETAILS = 2;
    private static final int MENU_EDIT = 3;
    private static final int MENU_DELETE = 4;


    private TextView errorMessageView;
    private View oldVersionWarningView;
    private View loadingSpinnerView;

    private final DbxLoadHandler handler = new DbxLoadHandler( this );

    private DbxFile sessionFile;
    private final Object sessionFileLock = new Object();
    // binary semaphore has the property (unlike many Lock implementations),
    // that the "lock" can be released by a thread other than the owner
    private final Semaphore mFileUseSemaphore = new Semaphore( 1 );
    private boolean userHasModifiedData = false;
    private boolean mHasLoadedAnyData = false;


    private EditText inputSearch;
    private IndexableListView mList;
    private IndexerAdapter adapter;
    private DataWrapper dataWrapper;
    private String mCurrentSessionName, mCurrentPassword;


    private final DbxFile.Listener mChangeListener = new DbxFile.Listener(){

        @Override
        public void onFileChange( DbxFile file ){
            // In case a notification is delivered late, make sure we're still
            // on-screen (sessionFile != null) and still working on the same file.
            synchronized( sessionFileLock ){
                if( file != sessionFile ){
                    return;
                }
            }

            if( userHasModifiedData ){
                // User has modified the text locally, so we no longer care
                // about external changes.
                return;
            }

            boolean currentIsLatest;
            boolean newerIsCached = false;
            try{
                currentIsLatest = file.getSyncStatus().isLatest;

                if( !currentIsLatest ){
                    newerIsCached = file.getNewerStatus().isCached;
                }
            }catch( DbxException e ){
                Log.w( TAG, "Failed to get sync status", e );
                return;
            }

            handler.sendIsShowingLatestMessage( currentIsLatest );

            // kick off an update if necessary
            if( newerIsCached || !mHasLoadedAnyData ){
                handler.sendDoUpdateMessage();
            }
        }
    };


    /* *****************************************************************
     * constructors
     * ****************************************************************/


    public SessionDetailFragment(){
    }


    public static SessionDetailFragment getInstance( DbxPath path, String password ){
        SessionDetailFragment fragment = new SessionDetailFragment();
        Bundle args = new Bundle();
        args.putString( BUNDLE_ARG_FILE_PATH, path.toString() );
        args.putString( BUNDLE_ARG_PASSWD, password );
        fragment.setArguments( args );
        return fragment;
    }

    /* *****************************************************************
     * oncreate
     * ****************************************************************/


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ){
        final View view = inflater.inflate( R.layout.fragment_session_detail, container, false );

        mList = ( IndexableListView ) view.findViewById( com.woozzu.android.indexablelistview.R.id.listview );
        dataWrapper = new DataWrapper( null, mCurrentSessionName, mCurrentPassword );
        adapter = new IndexerAdapter( getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>() );
        adapter.setDataWrapperReference( dataWrapper );
        mList.setAdapter( adapter );
        mList.setFastScrollEnabled( true );

        // show the context menu for the item on long click.
        // note: registerForContextMenu has the same effect as adding an mList.setOnItemLongClickListener with the
        // action:
        //        mList.showContextMenuForChild( view );
        //        return true;
        //
        registerForContextMenu( mList );

        // one click on an item opens the "detail" view of the account entry
        mList.setOnItemClickListener( new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick( AdapterView<?> parent, final View view, final int position, final long id ){
                String accountName = mList.getItemAtPosition( position ).toString();
                if( accountName == null ) return;
                Account account = dataWrapper.getAccount( accountName );
                startEditShowActivityForResult( account, true );
            }
        } );

        // adds a listener to filter the accounts on text change
        inputSearch = ( EditText ) view.findViewById( R.id.inputSearch );
        inputSearch.addTextChangedListener( new TextWatcherAdapter( inputSearch,
                new TextWatcherAdapter.TextWatcherListener(){

            @Override
            public void onTextChanged( EditText view, String text ){
                adapter.getFilter().filter( text );
            }
        } ) );

        oldVersionWarningView = view.findViewById( R.id.old_version );
        loadingSpinnerView = view.findViewById( R.id.note_loading );
        errorMessageView = ( TextView ) view.findViewById( R.id.error_message );

        setHasOptionsMenu( true );
        return view;
    }


    /* *****************************************************************
     * on activity result (from account details activity)
     * ****************************************************************/
    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ){
        Log.d( TAG, "On activity result." );
        // return from the show details/edit account
        if( requestCode == ACCOUNT_ACTIVITY_REQUEST_CODE ){ // show and/or edit existing account

            if( resultCode == Activity.RESULT_OK ){  // check for edition

                Bundle extras = data.getExtras();

                // updates the data with the possible modifications
                Account originalAccount = dataWrapper.getAccount( extras.getString( EXTRA_ORIGINAL_ACCOUNT_NAME_kEY ) );
                Account editedAccount = new Gson().fromJson( extras.getString( EXTRA_ACCOUNT_KEY ), Account.class );

                // if this returns false, it means the two accounts are identical --> nothing to do
                if( !dataWrapper.replaceAccount( originalAccount, editedAccount ) ) return;
                String oldName = originalAccount.getNameOrDefault();

                if( !originalAccount.getNameOrDefault().equals( // the account name was modified
                        editedAccount.getNameOrDefault() ) ){
                    // update the list adapter of this fragment
                    int position = adapter.getPosition( oldName );
                    adapter.remove( oldName );
                    adapter.insert( editedAccount.getNameOrDefault(), position );
                    adapter.notifyDataSetChanged();
                }
                // mark the data as modified
                userHasModifiedData = true;
            }
        }else if( requestCode == ACCOUNT_ACTIVITY_NEW_REQUEST_CODE && resultCode == RESULT_OK ){ // new account creation

            Account account = new Gson().fromJson( data.getStringExtra( EXTRA_ACCOUNT_KEY ), Account.class );
            if( account.getNameOrDefault().isEmpty() ) return; // no name = no account ^^
            dataWrapper.addAccount( account );
            userHasModifiedData = true;
            // clears and add to keep the items sorted (sorting made by the datawrapper)
            adapter.clear();
            adapter.addAll( dataWrapper.getAccountNames() );
            adapter.notifyDataSetChanged();

        }else{  // should not happen
            super.onActivityResult( requestCode, resultCode, data );
        }
    }


    /* *****************************************************************
     * on resume
     * ****************************************************************/


    @Override
    public void onResume(){
        super.onResume();

        //        userHasModifiedData = false;
        //mHasLoadedAnyData = false;

        DbxPath path = new DbxPath( getArguments().getString( BUNDLE_ARG_FILE_PATH ) );
        mCurrentPassword = getArguments().getString( BUNDLE_ARG_PASSWD );

        // Grab the session name from the path:
        String sessionName = Util.stripExtension( "data_ser", path.getName() );
        mCurrentSessionName = sessionName;
        getActivity().setTitle( sessionName );

        DbxAccount dbAccount = DboxConfig.getAccountManager( getActivity() ).getLinkedAccount();
        if( dbAccount == null ){
            Log.e( TAG, "No linked account." );
            return;
        }

        errorMessageView.setVisibility( View.GONE );
        loadingSpinnerView.setVisibility( View.VISIBLE );

        try{
            mFileUseSemaphore.acquire();
        }catch( InterruptedException e ){
            throw new RuntimeException( e );
        }

        try{
            DbxFileSystem fs = DbxFileSystem.forAccount( dbAccount );
            try{
                sessionFile = fs.open( path );
            }catch( DbxException.NotFound e ){
                //todo
                sessionFile = fs.create( path );
            }
        }catch( DbxException e ){
            Log.e( TAG, "failed to open or create file.", e );
            return;
        }

        sessionFile.addListener( mChangeListener );

        boolean latest;
        try{
            latest = sessionFile.getSyncStatus().isLatest;
        }catch( DbxException e ){
            Log.w( TAG, "Failed to get sync status", e );
            return;
        }


        handler.sendIsShowingLatestMessage( latest );
        handler.sendDoUpdateMessage();
    }


    /* *****************************************************************
     * on pause
     * ****************************************************************/


    @Override
    public void onPause(){
        super.onPause();

        synchronized( sessionFileLock ){
            sessionFile.removeListener( mChangeListener );

            // If the contents have changed, write them back to Dropbox
            if( userHasModifiedData && sessionFile != null ){
                // true : asks the thread to close the file after write
                startWriteLocalChangesThread( true );
            }else{
                sessionFile.close();
                sessionFile = null;
                mFileUseSemaphore.release();
            }
        }
    }


    /* *****************************************************************
     * option menu management
     * ****************************************************************/
    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater ){
        super.onCreateOptionsMenu( menu, inflater );

        MenuItem newAccountMenu = menu.add( R.string.menu_new_account );
        newAccountMenu.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick( MenuItem item ){
                Intent showIntent = new Intent( getActivity(), AccountActivity.class );
                showIntent.putExtra( EXTRA_REQUEST_CODE_KEY, NEW_REQUEST_CODE );
                startActivityForResult( showIntent, ACCOUNT_ACTIVITY_NEW_REQUEST_CODE );
                return true;
            }
        } );

        MenuItem settingsMenu = menu.add( R.string.settings );
        settingsMenu.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick( MenuItem item ){
                startActivity( new Intent( getActivity(), SettingsActivity.class ) );
                return true;
            }
        } );
    }

     /* *****************************************************************
     * context menu management
     * ****************************************************************/


    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo ){
        if( v.getId() == R.id.listview ){
            AdapterView.AdapterContextMenuInfo info = ( AdapterView.AdapterContextMenuInfo ) menuInfo;
            menu.setHeaderTitle( adapter.getItem( info.position ) );
            menu.add( Menu.NONE, MENU_COPY_PASS, Menu.NONE, R.string.menu_copy_pass );
            menu.add( Menu.NONE, MENU_COPY_PSEUDO, Menu.NONE, R.string.menu_copy_pseudo );
            menu.add( Menu.NONE, MENU_SHOW_DETAILS, Menu.NONE, R.string.menu_show_details );
            menu.add( Menu.NONE, MENU_EDIT, Menu.NONE, R.string.edit );
            menu.add( Menu.NONE, MENU_DELETE, Menu.NONE, R.string.menu_delete );
        }
    }


    @Override
    public boolean onContextItemSelected( MenuItem item ){
        AdapterView.AdapterContextMenuInfo info = ( AdapterView.AdapterContextMenuInfo ) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        final String accountName = mList.getItemAtPosition( info.position ).toString();

        switch( menuItemIndex ){

            case MENU_COPY_PASS:
                String pass = dataWrapper.getAccount( accountName ).getPassword();
                String msg;
                if( pass != null && !pass.isEmpty() ){
                    Util.copyToClipBoard( getActivity(), accountName + ":pass", pass );
                    msg = "Password copied to clipboard";
                }else{
                    msg = "Copy failed : password is empty !";
                }
                showToast( msg );
                break;

            case MENU_COPY_PSEUDO:
                String pseudo = dataWrapper.getAccount( accountName ).getPseudo();
                if( pseudo != null && !pseudo.isEmpty() ){
                    Util.copyToClipBoard( getActivity(), accountName + ":pseudo", pseudo );
                    msg = "Pseudo copied to clipboard";
                }else{
                    msg = "Pseudo is empty !";
                }
                showToast( msg );
                break;

            case MENU_SHOW_DETAILS:
            case MENU_EDIT:
                startEditShowActivityForResult( dataWrapper.getAccount( accountName ),
                        menuItemIndex == MENU_SHOW_DETAILS );

                break;

            case MENU_DELETE:
                AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
                builder.setMessage( "Are you sure ?" ).setCancelable( false ).setTitle( "Confirm " +
                        "" + "delete" );

                builder.setPositiveButton( "YES", new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int id ){
                        dataWrapper.removeAccount( accountName );
                        adapter.remove( accountName );
                        adapter.notifyDataSetChanged();
                        userHasModifiedData = true;
                    }
                } );

                builder.setNegativeButton( "NO", new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int id ){
                        dialog.cancel();
                    }
                } );

                builder.create().show();
                break;
        }// end switch
        return true;
    }


    private void startEditShowActivityForResult( Account account, boolean showDetailsOnly ){
        if( account == null ) return;
        Intent showIntent = new Intent( getActivity(), AccountActivity.class );
        showIntent.putExtra( Intent.EXTRA_TEXT, new Gson().toJson( account ) );
        showIntent.putExtra( EXTRA_REQUEST_CODE_KEY, showDetailsOnly ? SHOW_REQUEST_CODE : EDIT_REQUEST_CODE );
        startActivityForResult( showIntent, ACCOUNT_ACTIVITY_REQUEST_CODE );
    }//end

    /* *****************************************************************
     * sync management
     * ****************************************************************/


    private void startUpdateOnBackgroundThread(){
        new Thread( new Runnable(){
            @Override
            public void run(){
                synchronized( sessionFileLock ){
                    // do nothing is the file is null or if the data where modified locally
                    //                    if( sessionFile == null || userHasModifiedData ) {
                    //                        return;
                    //                    }
                    if( sessionFile == null ){
                        handler.sendLoadFailedMessage( "Error : the session file is null..." );
                        return;
                    }else if( userHasModifiedData ){
                        handler.sendUpdateDoneWithoutChangesMessage();
                        return;
                    }
                    boolean updated;
                    try{
                        updated = sessionFile.update();
                    }catch( DbxException e ){
                        Log.e( TAG, "failed to update file", e );
                        handler.sendLoadFailedMessage( e.toString() );
                        return;
                    }

                    // if some data where already loaded and there is no change,
                    // doesn't bother to reread the file and returns
                    if( mHasLoadedAnyData && !updated ){
                        handler.sendUpdateDoneWithoutChangesMessage();
                        return;
                    }

                    // from here, either to data are loaded for the first time,
                    // or there was an external change in the sessionFile
                    try{
                        Log.d( TAG, "starting read" );
                        Object contents = new JsonManager().deserialize( CRYPTO_ALGORITHM,
                                sessionFile.getReadStream(), mCurrentPassword,
                                new TypeToken<ArrayList<HashMap<String, String>>>(){
                        }.getType() );

                        if( contents != null ){
                            mHasLoadedAnyData = true;
                        }
                        Log.d( TAG, "read done" );

                        // asks the handler to update the view
                        handler.sendUpdateDoneWithChangesMessage( contents );

                    }catch( IOException e ){
                        Log.e( TAG, "failed to read file", e );
                        if( !mHasLoadedAnyData ){
                            handler.sendLoadFailedMessage( getString( R.string.error_failed_load ) );
                        }

                    }catch( JsonManager.WrongCredentialsException e ){
                        //TODO
                        handler.sendLoadFailedMessage( "Wrong credentials" );
                        e.printStackTrace();

                    }catch( Exception e ){
                        handler.sendLoadFailedMessage( "Sorry, but an unknown error occured" );
                        e.printStackTrace();

                    }
                }
            }
        } ).start();
    }


    private void startWriteLocalChangesThread( final boolean closeFileAfterWrite ){

        // Start a thread to do the write.
        new Thread( new Runnable(){
            @Override
            public void run(){
                Log.d( TAG, "starting write" );
                synchronized( sessionFileLock ){
                    try{
                        new JsonManager().serialize( dataWrapper.getRawData(), CRYPTO_ALGORITHM,
                                sessionFile.getWriteStream(), mCurrentPassword );
                        Log.d( TAG, "write done" );
                        userHasModifiedData = false;
                    }catch( Exception e ){
                        Log.e( TAG, "failed to write to file", e );
                    }
                    // if asked, closes the file
                    if( closeFileAfterWrite ){
                        sessionFile.close();
                        sessionFile = null;
                        mFileUseSemaphore.release();
                    }
                }
            }
        } ).start();

    }// end startWriteLocalChanges


    private void applyNewData( Object data ){
        // if the data were modified, do nothing
        //TODO
        if( userHasModifiedData || data == null ){
            return;
        }
        // updates the wrapper and the list adapter
        dataWrapper.setData( data );
        adapter.clear();
        adapter.addAll( dataWrapper.getAccountNames() );
        adapter.notifyDataSetChanged();
        // applies the filter again
        adapter.getFilter().filter( inputSearch.getText() );
        // explicitly reset mChanged to false since the setText above changed it to true
        userHasModifiedData = false;
    }


    /* *****************************************************************
     * Message handler
     * ****************************************************************/

    private static class DbxLoadHandler extends Handler{

        private final WeakReference<SessionDetailFragment> fragment;

        public static final int MESSAGE_IS_SHOWING_LATEST = 0;
        public static final int MESSAGE_DO_UPDATE = 1;
        public static final int MESSAGE_UPDATE_DONE = 2;
        public static final int MESSAGE_LOAD_FAILED = 3;

        private static final int TRUE = 1, FALSE = 0, UNDEF = -1;


        public DbxLoadHandler( SessionDetailFragment containingFragment ){
            fragment = new WeakReference<SessionDetailFragment>( containingFragment );
        }


        @Override
        public void handleMessage( Message msg ){
            SessionDetailFragment frag = fragment.get();
            if( frag == null ){
                return;
            }

            switch( msg.what ){

                case MESSAGE_IS_SHOWING_LATEST:
                    // arg1 : true if the file is the latest version
                    frag.oldVersionWarningView.setVisibility( msg.arg1 == TRUE ? View.GONE : View.VISIBLE );
                    return;

                case MESSAGE_DO_UPDATE:
                    // updates only if the user didn't modify the data
                    if( frag.userHasModifiedData ){
                        sendUpdateDoneWithoutChangesMessage();
                    }else{
                        frag.startUpdateOnBackgroundThread();
                    }

                    return;

                case MESSAGE_UPDATE_DONE:
                    if( frag.userHasModifiedData ){
                        Log.e( TAG, "Somehow user changed text while an update was in progress!" );
                    }

                    // be sure to hide the spinner
                    frag.loadingSpinnerView.setVisibility( View.GONE );
                    frag.errorMessageView.setVisibility( View.GONE );

                    // arg1 set to true only if there was a change, i.e. new data was loaded
                    if( msg.arg1 == TRUE ){
                        frag.applyNewData( msg.obj );
                    }
                    return;

                case MESSAGE_LOAD_FAILED:
                    frag.loadingSpinnerView.setVisibility( View.GONE );
                    // the object is the message to show
                    frag.errorMessageView.setText( ( String ) msg.obj );
                    frag.errorMessageView.setVisibility( View.VISIBLE );
                    return;

                default:
                    throw new RuntimeException( "Unknown message" );
            }
        }


        public void sendIsShowingLatestMessage( boolean isLatestVersion ){
            sendMessage( Message.obtain( this, MESSAGE_IS_SHOWING_LATEST, isLatestVersion ? TRUE : FALSE, UNDEF ) );
        }


        public void sendDoUpdateMessage(){
            sendMessage( Message.obtain( this, MESSAGE_DO_UPDATE ) );
        }


        public void sendUpdateDoneWithChangesMessage( Object newContents ){
            sendMessage( Message.obtain( this, MESSAGE_UPDATE_DONE, TRUE, UNDEF, newContents ) );
        }


        public void sendUpdateDoneWithoutChangesMessage(){
            sendMessage( Message.obtain( this, MESSAGE_UPDATE_DONE, FALSE, UNDEF ) );
        }


        public void sendLoadFailedMessage( String errorText ){
            sendMessage( Message.obtain( this, MESSAGE_LOAD_FAILED, errorText ) );
        }
    }

}
