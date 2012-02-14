package org.tint.ui;

import org.tint.R;
import org.tint.controllers.Controller;
import org.tint.model.DownloadItem;
import org.tint.tasks.ThumbnailTaker;
import org.tint.ui.activities.BookmarksActivity;
import org.tint.ui.activities.EditBookmarkActivity;
import org.tint.ui.activities.TintBrowserActivity;
import org.tint.ui.components.CustomWebView;
import org.tint.ui.dialogs.GeolocationPermissionsDialog;
import org.tint.ui.fragments.BaseWebViewFragment;
import org.tint.ui.fragments.StartPageFragment;
import org.tint.utils.ApplicationUtils;
import org.tint.utils.Constants;
import org.tint.utils.UrlUtils;

import android.app.ActionBar;
import android.app.DownloadManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.widget.FrameLayout;
import android.widget.Toast;

public abstract class BaseUIManager implements UIManager {
	
	protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS =
	        new FrameLayout.LayoutParams(
	        ViewGroup.LayoutParams.MATCH_PARENT,
	        ViewGroup.LayoutParams.MATCH_PARENT);
	
	private FrameLayout mFullscreenContainer;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private int mOriginalOrientation;
    
    private GeolocationPermissionsDialog mGeolocationPermissionsDialog;
	
	protected TintBrowserActivity mActivity;	
	protected ActionBar mActionBar;
	protected FragmentManager mFragmentManager;
	
	protected boolean mMenuVisible = false;
	
	private ValueCallback<Uri> mUploadMessage = null;
	
	protected StartPageFragment mStartPageFragment = null;
	
	public BaseUIManager(TintBrowserActivity activity) {
		mActivity = activity;
		
		mActionBar = mActivity.getActionBar();
		mFragmentManager = mActivity.getFragmentManager();
		
		mGeolocationPermissionsDialog = null;
		
		setupUI();
	}
	
	protected abstract void setupUI();
	
	protected abstract String getCurrentUrl();
	
	protected abstract int getTabCount();
	
	protected abstract void showStartPage();
	
	protected abstract void hideStartPage();
	
	protected void setApplicationButtonImage(Bitmap icon) {
		BitmapDrawable image = ApplicationUtils.getApplicationButtonImage(mActivity, icon);
		
		if (image != null) {			
			mActionBar.setIcon(image);
		} else {
			mActionBar.setIcon(R.drawable.ic_launcher);
		}
	}		
	
	@Override
	public TintBrowserActivity getMainActivity() {
		return mActivity;
	}
	
	@Override
	public void addTab(boolean loadHomePage) {
		if (loadHomePage) {
			addTab(PreferenceManager.getDefaultSharedPreferences(mActivity).getString(Constants.PREFERENCE_HOME_PAGE, Constants.URL_ABOUT_START));
		} else {
			addTab(null);
		}
	}
	
	@Override
	public void loadUrl(String url) {
		if ((url != null) &&
    			(url.length() > 0)) {
			
			if (UrlUtils.isUrl(url)) {
    			url = UrlUtils.checkUrl(url);
    		} else {
    			url = UrlUtils.getSearchUrl(mActivity, url);
    		}
			
			CustomWebView currentWebView = getCurrentWebView();
			
			if (url.equals(Constants.URL_ABOUT_START)) {				
				showStartPage();
				
				currentWebView.clearView();
				currentWebView.clearHistory();
			} else {
				hideStartPage();
				
				currentWebView.loadUrl(url);
			}
			
			currentWebView.requestFocus();
		}
	}
	
	@Override
	public void loadHomePage() {
		loadUrl(PreferenceManager.getDefaultSharedPreferences(mActivity).getString(Constants.PREFERENCE_HOME_PAGE, Constants.URL_ABOUT_START));
	}
	
	@Override
	public void loadCurrentUrl() {
		loadUrl(getCurrentUrl());
	}
	
	@Override
	public void openBookmarksActivityForResult() {
		Intent i = new Intent(mActivity, BookmarksActivity.class);
    	mActivity.startActivityForResult(i, TintBrowserActivity.ACTIVITY_BOOKMARKS);
	}
	
	@Override
	public void addBookmarkFromCurrentPage() {
		Intent i = new Intent(mActivity, EditBookmarkActivity.class);
		
		i.putExtra(Constants.EXTRA_ID, (long) -1);
    	i.putExtra(Constants.EXTRA_LABEL, getCurrentWebView().getTitle());
    	i.putExtra(Constants.EXTRA_URL, getCurrentWebView().getUrl());
    	
    	mActivity.startActivity(i);
	}
	
	@Override
	public void shareCurrentPage() {
		WebView webView = getCurrentWebView();
		
		if (webView != null) {
			ApplicationUtils.sharePage(mActivity, webView.getTitle(), webView.getUrl());
		}
	}
	
	@Override
	public void startSearch() {
		WebView webView = getCurrentWebView();
		
		if (webView != null) {
			webView.showFindDialog(null, true);
		}
	}
	
	@Override
	public void clearFormData() {
		WebViewDatabase.getInstance(mActivity).clearFormData();
		getCurrentWebView().clearFormData();
	}
	
	@Override
	public void clearCache() {
		getCurrentWebView().clearCache(true);
	}
	
	@Override
	public void setHttpAuthUsernamePassword(String host, String realm, String username, String password) {
		getCurrentWebView().setHttpAuthUsernamePassword(host, realm, username, password);
	}
	
	@Override
	public void setUploadMessage(ValueCallback<Uri> uploadMsg) {
		mUploadMessage = uploadMsg;
	}
	
	@Override
	public ValueCallback<Uri> getUploadMessage() {
		return mUploadMessage;
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		if (intent != null) {
			if (Intent.ACTION_VIEW.equals(intent.getAction()) ||
					Intent.ACTION_MAIN.equals(intent.getAction())) {
				// ACTION_VIEW and ACTION_MAIN can specify an url to load.
				String url = intent.getDataString();
				
				if (!TextUtils.isEmpty(url)) {
					if (!isCurrentTabReusable()) {
						addTab(url);
					} else {
						loadUrl(url);
					}					
				} else {
					// We do not have an url. Open a new tab if there is no tab currently opened,
					// else do nothing.
					if (getTabCount() <= 0) {
						addTab(true);
					}
				}
			} else if (Constants.ACTION_BROWSER_CONTEXT_MENU.equals(intent.getAction())) {
				if (intent.hasExtra(Constants.EXTRA_ACTION_ID)) {
					int actionId = intent.getIntExtra(Constants.EXTRA_ACTION_ID, -1);
					
					switch(actionId) {
					case TintBrowserActivity.CONTEXT_MENU_OPEN:
						loadUrl(intent.getStringExtra(Constants.EXTRA_URL));
						break;
					case TintBrowserActivity.CONTEXT_MENU_OPEN_IN_NEW_TAB:
						addTab(intent.getStringExtra(Constants.EXTRA_URL));
						break;
					case TintBrowserActivity.CONTEXT_MENU_COPY:
						ApplicationUtils.copyTextToClipboard(mActivity, intent.getStringExtra(Constants.EXTRA_URL), mActivity.getResources().getString(R.string.UrlCopyToastMessage));
						break;
					case TintBrowserActivity.CONTEXT_MENU_DOWNLOAD:
						DownloadItem item = new DownloadItem(intent.getStringExtra(Constants.EXTRA_URL));
						
						long id = ((DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(item);
						item.setId(id);
						
						Controller.getInstance().getDownloadsList().add(item);
						
						Toast.makeText(mActivity, String.format(mActivity.getString(R.string.DownloadStart), item.getFileName()), Toast.LENGTH_SHORT).show();
						
						break;
					case TintBrowserActivity.CONTEXT_MENU_SHARE:
						ApplicationUtils.sharePage(mActivity, null, intent.getStringExtra(Constants.EXTRA_URL));
						break;
					default:
						Controller.getInstance().getAddonManager().onContributedContextLinkMenuItemSelected(mActivity, actionId, intent, getCurrentWebView());
						break;
					}
				}
			}
		} else {
			addTab(true);
		}
	}
	
	@Override
	public boolean onKeyBack() {
		if (mCustomView != null) {
			onHideCustomView();
			return true;
		}		
		
		return false;
	}
	
	@Override
	public void onPageFinished(WebView view, String url) {
		new ThumbnailTaker(mActivity.getContentResolver(),
				url,
				view.getOriginalUrl(),
				view,
				ApplicationUtils.getBookmarksThumbnailsDimensions(mActivity)).execute();
	}
	
	@Override
	public void onReceivedIcon(WebView view, Bitmap icon) {
		if (view == getCurrentWebView()) {
			setApplicationButtonImage(icon);
		}
	}
	
	@Override
	public void onMainActivityPause() {
		getCurrentWebView().pauseTimers();
	}
	
	@Override
	public void onMainActivityResume() {
		getCurrentWebView().resumeTimers();
	}
	
	private void setFullscreen(boolean enabled) {
        Window win = mActivity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (enabled) {
            winParams.flags |=  bits;
            if (mCustomView != null) {
                mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                //mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            }
        } else {
            winParams.flags &= ~bits;
            if (mCustomView != null) {
                mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                //mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
        win.setAttributes(winParams);
    }
	
	@Override
	public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
		if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
		
		if (requestedOrientation == -1) {
			requestedOrientation = mActivity.getRequestedOrientation();
		}
		
		mOriginalOrientation = mActivity.getRequestedOrientation();
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        mFullscreenContainer = new FullscreenHolder(mActivity);
        mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        mCustomView = view;
        setFullscreen(true);
        mCustomViewCallback = callback;
        mActivity.setRequestedOrientation(requestedOrientation);
	}
	
	@Override
	public void onHideCustomView() {
		if (mCustomView == null)
            return;
		
        setFullscreen(false);
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        decor.removeView(mFullscreenContainer);
        mFullscreenContainer = null;
        mCustomView = null;
        mCustomViewCallback.onCustomViewHidden();
        // Show the content view.
        mActivity.setRequestedOrientation(mOriginalOrientation);
	}
	
	@Override
	public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
		if (mGeolocationPermissionsDialog == null) {
			mGeolocationPermissionsDialog = new GeolocationPermissionsDialog(mActivity);
		}
		
		mGeolocationPermissionsDialog.initialize(origin, callback);		
		mGeolocationPermissionsDialog.show();
		
	}

	@Override
	public void onGeolocationPermissionsHidePrompt() {
		if (mGeolocationPermissionsDialog != null) {
			mGeolocationPermissionsDialog.hide();
		}
	}
	
//	private void showStartPage() {
//		BaseWebViewFragment current = getCurrentWebViewFragment();
//		
//		if ((current != null) &&
//				(!current.isStartPageShown())) {
//
//			current.setStartPageShown(true);			
//
//			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
//
//			if (mStartPageFragment == null) {
//				mStartPageFragment = new StartPageFragment();
//				mStartPageFragment.setOnStartPageItemClickedListener(new OnStartPageItemClickedListener() {					
//					@Override
//					public void onStartPageItemClicked(String url) {
//						loadUrl(url);
//					}
//				});
//				
//				fragmentTransaction.add(R.id.WebViewContainer, mStartPageFragment);
//			}
//
//			fragmentTransaction.hide(current);
//			fragmentTransaction.show(mStartPageFragment);
//
//			fragmentTransaction.commit();
//			
//			onShowStartPage();
//		}
//	}
//	
//	private void hideStartPage() {
//		BaseWebViewFragment current = getCurrentWebViewFragment();
//		
//		if ((current != null) &&
//				(current.isStartPageShown())) {
//			
//			current.setStartPageShown(false);
//			
//			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();			
//
//			fragmentTransaction.hide(mStartPageFragment);
//			fragmentTransaction.show(current);
//
//			fragmentTransaction.commit();
//			
//			onHideStartPage();
//		}
//	}

	/**
	 * Check if the current tab can be reused to display an intent request.
	 * A tab is reusable if it is on the user-defined start page.
	 * @return True if the current tab can be reused.
	 */
	private boolean isCurrentTabReusable() {
		String homePageUrl = PreferenceManager.getDefaultSharedPreferences(mActivity).getString(Constants.PREFERENCE_HOME_PAGE, Constants.URL_ABOUT_START);
		BaseWebViewFragment currentWebViewFragment = getCurrentWebViewFragment();
		CustomWebView currentWebView = getCurrentWebView();
		
		return (currentWebViewFragment != null && currentWebViewFragment.isStartPageShown()) ||
				(currentWebView != null && homePageUrl != null && homePageUrl.equals(currentWebView.getUrl()));
	}
	
	static class FullscreenHolder extends FrameLayout {

        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
        }

        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }

    }

}