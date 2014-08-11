package edu.bc.kimahc.draw;

import java.nio.FloatBuffer;

import android.opengl.GLES20;

public class DiffStrat extends DrawStrat{

	private float[] abuf;
	public DiffStrat(FloatBuffer vertexBuffer, int bufLen) {
		super(vertexBuffer, bufLen);
		abuf = new float[audioBufferLength];
	}

	public int setVertexCount(int drawBufferLength) {
		return drawBufferLength/2;
	}

	public float setLineWidth(int horizontalZoom) {
		return 120f/(horizontalZoom+20f);
	}

	public FloatBuffer setDrawBuffers(float[] buf, int drawBufferLength, int horizontalZoom,
			int verticalZoom, int tab) {
		if(buf.length == audioBufferLength/2){
			//float max = buf[0];
			for(int i = 0; i < drawBufferLength/2; i=i+1){ //1034
				abuf[i*2] = (float)(i * 4f)/ (float)drawBufferLength - 1f;		//x coord
				abuf[i*2+1] = (buf[i])*(10f/(verticalZoom+1)) - 1f;///max;//*(20f/(verticalZoom+1));				//y
				//System.out.println(buf[i]);
			}	
			synchronized(vertexBuffer){
				vertexBuffer.clear();
		        vertexBuffer.put(abuf).flip();
		        vertexBuffer.position(0);
			}
		}
		return vertexBuffer;
	}

	public void draw(int vertexCount) {
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
	}
}
