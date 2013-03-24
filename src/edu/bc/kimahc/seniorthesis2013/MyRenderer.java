package edu.bc.kimahc.seniorthesis2013;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

public class MyRenderer implements Renderer {
	Triangle mTriangle;
	Square mSquare;
	private DrawWaveform drawWaveform;
	private DrawPitch drawPitch;
	private int tab;
	private int horizontalZoom;
	private int verticalZoom;
	private float[] audioBuffer = null;
	private float[] processedAudioBuffer = null;
	private boolean isProcessedDataReady = false;
	private boolean isDrawReady = false;
	private boolean surfaceReady = false;
	private long startTime, oldTime;
	private long endTime, dt;
	private long maxDelta;
	private long regDataTime = System.nanoTime();

	private long procDataTime = System.nanoTime();
	
	public MyRenderer(int tab, int horizontalZoom, int verticalZoom) {
		this.tab = tab;
		this.horizontalZoom = horizontalZoom;
		this.verticalZoom = verticalZoom;
		startTime = System.nanoTime();
		maxDelta = (long) (1000f*(2048f/44100f)); // 46ms frames
	}

	public void setTab(int i){
		tab = i;
	}
	
	public void sendAudioData(float[] floatAudioBuffer){
		regDataTime = System.nanoTime();
		if(drawWaveform != null && surfaceReady){
			this.audioBuffer = floatAudioBuffer;
			drawWaveform.sendAudioData(floatAudioBuffer, horizontalZoom, verticalZoom, tab);
			isDrawReady = true;
		}
		
	}
	
	public void sendProcessedAudioData(float[] processedAudioBuffer) {
		procDataTime = System.nanoTime();
		Log.i("processing time", ""+(procDataTime-regDataTime)/1000000l);
		if(drawWaveform != null && surfaceReady){
			this.processedAudioBuffer = processedAudioBuffer;
			drawWaveform.sendProcessedAudioData(processedAudioBuffer,horizontalZoom, verticalZoom, tab);
			//System.out.println("renderer fft section ready");
			isProcessedDataReady = true;
		}
	}
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        //isDrawReady = true;
        //drawWaveform = new DrawWaveform();
        /*
     // Set the background color to black ( rgba ).
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);  // OpenGL docs.
     		// Enable Smooth Shading, default not really needed.
        GLES20.glShadeModel(GL10.GL_SMOOTH);// OpenGL docs.
     		// Depth buffer setup.
        GLES20.glClearDepthf(1.0f);// OpenGL docs.
     		// Enables depth testing.
        GLES20.glEnable(GL10.GL_DEPTH_TEST);// OpenGL docs.
     		// The type of depth testing to do.
        GLES20.glDepthFunc(GL10.GL_LEQUAL);// OpenGL docs.
     		// Really nice perspective calculations.
        GLES20.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, // OpenGL docs.
                               GL10.GL_NICEST);
                               */
    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        
        endTime = System.nanoTime();
        dt = (long) ((endTime - startTime)/1000000l); //dt in ms
        
        if(dt < maxDelta){
			try {
				Thread.sleep(maxDelta - dt);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        //System.out.println("dt = " + dt);

        oldTime = startTime;
		startTime = System.nanoTime();
		System.out.println("draw frames = " + (startTime-oldTime)/1000000l);
		
        if(tab==1){
        	//if(isDrawReady && audioBuffer != null){
        		/*
        		short max = -1;
        		short min = 9999;
        		for(int i = 0; i < audioBuffer.length; i++){
        			if(audioBuffer[i] > max)
        				max = audioBuffer[i];
        			if(audioBuffer[i] < min)
        				min = audioBuffer[i];
        		}
        		*/
        		//System.out.println("max: " + max + " min: " + min);
        		
        		//mTriangle.setColor(max/12000f, 12000f/max, 0.3f, 1f);
        		//mTriangle.draw();
        		//System.out.println("renderer: " + audioBuffer[0]);
        		
        		//isDrawReady = false;
        		drawWaveform.draw(tab);
        		//mTriangle.draw();
        	//}
        		//mTriangle.draw();
        }
        else if(tab==2){
        	//if(isProcessedDataReady && audioBuffer != null){
        		//isDrawReady = false;
        		drawWaveform.draw(tab);
        		//isProcessedDataReady = false;
        	//}
    	}
        else if(tab==3){
        	//if(isProcessedDataReady && audioBuffer != null){
        		//isDrawReady = false;
        		drawWaveform.draw(tab);
        		//isProcessedDataReady = false; // this doesn't do anything. if data is processed (true) but strat obj changes, vertexbuffer null
        	//}
		}
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        //System.out.println(width+" " + height);
        // initialize a triangle
        mTriangle = new Triangle();
        mSquare = new Square();
        drawWaveform = new DrawWaveform();
        surfaceReady = true;
    }

	public void setHorizontalZoom(int horizontalZoom) {
		this.horizontalZoom = horizontalZoom;
	}
	
	public void setVerticalZoom(int verticalZoom) {
		this.verticalZoom = verticalZoom;
	}

}
class Triangle {

    private FloatBuffer vertexBuffer;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float triangleCoords[] = { // in counterclockwise order:
         0.0f,  0.622008459f, 0.0f,   // top
        -0.5f, -0.311004243f, 0.0f,   // bottom left
         0.5f, -0.311004243f, 0.0f    // bottom right
    };

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.5f, 0.76953125f, 0.22265625f, 1.0f };
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
	
	int vertexCount = triangleCoords.length/COORDS_PER_VERTEX; //Vertex count is the array divided by the size of the vertex ex. (x,y) or (x,y,z) 
	int vertexStride = COORDS_PER_VERTEX * 4;                //4 are how many bytes in a float
		
    public Triangle() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                triangleCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);
        
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
    public void draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

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
class Square {

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;
    static float squareCoords[] = { -0.5f,  0.5f,   // top left
                                    -0.5f, -0.5f,   // bottom left
                                     0.5f, -0.5f,   // bottom right
                                     0.5f,  0.5f }; // top right

    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices
    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };


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


    static final int vertexCount = squareCoords.length/COORDS_PER_VERTEX; //Vertex count is the array divided by the size of the vertex ex. (x,y) or (x,y,z) 
    static final int vertexStride = COORDS_PER_VERTEX * 4;                //4 are how many bytes in a float

    public Square() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4); // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2); // (# of coordinate values * 2 bytes per short)
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
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

    public void draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the Square
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // Draw the Square
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}