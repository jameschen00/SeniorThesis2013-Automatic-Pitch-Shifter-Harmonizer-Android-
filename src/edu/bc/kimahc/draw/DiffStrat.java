package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

import edu.bc.kimahc.seniorthesis2013.GlobalAppData;

import android.opengl.GLES20;
import android.os.Bundle;

public class DiffStrat extends DrawStrat{

	private float[] abuf, diffData;
	private GlobalAppData global;
	private float white[] = { 1f, 1f, 1f, 1.0f };
	
	public DiffStrat(FloatBuffer vertexBuffer, int bufLen) {
		super(vertexBuffer, bufLen);
		abuf = new float[audioBufferLength+4];
	}

	public int setVertexCount(int drawBufferLength) {
		return drawBufferLength/2;
	}

	public float setLineWidth(int horizontalZoom) {
		return 120f/(horizontalZoom+20f);
	}

	public FloatBuffer setDrawBuffers(Bundle bundle, int drawBufferLength,
			int horizontalZoom, int verticalZoom, int tab) {
		diffData = bundle.getFloatArray("processed");
		if(diffData.length == audioBufferLength/2){
			
			for(int i = 0; i < 2; i=i+1){
				abuf[i*2] = i*2-1f;		//x coord
				abuf[i*2+1] = (global.getYinThreshold())*(10f/(verticalZoom+1)) - 1f;//y
			}	
			
			for(int i = 0; i < drawBufferLength/2; i=i+1){ //1034
				abuf[4+i*2] = (float)(i * 4f)/ (float)drawBufferLength - 1f;		//x coord
				abuf[4+i*2+1] = (diffData[i])*(10f/(verticalZoom+1)) - 1f;		//y coord
			}	
			synchronized(vertexBuffer){
				vertexBuffer.clear();
		        vertexBuffer.put(abuf).flip();
		        vertexBuffer.position(0);
			}
		}
		return vertexBuffer;
	}
	
	public void draw(int vertexCount, int mColorHandle){
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 2, vertexCount);
		GLES20.glLineWidth(1f);
		GLES20.glUniform4fv(mColorHandle, 1, white, 0);
		GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
	}
}
