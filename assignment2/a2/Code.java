package a2;

import java.nio.*;
import javax.swing.*;
import java.awt.event.*;
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
	private int vbo[] = new int[9];
	private double startTime = 0.0;
	private double elapsedTime;
	private double tf;
	private float cameraX, cameraY, cameraZ;
	private float orientX, orientY, orientZ;
	private float cubeLocX, cubeLocY, cubeLocZ;
	private float trapeLocX, trapeLocY, trapeLocZ;
	private float objLocX, objLocY, objLocZ;
	private Camera cam = new Camera();
	private boolean axesOn;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);  // buffer for transfering matrix to uniform
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private int mvLoc, pLoc, axesmvLoc, axespLoc;
	private float aspect;

	// object variables
	private int tileTexture;
	private int dolphinTexture;
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
		setKeybinds(myCanvas);
	}

	public void display(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		// time
		elapsedTime = System.currentTimeMillis() - startTime;
		tf = elapsedTime/1000.0;
		

		// render axes program
		gl.glUseProgram(axesProgram);
		axesmvLoc = gl.glGetUniformLocation(axesProgram, "mv_matrix");
		axespLoc = gl.glGetUniformLocation(axesProgram, "p_matrix");
		// axes lines drawn
		gl.glUniformMatrix4fv(axesmvLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(axespLoc, 1, false, pMat.get(vals));

		if(axesOn == true) {
			gl.glDrawArrays(GL_LINES, 0, 6);
		}

		// render object program
		gl.glUseProgram(objectProgram);
		mvLoc = gl.glGetUniformLocation(objectProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(objectProgram, "p_matrix");
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		// camera location and orientation
		cameraX = cam.getX(); cameraY = cam.getY(); cameraZ = cam.getZ();
		orientX = cam.getRX(); orientY = cam.getRY(); orientZ = cam.getRZ();

		//System.out.println("(" + cam.getX() + ", " + cam.getY() + ", " + cam.getZ() + ")");
		vMat.identity().translation(-cameraX, -cameraY, -cameraZ);
		vMat.rotate(-orientX, -orientY, -orientZ, 0.0f);

		// cube object
		// mMat.identity().translation(0.0f, -22.0f, 0.0f);
		// mMat.scale(20.0f, 20.0f, 50.0f);
		// mvMat.identity();
		// mvMat.mul(vMat);
		// mvMat.mul(mMat);
		// gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		// gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		// // associate VBO with the corresponding vertex attribute in the vertex shader
		// gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		// gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		// gl.glEnableVertexAttribArray(0);
		// //adjust OpenGL and draw cube
		// gl.glEnable(GL_DEPTH_TEST);
 		// gl.glDepthFunc(GL_LEQUAL);
		// gl.glDrawArrays(GL_TRIANGLES, 0, 36);

		// cube object
		mMat.identity().translation(cubeLocX, cubeLocY, cubeLocZ);
		mMat.rotate((float)tf, (float)Math.cos(tf), (float)Math.sin(tf), 0.0f);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		// associate VBO with the corresponding vertex attribute in the vertex shader
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		//texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, tileTexture);
		//adjust OpenGL and draw cube
		gl.glEnable(GL_DEPTH_TEST);
 		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);

		// trapezoidal prism object
		mMat.identity();
		mMat.translation(trapeLocX, trapeLocY, trapeLocZ);
		mMat.translate((float)Math.sin(tf)*4.0f, 0.0f, (float)Math.cos(tf)*4.0f);
		mMat.rotate((float)tf, 0.0f, 1.0f, 0.0f);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);


		// draw dolphin obj file
		mMat.identity().translation(objLocX, objLocY, objLocZ);
		mMat.rotate((float)tf, 0.0f, 1.0f, 0.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, dolphinTexture);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());




	
	}

	public void init(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		startTime = System.currentTimeMillis();
		axesProgram = Utils.createShaderProgram("a2/vertShaderAxes.glsl", "a2/fragShaderAxes.glsl");
		objectProgram = Utils.createShaderProgram("a2/vertShader.glsl", "a2/fragShader.glsl");
		myModel = new ImportedModel("dolphinHighPoly.obj");
		setupVertices();
		
		cubeLocX = 0.0f; cubeLocY = -2.0f; cubeLocZ = 0.0f;
		trapeLocX = 3.0f; trapeLocY = 0.0f; trapeLocZ = 0.0f;
		objLocX = 0.0f; objLocY = 0.0f; objLocZ = 0.0f;
		axesOn = true;

		tileTexture = Utils.loadTextureAWT("floor_color.jpg");
		dolphinTexture = Utils.loadTextureAWT("Dolphin_HighPolyUV.png");
		
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

		float[] cubeTextureCoordinates =
		{	
			0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f
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

		//obj
		numObjVertices = myModel.getNumVertices();
		Vector3f[] vertices = myModel.getVertices();
		Vector2f[] texCoords = myModel.getTexCoords();
		Vector3f[] normals = myModel.getNormals();
		
		float[] pvalues = new float[numObjVertices*3];
		float[] tvalues = new float[numObjVertices*2];
		float[] nvalues = new float[numObjVertices*3];
		
		for (int i=0; i<numObjVertices; i++)
		{	pvalues[i*3]   = (float) (vertices[i]).x();
			pvalues[i*3+1] = (float) (vertices[i]).y();
			pvalues[i*3+2] = (float) (vertices[i]).z();
			tvalues[i*2]   = (float) (texCoords[i]).x();
			tvalues[i*2+1] = (float) (texCoords[i]).y();
			nvalues[i*3]   = (float) (normals[i]).x();
			nvalues[i*3+1] = (float) (normals[i]).y();
			nvalues[i*3+2] = (float) (normals[i]).z();
		}


		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer texBuf1 = Buffers.newDirectFloatBuffer(cubeTextureCoordinates);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf1.limit()*4, texBuf1, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer tpzBuf = Buffers.newDirectFloatBuffer(trapezoidPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, tpzBuf.limit()*4, tpzBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL_STATIC_DRAW);

	}

	public Camera getCamera() {
		return cam;
	}

	public void setKeybinds(GLCanvas myCanvas) {
		myCanvas.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_W) {
					cam.moveForward(0.1f);
				}
				if (e.getKeyCode() == KeyEvent.VK_A) {
					cam.strafeLeft(0.1f);
				}
				if (e.getKeyCode() == KeyEvent.VK_S) {
					cam.moveBackward(0.1f);
				}
				if (e.getKeyCode() == KeyEvent.VK_D) {
					cam.strafeRight(0.1f);
				}
				if (e.getKeyCode() == KeyEvent.VK_Q) {
					cam.moveUpward(0.1f);
				}
				if (e.getKeyCode() == KeyEvent.VK_E) {
					cam.moveDownward(0.1f);
				}
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					cam.panLeft(0.1f);
				}
				if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					cam.panRight(0.1f);
				}
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					cam.pitchUp(1.0f);
				}
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					cam.pitchDown(1.0f);
				}
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					axesOn = !axesOn;
				}
			}      
		});
		myCanvas.setFocusable(true);
		myCanvas.requestFocus();
	}

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}
}