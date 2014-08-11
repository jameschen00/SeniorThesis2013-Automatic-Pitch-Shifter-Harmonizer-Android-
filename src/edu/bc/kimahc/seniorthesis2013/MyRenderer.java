package edu.bc.kimahc.seniorthesis2013;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

public class MyRenderer implements Renderer {
	private DrawWaveform drawWaveform;
	private GlobalAppData global;
	private int tab;
	private int horizontalZoom;
	private int verticalZoom;
	private float[] audioBuffer = null;
	private float[] processedAudioBuffer = null;
	private boolean isProcessedDataReady = false;
	private boolean isDrawReady = false;
	private boolean surfaceReady = false;
	private long startTime, oldTime;
	private long endTime, dt;
	private long maxDelta;
	private long regDataTime = System.nanoTime();

	private long procDataTime = System.nanoTime();
	
	public MyRenderer() {
		global = GlobalAppData.getInstance();
		this.tab = global.getTab();
		this.horizontalZoom = global.getHorizontalZoom();
		this.verticalZoom = global.getVerticalZoom();
		startTime = System.nanoTime();
		maxDelta = (long) (1000f*(2048f/44100f)); // 46ms frames
	}
/*
	public void sendAudioData(float[] floatAudioBuffer){
		regDataTime = System.nanoTime();
		if(drawWaveform != null && surfaceReady){
			//this.audioBuffer = floatAudioBuffer;
			drawWaveform.sendAudioData(floatAudioBuffer);
			isDrawReady = true;
		}
		
	}
	*/
	public void sendProcessedAudioData(float[] processedAudioBuffer) {
		//procDataTime = System.nanoTime();
		//Log.i("processing time", ""+(procDataTime-regDataTime)/1000000l); //needs sendAudioData regular
		if(drawWaveform != null && surfaceReady){
			//this.processedAudioBuffer = processedAudioBuffer;
			drawWaveform.sendProcessedAudioData(processedAudioBuffer);
			//System.out.println("renderer fft section ready");
			isProcessedDataReady = true;
		}
	}
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        //isDrawReady = true;
        //drawWaveform = new DrawWaveform();
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
        drawWaveform = new DrawWaveform();
        surfaceReady = true;
    }
}