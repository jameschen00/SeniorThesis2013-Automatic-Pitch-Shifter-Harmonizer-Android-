package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

import android.os.Bundle;

public abstract class DrawStrat {
	protected FloatBuffer vertexBuffer;
	protected static int audioBufferLength;
	public DrawStrat(FloatBuffer vertexBuffer, int bufLen){
		this.vertexBuffer = vertexBuffer;
		DrawStrat.audioBufferLength = bufLen;
	}
	
	public abstract int setVertexCount(int drawBufferLength);

	public abstract float setLineWidth(int horizontalZoom);

	public abstract void draw(int vertexCount, int mColorHandle);

	public abstract FloatBuffer setDrawBuffers(Bundle bundle, int drawBufferLength,
			int horizontalZoom, int verticalZoom, int tab);
 
}
