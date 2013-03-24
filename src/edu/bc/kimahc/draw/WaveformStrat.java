package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

public class WaveformStrat extends DrawStrat{
	private float[] abuf;
	public WaveformStrat(FloatBuffer vertexBuffer, int bufLen) {
		super(vertexBuffer, bufLen);
		abuf = new float[audioBufferLength*2];
	}

	public FloatBuffer setDrawBuffers(float[] buf, int drawBufferLength, int verticalZoom, int tab) {
		//float[] abuf = new float[buf.length*2];
		for(int i = 0; i < drawBufferLength; i=i+1){
			abuf[2*i] = (float)(i * 2f)/ (float)drawBufferLength - 1f;	//x coord
			//System.out.println("i = " + abuf[2*i]);
			abuf[2*i+1] = (2*buf[i])*(50f/(verticalZoom+1));	//y coord
			//System.out.println("i+1 = " + abuf[2*i+1] + "  " + audioBuffer[0][i]);
			//for(int j = 0; j < kNumDrawBuffers; j++){
		}
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

	@Override
	public FloatBuffer setProcessedBuffers(float[] buf, int drawBufferLength,
			int verticalZoom, int tab) {
		// TODO Auto-generated method stub
		return null;
	}

}
