package com.wais.opticalflow;

import java.io.IOException;
import java.util.List;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class View extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "View";
    static {
		System.loadLibrary("opticalflow");
	}
	private int mFrameSize;
	private Bitmap mBitmap;
	private int[] mRGBA;

    private Camera              mCamera;
    private SurfaceHolder       mHolder;
    private int                 mFrameWidth;
    private int                 mFrameHeight;
    private byte[][]			mFrame;
    private boolean             mThreadRun;
    private byte[]              mBuffer;
    private int					frameIndex = 0;
    private boolean 			bufferIsSet = false;

    public View(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }


    public boolean openCamera() {
        Log.i(TAG, "openCamera");
        releaseCamera();
        mCamera = Camera.open();
        if(mCamera == null) {
        	Log.e(TAG, "Can't open camera!");
        	return false;
        }

        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (View.this) {
                    System.arraycopy(data, 0, mFrame[frameIndex], 0, data.length);
                    View.this.notify(); 
                }
                camera.addCallbackBuffer(mBuffer);
                if (frameIndex == 1) {
                	bufferIsSet = true;
                }
                frameIndex ^= 1;
                
            }
        });
        return true;
    }
    
    public void releaseCamera() {
        Log.i(TAG, "releaseCamera");
        mThreadRun = false;
        synchronized (this) {
	        if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
        onPreviewStopped();
    }
    
    public void setupCamera(int width, int height) {
        Log.i(TAG, "setupCamera");
        synchronized (this) {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();
                mFrameWidth = width;
                mFrameHeight = height;

                // selecting optimal camera preview size
                {
                    //int  minDiff = Integer.MAX_VALUE;
                    for (Camera.Size size : sizes) {
//                        if (Math.abs(size.height - height) < minDiff) {
//                            mFrameWidth = size.width;
//                            mFrameHeight = size.height;
//                            minDiff = Math.abs(size.height - height);
//                        }
                    	
                    	//picking the smallest camera size for fast opencv action
                    	if (size.height < height) {
                    		mFrameWidth = size.width;
                    		mFrameHeight = size.height;
                    		height = size.height;
                    	}
                    }
                }
                
                params.setPreviewSize(getFrameWidth(), getFrameHeight());
                
                List<String> FocusModes = params.getSupportedFocusModes();
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                {
                	params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }            
                
                mCamera.setParameters(params);
                
                /* Now allocate the buffer */
                params = mCamera.getParameters();
                int size = params.getPreviewSize().width * params.getPreviewSize().height;
                size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                mBuffer = new byte[size];
                /* The buffer where the current frame will be copied */
                mFrame = new byte [2][size];
                mCamera.addCallbackBuffer(mBuffer);

    			try {
    				mCamera.setPreviewDisplay(null);
    			} catch (IOException e) {
    				Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
    			}

                /* Notify that the preview is about to be started and deliver preview size */
                onPreviewStarted(params.getPreviewSize().width, params.getPreviewSize().height);

                /* Now we can start a preview */
                mCamera.startPreview();
            }
        }
    }
    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        setupCamera(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        releaseCamera();
    }

    public native void opticalFlow(int width, int height, byte[] frame1, byte[] frame2, int[] rgba);

    protected Bitmap processFrame(byte[] prev, byte[] curr) {
        int[] rgba = mRGBA;

        //findFeatures(getFrameWidth(), getFrameHeight(), prev, rgba);
        opticalFlow(getFrameWidth(), getFrameHeight(), prev, curr, rgba);
        
        Bitmap bmp = mBitmap; 
        bmp.setPixels(rgba, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());
        return bmp;
    }


    /**
     * This method is called when the preview process is being started. It is called before the first frame delivered and processFrame is called
     * It is called with the width and height parameters of the preview process. It can be used to prepare the data needed during the frame processing.
     * @param previewWidth - the width of the preview frames that will be delivered via processFrame
     * @param previewHeight - the height of the preview frames that will be delivered via processFrame
     */
    protected void onPreviewStarted(int previewWidtd, int previewHeight) {
		mFrameSize = previewWidtd * previewHeight;
		mRGBA = new int[mFrameSize];
		mBitmap = Bitmap.createBitmap(previewWidtd, previewHeight, Bitmap.Config.ARGB_8888);
	}
    /**
     * This method is called when preview is stopped. When this method is called the preview stopped and all the processing of frames already completed.
     * If the Bitmap object returned via processFrame is cached - it is a good time to recycle it.
     * Any other resources used during the preview can be released.
     */
	protected void onPreviewStopped() {
		if(mBitmap != null) {
			mBitmap.recycle();
			mBitmap = null;
		}
		mRGBA = null;	
	}

    public void run() {
        mThreadRun = true;
        //Log.i(TAG, "Starting processing thread");
        while (mThreadRun) {
        	Bitmap bmp = null;
        	if (bufferIsSet) {
        	

	            
	        	synchronized (this) {
	                try {
	                    this.wait();
	                    bmp = processFrame(mFrame[frameIndex^1], mFrame[frameIndex]);
	                } catch (InterruptedException e) {
	                    e.printStackTrace();
	                }
	            }
        	}
            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    //canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
					Matrix matrix = new Matrix();
					matrix.preTranslate((canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2);
					if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
					matrix.postRotate(90f,(canvas.getWidth()) / 2,(canvas.getHeight()) / 2);
					canvas.drawBitmap(bmp, matrix, new Paint());
					mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}