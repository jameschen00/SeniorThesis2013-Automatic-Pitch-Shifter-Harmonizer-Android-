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

public class AudioThread extends Thread
{ 
    private boolean alive = true;
    private Object mPauseLock = new Object();  
    private boolean mPaused;
    private Handler parentHandler;
    private int bufferSize = 2048;
    private short[][] buffers  = new short[256][bufferSize];
    private int lastBuffer = 0;
    private AudioRecord audioRecorder = null;
    private AudioTrack audioTrack = null;
    private int count = 0;
    private long oldtime = 0;
    private long newtime = 0;
    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public AudioThread(Handler pHandler)
    { 
    	parentHandler = pHandler;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        //start();
    }

    @Override
    public void run()
    { 
        Log.i("Audio", "Running Audio Thread");



        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {

        	int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
        	int channelMode = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        	int encodingMode = AudioFormat.ENCODING_PCM_16BIT;
        	int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMode, encodingMode);
        	Log.i("SR", Integer.toString(sampleRate));
        	
            //int N = AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            audioRecorder = new AudioRecord(AudioSource.MIC, sampleRate, channelMode, encodingMode, minBufferSize*10);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelMode, encodingMode, minBufferSize*10, AudioTrack.MODE_STREAM);

            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            
            audioRecorder.setPositionNotificationPeriod(bufferSize);
            audioRecorder.setRecordPositionUpdateListener(new OnRecordPositionUpdateListener() {
            	@Override
			    public void onPeriodicNotification(AudioRecord recorder) {
			        
			    	short[] buffer = buffers[++lastBuffer % buffers.length];
			        recorder.read(buffer, 0, bufferSize);
			        playTrack(buffer);
			        lastBuffer = lastBuffer % buffers.length;
			        
			        Message messageToParent = parentHandler.obtainMessage();
	                Bundle bundle = new Bundle();
	                bundle.putShortArray("aBuffer", buffer.clone());
	                count++;;
	                //bundle.putShortArray("aBuffer", count.clone());
	                bundle.putInt("countint", count);
	                //System.out.println("audiothread count: " + count);
	                
	                oldtime = newtime;
	                //newtime = System.nanoTime();
	                newtime = System.nanoTime();
	                //System.out.println("audiothread time diff: " + (newtime - oldtime) + "    count= " + count);
	                bundle.putLong("audiotime", newtime);
	                
	                messageToParent.setData(bundle);
	                parentHandler.sendMessage(messageToParent);
	                //Log.i("Map", "Sending buffer");
			    }
			
			    @Override
			    public void onMarkerReached(AudioRecord recorder) {
			    }
			});
            audioRecorder.startRecording();
            audioTrack.play();
            short[] buffer = buffers[++lastBuffer % buffers.length];
            audioRecorder.read(buffer, 0, bufferSize);
            playTrack(buffer);
            
            
            while(alive)
            { 
                //Log.i("Map", "Writing new data to buffer");
                //short[] buffer = buffers[lastBuffer++ % buffers.length];
                //minBufferSize = audioRecorder.read(buffer,0,buffer.length);
                //audioTrack.write(buffer, 0, buffer.length);

                
                synchronized (mPauseLock) {
                    while (mPaused) {
                        try {
                        	audioRecorder.stop();
                            audioTrack.stop();
                            mPauseLock.wait();
                        	audioRecorder.startRecording();
                            audioTrack.play();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
        catch(Throwable x)
        { 
            Log.w("Audio", "Error reading voice audio", x);
        }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
        finally
        { 
        	audioRecorder.stop();
        	audioRecorder.release();
            audioTrack.stop();
            audioTrack.release();
        	Log.i("released", "released audiorecorder stuff!!!");
        }
    }
    
    public void playTrack(short[] buffer){

        audioTrack.write(buffer, 0, bufferSize);
    }

    public void onPause() {
        synchronized (mPauseLock) {
            mPaused = true;
            Log.i("pause", "pause from inside thread");    
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
}