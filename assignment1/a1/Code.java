package a1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;

import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.*;

public class Code extends JFrame implements GLEventListener, MouseWheelListener
{	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];

	private float startTime, endTime, elapsedTime;

	private float x = 0.0f;			//location of triangle
	private float sc = 1.0f;		//scale value > 0.1
	private float inc = 0.01f;		//offset for moving triangle
	private float tc = 0.0f;		//toggle color 0 or 1
	private int dir = 2;		//1-4 default is 2 pointing left
	private boolean motionToggle = false;		//motion
	private int m = 0;

	public Code()
	{	
		setTitle("CSC 155 - Assignment 1");
		setSize(800, 800);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);

		JPanel myPanel = new JPanel(new BorderLayout());
		this.add(myPanel, BorderLayout.SOUTH);

		JButton myButton1 = new JButton ();
		leftAction triMotion = new leftAction();
		myButton1.setAction(triMotion);
		myButton1.setText("Triangle Motion");
		myPanel.add(myButton1, BorderLayout.WEST);

		JButton myButton2 = new JButton ();
		rightAction tcolor = new rightAction();
		myButton2.setAction(tcolor);
		myButton2.setText("Toggle Color");
		myPanel.add(myButton2, BorderLayout.EAST);

		// get the content pane of the JFrame (this)
		JComponent contentPane = (JComponent) this.getContentPane();
		// get the "focus is in the window" input map for the content pane
		int mapName = JComponent.WHEN_IN_FOCUSED_WINDOW;
		InputMap imap = contentPane.getInputMap(mapName);
		
		// create a keystroke object to represent the 1-4 keys
		KeyStroke keyOne = KeyStroke.getKeyStroke('1');
		KeyStroke keyTwo = KeyStroke.getKeyStroke('2');
		KeyStroke keyThree = KeyStroke.getKeyStroke('3');
		KeyStroke keyFour = KeyStroke.getKeyStroke('4');
		
		// map key objects
		imap.put(keyOne, "up");
		imap.put(keyTwo, "left");
		imap.put(keyThree, "down");
		imap.put(keyFour, "right");

		// get the action map for the content pane
		ActionMap amap = contentPane.getActionMap();
		// put commands into content pane's action map
		upCommand setDirectionUp = new upCommand();
		amap.put("up", setDirectionUp);
		leftCommand setDirectionLeft = new leftCommand();
		amap.put("left", setDirectionLeft);
		downCommand setDirectionDown = new downCommand();
		amap.put("down", setDirectionDown);
		rightCommand setDirectionRight = new rightCommand();
		amap.put("right", setDirectionRight);
		//have the JFrame request keyboard focus
		this.requestFocus();

		this.addMouseWheelListener(this);

		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	public void mouseWheelMoved(MouseWheelEvent e){
		float notches = e.getWheelRotation();
		if (notches < 0 && sc > 0.1f) {
			sc -= 0.01f;
		}
		else if (notches > 0) {
			sc += 0.01f;
		}
	}

	public class leftAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			motionToggle = !motionToggle;
		}
	}

	public class rightAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			if (tc == 0.0f) {
				tc = 1.0f;
			} else {
				tc = 0.0f;	//toggle color	
			}
		}
	}

	public class upCommand extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			dir = 1;
		}
	}
	public class leftCommand extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			dir = 2;
		}
	}
	public class downCommand extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			dir = 3;
		}
	}
	public class rightCommand extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			dir = 4;
		}
	}

	public float getElapsedTime() {
		return ((endTime - startTime)/1000.0f);
	}


	public void display(GLAutoDrawable drawable)
	{	
		startTime = endTime;
		endTime = System.currentTimeMillis();

		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);		//clear background to black, each time
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(renderingProgram);

		if (motionToggle) {
			x += inc;			//move triangle along x axis
			m = 1;
			if (x > 1.0f) inc = -getElapsedTime();			//(1,0) right
			if (x < -1.0f) inc = getElapsedTime();			//(-1,0) left
			else inc = 0.01f;
		} 
		else if (!motionToggle){
			inc = 0.0f;
			m = 0;
		}

		int offsetLoc = gl.glGetUniformLocation(renderingProgram, "offset");		//retrieve pointer to "offset"
		gl.glProgramUniform1f(renderingProgram, offsetLoc, x);						//send value in "x" to "offset"

		int motion = gl.glGetUniformLocation(renderingProgram, "motion");
		gl.glProgramUniform1i(renderingProgram, motion, m);

		int scale = gl.glGetUniformLocation(renderingProgram, "scale");
		gl.glProgramUniform1f(renderingProgram, scale, sc);

		int rotate = gl.glGetUniformLocation(renderingProgram, "rotate");
		gl.glProgramUniform1i(renderingProgram, rotate, dir);

		int changeColor = gl.glGetUniformLocation(renderingProgram, "toggleColor");
		gl.glProgramUniform1f(renderingProgram, changeColor, tc);

		gl.glDrawArrays(GL_TRIANGLES,0,3);

	}

	public void init(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		System.out.println("OpenGL Version: " + gl.glGetString(GL_VERSION));
		System.out.println("JOGL Version: " + Package.getPackage("com.jogamp.opengl").getImplementationVersion());
		System.out.println("Java Version: " + System.getProperty("java.version"));

		renderingProgram = Utils.createShaderProgram("a1/vertShader.glsl", "a1/fragShader.glsl");
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
	}

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}
}