package com.example.CampusEventDiscovery.ui.event;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.util.Constants;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * CampusMapActivity.java
 *
 * Displays a zoomable and pannable image of a specific campus building.
 * Improved to show the full image initially and handle zooming more naturally.
 */
public class CampusMapActivity extends AppCompatActivity {

    private ImageView ivCampusMap;
    private MaterialToolbar toolbarMap;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF start = new PointF();
    private float minScale = 1f;
    private float maxScale = 5f;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_map);

        ivCampusMap = findViewById(R.id.ivCampusMap);
        toolbarMap = findViewById(R.id.toolbarMap);

        toolbarMap.setNavigationOnClickListener(v -> finish());

        String locationKey = getIntent().getStringExtra("locationKey");
        if (locationKey != null) {
            toolbarMap.setTitle(locationKey);
            ivCampusMap.setImageResource(getMapResource(locationKey));
        }

        setupZoomLogic();

        // Initialize matrix to fit the image when the view is laid out
        ivCampusMap.post(() -> {
            if (ivCampusMap.getDrawable() == null) return;
            
            float viewWidth = ivCampusMap.getWidth();
            float viewHeight = ivCampusMap.getHeight();
            float drawableWidth = ivCampusMap.getDrawable().getIntrinsicWidth();
            float drawableHeight = ivCampusMap.getDrawable().getIntrinsicHeight();

            float scaleX = viewWidth / drawableWidth;
            float scaleY = viewHeight / drawableHeight;
            minScale = Math.min(scaleX, scaleY);
            
            matrix.setScale(minScale, minScale);
            
            // Center the image
            float redundantXSpace = viewWidth - (minScale * drawableWidth);
            float redundantYSpace = viewHeight - (minScale * drawableHeight);
            matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2);
            
            ivCampusMap.setImageMatrix(matrix);
        });
    }

    private int getMapResource(String key) {
        switch (key) {
            case Constants.MAP_LOC_SSE:            return R.drawable.map_sse;
            case Constants.MAP_LOC_HSS:            return R.drawable.map_hss;
            case Constants.MAP_LOC_SAHSOL:         return R.drawable.map_sahsol;
            case Constants.MAP_LOC_SPORTS_COMPLEX: return R.drawable.map_sportscomplex;
            case Constants.MAP_LOC_PARKING_LOT:    return R.drawable.map_parkinglot;
            case Constants.MAP_LOC_REDC:           return R.drawable.map_redc;
            case Constants.MAP_LOC_CRICKET_GROUND: return R.drawable.map_cricketground;
            case Constants.MAP_LOC_SDSB:           return R.drawable.map_sdsb;
            case Constants.MAP_LOC_IST:            return R.drawable.map_ist;
            case Constants.MAP_LOC_MASJID:         return R.drawable.map_masjid;
            default:                               return R.drawable.map_sse;
        }
    }

    private void setupZoomLogic() {
        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float[] values = new float[9];
                matrix.getValues(values);
                float currentScale = values[Matrix.MSCALE_X];

                if (currentScale * scaleFactor < minScale) {
                    scaleFactor = minScale / currentScale;
                } else if (currentScale * scaleFactor > maxScale) {
                    scaleFactor = maxScale / currentScale;
                }

                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                checkBounds();
                ivCampusMap.setImageMatrix(matrix);
                return true;
            }
        });

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float[] values = new float[9];
                matrix.getValues(values);
                float currentScale = values[Matrix.MSCALE_X];
                
                if (currentScale > minScale * 1.1f) {
                    // Reset to min scale
                    float viewWidth = ivCampusMap.getWidth();
                    float viewHeight = ivCampusMap.getHeight();
                    float drawableWidth = ivCampusMap.getDrawable().getIntrinsicWidth();
                    float drawableHeight = ivCampusMap.getDrawable().getIntrinsicHeight();
                    
                    matrix.setScale(minScale, minScale);
                    matrix.postTranslate((viewWidth - minScale * drawableWidth) / 2, 
                                       (viewHeight - minScale * drawableHeight) / 2);
                } else {
                    matrix.postScale(2.0f, 2.0f, e.getX(), e.getY());
                }
                checkBounds();
                ivCampusMap.setImageMatrix(matrix);
                return true;
            }
        });

        ivCampusMap.setOnTouchListener((v, event) -> {
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);

            PointF curr = new PointF(event.getX(), event.getY());

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(curr);
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        float dx = curr.x - start.x;
                        float dy = curr.y - start.y;
                        matrix.set(savedMatrix);
                        matrix.postTranslate(dx, dy);
                        checkBounds();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = ZOOM;
                    break;
            }

            ivCampusMap.setImageMatrix(matrix);
            return true;
        });
    }

    private void checkBounds() {
        if (ivCampusMap.getDrawable() == null) return;

        float[] values = new float[9];
        matrix.getValues(values);
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];

        float viewWidth = ivCampusMap.getWidth();
        float viewHeight = ivCampusMap.getHeight();
        float drawableWidth = ivCampusMap.getDrawable().getIntrinsicWidth() * scaleX;
        float drawableHeight = ivCampusMap.getDrawable().getIntrinsicHeight() * scaleY;

        // Constraint X
        if (drawableWidth < viewWidth) {
            transX = (viewWidth - drawableWidth) / 2;
        } else {
            if (transX > 0) transX = 0;
            if (transX < viewWidth - drawableWidth) transX = viewWidth - drawableWidth;
        }

        // Constraint Y
        if (drawableHeight < viewHeight) {
            transY = (viewHeight - drawableHeight) / 2;
        } else {
            if (transY > 0) transY = 0;
            if (transY < viewHeight - drawableHeight) transY = viewHeight - drawableHeight;
        }

        values[Matrix.MTRANS_X] = transX;
        values[Matrix.MTRANS_Y] = transY;
        matrix.setValues(values);
    }
}
