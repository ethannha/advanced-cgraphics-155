package a2;

import org.joml.*;
import org.joml.Math;

public class Camera
{
    private Vector3f u, v, n;
    private Vector3f defaultU, defaultV, defaultN;
    private Vector3f location, defaultLocation;
    private Vector3f rotation, defaultRotation;

    private Vector3f oldPosition, newPosition;
    private Vector4f fwdDirection, upDirection, rightDirection; 
    private Vector3f fwdVector, upVector, rightVector;
    
    public Camera()
    {	
        defaultLocation = new Vector3f(0.0f, 0.0f, 10.0f);
        defaultRotation = new Vector3f(0.0f, 0.0f, 0.0f);
        defaultU = new Vector3f(1.0f, 0.0f, 0.0f);
        defaultV = new Vector3f(0.0f, 1.0f, 0.0f);
        defaultN = new Vector3f(0.0f, 0.0f, -1.0f);

        location = new Vector3f(defaultLocation);
        rotation = new Vector3f(defaultRotation);
        u = new Vector3f(defaultU);
        v = new Vector3f(defaultV);
        n = new Vector3f(defaultN);
    }

    
    public void setLocation(Vector3f loc) { location.set(loc); }
    public Vector3f getLocation() { return new Vector3f(location); }
    
    public void setRotation(Vector3f rot ) { rotation.set(rot); }
    public Vector3f getRotation() { return new Vector3f(rotation); }

    public void setU(Vector3f newU) { u.set(newU); }
    public void setV(Vector3f newV) { v.set(newV); }
    public void setN(Vector3f newN) { n.set(newN); }

    public Vector3f getU() { return new Vector3f(u); }
    public Vector3f getV() { return new Vector3f(v); }
    public Vector3f getN() { return new Vector3f(n); }

    public float getX() { return location.x(); }
    public float getY() { return location.y(); }
    public float getZ() { return location.z(); }

    public float getRX() { return rotation.x(); }
    public float getRY() { return rotation.y(); }
    public float getRZ() { return rotation.z(); }
    

    /** camera moves forward */
    public void moveForward(float factor)
    {
        oldPosition = this.getLocation(); 
        Vector4f nVec = new Vector4f(this.getN(), 1f);
        fwdDirection = nVec; 
        fwdDirection.mul(factor); 
        newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z()); 
        this.setLocation(newPosition); 
    }
    
    /** camera moves backward */
    public void moveBackward(float factor)
    {
        oldPosition = this.getLocation(); 
        Vector4f nVec = new Vector4f(this.getN(), 1f);
        fwdDirection = nVec; 
        fwdDirection.mul(-factor); 
        newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z()); 
        this.setLocation(newPosition); 
    }

    /** camera strafes left */
    public void strafeLeft(float factor)
    {
        oldPosition = this.getLocation(); 
        Vector4f uVec = new Vector4f(this.getU(), 1f);
        rightDirection = uVec; 
        rightDirection.mul(-factor); 
        newPosition = oldPosition.add(rightDirection.x(), 0, 0); 
        this.setLocation(newPosition); 
    }

    /** camera strafes right */
    public void strafeRight(float factor)
    {
        oldPosition = this.getLocation(); 
        Vector4f uVec = new Vector4f(this.getU(), 1f);
        rightDirection = uVec; 
        rightDirection.mul(factor); 
        newPosition = oldPosition.add(rightDirection.x(), 0, 0); 
        this.setLocation(newPosition);  
    }

    /** camera moves upwards */
    public void moveUpward(float factor)
    {
        oldPosition = this.getLocation(); 
        Vector4f vVec = new Vector4f(this.getV(), 1f);
        upDirection = vVec; 
        upDirection.mul(factor); 
        newPosition = oldPosition.add(upDirection.x(), upDirection.y(), upDirection.z()); 
        this.setLocation(newPosition); 
    }

    /** camera moves downwards */
    public void moveDownward(float factor)
    {
        oldPosition = this.getLocation(); 
        Vector4f vVec = new Vector4f(this.getV(), 1f);
        upDirection = vVec; 
        upDirection.mul(-factor); 
        newPosition = oldPosition.add(upDirection.x(), upDirection.y(), upDirection.z()); 
        this.setLocation(newPosition); 
    }

    /** turns camera to the left */
    public void panLeft(float factor)
    {
        oldPosition = this.getLocation(); 
        Vector4f uVec = new Vector4f(this.getU(), 1f);
        rightDirection = uVec; 
        rightDirection.mul(-factor); 
        newPosition = oldPosition.add(rightDirection.x(), rightDirection.y(), rightDirection.z()); 
        this.setLocation(newPosition); 
    }

    /** turns camera to the right */
    public void panRight(float factor)
    {
        oldPosition = this.getLocation(); 
        Vector4f uVec = new Vector4f(this.getU(), 1f);
        rightDirection = uVec; 
        rightDirection.mul(factor); 
        newPosition = oldPosition.add(rightDirection.x(), rightDirection.y(), rightDirection.z()); 
        this.setLocation(newPosition); 
    }

    /** camera pitches upwards */
	public void pitchUp(float factor)
	{
        float radians = (float) Math.toRadians(-factor);
        Matrix3f rotateAmt = new Matrix3f().rotate(radians, u);
        this.setRotation(this.getRotation().mul(rotateAmt));
	}

    /** camera pitches downwards */
    public void pitchDown(float factor)
	{
        float radians = (float) Math.toRadians(factor);
        Matrix3f rotateAmt = new Matrix3f().rotate(radians, u);
        this.setRotation(this.getRotation().mul(rotateAmt));
	}
}