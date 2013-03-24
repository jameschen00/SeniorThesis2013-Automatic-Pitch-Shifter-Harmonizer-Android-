package edu.bc.kimahc.seniorthesis2013;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.badlogic.gdx.audio.analysis.FFT;
import com.badlogic.gdx.audio.analysis.FourierTransform;
public class AudioProcessingThread extends Thread
{ 
	
	private static final char WAVEFORMPLOT = '1';
	private static final char FFTPLOT = '2';
	private static final char PITCHPLOT = '3';
    private boolean alive = true;
    private Object mPauseLock = new Object();  
    private boolean mPaused;
    private Handler parentHandler;
    private int audioBufferLength = 2048;
    private int count = 0;
    private long oldtime = 0;
    private long newtime = 0;
    private FFT fft;
    private int SR = 44100;
    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public AudioProcessingThread(Handler pHandler)
    { 
    	parentHandler = pHandler;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        //start();
    }

    @Override
    public void run()
    { 
        Log.i("processing", "Running processing Thread");

		fft = new FFT(audioBufferLength, SR);

        while(alive)
        { 
            //Log.i("Map", "Writing new data to buffer");
            //short[] buffer = buffers[lastBuffer++ % buffers.length];
            //minBufferSize = audioRecorder.read(buffer,0,buffer.length);
            //audioTrack.write(buffer, 0, buffer.length);

                
            synchronized (mPauseLock) {
                while (mPaused) {
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                    }
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
    }

	public float[] processData(float[] floatAudioBuffer, String tab) {
		float[] processedBuffer = null;
		long startTime = System.nanoTime();
		switch(tab.charAt(3)){
			case WAVEFORMPLOT:
				return null;
			case FFTPLOT:
				fft.forward(floatAudioBuffer);
				processedBuffer = new float[floatAudioBuffer.length/2];
				for(int i = 0; i < processedBuffer.length; i++){
					processedBuffer[i] = fft.getBand(i);
					//Log.i("fft band ", i + " : " + processedBuffer[i]);
				}
				break;
			case PITCHPLOT:
				processedBuffer = computeAC(audioBufferLength, 44100, floatAudioBuffer);
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
		
		return processedBuffer.clone();

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

    	FFT fft = new FFT(N, fs);
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
}