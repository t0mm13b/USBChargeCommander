package com.t0mm13b.UsbChargeCommander;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.preference.PreferenceManager;
import java.io.*;
/*
#ZTE_USB_CHG_LHX_001  20100221  begin
on property:persist.sys.charging.disable=1
    write /sys/module/msm_battery/parameters/usb_chg_enable 0


on property:persist.sys.charging.disable=0
    write /sys/module/msm_battery/parameters/usb_chg_enable 1
#ZTE_USB_CHG_LHX_001  20100221  end
*/
public class UsbChargeCommander extends Activity {
	private static String TAG = "UsbChargeCommander";
	private static final int QUITMSG = 0xDEADBEEF;
    /** Called when the activity is first created. */
	private AlertDialog _adChargeUSB;
	private Context _context;
	private int _iIsCharging = 0;
	private SharedPreferences _prefs;
	private Handler _adHandler;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this._context = this;
        this._prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this._iIsCharging = this._prefs.getInt(this.getString(R.string.prefsUSBChargePropertyKey), 0);
        //
        this.InitDialog();
        //
        this._adChargeUSB.show();
        //
        this._adHandler = new Handler(){
            @Override
		    public void handleMessage(Message m){
		        switch(m.what){
		            case UsbChargeCommander.QUITMSG :
                        setResult(RESULT_OK);
		                finish();
		                break;
		        }
		    }
	    };
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }
   	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
	}
	@Override
	public void onResume(){
		super.onResume();
	}

    private void InitDialog(){
    	Log.v(TAG, "[InitDialog] *** ENTER ***");
    	this._adChargeUSB = new AlertDialog.Builder(this._context)
    	.setCancelable(false)
		.setTitle(R.string.adChargeUSB_Title)
		.setPositiveButton(R.string.adChargeUSB_Close, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				SharedPreferences.Editor edPrefs = _prefs.edit();
				edPrefs.putInt(getString(R.string.prefsUSBChargePropertyKey), _iIsCharging);
				boolean rvCommittd = edPrefs.commit();
				if (rvCommittd) Toast.makeText(_context, (_iIsCharging == 0) ? getString(R.string.toastUSBChargingDisabled) : getString(R.string.toastUSBChargingEnabled), Toast.LENGTH_LONG);
				else Toast.makeText(_context, getString(R.string.toastPrefSavingError), Toast.LENGTH_LONG);
				// need to fork off thread here....
				//RunAsRoot(_iIsCharging);
				new rootAsync().execute(_iIsCharging);
				_adChargeUSB.dismiss();
			}
		})
		.setSingleChoiceItems(R.array.array_adChargeUSB, _iIsCharging, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked on a radio button do some stuff */
				switch(whichButton){
    				case 0: // Disable
    					_iIsCharging = 0;
    					break;
    				case 1: // Enable
    					_iIsCharging = 1;
    					break;
				}
            }    			
		})
		.create();
    	Log.v(TAG, "[InitDialog] *** LEAVE ***");
    }
    class rootAsync extends AsyncTask<Integer, Void, Integer>{
        @Override
        protected Integer doInBackground(Integer... unused){
            int nVal = unused[0];
            int nRv = 0;
        	java.lang.Process p;
        	try {
        	   // Preform su to get root privledges
        	   p = Runtime.getRuntime().exec(getString(R.string.cmdSU));
        	   DataOutputStream os = new DataOutputStream(p.getOutputStream());
        	   StringBuilder sb = new StringBuilder();
        	   sb.append(getString(R.string.cmdSU_setprop));
        	   sb.append(" ");
        	   sb.append(nVal);
        	   sb.append("\n");
        	   // Attempt to write a file to a root-only
        	   os.writeBytes(sb.toString());
        	   Log.v(TAG, "[RunAsRoot] - sb = " + sb.toString());
    
        	   // Close the terminal
        	   os.writeBytes(getString(R.string.cmdSU_exit));
        	   os.writeBytes("\n");
        	   os.flush();
        	   os.close();
        	   try {
        	      p.waitFor();
        	      if (p.exitValue() != 255){
        	    	  // TODO Code to run on success
        	    	  Log.v(TAG, "[RunAsRoot] - OK");
        	    	  nRv = 0;
    	    	  }else{
    	    		  // TODO Code to run on unsuccessful
        	    	  Log.v(TAG, "[RunAsRoot] - Failed");
        	    	  nRv = -1;
        		  }
        	   }catch (InterruptedException e) {
        	      // TODO Code to run in interrupted exception
     	    	  Log.v(TAG, "[RunAsRoot] - No root");
     	    	  nRv = -2;
        	   }
        	}catch(IOException e){
        	   // TODO Code to run in input/output exception
        		Log.v(TAG, "[RunAsRoot] - No root");
        		nRv = -3;
        	}
        	return nRv;
        }
        @Override
        protected void onPostExecute(Integer result){
            //
            super.onPostExecute(result);
            //
            String sMsg = "";
            if (result < -1) sMsg = getString(R.string.asyncTaskMinusOthersMsg);
            else{
                switch(result){
                    case 0 : 
                        sMsg = getString(R.string.asyncTaskZeroMsg);
                        break;
                    case -1 :
                        sMsg = getString(R.string.asyncTaskMinusOneMsg);
                        break;
                    default:
                        sMsg = getString(R.string.asyncTaskUnknownMsg);
                        break;
                }
            }
            Toast
                .makeText(UsbChargeCommander.this, sMsg, Toast.LENGTH_SHORT)
                .show();
           	Message m = new Message();
			m.what = UsbChargeCommander.QUITMSG;
			_adHandler.sendMessage(m);
        }
    }
}
