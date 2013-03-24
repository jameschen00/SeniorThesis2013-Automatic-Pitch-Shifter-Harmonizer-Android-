package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

public class PitchStrat extends DrawStrat{

	private float[] abuf;
	public PitchStrat(FloatBuffer vertexBuffer, int bufLen) {
		super(vertexBuffer, bufLen);
		abuf = new float[audioBufferLength];
	}

	public FloatBuffer setDrawBuffers(float[] buf, int drawBufferLength, int verticalZoom, int tab) {
		//return null;
		return vertexBuffer;
	}

	public int setVertexCount(int drawBufferLength) {
		return drawBufferLength/2;
	}

	public float setLineWidth(int horizontalZoom) {
		return 120f/(horizontalZoom+20f);
	}

	@Override
	public FloatBuffer setProcessedBuffers(float[] buf, int drawBufferLength,
			int verticalZoom, int tab) {
		float max = buf[0];
		for(int i = 0; i < drawBufferLength/2; i=i+1){ //1034
			abuf[i*2] = (float)(i * 4f)/ (float)drawBufferLength - 1f;		//x coord
			abuf[i*2+1] = (buf[i])/max;//*(20f/(verticalZoom+1));				//y
			//System.out.println(acData[i]);
		}	
		synchronized(vertexBuffer){
			vertexBuffer.clear();
	        vertexBuffer.put(abuf).flip();
	        vertexBuffer.position(0);
		}
		return vertexBuffer;
	}

}
