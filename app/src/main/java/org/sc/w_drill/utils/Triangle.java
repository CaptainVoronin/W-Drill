package org.sc.w_drill.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import org.sc.w_drill.R;

/**
 * TODO: document your custom view class.
 */
public class Triangle extends View {
    public enum Position { U_LEFT, U_RIGHT, B_LEFT, B_RIGHT };

    private int mColor = Color.RED;
    private float mDimension = 0;
    private Position position;

    public Triangle(Context context) {
        super(context);
        init(null, 0);
    }

    public Triangle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public Triangle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.Triangle, defStyle, 0);

        mColor = a.getColor(
                R.styleable.Triangle_color,
                mColor);

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mDimension = a.getDimension(
                R.styleable.Triangle_dimension,
                mDimension);

        int p = a.getInteger( R.styleable.Triangle_position, 0 );

        switch( p )
        {
            case 0:
                position = Position.U_LEFT;
                break;
            case 1:
                position = Position.U_RIGHT;
                break;
            case 2:
                position = Position.B_LEFT;
                break;
            case 3:
                position = Position.B_RIGHT;
                break;
        }
        a.recycle();

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Point a, b, c;

        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        Paint paint = new Paint();

        paint.setColor( mColor );
        //canvas.drawPaint(paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        if( position == Position.U_LEFT )
        {
            a = new Point(0, 0);
            b = new Point(0, contentHeight);
            c = new Point(contentHeight, 0);
        } else if( position == Position.U_RIGHT )
        {
            a = new Point( contentWidth, 0);
            b = new Point( contentWidth, contentHeight);
            c = new Point( 0, 0);
        } else if( position == Position.B_LEFT )
        {
            a = new Point( 0, contentHeight );
            b = new Point( 0, 0);
            c = new Point( contentHeight, contentHeight);
        } else
        { // The bottom right position
            a = new Point( contentWidth, contentHeight );
            b = new Point( 0, contentHeight);
            c = new Point( contentHeight, 0);
        }

        Path path = new Path();
        path.reset();

        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo( a.x, a.y );
        path.lineTo(b.x, b.y);
        path.lineTo(c.x, c.y);
        path.lineTo(a.x, a.y);
        path.close();

        canvas.drawPath(path, paint);
    }

    /**
     * Gets the example color attribute value.
     * @return The example color attribute value.
     */
    public int getColor() {
        return mColor;
    }

    public void setColor( int _color )
    {

        mColor = _color;
        invalidate();
    }

    /**
     * Gets the example dimension attribute value.
     * @return The example dimension attribute value.
     */
    public float getDimension() {
        return mDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setDimension(float exampleDimension) {
        mDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    public Position getPosition()
    {
        return position;
    }
}
