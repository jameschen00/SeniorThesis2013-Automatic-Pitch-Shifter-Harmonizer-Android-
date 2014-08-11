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

import Music.Harmony;
import Music.Tones;
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
    private int lastSynthMark = 0;
    private GlobalAppData global;
    private float[] unprocessedAudioBufferPing = null;
    private float[] unprocessedAudioBufferPong = null;
    private float[] fftBuffer;
    private float[] waveformBuffer;
    private float[] prevShiftedBuffer;
    private float[] prevAudioBuffer;
    private float[] prevPitchMarks;
    private float[] combinedShiftedBuffer;
    private float[] combinedAudioBuffer;
    private float[] writeArrayFloat;
    private short[] writeArray;
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
    private PitchShifter pitchShifter;
    //private PitchMarker pitchMarker;
    private int maxPitchMarks;
    private static float maxFreq;
    private AudioTrack audioTrack = null;
    private volatile boolean isWriting = false;
    private volatile boolean isPlaying = false;
    private volatile boolean isLive = false;
    private volatile boolean isShifted = false;
    private boolean isPausedYet = false;
    private float[] pausedBuffer;
    private short[] shiftedShort;
    private int maxLoops;
    private int loopCounter = 0;
    private float lastMarkVal = 0;
    private float lastMarkIndex = 0;
    private float latestFreq = 0;
    
    private String filename = "saveAudioFile.txt";
    private FileOutputStream fileOutputStream;
    private DataOutputStream dataOutputStream;

    private FileInputStream fileInputStream;
    private DataInputStream dataInputStream;
    private Context context;
    private AudioThread audioThread;
    private Harmony harmony;
    
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
        float yinThresh = 0.2f;
        fastYin = new FastYin(SR, audioBufferLength, yinThresh, global.getLowFreqCutoff(), global.getHighFreqCutoff());
        global.setYinThreshold(yinThresh);
        maxFreq = global.getHighFreqCutoff();
        yin = new Yin(SR, audioBufferLength, yinThresh);
        pitchShifter = new PitchShifter();
        System.out.println("proc thread created");
        lowFreqCutoff = global.getLowFreqCutoff();
        highFreqCutoff = global.getHighFreqCutoff();
        context = global;
        
		fft = new FFT(audioBufferLength, SR);
		fastfft = new FloatFFT(audioBufferLength);
		fftBuffer = new float[audioBufferLength/2];
		maxPitchMarks = (int) (3*audioBufferLength/(0.7*SR/maxFreq)+0.5); //adding 5 to be safe. multiply by 2 to account because pitch marks come in pairs (amplitude and position)
		global.setMaxPitchMarks(maxPitchMarks);
		//waveformBuffer = new float[audioBufferLength+maxPitchMarks+1]; //add one more because the first element will be # of pitch marks
		waveformBuffer = new float[audioBufferLength];
		
        pausedBuffer = new float[audioBufferLength];
        writeArray = new short[audioBufferLength];
        writeArrayFloat = new float[audioBufferLength];
        
        prevShiftedBuffer = new float[audioBufferLength/2];	//only save half
        prevAudioBuffer = new float[audioBufferLength/2];
        prevPitchMarks = null;
        
        combinedShiftedBuffer = new float[audioBufferLength+prevShiftedBuffer.length]; //1.5 buffer = 2048 + 1024 = 3072
        combinedAudioBuffer = new float[audioBufferLength+prevAudioBuffer.length];
        
        harmony = new Harmony(0);
        
    }

    @Override
    public void run()
    { 
        Log.i("processing", "Running processing Thread");


    	int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
    	int channelMode = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    	int encodingMode = AudioFormat.ENCODING_PCM_16BIT;
    	int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMode, encodingMode);
    	audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelMode, encodingMode, minBufferSize*10, AudioTrack.MODE_STREAM);
    	 		
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
	                			processData(unprocessedAudioBufferPing);
	            	    		isPingNew = false;
			                	//Log.i("ping", "processed ping"); 
	                		}
	                	}
	                	else if(pingOrPong == PONG){
	                		synchronized(pongLock){
	                			processData(unprocessedAudioBufferPong);
	            	    		isPongNew = false;
			                	//Log.i("pong", "processed pong"); 
	                		}
	                	}
	                	else if(pingOrPong == -1){
	                		System.out.println("EXIT SOUND PROCESSING");
	                		break;
	                	}
                	}
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
    	short[] unprocessedBuffer = audioBuffer;
    	/*
		if(global.getPause()){
			if(!isPausedYet){
				isPausedYet = true;
				System.out.println("first pause");
				System.arraycopy(audioBuffer, 0, pausedBuffer, 0, audioBuffer.length);
			}
			unprocessedBuffer = pausedBuffer;
		}
		else{
			if(isPausedYet){
				isPausedYet = false;
				System.out.println("first unpause");
			}
		}
		*/
		
    	if(pingOrPong == PING){
    		synchronized(pongLock){
	    		unprocessedAudioBufferPong = convertShortArrayToFloatArray(unprocessedBuffer); //set other buffer
	    		pingOrPong = PONG;														 //switch to buffer with new data
	    		isPongNew = true; 
	    		isPingNew = false; //the other buffer becomes outdated so we don't want it to be processed after this new one
            	//Log.i("pong", "set pong data"); 
    		}
    	}
    	else if (pingOrPong == PONG){
    		synchronized(pingLock){
	    		unprocessedAudioBufferPing = convertShortArrayToFloatArray(unprocessedBuffer); //set unused buffer
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
	public void processData(float[] floatAudioBuffer) {

		float[] processedBuffer = null;
		float[] unprocessedBuffer = null;
		
		
		if(global.getPause()){
			if(!isPausedYet){
				isPausedYet = true;
				System.out.println("first pause");
				System.arraycopy(floatAudioBuffer, 0, pausedBuffer, 0, floatAudioBuffer.length);
			}
			floatAudioBuffer = pausedBuffer;
		}
		else{
			if(isPausedYet){
				isPausedYet = false;
				System.out.println("first unpause");
			}
		}
		
		
		

		float freq = computeFreqFastYin(floatAudioBuffer);
		if(freq > 0){
			latestFreq = freq;
		}
		//float freq = computeFreqFastYin(prevFrameBuffer);
		
		//float[] pitchMarks = PitchMarker.pitchMark(filteredSignal, freq, SR);
		
		System.arraycopy(prevAudioBuffer, 0, combinedAudioBuffer, 0, prevAudioBuffer.length); //copy previous 2nd half of mic data into first half of combined buffer
		System.arraycopy(floatAudioBuffer, 0, combinedAudioBuffer, prevAudioBuffer.length, audioBufferLength); //copy current mic data into second half of combined buffer
		
		/*
		if(global.getFilter()){
			floatAudioBuffer = LowPassFilter.filter(floatAudioBuffer, SR, 1200);
		}
		if(global.getFilter()){
			combinedAudioBuffer = LowPassFilter.filter(combinedAudioBuffer, SR, 1200);
		}
		*/
		
		long startTime = System.nanoTime();
		//float[] pitchMarks = PitchMarker.pitchMark(floatAudioBuffer, latestFreq, SR, lastMarkIndex, lastMarkVal);
		float[] pitchMarks = null;

		if(global.getFilter()){
			pitchMarks = PitchMarker.pitchMark(LowPassFilter.filter(combinedAudioBuffer, SR, 1200), freq, SR, lastMarkIndex, lastMarkVal);
		}else{
			pitchMarks = PitchMarker.pitchMark(combinedAudioBuffer, freq, SR, lastMarkIndex, lastMarkVal);
		}

		//float[] pitchMarks = PitchMarker.pitchMark(combinedAudioBuffer, freq, SR, lastMarkIndex, lastMarkVal);

		//float[] pitchMarks = PitchMarker.getMaxPeaks(floatAudioBuffer,60, 0.005f);	//only gives array indexes now
		//float[] pitchMarks = PitchMarker.getMaxPeaks(floatAudioBuffer,0,(int) ((float)SR/latestFreq), 3, 0.01f);
		//float[] pitchMarks = PitchMarker.pitchMark(prevFrameBuffer, freq, SR);

		long endTime = System.nanoTime();
		global.incMark((endTime-startTime)/1000000l);
        //Log.i("pitch mark", ""+(endTime-startTime)/1000000l);
		
		float autoFreq = -1;
		if(global.getAutotune()){
			autoFreq = Tones.getClosestPerfectFreq(freq);
		}
		int shift;
		if(global.getManual()){
			shift = global.getShiftAmount();
		}else{
			harmony.changeKey(global.getKey());
			shift = harmony.harmonizeAmount(freq);
			Log.i("harmony", "" + shift);
		}
		
		int combine = global.getCombine();
		if(shift == 0){
			combine = 0;
		}
			
			if(pitchMarks != null){
				startTime = System.nanoTime();
				if(autoFreq > 0){
					combinedShiftedBuffer = pitchShifter.pitchShiftContAuto(prevAudioBuffer, floatAudioBuffer, prevShiftedBuffer, pitchMarks, (int) lastMarkIndex, lastSynthMark, 1, 
							shift, 1, combine, SR, lowFreqCutoff, highFreqCutoff, autoFreq);
				}
				else{
					combinedShiftedBuffer = pitchShifter.pitchShiftCont(prevAudioBuffer, floatAudioBuffer, prevShiftedBuffer, pitchMarks, (int) lastMarkIndex, lastSynthMark, 1, 
							shift, 1, combine, SR, lowFreqCutoff, highFreqCutoff, autoFreq);
				}
				
				//lastMarkIndex+prevShiftedBuffer.length
		        endTime = System.nanoTime();
		        Log.i("combined shifter", ""+(endTime-startTime)/1000000l);
		        global.incShift((endTime-startTime)/1000000l);
				
				if(combinedShiftedBuffer != null){
					System.arraycopy(combinedShiftedBuffer, 0, writeArrayFloat, 0, audioBufferLength); //use first section (which should be completely shifted) to play
					if(!isPausedYet){
						System.arraycopy(combinedShiftedBuffer, audioBufferLength, prevShiftedBuffer, 0, prevShiftedBuffer.length); //save second section into prevShifted to use in next frame
						lastSynthMark = pitchShifter.getLastSynthesisMark();
					}
				}else{
					System.arraycopy(floatAudioBuffer, 0, writeArrayFloat, 0, audioBufferLength);
					Arrays.fill(prevShiftedBuffer, 0);
					lastSynthMark = -1;
				}
	
			}
			else{

				System.out.println(" synth: " + lastSynthMark);
				
				if(lastSynthMark > 0){	//implies previous pitch marks were not null
					//fill up gap since last synth mark and end of prev shifted buffer
					int period = (int) (SR/latestFreq);
					int start = (int)lastSynthMark + period;
					System.arraycopy(combinedAudioBuffer, 0, waveformBuffer, 0, audioBufferLength);
					Arrays.fill(writeArrayFloat, 0);
					System.arraycopy(prevShiftedBuffer, 0, writeArrayFloat, 0, prevShiftedBuffer.length);
					System.out.println("start: " + start + " synth: " + lastSynthMark + " per: " + period);
					PitchShifter.olaLeftHalfHanning(start, waveformBuffer, writeArrayFloat, lastSynthMark, period);
					System.arraycopy(combinedAudioBuffer, start, writeArrayFloat, start, audioBufferLength-start);
					if(!isPausedYet){
						System.arraycopy(combinedAudioBuffer, 0, prevShiftedBuffer, 0, prevShiftedBuffer.length);//copy repaired section
					}
				}
				else{
					//System.arraycopy(prevShiftedBuffer, 0, writeArrayFloat, 0, prevShiftedBuffer.length); //copy previous frame halfbuffer into first half of write
					//System.arraycopy(floatAudioBuffer, 0, writeArrayFloat, prevShiftedBuffer.length, prevShiftedBuffer.length);	//copy 1st half of audio buffer into 2nd half of write			
					System.arraycopy(combinedAudioBuffer, 0, writeArrayFloat, 0, audioBufferLength);
					if(!isPausedYet){
						System.arraycopy(floatAudioBuffer, prevShiftedBuffer.length, prevShiftedBuffer, 0, prevShiftedBuffer.length);//copy 2nd half of mic data (to use in next frame)
					}
					lastSynthMark = -1;
				}
				combinedShiftedBuffer = null;

			}
			if(global.getFilter()){
				writeArrayFloat = LowPassFilter.filter(writeArrayFloat, SR, 1200);
				writeArray = convertFloatArrayToShortArray(writeArrayFloat);
				combinedAudioBuffer = LowPassFilter.filter(combinedAudioBuffer, SR, 1200);
			}else{
				writeArray = convertFloatArrayToShortArray(writeArrayFloat);
			}
			//writeArray = convertFloatArrayToShortArray(writeArrayFloat);

		
		
		switch(global.getTab()){
			case WAVEFORMPLOT:
				System.arraycopy(combinedAudioBuffer, 0, waveformBuffer, 0, audioBufferLength);
				unprocessedBuffer = waveformBuffer;
				processedBuffer = writeArrayFloat;
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
				processedBuffer[0]	= Tones.getTone(latestFreq);
						// = computePitchFastYin(floatAudioBuffer);
				break;
		}
		

    	sendDataToMain(processedBuffer, unprocessedBuffer, pitchMarks, prevPitchMarks, freq, shift);
		
    	if(!isPausedYet){
	    	if(isWriting){
				try {
					short[] shortArray = convertFloatArrayToShortArray(floatAudioBuffer);
					dataOutputStream.writeInt(shortArray.length);
					for (int i = 0; i < shortArray.length; i++) {
						dataOutputStream.writeShort(shortArray[i]);
					}
	
					if(shortArray != null){
						writeSampleCount += shortArray.length;
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
						audioTrack.write(convertFloatArrayToShortArray(writeArrayFloat),0,audioLength);
						//System.out.println("status: " + audioTrack.getPlayState());
						//System.out.println("isPlaying: " + isPlaying + " avail: " + dataInputStream.available());
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
	    	else if(isLive){
				audioTrack.write(writeArray, 0, audioBufferLength);
			}

		
			System.arraycopy(floatAudioBuffer, prevAudioBuffer.length, prevAudioBuffer, 0, prevAudioBuffer.length); //save second section into prevAudio to use in next frame
			
			if(freq > 0){
				lastMarkIndex = pitchMarks[pitchMarks.length-2]-audioBufferLength;
				lastMarkVal = pitchMarks[pitchMarks.length-1];
				prevPitchMarks = truncatePitchMarks(pitchMarks, audioBufferLength);
				//System.arraycopy(pitchMarks, 0, prevPitchMarks, 0, pitchMarks.length);
			}
			else{
				lastMarkIndex = -audioBufferLength;
				lastMarkVal = 0;
				prevPitchMarks = null;
				//prevPitchMarks = null;
			}
			if(combinedShiftedBuffer == null){
				lastSynthMark = -1;
			}
		}
    	else if(isPlaying){
    		setBuffer(writeArray);
    	}
		//return processedBuffer;
		//return writeArrayFloat;
	}
	
	//only return marks that are greater than the cutoffValue, and then subtract the cutoff value from them
	// (same thing as subtracting the cutoff value first and then getting rid of all negative numbers)
	private float[] truncatePitchMarks(float[] marks, int cutoffValue){
		if(marks == null)
			return null;
		int index = 0;
		for(int i = 0; i < marks.length; i+=2){
			if(marks[i] > cutoffValue){
				index = i;
				break;
			}
		}
		float[] copyMarks = new float[marks.length-index];
		System.arraycopy(marks, index, copyMarks, 0, copyMarks.length);
		for(int i = 0; i < copyMarks.length; i+=2){
			copyMarks[i] = copyMarks[i] - cutoffValue;
		}
		return copyMarks;
	}
	
	private void sendDataToMain(float[] processedData, float[] unprocessedData, float[] pitchMarks, float[] prevPitchMarks, float freq, int shift){
		/*
    	 * send processed data to main thread
    	 */
    	
        Message messageToParent = uiHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("processed", processedData);
        bundle.putFloatArray("unprocessed", unprocessedData);
        bundle.putFloatArray("pitchMarks", pitchMarks);
        bundle.putFloatArray("prevPitchMarks", prevPitchMarks);
        
        oldtime = newtime;
        //newtime = System.nanoTime();
        newtime = System.nanoTime();
        //System.out.println("audiothread time diff: " + (newtime - oldtime) + "    count= " + count);
        bundle.putLong("audiotime", newtime);
        bundle.putFloat("freq", freq);
        bundle.putInt("shift", shift);
        messageToParent.setData(bundle);
        uiHandler.sendMessage(messageToParent);
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
		pitch[0] = Tones.getTone(yin.getPitch(array));
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
		float pitch = Tones.getTone(fastYin.getPitch(array));
        long endTime = System.nanoTime();
        Log.i("fast yin pitch", ""+(endTime-startTime)/1000000l);
		return pitch;
	}
	private float computeFreqFastYin(float[] array) {
		long startTime = System.nanoTime();
		float freq = fastYin.getPitch(array);
        long endTime = System.nanoTime();
        Log.i("fast yin freq delay", ""+(endTime-startTime)/1000000l);
        global.incYin((endTime-startTime)/1000000l);
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
    
    public void startWrite(){
    	if(!isWriting && !isPlaying){	//can only start if it's not currently doing anything
	    	try {
	    		writeSampleCount = 0;
	    		totalNumMarks = 0;
	    		//audioTrack.pause();
	    		//audioTrack.flush();
				fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
	    		//fileOutputStream = context.openFileOutput(filename, Context.MODE_WORLD_READABLE);
				dataOutputStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
	    	isWriting = true;
    	}
    }
    
	public void startLive() {
		if(!isLive){
			isLive = true;
			audioTrack.play();
		}
	}
	
	public void stopLive() {
		if(isLive){
			isLive = false;
			audioTrack.pause();
			audioTrack.flush();
		}
	}
	
    public void interruptWrite(){
    	synchronized(writeLock){
	    	if(isWriting){ //only do the following if it was actually writing already
		    	try {
		    		dataOutputStream.close();
		    		isWriting = false;
		    	}catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    	else if(isPlaying){
	    		try {
	    			dataInputStream.close();
	    			audioTrack.pause();
	    			audioTrack.flush();
	    			isPlaying = false;
	    			audioThread.onResume();
		    	}catch (IOException e) {
					e.printStackTrace();
				}		
	    	}
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
					
					if(global.getShiftAmount() < 99){
						isPlaying = true;
						Arrays.fill(prevAudioBuffer, 0);
						Arrays.fill(prevShiftedBuffer, 0);
						if(dataInputStream.available() > 0){
							int audioLength = dataInputStream.readInt();
							for(int i = 0; i < audioLength; i++){
								writeArray[i] = dataInputStream.readShort();
							}
							System.out.println("1st audio length: " + audioLength);
							//audioTrack.write(writeArray,0,audioLength);
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
						//shiftWholeSegment(fileLength);	//deprecated method
					}
	
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
    	}
    }
    
    /*
    public void shiftWholeSegment(int fileLength) throws IOException{
    	
    	if(fileLength <= 0){
    		Log.i("process thread", "can't shift - file len = 0");
    		return;
    	}
		long startTime = System.nanoTime();

			//floatAudioBuffer = PitchShifter.pitchShift(floatAudioBuffer, pitchMarks, 1, global.getShiftAmount(), 1);

		float[] pitchMarksArray = null;
		short[] completeWriteArray = new short[writeSampleCount];
		if(totalNumMarks > 0){
			pitchMarksArray = new float[totalNumMarks];
		}
		int readMarks = 0;
		int readSamples = 0;
		while(dataInputStream.available() > 0){
			int audioLength = dataInputStream.readInt();
			//System.out.println("audio length: " + audioLength);
			for(int i = 0; i < audioLength; i++){
				completeWriteArray[i+readSamples] = dataInputStream.readShort();
			}
			readSamples+= audioLength;
			int numMarks = dataInputStream.readInt();
			//System.out.println("numMarks: " + numMarks);
			if(numMarks > 0){
				for(int i = 0; i < numMarks; i++){
					pitchMarksArray[readMarks+i] = dataInputStream.readFloat();
				}
				readMarks += numMarks;
			}

		}
		dataInputStream.close();
		
		long endTime = System.nanoTime();
        Log.i("read data", ""+(endTime-startTime)/1000000l);

		
		startTime = System.nanoTime();
		System.out.println("write sample count = " + writeSampleCount);
		System.out.println("totalNumMarks = " + totalNumMarks);
		
		if(pitchMarksArray != null){

			//float[] filteredSignal = LowPassFilter.filter(convertShortArrayToFloatArray(completeWriteArray), SR, 1200);
			
			
			float[] shiftedArray = PitchShifter.pitchShift(convertShortArrayToFloatArray(completeWriteArray), pitchMarksArray, 
					1, global.getShiftAmount(), 1, global.getCombine(), SR, lowFreqCutoff, highFreqCutoff);
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
		
		audioThread.onResume();
    }
*/
    
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