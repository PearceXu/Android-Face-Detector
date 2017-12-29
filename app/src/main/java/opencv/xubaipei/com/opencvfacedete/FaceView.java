package opencv.xubaipei.com.opencvfacedete;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by xubaipei on 2017/12/28.
 */

public class FaceView extends View {
    PointF mMidPoint;
    float mEyeDistance;
    Paint mPaint;
    public FaceView(Context context) {
        super(context);
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setFace(PointF midEyePoint, float eyeDistance){
        if (midEyePoint == null || eyeDistance == 0){
            mMidPoint = new PointF();
            mEyeDistance = eyeDistance;
        }else {
            mMidPoint = midEyePoint;
            mEyeDistance = eyeDistance;
        }
        invalidate();
    }
    private void init(){
        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(2);
        mMidPoint = new PointF();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mEyeDistance = mEyeDistance *2;
        int pWidth =(int) ( mMidPoint.x - (mEyeDistance * 0.5f));
        int pHeight =(int) ( mMidPoint.y - (mEyeDistance * 0.5f)) ;
        Log.e("xubaipei","Paint-----------------pWidth:"+pWidth+" pHeight:"+pHeight);
        if (mEyeDistance == 0){
            return;
        }
        canvas.drawRect(pWidth,pHeight, pWidth + mEyeDistance,pHeight + mEyeDistance + mEyeDistance*0.2f,mPaint);
    }
}
