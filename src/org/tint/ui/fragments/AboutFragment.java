package org.tint.ui.fragments;

import org.tint.R;

import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.about_fragment, container, false);
		
		TextView versionText = (TextView) v.findViewById(R.id.AboutVersionText);
		versionText.setText(getVersion());
		
		return v;
	}
	
	/**
	 * Get the current package version.
	 * @return The current version.
	 */
	private String getVersion() {
		String result = "";		
		try {

			PackageManager manager = getActivity().getPackageManager();
			PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);

			result = String.format(getString(R.string.AboutVersionText), info.versionName, info.versionCode);

		} catch (NameNotFoundException e) {
			Log.w(AboutFragment.class.toString(), "Unable to get application version: " + e.getMessage());
			result = "Unable to get application version.";
		}

		return result;
	}

}