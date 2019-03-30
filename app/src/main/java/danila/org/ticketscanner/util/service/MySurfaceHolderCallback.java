package danila.org.ticketscanner.util.service;

import android.view.SurfaceHolder;

import com.google.android.gms.vision.CameraSource;

public class MySurfaceHolderCallback implements SurfaceHolder.Callback {

    private CameraSource cameraSource;

    public MySurfaceHolderCallback(CameraSource cameraSource) {
        this.cameraSource = cameraSource;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
