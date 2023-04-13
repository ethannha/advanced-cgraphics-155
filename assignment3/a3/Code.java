package a3;

import java.nio.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
	private int objectProgram, axesProgram, cubemapProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[5];
	private double startTime = 0.0;
	private double elapsedTime;
	private double tf;
	private float deltaTime = 0.0f;
	private long lastFrame;

	private float cameraX, cameraY, cameraZ;
	private float bigCubeLocX, bigCubeLocY, bigCubeLocZ;
	private float objLocX, objLocY, objLocZ;
	private Camera cam;
	private boolean axesOn;
	private int textureScaleLocation;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);  // buffer for transfering matrix to uniform
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mvLoc, pLoc, axesmvLoc, axespLoc, vLoc, nLoc, mLoc;
	private float aspect;

	// lighting variables
	private Vector3f initialLightLoc = new Vector3f();
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];

	// white light properties
	float[] globalAmbient = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightAmbient = new float[] { 0.1f, 0.1f, 0.1f, 1.0f };
	float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// gold material
	float[] matAmb = Utils.goldAmbient();
	float[] matDif = Utils.goldDiffuse();
	float[] matSpe = Utils.goldSpecular();
	float matShi = Utils.goldShininess();

	// object variables
	private int duckTexture, planeTexture, skyboxTexture;

	private int numObjVertices;
	private ImportedModel myModel;

	public Code()
	{	setTitle("CSC 155 - Assignment 3");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
		setKeybinds(myCanvas);
	}

	public void init(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		startTime = System.currentTimeMillis();
		lastFrame = System.currentTimeMillis();
		axesProgram = Utils.createShaderProgram("a3/vertAxesShader.glsl", "a3/fragAxesShader.glsl");
		objectProgram = Utils.createShaderProgram("a3/vertShader.glsl", "a3/fragShader.glsl");
		cubemapProgram = Utils.createShaderProgram("a3/vertCShader.glsl", "a3/fragCShader.glsl");
		myModel = new ImportedModel("assets/models/duck.obj");
		setupVertices();
		
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 8.0f;
		Vector3f camLoc = new Vector3f(cameraX, cameraY, cameraZ);
		cam = new Camera(camLoc);

		bigCubeLocX = 0.0f; bigCubeLocY = -4.0f; bigCubeLocZ = 0.0f;
		objLocX = 0.0f; objLocY = -2.0f; objLocZ = 0.0f;
		axesOn = true;

		duckTexture = Utils.loadTextureAWT("assets/textures/duck_uv.png");
		planeTexture = Utils.loadTextureAWT("assets/textures/pepe_tile.jpg");
		skyboxTexture = Utils.loadCubeMap("assets/cubeMap");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

	}

	public void display(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		// ====== TIME ELAPSED AND FRAMES SET UP ======
		elapsedTime = System.currentTimeMillis() - startTime;
		tf = elapsedTime/1000.0;
		
		long currentFrame = System.currentTimeMillis();
		deltaTime = (float) ((currentFrame - lastFrame) / 1000.0);
		lastFrame = currentFrame;

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		// ====== RENDER CUBE MAP PROGRAM ======
		gl.glUseProgram(cubemapProgram);

		vLoc = gl.glGetUniformLocation(cubemapProgram, "v_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));

		pLoc = gl.glGetUniformLocation(cubemapProgram, "p_matrix");
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		
		// ======================================================================= render skybox

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);	     // cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);

		gl.glDisable(GL_CULL_FACE);		//enable faces for objects


		// ======= RENDER AXES PROGRAM =======
		gl.glUseProgram(axesProgram);
		axesmvLoc = gl.glGetUniformLocation(axesProgram, "mv_matrix");
		axespLoc = gl.glGetUniformLocation(axesProgram, "p_matrix");
		// draw axes lines
		gl.glUniformMatrix4fv(axesmvLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(axespLoc, 1, false, pMat.get(vals));
		if(axesOn == true) {
			gl.glDrawArrays(GL_LINES, 0, 6);
		}


		// ======= RENDER OBJECTS PROGRAM =======
		gl.glUseProgram(objectProgram);
		mvLoc = gl.glGetUniformLocation(objectProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(objectProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(objectProgram, "norm_matrix");

		// ======== CAMERA/VIEW MATRIX SET UP ========
		vMat.identity();
		vMat = cam.getViewMatrix();

		// LIGHTING
		// setup lights based on current light position
		initialLightLoc = new Vector3f(0.0f, -1.0f, 0.0f);
		currentLightPos.set(initialLightLoc);
		//currentLightPos.rotateAxis((float)Math.toRadians(0.3f * deltaTime), 0.0f, 0.0f, 1.0f);
		installLights();

		// mv matrix for normal vector, is inverse transpose of mvMat
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// ======== MODELS AND MODEL-VIEW MATRICES SET UP ========

		// ======================================================================= huge cube object
		mMat.identity().translation(bigCubeLocX, bigCubeLocY, bigCubeLocZ);
		mMat.scale(2.0f, 2.0f, 2.0f);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		

		// put matrices into uniforms
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// get the location of uniform variable "textureScale" in the shader program
        textureScaleLocation = gl.glGetUniformLocation(objectProgram, "textureScale");
        // set the value "textureScale"
        gl.glUniform1f(textureScaleLocation, 2.0f);

		// associate VBO with the corresponding vertex attribute in the vertex shader
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		//texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, planeTexture);
		//adjust OpenGL and draw cube
		gl.glEnable(GL_DEPTH_TEST);
 		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);

		// change scale back to 1.0f
		gl.glUniform1f(textureScaleLocation, 1.0f);


		// ======================================================================= duck obj file
		mMat.identity().translation(objLocX, objLocY, objLocZ);
		mMat.rotate((float)tf, 0.0f, 1.0f, 0.0f);
		mMat.scale(1.0f, 1.0f, 1.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, duckTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
		gl.glGenerateMipmap(GL_TEXTURE_2D);


		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());

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
			0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f,

			1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,

			1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,

			1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,

			0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f,

			0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f
		};
		

		//obj file vertices
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

		// ======= VAOs & VBOs =======
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer bigCubeBuf = Buffers.newDirectFloatBuffer(cubeTextureCoordinates);
		gl.glBufferData(GL_ARRAY_BUFFER, bigCubeBuf.limit()*4, bigCubeBuf, GL_STATIC_DRAW);


		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL_STATIC_DRAW);


	}

	private void installLights()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		
		currentLightPos.mulPosition(vMat);
		lightPos[0]=currentLightPos.x();
		lightPos[1]=currentLightPos.y();
		lightPos[2]=currentLightPos.z();
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(objectProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(objectProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(objectProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(objectProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(objectProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(objectProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(objectProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(objectProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(objectProgram, "material.shininess");
	
		// set the uniform light and material values in the shader
		gl.glProgramUniform4fv(objectProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(objectProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(objectProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(objectProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(objectProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(objectProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(objectProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(objectProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(objectProgram, mshiLoc, matShi);
	}

	public void setKeybinds(GLCanvas myCanvas) {
		myCanvas.addKeyListener(new KeyAdapter() {
			Set<Integer> pressedKeys = new HashSet<>();
			
			public void keyPressed(KeyEvent e) {
				pressedKeys.add(e.getKeyCode());
				
				if (!pressedKeys.isEmpty()) {
					for (Iterator<Integer> it = pressedKeys.iterator(); it.hasNext();) {
						switch (it.next()) {
							case KeyEvent.VK_W:
								cam.moveForward(10.0f * deltaTime);
								break;
							case  KeyEvent.VK_A:
								cam.strafeLeft(10.0f * deltaTime);
								break;
							case KeyEvent.VK_S:
								cam.moveBackward(10.0f * deltaTime);
								break;
							case KeyEvent.VK_D:
								cam.strafeRight(10.0f * deltaTime);
								break;
							case KeyEvent.VK_Q:
								cam.moveUpward(10.0f * deltaTime);
								break;
							case KeyEvent.VK_E:
								cam.moveDownward(10.0f * deltaTime);
								break;
							case KeyEvent.VK_LEFT:
								cam.pan(80.0f * deltaTime);
								break;
							case KeyEvent.VK_RIGHT:
								cam.pan(-80.0f * deltaTime);
								break;
							case KeyEvent.VK_UP:
								cam.pitch(80.0f * deltaTime);
								break;
							case  KeyEvent.VK_DOWN:
								cam.pitch(-80.0f * deltaTime);
								break;
							case KeyEvent.VK_SPACE:
								axesOn = !axesOn;
								break;
						}
					}
				}
			}
			public void keyReleased(KeyEvent e) {
				pressedKeys.remove(e.getKeyCode());
			}   
		});
		myCanvas.setFocusable(true);
		myCanvas.requestFocus();
	}

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}
}