package a2;

import java.nio.*;
import javax.swing.*;
import java.lang.Math;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.*;
import com.jogamp.common.nio.Buffers;
import org.joml.*;

public class Code extends JFrame implements GLEventListener
{	
	private GLCanvas myCanvas;
	private int objectProgram, axesProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[5];
	private double startTime = 0.0;
	private double elapsedTime;
	private double tf;
	private float cameraX, cameraY, cameraZ;
	private float cubeLocX, cubeLocY, cubeLocZ, trapeLocX, trapeLocY, trapeLocZ;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);  // buffer for transfering matrix to uniform
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private int mvLoc, pLoc;
	private float aspect;

	// object variables
	private int shuttleTexture;
	private int numObjVertices;
	private ImportedModel myModel;

	public Code()
	{	setTitle("CSC 155 - Assignment 2");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		elapsedTime = System.currentTimeMillis() - startTime;
		tf = elapsedTime/1000.0;

		// render xyz axes
		gl.glUseProgram(axesProgram);
		gl.glDrawArrays(GL_LINES, 0, 6);


		gl.glUseProgram(objectProgram);

		mvLoc = gl.glGetUniformLocation(objectProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(objectProgram, "p_matrix");

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));


		mMat.identity();
		vMat.translation(-cameraX, -cameraY, -cameraZ);		// moves matrix to camera location
		mMat.translation(cubeLocX, cubeLocY, cubeLocZ);
		mMat.rotate((float)tf, (float)Math.cos(tf), (float)Math.sin(tf), 0.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);

		// cube object
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);


		mMat.identity();
		mMat.translation(trapeLocX, trapeLocY, trapeLocZ);
		mMat.translate((float)Math.sin(tf)*4.0f, 0.0f, (float)Math.cos(tf)*4.0f);
		mMat.rotate((float)tf, 0.0f, 1.0f, 0.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);

		// trapezoidal prism object
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);

		//gl.drawarrays myModel.getnumvertices

	}

	public void init(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		startTime = System.currentTimeMillis();
		axesProgram = Utils.createShaderProgram("a2/vertShaderAxes.glsl", "a2/fragShaderAxes.glsl");
		objectProgram = Utils.createShaderProgram("a2/vertShader.glsl", "a2/fragShader.glsl");
		setupVertices();
		
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 10.0f;
		cubeLocX = 0.0f; cubeLocY = -2.0f; cubeLocZ = 0.0f;
		trapeLocX = 3.0f; trapeLocY = 0.0f; trapeLocZ = 0.0f;
		
	}

	private void setupVertices()
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();

		float[] xAxis = { 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f };
		float[] yAxis = { 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f };
		float[] zAxis = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f };

		// 36 vertices of the 12 triangles making up a 2 x 2 x 2 cube centered at the origin
		float[] cubePositions =
		{	// (x,y,z) three times each line
			-1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f,  1.0f, -1.0f, -1.0f,  1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f, -1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f, -1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,
			-1.0f,  1.0f, -1.0f, 1.0f,  1.0f, -1.0f, 1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f, -1.0f
		};
		// 36 vertices of 12 triangles as well to create a trapezoidal prism
		float[] trapezoidPositions = 
		{
			-1.0f, -0.5f, -1.0f, 1.0f, -0.5f, -1.0f, 1.0f, -0.5f, 1.0f,		//bottom face
			1.0f, -0.5f, 1.0f, -1.0f, -0.5f, 1.0f, -1.0f, -0.5f, -1.0f,	
			-1.0f, -0.5f, -1.0f, 1.0f, -0.5f, -1.0f, -0.5f, 0.5f, -0.5f,	//front face
			-0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 1.0f, -0.5f, -1.0f,
			1.0f, -0.5f, -1.0f, 0.5f, 0.5f, -0.5f, 1.0f, -0.5f, 1.0f,		//right face
			1.0f, -0.5f, 1.0f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 
			0.5f, 0.5f, 0.5f, 1.0f, -0.5f, 1.0f, -1.0f, -0.5f, 1.0f, 		//back face
			-1.0f, -0.5f, 1.0f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 
			-0.5f, 0.5f, 0.5f, -1.0f, -0.5f, 1.0f, -1.0f, -0.5f, -1.0f, 	//left face
			-1.0f, -0.5f, -1.0f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 
			-0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 		//top face
			0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f
		};

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer tpzBuf = Buffers.newDirectFloatBuffer(trapezoidPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, tpzBuf.limit()*4, tpzBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer xAxisBuf = Buffers.newDirectFloatBuffer(xAxis);
		gl.glBufferData(GL_ARRAY_BUFFER, xAxisBuf.limit()*4, xAxisBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer yAxisBuf = Buffers.newDirectFloatBuffer(yAxis);
		gl.glBufferData(GL_ARRAY_BUFFER, xAxisBuf.limit()*4, yAxisBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer zAxisBuf = Buffers.newDirectFloatBuffer(zAxis);
		gl.glBufferData(GL_ARRAY_BUFFER, xAxisBuf.limit()*4, zAxisBuf, GL_STATIC_DRAW);


	}

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}
}