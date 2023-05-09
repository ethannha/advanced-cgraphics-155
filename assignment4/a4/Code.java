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
	private int renderingProgram1, renderingProgram2, axesProgram, cubemapProgram, floorProgram, lightDotProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[9];
	private double startTime = 0.0;
	private double elapsedTime;
	private double tf;
	private float deltaTime = 0.0f;
	private long lastFrame;

	private boolean axesOn, lightOn;
	private float xOffset, yOffset;
	private Camera cam;
	private Vector3f cameraPosition, lightPosition, duckPosition, ducklingPosition;
	private float planeHeight = 0.0f;

	// shadow stuff
	private int scSizeX, scSizeY;
	private int [] shadowTex = new int[1];
	private int [] shadowBuffer = new int[1];
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP1 = new Matrix4f();
	private Matrix4f shadowMVP2 = new Matrix4f();
	private Matrix4f b = new Matrix4f();

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);  // buffer for transfering matrix to uniform
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4fStack mvStack = new Matrix4fStack(6);
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int axesvLoc, axespLoc, mLoc, vLoc, pLoc, nLoc, sLoc;
	private float aspect;
	private Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

	// lighting variables
	private Light lightCube;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private int intensityLoc;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];
	private Vector4f lightDotColor;

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
	private int duckObjVertices, ducklingObjVertices;
	private ImportedModel duckModel, ducklingModel;

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
		
		cubemapProgram = Utils.createShaderProgram("a4/vertCShader.glsl", "a4/fragCShader.glsl");
		floorProgram = Utils.createShaderProgram("a4/vertFloorShader.glsl", "a4/fragFloorShader.glsl");
		lightDotProgram = Utils.createShaderProgram("a4/lightDotVert.glsl", "a4/lightDotFrag.glsl");
		axesProgram = Utils.createShaderProgram("a4/vertAxesShader.glsl", "a4/fragAxesShader.glsl");
		renderingProgram1 = Utils.createShaderProgram("a4/vertShader1.glsl", "a4/fragShader1.glsl");
		renderingProgram2 = Utils.createShaderProgram("a4/vertShader2.glsl", "a4/fragShader2.glsl");

		duckModel = new ImportedModel("assets/models/duck.obj");
		ducklingModel = new ImportedModel("assets/models/duckling.obj");

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
		
		setupVertices();
		setupShadowBuffers();
				
		b.set(
			0.5f, 0.0f, 0.0f, 0.0f,
			0.0f, 0.5f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.5f, 0.0f,
			0.5f, 0.5f, 0.5f, 1.0f);
		
		lightDotColor = new Vector4f(1.0f, 1.0f, 0.0f, 1.0f);

		cameraPosition = new Vector3f(0.0f, 2.0f, 12.0f);
		Vector3f targetPosition = new Vector3f(cameraPosition.x(), cameraPosition.y(), 0.0f);
		cam = new Camera(cameraPosition, targetPosition);

		lightPosition = new Vector3f(0.0f, 3.0f, 0.0f);
		lightCube = new Light(lightPosition);

		duckPosition = new Vector3f(0.0f, 0.0f, 2.0f);
		ducklingPosition = new Vector3f(0.0f, 0.0f, 3.0f);

		axesOn = true;
		lightOn = true;

		duckTexture = Utils.loadTextureAWT("assets/textures/duck_uv.png");
		ducklingTexture = Utils.loadTextureAWT("assets/textures/duckling_uv.png");
		skyboxTexture = Utils.loadCubeMap("assets/cubeMap");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

	}

	private void setupShadowBuffers()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();
	
		gl.glGenFramebuffers(1, shadowBuffer, 0);
	
		gl.glGenTextures(1, shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
						scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		
		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
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
		float[] PLANE_POSITIONS = {
			-120.0f, 0.0f, -120.0f,  -120.0f, 0.0f, 120.0f,  120.0f, 0.0f, -120.0f,  
			120.0f, 0.0f, -120.0f,  -120.0f, 0.0f, 120.0f,  120.0f, 0.0f, 120.0f
		};
		float[] PLANE_TEXCOORDS = {
			0.0f, 0.0f,  0.0f, 1.0f,  1.0f, 0.0f,
			1.0f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f
		};
		
		// duck object
		duckObjVertices = duckModel.getNumVertices();
		Vector3f[] vertices = duckModel.getVertices();
		Vector2f[] texCoords = duckModel.getTexCoords();
		Vector3f[] normals = duckModel.getNormals();
		
		float[] pvalues = new float[duckObjVertices*3];
		float[] tvalues = new float[duckObjVertices*2];
		float[] nvalues = new float[duckObjVertices*3];
		
		for (int i=0; i<duckObjVertices; i++)
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
		ducklingObjVertices = ducklingModel.getNumVertices();
		Vector3f[] vertices2 = ducklingModel.getVertices();
		Vector2f[] texCoords2 = ducklingModel.getTexCoords();
		Vector3f[] normals2 = ducklingModel.getNormals();
		
		float[] pvalues2 = new float[ducklingObjVertices*3];
		float[] tvalues2 = new float[ducklingObjVertices*2];
		float[] nvalues2 = new float[ducklingObjVertices*3];
		
		for (int j=0; j<ducklingObjVertices; j++)
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

		// cubemap
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);

		// duck
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);

		// duckling
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer vertBuf2 = Buffers.newDirectFloatBuffer(pvalues2);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf2.limit()*4, vertBuf2, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer texBuf2 = Buffers.newDirectFloatBuffer(tvalues2);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf2.limit()*4, texBuf2, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer norBuf2 = Buffers.newDirectFloatBuffer(nvalues2);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf2.limit()*4, norBuf2, GL_STATIC_DRAW);

		// floor plane
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer planeBuf = Buffers.newDirectFloatBuffer(PLANE_POSITIONS);
		gl.glBufferData(GL_ARRAY_BUFFER, planeBuf.limit()*4, planeBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer texBuf3 = Buffers.newDirectFloatBuffer(PLANE_TEXCOORDS);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf3.limit()*4, texBuf3, GL_STATIC_DRAW);

	}

	private void installLights(int renderingProgram2)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();

		if (lightOn == true) {
			float lightIntensity = 1.0f;
			intensityLoc = gl.glGetUniformLocation(renderingProgram2, "intensity");
			gl.glProgramUniform1f(renderingProgram2, intensityLoc, lightIntensity);
		} else {
			float lightIntensity = 0.0f;
			intensityLoc = gl.glGetUniformLocation(renderingProgram2, "intensity");
			gl.glProgramUniform1f(renderingProgram2, intensityLoc, lightIntensity);
		}
		
		lightPos[0]=currentLightPos.x();
		lightPos[1]=currentLightPos.y();
		lightPos[2]=currentLightPos.z();
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram2, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram2, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram2, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram2, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram2, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram2, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram2, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram2, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram2, "material.shininess");
	
		// set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram2, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram2, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram2, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram2, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram2, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram2, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram2, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram2, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram2, mshiLoc, matShi);
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

		// Set up the light dot shader program
		gl.glUseProgram(lightDotProgram);

		// Set the light position uniform
		int lightPositionLocation = gl.glGetUniformLocation(lightDotProgram, "lightPosition");
		gl.glUniform3f(lightPositionLocation, lightPosition.x, lightPosition.y, lightPosition.z);

		// Set the light dot color uniform
		int lightDotColorLocation = gl.glGetUniformLocation(lightDotProgram, "lightDotColor");
		gl.glUniform4f(lightDotColorLocation, 1.0f, 1.0f, 0.0f, 1.0f);	// yellow color

		currentLightPos.set(lightPosition);

		lightVmat.identity().setLookAt(currentLightPos, origin, up);	// vector from light to origin
		lightPmat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

	
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

		
		// ======= RENDER FLOOR PROGRAM =======
		gl.glUseProgram(floorProgram);
		mLoc = gl.glGetUniformLocation(floorProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(floorProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(floorProgram, "p_matrix");

		mMat.translation(0, planeHeight, 0);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);


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


		// shadows
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);

		gl.glDrawBuffer(GL_NONE);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);	//  for reducing
		gl.glPolygonOffset(3.0f, 5.0f);		//  shadow artifacts

		passOne();
		
		gl.glDisable(GL_POLYGON_OFFSET_FILL);	// artifact reduction, continued
		
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
	
		gl.glDrawBuffer(GL_FRONT);
		
		passTwo();
		

	}

	public void passOne() 
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(renderingProgram1);

		// ======================================================================= duck obj
		mvStack.pushMatrix();
		mvStack.translation(duckPosition);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);
		
		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);

		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, duckModel.getNumVertices());
		mvStack.popMatrix();
		
		// ======================================================================= duckling obj
		mvStack.pushMatrix();
		mvStack.translate(ducklingPosition);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);

		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);


		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, ducklingModel.getNumVertices());
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();

	}

	public void passTwo() 
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glUseProgram(renderingProgram2);
		
		// ======= RENDER OBJECTS PROGRAM =======
		mLoc = gl.glGetUniformLocation(renderingProgram2, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram2, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram2, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram2, "norm_matrix");
		sLoc = gl.glGetUniformLocation(renderingProgram2, "shadowMVP");

		// ======== CAMERA/VIEW MATRIX SET UP ========
		vMat.identity();
		mvStack.pushMatrix();
		vMat = cam.getViewMatrix();

		// LIGHTING
		// setup lights based on current light position
		currentLightPos.set(lightCube.getLocation());
		installLights(renderingProgram2);
		
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
		mvStack.translation(duckPosition);
		mvStack.pushMatrix();

		mMat.identity();
		mMat.set(mvStack);
		mMat.invert(invTrMat);

		invTrMat.identity();
		invTrMat.transpose(invTrMat);
		
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

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

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[4]);
		gl.glDrawElements(GL_TRIANGLES, duckObjVertices, GL_UNSIGNED_INT, 0);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, duckModel.getNumVertices());
		mvStack.popMatrix();
		

		// ======================================================================= duckling obj
		mvStack.pushMatrix();
		mvStack.translate(ducklingPosition);
		mvStack.pushMatrix();

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

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