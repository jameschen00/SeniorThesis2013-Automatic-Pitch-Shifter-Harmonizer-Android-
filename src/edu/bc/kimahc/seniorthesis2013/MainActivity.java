package edu.bc.kimahc.seniorthesis2013;

import java.util.HashMap;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import edu.bc.kimahc.seniorthesis2013.MainActivity.TabInfo;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
/*
 * For locking,

activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
For unlocking,

activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
where "activity" is your current Activity, in which you want to lock.

 */
public class MainActivity extends FragmentActivity implements TabHost.OnTabChangeListener, SeekBar.OnSeekBarChangeListener{
	private TabHost mTabHost;
	private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, TabInfo>();
	private static String drawFragmentTag = "drawFragment";
	private static AudioThread audioThread;
	//private static AudioProcessingThread audioProcessingThread;
	private static DrawFragment drawFrag = null;
	private static long audiotime = 0;
	private static long time = 0;
	private static long postproc = 0;
    private SeekBar horizontalZoomBar;
    private SeekBar verticalZoomBar;
    private int defaultHorizontalZoom = 50;
    private int defaultVerticalZoom = 50;
    private GlobalAppData global = GlobalAppData.getInstance();;
	private TextView shiftTextView;
	private CheckBox liveCheckBox;
	private CheckBox combineCheckBox;
	private CheckBox filterCheckBox;
	private TextView horZoomText;
	
	private static final Handler audioHandler = new Handler() {
		  @Override
		  public void handleMessage(Message msg) {
			  if(drawFrag != null){
				  Bundle bundle = msg.getData();
				  float[] processedAudioBuffer = bundle.getFloatArray("aBuffer");
				  audiotime = bundle.getLong("audiotime");
				  time = System.nanoTime();
				  System.out.println("handler: " + (time - audiotime)/1000000l);
				  if(processedAudioBuffer !=null){
					  //drawFrag.sendAudioData(floatAudioBuffer);
					  //if(audioProcessingThread != null)
						//  processedAudioBuffer = audioProcessingThread.processData(floatAudioBuffer);
					  drawFrag.sendProcessedAudioData(processedAudioBuffer);
				  }
				  //int count = bundle.getInt("countint");
				  //System.out.println("mainactivity count: " + count);
				  
				  //time = System.nanoTime();
				  //System.out.println("post proc handler delay: " + (time - audiotime)/1000000l);
				  //System.out.println("mainactivity time diff: " + (time - audiotime) + "    count= " + count);
				 
			  }
		     }
		 };
		 /*
	private static final Handler audioProcessingHandler = new Handler() {
		  @Override
		  public void handleMessage(Message msg) {
			  postproc = System.nanoTime();
			  Log.i("data to proc (handler to handler)", ""+(postproc - time)/1000000l);
			  if(drawFrag != null){
				  Bundle bundle = msg.getData();
				  float[] processedAudioBuffer = bundle.getFloatArray("paBuffer");
				  long startTime = bundle.getLong("processTime");

			        long endTime = System.nanoTime();
			        Log.i("proc handler delay = ", ""+(endTime-startTime)/1000000l);
				  if(processedAudioBuffer != null)	
					  drawFrag.sendProcessedAudioData(processedAudioBuffer);
				 
			  }
		     }
		 };		 
		 */
	public class TabInfo {
		 private String tag;
         private Class<?> clss;
         private Bundle args;
         private Fragment fragment;
         TabInfo(String tag, Class<?> clazz, Bundle args) {
        	 this.tag = tag;
        	 this.clss = clazz;
        	 this.args = args;
         }

	}

	class TabFactory implements TabContentFactory {

		private final Context mContext;

	    /**
	     * @param context
	     */
	    public TabFactory(Context context) {
	        mContext = context;
	    }

	    /** (non-Javadoc)
	     * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
	     */
	    public View createTabContent(String tag) {
	        View v = new View(mContext);
	        v.setMinimumWidth(0);
	        v.setMinimumHeight(0);
	        return v;
	    }

	}
	/** (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Step 1: Inflate layout
		setContentView(R.layout.activity_main);
		// Step 2: Setup TabHost
		initialiseTabHost(savedInstanceState);
        horizontalZoomBar = (SeekBar)findViewById(R.id.horZoom);
        verticalZoomBar = (SeekBar)findViewById(R.id.verZoom);
        horizontalZoomBar.setOnSeekBarChangeListener(this);
        verticalZoomBar.setOnSeekBarChangeListener(this);
        shiftTextView = (TextView) findViewById(R.id.shiftAmount);
        liveCheckBox = (CheckBox) findViewById(R.id.liveCheck);
        global.setLive(liveCheckBox.isChecked());
        combineCheckBox = (CheckBox) findViewById(R.id.combineCheck);
        global.setCombine(combineCheckBox.isChecked());
        filterCheckBox = (CheckBox) findViewById(R.id.filterCheck);
        global.setFilter(filterCheckBox.isChecked());
        horZoomText = (TextView) findViewById(R.id.horZoomText);
       // 
        //final Button pauseButton = (Button) findViewById(R.id.pauseButton);
        //pauseButton.setOnClickListener(new View.OnClickListener())
		
		if (savedInstanceState != null) {
			String tab = savedInstanceState.getString("tab");
			int horizontalZoom = savedInstanceState.getInt("horizontalZoom");
			int verticalZoom = savedInstanceState.getInt("verticalZoom");
			int shiftAmount = savedInstanceState.getInt("shiftAmount");
			global.setTab(convertTabtagToInt(tab));
			global.setHorizontalZoom(horizontalZoom);
			global.setVerticalZoom(verticalZoom);
			global.setShiftAmount(shiftAmount);
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
            horizontalZoomBar.setProgress(horizontalZoom);
            verticalZoomBar.setProgress(verticalZoom);
            horZoomText.setText(Integer.toString((int) (global.getAudioBufferLength() * (float)(horizontalZoom+1) / 101f)));
		}
		else{
			global.setTab(1);
			horizontalZoomBar.setProgress(defaultHorizontalZoom);
			verticalZoomBar.setProgress(defaultVerticalZoom);
			global.setHorizontalZoom(defaultHorizontalZoom);
			global.setVerticalZoom(defaultVerticalZoom);
			global.setShiftAmount(Integer.parseInt(shiftTextView.getText().toString()));
			horZoomText.setText(Integer.toString((int) (global.getAudioBufferLength() * (float)(defaultHorizontalZoom+1) / 101f)));
		}
	}
	
	protected void onResume(){
		super.onResume();
		audioThread = new AudioThread(audioHandler);
		//audioProcessingThread = new AudioProcessingThread(audioProcessingHandler);
		Log.i("state?", audioThread.getState().toString());
		if(audioThread.getState() == Thread.State.NEW){
			Log.i("starting", "starting new recording thread from onResume!");
			audioThread.start();
			}
		else if(audioThread.getState() == Thread.State.TERMINATED){
			Log.i("restarting", "REstarting new recording thread from onResume!!!!");
			audioThread = null;
			audioThread = new AudioThread(audioHandler);
			audioThread.start();
		}/*
		if(audioProcessingThread.getState() == Thread.State.NEW){
			Log.i("starting", "starting new processing thread from onResume!");
			audioProcessingThread.start();
			}
		else if(audioProcessingThread.getState() == Thread.State.TERMINATED){
			Log.i("restarting", "REstarting new processing thread from onResume!!!!");
			audioProcessingThread = null;
			audioProcessingThread = new AudioProcessingThread(audioProcessingHandler);
			audioProcessingThread.start();
		}*/
	}
	

    /*
    public void onStop(){
    	Log.i("stopping", "onSTOP!!!");
    	super.onStop();
    	audioThread.pauseRecording();
    }
    */
    
    public void onPause(){
    	Log.i("pausing", "onPAUSE!!!");
    	super.onPause();
    	audioThread.killThread();
    	audioThread.onResume();
    	audioThread = null;
    	
    	//audioProcessingThread.killThread();
    	//audioProcessingThread = null;
    	//audioThread.onPause();
    	
    }
    /*
    public void onDestroy(){
    	
    	Log.i("destroy", "onDestroy!!!");
    	super.onDestroy();
    	audioThread.killThread();
    	audioThread.onResume(); //free thread from lock
    	audioThread = null;
    	/*
    	 * 	protected void onDestroy() {
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
		
	}

    	 
    }
    */
	/*
	@Override
	public void onConfigurationChanged(Configuration cfg) {
	    super.onConfigurationChanged(cfg);
	    setContentView(R.layout.activity_main);
	}
	/** (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
     */
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        outState.putInt("horizontalZoom", horizontalZoomBar.getProgress());
        outState.putInt("verticalZoom", verticalZoomBar.getProgress());
        outState.putInt("shiftAmount", Integer.parseInt(shiftTextView.getText().toString()));
        super.onSaveInstanceState(outState);
    }

	/**
	 * Step 2: Setup TabHost
	 */
	private void initialiseTabHost(Bundle args) {
		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        TabInfo tabInfo = null;
        MainActivity.addTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab1").setIndicator("Waveform"), ( tabInfo = new TabInfo("Tab1", DrawFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        MainActivity.addTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab2").setIndicator("FFT"), ( tabInfo = new TabInfo("Tab2", DrawFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        MainActivity.addTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab3").setIndicator("AC"), ( tabInfo = new TabInfo("Tab3", DrawFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        MainActivity.addTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab4").setIndicator("Pitch"), ( tabInfo = new TabInfo("Tab4", DrawFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        // Default to first tab
        this.onTabChanged("Tab1");
        //
        mTabHost.setOnTabChangedListener(this);
	}

	/**
	 * @param activity
	 * @param tabHost
	 * @param tabSpec
	 * @param clss
	 * @param args
	 */
	private static void addTab(MainActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
		// Attach a Tab view factory to the spec
		tabSpec.setContent(activity.new TabFactory(activity));
        
		//String tag = tabSpec.getTag();

        // Check to see if we already have a fragment for this tab, probably
        // from a previously saved state.  If so, deactivate it, because our
        // initial state is that a tab isn't shown.
        tabInfo.fragment = activity.getSupportFragmentManager().findFragmentByTag(drawFragmentTag);
        /*
        if (tabInfo.fragment != null && !tabInfo.fragment.isDetached()) {
        	System.out.println("detached");
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.detach(tabInfo.fragment);
            ft.commit();
            activity.getSupportFragmentManager().executePendingTransactions();
        }
        */

        tabHost.addTab(tabSpec);
	}

	/** (non-Javadoc)
	 * @see android.widget.TabHost.OnTabChangeListener#onTabChanged(java.lang.String)
	 */
	public void onTabChanged(String tag) {
		TabInfo newTab = (TabInfo) mapTabInfo.get(tag);
		if (this.getSupportFragmentManager().findFragmentByTag(drawFragmentTag) == null) {
			FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
            if (newTab != null) {
                if (newTab.fragment == null) {
                    newTab.fragment = Fragment.instantiate(this,
                            newTab.clss.getName(), newTab.args);
                    ft.add(R.id.realtabcontent, newTab.fragment, drawFragmentTag);
                } else {
                    ft.attach(newTab.fragment);
                }
            }
            ft.commit();
            getSupportFragmentManager().executePendingTransactions();
		}
		
		drawFrag = (DrawFragment) this.getSupportFragmentManager().findFragmentByTag(drawFragmentTag);
		if(drawFrag != null&&drawFrag.isResumed()){
			//drawFrag.setTab(tag);
			
			//System.out.println("tab tag = " + tag);
		}else{
			//System.out.println("wtf");
		}
		global.setTab(convertTabtagToInt(tag));
    }
	
    public static float[] convertShortArrayToFloatArray(short[] shortArray)
    {
    	float[] floatArray = new float[shortArray.length];
        for (int i = 0; i < shortArray.length; i++)
        	floatArray[i] = (float)(shortArray[i]/32768f);
        return floatArray;
    }

    public int convertTabtagToInt(String tab){
    	return tab.charAt(3)-'0';
    }


    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
    	if(seekBar.equals(horizontalZoomBar)){
    		global.setHorizontalZoom(progress);
    		horZoomText.setText(Integer.toString((int) (global.getAudioBufferLength() * (float)(progress+1) / 101f)));
    	}
    	if(seekBar.equals(verticalZoomBar))
    		global.setVerticalZoom(progress);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        
    }
    
    public void pauseDraw(View view) {
        global.setPause();
    }
    
    public void subShift(View view) {
    	int shift = Integer.parseInt(shiftTextView.getText().toString());
    	shift--;
    	shiftTextView.setText(Integer.toString(shift));
    	global.setShiftAmount(shift);
    }
    
    public void addShift(View view) {
    	int shift = Integer.parseInt(shiftTextView.getText().toString());
    	shift++;
    	shiftTextView.setText(Integer.toString(shift));
    	global.setShiftAmount(shift);
    }
    
    public void toggleRecord(View view) {
    	if(audioThread.isAlive()){
	        // Is the toggle on?
	        boolean on = ((ToggleButton) view).isChecked();
	        
	        if (on && !global.getLive()) {
	            audioThread.startWriteFile(); //start writing to file if live is not on
	        } else {
	        	audioThread.stopWriteFile(); //must try to stop writing file if started, regardless of what the live checkbox is now
	        }
    	}
    }
    
    public void toggleLive(View view){
    	global.setLive(((CheckBox) view).isChecked());
    }
    
    public void toggleCombine(View view){
    	global.setCombine(((CheckBox) view).isChecked());
    }
    
    public void toggleFilter(View view){
    	global.setFilter(((CheckBox) view).isChecked());
    }
}