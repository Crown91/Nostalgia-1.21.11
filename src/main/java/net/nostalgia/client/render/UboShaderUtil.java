package net.nostalgia.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class UboShaderUtil {

    /**
     * 1.21.11 removed Camera.getViewRotationMatrix / getViewRotationProjectionMatrix.
     * The camera now only exposes its orientation quaternion, so the view matrix is
     * rebuilt here: the view transform is the inverse of the camera orientation, and
     * for a pure rotation the inverse is the conjugate.
     *
     * camera.rotation() hands out the camera's own live quaternion field, so the
     * conjugate is written into a fresh Quaternionf instead of in place - mutating it
     * would silently rotate the whole game view.
     */
    public static Matrix4f getViewRotationMatrix(Camera camera) {
        return new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
    }

    public static Matrix4f getInverseViewProjMatrix(Camera camera, Matrix4f localCapturedProj) {
        if (PortalSkyRenderer.capturedProjectionMatrix != null && PortalSkyRenderer.capturedModelViewMatrix != null) {
            return new Matrix4f(PortalSkyRenderer.capturedProjectionMatrix).mul(PortalSkyRenderer.capturedModelViewMatrix).invert();
        } else if (localCapturedProj != null) {
            return new Matrix4f(localCapturedProj).mul(getViewRotationMatrix(camera)).invert();
        } else {
            // No projection matrix has been captured yet (only possible before the first
            // rendered frame of an effect). Fall back to the rotation part alone rather
            // than inventing a projection with a guessed field of view.
            return getViewRotationMatrix(camera).invert();
        }
    }

    public static float getShaderTimeSeconds(DeltaTracker tracker) {
        return tracker.getGameTimeDeltaTicks() / 20.0f;
    }
}
