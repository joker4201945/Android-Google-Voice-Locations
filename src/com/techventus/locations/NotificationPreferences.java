package com.techventus.locations;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

/**
 * The Class NotificationPreferences.
 */
public class NotificationPreferences extends Activity{
	
	/** The sound toggle button. */
	ToggleButton soundToggleButton;
	
	/** The alert toggle button. */
	ToggleButton alertToggleButton;
	
	/** The launch toggle button. */
	ToggleButton launchToggleButton;
	
	/** The preferences. */
	SharedPreferences preferences ;
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.notificationpreferences);

		soundToggleButton = (ToggleButton)findViewById(R.id.soundToggleButton);
		alertToggleButton = (ToggleButton)findViewById(R.id.alertToggleButton);
		launchToggleButton = (ToggleButton)findViewById(R.id.launchActivityButton);
		
		preferences   = this.getSharedPreferences(Settings.SharedPrefKey.PREFERENCES, 0);
	      
		soundToggleButton.setOnClickListener(click);
		alertToggleButton.setOnClickListener(click);
		launchToggleButton.setOnClickListener(click);
		
		alertToggleButton.setChecked(preferences.getBoolean(Settings.SharedPrefKey.NOTIFICATION_ACTIVE, true));
		soundToggleButton.setChecked(preferences.getBoolean(Settings.SharedPrefKey.SOUND_ACTIVE, true));
		launchToggleButton.setChecked(preferences.getBoolean(Settings.SharedPrefKey.NOTIFICATION_APP_LAUNCH, true));
	}
	

	
	
	/** The click. Click Listener that captures all Toggle Click Actions */
	OnClickListener click = new OnClickListener(){

		@Override
		public void onClick(View arg0) {
			switch(arg0.getId()){
				case R.id.soundToggleButton:
				{
					soundToggle();
				}
				case R.id.alertToggleButton:
				{
					alertToggle();
				}
				case R.id.launchActivityButton:
				{
					launchToggle();
				}
			}
		}
	};
	
	/**
	 * Alert Toggle. If Enabled, a Taskbar alert will display whenever a user changes defined location
	 */
	void alertToggle(){
		Editor edit = preferences.edit();
		edit.putBoolean(Settings.SharedPrefKey.NOTIFICATION_ACTIVE, alertToggleButton.isChecked());
		edit.commit();

	}
	
	/**
	 * Sound toggle. If Sound Toggle is enabled, Taskbar alert will also play default sound
	 */
	void soundToggle(){
		Editor edit = preferences.edit();
		edit.putBoolean(Settings.SharedPrefKey.SOUND_ACTIVE, soundToggleButton.isChecked());
		edit.commit();
	}
	
	/**
	 * Launch toggle. If Launch Toggle is Enabled, Clicking the Notification will launch Main Activity of this Application
	 */
	void launchToggle(){
		Editor edit = preferences.edit();
		edit.putBoolean(Settings.SharedPrefKey.NOTIFICATION_APP_LAUNCH, launchToggleButton.isChecked());
		edit.commit();
		
	}
	
	
}
