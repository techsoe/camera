package com.example.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class SquareOverlayView extends View {
    private Paint squarePaint;

    public SquareOverlayView(Context context) {
        super(context);
        init();
    }

    public SquareOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SquareOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        squarePaint = new Paint();
        squarePaint.setColor(getResources().getColor(R.color.white));
        squarePaint.setStyle(Paint.Style.STROKE);
        squarePaint.setStrokeWidth(10); // Adjust the stroke width as needed
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int squareSize = Math.min(width, height) / 2; // Adjust the size of the square as needed
        int left = (width - squareSize) / 2;
        int top = (height - squareSize) / 2;
        int right = left + squareSize;
        int bottom = top + squareSize;

        canvas.drawRect(left, top, right, bottom, squarePaint);
    }


}
