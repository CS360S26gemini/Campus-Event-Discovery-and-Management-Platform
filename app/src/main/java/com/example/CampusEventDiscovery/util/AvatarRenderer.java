package com.example.CampusEventDiscovery.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.example.CampusEventDiscovery.model.AvatarConfig;

/**
 * Draws local, offline profile avatars from a small set of configurable traits.
 */
public final class AvatarRenderer {

    private static final int[] SKIN_TONES = {
            Color.rgb(255, 224, 189),
            Color.rgb(241, 194, 125),
            Color.rgb(224, 172, 105),
            Color.rgb(198, 134, 66),
            Color.rgb(141, 85, 36)
    };

    private static final int[] HAIR_COLORS = {
            Color.rgb(35, 31, 32),
            Color.rgb(88, 56, 39),
            Color.rgb(143, 80, 49),
            Color.rgb(214, 170, 92),
            Color.rgb(113, 75, 145)
    };

    private static final int[] BACKGROUNDS = {
            Color.rgb(123, 47, 190),
            Color.rgb(32, 122, 104),
            Color.rgb(35, 105, 176),
            Color.rgb(190, 85, 70),
            Color.rgb(88, 91, 105)
    };

    private static final int[] SHIRTS = {
            Color.rgb(64, 74, 190),
            Color.rgb(39, 137, 90),
            Color.rgb(212, 118, 44),
            Color.rgb(135, 62, 158),
            Color.rgb(33, 36, 46)
    };

    private AvatarRenderer() {
        // Utility class.
    }

    public static Bitmap render(AvatarConfig config, int sizePx) {
        int safeSize = Math.max(64, sizePx);
        AvatarConfig safeConfig = config == null ? new AvatarConfig() : config;

        Bitmap bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float size = safeSize;

        drawBackground(canvas, paint, safeConfig, size);
        drawShoulders(canvas, paint, safeConfig, size);
        drawHairBehindFace(canvas, paint, safeConfig, size);
        drawFace(canvas, paint, safeConfig, size);
        drawHairFront(canvas, paint, safeConfig, size);
        drawFeatures(canvas, paint, safeConfig, size);
        drawAccessory(canvas, paint, safeConfig, size);
        drawInitials(canvas, paint, safeConfig, size);

        return bitmap;
    }

    private static void drawBackground(Canvas canvas, Paint paint, AvatarConfig config, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BACKGROUNDS[config.getBackground()]);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setColor(Color.argb(42, 255, 255, 255));
        canvas.drawCircle(size * 0.2f, size * 0.22f, size * 0.18f, paint);
        canvas.drawCircle(size * 0.85f, size * 0.75f, size * 0.22f, paint);
    }

    private static void drawShoulders(Canvas canvas, Paint paint, AvatarConfig config, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SHIRTS[config.getShirtColor()]);
        RectF shirt = new RectF(size * 0.16f, size * 0.68f, size * 0.84f, size * 1.08f);
        canvas.drawRoundRect(shirt, size * 0.24f, size * 0.24f, paint);

        paint.setColor(SKIN_TONES[config.getSkinTone()]);
        RectF neck = new RectF(size * 0.42f, size * 0.58f, size * 0.58f, size * 0.76f);
        canvas.drawRoundRect(neck, size * 0.06f, size * 0.06f, paint);
    }

    private static void drawHairBehindFace(Canvas canvas, Paint paint, AvatarConfig config, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(HAIR_COLORS[config.getHairColor()]);

        if (config.getHairStyle() == 1) {
            RectF hair = new RectF(size * 0.24f, size * 0.16f, size * 0.76f, size * 0.68f);
            canvas.drawRoundRect(hair, size * 0.23f, size * 0.23f, paint);
        } else if (config.getHairStyle() == 3) {
            canvas.drawCircle(size * 0.73f, size * 0.28f, size * 0.12f, paint);
        }
    }

    private static void drawFace(Canvas canvas, Paint paint, AvatarConfig config, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SKIN_TONES[config.getSkinTone()]);
        canvas.drawCircle(size * 0.28f, size * 0.45f, size * 0.055f, paint);
        canvas.drawCircle(size * 0.72f, size * 0.45f, size * 0.055f, paint);
        canvas.drawOval(new RectF(size * 0.27f, size * 0.19f, size * 0.73f, size * 0.65f), paint);
    }

    private static void drawHairFront(Canvas canvas, Paint paint, AvatarConfig config, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(HAIR_COLORS[config.getHairColor()]);

        switch (config.getHairStyle()) {
            case 0:
                canvas.drawArc(new RectF(size * 0.27f, size * 0.15f, size * 0.73f, size * 0.48f), 180, 180, true, paint);
                break;
            case 1:
                canvas.drawArc(new RectF(size * 0.28f, size * 0.14f, size * 0.72f, size * 0.42f), 180, 180, true, paint);
                break;
            case 2:
                for (int i = 0; i < 7; i++) {
                    float cx = size * (0.29f + i * 0.07f);
                    float cy = size * (0.25f + (i % 2 == 0 ? 0.01f : -0.02f));
                    canvas.drawCircle(cx, cy, size * 0.075f, paint);
                }
                break;
            case 3:
                canvas.drawArc(new RectF(size * 0.30f, size * 0.13f, size * 0.70f, size * 0.42f), 180, 180, true, paint);
                break;
            case 4:
            default:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(size * 0.03f);
                canvas.drawArc(new RectF(size * 0.30f, size * 0.18f, size * 0.70f, size * 0.40f), 190, 160, false, paint);
                paint.setStyle(Paint.Style.FILL);
                break;
        }
    }

    private static void drawFeatures(Canvas canvas, Paint paint, AvatarConfig config, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(32, 32, 36));
        canvas.drawCircle(size * 0.42f, size * 0.42f, size * 0.025f, paint);
        canvas.drawCircle(size * 0.58f, size * 0.42f, size * 0.025f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(size * 0.018f);
        paint.setColor(darken(SKIN_TONES[config.getSkinTone()]));
        canvas.drawLine(size * 0.50f, size * 0.44f, size * 0.48f, size * 0.51f, paint);

        paint.setColor(Color.rgb(117, 55, 57));
        paint.setStrokeWidth(size * 0.024f);
        canvas.drawArc(new RectF(size * 0.42f, size * 0.48f, size * 0.58f, size * 0.59f), 20, 140, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void drawAccessory(Canvas canvas, Paint paint, AvatarConfig config, float size) {
        switch (config.getAccessory()) {
            case 1:
                drawGlasses(canvas, paint, size);
                break;
            case 2:
                drawCap(canvas, paint, size);
                break;
            case 3:
                drawHeadphones(canvas, paint, size);
                break;
            case 0:
            default:
                break;
        }
    }

    private static void drawGlasses(Canvas canvas, Paint paint, float size) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.018f);
        paint.setColor(Color.rgb(24, 25, 30));
        canvas.drawCircle(size * 0.42f, size * 0.42f, size * 0.055f, paint);
        canvas.drawCircle(size * 0.58f, size * 0.42f, size * 0.055f, paint);
        canvas.drawLine(size * 0.475f, size * 0.42f, size * 0.525f, size * 0.42f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void drawCap(Canvas canvas, Paint paint, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(28, 34, 51));
        Path cap = new Path();
        cap.moveTo(size * 0.28f, size * 0.27f);
        cap.quadTo(size * 0.50f, size * 0.08f, size * 0.72f, size * 0.27f);
        cap.lineTo(size * 0.68f, size * 0.33f);
        cap.quadTo(size * 0.50f, size * 0.25f, size * 0.32f, size * 0.33f);
        cap.close();
        canvas.drawPath(cap, paint);

        paint.setColor(Color.rgb(235, 236, 242));
        canvas.drawOval(new RectF(size * 0.46f, size * 0.17f, size * 0.54f, size * 0.25f), paint);
    }

    private static void drawHeadphones(Canvas canvas, Paint paint, float size) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.025f);
        paint.setColor(Color.rgb(25, 28, 36));
        canvas.drawArc(new RectF(size * 0.31f, size * 0.21f, size * 0.69f, size * 0.55f), 200, 140, false, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(size * 0.25f, size * 0.38f, size * 0.32f, size * 0.52f), size * 0.03f, size * 0.03f, paint);
        canvas.drawRoundRect(new RectF(size * 0.68f, size * 0.38f, size * 0.75f, size * 0.52f), size * 0.03f, size * 0.03f, paint);
    }

    private static void drawInitials(Canvas canvas, Paint paint, AvatarConfig config, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(size * 0.13f);
        paint.setColor(Color.WHITE);
        canvas.drawText(config.getInitials(), size * 0.5f, size * 0.86f, paint);
        paint.setFakeBoldText(false);
    }

    private static int darken(int color) {
        return Color.rgb(
                Math.max(0, (int) (Color.red(color) * 0.72f)),
                Math.max(0, (int) (Color.green(color) * 0.72f)),
                Math.max(0, (int) (Color.blue(color) * 0.72f))
        );
    }
}
