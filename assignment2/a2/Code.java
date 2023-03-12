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
	private int renderingProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[2];
	private double startTime = 0.0;
	private double elapsedTime;
	private double tf;
	private float cameraX, cameraY, cameraZ;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);  // buffer for transfering matrix to uniform
	private Matrix4fStack mvStack = new Matrix4fStack(5);	//model-view matrix stack
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
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

		gl.glUseProgram(renderingProgram);

		mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		// push view matrix initial empty state
		mvStack.pushMatrix();
		mvStack.translate(-cameraX, -cameraY, -cameraZ);		//moves matrix to camera location

		// parent cube object
		mvStack.pushMatrix();			//push parent matrix
		mvStack.translate(0.0f, 0.0f, 0.0f);
		mvStack.pushMatrix();
		mvStack.rotate((float)tf, (float)Math.cos(tf), (float)Math.sin(tf), 0.0f);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		mvStack.popMatrix();

		// trapezoidal prism object
		mvStack.pushMatrix();			//push child matrix
		mvStack.translate((float)Math.sin(tf)*4.0f, 0.0f, (float)Math.cos(tf)*4.0f);
		mvStack.pushMatrix();
		mvStack.rotate((float)tf, 0.0f, 1.0f, 0.0f);
		mvStack.scale(0.6f, 0.6f, 0.6f);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		mvStack.popMatrix();

		// mini trapezoid prism
		mvStack.pushMatrix();
		mvStack.translate((float)Math.cos(tf), (float)Math.sin(tf), 0);
		mvStack.rotate((float)tf, 0.0f, 0.0f, 1.0f);
		mvStack.scale(0.2f, 0.2f, 0.2f);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		mvStack.popMatrix();  
		mvStack.popMatrix();  
		mvStack.popMatrix();
		mvStack.popMatrix();
	}

	public void init(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		startTime = System.currentTimeMillis();
		renderingProgram = Utils.createShaderProgram("a2/vertShader.glsl", "a2/fragShader.glsl");
		setupVertices();
		
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 10.0f;
	}

	private void setupVertices()
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();

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
	}

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}
}