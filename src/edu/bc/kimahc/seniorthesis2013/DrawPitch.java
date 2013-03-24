package edu.bc.kimahc.seniorthesis2013;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
public class DrawPitch {

    private FloatBuffer vertexBuffer;
    private short[][] audioBuffer;
    private int kNumDrawBuffers = 1024;
    private int audioBufferLength;
    private int circularBufferCount;
	private int vertexCount;
	private int vertexStride;
		
    
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
	

	
    public DrawPitch() {

        // audioBuffer = new short[kNumDrawBuffers][2048];
        //audioBufferLength = 2048;
        audioBufferLength = 2048;
        audioBuffer = new short[kNumDrawBuffers][audioBufferLength];
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
    
	public void sendAudioData(short[] audioBuffer){
		//this.audioBuffer[circularBufferCount] = audioBuffer;
		setDrawBuffers(audioBuffer);
		//circularBufferCount++;
		if(circularBufferCount == kNumDrawBuffers)
			circularBufferCount = 0;
		//setDrawBuffers(audioBuffer);
	}
	
	public void setDrawBuffers(short[] buf){
		audioBuffer[circularBufferCount] = buf;
		float[] abuf = new float[audioBufferLength*2];
		//float[] abuf = new float[audioBufferLength*2];
		float max = 65536; // 2^16
		for(int i = 0; i < audioBufferLength ; i=i+1){
			
			abuf[2*i] = (float)(i * 2f)/ (float)audioBufferLength - 1f;
			//System.out.println("i = " + abuf[2*i]);
			//if(audioBuffer[0][i] > 0)
			//	abuf[2*i+1] = 10*(audioBuffer[0][i])/max + 0.5f;
			//else
			//	abuf[2*i+1] = 10*(audioBuffer[0][i])/max - 0.5f;
			abuf[2*i+1] = 10*(audioBuffer[0][i])/max;
			//System.out.println("i+1 = " + abuf[2*i+1] + "  " + audioBuffer[0][i]);
			//for(int j = 0; j < kNumDrawBuffers; j++){
			
		}
		//System.out.println("VB size: " + vertexBuffer.capacity());
		vertexBuffer.clear();
        vertexBuffer.put(abuf).flip();
        //System.out.println("position: " + vertexBuffer.position());
        // set the buffer to read the first coordinate
        //vertexBuffer.position(0);
		
		//draw();
	}
    
    public void draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexBuffer.capacity(), vertexBuffer);
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

        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
        //GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
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