package linder.easypass.what;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.SectionIndexer;
import com.woozzu.android.util.StringMatcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * User: lucy
 * Date: 16/09/13
 * Version: 0.1
 */
public class IndexerAdapter extends ArrayAdapter<String> implements SectionIndexer {

    private final static String SECTIONS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final static String MINIFIED_SECTIONS = "#A.EFG.JKLM.OP.RS.Z";
    private WeakReference<DataWrapper> dataWrapperReference;
    private SessionsFilter filter;


    public IndexerAdapter( Context context, int textViewResourceId, List<String> objects ) {
        super( context, textViewResourceId, objects );
    }

    @Override
    public int getPositionForSection( int section ) {
        // If there is no item for current section, previous section will be selected
        for( int i = section; i >= 0; i-- ) {
            for( int j = 0; j < getCount(); j++ ) {
                if( i == 0 ) {
                    // For numeric section
                    for( int k = 0; k <= 9; k++ ) {
                        if( StringMatcher.match( String.valueOf( getItem( j ).charAt( 0 ) ),
                                String.valueOf( k ) ) ) {
                            return j;
                        }
                    }
                } else {
                    if( StringMatcher.match( String.valueOf( getItem( j ).charAt( 0 ) ),
                            String.valueOf( SECTIONS.charAt( i ) ) ) ) {
                        return j;
                    }
                }
            }
        }
        return 0;
    }


    @Override
    public int getSectionForPosition( int position ) {
        return 0;
    }


    @Override
    public Object[] getSections() {
        String[] sections = new String[ SECTIONS.length() ];
        for( int i = 0; i < SECTIONS.length(); i++ )
            sections[ i ] = String.valueOf( SECTIONS.charAt( i ) );
        return sections;
    }


    public void setDataWrapperReference( DataWrapper wrapper ) {
        dataWrapperReference = new WeakReference<DataWrapper>( wrapper );
    }//end updateWrapper

    /* *****************************************************************
     * filtering
     * ****************************************************************/


    @Override
    public Filter getFilter() {
        if( filter == null ) {
            filter = new SessionsFilter();
        }//end if
        return filter;
    }


    private class SessionsFilter extends Filter {

        @Override
        protected FilterResults performFiltering( CharSequence constraintSequence ) {

            DataWrapper dataWrapper = dataWrapperReference.get();
            if( dataWrapper == null ) return null;


            FilterResults result = new FilterResults();

            // if a constraint, i.e. a string is specified
            if( constraintSequence != null && constraintSequence.toString().length() > 0 ) {
                ArrayList<String> filteredItems = new ArrayList<String>();

                    String[] constraints = constraintSequence.toString().split( "[ ]+" );

                    for( Account account : dataWrapper.getData() ) {
                        if( account.fieldsContains( constraints ) ) {
                            filteredItems.add( account.getName() );
                        }
                    }//end for


                result.count = filteredItems.size();
                result.values = filteredItems;

            } else {
                // if no filter, just returns the entire map
                synchronized( this ) {
                    result.count = dataWrapper.getData().size();
                    result.values = new ArrayList<String>( dataWrapper.getAccountNames() );
                }
            }

            return result;
        }


        @SuppressWarnings( "unchecked" )
        @Override
        protected void publishResults( CharSequence constraint, FilterResults results ) {
            if( results == null || results.values == null ) return;
            clear();
            addAll( ( ArrayList<String> ) results.values );
            notifyDataSetChanged();
        }


    }//end filter class
}//end class

