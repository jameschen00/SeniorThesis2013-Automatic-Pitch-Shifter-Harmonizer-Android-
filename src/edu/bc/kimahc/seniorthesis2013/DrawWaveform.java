package edu.bc.kimahc.seniorthesis2013;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import com.badlogic.gdx.audio.analysis.FFT;
import com.badlogic.gdx.audio.analysis.FourierTransform;

import edu.bc.kimahc.draw.DrawStrat;
import edu.bc.kimahc.draw.FFTStrat;
import edu.bc.kimahc.draw.PitchStrat;
import edu.bc.kimahc.draw.WaveformStrat;

public class DrawWaveform {

    private FloatBuffer vertexBuffer;
    private float[][] audioBuffer;
    private int kNumDrawBuffers = 1024;
    private int audioBufferLength;
    private int circularBufferCount;
    private int drawBufferLength;
	private int vertexCount;
	private int vertexStride;
	private float lineWidth = 1f;
	private static final int WAVEFORMPLOT = 1;
	private static final int FFTPLOT = 2;
	private static final int PITCHPLOT = 3;
	
    private DrawStrat waveformStrat;
    private DrawStrat fftStrat;
    private DrawStrat pitchStrat;
    private DrawStrat currentStrat;
	
	long time;

	private long startTime;
	private long endTime, dt;
	
    
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;
    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.5f, 0.76953125f, 0.22265625f, 1.0f };
    //float color[] = { 0, 1f, 0, 1.0f };
	private final String vertexShaderCode =
		    "attribute vec4 vPosition;" +
		    "void main() {" +
		    "  gl_Position = vPosition;" +
		    "}";

	private final String fragmentShaderCode =
		    "precision mediump float;" +
		    "uniform vec4 vColor;" +
		    "void main() {" +
		    "  gl_FragColor = vColor;" +
		    "}";
	int mProgram;
	

	
    public DrawWaveform() {

    	startTime = System.nanoTime();
        // audioBuffer = new short[kNumDrawBuffers][2048];
        //audioBufferLength = 2048;
        audioBufferLength = 2048;
        audioBuffer = new float[kNumDrawBuffers][audioBufferLength];
        circularBufferCount = 0;
        
    	vertexCount = audioBufferLength/COORDS_PER_VERTEX * 2 ; //Vertex count is the array divided by the size of the vertex ex. (x,y) or (x,y,z) 
    	vertexStride = COORDS_PER_VERTEX * 4;                //4 are how many bytes in a float
    		
    	
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
        		audioBufferLength * 2 * 4);
       
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();


        waveformStrat = new WaveformStrat(vertexBuffer, audioBufferLength);
        fftStrat = new FFTStrat(vertexBuffer, audioBufferLength);
        pitchStrat = new PitchStrat(vertexBuffer, audioBufferLength);
        currentStrat = waveformStrat;
        System.out.println("bb size: " + bb.capacity());
		System.out.println("vb size: " + vertexBuffer.capacity());
        // add the coordinates to the FloatBuffer
        //vertexBuffer.put(triangleCoords);
        // set the buffer to read the first coordinate
        //vertexBuffer.position(0);
        
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
        

    }
    public void setColor(float r, float g, float b, float a){

       color[0] = r;
       color[1] = g;
       color[2] = b;
       color[3] = a;
    }
    
	public void sendAudioData(float[] floatAudioBuffer, int horizontalZoom, int verticalZoom, int tab){
		//this.audioBuffer[circularBufferCount] = audioBuffer;
		if(tab == WAVEFORMPLOT){
			currentStrat = waveformStrat;
		}
		else if(tab == FFTPLOT){
			currentStrat = fftStrat;
		}
		else if(tab == PITCHPLOT){
			currentStrat = pitchStrat;
		}
		setDrawBuffers(floatAudioBuffer, horizontalZoom, verticalZoom, tab);
		//circularBufferCount++;
		if(circularBufferCount == kNumDrawBuffers)
			circularBufferCount = 0;
		//setDrawBuffers(audioBuffer);
	}
	
	public void sendProcessedAudioData(float[] processedAudioBuffer,
			int horizontalZoom, int verticalZoom, int tab) {
		setProcessedDrawBuffers(processedAudioBuffer, horizontalZoom, verticalZoom, tab);
		
	}
	
	public void setProcessedDrawBuffers(float[] buf, int horizontalZoom, int verticalZoom, int tab){
		currentStrat.setProcessedBuffers(buf, drawBufferLength, verticalZoom, tab);
	}
	public void setDrawBuffers(float[] buf, int horizontalZoom, int verticalZoom, int tab){

		// do something diff for audio and fft data
		//System.out.println(120f/(horizontalZoom+20f));
		drawBufferLength = audioBufferLength * (horizontalZoom+1) / 101;
		
		//audioBuffer[circularBufferCount] = buf;
		
		//float[] abuf = new float[audioBufferLength*2];
		//float max = 65536; // 2^16

		
		//float[] abuf = new float[audioBufferLength*2];
		vertexBuffer = currentStrat.setDrawBuffers(buf, drawBufferLength, verticalZoom, tab);
		vertexCount = currentStrat.setVertexCount(drawBufferLength);
		lineWidth = currentStrat.setLineWidth(horizontalZoom);
		
		
		/*
		if(tab == WAVEFORMPLOT){
			vertexCount = drawBufferLength;
			lineWidth = 120f/(horizontalZoom+20f);
			for(int i = 0; i < drawBufferLength; i=i+1){
				abuf[2*i] = (float)(i * 2f)/ (float)drawBufferLength - 1f;	//x coord
				//System.out.println("i = " + abuf[2*i]);
				abuf[2*i+1] = (2*audioBuffer[0][i])*(50f/(verticalZoom+1));	//y coord
				//System.out.println("i+1 = " + abuf[2*i+1] + "  " + audioBuffer[0][i]);
				//for(int j = 0; j < kNumDrawBuffers; j++){
			}
		}
		else if(tab == FFTPLOT){
			vertexCount = drawBufferLength;
			lineWidth = 120f/(horizontalZoom+20f);
			time = System.nanoTime();
			FFT fft = new FFT(audioBufferLength, 44100);
			fft.forward(buf);
			//System.out.print("fft: " + (System.nanoTime() - time));
			for(int i = 0; i < drawBufferLength/2; i=i+1){
				abuf[4*i] = (float)(i * 4f)/ (float)drawBufferLength - 1f;		//bottom x
				abuf[4*i+1] = -1f;												//bottom y
				abuf[4*i+2] = (float)(i * 4f)/ (float)drawBufferLength - 1f;	//upper x
				abuf[4*i+3] = fft.getBand(i)/(verticalZoom+1)-1f;		//upper y
				//System.out.println(fft.getBand(i/2));
			}
		}
		else if(tab == PITCHPLOT){
			vertexCount = drawBufferLength/2;
			lineWidth = 120f/(horizontalZoom+20f)+2;
			FFT fft = new FFT(audioBufferLength, 44100);
			fft.forward(buf);
			float[] acData = computeAC(audioBufferLength, 44100, buf);
			float max = acData[0];
			//System.out.println(max);
			for(int i = 0; i < drawBufferLength/2; i=i+1){ //1034
				abuf[i*2] = (float)(i * 4f)/ (float)drawBufferLength - 1f;		//x coord
				abuf[i*2+1] = (acData[i])/max;//*(20f/(verticalZoom+1));				//y
				//System.out.println(acData[i]);
			}	
		}
		//System.out.println("VB size: " + vertexBuffer.capacity());
		vertexBuffer.clear();
        vertexBuffer.put(abuf).flip();
        //System.out.println("position: " + vertexBuffer.position());
        // set the buffer to read the first coordinate
        //vertexBuffer.position(0);
		
		//draw();
		 * 
		 */
	}
    
    public void draw(int tab) {
    	

		startTime = System.nanoTime();
    	
    	synchronized(vertexBuffer){
	        // Add program to OpenGL ES environment
	        GLES20.glUseProgram(mProgram);
	        //GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexBuffer.capacity(), vertexBuffer);
	        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexBuffer.capacity(), vertexBuffer);
	    	
    	}
        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);
        //System.out.println("Coords: " + COORDS_PER_VERTEX + ", stride" + vertexStride + ", vertexcount" + vertexCount);
        // get handle to fragment shader's vColor member
        int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // Draw the triangle
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        GLES20.glLineWidth(lineWidth);
        if(tab == WAVEFORMPLOT)
        	GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
        else if(tab == FFTPLOT)
        	GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);
        else if(tab == PITCHPLOT){
        	GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
        }
        //System.out.println("  draw: " + (System.nanoTime() - time));
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
        //GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        
        endTime = System.nanoTime();
        dt = (endTime - startTime)/1000000l;
        System.out.println("drawing delay = " + dt);
    }
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
    

}