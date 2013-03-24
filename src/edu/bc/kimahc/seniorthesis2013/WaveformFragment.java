package edu.bc.kimahc.seniorthesis2013;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class WaveformFragment extends Fragment{
	private TextView tv;
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//setRetainInstance(true);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){

		
		View v = inflater.inflate(R.layout.waveform_fragment, container, false);
		tv = (TextView) v.findViewById(R.id.waveformText);
		if (savedInstanceState != null) {
            tv.setText(savedInstanceState.getString("text")); //set the text as per the saved state
        }
		return v;
		
	}
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("text", (String) tv.getText()); //save the text selected
        super.onSaveInstanceState(outState);
    }
    
	public void setText(String text){
		tv.setText(text);
	}
}
