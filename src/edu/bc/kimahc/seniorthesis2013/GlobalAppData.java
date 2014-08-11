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
	private int combineAmount;
	private int audioBufferLength = 2048;
	private final int highFreqCutoff = 1200;
	private final int lowFreqCutoff = 50;
	private boolean pause = false;
	private boolean autotune = false;
	private boolean isFiltered;
	private boolean isLive = false;
	private boolean isManual = true;
    private int maxPitchMarks;
    private int pitchShiftAmount = 0;
    private int key = 0;
    private volatile float peakVar =0, valleyVar =0, peakNum =0, valleyNum = 0, exPeakNum = 0, exValleyNum = 0, valleyEst = 0, peakEst = 0;
    private volatile float yinDelay = 0, yinCount = 0, markDelay = 0, markCount = 0, shiftDelay = 0, shiftCount = 0;
	private float yinThreshold = 0f;
	
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
	
	public void setKey(int root) {
		key = root%12;	
	}
	
	public int getKey() {
		return key;	
	}

	public void setCombine(int combine){
		combineAmount = combine;
	}
	
	public int getCombine(){
		return combineAmount;
	}
	
	public synchronized void incValleyVar(float inc, int num, float expPer){

		valleyVar += inc*(num-1);
		valleyNum += num-1;
		valleyEst += expPer*(num-1);
		float std = (float) Math.sqrt(inc);
		if(std/expPer > 0.059f){
			exValleyNum += num-1;
		}
		float avgstd = (float) Math.sqrt(valleyVar/valleyNum);
		
		Log.i("valley std", "" + avgstd + " avgper " + (valleyEst/valleyNum)+ " %std/per>0.059= " + (exValleyNum/valleyNum) + " c= " + valleyNum);
		
	}
	public synchronized void incPeakVar(float inc, int num, float expPer){
		
		peakVar += inc*(num-1);
		peakNum += num-1;
		peakEst += expPer*(num-1);
		float std = (float) Math.sqrt(inc);
		if(std/expPer > 0.059f){
			exPeakNum += num-1;
		}
		float avgstd = (float) Math.sqrt(peakVar/peakNum);
		
		Log.i("peak std", "" + avgstd + " avgper " + (peakEst/peakNum) + " %std/per>0.059= " + (exPeakNum/peakNum) + " c= " + peakNum);
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
	
	public void incYin(long delay){
		yinDelay += (float) delay;
		yinCount++;
		Log.i("avg yin delay", "" + yinDelay/yinCount);
	}
	
	public void incMark(long delay){
		markDelay += (float) delay;
		markCount++;
		Log.i("avg mark delay", "" + markDelay/markCount);
	}
	
	public void incShift(long delay){
		shiftDelay += (float) delay;
		shiftCount++;
		Log.i("avg shift delay", "" + shiftDelay/shiftCount);
	}
	public void setYinThreshold(float f) {
		yinThreshold = f;
	}
	public float getYinThreshold() {
		return yinThreshold;
	}
	public void setAutotune(boolean checked) {
		autotune = checked;
	}
	
	public boolean getAutotune() {
		return autotune;
	}
	public void setLive(boolean checked) {
		isLive = checked;
	}
	
	public boolean getLive() {
		return isLive;
	}
	public void setManual(boolean checked) {
		isManual = checked;
	}
	
	public boolean getManual() {
		return isManual;
	}
}
