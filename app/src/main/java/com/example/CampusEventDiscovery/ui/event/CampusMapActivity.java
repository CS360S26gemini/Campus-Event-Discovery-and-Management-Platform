package com.example.CampusEventDiscovery.ui.event;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * CampusMapActivity.java
 *
 * Displays a zoomable and pannable map image with boundary constraints.
 * Ensures the image starts centered and cannot be zoomed out beyond fitting the screen.
 */
public class CampusMapActivity extends AppCompatActivity implements View.OnTouchListener {

    private ImageView ivCampusMap;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    // States
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // Zooming/Panning tracking
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private float minScale = 1f;
    private float maxScale = 5f;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_map);

        MaterialToolbar toolbar = findViewById(R.id.toolbarCampusMap);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivCampusMap = findViewById(R.id.ivCampusMap);
        ivCampusMap.setScaleType(ImageView.ScaleType.MATRIX);
        ivCampusMap.setOnTouchListener(this);
        
        String locationKey = getIntent().getStringExtra("locationKey");
        if (locationKey != null) {
            toolbar.setTitle("Map: " + locationKey);
            ivCampusMap.setImageResource(getMapResource(locationKey));
        } else {
            ivCampusMap.setImageResource(R.drawable.map_hss);
        }

        // Initialize matrix after the view is laid out to center the image
        ivCampusMap.post(this::initializeMapMatrix);
    }

    private void initializeMapMatrix() {
        Drawable drawable = ivCampusMap.getDrawable();
        if (drawable == null) return;

        float viewWidth = ivCampusMap.getWidth();
        float viewHeight = ivCampusMap.getHeight();
        float imgWidth = drawable.getIntrinsicWidth();
        float imgHeight = drawable.getIntrinsicHeight();

        // Calculate scale to fit image to screen (aspect fit)
        float scaleX = viewWidth / imgWidth;
        float scaleY = viewHeight / imgHeight;
        minScale = Math.min(scaleX, scaleY);
        
        matrix.setScale(minScale, minScale);

        // Center the image
        float redundantXSpace = viewWidth - (minScale * imgWidth);
        float redundantYSpace = viewHeight - (minScale * imgHeight);
        matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2);

        ivCampusMap.setImageMatrix(matrix);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView view = (ImageView) v;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        
                        // Limit zoom out to minScale
                        float[] values = new float[9];
                        matrix.getValues(values);
                        float currentScale = values[Matrix.MSCALE_X];
                        if (currentScale * scale < minScale) {
                            scale = minScale / currentScale;
                        } else if (currentScale * scale > maxScale) {
                            scale = maxScale / currentScale;
                        }
                        
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        // Boundary checks to prevent dragging image off screen
        checkBounds();
        view.setImageMatrix(matrix);
        return true;
    }

    private void checkBounds() {
        float[] values = new float[9];
        matrix.getValues(values);
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];

        Drawable d = ivCampusMap.getDrawable();
        if (d == null) return;

        float imgWidth = d.getIntrinsicWidth() * scaleX;
        float imgHeight = d.getIntrinsicHeight() * scaleY;
        float viewWidth = ivCampusMap.getWidth();
        float viewHeight = ivCampusMap.getHeight();

        // Prevent dragging away from edges if image is smaller than view
        if (imgWidth < viewWidth) {
            transX = (viewWidth - imgWidth) / 2;
        } else {
            if (transX > 0) transX = 0;
            if (transX < viewWidth - imgWidth) transX = viewWidth - imgWidth;
        }

        if (imgHeight < viewHeight) {
            transY = (viewHeight - imgHeight) / 2;
        } else {
            if (transY > 0) transY = 0;
            if (transY < viewHeight - imgHeight) transY = viewHeight - imgHeight;
        }

        values[Matrix.MTRANS_X] = transX;
        values[Matrix.MTRANS_Y] = transY;
        matrix.setValues(values);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private int getMapResource(String key) {
        if (key == null) return R.drawable.map_hss;

        switch (key) {
            case Constants.MAP_LOC_SSE: return R.drawable.map_sse;
            case Constants.MAP_LOC_SDSB: return R.drawable.map_sdsb;
            case Constants.MAP_LOC_SAHSOL: return R.drawable.map_sahsol;
            case Constants.MAP_LOC_SPORTS_COMPLEX: return R.drawable.map_sportscomplex;
            case Constants.MAP_LOC_PARKING_LOT: return R.drawable.map_parkinglot;
            case Constants.MAP_LOC_REDC: return R.drawable.map_redc;
            case Constants.MAP_LOC_CRICKET_GROUND: return R.drawable.map_cricketground;
            case Constants.MAP_LOC_IST: return R.drawable.map_ist;
            case Constants.MAP_LOC_MASJID: return R.drawable.map_masjid;
            default: return R.drawable.map_hss;
        }
    }
}
