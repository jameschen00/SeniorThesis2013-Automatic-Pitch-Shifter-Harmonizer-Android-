package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.os.Bundle;

public class ACStrat extends DrawStrat{

	private float[] abuf, acBuffer;
	public ACStrat(FloatBuffer vertexBuffer, int bufLen) {
		super(vertexBuffer, bufLen);
		abuf = new float[audioBufferLength];
	}

	public int setVertexCount(int drawBufferLength) {
		return drawBufferLength/2;
	}

	public float setLineWidth(int horizontalZoom) {
		return 120f/(horizontalZoom+20f);
	}

	public void draw(int vertexCount, int mColorHandle){
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
	}


	public FloatBuffer setDrawBuffers(Bundle bundle, int drawBufferLength,
			int horizontalZoom, int verticalZoom, int tab) {
		acBuffer = bundle.getFloatArray("processed");
		if(acBuffer.length == audioBufferLength/2){
			for(int i = 0; i < drawBufferLength/2; i=i+1){ //1034
				abuf[i*2] = (float)(i * 4f)/ (float)drawBufferLength - 1f;		//x coord
				abuf[i*2+1] = (acBuffer[i])*(10f/(verticalZoom+1)) - 1f;		//y
			}	
			synchronized(vertexBuffer){
				vertexBuffer.clear();
		        vertexBuffer.put(abuf).flip();
		        vertexBuffer.position(0);
			}
		}
		return vertexBuffer;
	}
}
