package a2;

import org.joml.*;
import org.joml.Math;

public class Camera
{
    private Vector3f location;
    private Vector3f targetPos;
    private Vector3f U, V, N;

    private Vector3f defaultLocation, defaultTarget;
    private Vector3f defaultU, defaultV, defaultN;
    
    public Camera(Vector3f loc) {
        defaultLocation = new Vector3f(loc);
        defaultTarget = new Vector3f(0.0f, 0.0f, 0.0f);
        defaultU = new Vector3f(1.0f, 0.0f, 0.0f);
        defaultV = new Vector3f(0.0f, 1.0f, 0.0f);
        defaultN = new Vector3f(0.0f, 0.0f, -1.0f);

        location = new Vector3f(defaultLocation);
        targetPos = new Vector3f(defaultTarget);
        U = new Vector3f(defaultU);
        V = new Vector3f(defaultV);
        N = new Vector3f(defaultN);
    }

    public Vector3f getLocation() { return location; }
    public Vector3f getU() { return U; }
    public Vector3f getV() { return V; }
    public Vector3f getN() { return N; }
    public float getX() { return location.x; }
    public float getY() { return location.y; }
    public float getZ() { return location.z; }

    
    public void setLocation(Vector3f loc) { location = loc; }

    /** camera moves forward */
    public void moveForward(float distance)
    {
        Vector3f result = new Vector3f(N);
        result.mul(distance);
        location.add(result);
        targetPos.add(result);
    }
    
    /** camera moves backward */
    public void moveBackward(float distance)
    {
        Vector3f result = new Vector3f(N);
        result.mul(distance);
        location.sub(result);
        targetPos.sub(result);
    }

    /** camera strafes left */
    public void strafeLeft(float distance)
    {
        Vector3f result = new Vector3f(U);
        result.mul(distance);
        location.sub(result);
        targetPos.sub(result);
    }

    /** camera strafes right */
    public void strafeRight(float distance)
    {
        Vector3f result = new Vector3f(U);
        result.mul(distance);
        location.add(result); 
        targetPos.add(result);
    }

    /** camera moves upwards */
    public void moveUpward(float distance)
    {
        Vector3f result = new Vector3f(V);
        result.mul(distance);
        location.add(result);
    }

    /** camera moves downwards */
    public void moveDownward(float distance)
    {
        Vector3f result = new Vector3f(V);
        result.mul(distance);
        location.sub(result);
    }

    /** turns camera left or right */
    public void pan(float angle)
    {
        float radians = (float) Math.toRadians(angle);
        Matrix3f rotation = new Matrix3f().rotate(radians, V);
        N = rotation.transform(N);
        U = rotation.transform(U);
        targetPos.set(location).add(N);
    }

    /** camera pitches up or down */
	public void pitch(float angle)
	{
        float radians = (float) Math.toRadians(angle);
        Matrix3f rotation = new Matrix3f().rotate(radians, U);
        N = rotation.transform(N);
        V = rotation.transform(V);
        targetPos.set(location).add(N);
	}

    // getViewMatrix method
    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(location, targetPos, V);
    }

}