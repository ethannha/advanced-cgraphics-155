package a4;

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
	private int renderingProgram, axesProgram, cubeMapProgram, planeProgram, normalMapProgram, envMapProgram, geometryProgram, lightCubeProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[28];
	private double startTime = 0.0;
	private double elapsedTime;
	private double tf;
	private float deltaTime = 0.0f;
	private long lastFrame;

	private float cameraX, cameraY, cameraZ;	//location of camera
	private float plightX, plightY, plightZ;
	private float planeX, planeY, planeZ;
	private float duckX, duckY, duckZ;
	private float ducklingX, ducklingY, ducklingZ;
	private float signX, signY, signZ;
	private float wellX, wellY, wellZ;
	private float sunX, sunY, sunZ;
	private float snowmanX, snowmanY, snowmanZ;
	private float reflectX, reflectY, reflectZ;

	private float xOffset, yOffset;
	private Camera cam;
	private boolean axesOn, lightOn;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);  // buffer for transfering matrix to uniform
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private Matrix4fStack mvStack = new Matrix4fStack(14);
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int axesvLoc, axespLoc, mLoc, vLoc, pLoc, nLoc, mvLoc;
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
	
	private float[] thisAmb, thisDif, thisSpe;
	private float thisShi;

	// custom material properties
	private float[] matAmb1 = new float[] { 0.5f, 0.7f, 0.8f, 1.0f };
	private float[] matDif1 = new float[] { 0.8f, 0.9f, 1.0f, 1.0f };
	private float[] matSpe1 = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	private float matShi1 = 20.0f;

	// gold material properties
	private float[] matAmb2 = Utils.goldAmbient();
	private float[] matDif2 = Utils.goldDiffuse();
	private float[] matSpe2 = Utils.goldSpecular();
	private float matShi2 = Utils.goldShininess();

	// silver material properties
	private float[] matAmb3 = Utils.silverAmbient();
	private float[] matDif3 = Utils.silverDiffuse();
	private float[] matSpe3 = Utils.silverSpecular();
	private float matShi3 = Utils.silverShininess();

	// object variables
	private int planeTexture, planeHeight, planeNormalMap;
	private int skyboxTexture, duckTexture, ducklingTexture, signTexture, wellTexture;
	private ImportedModel duckModel, ducklingModel, signModel, wellModel;

	private Sphere mySphere = new Sphere(48);
	private int sunNormalMap;
	private int sunTexture;
	private int numSphereVertices;


	private Torus myTorus = new Torus(0.8f, 1.0f, 48);
	private int numTorusVertices, numTorusIndices;
	private Torus mySnowman = new Torus(0.1f, 2.0f, 48);
	private int numSnowVertices, numSnowIndices;

	// anaglyph
	private float IOD = 0.01f;  // tunable interocular distance
	private float near = 0.01f;
	private float far = 100.0f;


	public Code()
	{	setTitle("CSC 155 - Assignment 4");
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
		axesProgram = Utils.createShaderProgram("a4/vertAxesShader.glsl", "a4/fragAxesShader.glsl");
		renderingProgram = Utils.createShaderProgram("a4/vertShader.glsl", "a4/fragShader.glsl");
		lightCubeProgram = Utils.createShaderProgram("a4/vertLightQBShader.glsl", "a4/fragLightQBShader.glsl");
		planeProgram = Utils.createShaderProgram("a4/vertTessShader.glsl", "a4/tessCShader.glsl", "a4/tessEShader.glsl", "a4/fragTessShader.glsl");
		cubeMapProgram = Utils.createShaderProgram("a4/vertCubeShader.glsl", "a4/fragCubeShader.glsl");
		envMapProgram = Utils.createShaderProgram("a4/vertEnvShader.glsl", "a4/fragEnvShader.glsl");
		geometryProgram = Utils.createShaderProgram("a4/vertGeomAddShader.glsl", "a4/geomShader.glsl", "a4/fragGeomAddShader.glsl");

		// object models
		duckModel = new ImportedModel("assets/models/duck.obj");
		ducklingModel = new ImportedModel("assets/models/duckling.obj");
		signModel = new ImportedModel("assets/models/sign.obj");
		wellModel = new ImportedModel("assets/models/well.obj");
		setupVertices();

		// object textures
		normalMapProgram = Utils.createShaderProgram("a4/vertNormalShader.glsl", "a4/fragNormalShader.glsl");
		sunTexture = Utils.loadTexture("assets/textures/sun.jpg");
		sunNormalMap = Utils.loadTexture("assets/textures/sun_normal.jpg");

		duckTexture = Utils.loadTextureAWT("assets/textures/duck_uv.png");
		ducklingTexture = Utils.loadTextureAWT("assets/textures/duckling_uv.png");
		signTexture = Utils.loadTextureAWT("assets/textures/sign_uv.png");
		wellTexture = Utils.loadTextureAWT("assets/textures/blinn4_baseColor.jpg");

		planeTexture = Utils.loadTexture("assets/textures/terrain.jpg");
		planeHeight = Utils.loadTexture("assets/textures/terrain_bump.jpg");
		planeNormalMap = Utils.loadTexture("assets/textures/terrain_normal.jpg");
		
		skyboxTexture = Utils.loadCubeMap("assets/cubeMap");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

		axesOn = false;
		lightOn = true;
		
		cameraX = 1.5f; cameraY = 2.0f; cameraZ = 6.0f;
		Vector3f camLoc = new Vector3f(cameraX, cameraY, cameraZ);
		Vector3f tarLoc = new Vector3f(cameraX, cameraY, 0.0f);
		cam = new Camera(camLoc, tarLoc);

		plightX = 0.0f; plightY = 3.0f; plightZ = 0.0f;
		Vector3f initialLightLoc = new Vector3f(plightX, plightY, plightZ);
		lightCube = new Light(initialLightLoc);

		// object locations
		planeX = 0.0f; 		planeY = -1.0f; 	planeZ = 0.0f;
		duckX = -0.5f; 		duckY = -0.1f; 		duckZ = 1.5f;
		ducklingX = 0.0f; 	ducklingY = 1.83f; 	ducklingZ = -1.5f;
		signX = -1.5f; 		signY = -0.2f; 		signZ = -0.8f;
		wellX = 0.0f; 		wellY = -0.2f; 		wellZ = -3.0f;
		sunX = 4.0f; 		sunY = 12.0f; 		sunZ = -8.0f;
		snowmanX = 5.0f; 	snowmanY = 0.5f; 	snowmanZ = -2.0f;
		reflectX = 0.1f; 	reflectY = 0.4f; 	reflectZ = -3.05f;

		//initially use custom material
		thisAmb = matAmb1;
		thisDif = matDif1;
		thisSpe = matSpe1;
		thisShi = matShi1;
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
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, thisAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, thisDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, thisSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, thisShi);
	}

	private void computePerspectiveMatrix(float leftRight)
	{	float top = (float)Math.tan(1.0472f / 2.0f) * (float)near;
		float bottom = -top;
		float frustumshift = (IOD / 2.0f) * near / far;
		float left = -aspect * top - frustumshift * leftRight;
		float right = aspect * top - frustumshift * leftRight;
		pMat.setFrustum(left, right, bottom, top, near, far);
	}

	public void display(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glColorMask(true, true, true, true);
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClearColor(0.7f, 0.8f, 0.8f, 1.0f); // background fog color is bluish-grey
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		// ====== TIME ELAPSED AND FRAMES SET UP ======
		elapsedTime = System.currentTimeMillis() - startTime;
		tf = elapsedTime/1000.0;
		long currentFrame = System.currentTimeMillis();
		deltaTime = (float) ((currentFrame - lastFrame) / 1000.0);
		lastFrame = currentFrame;

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		gl.glColorMask(true, false, false, false);
		scene(-2.0f);
				
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		gl.glColorMask(false, true, true, false);
		scene(2.0f);
	}

	public void scene(float leftRight) 
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		computePerspectiveMatrix(leftRight);

		// ======== CAMERA/VIEW MATRIX SET UP ========
		vMat.identity();
		mvStack.pushMatrix();
		vMat = cam.getViewMatrix();
		vMat.translate(-(leftRight * IOD/2.0f), 0.0f, 0.0f);

		// ====== RENDER CUBE MAP PROGRAM ======
		gl.glUseProgram(cubeMapProgram);

		vLoc = gl.glGetUniformLocation(cubeMapProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(cubeMapProgram, "p_matrix");
		
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		
// ======================================================================= render skybox

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(7, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(7);
		
		gl.glActiveTexture(GL_TEXTURE4);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);	     // cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);

		gl.glDisable(GL_CULL_FACE);		//enable faces for objects


		// ====== RENDER PLANE PROGRAM ======
		gl.glUseProgram(planeProgram);

		mLoc = gl.glGetUniformLocation(planeProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(planeProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(planeProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(planeProgram, "norm_matrix");
		
// ============================================================================ render the plane
		

		mvStack.pushMatrix();
		mvStack.translation(planeX, planeY, planeZ);
		mvStack.scale(30.0f, 30.0f, 30.0f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		currentLightPos.set(lightCube.getLocation());
		installLights(planeProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		
		gl.glActiveTexture(GL_TEXTURE4);
		gl.glBindTexture(GL_TEXTURE_2D, planeTexture);
		gl.glActiveTexture(GL_TEXTURE5);
		gl.glBindTexture(GL_TEXTURE_2D, planeHeight);
		gl.glActiveTexture(GL_TEXTURE6);
		gl.glBindTexture(GL_TEXTURE_2D, planeNormalMap);
	
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CW);

		gl.glPatchParameteri(GL_PATCH_VERTICES, 4);
		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		gl.glDrawArraysInstanced(GL_PATCHES, 0, 4, 64*64);

		gl.glDisable(GL_CULL_FACE);

		mvStack.popMatrix();


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

		// ======= RENDER LIGHT CUBE PROGRAM =======
		gl.glUseProgram(lightCubeProgram);
		mLoc = gl.glGetUniformLocation(lightCubeProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(lightCubeProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(lightCubeProgram, "p_matrix");
// ======================================================================= light cube object
		mvStack.pushMatrix();
		mvStack.translation(lightCube.getX(), lightCube.getY(), lightCube.getZ());
		mvStack.scale(0.1f, 0.1f, 0.1f);
		mvStack.pushMatrix();
	
		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(lightCubeProgram);

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


		// ======= RENDER OBJECTS PROGRAM =======
		gl.glUseProgram(renderingProgram);

		mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

// ======================================================================= duck obj
		mvStack.pushMatrix();
		mvStack.translation(duckX, duckY, duckZ);
		mvStack.scale(0.3f, 0.3f, 0.3f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(renderingProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, duckTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
		gl.glGenerateMipmap(GL_TEXTURE_2D);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, duckModel.getNumVertices());
		mvStack.popMatrix();


// ======================================================================= duckling obj
		thisAmb = matAmb2;
		thisDif = matDif2;
		thisSpe = matSpe2;
		thisShi = matShi2;

		mvStack.pushMatrix();
		mvStack.translation(ducklingX, ducklingY, ducklingZ);
		mvStack.scale(0.5f, 0.5f, 0.5f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(renderingProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, ducklingTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
		gl.glGenerateMipmap(GL_TEXTURE_2D);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, ducklingModel.getNumVertices());
		mvStack.popMatrix();


// ======================================================================= sign obj

		mvStack.pushMatrix();
		mvStack.translation(signX, signY, signZ);
		mvStack.scale(0.1f, 0.1f, 0.1f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(renderingProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
		gl.glBindTexture(GL_TEXTURE_2D, signTexture);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, signModel.getNumVertices());
		mvStack.popMatrix();


// ======================================================================= well obj
		thisAmb = matAmb1;
		thisDif = matDif1;
		thisSpe = matSpe1;
		thisShi = matShi1;

		mvStack.pushMatrix();
		mvStack.translation(wellX, wellY, wellZ);
		mvStack.scale(0.1f, 0.1f, 0.1f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(renderingProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
		gl.glBindTexture(GL_TEXTURE_2D, wellTexture);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, wellModel.getNumVertices());
		mvStack.popMatrix();
		



		// ====== RENDER NORMAL MAP PROGRAM ======
		gl.glUseProgram(normalMapProgram);

				
		mLoc = gl.glGetUniformLocation(normalMapProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(normalMapProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(normalMapProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(normalMapProgram, "norm_matrix");

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		
//==================================================== render normal mapped sun sphere

		mvStack.pushMatrix();
		mvStack.translation(sunX, sunY, sunZ);
		mvStack.scale(3.0f, 3.0f, 3.0f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(normalMapProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[16]);
		gl.glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(3);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[17]);
		gl.glVertexAttribPointer(4, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(5);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[18]);
		gl.glVertexAttribPointer(5, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(4);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[19]);
		gl.glVertexAttribPointer(6, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(6);
		
		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_2D, sunNormalMap);

		gl.glActiveTexture(GL_TEXTURE3);
		gl.glBindTexture(GL_TEXTURE_2D, sunTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVertices);
		mvStack.popMatrix();


		// ====== RENDER NORMAL MAP PROGRAM ======
		gl.glUseProgram(geometryProgram);

		mLoc = gl.glGetUniformLocation(geometryProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(geometryProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(geometryProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(geometryProgram, "norm_matrix");

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

//==================================================== render geometry addition snowball
		
		thisAmb = matAmb1;
		thisDif = matDif1;
		thisSpe = matSpe1;
		thisShi = matShi1;

		mvStack.pushMatrix();
		mvStack.translation(snowmanX, snowmanY, snowmanZ);
		mvStack.scale(0.5f, 0.5f, 0.5f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(geometryProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[24]);
		gl.glVertexAttribPointer(9, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(9);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[26]);
		gl.glVertexAttribPointer(10, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(10);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[27]);
		gl.glDrawElements(GL_TRIANGLES, numSnowIndices, GL_UNSIGNED_INT, 0);

		mvStack.popMatrix();

//==================================================== render geometry addition snowball2

		mvStack.pushMatrix();
		mvStack.translation(snowmanX, snowmanY+1.5f, snowmanZ);
		mvStack.scale(0.4f, 0.4f, 0.4f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(geometryProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[24]);
		gl.glVertexAttribPointer(9, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(9);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[26]);
		gl.glVertexAttribPointer(10, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(10);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[27]);
		gl.glDrawElements(GL_TRIANGLES, numSnowIndices, GL_UNSIGNED_INT, 0);

		mvStack.popMatrix();


//==================================================== render geometry addition snowball3

		mvStack.pushMatrix();
		mvStack.translation(snowmanX, snowmanY+2.7f, snowmanZ);
		mvStack.scale(0.3f, 0.3f, 0.3f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvStack.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(geometryProgram);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[24]);
		gl.glVertexAttribPointer(9, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(9);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[26]);
		gl.glVertexAttribPointer(10, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(10);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[27]);
		gl.glDrawElements(GL_TRIANGLES, numSnowIndices, GL_UNSIGNED_INT, 0);

		mvStack.popMatrix();

// ======================================================================= render reflective torus
		
		gl.glUseProgram(envMapProgram);
		
		mvLoc = gl.glGetUniformLocation(envMapProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(envMapProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(envMapProgram, "norm_matrix");

		mvStack.pushMatrix();
		mvStack.translation(reflectX, reflectY, reflectZ);
		mvStack.scale(0.9f, 0.9f, 0.9f);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		
		mvStack.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(envMapProgram);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[20]);
		gl.glVertexAttribPointer(7, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(7);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[22]);
		gl.glVertexAttribPointer(8, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(8);

		gl.glActiveTexture(GL_TEXTURE4);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[23]);
		gl.glDrawElements(GL_TRIANGLES, numTorusIndices, GL_UNSIGNED_INT, 0);
		
		mvStack.popMatrix();

		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
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
/*
		float[] PLANE_POSITIONS = {
			-60.0f, 0.0f, -60.0f,  -60.0f, 0.0f, 60.0f,  60.0f, 0.0f, -60.0f,  
			60.0f, 0.0f, -60.0f,  -60.0f, 0.0f, 60.0f,  60.0f, 0.0f, 60.0f
		};
		float[] PLANE_TEXCOORDS = {
			0.0f, 0.0f,  0.0f, 1.0f,  1.0f, 0.0f,
			1.0f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f
		};
		float[] PLANE_NORMALS = {
			0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f
		};
*/	
		// duck object
		Vector3f[] vertices = duckModel.getVertices();
		Vector2f[] texCoords = duckModel.getTexCoords();
		Vector3f[] normals = duckModel.getNormals();
		
		float[] duckPvalues = new float[duckModel.getNumVertices()*3];
		float[] duckNvalues = new float[duckModel.getNumVertices()*3];
		float[] duckTvalues = new float[duckModel.getNumVertices()*2];
		
		for (int i=0; i<duckModel.getNumVertices(); i++)
		{	duckPvalues[i*3]   = (float) (vertices[i]).x();
			duckPvalues[i*3+1] = (float) (vertices[i]).y();
			duckPvalues[i*3+2] = (float) (vertices[i]).z();
			duckNvalues[i*3]   = (float) (normals[i]).x();
			duckNvalues[i*3+1] = (float) (normals[i]).y();
			duckNvalues[i*3+2] = (float) (normals[i]).z();
			duckTvalues[i*2]   = (float) (texCoords[i]).x();
			duckTvalues[i*2+1] = (float) (texCoords[i]).y();
		}

		// duckling object
		Vector3f[] vertices2 = ducklingModel.getVertices();
		Vector2f[] texCoords2 = ducklingModel.getTexCoords();
		Vector3f[] normals2 = ducklingModel.getNormals();
		
		float[] ducklingPvalues = new float[ducklingModel.getNumVertices()*3];
		float[] ducklingNvalues = new float[ducklingModel.getNumVertices()*3];
		float[] ducklingTvalues = new float[ducklingModel.getNumVertices()*2];

		for (int i=0; i<ducklingModel.getNumVertices(); i++)
		{	ducklingPvalues[i*3]   = (float) (vertices2[i]).x();
			ducklingPvalues[i*3+1] = (float) (vertices2[i]).y();
			ducklingPvalues[i*3+2] = (float) (vertices2[i]).z();
			ducklingNvalues[i*3]   = (float) (normals2[i]).x();
			ducklingNvalues[i*3+1] = (float) (normals2[i]).y();
			ducklingNvalues[i*3+2] = (float) (normals2[i]).z();
			ducklingTvalues[i*2]   = (float) (texCoords2[i]).x();
			ducklingTvalues[i*2+1] = (float) (texCoords2[i]).y();
		}

		// sign object
		Vector3f[] vertices3 = signModel.getVertices();
		Vector2f[] texCoords3 = signModel.getTexCoords();
		Vector3f[] normals3 = signModel.getNormals();
		
		float[] signPvalues = new float[signModel.getNumVertices()*3];
		float[] signNvalues = new float[signModel.getNumVertices()*3];
		float[] signTvalues = new float[signModel.getNumVertices()*2];

		for (int i=0; i<signModel.getNumVertices(); i++)
		{	signPvalues[i*3]   = (float) (vertices3[i]).x();
			signPvalues[i*3+1] = (float) (vertices3[i]).y();
			signPvalues[i*3+2] = (float) (vertices3[i]).z();
			signNvalues[i*3]   = (float) (normals3[i]).x();
			signNvalues[i*3+1] = (float) (normals3[i]).y();
			signNvalues[i*3+2] = (float) (normals3[i]).z();
			signTvalues[i*2]   = (float) (texCoords3[i]).x();
			signTvalues[i*2+1] = (float) (texCoords3[i]).y();
		}

		// well object
		Vector3f[] vertices4 = wellModel.getVertices();
		Vector2f[] texCoords4 = wellModel.getTexCoords();
		Vector3f[] normals4 = wellModel.getNormals();
		
		float[] wellPvalues = new float[wellModel.getNumVertices()*3];
		float[] wellNvalues = new float[wellModel.getNumVertices()*3];
		float[] wellTvalues = new float[wellModel.getNumVertices()*2];

		for (int i=0; i<wellModel.getNumVertices(); i++)
		{	wellPvalues[i*3]   = (float) (vertices4[i]).x();
			wellPvalues[i*3+1] = (float) (vertices4[i]).y();
			wellPvalues[i*3+2] = (float) (vertices4[i]).z();
			wellNvalues[i*3]   = (float) (normals4[i]).x();
			wellNvalues[i*3+1] = (float) (normals4[i]).y();
			wellNvalues[i*3+2] = (float) (normals4[i]).z();
			wellTvalues[i*2]   = (float) (texCoords4[i]).x();
			wellTvalues[i*2+1] = (float) (texCoords4[i]).y();
		}


		//sun
		numSphereVertices = mySphere.getIndices().length;
		
		int[] indices = mySphere.getIndices();
		Vector3f[] vertices5 = mySphere.getVertices();
		Vector2f[] texCoords5 = mySphere.getTexCoords();
		Vector3f[] normals5 = mySphere.getNormals();
		Vector3f[] tangents5 = mySphere.getTangents();
		
		float[] normalPvalues = new float[indices.length*3];
		float[] normalTvalues = new float[indices.length*2];
		float[] normalNvalues = new float[indices.length*3];
		float[] normalTanvalues = new float[indices.length*3];

		for (int i=0; i<indices.length; i++)
		{	normalPvalues[i*3]   = (float) (vertices5[indices[i]]).x();
			normalPvalues[i*3+1] = (float) (vertices5[indices[i]]).y();
			normalPvalues[i*3+2] = (float) (vertices5[indices[i]]).z();
			normalTvalues[i*2]   = (float) (texCoords5[indices[i]]).x();
			normalTvalues[i*2+1] = (float) (texCoords5[indices[i]]).y();
			normalNvalues[i*3]   = (float) (normals5[indices[i]]).x();
			normalNvalues[i*3+1] = (float) (normals5[indices[i]]).y();
			normalNvalues[i*3+2] = (float) (normals5[indices[i]]).z();
			normalTanvalues[i*3] = (float) (tangents5[indices[i]]).x();
			normalTanvalues[i*3+1] = (float) (tangents5[indices[i]]).y();
			normalTanvalues[i*3+2] = (float) (tangents5[indices[i]]).z();
		}

		// torus
		numTorusVertices = myTorus.getNumVertices();
		numTorusIndices = myTorus.getNumIndices();

		Vector3f[] torVertices = myTorus.getVertices();
		Vector2f[] torTexCoords = myTorus.getTexCoords();
		Vector3f[] torNormals = myTorus.getNormals();
		int[] torIndices = myTorus.getIndices();

		float[] torPvalues = new float[vertices.length*3];
		float[] torTvalues = new float[texCoords.length*2];
		float[] torNvalues = new float[normals.length*3];

		for (int i=0; i<numTorusVertices; i++)
		{	torPvalues[i*3]   = (float) torVertices[i].x();
			torPvalues[i*3+1] = (float) torVertices[i].y();
			torPvalues[i*3+2] = (float) torVertices[i].z();
			torTvalues[i*2]   = (float) torTexCoords[i].x();
			torTvalues[i*2+1] = (float) torTexCoords[i].y();
			torNvalues[i*3]   = (float) torNormals[i].x();
			torNvalues[i*3+1] = (float) torNormals[i].y();
			torNvalues[i*3+2] = (float) torNormals[i].z();
		}

		// snowman
		numSnowVertices = mySnowman.getNumVertices();
		numSnowIndices = mySnowman.getNumIndices();

		Vector3f[] snowVertices = mySnowman.getVertices();
		Vector2f[] snowTexCoords = mySnowman.getTexCoords();
		Vector3f[] snowNormals = mySnowman.getNormals();
		int[] snowIndices = mySnowman.getIndices();

		float[] snowPvalues = new float[vertices.length*3];
		float[] snowTvalues = new float[texCoords.length*2];
		float[] snowNvalues = new float[normals.length*3];

		for (int i=0; i<numSnowVertices; i++)
		{	snowPvalues[i*3]   = (float) snowVertices[i].x();
			snowPvalues[i*3+1] = (float) snowVertices[i].y();
			snowPvalues[i*3+2] = (float) snowVertices[i].z();
			snowTvalues[i*2]   = (float) snowTexCoords[i].x();
			snowTvalues[i*2+1] = (float) snowTexCoords[i].y();
			snowNvalues[i*3]   = (float) snowNormals[i].x();
			snowNvalues[i*3+1] = (float) snowNormals[i].y();
			snowNvalues[i*3+2] = (float) snowNormals[i].z();
		}

		// buffers definition
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		// cubemap
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);

/*
		// plane vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer planeVertBuf = Buffers.newDirectFloatBuffer(PLANE_POSITIONS);
		gl.glBufferData(GL_ARRAY_BUFFER, planeVertBuf.limit()*4, planeVertBuf, GL_STATIC_DRAW);
		// plane normal coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer planeNorBuf = Buffers.newDirectFloatBuffer(PLANE_NORMALS);
		gl.glBufferData(GL_ARRAY_BUFFER, planeNorBuf.limit()*4, planeNorBuf, GL_STATIC_DRAW);
		// plane texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer planeTexBuf = Buffers.newDirectFloatBuffer(PLANE_TEXCOORDS);
		gl.glBufferData(GL_ARRAY_BUFFER, planeTexBuf.limit()*4, planeTexBuf, GL_STATIC_DRAW);
 */
		// duck vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer duckVertBuf = Buffers.newDirectFloatBuffer(duckPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, duckVertBuf.limit()*4, duckVertBuf, GL_STATIC_DRAW);
		// duck normal coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer duckNorBuf = Buffers.newDirectFloatBuffer(duckNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, duckNorBuf.limit()*4, duckNorBuf, GL_STATIC_DRAW);
		// duck texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer duckTexBuf = Buffers.newDirectFloatBuffer(duckTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, duckTexBuf.limit()*4, duckTexBuf, GL_STATIC_DRAW);

		// duckling vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer ducklingVertBuf = Buffers.newDirectFloatBuffer(ducklingPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, ducklingVertBuf.limit()*4, ducklingVertBuf, GL_STATIC_DRAW);
		// duckling normal coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer ducklingNorBuf = Buffers.newDirectFloatBuffer(ducklingNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, ducklingNorBuf.limit()*4, ducklingNorBuf, GL_STATIC_DRAW);
		// duckling texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer ducklingTexBuf = Buffers.newDirectFloatBuffer(ducklingTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, ducklingTexBuf.limit()*4, ducklingTexBuf, GL_STATIC_DRAW);

		// sign vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer signVertBuf = Buffers.newDirectFloatBuffer(signPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, signVertBuf.limit()*4, signVertBuf, GL_STATIC_DRAW);
		// sign normal coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		FloatBuffer signNorBuf = Buffers.newDirectFloatBuffer(signNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, signNorBuf.limit()*4, signNorBuf, GL_STATIC_DRAW);
		// sign texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		FloatBuffer signTexBuf = Buffers.newDirectFloatBuffer(signTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, signTexBuf.limit()*4, signTexBuf, GL_STATIC_DRAW);

		// well vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		FloatBuffer wellVertBuf = Buffers.newDirectFloatBuffer(wellPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, wellVertBuf.limit()*4, wellVertBuf, GL_STATIC_DRAW);
		// well normal coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		FloatBuffer wellNorBuf = Buffers.newDirectFloatBuffer(wellNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, wellNorBuf.limit()*4, wellNorBuf, GL_STATIC_DRAW);
		// well texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
		FloatBuffer wellTexBuf = Buffers.newDirectFloatBuffer(wellTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, wellTexBuf.limit()*4, wellTexBuf, GL_STATIC_DRAW);
		
		// sphere vertices (for bump/normal mapping)
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[16]);
		FloatBuffer normalVertBuf = Buffers.newDirectFloatBuffer(normalPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, normalVertBuf.limit()*4, normalVertBuf, GL_STATIC_DRAW);
		// sphere texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[17]);
		FloatBuffer normalTexBuf = Buffers.newDirectFloatBuffer(normalTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, normalTexBuf.limit()*4, normalTexBuf, GL_STATIC_DRAW);
		// sphere normal
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[18]);
		FloatBuffer normalNorBuf = Buffers.newDirectFloatBuffer(normalNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, normalNorBuf.limit()*4, normalNorBuf, GL_STATIC_DRAW);
		// sphere tangent
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[19]);
		FloatBuffer normalTanBuf = Buffers.newDirectFloatBuffer(normalTanvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, normalTanBuf.limit()*4, normalTanBuf, GL_STATIC_DRAW);

		// torus vertices (env mapping)
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[20]);
		FloatBuffer torBuf = Buffers.newDirectFloatBuffer(torPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, torBuf.limit()*4, torBuf, GL_STATIC_DRAW);
		// torus texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[21]);
		FloatBuffer torTexBuf = Buffers.newDirectFloatBuffer(torTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, torTexBuf.limit()*4, torTexBuf, GL_STATIC_DRAW);
		// torus normal
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[22]);
		FloatBuffer torNorBuf = Buffers.newDirectFloatBuffer(torNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, torNorBuf.limit()*4, torNorBuf, GL_STATIC_DRAW);
		// torus indices
		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[23]);
		IntBuffer idxBuf = Buffers.newDirectIntBuffer(torIndices);
		gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf.limit()*4, idxBuf, GL_STATIC_DRAW);

		// snowball vertices (env mapping)
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[24]);
		FloatBuffer snowBuf = Buffers.newDirectFloatBuffer(snowPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, snowBuf.limit()*4, snowBuf, GL_STATIC_DRAW);
		// snowball texture
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[25]);
		FloatBuffer snowTexBuf = Buffers.newDirectFloatBuffer(snowTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, snowTexBuf.limit()*4, snowTexBuf, GL_STATIC_DRAW);
		// snowball normal
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[26]);
		FloatBuffer snowNorBuf = Buffers.newDirectFloatBuffer(snowNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, snowNorBuf.limit()*4, snowNorBuf, GL_STATIC_DRAW);
		// snowball indices
		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[27]);
		IntBuffer idxBuf2 = Buffers.newDirectIntBuffer(snowIndices);
		gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf2.limit()*4, idxBuf2, GL_STATIC_DRAW);
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

				lightCube.setX(x/40);
				lightCube.setZ(y/40);
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
								cam.pan(200.0f * deltaTime);
								break;
							case KeyEvent.VK_RIGHT:
								cam.pan(-200.0f * deltaTime);
								break;
							case KeyEvent.VK_UP:
								cam.pitch(200.0f * deltaTime);
								break;
							case  KeyEvent.VK_DOWN:
								cam.pitch(-200.0f * deltaTime);
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