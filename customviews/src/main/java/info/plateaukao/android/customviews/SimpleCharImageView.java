package info.plateaukao.android.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

public class SimpleCharImageView extends SquareImageView {
	private int borderSize = 20;
	private int crossSize = 5;

	private int gridline_color = Color.RED;
	private Paint paint = new Paint();

	public enum GRID_TYPE {
	    GRID_9,
	    DIAGNAL
	}
	private GRID_TYPE gridType = GRID_TYPE.GRID_9;

	public void setGridType(GRID_TYPE type) {
		gridType = type;
		invalidate();
	}

    public SimpleCharImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
		init(context, attrs);
    }

    public SimpleCharImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
		init(context, attrs);
    }

    public SimpleCharImageView(Context context) {
        super(context);
		init(context, null);
    }

	private void init(Context context, AttributeSet attrs) {
	    TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CalliImageView);
	    try{
	        borderSize = (int)a.getDimension(R.styleable.CalliImageView_borderSize, 20);
	        crossSize =  (int)a.getDimension(R.styleable.CalliImageView_crossSize, 5);
	    } finally {
	        a.recycle();
	    }
	    
	}

    @Override
    protected void onDraw(Canvas canvas) {
        drawGridLine(canvas);
        super.onDraw(canvas);
    }

    
    //private PorterDuffXfermode mode = new PorterDuffXfermode(Mode.XOR);
    private void drawGridLine(Canvas canvas){
        //paint.setXfermode(mode);
    	paint.setColor(gridline_color);
        paint.setAlpha(100);

    	paint.setStrokeWidth(borderSize);

    	// border
    	canvas.drawLine(0, 0, getMeasuredWidth(), 0, paint);
    	canvas.drawLine(0, 0, 0, getMeasuredHeight(), paint);
    	canvas.drawLine(getMeasuredWidth(), 0, getMeasuredWidth(), getMeasuredHeight(), paint);
    	canvas.drawLine(0, getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight(), paint);

    	// inside
    	switch (gridType){
    	    case GRID_9:
    	    {
    	        float stepWidth = getMeasuredWidth() / 3;
    	        float stepHeight = getMeasuredHeight() / 3;

    	        paint.setStrokeWidth(crossSize);

    	        canvas.drawLine(0, stepHeight, getMeasuredWidth(), stepHeight, paint);
    	        canvas.drawLine(0, stepHeight*2, getMeasuredWidth(), stepHeight*2, paint);
    	        canvas.drawLine(stepWidth, 0, stepWidth, getMeasuredHeight(), paint);
    	        canvas.drawLine(stepWidth*2, 0, stepWidth*2, getMeasuredHeight(), paint);
    	        break;
    	    }
    	    case DIAGNAL:
    	    {
    	        paint.setStrokeWidth(crossSize);
    	        // diagnal
    	        canvas.drawLine(0, 0, getMeasuredWidth(), getMeasuredHeight(), paint);
    	        canvas.drawLine(0, getMeasuredHeight(), getMeasuredWidth(), 0, paint);
    	        
    	        
    	        // cross
    	        float stepWidth = getMeasuredWidth() / 2;
    	        float stepHeight = getMeasuredHeight() / 2;


    	        canvas.drawLine(0, stepHeight, getMeasuredWidth(), stepHeight, paint);
    	        canvas.drawLine(stepWidth, 0, stepWidth, getMeasuredHeight(), paint);
    	        break;
    	    }
    	    default:
    	        break;
    	}
    }
}
