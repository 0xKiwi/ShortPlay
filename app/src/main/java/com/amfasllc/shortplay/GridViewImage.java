package com.amfasllc.shortplay;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class GridViewImage extends ImageView {

    public GridViewImage(Context context) {
        super(context);
    }

    public GridViewImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridViewImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
