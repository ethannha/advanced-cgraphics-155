package a3;

import org.joml.*;

public class Light {
    private Vector3f location;
    private Vector3f U, V, N;

    private Vector3f defaultLocation;
    private Vector3f defaultU, defaultV, defaultN;
    
    public Light(Vector3f loc) {
        defaultLocation = new Vector3f(loc);
        defaultU = new Vector3f(1.0f, 0.0f, 0.0f);
        defaultV = new Vector3f(0.0f, 1.0f, 0.0f);
        defaultN = new Vector3f(0.0f, 0.0f, -1.0f);

        location = new Vector3f(defaultLocation);
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
    public void setX(float x) { location.x = x; }
    public void setY(float y) { location.y = y; }
    public void setZ(float z) { location.z = z; }

    /** camera moves forward */
    public void moveForward(float distance)
    {
        Vector3f result = new Vector3f(N);
        result.mul(distance);
        location.add(result);
    }
    
    /** camera moves backward */
    public void moveBackward(float distance)
    {
        Vector3f result = new Vector3f(N);
        result.mul(distance);
        location.sub(result);
    }

    /** camera strafes left */
    public void moveLeft(float distance)
    {
        Vector3f result = new Vector3f(U);
        result.mul(distance);
        location.sub(result);
    }

    /** camera strafes right */
    public void moveRight(float distance)
    {
        Vector3f result = new Vector3f(U);
        result.mul(distance);
        location.add(result);
    }

    /** camera moves upwards */
    public void moveUpward(float distance)
    {
        Vector3f result = new Vector3f(defaultV);
        result.mul(distance);
        location.add(result);
    }

    /** camera moves downwards */
    public void moveDownward(float distance)
    {
        Vector3f result = new Vector3f(defaultV);
        result.mul(distance);
        location.sub(result);
    }
}