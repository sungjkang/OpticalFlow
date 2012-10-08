package com.wais.opticalflow;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.Window;

public class OpticalFlowActivity extends Activity {
	private View mView;
	
	
	private BaseLoaderCallback  mOpenCVCallBack = new BaseLoaderCallback(this) {
    	@Override
    	public void onManagerConnected(int status) {
    		switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{	
					// Load native library after(!) OpenCV initialization
					//System.loadLibrary("optical_flow");
					
					// Create and set View
					mView = new View(mAppContext);
					setContentView(mView);
					// Check native OpenCV camera
					if( !mView.openCamera() ) {
						AlertDialog ad = new AlertDialog.Builder(mAppContext).create();
			            ad.setCancelable(false); // This blocks the "BACK" button
			            ad.setMessage("Fatal error: can't open camera!");
			            ad.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int which) {
			                    dialog.dismiss();
			                    finish();
			                }
			            });
						ad.show();
					}
				} break;
				default:
				{
					super.onManagerConnected(status);
				} break;
			}
    	}
	};

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optical_flow);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack);
        //int temp = foo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_optical_flow, menu);
        return true;
        
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if (mView != null)
    		mView.releaseCamera();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if( mView != null && !mView.openCamera() ) {
            // MessageBox and exit app
            AlertDialog ad = new AlertDialog.Builder(this).create();
            ad.setCancelable(false); // This blocks the "BACK" button
            ad.setMessage("Fatal error: can't open camera!");
            ad.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            ad.show();
        }
    }
}
