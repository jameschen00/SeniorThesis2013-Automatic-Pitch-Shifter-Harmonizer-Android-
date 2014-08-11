package edu.bc.kimahc.seniorthesis2013;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class GlobalAppData extends Application{
	
	private static GlobalAppData global;
	private static Context context;
	private int sampleRate;
	private volatile int tab;
	private int horizontalZoom;
	private int verticalZoom;
	private int audioBufferLength = 2048;
	private final int highFreqCutoff = 1200;
	private final int lowFreqCutoff = 50;
	private boolean pause = false;
	private boolean isLive;
	private boolean isCombined;
	private boolean isFiltered;
    private int maxPitchMarks;
    private int pitchShiftAmount = 0;
    private volatile float peakVar =0, valleyVar =0, peakNum =0, valleyNum = 0, exPeakNum = 0, exValleyNum = 0;
	//private 
	
    public int getAudioBufferLength(){
    	return audioBufferLength;
    }
	public int getHorizontalZoom() {
		return horizontalZoom;
	}

	public void setHorizontalZoom(int horizontalZoom) {
		this.horizontalZoom = horizontalZoom;
	}

	public int getVerticalZoom() {
		return verticalZoom;
	}

	public void setVerticalZoom(int verticalZoom) {
		this.verticalZoom = verticalZoom;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public int getTab() {
		return tab;
	}

	public void setTab(int tab) {
		this.tab = tab;
	}

	public GlobalAppData(){
		super();
		global = this;
		//context = getApplicationContext();
	}
	
	public static GlobalAppData getInstance(){
		return global;
	}
	
    public static Context getContext() {
        return context;
    }
	public synchronized void setPause() {
		pause = !pause;
	}
	
	public synchronized boolean getPause() {
		return pause;
	}

	public void setMaxPitchMarks(int maxPitchMarks) {
		this.maxPitchMarks = maxPitchMarks;
		
	}
	
	public int getMaxPitchMarks() {
		return maxPitchMarks;
	}
	

	public void setShiftAmount(int shiftAmount) {
		pitchShiftAmount = shiftAmount;	
	}
	
	public int getShiftAmount() {
		return pitchShiftAmount;	
	}

	public void setLive(boolean checked) {
		isLive = checked;
		
	}
	
	public boolean getLive(){
		return isLive;
	}
	
	public void setCombine(boolean combine){
		isCombined = combine;
	}
	
	public boolean getCombine(){
		return isCombined;
	}
	
	public synchronized void incValleyVar(float inc, int num){
		valleyVar += inc*num;
		valleyNum += num;
		if(inc > 100){
			exValleyNum += num;
		}
		Log.i("avg valley var", "" + valleyVar/valleyNum + "  % var > 100 = " + (exValleyNum/valleyNum) + " count = " + valleyNum);
		
	}
	public synchronized void incPeakVar(float inc, int num){
		peakVar += inc*num;
		peakNum += num;
		if(inc > 100){
			exPeakNum += num;
		}
		Log.i("avg peak var", "" + peakVar/peakNum + "  % var > 100 = " + (exPeakNum/peakNum) + " count = " + peakNum);
	}
	
	public float getValleyVar(){
		return valleyVar/valleyNum;
	}
	
	public float getPeakVar(){
		return peakVar/peakNum;
	}

	public void setFilter(boolean checked) {
		isFiltered = checked;
	}
	
	public boolean getFilter(){
		return isFiltered;
	}
	public int getHighFreqCutoff() {
		return highFreqCutoff;
	}
	public int getLowFreqCutoff() {
		return lowFreqCutoff;
	}
}
