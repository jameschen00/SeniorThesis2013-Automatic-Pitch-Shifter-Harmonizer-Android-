package edu.bc.kimahc.seniorthesis2013;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.badlogic.gdx.audio.analysis.FFT;
import com.badlogic.gdx.audio.analysis.FourierTransform;

import edu.bc.kimahc.soundProcessing.FastYin;
import edu.bc.kimahc.soundProcessing.FloatFFT;
import edu.bc.kimahc.soundProcessing.LowPassFilter;
import edu.bc.kimahc.soundProcessing.PitchMarker;
import edu.bc.kimahc.soundProcessing.PitchShifter;
import edu.bc.kimahc.soundProcessing.Yin;
public class AudioProcessingThread extends Thread
{ 
	
	private static final int WAVEFORMPLOT = 1;
	private static final int FFTPLOT = 2;
	private static final int ACPLOT = 3;
	private static final int PITCHPLOT = 4;
    private boolean alive = true;
    private Object mPauseLock = new Object();  
    private boolean mPaused;
    private Handler uiHandler;
    private int audioBufferLength;
    private int count = 0;
    private long oldtime = 0;
    private long newtime = 0;
    private FFT fft;
    private int SR;
    private int lowFreqCutoff;
    private int highFreqCutoff;
    private int writeSampleCount = 0;
    private int totalNumMarks = 0;
    private GlobalAppData global;
    private float[] unprocessedAudioBufferPing = null;
    private float[] unprocessedAudioBufferPong = null;
    private float[] processedBuf = null;
    private float[] fftBuffer;
    private float[] waveformBuffer;
    private float[] prevFrameBuffer;
    private short[] writeArray = new short[audioBufferLength];
    private volatile boolean isPingNew = false;
    private volatile boolean isPongNew = false;
    private volatile int pingOrPong = 0;
    private static final int PING = 0;
    private static final int PONG = 1;
    private Object mNewLock = new Object(); 
    private Object pingLock = new Object();
    private Object pongLock = new Object();
    private Object writeLock = new Object();
    private FastYin fastYin;
    private Yin yin;
    private FloatFFT fastfft;
    //private PitchMarker pitchMarker;
    private int maxPitchMarks;
    private final static float maxFreq = 1200f;
    private AudioTrack audioTrack = null;
    private volatile boolean isWriting = false;
    private volatile boolean isPlaying = false;
    private volatile boolean isShifted = false;
    private short[] shiftedShort;
    private int maxLoops;
    private int loopCounter = 0;
    private String filename = "saveAudioFile.txt";
    private FileOutputStream fileOutputStream;
    private DataOutputStream dataOutputStream;

    private FileInputStream fileInputStream;
    private DataInputStream dataInputStream;
    private Context context;
    private AudioThread audioThread;
    
    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     * @param audioThread 
     */
    public AudioProcessingThread(Handler pHandler, AudioThread audioThread)
    { 
    	uiHandler = pHandler;
    	this.audioThread = audioThread;
        //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        global = GlobalAppData.getInstance();
        SR = global.getSampleRate();
        audioBufferLength = global.getAudioBufferLength();
        fastYin = new FastYin(SR, audioBufferLength, 0.15, global.getLowFreqCutoff(), global.getHighFreqCutoff());
        yin = new Yin(SR, audioBufferLength, 0.15);
        System.out.println("proc thread created");
        lowFreqCutoff = global.getLowFreqCutoff();
        highFreqCutoff = global.getHighFreqCutoff();
        context = global;
        prevFrameBuffer = new float[audioBufferLength];
		
		Log.i("prevFrameBuffer", ""+prevFrameBuffer.length + "  " + prevFrameBuffer[1025]);
    }

    @Override
    public void run()
    { 
        Log.i("processing", "Running processing Thread");

		fft = new FFT(audioBufferLength, SR);
		fastfft = new FloatFFT(audioBufferLength);
		fftBuffer = new float[audioBufferLength/2];
		maxPitchMarks = (int) (2*audioBufferLength/(0.7*SR/maxFreq)+5); //adding 5 to be safe. multiply by 2 to account because pitch marks come in pairs (amplitude and position)
		global.setMaxPitchMarks(maxPitchMarks);
		waveformBuffer = new float[audioBufferLength+maxPitchMarks+1]; //add one more because the first element will be # of pitch marks
		
		
    	int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
    	int channelMode = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    	int encodingMode = AudioFormat.ENCODING_PCM_16BIT;
    	int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMode, encodingMode);
    	audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelMode, encodingMode, minBufferSize*10, AudioTrack.MODE_STREAM);
    	//if(global.getLive()){
    		audioTrack.play();
    	//}
    	
        while(alive)
        { 	
                try {

                	

                	synchronized (mNewLock) {
	                	while (!isPingNew && !isPongNew) { //block if there is no new data to process
		                	//Log.i("wait", "waiting for new data"); 
		                	mNewLock.wait();
	                	}
                	}
                	//if(!global.getPause()){
                	synchronized(writeLock){	//make it unable to change the status of writing/playback mid processing
	                	if (pingOrPong == PING){
	                		synchronized(pingLock){
	                			processedBuf = processData(unprocessedAudioBufferPing);
	            	    		isPingNew = false;
			                	Log.i("ping", "processed ping"); 
	                		}
	                	}
	                	else if(pingOrPong == PONG){
	                		synchronized(pongLock){
	                			processedBuf = processData(unprocessedAudioBufferPong);
	            	    		isPongNew = false;
			                	Log.i("pong", "processed pong"); 
	                		}
	                	}
	                	else if(pingOrPong == -1){
	                		System.out.println("EXIT SOUND PROCESSING");
	                		break;
	                	}
                	}
                	sendDataToMain(processedBuf);
                	//}

                } catch (InterruptedException e) {
                }

        }
        cleanUp();
    }
    
    private void cleanUp(){
    	short[] zeroBuffer = new short[audioBufferLength];
    	audioTrack.write(zeroBuffer, 0, audioBufferLength);
        audioTrack.stop();
        audioTrack.release();
        synchronized(writeLock){
	    	if(isWriting){
	    		try {
					dataOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    	if(isPlaying){
	    		try {
	    			
					dataInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
        }
        Log.i("ended", "ended processing");
    }

    public void onPause() {
        synchronized (mPauseLock) {
            mPaused = true;
            Log.i("pause", "pause from inside processing thread");    
        }
    }

    public void onResume() {
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }
    
    public void killThread(){
    	alive = false;
    	synchronized(pongLock){
	    	isPongNew = true;
	    	pingOrPong = -1;
    	}
    	synchronized(mNewLock){
    		mNewLock.notify();
    	}
    }

    public void setBuffer(short[] audioBuffer){
    	if(pingOrPong == PING){
    		synchronized(pongLock){
	    		unprocessedAudioBufferPong = convertShortArrayToFloatArray(audioBuffer); //set other buffer
	    		pingOrPong = PONG;														 //switch to buffer with new data
	    		isPongNew = true; 
	    		isPingNew = false; //the other buffer becomes outdated so we don't want it to be processed after this new one
            	//Log.i("pong", "set pong data"); 
    		}
    	}
    	else if (pingOrPong == PONG){
    		synchronized(pingLock){
	    		unprocessedAudioBufferPing = convertShortArrayToFloatArray(audioBuffer); //set unused buffer
	    		pingOrPong = PING;
	    		isPingNew = true;
	    		isPongNew = false;
            	//Log.i("ping", "set ping data"); 
    		}
    	}
	    	synchronized(mNewLock){
	    		mNewLock.notify();
	    	}
    }
	public float[] processData(float[] floatAudioBuffer) {

		float[] processedBuffer = null;
		
		
		//float freq = computeFreqFastYin(floatAudioBuffer);
		float freq = computeFreqFastYin(prevFrameBuffer);
		long startTime = System.nanoTime();
		//float[] pitchMarks = PitchMarker.pitchMark(filteredSignal, freq, SR);
		
		//float[] pitchMarks = PitchMarker.pitchMark(floatAudioBuffer, freq, SR);
		float[] pitchMarks = PitchMarker.pitchMark(prevFrameBuffer, freq, SR);
		
		long endTime = System.nanoTime();
        //Log.i("pitch mark", ""+(endTime-startTime)/1000000l);
		switch(global.getTab()){
			case WAVEFORMPLOT:
				
				if(pitchMarks == null){
					processedBuffer = floatAudioBuffer;
				}
				else{

					System.arraycopy(floatAudioBuffer, 0, waveformBuffer, 0, floatAudioBuffer.length);
					waveformBuffer[audioBufferLength] = pitchMarks.length;
					System.arraycopy(pitchMarks, 0, waveformBuffer, floatAudioBuffer.length+1, pitchMarks.length);
					
					/*
					for(int i = audioBufferLength; i < audioBufferLength+pitchMarks.length+1; i++){
						System.out.println("wv buf at " + i + ":  " + waveformBuffer[i]);
					}
					*/
					processedBuffer = waveformBuffer;
					//return processedBuffer;
					
					//return floatAudioBuffer;


				}
				break;
			case FFTPLOT:
				fft.forward(floatAudioBuffer);
				processedBuffer = fftBuffer;
				for(int i = 0; i < processedBuffer.length; i++){
					processedBuffer[i] = fft.getBand(i);
					//Log.i("fft band ", i + " : " + processedBuffer[i]);
				}
				break;
			case ACPLOT:
				//processedBuffer = computeAC(audioBufferLength, SR, floatAudioBuffer);
				processedBuffer = computeACFastYin(audioBufferLength, SR, floatAudioBuffer);
				//processedBuffer = computeFastAC(audioBufferLength, SR, floatAudioBuffer);
				break;
			case PITCHPLOT:
				processedBuffer = new float[1];
				processedBuffer[0]	= getTone(freq);
						// = computePitchFastYin(floatAudioBuffer);
				break;
		}
		/*
        long endTime = System.nanoTime();
        Log.i("process data(in thread)", ""+(endTime-startTime)/1000000l);
        Message messageToParent = parentHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("paBuffer", processedBuffer.clone()); //half length of audiobuffer
        bundle.putLong("processTime", endTime);
        messageToParent.setData(bundle);
        parentHandler.sendMessage(messageToParent);
        //Log.i("Map", "Sending buffer");
         * 
         
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		if(global.getLive() && !isWriting){// && !isPlaying){		//writing to a file takes precedence
			if(pitchMarks != null){
				float[] shiftedArray = PitchShifter.pitchShift(prevFrameBuffer, pitchMarks, 1, 
						global.getShiftAmount(), 1, global.getCombine(), global.getFilter(), SR, lowFreqCutoff, highFreqCutoff);
				prevFrameBuffer = floatAudioBuffer;
				writeArray = convertFloatArrayToShortArray(shiftedArray);
				audioTrack.write(writeArray, 0, audioBufferLength);
			}
			else{
				if(global.getFilter())
					floatAudioBuffer = LowPassFilter.filter(floatAudioBuffer, SR, 1200);
				writeArray = convertFloatArrayToShortArray(prevFrameBuffer);
				prevFrameBuffer = floatAudioBuffer;
				audioTrack.write(writeArray, 0, audioBufferLength);

			}

			//audioTrack.play();
		}else if(isWriting){
			try {
				short[] shortArray = convertFloatArrayToShortArray(floatAudioBuffer);
				dataOutputStream.writeInt(shortArray.length);
				for (int i = 0; i < shortArray.length; i++) {
					dataOutputStream.writeShort(shortArray[i]);
				}
				
				
				dataOutputStream.writeFloat(freq);

				if(shortArray != null){
					writeSampleCount += shortArray.length;
				}
				if(pitchMarks != null){
					totalNumMarks += pitchMarks.length;
				}
				System.out.println("WRITING");
				} catch (Exception e) {
				  e.printStackTrace();
				}
			
		}
		else if(isPlaying){
			try {
				if(dataInputStream.available() > 0){
					int audioLength = dataInputStream.readInt();
					for(int i = 0; i < audioLength; i++){
						writeArray[i] = dataInputStream.readShort();
					}
					System.out.println("audio length: " + audioLength);
					audioTrack.write(writeArray,0,audioLength);
					System.out.println("status: " + audioTrack.getPlayState());
					System.out.println("isPlaying: " + isPlaying + " avail: " + dataInputStream.available());
					setBuffer(writeArray);
				}
				else{
					dataInputStream.close();
					isPlaying = false;
					audioThread.onResume();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		/*
		else if(isShifted){
	        
			if(loopCounter < maxLoops){
	        	audioTrack.write(shiftedShort,loopCounter-1*audioBufferLength,audioBufferLength);
	        	short[] sendShort = new short[audioBufferLength];
	        	
	         	System.arraycopy(shiftedShort,loopCounter*audioBufferLength, sendShort, 0, audioBufferLength);
	         	setBuffer(sendShort);
	         	loopCounter++;
			}
			else{
				audioThread.onResume();
				isShifted = false;
			}	
		}
		*/
		return processedBuffer;
	}
	
	private void sendDataToMain(float[] data){
		/*
    	 * send processed data to main thread
    	 */
    	
        Message messageToParent = uiHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("aBuffer", data);
        
        oldtime = newtime;
        //newtime = System.nanoTime();
        newtime = System.nanoTime();
        //System.out.println("audiothread time diff: " + (newtime - oldtime) + "    count= " + count);
        bundle.putLong("audiotime", newtime);
        
        messageToParent.setData(bundle);
        uiHandler.sendMessage(messageToParent);
	}

	private float[] fft(int N, int fs, float[] array) {
    	float[] fft_cpx, tmpr, tmpi;
    	float[] res = new float[N / 2];
    	// float[] mod_spec =new float[array.length/2];
    	float[] real_mod = new float[N];
    	float[] imag_mod = new float[N];
    	double[] real = new double[N];
    	double[] imag = new double[N];
    	double[] mag = new double[N];
    	double[] phase = new double[N];
    	float[] new_array = new float[N];
    	// Zero Pad signal
    	for (int i = 0; i < N; i++) {

	    	if (i < array.length) {
	    		new_array[i] = array[i];
	    	} else {
	    		new_array[i] = 0;
	    	}
    	}

    	FFT fft = new FFT(N, fs);

    	fft.forward(new_array);
    	fft_cpx = fft.getSpectrum();
    	tmpi = fft.getImaginaryPart();
    	tmpr = fft.getRealPart();
    	for (int i = 0; i < new_array.length; i++) {
    	real[i] = (double) tmpr[i];
    	imag[i] = (double) tmpi[i];

    	mag[i] = Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
    	phase[i] = Math.atan2(imag[i], real[i]);

    	/**** Reconstruction ****/
    	real_mod[i] = (float) (mag[i] * Math.cos(phase[i]));
    	imag_mod[i] = (float) (mag[i] * Math.sin(phase[i]));

    	}
    	fft.inverse(real_mod, imag_mod, res);
    	return res;

	}
	private float[] computeACYin(int N, int fs, float[] array) {
		long startTime = System.nanoTime();
		
		float[] ac = yin.getDifference(array);

        long endTime = System.nanoTime();
        Log.i("reg yin ac", ""+(endTime-startTime)/1000000l);
		return ac;

	}
	
	private float[] computeACFastYin(int N, int fs, float[] array) {
		long startTime = System.nanoTime();
		
		float[] ac = fastYin.getDifference(array);

        long endTime = System.nanoTime();
        Log.i("fast yin ac", ""+(endTime-startTime)/1000000l);
		return ac;
	}
	private float[] computePitchYin(float[] array) {
		long startTime = System.nanoTime();
		float[] pitch = new float[1];
		pitch[0] = getTone(yin.getPitch(array));
        long endTime = System.nanoTime();
        Log.i("reg yin pitch", ""+(endTime-startTime)/1000000l);
		return pitch;
	}
	/*
	private float[] computePitchFastYin(float[] array) {
		long startTime = System.nanoTime();
		float[] pitch = new float[1];
		pitch[0] = getTone(fastYin.getPitch(array));
        long endTime = System.nanoTime();
        Log.i("fast yin pitch", ""+(endTime-startTime)/1000000l);
		return pitch;
	}*/
	private float computePitchFastYin(float[] array) {
		long startTime = System.nanoTime();
		float pitch = getTone(fastYin.getPitch(array));
        long endTime = System.nanoTime();
        Log.i("fast yin pitch", ""+(endTime-startTime)/1000000l);
		return pitch;
	}
	private float computeFreqFastYin(float[] array) {
		long startTime = System.nanoTime();
		float freq = fastYin.getPitch(array);
        long endTime = System.nanoTime();
        Log.i("fast yin freq delay", ""+(endTime-startTime)/1000000l);
		return freq;
	}
	
    private float[] computeAC(int N, int fs, float[] array) {

        long startTime = System.nanoTime();
    	float[] tmpr, tmpi;
    	float[] result = new float[N / 2];
    	// float[] mod_spec =new float[array.length/2];
    	float[] real = new float[N];
    	float[] imag = new float[N];
    	float[] new_array = new float[N];
    	// Zero Pad signal
    	for (int i = 0; i < N; i++) {

	    	if (i < array.length) {
	    		new_array[i] = array[i];
	    	} else {
	    		new_array[i] = 0;
	    	}
    	}

    	fft.window(FourierTransform.HAMMING);
    	fft.forward(new_array); //take fft
    	//fft_cpx = fft.getSpectrum();
    	tmpi = fft.getImaginaryPart();
    	tmpr = fft.getRealPart();
    	
    	for (int i = 0; i < new_array.length; i++) {
	    	//real[i] = (double) tmpr[i];
	    	//imag[i] = (double) tmpi[i];
	
	    	//mag[i] = Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
	    	//phase[i] = Math.atan2(imag[i], real[i]);
	    	real[i] = tmpr[i]*tmpr[i];
	    	imag[i] = tmpi[i]*tmpi[i];
    	}
    	fft.inverse(real, imag, result);
        long endTime = System.nanoTime();
        Log.i("compute ac", ""+(endTime-startTime)/1000000l);
    	return result;

	}
    
    private float[] computeFastAC(int N, int fs, float[] array) {

        long startTime = System.nanoTime();
    	float[] tmpr, tmpi;
    	float[] result = new float[N / 2];
    	// float[] mod_spec =new float[array.length/2];
    	float[] real = new float[N];
    	float[] imag = new float[N];
    	float[] fftarray = new float[N*2]; //first N will be full of input
    	// Zero Pad signal
    	for (int i = 0; i < N; i++) {

	    	if (i < array.length) {
	    		fftarray[i] = array[i];
	    	} else {
	    		fftarray[i] = 0;
	    	}
    	}

    	//fft.window(FourierTransform.HAMMING);
    	fastfft.realForwardFull(fftarray); //take fft

    	
    	for (int i = 0; i < fftarray.length; i++) {
	    	fftarray[i] = fftarray[i]*fftarray[i];
    	}
    	fastfft.complexInverse(fftarray, true);
        long endTime = System.nanoTime();
        Log.i("compute ac fast", ""+(endTime-startTime)/1000000l);
    	return result;

	}
    
    public float[] computePitch(float[] array){
    	float[] pitch = new float[1];
    	float[] fftResult = computeAC(audioBufferLength, SR, array);
    	
		return pitch;
    	
    }
    
    public static float[] convertShortArrayToFloatArray(short[] shortArray)
    {
    	if (shortArray == null){
    		return null;
    	}
    	float[] floatArray = new float[shortArray.length];
        for (int i = 0; i < shortArray.length; i++)
        	floatArray[i] = (float)(shortArray[i]/32768f);
        return floatArray;
    }
    
    public static short[] convertFloatArrayToShortArray(float[] floatArray)
    {
    	if (floatArray == null){
    		return null;
    	}
    	short[] shortArray = new short[floatArray.length];
        for (int i = 0; i < floatArray.length; i++)
        	shortArray[i] = (short)(floatArray[i]*32768f);
        return shortArray;
    }
    
    public float getTone(float freq){
    	return (float) (69.0+(12*Math.log(freq/440.0))/Math.log(2.0));
    }
    /*
    - (int) pitchIndex: (Float32) toneNum{ //returns pitch number from 0 to 11 (0 is C, 11 is B)
        return (int)(0.5+toneNum)%12;
    }

    - (Float32) pitchIndexFloat: (Float32) toneNum{ //returns pitch number from 0 to 11 (0 is C, 11 is B) with decimals
        Float32 fractPart, intPart, pitch;
        fractPart = modff(toneNum, &intPart);
        pitch = Float32((int)(intPart)%12)+fractPart;
        if (pitch > 11.5)
            return pitch-12.;
        return pitch;
    }*/
    
    public float getPitchIndex(float freq){ //returns pitch number from (-0.5 11.5] (0 is C, 11 is B)
    	float tone = getTone(freq);
    	float fractionalPart = tone-((long)tone); //isolates fraction
    	float integralPart = tone - fractionalPart; //isolates whole number
    	float pitch = integralPart % 12 + fractionalPart;
    	if (pitch > 11.5)
    		return pitch-12;
    	return pitch;
    }
    
    public void startWrite(){
    	if(!isWriting && !isPlaying){	//can only start if it's not currently doing anything
	    	try {
	    		writeSampleCount = 0;
	    		totalNumMarks = 0;
	    		audioTrack.pause();
	    		audioTrack.flush();
				fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
	    		//fileOutputStream = context.openFileOutput(filename, Context.MODE_WORLD_READABLE);
				dataOutputStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
	    	isWriting = true;
    	}
    }
    
    public void stopWrite(){
    	synchronized(writeLock){
	    	if(isWriting){ //only do the following if it was actually writing already
		    	try {
		    		dataOutputStream.close();
		    		File file = context.getFileStreamPath(filename);
					
		    		
		    		int fileLength = (int) file.length(); //gives # of bytes
					System.out.println("file length: " + fileLength);
					
					fileInputStream = new FileInputStream(file);
					//fileInputStream = context.openFileInput(filename);
					dataInputStream = new DataInputStream(new BufferedInputStream(fileInputStream));
					
					isWriting = false;
					audioTrack.play();
					
					if(global.getShiftAmount() == 999){
						isPlaying = true;
						if(dataInputStream.available() > 0){
							int audioLength = dataInputStream.readInt();
							for(int i = 0; i < audioLength; i++){
								writeArray[i] = dataInputStream.readShort();
							}
							System.out.println("1st audio length: " + audioLength);
							audioTrack.write(writeArray,0,audioLength);
							synchronized(pingLock){
								if(isPingNew){	//if last write buffer was already set, hijack the processData buffer
									unprocessedAudioBufferPing = convertShortArrayToFloatArray(writeArray);
									return;
								}
							}
							synchronized(pongLock){
								if(isPongNew){	//if last write buffer was already set, hijack the processData buffer
									unprocessedAudioBufferPong = convertShortArrayToFloatArray(writeArray);
									return;
								}
							}
							setBuffer(writeArray); //else set a new buffer by itself
						}
					}
					else{
						isPlaying = false;
						shiftWholeSegment(fileLength);
					}
	
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
    	}
    }
    
    public void shiftWholeSegment(int fileLength) throws IOException{
    	
    	if(fileLength <= 0){
    		Log.i("process thread", "can't shift - file len = 0");
    		return;
    	}
		long startTime = System.nanoTime();

			//floatAudioBuffer = PitchShifter.pitchShift(floatAudioBuffer, pitchMarks, 1, global.getShiftAmount(), 1);

		float[] freqArray = null;
		short[] completeWriteArray = new short[writeSampleCount];
		int numBuffers = (int) Math.ceil(writeSampleCount/audioBufferLength);
		freqArray = new float[numBuffers];
		int readFreq = 0;
		int readSamples = 0;
		while(dataInputStream.available() > 0){
			int audioLength = dataInputStream.readInt();
			//System.out.println("audio length: " + audioLength);
			for(int i = 0; i < audioLength; i++){
				completeWriteArray[i+readSamples] = dataInputStream.readShort();
			}
			readSamples+= audioLength;
			//System.out.println("numMarks: " + numMarks);
			if(numBuffers > 0){
				freqArray[readFreq] = dataInputStream.readFloat();
				readFreq ++;
			}
		}
		dataInputStream.close();
		
		long endTime = System.nanoTime();
        Log.i("read data", ""+(endTime-startTime)/1000000l);
        /*
		for(int i = 0; i < totalNumMarks/2; i++){
			Log.i(""+i, pitchMarksArray[i*2]+"");
		}*/
		
		startTime = System.nanoTime();
		System.out.println("write sample count = " + writeSampleCount);
		System.out.println("totalNumMarks = " + totalNumMarks);
		
		if(freqArray != null){
			/*
			for (int k = 0; k < pitchMarksArray.length; k++){
				System.out.println(pitchMarksArray[k]);
			}*/
			//float[] filteredSignal = LowPassFilter.filter(convertShortArrayToFloatArray(completeWriteArray), SR, 1200);
			
			//pitch mark LWHGLJKRGLKSJDLGKJSl
			float[] pitchMarksArray = null;
			float[] shiftedArray = PitchShifter.pitchShift(convertShortArrayToFloatArray(completeWriteArray), pitchMarksArray, 
					1, global.getShiftAmount(), 1, global.getCombine(), global.getFilter(), SR, lowFreqCutoff, highFreqCutoff);
			//float[] shiftedArray = PitchShifter.pitchShift(filteredSignal, pitchMarksArray, 1, global.getShiftAmount(), 1, global.getCombine());
			
			endTime = System.nanoTime();
	        Log.i("pitch mark", ""+(endTime-startTime)/1000000l);

	        System.out.println("shifted array length = " + shiftedArray.length);
	        shiftedShort = convertFloatArrayToShortArray(shiftedArray);
		}
		else{
			shiftedShort = completeWriteArray;
		}
        
        
        maxLoops = (int) Math.ceil(writeSampleCount/audioBufferLength);
        loopCounter = 1;
        System.out.println("max loops = " + maxLoops);
        short[] sendShort = new short[audioBufferLength];
    	System.arraycopy(shiftedShort,0, sendShort, 0, audioBufferLength);
    	
    	
    	//setBuffer(sendShort);
    	//isShifted = true;
    	for(int i = 0; i < maxLoops; i++){
    		audioTrack.write(shiftedShort,i*audioBufferLength,audioBufferLength);
    	}
		/*
    	if(loopCounter < maxLoops){
        	audioTrack.write(shiftedShort,loopCounter-1*audioBufferLength,audioBufferLength);
        	short[] sendShort = new short[audioBufferLength];
        	
         	System.arraycopy(shiftedShort,loopCounter*audioBufferLength, sendShort, 0, audioBufferLength);
         	setBuffer(sendShort);
         	loopCounter++;
		}
		else{
		*/
		audioThread.onResume();
    	
    	

/*
        int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
    	int channelMode = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    	int encodingMode = AudioFormat.ENCODING_PCM_16BIT;
    	int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMode, encodingMode);
    	AudioTrack staticAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelMode, encodingMode, writeSampleCount*2, AudioTrack.MODE_STATIC);
    	Log.i("staticAudioTrack.State={0}", ""+staticAudioTrack.getState());
        
        staticAudioTrack.write(convertFloatArrayToShortArray(shiftedArray), 0, writeSampleCount);
        Log.i("staticAudioTrack.State={0}", ""+staticAudioTrack.getState());
        staticAudioTrack.play();
        Log.i("staticAudioTrack.State={0}", ""+staticAudioTrack.getState());
        //staticAudioTrack.stop();
        */
        
    }
    
    public boolean isWriting(){
    	return isWriting;
    }
    
    public boolean isPlaying(){
    	return isPlaying;
    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}