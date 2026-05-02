package com.example.CampusEventDiscovery.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.example.CampusEventDiscovery.R;

/**
 * Stores and applies local app appearance preferences.
 */
public final class ThemeManager {

    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_ACCENT = "accent_color";
    private static final int BASE_PURPLE = Color.rgb(123, 47, 190);
    private static final int LIGHT_PURPLE_RIPPLE = Color.argb(0x33, 123, 47, 190);
    private static final int DARK_PURPLE_RIPPLE = Color.argb(0x55, 123, 47, 190);
    private static boolean touchFeedbackLifecycleInstalled = false;

    public static final int ACCENT_PURPLE = 0;
    public static final int ACCENT_BLUE = 1;
    public static final int ACCENT_GREEN = 2;
    public static final int ACCENT_ORANGE = 3;
    public static final int ACCENT_PINK = 4;
    public static final int ACCENT_TEAL = 5;

    private static final int[] ACCENT_COLORS = {
            Color.rgb(123, 47, 190), // Purple
            Color.rgb(30, 96, 178),  // Blue
            Color.rgb(31, 129, 92),  // Green
            Color.rgb(196, 88, 42),  // Orange
            Color.rgb(188, 24, 110), // Pink
            Color.rgb(0, 106, 106)   // Teal
    };

    private ThemeManager() {
    }

    public static void applyStoredTheme(Context context) {
        applyThemeMode(isDarkModeEnabled(context));
    }

    public static void applyAccentTheme(Activity activity) {
        if (activity != null) {
            activity.setTheme(resolveAccentTheme(activity));
        }
    }

    public static void installAccentLifecycle(Application application) {
        if (application == null || touchFeedbackLifecycleInstalled) {
            return;
        }
        touchFeedbackLifecycleInstalled = true;
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                scheduleTouchFeedbackPass(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    public static boolean applyThemePreference(Context context, boolean darkMode) {
        saveThemePreference(context, darkMode);
        return applyThemeMode(darkMode);
    }

    public static boolean syncThemePreference(Context context, boolean darkMode) {
        saveThemePreference(context, darkMode);
        return applyThemeMode(darkMode);
    }

    public static boolean isDarkModeEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, true);
    }

    public static int getAccentPreference(Context context) {
        int accent = getPrefs(context).getInt(KEY_ACCENT, ACCENT_PURPLE);
        if (accent < ACCENT_PURPLE || accent > ACCENT_TEAL) {
            return ACCENT_PURPLE;
        }
        return accent;
    }

    public static void setAccentPreference(Context context, int accent) {
        getPrefs(context)
                .edit()
                .putInt(KEY_ACCENT, accent)
                .apply();
    }

    public static void applyAccentToActivity(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        View root = activity.getWindow().getDecorView();
        applyAccentToViewTree(activity, root);
    }

    public static int getAccentColor(Context context) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            return typedValue.data;
        }
        return ACCENT_COLORS[getAccentPreference(context)];
    }

    public static int resolveAccentTheme(Context context) {
        int accent = getAccentPreference(context);
        switch (accent) {
            case ACCENT_BLUE:
                return R.style.Theme_CampusEventDiscovery_Blue;
            case ACCENT_GREEN:
                return R.style.Theme_CampusEventDiscovery_Green;
            case ACCENT_ORANGE:
                return R.style.Theme_CampusEventDiscovery_Orange;
            case ACCENT_PINK:
                return R.style.Theme_CampusEventDiscovery_Pink;
            case ACCENT_TEAL:
                return R.style.Theme_CampusEventDiscovery_Teal;
            default:
                return R.style.Theme_CampusEventDiscovery;
        }
    }

    public static void applyAccentToSearchSortControl(Context context, TextView sortControl) {
        if (sortControl != null) {
            sortControl.setTextColor(getAccentColor(context));
        }
    }

    public static void applyAccentToMainNavigation(Context context,
                                                   BottomNavigationView bottomNavigationView,
                                                   ExtendedFloatingActionButton fab) {
        int accentColor = getAccentColor(context);
        if (bottomNavigationView != null) {
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
            };
            int[] colors = new int[]{
                    accentColor,
                    resolveMutedNavColor(context)
            };
            ColorStateList navColors = new ColorStateList(states, colors);
            bottomNavigationView.setItemIconTintList(navColors);
            bottomNavigationView.setItemTextColor(navColors);
        }

        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        }
    }

    public static void applyAccentPreview(View preview, Context context) {
        if (preview != null) {
            preview.setBackgroundTintList(ColorStateList.valueOf(getAccentColor(context)));
        }
    }

    public static void applyAccentToViewTree(Context context, View root) {
        if (context == null || root == null) {
            return;
        }

        int accentColor = getAccentColor(context);
        int onAccentColor = Color.WHITE;
        int rippleColor = withAlpha(accentColor, isDarkModeEnabled(context) ? 0x55 : 0x33);
        ColorStateList accentList = ColorStateList.valueOf(accentColor);
        ColorStateList onAccentList = ColorStateList.valueOf(onAccentColor);
        ColorStateList rippleList = ColorStateList.valueOf(rippleColor);
        applyTouchFeedback(context, root, rippleList);

        if (root instanceof BottomNavigationView) {
            applyAccentToMainNavigation(context, (BottomNavigationView) root, null);
        } else if (root instanceof ExtendedFloatingActionButton) {
            ((ExtendedFloatingActionButton) root).setBackgroundTintList(accentList);
        } else if (root instanceof MaterialButton) {
            applyAccentToMaterialButton((MaterialButton) root, accentList, onAccentList, rippleList);
        } else if (root instanceof Chip) {
            applyAccentToChip((Chip) root, accentColor, rippleColor);
        } else if (root instanceof TextInputLayout) {
            applyAccentToTextInput((TextInputLayout) root, context, accentColor);
        } else if (root instanceof MaterialCardView) {
            MaterialCardView cardView = (MaterialCardView) root;
            if (usesPurple(cardView.getRippleColor())) {
                cardView.setRippleColor(rippleList);
            }
        } else if (root instanceof CompoundButton) {
            applyAccentToCompoundButton((CompoundButton) root, context, accentColor);
        } else if (root instanceof ProgressBar) {
            ProgressBar progressBar = (ProgressBar) root;
            progressBar.setIndeterminateTintList(accentList);
            progressBar.setProgressTintList(accentList);
        } else if (root instanceof ImageView) {
            ImageView imageView = (ImageView) root;
            if (usesPurple(imageView.getImageTintList())) {
                imageView.setImageTintList(accentList);
            }
        } else if (root instanceof TextView) {
            TextView textView = (TextView) root;
            if (isPurpleLike(textView.getCurrentTextColor())) {
                textView.setTextColor(accentColor);
            }
        }

        if (!(root instanceof MaterialButtonToggleGroup) && usesPurple(root.getBackgroundTintList())) {
            root.setBackgroundTintList(accentList);
        }

        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAccentToViewTree(context, group.getChildAt(i));
            }
        }
    }

    public static void applyTouchFeedbackToViewTree(Context context, View root) {
        if (context == null || root == null) {
            return;
        }
        int accentColor = getAccentColor(context);
        int rippleColor = withAlpha(accentColor, isDarkModeEnabled(context) ? 0x55 : 0x33);
        applyTouchFeedback(context, root, ColorStateList.valueOf(rippleColor));
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTouchFeedbackToViewTree(context, group.getChildAt(i));
            }
        }
    }

    private static void scheduleTouchFeedbackPass(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        View root = activity.getWindow().getDecorView();
        Runnable pass = () -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                applyTouchFeedbackToViewTree(activity, root);
            }
        };
        root.post(pass);
        root.postDelayed(pass, 350L);
    }

    private static void applyTouchFeedback(Context context, View view, ColorStateList rippleList) {
        if (view instanceof MaterialCardView) {
            MaterialCardView cardView = (MaterialCardView) view;
            if (cardView.isClickable() || cardView.isFocusable()) {
                cardView.setRippleColor(rippleList);
            }
            return;
        }

        if (!view.isClickable() || shouldKeepNativeTouchFeedback(view)) {
            return;
        }

        if (view.getForeground() == null) {
            Drawable foreground = resolveSelectableForeground(context);
            if (foreground != null) {
                view.setForeground(foreground);
            }
        }
    }

    private static boolean shouldKeepNativeTouchFeedback(View view) {
        return view instanceof MaterialButton
                || view instanceof Chip
                || view instanceof BottomNavigationView
                || view instanceof CompoundButton
                || view instanceof TextInputLayout
                || view instanceof android.widget.EditText;
    }

    private static Drawable resolveSelectableForeground(Context context) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true)
                || value.resourceId == 0) {
            return null;
        }
        return context.getDrawable(value.resourceId);
    }

    private static void saveThemePreference(Context context, boolean darkMode) {
        getPrefs(context)
                .edit()
                .putBoolean(KEY_DARK_MODE, darkMode)
                .apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static int resolveMutedNavColor(Context context) {
        return isDarkModeEnabled(context)
                ? Color.rgb(204, 196, 207)
                : Color.rgb(74, 69, 78);
    }

    private static void applyAccentToMaterialButton(MaterialButton button,
                                                    ColorStateList accentList,
                                                    ColorStateList onAccentList,
                                                    ColorStateList rippleList) {
        if (isSegmentedControlButton(button)) {
            return;
        }

        if (isInsideMaterialButtonToggleGroup(button)) {
            return;
        }

        if (usesPurple(button.getBackgroundTintList())) {
            button.setBackgroundTintList(accentList);
            button.setTextColor(onAccentList);
            button.setIconTint(onAccentList);
        }

        if (usesPurple(button.getStrokeColor())) {
            button.setStrokeColor(accentList);
            button.setTextColor(accentList);
            button.setIconTint(accentList);
        }

        if (usesPurple(button.getRippleColor())) {
            button.setRippleColor(rippleList);
        }

        if (isPurpleLike(button.getCurrentTextColor())) {
            button.setTextColor(accentList);
        }

        if (usesPurple(button.getIconTint())) {
            button.setIconTint(accentList);
        }
    }

    private static boolean isInsideMaterialButtonToggleGroup(View view) {
        ViewParent parent = view == null ? null : view.getParent();
        while (parent instanceof View) {
            if (parent instanceof MaterialButtonToggleGroup) {
                return true;
            }
            parent = ((View) parent).getParent();
        }
        return false;
    }

    private static boolean isSegmentedControlButton(MaterialButton button) {
        if (button == null) {
            return false;
        }

        int id = button.getId();
        return id == R.id.btnPendingApprovals
                || id == R.id.btnRejectedEvents
                || id == R.id.btnVendorApproved
                || id == R.id.btnVendorPending
                || id == R.id.btnVendorRejected;
    }

    private static void applyAccentToChip(Chip chip, int accentColor, int rippleColor) {
        if (chip.isCheckable()) {
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
            };
            int[] chipColors = new int[]{
                    accentColor,
                    chip.getChipBackgroundColor() == null
                            ? Color.TRANSPARENT
                            : chip.getChipBackgroundColor().getDefaultColor()
            };
            chip.setChipBackgroundColor(new ColorStateList(states, chipColors));
        } else if (usesPurple(chip.getChipBackgroundColor())) {
            chip.setChipBackgroundColor(ColorStateList.valueOf(accentColor));
        }

        if (usesPurple(chip.getChipStrokeColor())) {
            chip.setChipStrokeColor(ColorStateList.valueOf(accentColor));
        }
        if (usesPurple(chip.getRippleColor())) {
            chip.setRippleColor(ColorStateList.valueOf(rippleColor));
        }
        if (isPurpleLike(chip.getCurrentTextColor())) {
            chip.setTextColor(accentColor);
        }
    }

    private static void applyAccentToTextInput(TextInputLayout textInputLayout,
                                               Context context,
                                               int accentColor) {
        int surfaceVariant = isDarkModeEnabled(context)
                ? Color.rgb(204, 196, 207)
                : Color.rgb(74, 69, 78);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_focused},
                new int[]{}
        };
        textInputLayout.setBoxStrokeColorStateList(new ColorStateList(
                states,
                new int[]{accentColor, surfaceVariant}
        ));
    }

    private static void applyAccentToCompoundButton(CompoundButton button,
                                                    Context context,
                                                    int accentColor) {
        int uncheckedColor = resolveMutedNavColor(context);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        button.setButtonTintList(new ColorStateList(states, new int[]{accentColor, uncheckedColor}));
    }

    private static boolean usesPurple(ColorStateList colorStateList) {
        if (colorStateList == null) {
            return false;
        }

        return isPurpleLike(colorStateList.getDefaultColor())
                || isPurpleLike(colorStateList.getColorForState(new int[]{android.R.attr.state_checked}, Color.TRANSPARENT))
                || isPurpleLike(colorStateList.getColorForState(new int[]{android.R.attr.state_pressed}, Color.TRANSPARENT))
                || isPurpleLike(colorStateList.getColorForState(new int[]{android.R.attr.state_focused}, Color.TRANSPARENT));
    }

    private static boolean isPurpleLike(int color) {
        return sameRgb(color, BASE_PURPLE)
                || sameRgb(color, LIGHT_PURPLE_RIPPLE)
                || sameRgb(color, DARK_PURPLE_RIPPLE);
    }

    private static boolean sameRgb(int left, int right) {
        return Color.red(left) == Color.red(right)
                && Color.green(left) == Color.green(right)
                && Color.blue(left) == Color.blue(right);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static boolean applyThemeMode(boolean darkMode) {
        int targetMode = darkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;

        if (AppCompatDelegate.getDefaultNightMode() == targetMode) {
            return false;
        }

        AppCompatDelegate.setDefaultNightMode(targetMode);
        return true;
    }
}
