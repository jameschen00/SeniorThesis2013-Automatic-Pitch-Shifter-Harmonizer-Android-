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
    private boolean alive = false;
    private Object mPauseLock = new Object();  
    private volatile boolean mPaused;
    private Handler uiHandler;
    private int bufferSize = 2048;
    private short[][] buffers  = new short[256][bufferSize];
    private int lastBuffer = 0;
    private AudioRecord audioRecorder = null;
    private AudioTrack audioTrack = null;
    private int count = 0;
    private long oldtime = 0;
    private long newtime = 0;
    private GlobalAppData global;
    private static AudioProcessingThread audioProcessingThread;
    private short[] latestBuffer = null;
    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public AudioThread(Handler pHandler)
    { 
    	uiHandler = pHandler;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        global = GlobalAppData.getInstance();
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
        	global.setSampleRate(sampleRate);
        	
        	//create audio processing thread after sample rate has been determined
    		audioProcessingThread = new AudioProcessingThread(uiHandler, this);
    		if(audioProcessingThread.getState() == Thread.State.NEW){
    			Log.i("starting", "starting new processing thread from audioThread!");
    			audioProcessingThread.start();
    			}
    		else if(audioProcessingThread.getState() == Thread.State.TERMINATED){
    			Log.i("restarting", "REstarting new processing thread from audioThread!!!!");
    			audioProcessingThread = null;
    			audioProcessingThread = new AudioProcessingThread(uiHandler, this);
    			audioProcessingThread.start();
    		}
    		
            //int N = AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            audioRecorder = new AudioRecord(AudioSource.MIC, sampleRate, channelMode, encodingMode, minBufferSize*10);
            //audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelMode, encodingMode, minBufferSize*10, AudioTrack.MODE_STREAM);

            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            
            audioRecorder.setPositionNotificationPeriod(bufferSize);
            audioRecorder.setRecordPositionUpdateListener(new OnRecordPositionUpdateListener() {
            	@Override
			    public void onPeriodicNotification(AudioRecord recorder) {
			        
			    	//short[] buffer = buffers[++lastBuffer % buffers.length];
			        //recorder.read(buffer, 0, bufferSize);
			        //playTrack(buffer);
			        //lastBuffer = lastBuffer % buffers.length;
            		if(!mPaused && alive && latestBuffer != null) {
            			audioProcessingThread.setBuffer(latestBuffer);
            		}
			        
			        /*
			        Message messageToParent = uiHandler.obtainMessage();
	                Bundle bundle = new Bundle();
	                bundle.putShortArray("aBuffer", buffer);
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
	                uiHandler.sendMessage(messageToParent);
	                //Log.i("Map", "Sending buffer");
	                 * 
	                 * 
	                 */
			    }
			
			    @Override
			    public void onMarkerReached(AudioRecord recorder) {
			    }
			});
            audioRecorder.startRecording();
            //audioTrack.play();
            short[] buffer;// = buffers[++lastBuffer % buffers.length];
            //audioRecorder.read(buffer, 0, bufferSize);
            //playTrack(buffer);
            
            alive = true;
            while(alive)
            { 
                //Log.i("Map", "Writing new data to buffer");
                //short[] buffer = buffers[lastBuffer++ % buffers.length];
                //minBufferSize = audioRecorder.read(buffer,0,buffer.length);
                //audioTrack.write(buffer, 0, buffer.length);

            	//System.out.println("not PAUSED");
            	

                
		    	buffer = buffers[++lastBuffer % buffers.length];
		        audioRecorder.read(buffer, 0, bufferSize);
		        lastBuffer = lastBuffer % buffers.length;
		        latestBuffer = buffers[lastBuffer];
		        
                synchronized (mPauseLock) {
                    while (mPaused) {
                        try {
                        	audioRecorder.stop();
                            //audioTrack.stop();
                        	System.out.println("I AM PAUSED");
                            mPauseLock.wait();
                        	audioRecorder.startRecording();
                            //buffer = buffers[++lastBuffer % buffers.length];
                            //audioRecorder.read(buffer, 0, bufferSize);
                            //audioTrack.play();
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
            //audioTrack.stop();
            //audioTrack.release();
        	Log.i("released", "released audiorecorder stuff!!!");
        	
        	audioProcessingThread.killThread(); //kill processing thread
        }
    }
    
    public void playTrack(short[] buffer){
        audioTrack.write(buffer, 0, bufferSize);
    }
    
    public void startWriteFile(){
    	Log.i("audioThread", "start write");
    	audioProcessingThread.startWrite();
    }
	public void stopWriteFile() {
		if(audioProcessingThread.isWriting()){
			Log.i("audioThread", "stop write...isWriting = " + audioProcessingThread.isWriting());
			onPause();
			audioProcessingThread.stopWrite();
		}
		else{
			Log.i("audioThread", "stop write...isWriting = " + audioProcessingThread.isWriting());
		}
	}

	public void interruptWrite(){
		audioProcessingThread.interruptWrite();
	}
    public void onPause() {
        synchronized (mPauseLock) {
            mPaused = true;
            Log.i("audioThread", "pause from inside audioThread");    
        }
    }

    public void onResume() {
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
            Log.i("audioThread", "resume from inside audioThread");    
        }
    }
    
    public void killThread(){
    	alive = false;
    }

	public void startLive() {
		audioProcessingThread.startLive();
	}

	public void stopLive() {
		audioProcessingThread.stopLive();
	}

}