package rachel.materialapps.imgcompressor.ui.pages;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import rachel.materialapps.imgcompressor.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}