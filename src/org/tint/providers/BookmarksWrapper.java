package org.tint.providers;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.tint.model.BookmarkHistoryItem;
import org.tint.model.UrlSuggestionCursorAdapter;
import org.tint.model.UrlSuggestionItem;
import org.tint.model.UrlSuggestionItemComparator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class BookmarksWrapper {
	
	public static String[] HISTORY_BOOKMARKS_PROJECTION = new String[] {
		BookmarksProvider.Columns._ID,
        BookmarksProvider.Columns.TITLE,
        BookmarksProvider.Columns.URL,
        BookmarksProvider.Columns.VISITS,
        BookmarksProvider.Columns.CREATION_DATE,
        BookmarksProvider.Columns.VISITED_DATE,
        BookmarksProvider.Columns.BOOKMARK,
        BookmarksProvider.Columns.FAVICON,
        BookmarksProvider.Columns.THUMBNAIL };
	
	public static CursorLoader getCursorLoaderForStartPage(Context context, int limit) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());          
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.DAY_OF_YEAR, - 14);
		
		String whereClause = BookmarksProvider.Columns.VISITED_DATE + " > " + Long.toString(c.getTimeInMillis());	
		
		String orderClause = BookmarksProvider.Columns.VISITS + " DESC, " + 
				BookmarksProvider.Columns.VISITED_DATE + " DESC, " +
				BookmarksProvider.Columns.TITLE + " COLLATE NOCASE LIMIT " + Integer.toString(limit);
		
		return new CursorLoader(context, BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, orderClause);
	}
	
	public static CursorLoader getCursorLoaderForBookmarks(Context context) {
		String whereClause = BookmarksProvider.Columns.BOOKMARK + " = 1";
		String orderClause = BookmarksProvider.Columns.VISITS + " DESC, " + BookmarksProvider.Columns.TITLE + " COLLATE NOCASE";
		
		return new CursorLoader(context, BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, orderClause);
	}
	
	public static CursorLoader getCursorLoaderForHistory(Context context) {
		String whereClause = BookmarksProvider.Columns.VISITS + " > 0";
		String orderClause = BookmarksProvider.Columns.VISITED_DATE + " DESC";
		
		return new CursorLoader(context, BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, orderClause);
	}
	
	public static Cursor getAllHistoryBookmarks(ContentResolver contentResolver) {
		return contentResolver.query(BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, null, null, null);
	}
	
	public static Cursor getBookmarks(ContentResolver contentResolver) {
		String whereClause = BookmarksProvider.Columns.BOOKMARK + " = 1";
		String orderClause = BookmarksProvider.Columns.VISITS + " DESC, " + BookmarksProvider.Columns.TITLE + " COLLATE NOCASE";
		
		return contentResolver.query(BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, orderClause);
	}
	
	public static BookmarkHistoryItem getBookmarkById(ContentResolver contentResolver, long id) {
		BookmarkHistoryItem result = null;
		String whereClause = BookmarksProvider.Columns._ID + " = " + id;
		
		Cursor c = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, null);
		if (c != null) {
			if (c.moveToFirst()) {				
				String title = c.getString(c.getColumnIndex(BookmarksProvider.Columns.TITLE));
                String url = c.getString(c.getColumnIndex(BookmarksProvider.Columns.URL));
                boolean isBookmarks = c.getInt(c.getColumnIndex(BookmarksProvider.Columns.BOOKMARK)) > 0 ? true : false;
                byte[] favIcon = c.getBlob(c.getColumnIndex(BookmarksProvider.Columns.FAVICON));
                result = new BookmarkHistoryItem(id, title, url, isBookmarks, favIcon);
			}
			
			c.close();
		}
		
		return result;
	}
	
	/**
	 * Clear the history/bookmarks table.
	 * @param contentResolver The content resolver.
	 * @param clearHistory If true, history items will be cleared.
	 * @param clearBookmarks If true, bookmarked items will be cleared.
	 */
	public static void clearHistoryAndOrBookmarks(ContentResolver contentResolver, boolean clearHistory, boolean clearBookmarks) {
		
		if (!clearHistory && !clearBookmarks) {
			return;
		}
		
		String whereClause = null;
		if (clearHistory && clearBookmarks) {
			whereClause = null;
		} else if (clearHistory) {
			whereClause = "(" + BookmarksProvider.Columns.BOOKMARK + " = 0) OR (" + BookmarksProvider.Columns.BOOKMARK + " IS NULL)";
		} else if (clearBookmarks) {
			whereClause = BookmarksProvider.Columns.BOOKMARK + " = 1";
		}
		
		contentResolver.delete(BookmarksProvider.BOOKMARKS_URI, whereClause, null);		
	}
	
	/**
	 * Modify a bookmark/history record. If an id is provided, it look for it and update its values. If not, values will be inserted.
	 * If no id is provided, it look for a record with the given url. It found, its values are updated. If not, values will be inserted.
	 * @param contentResolver The content resolver.
	 * @param id The record id to look for.
	 * @param title The record title.
	 * @param url The record url.
	 * @param isBookmark If True, the record will be a bookmark.
	 */
	public static void setAsBookmark(ContentResolver contentResolver, long id, String title, String url, boolean isBookmark) {

		boolean bookmarkExist = false;

		if (id != -1) {
			String[] colums = new String[] { BookmarksProvider.Columns._ID };
			String whereClause = BookmarksProvider.Columns._ID + " = " + id;

			Cursor cursor = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, colums, whereClause, null, null);
			bookmarkExist = (cursor != null) && (cursor.moveToFirst());
		} else {
			String[] colums = new String[] { BookmarksProvider.Columns._ID };
			String whereClause = BookmarksProvider.Columns.URL + " = \"" + url + "\"";

			Cursor cursor = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, colums, whereClause, null, null);
			bookmarkExist = (cursor != null) && (cursor.moveToFirst());
			if (bookmarkExist) {
				id = cursor.getLong(cursor.getColumnIndex(BookmarksProvider.Columns._ID));
			}
		}

		ContentValues values = new ContentValues();
		if (title != null) {
			values.put(BookmarksProvider.Columns.TITLE, title);
		}

		if (url != null) {
			values.put(BookmarksProvider.Columns.URL, url);
		}

		if (isBookmark) {
			values.put(BookmarksProvider.Columns.BOOKMARK, 1);
			values.put(BookmarksProvider.Columns.CREATION_DATE, new Date().getTime());
		} else {
			values.put(BookmarksProvider.Columns.BOOKMARK, 0);
		}

		if (bookmarkExist) {                                    
			contentResolver.update(BookmarksProvider.BOOKMARKS_URI, values, BookmarksProvider.Columns._ID + " = " + id, null);
		} else {                        
			contentResolver.insert(BookmarksProvider.BOOKMARKS_URI, values);
		}
	}
	
	public static void deleteBookmark(ContentResolver contentResolver, long id) {
		String whereClause = BookmarksProvider.Columns._ID + " = " + id;
        
		Cursor c = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, null);
		if (c != null) {
			if (c.moveToFirst()) {
				if (c.getInt(c.getColumnIndex(BookmarksProvider.Columns.BOOKMARK)) == 1) {
					if (c.getInt(c.getColumnIndex(BookmarksProvider.Columns.VISITS)) > 0) {
						
						// If this record has been visited, keep it in history, but remove its bookmark flag.
                        ContentValues values = new ContentValues();
                        values.put(BookmarksProvider.Columns.BOOKMARK, 0);
                        values.putNull(BookmarksProvider.Columns.CREATION_DATE);
                        
                        contentResolver.update(BookmarksProvider.BOOKMARKS_URI, values, whereClause, null);

					} else {
						// never visited, it can be deleted.
						contentResolver.delete(BookmarksProvider.BOOKMARKS_URI, whereClause, null);
					}
				}
			}
			
			c.close();
		}
	}
	
	public static void deleteHistoryRecord(ContentResolver contentResolver, long id) {
		String whereClause = BookmarksProvider.Columns._ID + " = " + id;
        
		Cursor c = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, null);
		if (c != null) {
			if (c.moveToFirst()) {
				if (c.getInt(c.getColumnIndex(BookmarksProvider.Columns.BOOKMARK)) > 0) {
					// This is a bookmark, we cannot delete it. Instead, set visits count to 0 and visited date to null.
					ContentValues values = new ContentValues();
                    values.put(BookmarksProvider.Columns.VISITS, 0);
                    values.putNull(BookmarksProvider.Columns.VISITED_DATE);
                    
                    contentResolver.update(BookmarksProvider.BOOKMARKS_URI, values, whereClause, null);
				} else {
					// Not a bookmark, it can be deleted.
					contentResolver.delete(BookmarksProvider.BOOKMARKS_URI, whereClause, null);
				}
			}
			
			c.close();
		}
	}
	
	/**
	 * Update the history: visit count and last visited date.
	 * @param contentResolver The content resolver.
	 * @param title The title.
	 * @param url The url.
	 * @param originalUrl The original url 
	 */
	public static void updateHistory(ContentResolver contentResolver, String title, String url, String originalUrl) {
		String[] colums = new String[] { BookmarksProvider.Columns._ID, BookmarksProvider.Columns.URL, BookmarksProvider.Columns.BOOKMARK, BookmarksProvider.Columns.VISITS };
		String whereClause = BookmarksProvider.Columns.URL + " = \"" + url + "\" OR " + BookmarksProvider.Columns.URL + " = \"" + originalUrl + "\"";

		Cursor cursor = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, colums, whereClause, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {

				long id = cursor.getLong(cursor.getColumnIndex(BookmarksProvider.Columns._ID));
				int visits = cursor.getInt(cursor.getColumnIndex(BookmarksProvider.Columns.VISITS)) + 1;

				ContentValues values = new ContentValues();

				// If its not a bookmark, we can update the title. If we were doing it on bookmarks, we would override the title choosen by the user.
				if (cursor.getInt(cursor.getColumnIndex(BookmarksProvider.Columns.BOOKMARK)) != 1) {
					values.put(BookmarksProvider.Columns.TITLE, title);
				}

				values.put(BookmarksProvider.Columns.VISITED_DATE, new Date().getTime());
				values.put(BookmarksProvider.Columns.VISITS, visits);

				contentResolver.update(BookmarksProvider.BOOKMARKS_URI, values, BookmarksProvider.Columns._ID + " = " + id, null);

			} else {
				ContentValues values = new ContentValues();
				values.put(BookmarksProvider.Columns.TITLE, title);
				values.put(BookmarksProvider.Columns.URL, url);
				values.put(BookmarksProvider.Columns.VISITED_DATE, new Date().getTime());
				values.put(BookmarksProvider.Columns.VISITS, 1);
				values.put(BookmarksProvider.Columns.BOOKMARK, 0);

				contentResolver.insert(BookmarksProvider.BOOKMARKS_URI, values);
			}               

			cursor.close();
		}
	}
	
	/**
	 * Remove from history values prior to now minus the number of days defined in preferences.
	 * Only delete history items. For bookmarks, reset their visited value date and visits count.
	 * @param contentResolver The content resolver.
	 */
	public static void truncateHistory(ContentResolver contentResolver, String prefHistorySize) {
		int historySize;
		try {
			historySize = Integer.parseInt(prefHistorySize);
		} catch (NumberFormatException e) {
			historySize = 90;
		}

		Calendar c = Calendar.getInstance();
		c.setTime(new Date());          
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.DAY_OF_YEAR, - historySize);

		String whereClauseDelete = "(" + BookmarksProvider.Columns.BOOKMARK + " = 0 OR " + BookmarksProvider.Columns.BOOKMARK + " IS NULL) AND " + BookmarksProvider.Columns.VISITED_DATE + " < " + c.getTimeInMillis();
		String whereClauseUpdate = BookmarksProvider.Columns.BOOKMARK + " = 1  AND " + BookmarksProvider.Columns.VISITED_DATE + " < " + c.getTimeInMillis();
		
		ContentValues updateValues = new ContentValues();
		updateValues.putNull(BookmarksProvider.Columns.VISITED_DATE);
		updateValues.put(BookmarksProvider.Columns.VISITS, 0);
		
		try {
			contentResolver.delete(BookmarksProvider.BOOKMARKS_URI, whereClauseDelete, null);
			contentResolver.update(BookmarksProvider.BOOKMARKS_URI, updateValues, whereClauseUpdate, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.w("BookmarksWrapper", "Unable to truncate history: " + e.getMessage());
		}
	}
	
	/**
	 * Update the favicon in history/bookmarks database.
	 * @param contentResolver The content resolver.
	 * @param url The url.
	 * @param originalUrl The original url.
	 * @param favicon The favicon.
	 */
	public static void updateFavicon(ContentResolver contentResolver, String url, String originalUrl, Bitmap favicon) {
		String whereClause;
		
		if (!url.equals(originalUrl)) {
			whereClause = BookmarksProvider.Columns.URL + " = \"" + url + "\" OR " + BookmarksProvider.Columns.URL + " = \"" + originalUrl + "\"";
		} else {
			whereClause = BookmarksProvider.Columns.URL + " = \"" + url + "\"";
		}

		BitmapDrawable icon = new BitmapDrawable(favicon);

		ByteArrayOutputStream os = new ByteArrayOutputStream();         
		icon.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, os);

		ContentValues values = new ContentValues();
		values.put(BookmarksProvider.Columns.FAVICON, os.toByteArray());				

		try {
			contentResolver.update(BookmarksProvider.BOOKMARKS_URI, values, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.w("BookmarksWrapper", "Unable to update favicon: " + e.getMessage());
		}
	}
	
	public static void updateThumbnail(ContentResolver contentResolver, String url, String originalUrl, Bitmap thumbnail) {
		String whereClause;
		
		if (!url.equals(originalUrl)) {
			whereClause = "(" + BookmarksProvider.Columns.URL + " = \"" + url + "\" OR " + BookmarksProvider.Columns.URL + " = \"" + originalUrl + "\")";
		} else {
			whereClause = BookmarksProvider.Columns.URL + " = \"" + url + "\"";
		}
		
		whereClause += " AND " + BookmarksProvider.Columns.BOOKMARK + " = 1";

		BitmapDrawable icon = new BitmapDrawable(thumbnail);

		ByteArrayOutputStream os = new ByteArrayOutputStream();         
		icon.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, os);

		ContentValues values = new ContentValues();
		values.put(BookmarksProvider.Columns.THUMBNAIL, os.toByteArray());				

		try {
			contentResolver.update(BookmarksProvider.BOOKMARKS_URI, values, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.w("BookmarksWrapper", "Unable to update thumbnail: " + e.getMessage());
		}
	}
	
	public static boolean urlHasBookmark(ContentResolver contentResolver, String url, String originalUrl) {
		String whereClause;
		
		if (!url.equals(originalUrl)) {
			whereClause = "(" + BookmarksProvider.Columns.URL + " = \"" + url + "\" OR " + BookmarksProvider.Columns.URL + " = \"" + originalUrl + "\")";
		} else {
			whereClause = BookmarksProvider.Columns.URL + " = \"" + url + "\"";
		}
		
		whereClause += " AND " + BookmarksProvider.Columns.BOOKMARK + " = 1";
		
		Cursor c = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, null);
		
		return c != null && c.getCount() > 0;
	}
	
	public static CursorLoader getBookmarksCursorLoaderWithLimit(Context context, int limit) {
		String whereClause = BookmarksProvider.Columns.BOOKMARK + " = 1";
		String orderClause = BookmarksProvider.Columns.VISITS + " DESC LIMIT " + Integer.toString(limit);
		
		return new CursorLoader(context, BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, orderClause);
	}
	
	public static CursorLoader getHistoryCursorLoaderWithLimit(Context context, int limit) {
		String whereClause = BookmarksProvider.Columns.VISITS + " > 0";
		String orderClause = BookmarksProvider.Columns.VISITED_DATE + " DESC LIMIT " + Integer.toString(limit);
		
		return new CursorLoader(context, BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, orderClause);
	}
	
	/**
	 * Get a list of most visited bookmarks items, limited in size.
	 * @param contentResolver The content resolver.
	 * @param limit The size limit.
	 * @return A list of BookmarkItem.
	 */
	public static List<BookmarkHistoryItem> getBookmarksWithLimit(ContentResolver contentResolver, int limit) {
//		List<BookmarkItem> result = new ArrayList<BookmarkItem>();
		
		String whereClause = BookmarksProvider.Columns.BOOKMARK + " = 1";
		String orderClause = BookmarksProvider.Columns.VISITS + " DESC";
				
		Cursor cursor = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, orderClause);
		
		return mapCursorToBookmarkHistoryItemListWithLimit(cursor, limit);
//		if (cursor != null) {
//			if (cursor.moveToFirst()) {
//				
//				int columnTitle = cursor.getColumnIndex(BookmarksProvider.Columns.TITLE);
//				int columnUrl = cursor.getColumnIndex(BookmarksProvider.Columns.URL);
//				
//				int count = 0;
//				while (!cursor.isAfterLast() &&
//						(count < limit)) {
//					
//					BookmarkItem item = new BookmarkItem(
//							cursor.getString(columnTitle),
//							cursor.getString(columnUrl));
//					
//					result.add(item);
//					
//					count++;
//					cursor.moveToNext();
//				}
//			}
//			
//			cursor.close();
//		}
//		
//		return result;
	}
	
	/**
	 * Get a list of most recent history items, limited in size.
	 * @param contentResolver The content resolver.
	 * @param limit The size limit.
	 * @return A list of HistoryItem.
	 */
	public static List<BookmarkHistoryItem> getHistoryWithLimit(ContentResolver contentResolver, int limit) {
//		List<BookmarkHistoryItem> result = new ArrayList<BookmarkHistoryItem>();
		
		String whereClause = BookmarksProvider.Columns.VISITS + " > 0";
		String orderClause = BookmarksProvider.Columns.VISITED_DATE + " DESC";
		
		Cursor cursor = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, orderClause);
		
		return mapCursorToBookmarkHistoryItemListWithLimit(cursor, limit);
//		if (cursor != null) {
//			if (cursor.moveToFirst()) {
//				
//				int columnId = cursor.getColumnIndex(BookmarksProvider.Columns._ID);
//				int columnTitle = cursor.getColumnIndex(BookmarksProvider.Columns.TITLE);
//				int columnUrl = cursor.getColumnIndex(BookmarksProvider.Columns.URL);
//				int columnBookmark = cursor.getColumnIndex(BookmarksProvider.Columns.BOOKMARK);
//				
//				int count = 0;
//				while (!cursor.isAfterLast() &&
//						(count < limit)) {
//					
//					BookmarkHistoryItem item = new BookmarkHistoryItem(
//							cursor.getLong(columnId),
//							cursor.getString(columnTitle),
//							cursor.getString(columnUrl),
//							cursor.getInt(columnBookmark) >= 1 ? true : false,
//							null);
//					
//					result.add(item);
//					
//					count++;
//					cursor.moveToNext();
//				}
//			}
//			
//			cursor.close();
//		}
//		
//		return result;
	}
	
	private static List<BookmarkHistoryItem> mapCursorToBookmarkHistoryItemListWithLimit(Cursor cursor, int limit) {
		List<BookmarkHistoryItem> result = new ArrayList<BookmarkHistoryItem>();
		
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				
				int columnId = cursor.getColumnIndex(BookmarksProvider.Columns._ID);
				int columnTitle = cursor.getColumnIndex(BookmarksProvider.Columns.TITLE);
				int columnUrl = cursor.getColumnIndex(BookmarksProvider.Columns.URL);
				int columnBookmark = cursor.getColumnIndex(BookmarksProvider.Columns.BOOKMARK);
				int columnFavicon = cursor.getColumnIndex(BookmarksProvider.Columns.FAVICON);
				
				int count = 0;
				while (!cursor.isAfterLast() &&
						(count < limit)) {
					
					BookmarkHistoryItem item = new BookmarkHistoryItem(
							cursor.getLong(columnId),
							cursor.getString(columnTitle),
							cursor.getString(columnUrl),
							cursor.getInt(columnBookmark) >= 1 ? true : false,
							cursor.getBlob(columnFavicon));
					
					result.add(item);
					
					count++;
					cursor.moveToNext();
				}
			}
			
			cursor.close();
		}
		
		return result;
	}
	
	public static void toggleBookmark(ContentResolver contentResolver, long id, boolean bookmark) {
		String[] colums = new String[] { BookmarksProvider.Columns._ID };
		String whereClause = BookmarksProvider.Columns._ID + " = " + id;

		Cursor cursor = contentResolver.query(BookmarksProvider.BOOKMARKS_URI, colums, whereClause, null, null);
		boolean recordExists = (cursor != null) && (cursor.moveToFirst());
		
		if (recordExists) {
			ContentValues values = new ContentValues();
			
			values.put(BookmarksProvider.Columns.BOOKMARK, bookmark);
			if (bookmark) {
				values.put(BookmarksProvider.Columns.CREATION_DATE, new Date().getTime());
			} else {
				values.putNull(BookmarksProvider.Columns.CREATION_DATE);
				values.putNull(BookmarksProvider.Columns.THUMBNAIL);
			}
			
			contentResolver.update(BookmarksProvider.BOOKMARKS_URI, values, whereClause, null);
		}
	}
	
	/**
	 * Insert a full record in history/bookmarks database.
	 * @param contentResolver The content resolver.
	 * @param title The record title.
	 * @param url The record url.
	 * @param visits The record visit count.
	 * @param visitedDate The record last visit date.
	 * @param creationDate The record bookmark creation date.
	 * @param bookmark The bookmark flag.
	 */
	public static void insertRawRecord(ContentResolver contentResolver, String title, String url, int visits, long visitedDate, long creationDate, int bookmark) {
		ContentValues values = new ContentValues();
		values.put(BookmarksProvider.Columns.TITLE, title);
		values.put(BookmarksProvider.Columns.URL, url);
		values.put(BookmarksProvider.Columns.VISITS, visits);
		
		if (visitedDate > 0) {
			values.put(BookmarksProvider.Columns.VISITED_DATE, visitedDate);
		} else {
			values.putNull(BookmarksProvider.Columns.VISITED_DATE);
		}
		
		if (creationDate > 0) {
			values.put(BookmarksProvider.Columns.CREATION_DATE, creationDate);
		} else {
			values.putNull(BookmarksProvider.Columns.CREATION_DATE);
		}
		
		if (bookmark > 0) {
			values.put(BookmarksProvider.Columns.BOOKMARK, 1);
		} else {
			values.put(BookmarksProvider.Columns.BOOKMARK, 0);
		}
		
		contentResolver.insert(BookmarksProvider.BOOKMARKS_URI, values);
	}
	
	/**
     * Get a cursor for suggestions, given a search pattern.
     * Search on history and bookmarks, on title and url.
     * The result list is sorted based on each result note.
     * @see UrlSuggestionItem for how a note is computed.
     * @param contentResolver The content resolver.
     * @param pattern The pattern to search for.
     * @return A cursor of suggections.
     */
    public static Cursor getUrlSuggestions(ContentResolver contentResolver, String pattern) {
    	MatrixCursor cursor = new MatrixCursor(new String[] {
    			UrlSuggestionCursorAdapter.URL_SUGGESTION_ID,
    			UrlSuggestionCursorAdapter.URL_SUGGESTION_TITLE,
    			UrlSuggestionCursorAdapter.URL_SUGGESTION_URL,
    			UrlSuggestionCursorAdapter.URL_SUGGESTION_TYPE });
    	
    	if ((pattern != null) &&
    			(pattern.length() > 0)) {
    		
    		String sqlPattern = "%" + pattern + "%";
    		
    		List<UrlSuggestionItem> results = new ArrayList<UrlSuggestionItem>();
    		
    		Cursor stockCursor = contentResolver.query(BookmarksProvider.BOOKMARKS_URI,
    				HISTORY_BOOKMARKS_PROJECTION,
    				BookmarksProvider.Columns.TITLE + " LIKE '" + sqlPattern + "' OR " + BookmarksProvider.Columns.URL  + " LIKE '" + sqlPattern + "'",
    				null,
    				null);
    		
    		if (stockCursor != null) {
    			if (stockCursor.moveToFirst()) {
    				int titleId = stockCursor.getColumnIndex(BookmarksProvider.Columns.TITLE);
    				int urlId = stockCursor.getColumnIndex(BookmarksProvider.Columns.URL);
    				int bookmarkId = stockCursor.getColumnIndex(BookmarksProvider.Columns.BOOKMARK);
    				
    				do {
    					boolean isFolder = stockCursor.getInt(bookmarkId) > 0 ? true : false;
    					results.add(new UrlSuggestionItem(pattern,
    							stockCursor.getString(titleId),
    							stockCursor.getString(urlId),
    							isFolder ? 2 : 1));
    					
    				} while (stockCursor.moveToNext());
    			}
    			
    			stockCursor.close();
    		}
    		
//    		if (lookInWeaveBookmarks) {
//    			Cursor weaveCursor = contentResolver.query(WeaveColumns.CONTENT_URI,
//    					null,
//    					WeaveColumns.WEAVE_BOOKMARKS_FOLDER + " = 0 AND (" +  WeaveColumns.WEAVE_BOOKMARKS_TITLE + " LIKE '" + sqlPattern + "' OR " + WeaveColumns.WEAVE_BOOKMARKS_URL  + " LIKE '" + sqlPattern + "')",
//    					null, null);
//
//    			if (weaveCursor != null) {
//    				if (weaveCursor.moveToFirst()) {
//    					
//    					int weaveTitleId = weaveCursor.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_TITLE);
//        				int weaveUrlId = weaveCursor.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_URL);
//    					
//    					do {
//    						results.add(new UrlSuggestionItem(pattern,
//    								weaveCursor.getString(weaveTitleId),
//    								weaveCursor.getString(weaveUrlId),
//    								3));
//    					} while (weaveCursor.moveToNext());
//    				}
//
//    				weaveCursor.close();
//    			}
//    		}
    		
    		// Sort results.
    		Collections.sort(results, new UrlSuggestionItemComparator());
    		
    		// Copy results to the output MatrixCursor.
    		int idCounter = -1;
    		for (UrlSuggestionItem item : results) {
    			idCounter++;
				
				String[] row = new String[4];
				row[0] = Integer.toString(idCounter);
				row[1] = item.getTitle();
				row[2] = item.getUrl();
				row[3] = Integer.toString(item.getType());
				
				cursor.addRow(row);
    		}
    	}
    	
    	return cursor;
    }

}