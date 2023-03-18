package a2;

import org.joml.*;
import org.joml.Math;

public class Camera
{
    private Matrix4f viewTransform, rotation;
    private Vector3f u, v, n, location, rotationVector;
    private Vector4f vectorU, vectorV, vectorN;
    private Vector3f cameraTarget = new Vector3f(0.0f, 0.0f, 0.0f);
    
    public Camera()
    {	
        viewTransform = new Matrix4f();
        rotation = new Matrix4f();
        location = new Vector3f(0.0f,0.0f,10.0f);

        u = new Vector3f(1.0f, 0.0f, 0.0f);
        v = new Vector3f(0.0f, 1.0f, 0.0f);
        n = new Vector3f(0.0f, 0.0f, -1.0f);

        vectorU = new Vector4f(this.getU(), 1f);
        vectorV = new Vector4f(this.getV(), 1f);
        vectorN = new Vector4f(this.getN(), 1f);
    }

    public void setLocation(Vector3f loc) { this.location = loc; }
    public void setLocation(float x, float y, float z) 
    { 
        this.location.x = x;
        this.location.y = y;
        this.location.z = z;
    }
    public Vector3f getLocation() { return new Vector3f(this.location); }

    public void setRotation(Matrix4f rot) { this.rotation = rot; }
    public Matrix4f getRotation() { return new Matrix4f(this.rotation); }

    public void setU(Vector3f newU) { this.u.set(newU); }
    public void setV(Vector3f newV) { this.v.set(newV); }
    public void setN(Vector3f newN) { this.n.set(newN); }

    public Vector3f getU() { return new Vector3f(this.u); }
    public Vector3f getV() { return new Vector3f(this.v); }
    public Vector3f getN() { return new Vector3f(this.n); }

    public void setX(float x) { this.location.x = x; }
    public void setY(float y) { this.location.y = y; }
    public void setZ(float z) { this.location.z = z; }

    public float getX() { return this.location.x; }
    public float getY() { return this.location.y; }
    public float getZ() { return this.location.z; }
    
    public void setViewTransform() 
    {
        n.x = (float) Math.cos(Math.toRadians(rotationVector.x)) * (float) Math.sin(Math.toRadians(rotationVector.y));
        n.y = (float) Math.sin(Math.toRadians(rotationVector.x));
        n.z = (float) Math.cos(Math.toRadians(rotationVector.x)) * (float) Math.cos(Math.toRadians(rotationVector.y));

        u.x = (float) Math.sin(Math.toRadians(rotationVector.y - 90));
        u.y = 0;
        u.z = (float) Math.cos(Math.toRadians(rotationVector.y - 90));

        rotation.set(
            u.x, u.y, u.z, 0,
            v.x, v.y, v.z, 0,
            n.x, n.y, n.z, 0,
            0, 0, 0, 1
        );
        Matrix4f translation = new Matrix4f();
        translation.set(
            1, 0, 0, -location.x,
            0, 1, 0, -location.y,
            0, 0, 1, -location.z,
            0, 0, 0, 1
        );
        viewTransform = new Matrix4f(rotation.mul(translation));
    }
    public Matrix4f getViewTransform() { return viewTransform; }


    /** camera moves forward */
    public void moveForward(float factor)
    {
        vectorN.mul(factor); 
        Vector3f newPosition = this.getLocation().add(0, 0, vectorN.z()); 
        this.setLocation(newPosition); 
        //setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
    }
    
    /** camera moves backward */
    public void moveBackward(float factor)
    {
        vectorN.mul(factor); 
        Vector3f newPosition = this.getLocation().add(0, 0, -vectorN.z()); 
        this.setLocation(newPosition); 
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
    }

    /** camera strafes left */
    public void strafeLeft(float factor)
    {
        vectorU.mul(factor); 
        Vector3f newPosition = this.getLocation().add(-vectorU.x(), 0, 0); 
        this.setLocation(newPosition); 
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
    }

    /** camera strafes right */
    public void strafeRight(float factor)
    {
        vectorU.mul(factor); 
        Vector3f newPosition = this.getLocation().add(vectorU.x(), 0, 0); 
        this.setLocation(newPosition);  
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
    }

    /** camera moves upwards */
    public void moveUpward(float factor)
    {
        vectorV.mul(factor); 
        Vector3f newPosition = this.getLocation().add(0, vectorV.y(), 0); 
        this.setLocation(newPosition); 
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
    }

    /** camera moves downwards */
    public void moveDownward(float factor)
    {
        vectorV.mul(factor); 
        Vector3f newPosition = this.getLocation().add(0, -vectorV.y(), 0); 
        this.setLocation(newPosition); 
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
    }

    /** turns camera to the left */
    public void panLeft(float factor)
    {
        vectorU.mul(factor); 
        Vector3f newPosition = this.getLocation().add(-vectorU.x(), -vectorU.y(), -vectorU.z()); 
        this.setLocation(newPosition); 
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
    }

    /** turns camera to the right */
    public void panRight(float factor)
    {
        vectorU.mul(factor); 
        Vector3f newPosition = this.getLocation().add(vectorU.x(), vectorU.y(), vectorU.z()); 
        this.setLocation(newPosition); 
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
    }

    /** camera pitches upwards */
	public void pitchUp(float degree)
	{
        float radians = (float)Math.toRadians(-degree);
        Matrix4f newRotation = new Matrix4f().rotate(radians, u);
        this.setRotation(rotation.mul(newRotation));
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
	}

    /** camera pitches downwards */
    public void pitchDown(float degree)
	{
        float radians = (float)Math.toRadians(degree);
        Matrix4f newRotation = new Matrix4f().rotate(radians, u);
        this.setRotation(rotation.mul(newRotation));
        // setViewTransform();
        System.out.println(location.x + ", " + location.y + ", " + location.z);
	}
}