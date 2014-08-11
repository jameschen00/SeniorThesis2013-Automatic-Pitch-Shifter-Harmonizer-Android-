package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

import edu.bc.kimahc.seniorthesis2013.GlobalAppData;
import edu.bc.kimahc.soundProcessing.LowPassFilter;
import edu.bc.kimahc.soundProcessing.PitchMarker;
import edu.bc.kimahc.soundProcessing.PitchShifter;

import android.opengl.GLES20;

public class WaveformStrat extends DrawStrat{
	private float[] abuf, shiftedBuffer, shiftedMarks;
	private int pitchMarksLength = 0;
	private GlobalAppData global;
	private int latestVert = 0;
	private int latestDraw = 0;
	float color[] = { 1f, 0f, 0f, 1.0f };
	public WaveformStrat(FloatBuffer vertexBuffer, int bufLen) {
		super(vertexBuffer, bufLen);
		global = GlobalAppData.getInstance();
		abuf = new float[audioBufferLength*2 + 360]; //manually added 160...better to use max pitch marks (which is calculated later)
		shiftedBuffer = new float[audioBufferLength*2];
	}

	public synchronized FloatBuffer setDrawBuffers(float[] buf, int drawBufferLength, int horizontalZoom,
			int verticalZoom, int tab) {
		//float[] abuf = new float[buf.length*2];
		if(buf.length == audioBufferLength){ //the given buffer must be the correct size
			pitchMarksLength = 0;
			for(int i = 0; i < drawBufferLength; i=i+1){
				abuf[2*i] = (float)(i * 2f)/ (float)drawBufferLength - 1f;	//x coord
				//System.out.println("i = " + abuf[2*i]);
				abuf[2*i+1] = (2*buf[i])*(50f/(verticalZoom+1))+0.5f;	//y coord
				//System.out.println("i+1 = " + abuf[2*i+1] + "  " + audioBuffer[0][i]);
				//for(int j = 0; j < kNumDrawBuffers; j++){
			}
			synchronized(vertexBuffer){
				vertexBuffer.clear();
		        vertexBuffer.put(abuf).flip();
		        vertexBuffer.position(0);
			}
		}
		else if(buf.length > audioBufferLength){ //if buffer data also includes pitch marks, draw differently
			for(int i = 0; i < drawBufferLength; i=i+1){
				abuf[2*i] = (float)(i * 2f)/ (float)drawBufferLength - 1f;	//x coord
				//System.out.println("i = " + abuf[2*i]);
				abuf[2*i+1] = (2*buf[i])*(50f/(verticalZoom+1))+0.5f;	//y coord
				//System.out.println("i+1 = " + abuf[2*i+1] + "  " + audioBuffer[0][i]);
				//for(int j = 0; j < kNumDrawBuffers; j++){
			}
			//System.out.println("abuf length = " + abuf.length);
			int offset = audioBufferLength-1; //subtract one to include the pitchMarksLength element
			pitchMarksLength = (int) buf[audioBufferLength]; //the first element after the audio buffer contains the number of pitch marks		
			
			//now drawing pitch marks
			for(int i = audioBufferLength*2; i < audioBufferLength*2 + pitchMarksLength; i=i+2){
				abuf[i] = (float)(buf[i-offset] * 2f)/ (float)drawBufferLength - 1f;	//x coord
				abuf[i+1] = (2*buf[i+1-offset])*(50f/(verticalZoom+1))+0.5f;	//y coord
				}
			float[] pitchMarks = new float[pitchMarksLength];
			System.arraycopy(buf, audioBufferLength+1, pitchMarks, 0, pitchMarksLength);
			float[] floatAudioBuffer = new float[audioBufferLength];
			System.arraycopy(buf, 0, floatAudioBuffer, 0, audioBufferLength);
			
			
			//shiftedBuffer = PitchShifter.pitchShift(floatAudioBuffer, pitchMarks, 1, global.getShiftAmount(), 1, false, 44100, global.getLowFreqCutoff(), global.getHighFreqCutoff());
			shiftedBuffer = LowPassFilter.filter(floatAudioBuffer, 44100, 1200f);
			//shiftedMarks = PitchShifter.pitchShiftMarks(floatAudioBuffer, pitchMarks, 1, global.getShiftAmount(), 1);
			float est;
			if(pitchMarks.length > 3)
				est = 44100f/(pitchMarks[2] - pitchMarks[0]);
			else
				est = audioBufferLength;
			shiftedMarks = PitchMarker.pitchMark(shiftedBuffer, est, 44100);
			
			
			latestVert = verticalZoom;
			latestDraw = drawBufferLength;
			synchronized(vertexBuffer){
				vertexBuffer.clear();
		        vertexBuffer.put(abuf).flip();
		        vertexBuffer.position(0);
			}
		}
		return vertexBuffer;
	}

	public int setVertexCount(int drawBufferLength) {
		return drawBufferLength;
	}

	public float setLineWidth(int horizontalZoom) {
		return 120f/(horizontalZoom+20f);
	}

	public synchronized void draw(int vertexCount) {
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
		if(pitchMarksLength > 0){
			//GLES20.gl
			GLES20.glDrawArrays(GLES20.GL_POINTS, audioBufferLength, pitchMarksLength/2);
			
			if(shiftedBuffer !=null){
				for(int i = 0; i < latestDraw; i=i+1){
					abuf[2*i] = (float)(i * 2f)/ (float)latestDraw - 1f;	//x coord
					//System.out.println("i = " + abuf[2*i]);
					abuf[2*i+1] = (2*shiftedBuffer[i])*(50f/(latestVert+1))-0.5f;	//y coord
					//System.out.println("i+1 = " + abuf[2*i+1] + "  " + audioBuffer[0][i]);
					//for(int j = 0; j < kNumDrawBuffers; j++){
				}
			}
			
			if(shiftedMarks != null){
				int synthMarksLength =  shiftedMarks.length;
				if(synthMarksLength > 0){
					int offset = audioBufferLength*2;
					//now drawing pitch marks
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
					//GLES20.glUniform4fv(mColorHandle, 1, color, 0);
					GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
					GLES20.glDrawArrays(GLES20.GL_POINTS, audioBufferLength, synthMarksLength/2);
				}
			}
			
			
		}
		
		
	}
}
