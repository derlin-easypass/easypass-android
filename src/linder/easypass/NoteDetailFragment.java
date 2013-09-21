package linder.easypass;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.dropbox.sync.android.*;
import com.google.gson.reflect.TypeToken;
import com.woozzu.android.widget.IndexableListView;
import linder.easypass.what.DataWrapper;
import linder.easypass.what.IndexerAdapter;
import linder.easypass.what.JsonManager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class NoteDetailFragment extends Fragment {
    private static final String TAG = NoteDetailFragment.class.getName();

    private static final String ARG_PATH = "path";
    private static final String CRYPTO_ALGORITHM = "aes-128-cbc";
    private static final String ARG_PASS = "password";

    private EditText mText;
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


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        final View view = inflater.inflate( R.layout.fragment_note_detail, container, false );

        mList = ( IndexableListView ) view.findViewById( com.woozzu.android.indexablelistview.R
                .id.listview );
        dataWrapper = new DataWrapper( null, mCurrentSessionName, mCurrentPassword );
        mAdapter = new IndexerAdapter( getActivity(), android.R.layout.simple_list_item_1,
                new ArrayList<String>() );
        mList.setAdapter( mAdapter );
        mList.setFastScrollEnabled( true );

        mOldVersionWarningView = view.findViewById( R.id.old_version );
        mLoadingSpinner = view.findViewById( R.id.note_loading );
        mErrorMessage = ( TextView ) view.findViewById( R.id.error_message );

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();

        //        mText.setEnabled( false );
        //        mText.setText( "" );
        mUserHasModifiedText = false;
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

        /*
         * Since mFile is written asynchronously after onPause, it's possible
         * that the activity is resumed again before a write finishes. This
         * semaphore prevents us from trying to re-open the file while it's
         * still being written in the background - we hold it whenever mFile is
         * in use, and release it when the write is finished and we're done with
         * the file.
         */
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
                //                final String newContents = mText.getText().toString();
                final String newContents = "";
                mUserHasModifiedText = false;

                // Start a thread to do the write.
                new Thread( new Runnable() {
                    @Override
                    public void run() {
                        Log.d( TAG, "starting write" );
                        synchronized( mFileLock ) {
                            try {
                                mFile.writeString( newContents );
                            } catch( IOException e ) {
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
                        }catch(Exception e){
                           mHandler.sendLoadFailedMessage( "Sorry, but an unknown error occured"
                           );
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


    private void applyNewText( List<Object[]> data ) {
        if( mUserHasModifiedText || data == null ) {
            return;
        }

        dataWrapper.setData( data );
        mAdapter.clear();
        mAdapter.addAll( dataWrapper.getAccountNames() );
        mAdapter.notifyDataSetChanged();
        // explicitly reset mChanged to false since the setText above changed it to true
        mUserHasModifiedText = false;
    }


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

                //                frag.mText.setVisibility( View.VISIBLE );
                frag.mLoadingSpinner.setVisibility( View.GONE );
                frag.mErrorMessage.setVisibility( View.GONE );

                boolean gotNewData = msg.arg1 != 0;
                if( gotNewData ) {
                    List<Object[]> contents = ( List<Object[]> ) msg.obj;
                    frag.applyNewText( contents );
                }

            } else if( msg.what == MESSAGE_LOAD_FAILED ) {
                String errorText = ( String ) msg.obj;
                //                frag.mText.setVisibility( View.GONE );
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
