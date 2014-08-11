package edu.bc.kimahc.seniorthesis2013;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.util.Log;

public class MyRenderer implements Renderer {
	private DrawClass drawWaveform;
	private GlobalAppData global;
	private boolean surfaceReady = false;
	private long startTime, oldTime;
	private long endTime, dt;
	private long maxDelta;
	private long regDataTime = System.nanoTime();

	private long procDataTime = System.nanoTime();
	
	public MyRenderer() {
		global = GlobalAppData.getInstance();
		startTime = System.nanoTime();
		maxDelta = (long) (1000f*(2048f/44100f)); // 46ms frames
	}

	public void sendProcessedBundle(Bundle bundle) {
		if(drawWaveform != null && surfaceReady){
			drawWaveform.sendProcessedBundle(bundle);
		}
	}

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
        /*
     // Set the background color to black ( rgba ).
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);  // OpenGL docs.
     		// Enable Smooth Shading, default not really needed.
        GLES20.glShadeModel(GL10.GL_SMOOTH);// OpenGL docs.
     		// Depth buffer setup.
        GLES20.glClearDepthf(1.0f);// OpenGL docs.
     		// Enables depth testing.
        GLES20.glEnable(GL10.GL_DEPTH_TEST);// OpenGL docs.
     		// The type of depth testing to do.
        GLES20.glDepthFunc(GL10.GL_LEQUAL);// OpenGL docs.
     		// Really nice perspective calculations.
        GLES20.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, // OpenGL docs.
                               GL10.GL_NICEST);
                               */
    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
    	
    	
    	
    	
	        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
	        
	        endTime = System.nanoTime();
	        dt = (long) ((endTime - startTime)/1000000l); //dt in ms
	        /*
	        if(dt < maxDelta){
				try {
					Thread.sleep(maxDelta - dt);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	        */
	        //System.out.println("dt = " + dt);
	
	        oldTime = startTime;
			startTime = System.nanoTime();
			//System.out.println("draw frames = " + (startTime-oldTime)/1000000l);
			
			drawWaveform.draw();

    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        drawWaveform = new DrawClass();
        surfaceReady = true;
    }
}