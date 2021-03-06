package com.techventus.locations;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import gvjava.org.json.JSONException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.techventus.server.voice.Voice;
import com.techventus.server.voice.datatypes.AllSettings;
import com.techventus.server.voice.datatypes.Phone;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView.BufferType;

/**
 * Dialer.  This Activity is made in response to a lack of Apps Available 
 * for Using the Callback Feature of Google Voice which is compatible with 
 * normal use of the Official Google Voice App.  
 * 
 * This Activity Contains its own Dialer.  It may seem excessive, but hopefully it is as easy
 * to use as the built in Dialer.  It ought to save many users a great deal of money.
 */
public class Dialer extends Activity{

	 /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 150;

    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 80;

	/** The TAG. */
	private static final String TAG = "TECHVENTUS - Dialer";
	
	/** The button star. */
	private Button button00,button01,button02,button03,button04,button05,button06,button07,button08,button09,buttonDel,buttonPound,buttonStar ;

    private ToneGenerator mToneGenerator;

    private Object mToneGeneratorLock = new Object();

	/** The dial number. */
	private TextView mDialNumber;
	
	/** The phone select. */
	Spinner phoneSelect;
	
	/** The Call button. */
	Button CallButton;
	
	/** The selected phone. */
	String selectedPhone = "";
	
	/** The Contact lookup. */
	ImageView ContactLookup;
	
	/** The phones. */
	Phone[] phones;
	
	/** The phone strings. */
	String[] phoneStrings;
	
	/** The voice. */
	Voice voice;

    AdView mAdView;

	 // determines if we want to playback local DTMF tones.
    private boolean mDTMFToneEnabled;
    
    private int vol = 0;
	
	/**
	 * AsyncTask Background Task to Set the local Voice Object
	 * and check Google Voice connectivity. It is meant to be fired
	 * when the Activity Resumes (or is Created) after checking the Network
	 * State is Active and Connectable.
	 * If the successful Voice object is connectable and instantiated, the setPhonesTask,
	 * another AsyncTask meant to populate the Phones choices, 
	 * is launched onPostExecute.  If the Network is Connectable, but the 
	 * Google Voice cannot Log In, the Credentials Activity is Launched
	 *
	 *
	 * @return the AsyncTask
	 */
	AsyncTask<Void, Void, Boolean> setVoiceTask(){ 
		AsyncTask<Void,Void,Boolean> ret = new AsyncTask<Void,Void,Boolean>(){


			@Override
			protected void onPreExecute()
			{
				if(isNetworkConnected())
				{
					return;
				}
				else
				{
					this.cancel(true);
					networkExit();
				}
			}
			@Override
			protected Boolean doInBackground(Void... arg0) {
				Log.v(TAG,"SET VOICE") ;
				return setVoice();

			}
			@Override
			protected void onPostExecute(Boolean result){
				if(!result){
					
		    		Intent intent = new Intent(Dialer.this,LoginCredentials.class);
		    		startActivity(intent);
		    		Dialer.this.finish();
				}else{
					setPhonesTask().execute();
				}

			}
		};
		return ret;
		
	}
	
	
	
	/**
	 * Sets the phones task 
	 *
	 * @return the async task
	 */
	AsyncTask<Void, Void, Boolean> setPhonesTask(){ 
		AsyncTask<Void,Void,Boolean> ret= new AsyncTask<Void,Void,Boolean>(){
			@Override
			protected void onPreExecute() {
				if(isNetworkConnected())
					return;
				else{
					this.cancel(true);
					networkExit();
				}
			}
			@Override
			protected Boolean doInBackground(Void... arg0) {	
				return setPhones();
			}
			@Override
			protected void onPostExecute(Boolean result){
				if(!result){
					Toast.makeText(Dialer.this, "SET PHONES FROM GOOGLE VOICE FAILED", Toast.LENGTH_LONG).show();
					launchDefaultDialer();

				}else{
					if(phoneStrings!=null && phoneStrings.length>0){
						ArrayAdapter<String> adapter = new ArrayAdapter<String>(Dialer.this,android.R.layout.simple_spinner_dropdown_item,phoneStrings);
						phoneSelect.setAdapter(adapter);
					}


				}

			}
		};
		return ret;
		
	}
	
	private void networkExit(){
		if(!isNetworkConnected()){
			launchDefaultDialer();
		}
	}
	
	
	/**
	 * Sets the phones.
	 *
	 * @return true, if successful
	 */
	private boolean setPhones(){
		boolean ret;
		if(voice!=null){
			try {
				AllSettings settings = voice.getSettings(false);
				phones = settings.getPhones();
				if(phones!=null && phones.length>0){

					phoneStrings = new String[phones.length];
					int i=0;
					for(Phone phone:phones){
						phoneStrings[i]=phone.getName();
						System.out.println(phoneStrings[i]);
						i++;
					}
					ret = true;
				}else{
					ret = false;
				}
			} catch (JSONException e) {
				ret = false;
				e.printStackTrace();
			} catch (IOException e) {
				//LAUNCH DEFAULT DIALER
				ret = false;
				e.printStackTrace();
			}catch(Exception h){
				//UNKNOWN ERROR
				ret = false;
				h.printStackTrace();
			}
		}else{
			ret = false;
		}
		return ret;
	}
	
	
	/**
	 * Launch default dialer.
	 */
	private void launchDefaultDialer(){
		Toast.makeText(getApplicationContext(), "ERROR CONNECTING TO GOOGLE VOICE  LAUCHING DEFAULT DIALER...", Toast.LENGTH_LONG).show();
		Log.e(TAG,"ERROR SETTING VOICE in DIALER - CHECK CONNECTIVITY??");
		Timer t = new Timer();
		TimerTask tt = new TimerTask(){

			@Override
			public void run() {
				Intent intent = new Intent(Intent.ACTION_DIAL);
				startActivity(intent);
				Dialer.this.finish();
			}
		};
		t.schedule(tt, 3000);
	}
	

	/**
	 * Sets the voice.  This Method should only be called inside a 
	 * BackgroundTask since it makes HTTP calls.
	 *
	 * @return true, if successful
	 */
	private boolean setVoice(){
		
		voice=null;
		try {
			voice = VoiceSingleton.getVoiceSingleton().getVoice();
			return true;
		} catch (Exception e1) {
			e1.printStackTrace();
			if(voice==null){
				VoiceSingleton.reset();
				SharedPreferences preferences = this.getSharedPreferences(Settings.SharedPrefKey.PREFERENCES, 0);
				String username = preferences.getString("username", "");
		    	String password = preferences.getString("password", "");
		    	if(!username.equals("")&&!password.equals("")){
		    		try {
						VoiceSingleton vs = VoiceSingleton.getOrCreateVoiceSingleton(username,Settings.decrypt(password, 10));
						voice = vs.getVoice();
						
		    		}catch (com.techventus.server.voice.exception.BadAuthenticationException ba){

		    			Intent i = new Intent(Dialer.this, LoginCredentials.class);
		    			preferences.edit().remove("username").remove("password").apply();
		    			startActivity(i);
					} catch (Exception e) {
						e.printStackTrace();
						return false;
						
						
					}
		    		return true;
		    	}else{
		    		return false;
		    	}
			}
		}
		return false;
	}
	
	/**
	 * Checks if is network connected.
	 *
	 * @return true, if is network connected
	 */
	private boolean isNetworkConnected(){
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
	
		if (ni!=null && ni.isConnected()){
			return true;
		}else{
			return false;
		}
	}
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		this.setContentView(R.layout.dialpad);
        mAdView= (AdView)this.findViewById(R.id.ad);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("TEST_DEVICE_ID")
                .build();
        mAdView.loadAd(adRequest);
		
		ContactLookup = (ImageView)this.findViewById(R.id.Contacts);
		
		button01 =(Button) this.findViewById( R.id.Button01);
		button01.setText(Html.fromHtml("1&nbsp;&nbsp;&nbsp;<small><small><small>&nbsp;&nbsp;&nbsp;</small></small></small>"));
		
		button02 =(Button) this.findViewById( R.id.Button02);
		button02.setText(Html.fromHtml("2&nbsp;<small><small><small>ABC</small></small></small>"));
		button03 =(Button) this.findViewById( R.id.Button03);
		button03.setText(Html.fromHtml("3&nbsp;<small><small><small>DEF</small></small></small>"));
		
		button04 =(Button) this.findViewById( R.id.Button04);
		button04.setText(Html.fromHtml("4&nbsp;<small><small><small>GHI</small></small></small>"));
		
		button05 =(Button) this.findViewById( R.id.Button05);
		button05.setText(Html.fromHtml("5&nbsp;<small><small><small>JKL</small></small></small>"));
		
		button06 =(Button) this.findViewById( R.id.Button06);
		button06.setText(Html.fromHtml("6&nbsp;<small><small><small>MNO</small></small></small>"));
		
		
		button07 =(Button) this.findViewById( R.id.Button07);
		button07.setText(Html.fromHtml("7&nbsp;<small><small><small>PQRS</small></small></small>"));
		
		button08 =(Button) this.findViewById( R.id.Button08);
		button08.setText(Html.fromHtml("8&nbsp;<small><small><small>TUV</small></small></small>"));
		
		button09 =(Button) this.findViewById( R.id.Button09);
		button09.setText(Html.fromHtml("9&nbsp;<small><small><small>WXYZ</small></small></small>"));
		
		button00 =(Button) this.findViewById( R.id.Button00);
		button00.setText(Html.fromHtml("0&nbsp;<small><small><small>+</small></small></small>"));
		
		buttonDel =(Button) this.findViewById( R.id.ButtonDel);
		buttonPound =(Button) this.findViewById( R.id.ButtonPound);
		buttonStar =(Button) this.findViewById( R.id.ButtonStar);
		
		CallButton = (Button)this.findViewById(R.id.CALLButton);
		
		phoneSelect = (Spinner)this.findViewById(R.id.phoneSelectSpinner);
		
		mDialNumber = (TextView)this.findViewById(R.id.Number);
		mDialNumber.setText("", BufferType.EDITABLE);
		
		phoneSelect.setOnItemSelectedListener(select);
		
		button00.setOnLongClickListener(longClick0);
		button01.setOnClickListener(tapNumber);
		button02.setOnClickListener(tapNumber);
		button03.setOnClickListener(tapNumber);
		button04.setOnClickListener(tapNumber);
		button05.setOnClickListener(tapNumber);
		button06.setOnClickListener(tapNumber);
		button07.setOnClickListener(tapNumber);
		button08.setOnClickListener(tapNumber);
		button09.setOnClickListener(tapNumber);
		button00.setOnClickListener(tapNumber);
		buttonDel.setOnClickListener(tapNumber);
		buttonDel.setOnLongClickListener(longDeleteClick);
		buttonPound.setOnClickListener(tapNumber);
		buttonStar.setOnClickListener(tapNumber);
		CallButton.setOnClickListener(tapNumber);
		
		ContactLookup.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				getNumber();
			}
			
		});
		
	}
	

    @Override
    public void onPause() {
    
        super.onPause();

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }
    }
	
	   @Override
	    protected void onDestroy() {
	        super.onDestroy();
	        synchronized (mToneGeneratorLock) {
	            if (mToneGenerator != null) {
	                mToneGenerator.release();
	                mToneGenerator = null;
	            }
	        }
	     
	    }
	

	    /**
	     * Plays the specified tone for TONE_LENGTH_MS milliseconds.
	     *
	     * The tone is played locally, using the audio stream for phone calls.
	     * Tones are played only if the "Audible touch tones" user preference
	     * is checked, and are NOT played if the device is in silent mode.
	     *
	     * @param tone a tone code from {@link ToneGenerator}
	     */
	   synchronized void  playTone(int tone) {
	    	
	    	try{
	        // if local tone playback is disabled, just return.
	        if (!mDTMFToneEnabled) {
	            return;
	        }

	        // Also do nothing if the phone is in silent mode.
	        // We need to re-check the ringer mode for *every* playTone()
	        // call, rather than keeping a local flag that's updated in
	        // onResume(), since it's possible to toggle silent mode without
	        // leaving the current activity (via the ENDCALL-longpress menu.)
	        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	        int ringerMode = audioManager.getRingerMode();
	        
	        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
	            || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
	            return;
	        }

			 synchronized (mToneGeneratorLock) {
		            if (mToneGenerator == null) {
		                try {
		                	
		                    mToneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF,
		                            TONE_RELATIVE_VOLUME);
		                    setVolumeControlStream(AudioManager.STREAM_DTMF);
		                } catch (RuntimeException e) {
		                    Log.w(TAG, "Exception caught while creating local tone generator: " + e);
		                    mToneGenerator = null;
		                }
		            }
		        }

	        synchronized (mToneGeneratorLock) {
	            if (mToneGenerator == null) {
	                Log.w(TAG, "playTone: mToneGenerator == null, tone: " + tone);
	                return;
	            }
	            
	        }
	        
	        
	        int curvol = audioManager.getStreamVolume(AudioManager.STREAM_DTMF);
	        System.out.println(vol);
	        //IF VOLUME HAS CHANGED, reINSTANTIATE THE OBJECT
	        if(vol!=curvol){
	        	
	        	vol=curvol;
                mToneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF,
                        TONE_RELATIVE_VOLUME);

	            if (mToneGenerator == null) {
	                Log.w(TAG, "playTone: mToneGenerator == null, tone: " + tone);
	                return;
	            }  
	        }
	            // Start the new tone (will stop any playing tone)
	            mToneGenerator.startTone(tone, TONE_LENGTH_MS);
	    	}catch(Exception d){
	    		d.printStackTrace();
	    	}
	    }
	
	/** The long click0. */
    OnLongClickListener longClick0 = new OnLongClickListener(){

		@Override
		public boolean onLongClick(View arg0) {
			addText('+');
			return true;
		}
		
	};
	
	/** The select. */
	OnItemSelectedListener select = new OnItemSelectedListener(){


		@SuppressWarnings("rawtypes")
		@Override
		public void onItemSelected(AdapterView arg0, View arg1, int arg2,
				long arg3) {
			System.out.println(arg0);
			System.out.println("Phone index "+arg2);
			if(phoneStrings!=null && phoneStrings.length>0){
				selectedPhone = phoneStrings[arg2];// PHONES[arg2];
				System.out.println(selectedPhone);
			}else{
				Log.e(TAG, "Item Selected whilst no phones available");
			}
		}

		@SuppressWarnings({  "rawtypes" })
		@Override
		public void onNothingSelected(AdapterView arg0) {
			Toast.makeText(Dialer.this, "NOTHING SELECTED", Toast.LENGTH_SHORT).show();
			
		}
		
	};
	
	/** The long delete click. */
	OnLongClickListener longDeleteClick = new OnLongClickListener(){

		@Override
		public boolean onLongClick(View v) {
			mDialNumber.setText("", BufferType.EDITABLE);
			return true;
		}
		
	};
	
	/** The tap number. */
	OnClickListener tapNumber = new OnClickListener(){

		@Override
		public void onClick(View arg0) {
			if(arg0.getId() ==  CallButton.getId()){
				Log.e(TAG, "CALL SELECTED");
				if(selectedPhone.equals(""))
					Log.e(TAG,"blank selectedPhone");
			}
			System.out.println(arg0.getId());
			if( arg0.getId() ==button00.getId()){ addText('0');  playTone(ToneGenerator.TONE_DTMF_0);}
			else if(arg0.getId() == button01.getId()){ addText('1'); playTone(ToneGenerator.TONE_DTMF_1); }
			else if(arg0.getId() ==  button02.getId()){ addText('2');  playTone(ToneGenerator.TONE_DTMF_2); }
			else if(arg0.getId() ==  button03.getId()){ addText('3');   playTone(ToneGenerator.TONE_DTMF_3);}
			else if(arg0.getId() ==  button04.getId()){ addText('4');  playTone(ToneGenerator.TONE_DTMF_4); }
			else if(arg0.getId() ==  button05.getId()){ addText('5');  playTone(ToneGenerator.TONE_DTMF_5); }
			else if(arg0.getId() ==  button06.getId()){ addText('6');  playTone(ToneGenerator.TONE_DTMF_6); }
			else if(arg0.getId() ==  button07.getId()){ addText('7');  playTone(ToneGenerator.TONE_DTMF_7); }
			else if(arg0.getId() ==  button08.getId()){ addText('8');  playTone(ToneGenerator.TONE_DTMF_8); }
			else if(arg0.getId() ==  button09.getId()){ addText('9');  playTone(ToneGenerator.TONE_DTMF_9); }
			else if(arg0.getId() ==  buttonDel.getId()){ delText(); }
			else if(arg0.getId() ==  buttonPound.getId()){ addText('#');  playTone(ToneGenerator.TONE_DTMF_P); }
			else if(arg0.getId() ==  buttonStar.getId()){ addText('*');   playTone(ToneGenerator.TONE_DTMF_S);}
			else if(arg0.getId() ==  CallButton.getId() && !selectedPhone.equals("")){ 
		
					
				if(!isNetworkConnected()){
					
					Toast.makeText(getApplicationContext(), "NETWORK NOT CONNECTED", Toast.LENGTH_LONG).show();
					launchDefaultDialer();
					return;
				}
				
				 final ProgressDialog dialog = ProgressDialog.show(Dialer.this, "", 
                        "Initiating Call. Please wait...", true,true);
				AsyncTask<Void,Void,Boolean> dialTask = new AsyncTask<Void, Void, Boolean>()
				{
					@Override
					protected Boolean doInBackground(Void... voids)
					{
						boolean dialBool =dial(selectedPhone, mDialNumber.getText().toString());
						return dialBool;
					}

					@Override
					protected void onPostExecute(Boolean dialBool)
					{
						if(!dialBool){
							try{
								dialog.dismiss();
							}catch(Exception e){
								e.printStackTrace();
							}
							Toast.makeText(Dialer.this, "ERROR PLACING CALL - CHECK CONNECTIVITY", Toast.LENGTH_LONG).show();
						}
					}
				}  ;
				dialTask.execute();

					
				TimerTask endTask = new TimerTask(){
					@Override
					public void run() {
						try{
							if(dialog.isShowing()){
								dialog.dismiss();
							}
						}catch(Exception e){
							e.printStackTrace();
						}
						Dialer.this.finish();
					}
				};
					
				//This is a somewhat arbitrary timer.  There is no
				//API callback mechanism to know when the call
				//is actually initiated.
				//We allocate this time for ProgressDialog as a rough estimate 
				//to appease the user as a usability feature
				
				Timer timer = new Timer();
				timer.schedule(endTask, 7000);
			}else if(arg0.getId() ==  CallButton.getId() && selectedPhone.equals("")){
				System.out.println("PHONE MUST BE SELECTED");
				if(phoneStrings == null || phoneStrings.length /*PHONES.length*/<1){
					Toast toast = Toast.makeText(getApplicationContext(), "Phone Must Be Selected\nCheck Google Conectivity.", Toast.LENGTH_SHORT);
					toast.show();
				}else{
					Toast toast = Toast.makeText(getApplicationContext(), "Phone Must Be Selected", Toast.LENGTH_SHORT);
					toast.show();
				}
			}
		}
	};
	
	/**
	 * Del text.
	 */
	void delText(){
		CharSequence start = mDialNumber.getText();
		String ret = start.toString();
		int len = ret.length();
		if(len>0){
			ret = ret.substring(0, len-1);
		}
		mDialNumber.setText(ret,BufferType.EDITABLE);
		mDialNumber.setInputType(InputType.TYPE_CLASS_PHONE);
	}
	
	/**
	 * Adds the text.
	 *
	 * @param newChar the new char
	 */
	void addText(char newChar){
		CharSequence start = mDialNumber.getText();
		String ret = start.toString();
		ret +=newChar;
		mDialNumber.setText(ret, BufferType.EDITABLE);
	}


	/**
	 * Gets the contact info.
	 *
	 * @param intent the intent
	 * @return the contact info
	 */
	protected void getContactInfo(Intent intent)
	{
	   String phoneNumber = "";
	   Cursor cursor =  managedQuery(intent.getData(), null, null, null, null);  
	   while (cursor.moveToNext()) 
	   {     
		   
	       String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

	       String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

	       if ( hasPhone.equalsIgnoreCase("1"))
	           hasPhone = "true";
	       else
	           hasPhone = "false" ;

	       if (Boolean.parseBoolean(hasPhone)) 
	       {
	        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId,null, null);
	        
	        List<String> numberList = new ArrayList<String>();
	        
	        
	        while (phones.moveToNext()) 
	        {
	        	
	          phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
	          numberList.add(phoneNumber);
	        
	        }
	        
	        final String[] numberAr = numberList.toArray(new String[numberList.size()]);
	        if(numberAr.length>1){
		        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        builder.setTitle("Which Phone");
		        builder.setItems(numberAr, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int item) {
		            	Log.e(TAG, "SETTING Number NOW "+numberAr[item]);
		            	 mDialNumber.setText(numberAr[item], BufferType.EDITABLE);
		            	// mDialNumber.
		            	 Log.e(TAG, mDialNumber.getText().toString());
		              }
		        });
		         builder.create().show();
		        
		        Toast.makeText(this, "Alert Shown", Toast.LENGTH_SHORT).show();
	        }else if (numberAr.length==1){
	        	mDialNumber.setText(numberAr[0]);
	        }
	       }

	  }
	   cursor.close();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent) 
	{
		if (requestCode == 10600  && resultCode ==-1)
		{         
			getContactInfo(intent); 
		}
	}
	
	/**
	 * Gets the number.
	 *
	 * @return the number
	 */
	public void getNumber(){
				int PICK_CONTACT = 10600;
				Intent intentContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI); 
				startActivityForResult(intentContact, PICK_CONTACT);
			
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override 
	public void onResume(){
		super.onResume();
		//TODO, Think about making a system setting for this feature
        mDTMFToneEnabled = true;
		if(isNetworkConnected()){
			if(phones==null || voice == null)
				setVoiceTask().execute();

		}else{
			Toast.makeText(getApplicationContext(), "NETWORK NOT CONNECTED", Toast.LENGTH_LONG).show();
			launchDefaultDialer();
		}
		
		
		 synchronized (mToneGeneratorLock) {
	            if (mToneGenerator == null) {
	                try {
	                    mToneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF,
	                            TONE_RELATIVE_VOLUME);
	                   // AudioManager.St
	                    setVolumeControlStream(AudioManager.STREAM_DTMF);
	                } catch (RuntimeException e) {
	                    Log.w(TAG, "Exception caught while creating local tone generator: " + e);
	                    mToneGenerator = null;
	                }
	            }
	        }
	}

	
	/**
	 * Dial.
	 *
	 * @param phoneString the phone string
	 * @param number the number
	 * @return true, if successful
	 */
	public boolean dial(String phoneString, String number) {

		if(voice!=null && phones!=null && phones.length>0 && !phoneString.equals("")){
			try {
				for(Phone phone:phones){
				   if(phone.getName().equals(phoneString)){
//					   System.out.println("CALLING "+phone.getPhoneNumber()+" "+number);
					   Log.v(TAG, "CALLING " + phone.getName() + " " + phone.getPhoneNumber() + " " + number) ;
					   voice.call(phone.getPhoneNumber(), number, String.valueOf(phone.getType()));
					   Log.v(TAG,"Done CALLING ") ;

					   return true;
				   }
				}
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
				return false;
			}
			return false;
		}else{
			//NULL VOICE OBJECT
			Toast.makeText(Dialer.this, "THERE WAS AN ERROR, Call Not Placed "+phoneString+" "+number+" null voice?"+(voice==null), Toast.LENGTH_LONG).show();
			Log.e(TAG, "THERE WAS AN ERROR, Call Not Placed "+phoneString+" "+number+" null voice?"+(voice==null));			  
			return false;
		}

	}

}		
