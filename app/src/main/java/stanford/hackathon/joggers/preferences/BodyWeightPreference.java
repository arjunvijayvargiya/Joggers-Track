package stanford.hackathon.joggers.preferences;


import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class BodyWeightPreference extends EditMeasurementPreference {

	public BodyWeightPreference(Context context) {
		super(context);
	}
	public BodyWeightPreference(Context context, AttributeSet attr) {
		super(context, attr);
	}
	public BodyWeightPreference(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);
	}

	protected void initPreferenceDetails() {
		
	}
}

