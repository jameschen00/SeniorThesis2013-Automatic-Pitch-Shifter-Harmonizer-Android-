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

public class PitchFragment extends Fragment{
	private static final String tag = "PitchFrag";
	private GLSurfaceView surfaceView;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		surfaceView = (GLSurfaceView) getView().findViewById(R.id.surfaceView); 

	    surfaceView.setEGLContextClientVersion(2);

	    //getActivity().setContentView(surfaceView);
		
		//surfaceView.setRenderer(new OpenGLRenderer());
		
	}
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		return inflater.inflate(R.layout.pitch_fragment, container, false);
		
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
}
