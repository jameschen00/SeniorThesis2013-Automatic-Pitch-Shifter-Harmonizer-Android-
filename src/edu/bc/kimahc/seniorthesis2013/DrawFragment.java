package edu.bc.kimahc.seniorthesis2013;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DrawFragment extends Fragment{
	private int tab = 1;
	private GLSurfaceView surfaceView;
	private MyRenderer renderer;
	private long startTime;
	private long endTime, dt;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View v = inflater.inflate(R.layout.draw_fragment, container, false);

		surfaceView = (GLSurfaceView) v.findViewById(R.id.surfaceView); 
	    surfaceView.setEGLContextClientVersion(2);
	    renderer = new MyRenderer();
	    surfaceView.setRenderer(renderer);
	    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	    System.out.println("tab = " + tab);
		return v;
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
	
	public void sendProcessedBundle(Bundle bundle) {
		if(renderer != null && bundle != null){
			renderer.sendProcessedBundle(bundle);
			surfaceView.requestRender();
		}
	}

}
