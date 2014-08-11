package edu.bc.kimahc.seniorthesis2013;

import java.util.HashMap;


import Music.Tones;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    private SeekBar combineBar;
    private int defaultHorizontalZoom = 50;
    private int defaultVerticalZoom = 50;
    private int defaultCombine = 50;
    private GlobalAppData global = GlobalAppData.getInstance();;
	private TextView shiftTextView;
	private CheckBox autotuneCheckBox;
	private CheckBox filterCheckBox;
	private TextView horZoomText;
	private TextView combineText;
	private TextView freqText;
	private TextView noteText;
	private TextView shiftedNoteText;
	
	private final Handler audioHandler = new Handler() {
		  @Override
		  public void handleMessage(Message msg) {
			  if(drawFrag != null){
				  Bundle bundle = msg.getData();
				  audiotime = bundle.getLong("audiotime");
				  time = System.nanoTime();
				  //Log.i("handler", "" +(time - audiotime)/1000000l);
				  float freq = bundle.getFloat("freq");
				  String note = Tones.getFullNoteString(freq);
				  int shift = bundle.getInt("shift");
				  float shiftedFreq = (float) Math.pow(2, ((float)shift/12f)) * freq;
				  String shiftedNote = Tones.getFullNoteString(shiftedFreq);
				  String freqString;
				  if(freq > 0){
					  freqString = String.format("%.1f", freq);
				  }
				  else{
					  freqString = "n/a";
				  }
				  
				  freqText.setText(freqString);
				  noteText.setText(note);
				  shiftedNoteText.setText(shiftedNote);
				  
				  
				  if(bundle !=null){
					  drawFrag.sendProcessedBundle(bundle);
				  }

			  }
		     }
		 };

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
        combineBar = (SeekBar)findViewById(R.id.combineBar);
        horizontalZoomBar.setOnSeekBarChangeListener(this);
        verticalZoomBar.setOnSeekBarChangeListener(this);
        combineBar.setOnSeekBarChangeListener(this);
        shiftTextView = (TextView) findViewById(R.id.shiftAmount);
        autotuneCheckBox = (CheckBox) findViewById(R.id.autotuneCheck);
        global.setAutotune(autotuneCheckBox.isChecked());
        filterCheckBox = (CheckBox) findViewById(R.id.filterCheck);
        global.setFilter(filterCheckBox.isChecked());
        horZoomText = (TextView) findViewById(R.id.horZoomText);
        combineText = (TextView) findViewById(R.id.combineText);
        freqText = (TextView) findViewById(R.id.freqNum);
        noteText = (TextView) findViewById(R.id.noteText);
        shiftedNoteText = (TextView) findViewById(R.id.shiftedNoteText);
       // 
        //final Button pauseButton = (Button) findViewById(R.id.pauseButton);
        //pauseButton.setOnClickListener(new View.OnClickListener())
		
		if (savedInstanceState != null) {
			String tab = savedInstanceState.getString("tab");
			int horizontalZoom = savedInstanceState.getInt("horizontalZoom");
			int verticalZoom = savedInstanceState.getInt("verticalZoom");
			int shiftAmount = savedInstanceState.getInt("shiftAmount");
			int combine = savedInstanceState.getInt("combine");
			int key = savedInstanceState.getInt("key");
			boolean manual = savedInstanceState.getBoolean("manual");
			global.setTab(convertTabtagToInt(tab));
			global.setHorizontalZoom(horizontalZoom);
			global.setVerticalZoom(verticalZoom);
			global.setShiftAmount(shiftAmount);
			global.setCombine(combine);
			global.setManual(manual);
			global.setKey(key);
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
            horizontalZoomBar.setProgress(horizontalZoom);
            verticalZoomBar.setProgress(verticalZoom);
            combineBar.setProgress(combine);
            horZoomText.setText(Integer.toString((int) (global.getAudioBufferLength() * (float)(horizontalZoom+1) / 101f)));
            combineText.setText(Integer.toString(combine) + "% orig");
            
            if(manual){
            	shiftTextView.setText(Integer.toString(shiftAmount));
            }
            else{
            	shiftTextView.setText(Tones.getNoteFromPitch(key));
            }
		}
		else{
			global.setTab(1);
			horizontalZoomBar.setProgress(defaultHorizontalZoom);
			verticalZoomBar.setProgress(defaultVerticalZoom);
			combineBar.setProgress(defaultCombine);
			global.setHorizontalZoom(defaultHorizontalZoom);
			global.setVerticalZoom(defaultVerticalZoom);
			global.setCombine(defaultCombine);
			global.setShiftAmount(Integer.parseInt(shiftTextView.getText().toString()));
			global.setManual(true);
			global.setKey(0);
			horZoomText.setText(Integer.toString((int) (global.getAudioBufferLength() * (float)(defaultHorizontalZoom+1) / 101f)));
			combineText.setText(Integer.toString(defaultCombine) + "% orig");
		}
	}
	
	protected void onResume(){
		super.onResume();
		audioThread = new AudioThread(audioHandler);
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
		}
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
        if(audioThread.isAlive()){
        	audioThread.interruptWrite();
        	audioThread.stopLive();
        }
    	audioThread.killThread();
    	audioThread.onResume();
    	audioThread = null;
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
        outState.putInt("shiftAmount", global.getShiftAmount());
        outState.putInt("key", global.getKey());
        outState.putBoolean("manual", global.getManual());
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
    	if(seekBar.equals(verticalZoomBar)){
    		global.setVerticalZoom(progress);
    	}
    	if(seekBar.equals(combineBar)){
    		global.setCombine(progress);
    		combineText.setText(Integer.toString(progress) + "% orig");
    	}
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        
    }
    
    public void pauseDraw(View view) {
        global.setPause();
    }
    
    public void subShift(View view) {
    	if(global.getManual()){
        	int shift = Integer.parseInt(shiftTextView.getText().toString());
        	shift--;
        	shiftTextView.setText(Integer.toString(shift));
        	global.setShiftAmount(shift);
    	}else{
    		int key = Tones.getPitchFromNote(shiftTextView.getText().toString());
    		key--;
    		key = (key+12) %12;
    		shiftTextView.setText(Tones.getNoteFromPitch(key));
    		global.setKey(key);
    	}

    }
    
    public void addShift(View view) {
    	if(global.getManual()){
        	int shift = Integer.parseInt(shiftTextView.getText().toString());
        	shift++;
        	shiftTextView.setText(Integer.toString(shift));
        	global.setShiftAmount(shift);
    	}else{
    		int key = Tones.getPitchFromNote(shiftTextView.getText().toString());
    		key++;
    		key = (key+12) %12;
    		shiftTextView.setText(Tones.getNoteFromPitch(key));
    		global.setKey(key);
    	}
    }
    
    public void toggleRecord(View view) {
    	if(audioThread.isAlive()){
	        // Is the toggle on?
	        boolean on = ((ToggleButton) view).isChecked();
	        
	        if (on) {
	            audioThread.startWriteFile(); //start writing to file if live is not on
	        } else {
	        	audioThread.stopWriteFile(); //must try to stop writing file if started, regardless of what the live checkbox is now
	        }
    	}
    }
    
    public void toggleAutotune(View view){
    	global.setAutotune(((CheckBox) view).isChecked());
    }
    
    public void toggleFilter(View view){
    	global.setFilter(((CheckBox) view).isChecked());
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;

    }
    
    public boolean onOptionsItemSelected(MenuItem item)
    {
         
        switch (item.getItemId())
        {
        case R.id.record_title:
            if(global.getLive()){	//if it was live, change to record
                if(audioThread.isAlive()){
                	audioThread.stopLive();
                }
                item.setTitle(R.string.record_title);
                global.setLive(false);
            }else{	//change to live
                if(audioThread.isAlive()){
                	audioThread.interruptWrite();
                	audioThread.startLive();
                }
                item.setTitle(R.string.live_title);
                global.setLive(true);
            }  
            return true;
            case R.id.manual_title:
                if(global.getManual()){	//change to automatic
                    item.setTitle(R.string.manual_title);
                    global.setManual(false);
                    shiftTextView.setText(Tones.getNoteFromPitch(global.getKey()));
                }else{	//change to manual
                    item.setTitle(R.string.auto_title);
                    global.setManual(true);
                	shiftTextView.setText(Integer.toString(global.getShiftAmount()));
                    
                }  
                return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }    
}