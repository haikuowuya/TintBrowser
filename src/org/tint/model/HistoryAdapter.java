package org.tint.model;

import org.tint.R;
import org.tint.providers.BookmarksProvider;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DateSorter;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Custom adapter for displaying history, splitted in bins.
 * Adapted from:
 * https://github.com/CyanogenMod/android_packages_apps_Browser/blob/gingerbread/src/com/android/browser/BrowserHistoryPage.java
 * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android-apps/2.2_r1.1/com/android/browser/DateSortedExpandableListAdapter.java/?v=source
 */
public class HistoryAdapter extends BaseExpandableListAdapter {
	
	private LayoutInflater mInflater = null;
	
	private int[] mItemMap;
	private int mNumberOfBins;
	private DateSorter mDateSorter;
	private int mIdIndex;
	
	private Context mContext;
	private Cursor mCursor;
	
	private int mDateIndex;
	
	private int mFaviconSize;
	
	private OnCheckedChangeListener mBookmarkStarChangeListener;
	
	/**
	 * Constructor.
	 * @param context The current context.
	 * @param cursor The data cursor.
	 * @param dateIndex The date index ?
	 */
	public HistoryAdapter(Context context, OnCheckedChangeListener bookmarksChangeListener, int faviconSize) {
		mContext = context;
		mBookmarkStarChangeListener = bookmarksChangeListener;
		mCursor = null;
		mDateIndex = -1;
		mFaviconSize = faviconSize;
		
		mDateSorter = new DateSorter(mContext);
		
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public void changeCursor(Cursor cursor) {
		if (mCursor == cursor) {
			return;
		}
		
		mCursor = cursor;
		
		if (mCursor != null) {
			mIdIndex = mCursor.getColumnIndexOrThrow(BookmarksProvider.Columns._ID);
			mDateIndex = mCursor.getColumnIndexOrThrow(BookmarksProvider.Columns.VISITED_DATE);
			
			buildMap();
			
			notifyDataSetChanged();
		} else {
			mIdIndex = -1;
			mDateIndex = -1;
			
			notifyDataSetInvalidated();
		}
	}
	
	/**
	 * Get a long-typed data from mCursor.
	 * @param cursorIndex The column index.
	 * @return The long data.
	 */
	private long getLong(int cursorIndex) {
        return mCursor.getLong(cursorIndex);
    }
	
	/**
	 * Split the data in the cursor into several "bins": today, yesterday, last 7 days, last month, older.
	 */
	private void buildMap() {
		int[] array = new int[DateSorter.DAY_COUNT];
        // Zero out the array.
        for (int j = 0; j < DateSorter.DAY_COUNT; j++) {
            array[j] = 0;
        }
        
        mNumberOfBins = 0;
        int dateIndex = -1;
        if (mCursor.moveToFirst() && mCursor.getCount() > 0) {
            while (!mCursor.isAfterLast()) {
                long date = getLong(mDateIndex);
                int index = mDateSorter.getIndex(date);
                if (index > dateIndex) {
                    mNumberOfBins++;
                    if (index == DateSorter.DAY_COUNT - 1) {
                        // We are already in the last bin, so it will
                        // include all the remaining items
                        array[index] = mCursor.getCount()
                                - mCursor.getPosition();
                        break;
                    }
                    dateIndex = index;
                }
                array[dateIndex]++;
                mCursor.moveToNext();
            }
        }
        
        mItemMap = array;
	}
	
	/**
     * Translates from a group position in the ExpandableList to a bin.  This is
     * necessary because some groups have no history items, so we do not include
     * those in the ExpandableList.
     * @param groupPosition Position in the ExpandableList's set of groups
     * @return The corresponding bin that holds that group.
     */
    private int groupPositionToBin(int groupPosition) {
        if (groupPosition < 0 || groupPosition >= DateSorter.DAY_COUNT) {
            throw new AssertionError("group position out of range");
        }
        
        if (DateSorter.DAY_COUNT == mNumberOfBins || 0 == mNumberOfBins) {
            // In the first case, we have exactly the same number of bins
            // as our maximum possible, so there is no need to do a
            // conversion
            // The second statement is in case this method gets called when
            // the array is empty, in which case the provided groupPosition
            // will do fine.
            return groupPosition;
        }
        
        int arrayPosition = -1;
        while (groupPosition > -1) {
            arrayPosition++;
            if (mItemMap[arrayPosition] != 0) {
                groupPosition--;
            }
        }
        
        return arrayPosition;
    }
    
    /**
     * Move the cursor to the record corresponding to the given group position and child position. 
     * @param groupPosition The group position.
     * @param childPosition The child position.
     * @return True if the move has succeeded.
     */
	private boolean moveCursorToChildPosition(int groupPosition, int childPosition) {
        if (mCursor.isClosed()) {
        	return false;
        }
        
        groupPosition = groupPositionToBin(groupPosition);
        int index = childPosition;
        for (int i = 0; i < groupPosition; i++) {
            index += mItemMap[i];
        }
        
        return mCursor.moveToPosition(index);
    }
	
	/**
	 * Create a new view.
	 * @return The created view.
	 */
	private TextView getGenericView() {
        // Layout parameters for the ExpandableListView
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, (int) (45 * mContext.getResources().getDisplayMetrics().density));

        TextView textView = new TextView(mContext);
        textView.setLayoutParams(lp);
        // Center the text vertically
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        // Set the text starting position
        textView.setPadding((int) (35 * mContext.getResources().getDisplayMetrics().density), 0, 0, 0);
        
        return textView;
    }
	
	/**
	 * Create a new child view.
	 * @return The created view.
	 */
	private View getCustomChildView() {		
		return mInflater.inflate(R.layout.history_row, null, false);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		moveCursorToChildPosition(groupPosition, childPosition);

		return new BookmarkHistoryItem(mCursor.getLong(mCursor.getColumnIndex(BookmarksProvider.Columns._ID)),
				mCursor.getString(mCursor.getColumnIndex(BookmarksProvider.Columns.TITLE)),
				mCursor.getString(mCursor.getColumnIndex(BookmarksProvider.Columns.URL)),
				mCursor.getInt(mCursor.getColumnIndex(BookmarksProvider.Columns.BOOKMARK)) >= 1 ? true : false,
				mCursor.getBlob(mCursor.getColumnIndex(BookmarksProvider.Columns.FAVICON)));
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		if (moveCursorToChildPosition(groupPosition, childPosition)) {
            return getLong(mIdIndex);
        }
		
        return 0;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View view = getCustomChildView();
        
		TextView titleView = (TextView) view.findViewById(R.id.HistoryRow_Title);
		
		BookmarkHistoryItem item = (BookmarkHistoryItem) getChild(groupPosition, childPosition);
		titleView.setText(item.getTitle());
		
		TextView urlView = (TextView) view.findViewById(R.id.HistoryRow_Url);		 					
		urlView.setText(item.getUrl());
		
		CheckBox bookmarkStar = (CheckBox) view.findViewById(R.id.HistoryRow_BookmarkStar);
		
		bookmarkStar.setTag(item.getId());
		
		bookmarkStar.setOnCheckedChangeListener(null);
		bookmarkStar.setChecked(item.isBookmark());
		bookmarkStar.setOnCheckedChangeListener(mBookmarkStarChangeListener);
		
		ImageView faviconView = (ImageView) view.findViewById(R.id.HistoryRow_Thumbnail);
		Bitmap favicon = item.getFavicon();
		if (favicon != null) {
			BitmapDrawable icon = new BitmapDrawable(favicon);
			
			Bitmap bm = Bitmap.createBitmap(mFaviconSize, mFaviconSize, Bitmap.Config.ARGB_4444);
			Canvas canvas = new Canvas(bm);
			
			icon.setBounds(0, 0, mFaviconSize, mFaviconSize);
			icon.draw(canvas);
			
			faviconView.setImageBitmap(bm);
		} else {
			faviconView.setImageResource(R.drawable.app_web_browser_sm);
		}
        
        return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mItemMap[groupPositionToBin(groupPosition)];
	}

	@Override
	public Object getGroup(int groupPosition) {
		int binIndex = groupPositionToBin(groupPosition);
		
		switch (binIndex) {
		case 0: return mContext.getResources().getString(R.string.HistoryToday);
		case 1: return mContext.getResources().getString(R.string.HistoryYesterday);
		case 2: return mContext.getResources().getString(R.string.HistoryLastSevenDays);
		case 3: return mContext.getResources().getString(R.string.HistoryLastMonth);
		default: return mContext.getResources().getString(R.string.HistoryOlder);
		}
	}

	@Override
	public int getGroupCount() {
		return mNumberOfBins;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		TextView textView = getGenericView();
        textView.setText(getGroup(groupPosition).toString());
        
        return textView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}