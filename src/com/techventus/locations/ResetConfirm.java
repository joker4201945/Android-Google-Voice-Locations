package com.techventus.locations;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class ResetConfirm extends Activity
{
	
	String TAG = "TECHVENTUS - RestConfirm";

	Button confirmResetButton;
	Button cancelResetButton;
	

	GVLServiceInterface mIRemoteService;
	private ServiceConnection mConnection = new ServiceConnection()
	{
	    // Called when the connection with the service is established
	    public void onServiceConnected(ComponentName className, IBinder service)
	    {
	        // Following the example above for an AIDL interface,
	        // this gets an instance of the IRemoteInterface, which we can use to call on the service
	        mIRemoteService = GVLServiceInterface.Stub.asInterface(service);
	    }

	    // Called when the connection with the service disconnects unexpectedly
	    public void onServiceDisconnected(ComponentName className)
	    {
	        Log.e(TAG, "Service has unexpectedly disconnected");
	        mIRemoteService = null;
	    }
	};

	AdView mAdView;
	
	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ); 
		setContentView(R.layout.resetconfirm);

		mAdView= (AdView)this.findViewById(R.id.ad);
		AdRequest adRequest = new AdRequest.Builder()
				.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
				.addTestDevice("TEST_DEVICE_ID")
				.build();
		mAdView.loadAd(adRequest);
		
		
		Intent hello_service = new Intent(this, BackgroundService2.class);
	    
		bindService( hello_service, mConnection,Context.BIND_AUTO_CREATE);
		
		 //TODO MAKE A RESET BROADCAST INTENT RATHER THAN
		
		confirmResetButton = (Button)findViewById(R.id.confirmReset);
		cancelResetButton  = (Button)findViewById(R.id.cancelReset);
		
		confirmResetButton.setOnClickListener(confirmResetClick);
		cancelResetButton.setOnClickListener(cancelResetClick);
		
		
	}
	
	OnClickListener confirmResetClick = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			if(mIRemoteService!=null)
			{
				try
				{
					mIRemoteService.reset();
				} catch (RemoteException e1)
				{
					e1.printStackTrace();
				}
			}else{
				Toast.makeText(ResetConfirm.this, "No Connection to Background Service", Toast.LENGTH_SHORT);
			}
			

			ResetConfirm.this.finish();
		}
		
	};
	
	OnClickListener cancelResetClick = new OnClickListener()
    {

		@Override
		public void onClick(View arg0)
        {
			finish();
		}
		
	};
	
	
	

	@Override 
	public void onResume()
    {
		super.onResume();
		
	    Intent hello_service = new Intent(this, BackgroundService2.class);
	    
		bindService( hello_service, mConnection,Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onPause(){
		try{
			unbindService(mConnection);
		}catch(Exception e){
			e.printStackTrace();
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		mAdView.destroy();
		super.onDestroy();
	}

	
}
