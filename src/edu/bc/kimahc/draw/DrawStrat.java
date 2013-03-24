package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

public abstract class DrawStrat {
	protected FloatBuffer vertexBuffer;
	protected static int audioBufferLength;
	public DrawStrat(FloatBuffer vertexBuffer, int bufLen){
		this.vertexBuffer = vertexBuffer;
		DrawStrat.audioBufferLength = bufLen;
	}
	
	public abstract FloatBuffer setDrawBuffers(float[] buf, int drawBufferLength, int verticalZoom, int tab);
	
	public abstract FloatBuffer setProcessedBuffers(float[] buf, int drawBufferLength, int verticalZoom, int tab);

	public abstract int setVertexCount(int drawBufferLength);

	public abstract float setLineWidth(int horizontalZoom);
 
}
