package edu.bc.kimahc.seniorthesis2013;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class DrawFragment extends Fragment{
	private int tab = 1;
	private GLSurfaceView surfaceView;
	private MyRenderer renderer;
	private int horizontalZoom = 50;
	private int verticalZoom = 50;
	private long startTime;
	private long endTime, dt;
	
	public void onCreate(Bundle savedInstanceState){
		startTime = System.nanoTime();
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
            tab = savedInstanceState.getInt("tab"); //set the tab as per the saved state
            horizontalZoom = savedInstanceState.getInt("horizontalZoom");
            verticalZoom = savedInstanceState.getInt("verticalZoom");
        }
		
	}
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View v = inflater.inflate(R.layout.draw_fragment, container, false);

		surfaceView = (GLSurfaceView) v.findViewById(R.id.surfaceView); 
	    surfaceView.setEGLContextClientVersion(2);
	    renderer = new MyRenderer(tab, horizontalZoom, verticalZoom);
	    surfaceView.setRenderer(renderer);
	    //surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	    System.out.println("tab = " + tab);
		return v;
	}
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("tab", tab); //save the tab selected
        outState.putInt("horizontalZoom", horizontalZoom);
        outState.putInt("verticalZoom", verticalZoom);
        super.onSaveInstanceState(outState);
    }
    public void setTab(String tabString){
	    if(tabString.equalsIgnoreCase("tab1")){
	    	tab = 1;
	    	renderer.setTab(tab);
	    }
	    else if(tabString.equalsIgnoreCase("tab2")){
	    	tab = 2;
	    	renderer.setTab(tab);
	    }
	    else if(tabString.equalsIgnoreCase("tab3")){
	    	tab = 3;
	    	renderer.setTab(tab);
	    }
    }
    
	public void setHorizontalZoom(int progress) {
		horizontalZoom = progress;
		if(renderer != null)
			renderer.setHorizontalZoom(horizontalZoom);
	}
	
	public void setVerticalZoom(int progress) {
		verticalZoom = progress;
		if(renderer != null)
			renderer.setVerticalZoom(verticalZoom);
	}
	
	@Override
	public void onResume()
	{
	    // The activity must call the GL surface view's onResume() on activity onResume().
	    super.onResume();
	    surfaceView.onResume();
	}
	 
	@Override
	public void onPause()
	{
	    // The activity must call the GL surface view's onPause() on activity onPause().
	    super.onPause();
	    surfaceView.onPause();
	}
	
	public void sendAudioData(float[] floatAudioBuffer){
		if(renderer != null && floatAudioBuffer != null){
/*
	        endTime = System.nanoTime();
	        dt = endTime - startTime;
	        System.out.println("dt = " + dt);
			startTime = System.nanoTime();
			
			long rStartTime = System.nanoTime();
			
			renderer.sendAudioData(audioBuffer);
			
	        long rEndTime = System.nanoTime();
	        dt = rEndTime - rStartTime;
	        System.out.println("send data = " + dt);
			
			surfaceView.requestRender();
			*/
			renderer.sendAudioData(floatAudioBuffer);
		}
			
	}
/*
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		v.onSaveInstanceState();
		if(v.onSaveInstanceState(outState) == null)
			Log.i(tag,"Saving state FAILED!");
		else
			Log.i(tag, "Saving state succeeded.");      
	}
	
	
	public void onRestoreInstanceState(Parcelable state){
		
	}
*/
	public void sendProcessedAudioData(float[] processedAudioBuffer) {
		if(renderer != null && processedAudioBuffer != null){
			renderer.sendProcessedAudioData(processedAudioBuffer);
		}
	}

}
