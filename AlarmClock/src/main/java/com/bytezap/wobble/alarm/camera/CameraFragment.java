/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm.camera;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.bytezap.wobble.R;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.ToastGaffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class CameraFragment extends Fragment implements View.OnClickListener {

    private static final String CURRENT_CAMERA = "Camera_Current";

    private static final String IS_FLASH_ON = "Camera_Flash";

    // Native camera.
    private Camera mCamera;

    // View to display the camera output.
    private CameraPreview mPreview;

    /**
     * Whether the flash is switched on or not.
     */
    private boolean mIsFlashOn = false;

    /*
    Camera ID currently chosen
     */
    private int mCurrentCamera;

    private ImageButton flashButton;

    /**
     * Picture Callback for handling a picture capture and saving it out to a file.
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                ToastGaffer.showToast(getActivity(), "Image retrieval failed");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                ToastGaffer.showToast(getActivity(), getString(R.string.picture_selected));
                Intent resultIntent = new Intent();
                resultIntent.putExtra(CameraActivity.IMAGE_PATH, pictureFile.getAbsolutePath());
                getActivity().setResult(Activity.RESULT_OK, resultIntent);

                getActivity().finish();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Default empty constructor.
     */
    public CameraFragment() {
        super();
    }

    /**
     * Static factory method
     *
     * @return
     */
    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    /**
     * Safe method for getting a camera instance.
     *
     * @return
     */
    public static Camera getCameraInstance(int mCurrentCamera) {
        Camera c = null;
        try {
            c = Camera.open(mCurrentCamera); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * OnCreateView fragment override
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_fragment, container, false);

        ImageButton captureButton = view.findViewById(R.id.capture_cam);
        ImageButton flipButton = view.findViewById(R.id.flip_cam);
        flashButton = view.findViewById(R.id.flash_cam);

        int mNumberOfCameras = Camera.getNumberOfCameras();

        if (savedInstanceState!=null) {
            mCurrentCamera = savedInstanceState.getInt(CURRENT_CAMERA);
            mIsFlashOn = savedInstanceState.getBoolean(IS_FLASH_ON);
        } else {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < mNumberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCurrentCamera = i;
                }
            }
        }

        safeCameraOpenInView(view);

        boolean flashAvailable = isFlashAvailable();
        flashButton.setVisibility(flashAvailable ? View.VISIBLE : View.INVISIBLE);
        flipButton.setVisibility(mNumberOfCameras > 1 ? View.VISIBLE : View.INVISIBLE);

        if (flashAvailable) {
            flashButton.setOnClickListener(this);
        }
        captureButton.setOnClickListener(this);
        if (mNumberOfCameras > 1) {
            flipButton.setOnClickListener(this);
        }

        return view;
    }

    /**
     * Recommended safe way to open the camera.
     *
     * @param view
     * @return
     */
    private boolean safeCameraOpenInView(View view) {
        boolean qOpened;
        releaseCameraAndPreview();
        mCamera = getCameraInstance(mCurrentCamera);
        qOpened = (mCamera != null);

        if (qOpened) {
            mPreview = new CameraPreview(getActivity().getApplicationContext(), mCamera, view);
            List<String> mSupportedFlashModes = mCamera.getParameters().getSupportedFlashModes();

            Camera.Parameters parameters = mCamera.getParameters();

            if (mSupportedFlashModes != null) {
                parameters.setFlashMode(mIsFlashOn ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                flashButton.setImageResource(mIsFlashOn ? R.drawable.ic_flash : R.drawable.ic_flash_off);
            }
            Camera.Size pictureSize = getAppropriatePictureSize(parameters);
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            parameters.setPictureFormat(ImageFormat.JPEG);
            mCamera.setParameters(parameters);
            FrameLayout preview = view.findViewById(R.id.cam_preview);
            preview.addView(mPreview);
            setCameraDisplayOrientation(getActivity(), mCurrentCamera, mCamera);
        }
        return qOpened;
    }

    private Camera.Size getAppropriatePictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                long resultArea = result.width * result.height;
                long newArea = size.width * size.height;

                if (newArea < resultArea && size.width >=800 && size.height >= 600) {
                    result = size;
                }
            }
        }

        return(result);
    }

    public boolean isFlashAvailable(){
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return false;
        }

        if (mCurrentCamera == Camera.CameraInfo.CAMERA_FACING_FRONT && !CommonUtils.is17OrLater()) {
            return false;
        }

        Camera.Parameters params = mCamera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        if(flashModes == null || flashModes.isEmpty() || flashModes.size() == 1 && flashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
            return false;
        }

        for(String flashMode : flashModes) {
            if(Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_CAMERA, mCurrentCamera);
        outState.putBoolean(IS_FLASH_ON, mIsFlashOn);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera!=null) {
            mCamera.stopPreview();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCamera!=null) {
            mPreview.startCameraPreview();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseCameraAndPreview();
    }

    /**
     * Clear any existing preview/camera.
     */
    private void releaseCameraAndPreview() {

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        if (mPreview != null) {
            mPreview.destroyDrawingCache();
            mPreview.mCamera = null;
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.capture_cam:
                // get an image from the camera
                mCamera.takePicture(null, null, mPicture);
                break;

            case R.id.flash_cam:
                if (mCamera == null) {
                    return;
                }
                mIsFlashOn = !mIsFlashOn;
                try {
                    Camera.Parameters p = mCamera.getParameters();
                    p.setFlashMode(mIsFlashOn ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(p);
                    mCamera.startPreview();
                    flashButton.setImageResource(mIsFlashOn ? R.drawable.ic_flash : R.drawable.ic_flash_off);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.flip_cam:
                // Acquire the next camera and request Preview to reconfigure
                // parameters.
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }
                mCurrentCamera = mCurrentCamera == Camera.CameraInfo.CAMERA_FACING_BACK ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
                mCamera = getCameraInstance(mCurrentCamera);

                if (mCamera != null) {
                    List<String> mSupportedFlashModes = mCamera.getParameters().getSupportedFlashModes();

                    Camera.Parameters parameters = mCamera.getParameters();
                    if (mSupportedFlashModes != null) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(parameters);
                    }
                    Camera.Size pictureSize = getAppropriatePictureSize(parameters);
                    parameters.setPictureSize(pictureSize.width, pictureSize.height);
                    parameters.setPictureFormat(ImageFormat.JPEG);
                    mCamera.setParameters(parameters);
                    flashButton.setVisibility(isFlashAvailable() ? View.VISIBLE : View.INVISIBLE);
                    mPreview.mCamera = mCamera;
                    setCameraDisplayOrientation(getActivity(), mCurrentCamera, mCamera);
                    mPreview.startCameraPreview();
                }
                break;
        }
    }

    /**
     * Used to return the camera File output.
     *
     * @return
     */
    private File getOutputMediaFile() {

        // Create a media file name
        String timeStamp = DateFormat.getDateTimeInstance().format(new Date());
        return new File(getActivity().getExternalFilesDir(null), "IMG_" + timeStamp + ".jpg");
    }

    /**
     * Surface on which the camera projects it's capture results.
     */
    class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        // SurfaceHolder
        private SurfaceHolder mHolder;

        // Our Camera.
        private Camera mCamera;

        // Parent Context.
        private Context mContext;

        // Camera Sizing (For rotation, orientation changes)
        private Camera.Size mPreviewSize;

        // List of supported preview sizes
        private List<Camera.Size> mSupportedPreviewSizes;

        // View holding this camera.
        private View mCameraView;

        public CameraPreview(Context context, Camera camera, View cameraView) {
            super(context);

            // Capture the context
            mCameraView = cameraView;
            mContext = context;
            setCamera(camera);

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setKeepScreenOn(true);
        }

        /**
         * Begin the preview of the camera input.
         */
        public void startCameraPreview() {
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Extract supported preview and flash modes from the camera.
         *
         * @param camera
         */
        private void setCamera(Camera camera) {
            mCamera = camera;
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }

        /**
         * The Surface has been created, now tell the camera where to draw the preview.
         *
         * @param holder
         */
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Dispose of the camera preview.
         *
         * @param holder
         */
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                mCamera.stopPreview();
            }
        }

        /**
         * React to surface changed events
         *
         * @param holder
         * @param format
         * @param w
         * @param h
         */
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                Camera.Parameters parameters = mCamera.getParameters();

                if (mCurrentCamera == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    // Set the auto-focus mode to "continuous"
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }

                // Preview size must exist.
                if (mPreviewSize != null) {
                    Camera.Size previewSize = mPreviewSize;
                    parameters.setPreviewSize(previewSize.width, previewSize.height);
                }

                mCamera.setParameters(parameters);
                setCameraDisplayOrientation(getActivity(), mCurrentCamera, mCamera);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Calculate the measurements of the layout
         *
         * @param widthMeasureSpec
         * @param heightMeasureSpec
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            setMeasuredDimension(width, height);

            if (mSupportedPreviewSizes != null) {
                mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
            }
        }

        /**
         * Update the layout based on rotation and orientation changes.
         *
         * @param changed
         * @param left
         * @param top
         * @param right
         * @param bottom
         */
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            if (changed) {
                final int width = right - left;
                final int height = bottom - top;

                int previewWidth = width;
                int previewHeight = height;

                if (mPreviewSize != null) {
                    Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

                    switch (display.getRotation()) {
                        case Surface.ROTATION_0:
                            previewWidth = mPreviewSize.height;
                            previewHeight = mPreviewSize.width;
                            mCamera.setDisplayOrientation(90);
                            break;
                        case Surface.ROTATION_90:
                            previewWidth = mPreviewSize.width;
                            previewHeight = mPreviewSize.height;
                            break;
                        case Surface.ROTATION_180:
                            previewWidth = mPreviewSize.height;
                            previewHeight = mPreviewSize.width;
                            break;
                        case Surface.ROTATION_270:
                            previewWidth = mPreviewSize.width;
                            previewHeight = mPreviewSize.height;
                            mCamera.setDisplayOrientation(180);
                            break;
                    }
                }

                final int scaledChildHeight = previewHeight * width / previewWidth;
                mCameraView.layout(0, height - scaledChildHeight, width, height);
            }
        }

        /**
         * @param sizes
         * @param width
         * @param height
         * @return
         */
        private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            Camera.Size optimalSize = null;

            final double ASPECT_TOLERANCE = 0.1;
            double targetRatio = (double) height / width;

            // Try to find a size match which suits the whole screen minus the menu on the left.
            for (Camera.Size size : sizes) {

                if (size.height != width) continue;
                double ratio = (double) size.width / size.height;
                if (ratio <= targetRatio + ASPECT_TOLERANCE && ratio >= targetRatio - ASPECT_TOLERANCE) {
                    optimalSize = size;
                }
            }

            // If we cannot find the one that matches the aspect ratio, ignore the requirement.
            if (optimalSize == null) {
                // TODO : Backup in case we don't get a size.
            }

            return optimalSize;
        }
    }
}
