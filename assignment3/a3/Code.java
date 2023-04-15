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
	private int vbo[] = new int[7];
	private double startTime = 0.0;
	private double elapsedTime;
	private double tf;
	private float deltaTime = 0.0f;
	private long lastFrame;

	private float cameraX, cameraY, cameraZ;	//location of camera
	private float plightX, plightY, plightZ;
	private float objLocX, objLocY, objLocZ;
	private float xOffset, yOffset;
	private Camera cam;
	private boolean axesOn, lightOn;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);  // buffer for transfering matrix to uniform
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4fStack mvStack = new Matrix4fStack(6);
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int axesvLoc, axespLoc, mLoc, vLoc, pLoc, nLoc;
	private float aspect;

	// lighting variables
	private Light lightCube;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private int intensityLoc;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];

	// light properties
	private float[] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	private float[] lightAmbient = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
	private float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	private float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// custom material properties
	private float[] matAmb = new float[] { 0.5f, 0.7f, 0.8f, 1.0f };
	private float[] matDif = new float[] { 0.8f, 0.9f, 1.0f, 1.0f };
	private float[] matSpe = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	private float matShi = 250.0f;

	// gold material properties
	private float[] matAmb2 = Utils.goldAmbient();
	private float[] matDif2 = Utils.goldDiffuse();
	private float[] matSpe2 = Utils.goldSpecular();
	private float matShi2 = Utils.goldShininess();

	// object variables
	private int skyboxTexture, duckTexture, ducklingTexture;
	private int numObjVertices, numObjVertices2;
	private ImportedModel duckModel, ducklingModel;

	public Code()
	{	setTitle("CSC 155 - Assignment 3");
		setSize(800, 800);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
		addMouseMotion(myCanvas);
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
		duckModel = new ImportedModel("assets/models/duck.obj");
		ducklingModel = new ImportedModel("assets/models/duckling.obj");
		setupVertices();
		
		cameraX = 0.0f; cameraY = 2.0f; cameraZ = 12.0f;
		Vector3f camLoc = new Vector3f(cameraX, cameraY, cameraZ);
		Vector3f tarLoc = new Vector3f(cameraX, cameraY, 0.0f);
		cam = new Camera(camLoc, tarLoc);
		plightX = 0.0f; plightY = 3.0f; plightZ = 0.0f;
		Vector3f initialLightLoc = new Vector3f(plightX, plightY, plightZ);
		lightCube = new Light(initialLightLoc);
		objLocX = -4.0f; objLocY = 0.0f; objLocZ = 2.0f;
		axesOn = true;
		lightOn = true;

		duckTexture = Utils.loadTextureAWT("assets/textures/duck_uv.png");
		ducklingTexture = Utils.loadTextureAWT("assets/textures/duckling_uv.png");
		skyboxTexture = Utils.loadCubeMap("assets/cubeMap");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

	}
	
	private void installLights(int renderingProgram)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();

		if (lightOn == true) {
			float lightIntensity = 1.0f;
			intensityLoc = gl.glGetUniformLocation(renderingProgram, "intensity");
			gl.glProgramUniform1f(renderingProgram, intensityLoc, lightIntensity);
		} else {
			float lightIntensity = 0.0f;
			intensityLoc = gl.glGetUniformLocation(renderingProgram, "intensity");
			gl.glProgramUniform1f(renderingProgram, intensityLoc, lightIntensity);
		}
		
		lightPos[0]=currentLightPos.x();
		lightPos[1]=currentLightPos.y();
		lightPos[2]=currentLightPos.z();
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
	
		// set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
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
		axesvLoc = gl.glGetUniformLocation(axesProgram, "v_matrix");
		axespLoc = gl.glGetUniformLocation(axesProgram, "p_matrix");
		// draw axes lines
		gl.glUniformMatrix4fv(axesvLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(axespLoc, 1, false, pMat.get(vals));
		if(axesOn == true) {
			gl.glDrawArrays(GL_LINES, 0, 6);
		}

		// ======= RENDER OBJECTS PROGRAM =======
		gl.glUseProgram(objectProgram);
		mLoc = gl.glGetUniformLocation(objectProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(objectProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(objectProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(objectProgram, "norm_matrix");

		// ======== CAMERA/VIEW MATRIX SET UP ========
		vMat.identity();
		mvStack.pushMatrix();
		vMat = cam.getViewMatrix();

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(objectProgram);


		// ======== MODELS AND MODEL-VIEW MATRICES SET UP ========

		// ======================================================================= light cube object
		mvStack.pushMatrix();
		mvStack.translation(lightCube.getX(), lightCube.getY(), lightCube.getZ());
		mvStack.pushMatrix();
		mvStack.scale(0.2f, 0.2f, 0.2f);
	
		mMat.identity();
		mMat.set(mvStack);
		mMat.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// put matrices into uniforms
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		// associate VBO with the corresponding vertex attribute in the vertex shader
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		//adjust OpenGL and draw cube
		if(lightOn) {
			gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		}
		mvStack.popMatrix();


		// ======================================================================= duck obj
		mvStack.pushMatrix();
		mvStack.translation(objLocX, objLocY, objLocZ);
		objLocX += 2.0f * deltaTime;
		if(objLocX > 4.0f){
			objLocX = -4.0f;
		}
		mvStack.pushMatrix();
		float radians = (float) Math.toRadians(-90);
		//mvStack.rotate(radians, 0.0f, 1.0f, 0.0f);

		mMat.identity();
		mMat.set(mvStack);
		mMat.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, duckTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
		gl.glGenerateMipmap(GL_TEXTURE_2D);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, duckModel.getNumVertices());
		mvStack.popMatrix();


		// ======================================================================= duckling obj


		mvStack.pushMatrix();
		mvStack.translate(((float)Math.sin(tf)*2.0f), 0.0f, ((float)Math.cos(tf)*2.0f));
		mvStack.pushMatrix();
		//mvStack.rotate((float)tf, 0.0f, 1.0f, 0.0f);

		mMat.identity();
		mMat.set(mvStack);
		mMat.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, ducklingTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
		gl.glGenerateMipmap(GL_TEXTURE_2D);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, ducklingModel.getNumVertices());
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
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
		
		// duck object
		numObjVertices = duckModel.getNumVertices();
		Vector3f[] vertices = duckModel.getVertices();
		Vector2f[] texCoords = duckModel.getTexCoords();
		Vector3f[] normals = duckModel.getNormals();
		
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

		// duckling object
		numObjVertices2 = ducklingModel.getNumVertices();
		Vector3f[] vertices2 = ducklingModel.getVertices();
		Vector2f[] texCoords2 = ducklingModel.getTexCoords();
		Vector3f[] normals2 = ducklingModel.getNormals();
		
		float[] pvalues2 = new float[numObjVertices2*3];
		float[] tvalues2 = new float[numObjVertices2*2];
		float[] nvalues2 = new float[numObjVertices2*3];
		
		for (int j=0; j<numObjVertices2; j++)
		{	pvalues2[j*3]   = (float) (vertices2[j]).x();
			pvalues2[j*3+1] = (float) (vertices2[j]).y();
			pvalues2[j*3+2] = (float) (vertices2[j]).z();
			tvalues2[j*2]   = (float) (texCoords2[j]).x();
			tvalues2[j*2+1] = (float) (texCoords2[j]).y();
			nvalues2[j*3]   = (float) (normals2[j]).x();
			nvalues2[j*3+1] = (float) (normals2[j]).y();
			nvalues2[j*3+2] = (float) (normals2[j]).z();
		}

		// ======= VAOs & VBOs =======
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);


		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer vertBuf2 = Buffers.newDirectFloatBuffer(pvalues2);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf2.limit()*4, vertBuf2, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer texBuf2 = Buffers.newDirectFloatBuffer(tvalues2);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf2.limit()*4, texBuf2, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer norBuf2 = Buffers.newDirectFloatBuffer(nvalues2);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf2.limit()*4, norBuf2, GL_STATIC_DRAW);

	}

	public void addMouseMotion(GLCanvas myCanvas) {
		myCanvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				xOffset = e.getX() - lightCube.getX();
				yOffset = e.getY() - lightCube.getZ();
			}
			public void mouseReleased(MouseEvent e) {}
		});
		myCanvas.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				float x = e.getX() - xOffset;
				float y = e.getY() - yOffset;
				x -= lightCube.getX() / 2;
				y -= lightCube.getZ() / 2;

				lightCube.setX(x/50);
				lightCube.setZ(y/50);
   			 }
		});
		myCanvas.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				float height = lightCube.getY();
				int rotation = e.getWheelRotation();
				if (rotation < 0) {
					// zoom in
					height += 0.2f;
				} else {
					// zoom out
					height -= 0.2f;
				}
				lightCube.setY(height);
			}
		});
		myCanvas.setFocusable(true);
		myCanvas.requestFocus();
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
								cam.pan(120.0f * deltaTime);
								break;
							case KeyEvent.VK_RIGHT:
								cam.pan(-120.0f * deltaTime);
								break;
							case KeyEvent.VK_UP:
								cam.pitch(120.0f * deltaTime);
								break;
							case  KeyEvent.VK_DOWN:
								cam.pitch(-120.0f * deltaTime);
								break;
							case KeyEvent.VK_SPACE:
								axesOn = !axesOn;
								break;
							case KeyEvent.VK_I:
								lightCube.moveForward(10.0f * deltaTime);
								break;
							case  KeyEvent.VK_J:
								lightCube.moveLeft(10.0f * deltaTime);
								break;
							case KeyEvent.VK_K:
								lightCube.moveBackward(10.0f * deltaTime);
								break;
							case KeyEvent.VK_L:
								lightCube.moveRight(10.0f * deltaTime);
								break;
							case KeyEvent.VK_U:
								lightCube.moveUpward(10.0f * deltaTime);
								break;
							case KeyEvent.VK_O:
								lightCube.moveDownward(10.0f * deltaTime);
								break;
							case KeyEvent.VK_F:
								lightOn = !lightOn;
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