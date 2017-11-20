package example.com.super_res;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import static android.R.attr.x;

/**
 * Created by hania on 13.11.17.
 */

public class MyDragView extends View {
    //private final static String LogTag = MyRect.VIEW_LOG_TAG
    private int myWidth;
    //private Region mRegion;
    private final Point mStartPosition = new Point(0, 0);
    private Point mPosition;
    private Context mContext;
    private Boolean inRect;
    private int pX, pY, mX, mY;


    public MyDragView(Context context, int width, int x, int y) {
        super(context);
        setFocusable(true);
        myWidth = width;
        mContext = context;
        mPosition = new Point(mStartPosition.x, mStartPosition.y);
        inRect = false;
        pX = x;
        pY = y;
        mX = 1;
        mY = 1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Point position = getPosition();
        Paint myPaint = new Paint();
        int myColor = ContextCompat.getColor(mContext, R.color.ketchup);
        myPaint.setColor(myColor);
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setStrokeWidth(10);
        canvas.drawRect(mPosition.x, mPosition.y, mPosition.x + myWidth, mPosition.y + myWidth, myPaint);
        mX = canvas.getHeight();
        mY = canvas.getWidth();
    }

    public final Point getPosition()
    {
//        int x = mPosition.x/mX *pX;
//        int y = mPosition.y/mY *pY;
//
//        return new Point(x, y);
        return mPosition;
    }

    public final void setPosition(final Point position)
    {
       mPosition = position;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int X = (int) event.getX() - myWidth/2;
        int Y = (int) event.getY() - myWidth/2;

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN: {
                if (X > mPosition.x && X < mPosition.x + myWidth) {
                    inRect = true;
                }
                if (Y > mPosition.y && Y < mPosition.y + myWidth) {
                    inRect = true;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int x = 0, y = 0;

                mPosition.x = X;
                mPosition.y = Y;

                invalidate();

                break;
            }
            case MotionEvent.ACTION_UP:
            {
                inRect = false;
            }
            default:{
                return super.onTouchEvent(event);
            }

        }
        return true;
    }
}
