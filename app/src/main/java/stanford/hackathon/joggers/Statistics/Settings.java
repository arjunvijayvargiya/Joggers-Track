package stanford.hackathon.joggers.Statistics;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import stanford.hackathon.joggers.R;

public class Settings extends PreferenceActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);
    }
}
