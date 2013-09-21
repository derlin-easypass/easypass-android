package linder.easypass;

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
import linder.easypass.what.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static linder.easypass.EasyPassApplication.TAG;

public class NoteDetailFragment extends Fragment {

    // keys to the bundle's extras
    private static final String ARG_PATH = "path";
    private static final String ARG_PASS = "password";

    // algo for the deserialisation of data
    private static final String CRYPTO_ALGORITHM = "aes-128-cbc";
    // request codes for startActivityForResult calls
    private static final int EDIT_REQUEST_CODE = 9;

    // context menu items
    private static final int MENU_COPY_PASS = 0, MENU_SHOW_DETAILS = 1, MENU_EDIT = 2,
            MENU_DELETE = 3;


    private TextView mErrorMessage;
    private View mOldVersionWarningView;
    private View mLoadingSpinner;

    private final DbxLoadHandler mHandler = new DbxLoadHandler( this );

    private DbxFile mFile;
    private final Object mFileLock = new Object();
    private final Semaphore mFileUseSemaphore = new Semaphore( 1 );
    private boolean mUserHasModifiedText = false;
    private boolean mHasLoadedAnyData = false;


    private IndexableListView mList;
    private IndexerAdapter mAdapter;
    private DataWrapper dataWrapper;
    private String mCurrentSessionName, mCurrentPassword;


    private final DbxFile.Listener mChangeListener = new DbxFile.Listener() {

        @Override
        public void onFileChange( DbxFile file ) {
            // In case a notification is delivered late, make sure we're still
            // on-screen (mFile != null) and still working on the same file.
            synchronized( mFileLock ) {
                if( file != mFile ) {
                    return;
                }
            }

            if( mUserHasModifiedText ) {
                // User has modified the text locally, so we no longer care
                // about external changes.
                return;
            }

            boolean currentIsLatest;
            boolean newerIsCached = false;
            try {
                currentIsLatest = file.getSyncStatus().isLatest;

                if( !currentIsLatest ) {
                    newerIsCached = file.getNewerStatus().isCached;
                }
            } catch( DbxException e ) {
                Log.w( TAG, "Failed to get sync status", e );
                return;
            }

            mHandler.sendIsShowingLatestMessage( currentIsLatest );

            // kick off an update if necessary
            if( newerIsCached || !mHasLoadedAnyData ) {
                mHandler.sendDoUpdateMessage();
            }
        }
    };
    private EditText inputSearch;


    /* *****************************************************************
     * constructors
     * ****************************************************************/


    public NoteDetailFragment() {
    }


    public static NoteDetailFragment getInstance( DbxPath path, String password ) {
        NoteDetailFragment fragment = new NoteDetailFragment();
        Bundle args = new Bundle();
        args.putString( ARG_PATH, path.toString() );
        args.putString( ARG_PASS, password );
        fragment.setArguments( args );
        return fragment;
    }

    /* *****************************************************************
     * override common activity/fragment methods
     * ****************************************************************/


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        final View view = inflater.inflate( R.layout.fragment_note_detail, container, false );

        mList = ( IndexableListView ) view.findViewById( com.woozzu.android.indexablelistview.R
                .id.listview );
        dataWrapper = new DataWrapper( null, mCurrentSessionName, mCurrentPassword );
        mAdapter = new IndexerAdapter( getActivity(), android.R.layout.simple_list_item_1,
                new ArrayList<String>() );
        mAdapter.setDataWrapperReference( dataWrapper );
        mList.setAdapter( mAdapter );
        mList.setFastScrollEnabled( true );
        registerForContextMenu( mList );

        // adds a listener to filter the accounts on text change
        inputSearch = ( EditText ) view.findViewById( R.id.inputSearch );
        inputSearch.addTextChangedListener( new TextWatcherAdapter( inputSearch,
                new TextWatcherAdapter.TextWatcherListener() {

            @Override
            public void onTextChanged( EditText view, String text ) {
                mAdapter.getFilter().filter( text );
            }
        } ) );

        mOldVersionWarningView = view.findViewById( R.id.old_version );
        mLoadingSpinner = view.findViewById( R.id.note_loading );
        mErrorMessage = ( TextView ) view.findViewById( R.id.error_message );

        return view;
    }


    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if( requestCode == EDIT_REQUEST_CODE ) {
            if( resultCode == Activity.RESULT_OK ) {
                Bundle extras = data.getExtras();
                Account originalAccount = dataWrapper.getAccount( extras.getString(
                        EditAccountActivity.EXTRA_ORIGINAL_ACCOUNT_NAME ) );
                Account editedAccount = new Gson().fromJson( extras.getString(
                        EditAccountActivity.EXTRA_ACCOUNT_KEY ), Account.class );

                // if return false, it means the two accounts are identical
                if( !dataWrapper.replaceAccount( originalAccount, editedAccount ) ) return;
                String oldName = originalAccount.getNameOrDefault();

                if( !originalAccount.getNameOrDefault().equals( editedAccount.getNameOrDefault()
                ) ) {
                    int position = mAdapter.getPosition( oldName );
                    mAdapter.remove( oldName );
                    mAdapter.insert( editedAccount.getNameOrDefault(), position );
                    mAdapter.notifyDataSetChanged();
                }
                mUserHasModifiedText = true;
            }

        } else {
            super.onActivityResult( requestCode, resultCode, data );
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        //        mUserHasModifiedText = false;
        mHasLoadedAnyData = false;

        DbxPath path = new DbxPath( getArguments().getString( ARG_PATH ) );
        mCurrentPassword = getArguments().getString( ARG_PASS );

        // Grab the note name from the path:
        String sessionName = Util.stripExtension( "data_ser", path.getName() );

        getActivity().setTitle( sessionName );
        mCurrentSessionName = sessionName;

        DbxAccount acct = NotesAppConfig.getAccountManager( getActivity() ).getLinkedAccount();
        if( null == acct ) {
            Log.e( TAG, "No linked account." );
            return;
        }

        mErrorMessage.setVisibility( View.GONE );
        mLoadingSpinner.setVisibility( View.VISIBLE );

        try {
            mFileUseSemaphore.acquire();
        } catch( InterruptedException e ) {
            throw new RuntimeException( e );
        }

        try {
            DbxFileSystem fs = DbxFileSystem.forAccount( acct );
            try {
                mFile = fs.open( path );
            } catch( DbxException.NotFound e ) {
                mFile = fs.create( path );
            }
        } catch( DbxException e ) {
            Log.e( TAG, "failed to open or create file.", e );
            return;
        }

        mFile.addListener( mChangeListener );

        boolean latest;
        try {
            latest = mFile.getSyncStatus().isLatest;
        } catch( DbxException e ) {
            Log.w( TAG, "Failed to get sync status", e );
            return;
        }


        mHandler.sendIsShowingLatestMessage( latest );
        mHandler.sendDoUpdateMessage();
    }


    @Override
    public void onPause() {
        super.onPause();

        synchronized( mFileLock ) {
            mFile.removeListener( mChangeListener );

            // If the contents have changed, write them back to Dropbox
            if( mUserHasModifiedText && mFile != null ) {
                mUserHasModifiedText = false;

                // Start a thread to do the write.
                new Thread( new Runnable() {
                    @Override
                    public void run() {
                        Log.d( TAG, "starting write" );
                        synchronized( mFileLock ) {
                            try {

                                new JsonManager().serialize( dataWrapper.getArrayOfObjects(),
                                        CRYPTO_ALGORITHM, mFile.getWriteStream(),
                                        mCurrentPassword );

                            } catch( Exception e ) {

                                Log.e( TAG, "failed to write to file", e );
                            }
                            mFile.close();
                            Log.d( TAG, "write done" );
                            mFile = null;
                        }
                        mFileUseSemaphore.release();
                    }
                } ).start();

            } else {
                mFile.close();
                mFile = null;
                mFileUseSemaphore.release();
            }
        }
    }

    /* *****************************************************************
     * context menu management
     * ****************************************************************/


    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo
            menuInfo ) {
        if( v.getId() == R.id.listview ) {
            AdapterView.AdapterContextMenuInfo info = ( AdapterView.AdapterContextMenuInfo )
                    menuInfo;
            menu.setHeaderTitle( mAdapter.getItem( info.position ) );
            menu.add( Menu.NONE, MENU_COPY_PASS, Menu.NONE, R.string.menu_copy_pass );
            menu.add( Menu.NONE, MENU_SHOW_DETAILS, Menu.NONE, R.string.menu_show_details );
            menu.add( Menu.NONE, MENU_EDIT, Menu.NONE, R.string.edit );
            menu.add( Menu.NONE, MENU_DELETE, Menu.NONE, R.string.menu_delete );
        }
    }


    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        AdapterView.AdapterContextMenuInfo info = ( AdapterView.AdapterContextMenuInfo ) item
                .getMenuInfo();
        int menuItemIndex = item.getItemId();
        final String accountName = mList.getItemAtPosition( info.position ).toString();

        switch( menuItemIndex ) {

            case MENU_COPY_PASS:
                break;

            case MENU_SHOW_DETAILS:
                break;

            case MENU_EDIT:
                if( accountName == null ) break;
                Intent intent = new Intent( getActivity(), EditAccountActivity.class );
                intent.putExtra( Intent.EXTRA_TEXT, new Gson().toJson( dataWrapper.getAccount(
                        accountName ) ) );
                startActivityForResult( intent, EDIT_REQUEST_CODE );
                break;

            case MENU_DELETE:
                AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
                builder.setMessage( "Are you sure ?" ).setCancelable( false ).setTitle( "Confirm " +
                        "" + "delete" );

                builder.setPositiveButton( "YES", new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int id ) {
                        dataWrapper.removeAccount( accountName );
                        mAdapter.remove( accountName );
                        mAdapter.notifyDataSetChanged();
                        mUserHasModifiedText = true;
                    }
                } );

                builder.setNegativeButton( "NO", new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int id ) {
                        dialog.cancel();
                    }
                } );

                builder.create().show();
                break;
        }// end switch
        return true;
    }
     /* *****************************************************************
     * sync management
     * ****************************************************************/


    private void startUpdateOnBackgroundThread() {
        new Thread( new Runnable() {
            @Override
            public void run() {
                synchronized( mFileLock ) {
                    if( null == mFile || mUserHasModifiedText ) {
                        return;
                    }
                    boolean updated;
                    try {
                        updated = mFile.update();
                    } catch( DbxException e ) {
                        Log.e( TAG, "failed to update file", e );
                        mHandler.sendLoadFailedMessage( e.toString() );
                        return;
                    }

                    if( !mHasLoadedAnyData || updated ) {
                        Log.d( TAG, "starting read" );
                        ArrayList<Object[]> contents;
                        try {
                            contents = ( ArrayList<Object[]> ) new JsonManager().deserialize(
                                    CRYPTO_ALGORITHM, mFile.getReadStream(), mCurrentPassword,
                                    new TypeToken<ArrayList<Object[]>>() {
                            }.getType() );

                        } catch( IOException e ) {
                            Log.e( TAG, "failed to read file", e );
                            if( !mHasLoadedAnyData ) {
                                mHandler.sendLoadFailedMessage( getString( R.string
                                        .error_failed_load ) );
                            }
                            return;
                        } catch( JsonManager.WrongCredentialsException e ) {
                            mHandler.sendLoadFailedMessage( "wrong credentials" );
                            e.printStackTrace();
                            return;
                        } catch( Exception e ) {
                            mHandler.sendLoadFailedMessage( "Sorry, but an unknown error occured" );
                            e.printStackTrace();
                            return;
                        }
                        Log.d( TAG, "read done" );

                        if( contents != null ) {
                            mHasLoadedAnyData = true;
                        }

                        mHandler.sendUpdateDoneWithChangesMessage( contents );
                    } else {
                        mHandler.sendUpdateDoneWithoutChangesMessage();
                    }
                }
            }
        } ).start();
    }


    private void applyNewData( List<Object[]> data ) {
        if( mUserHasModifiedText || data == null ) {
            return;
        }
        dataWrapper.setData( data );
        mAdapter.clear();
        mAdapter.addAll( dataWrapper.getAccountNames() );
        mAdapter.notifyDataSetChanged();
        mAdapter.getFilter().filter( inputSearch.getText() );
        // explicitly reset mChanged to false since the setText above changed it to true
        mUserHasModifiedText = false;
    }


    /* *****************************************************************
     * Message handler
     * ****************************************************************/

    private static class DbxLoadHandler extends Handler {

        private final WeakReference<NoteDetailFragment> mFragment;

        public static final int MESSAGE_IS_SHOWING_LATEST = 0;
        public static final int MESSAGE_DO_UPDATE = 1;
        public static final int MESSAGE_UPDATE_DONE = 2;
        public static final int MESSAGE_LOAD_FAILED = 3;


        public DbxLoadHandler( NoteDetailFragment containingFragment ) {
            mFragment = new WeakReference<NoteDetailFragment>( containingFragment );
        }


        @Override
        public void handleMessage( Message msg ) {
            NoteDetailFragment frag = mFragment.get();
            if( frag == null ) {
                return;
            }

            if( msg.what == MESSAGE_IS_SHOWING_LATEST ) {
                boolean latest = msg.arg1 != 0;
                frag.mOldVersionWarningView.setVisibility( latest ? View.GONE : View.VISIBLE );
            } else if( msg.what == MESSAGE_DO_UPDATE ) {
                if( frag.mUserHasModifiedText ) {
                    // user has made changes to the file, so ignore this request
                    return;
                }

                // disable UI before doing an update - if user were to make
                // changes between now and when the update completes, they would
                // erroneously be applied on top of that newer version, so
                // prevent that by just temporarily disabling the UI (should be
                // quick anyway).
                //frag.mText.setEnabled( false );

                frag.startUpdateOnBackgroundThread();
            } else if( msg.what == MESSAGE_UPDATE_DONE ) {
                if( frag.mUserHasModifiedText ) {
                    Log.e( TAG, "Somehow user changed text while an update was in progress!" );
                }

                frag.mLoadingSpinner.setVisibility( View.GONE );
                frag.mErrorMessage.setVisibility( View.GONE );

                boolean gotNewData = msg.arg1 != 0;
                if( gotNewData ) {
                    List<Object[]> contents = ( List<Object[]> ) msg.obj;
                    frag.applyNewData( contents );
                }

            } else if( msg.what == MESSAGE_LOAD_FAILED ) {
                String errorText = ( String ) msg.obj;
                frag.mLoadingSpinner.setVisibility( View.GONE );
                frag.mErrorMessage.setText( errorText );
                frag.mErrorMessage.setVisibility( View.VISIBLE );
            } else {
                throw new RuntimeException( "Unknown message" );
            }
        }


        public void sendIsShowingLatestMessage( boolean isLatestVersion ) {
            sendMessage( Message.obtain( this, MESSAGE_IS_SHOWING_LATEST,
                    isLatestVersion ? 1 : 0, -1 ) );
        }


        public void sendDoUpdateMessage() {
            sendMessage( Message.obtain( this, MESSAGE_DO_UPDATE ) );
        }


        public void sendUpdateDoneWithChangesMessage( List<Object[]> newContents ) {
            sendMessage( Message.obtain( this, MESSAGE_UPDATE_DONE, 1, -1, newContents ) );
        }


        public void sendUpdateDoneWithoutChangesMessage() {
            sendMessage( Message.obtain( this, MESSAGE_UPDATE_DONE, 0, -1 ) );
        }


        public void sendLoadFailedMessage( String errorText ) {
            sendMessage( Message.obtain( this, MESSAGE_LOAD_FAILED, errorText ) );
        }
    }

}
