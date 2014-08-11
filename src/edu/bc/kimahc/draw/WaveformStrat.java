package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

import edu.bc.kimahc.seniorthesis2013.GlobalAppData;
import edu.bc.kimahc.soundProcessing.LowPassFilter;
import edu.bc.kimahc.soundProcessing.PitchMarker;
import edu.bc.kimahc.soundProcessing.PitchShifter;

import Music.Tones;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;

public class WaveformStrat extends DrawStrat{
	private float[] abuf, shiftedBuffer, shiftedMarks, unprocessedBuffer, processedBuffer, pitchMarks, prevPitchMarks;
	private float freq;
	private int pitchMarksLength = 0, prevMarksLength = 0;;
	private GlobalAppData global;
	private int latestVert = 0;
	private int latestDraw = 0;
	private float red[] = { 0.86f, 0.0f, 0.0f, 1.0f };
	private float blue[] = { 0f, 0.0f, 0.8f, 1.0f };
	private float color[] = { 0.5f, 0.76953125f, 0.22265625f, 1.0f };
	public WaveformStrat(FloatBuffer vertexBuffer, int bufLen) {
		super(vertexBuffer, bufLen);
		global = GlobalAppData.getInstance();
		abuf = new float[audioBufferLength*2 + 360]; //manually added 360...better to use max pitch marks (which is calculated later)
		shiftedBuffer = new float[audioBufferLength*2];
	}

	public FloatBuffer setDrawBuffers(Bundle bundle, int drawBufferLength,
			int horizontalZoom, int verticalZoom, int tab) {
		unprocessedBuffer = bundle.getFloatArray("unprocessed");
		processedBuffer = bundle.getFloatArray("processed");
		pitchMarks = bundle.getFloatArray("pitchMarks");
		prevPitchMarks = bundle.getFloatArray("prevPitchMarks");
		prevMarksLength = 0;
		
		if(unprocessedBuffer != null){
			pitchMarksLength = 0;
			for(int i = 0; i < drawBufferLength; i=i+1){
				abuf[2*i] = (float)(i * 2f)/ (float)drawBufferLength - 1f;	//x coord
				abuf[2*i+1] = (2*unprocessedBuffer[i])*(50f/(verticalZoom+1))+0.5f;	//y coord
			}
		}
		if(prevPitchMarks != null){ // draw previous pitch marks separately
			prevMarksLength = prevPitchMarks.length;	

			//now drawing previous pitch marks
			int offset = audioBufferLength*2;
			for(int i = 0; i < prevMarksLength; i=i+2){
				abuf[offset+i] = (float)(prevPitchMarks[i] * 2f)/ (float)drawBufferLength - 1f;	//x coord
				abuf[offset+i+1] = (prevPitchMarks[i+1] * 2f)*(50f/(verticalZoom+1))+0.5f;	//y coord
				}
		}
		if(pitchMarks != null){ // draw pitch marks separately
			pitchMarksLength = pitchMarks.length;

			//now drawing pitch marks
			int offset = audioBufferLength*2 + prevMarksLength;
			for(int i = 0; i < pitchMarksLength; i=i+2){
				abuf[offset+i] = (float)(pitchMarks[i] * 2f)/ (float)drawBufferLength - 1f;	//x coord
				abuf[offset+i+1] = (pitchMarks[i+1] * 2f)*(50f/(verticalZoom+1))+0.5f;	//y coord
				}

			
			//shiftedBuffer = PitchShifter.pitchShift(floatAudioBuffer, pitchMarks, 1, global.getShiftAmount(), 1, false, 44100, global.getLowFreqCutoff(), global.getHighFreqCutoff());
			
			//shiftedMarks = PitchShifter.pitchShiftMarks(floatAudioBuffer, pitchMarks, 1, global.getShiftAmount(), 1);
			
			
			float freq = bundle.getFloat("freq");
			freq = (float) Math.pow(2, (global.getShiftAmount()/12))*freq;
			//shiftedMarks = PitchMarker.pitchMark(shiftedBuffer, freq, 44100);
		}
		shiftedBuffer = processedBuffer;
		latestVert = verticalZoom;
		latestDraw = drawBufferLength;
		synchronized(vertexBuffer){
			vertexBuffer.clear();
	        vertexBuffer.put(abuf).flip();
	        vertexBuffer.position(0);
		}
		return vertexBuffer;
	}
	
	public int setVertexCount(int drawBufferLength) {
		return drawBufferLength;
	}

	public float setLineWidth(int horizontalZoom) {
		return 120f/(horizontalZoom+20f);
	}

	public synchronized void draw(int vertexCount, int mColorHandle){
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
		
		if(prevMarksLength > 0){
			GLES20.glUniform4fv(mColorHandle, 1, blue, 0);
			GLES20.glDrawArrays(GLES20.GL_POINTS, audioBufferLength, prevMarksLength/2);
		}
		
		if(pitchMarksLength > 0){
			GLES20.glUniform4fv(mColorHandle, 1, red, 0);
			GLES20.glDrawArrays(GLES20.GL_POINTS, audioBufferLength+prevMarksLength/2, pitchMarksLength/2);
		}
		
		if(shiftedBuffer !=null && shiftedBuffer.length >= latestDraw){
			for(int i = 0; i < latestDraw; i=i+1){
				abuf[2*i] = (float)(i * 2f)/ (float)latestDraw - 1f;	//x coord
				abuf[2*i+1] = (2*shiftedBuffer[i])*(50f/(latestVert+1))-0.5f;	//y coord
			}
			synchronized(vertexBuffer){
				vertexBuffer.clear();
		        vertexBuffer.put(abuf).flip();
		        vertexBuffer.position(0);
			}
			GLES20.glUniform4fv(mColorHandle, 1, color, 0);
			GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
		}
		
		if(shiftedMarks != null){ //this isnt currently set
			int synthMarksLength =  shiftedMarks.length;
			if(synthMarksLength > 0){
				int offset = audioBufferLength*2;
				//now drawing synth pitch marks
				for(int i = 0; i < synthMarksLength; i=i+2){
					abuf[i+offset] = (float)(shiftedMarks[i] * 2f)/ (float)latestDraw - 1f;	//x coord
					abuf[i+offset+1] = (2*shiftedMarks[i+1])*(50f/(latestVert+1))-0.5f;	//y coord
					}

				synchronized(vertexBuffer){
					vertexBuffer.clear();
			        vertexBuffer.put(abuf).flip();
			        vertexBuffer.position(0);
				}
				//GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexBuffer.capacity(), vertexBuffer);
				GLES20.glUniform4fv(mColorHandle, 1, red, 0);
				GLES20.glDrawArrays(GLES20.GL_POINTS, audioBufferLength, synthMarksLength/2);
			}
		}
	
		
		
	}
}
