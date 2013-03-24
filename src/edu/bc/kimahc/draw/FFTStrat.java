package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

public class FFTStrat extends DrawStrat{

	private float[] abuf;
	public FFTStrat(FloatBuffer vertexBuffer, int bufLen) {
		super(vertexBuffer, bufLen);
		abuf = new float[audioBufferLength*2];
	}


	public FloatBuffer setDrawBuffers(float[] buf, int drawBufferLength, int verticalZoom, int tab) {
		return vertexBuffer;
	}
	
	public int setVertexCount(int drawBufferLength) {
		return drawBufferLength;
	}
	
	public float setLineWidth(int horizontalZoom) {
		return 120f/(horizontalZoom+20f)+2;
	}


	@Override
	public FloatBuffer setProcessedBuffers(float[] buf, int drawBufferLength,
			int verticalZoom, int tab) {
		//System.out.println("abuf length = " + abuf.length);
		//System.out.println("buf length = " + buf.length);
		for(int i = 0; i < drawBufferLength/2; i=i+1){
			abuf[4*i] = (float)(i * 4f)/ (float)drawBufferLength - 1f;		//bottom x
			abuf[4*i+1] = -1f;												//bottom y
			abuf[4*i+2] = (float)(i * 4f)/ (float)drawBufferLength - 1f;	//upper x
			abuf[4*i+3] = buf[i]/(verticalZoom+1)-1f;						//upper y
			//System.out.println(fft.getBand(i/2));
		}

		synchronized(vertexBuffer){
			vertexBuffer.clear();
	        vertexBuffer.put(abuf).flip();
	        vertexBuffer.position(0);
		}
		return vertexBuffer;
	}

}
