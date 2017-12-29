package opencv.xubaipei.com.opencvfacedete;

import android.Manifest;
import android.app.ActionBar;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    static {
        System.loadLibrary("native-lib");
    }

    final int PERMISSION_CAMERA = 0x01;

    SurfaceView mSurfaceView;
    Camera mCamera;
    SurfaceHolder mHolder;
    FaceDetector mFaceDetector;
    int mBitmapWidth;
    int mBitmapHeight;
    FaceView mFaceView;
    Camera.Size mPreviewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Display display = getWindow().getWindowManager().getDefaultDisplay();
        mBitmapWidth = display.getWidth();
        mBitmapHeight = display.getHeight();
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(this);
        if (mFaceView == null) {
            mFaceView = new FaceView(this);
            addContentView(mFaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this,"no permission",Toast.LENGTH_SHORT).show();
        }else{
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},PERMISSION_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length>0 && requestCode == PERMISSION_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            openCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
        }
    }

    private void openCamera(){
        mHolder = mSurfaceView.getHolder();
        mCamera = Camera.open(1);
        mCamera.startPreview();
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    findFace(bytes);
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }

        List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            mBitmapWidth = previewSizes.get(0).width;
            mBitmapHeight = previewSizes.get(0).height;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mBitmapWidth,mBitmapHeight);
        mPreviewSize = previewSizes.get(0);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {
                mCamera.autoFocus(this);
            }
        });
        mCamera.setDisplayOrientation(90);
    }

    private Bitmap decodeBitmap(byte[] data){
        Bitmap bitmap = null;
        Bitmap tempBitmap = null;

        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,mBitmapWidth,mBitmapHeight,null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!yuvImage.compressToJpeg(new Rect(0,0,mBitmapWidth,mBitmapHeight), 100, baos)) {
            return null;
        }
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inPreferredConfig = Bitmap.Config.RGB_565;
        tempBitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(baos.toByteArray()),null, bfo);

        List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        int index = previewSizes.size() - 3;
        Camera.Size size = previewSizes.get(index);
        bitmap = Bitmap.createScaledBitmap(tempBitmap,size.width,size.height,false);
        return rotate(bitmap,-90);
    }


    //Rotate Bitmap
    public final static Bitmap rotate(Bitmap b, float degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2,
                    (float) b.getHeight() / 2);

            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
                    b.getHeight(), m, true);
            if (b != b2) {
                b.recycle();
                b = b2;
            }

        }
        return b;
    }


    private void findFace(byte[] data){
        Bitmap targetBitmap = decodeBitmap(data);
        FaceDetector.Face[] faces = new FaceDetector.Face[10];
        if (mFaceDetector == null) {
            mFaceDetector = new FaceDetector(targetBitmap.getWidth(),targetBitmap.getHeight(), 10);
        }
        int faceFoundNum = mFaceDetector.findFaces(targetBitmap,faces);
        float eyesDistance = 0f;
        mFaceView.setFace(null,0);

        if (faceFoundNum >0){
            for (int i = 0;i<faceFoundNum;i++){
                FaceDetector.Face face = faces[i];
                if (face != null){
                    PointF eyeMidPoint = new PointF();
                    face.getMidPoint(eyeMidPoint);
                    eyesDistance = face.eyesDistance();
                    Log.e("xubaipei2","eyeDistance:"+eyesDistance+" eyeMidPointX:"+eyeMidPoint.x+"eyeMidPointY:"+eyeMidPoint.y+
                            "poseX:"+face.pose(FaceDetector.Face.EULER_X)+
                            "poseY:"+face.pose(FaceDetector.Face.EULER_Y)+
                            "poseZ:"+face.pose(FaceDetector.Face.EULER_Z));

                    PointF realPoint = new PointF();
                    int targetWidth = targetBitmap.getWidth();
                    int targetHeight = targetBitmap.getHeight();
                    float ratioY = (float)((double)mPreviewSize.width  / (double)targetHeight);
                    float ratioX =    (float )((double)mPreviewSize.height /(double) targetWidth);

                    float realDistance = eyesDistance * ratioY;
                    realPoint.x = (targetWidth - eyeMidPoint.x) * ratioX;
                    realPoint.y = eyeMidPoint.y * ratioY;
                    mFaceView.setFace(realPoint,realDistance);
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
