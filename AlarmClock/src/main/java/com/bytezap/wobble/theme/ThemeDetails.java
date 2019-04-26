package com.bytezap.wobble.theme;

import android.graphics.Color;

public class ThemeDetails {

    //Themes
    public static final int THEME_RAINY_DAY = 0;
    public static final int THEME_MOUNTAINS = 1;
    public static final int THEME_SUNRISE = 2;
    public static final int THEME_SHIMMERING_NIGHT = 3;
    public static final int THEME_AURORA = 4;
    public static final int THEME_BEACH = 5;
    public static final int THEME_BLUE_NIGHT = 6;
    public static final int THEME_FOGGY_FOREST = 7;
    public static final int THEME_DARK_COSMOS = 8;
    public static final int THEME_OF_TIME = 9;
    public static final int THEME_CYAN = 10;
    public static final int THEME_THISTLE_PURPLE = 11;
    public static final int THEME_CUSTOM = 12;
    public static final int THEME_RANDOM = 13;

    public static final int THEME_DEFAULT = 0;

    //Patterns
    public static final int PATTERN_RED = 0;
    public static final int PATTERN_BLUE = 1;
    public static final int PATTERN_BLACK = 2;
    public static final int PATTERN_GREEN = 3;
    public static final int PATTERN_GRID_RED = 4;
    public static final int PATTERN_GRID_GREEN = 5;
    public static final int PATTERN_GRID_BROWN = 6;
    public static final int TOTAL_THEMES = 12;

    public static int getThemeAccent(int tNumber){
        switch (tNumber){
            case THEME_RAINY_DAY:
                return Color.parseColor("#464a6c");

            case THEME_BLUE_NIGHT:
                return Color.parseColor("#039BE5");

            case THEME_FOGGY_FOREST:
                return Color.parseColor("#009688");

            case THEME_AURORA:
            case THEME_BEACH:
            case THEME_CYAN:
                return Color.parseColor("#1DE9B6");

            case THEME_SHIMMERING_NIGHT:
                return Color.parseColor("#023447");

            case THEME_DARK_COSMOS:
            case THEME_OF_TIME:
                return Color.parseColor("#212121");

            case THEME_SUNRISE:
            case THEME_MOUNTAINS:
                return Color.parseColor("#3F51B5");

            case THEME_THISTLE_PURPLE:
                return Color.parseColor("#F06292");

            case THEME_CUSTOM:
                return Color.parseColor("#009688");

            default:
                return Color.parseColor("#009688");
        }
    }

    public static int getThemeRadiance(int tNumber){
        switch (tNumber){
            case THEME_BLUE_NIGHT:
                return Color.parseColor("#42A5F5");

            case THEME_FOGGY_FOREST:
                return Color.parseColor("#A5D6A7");

            case THEME_AURORA:
            case THEME_BEACH:
            case THEME_CYAN:
                return Color.parseColor("#D7CCC8");

            case THEME_SHIMMERING_NIGHT:
                return Color.parseColor("#42A5F5");

            case THEME_RAINY_DAY:
            case THEME_MOUNTAINS:
                return Color.parseColor("#C5CAE9");

            case THEME_OF_TIME:
                return Color.parseColor("#9E9E9E");

            case THEME_DARK_COSMOS:
                return Color.parseColor("#757575");

            case THEME_SUNRISE:
                return Color.parseColor("#D1C4E9");

            case THEME_THISTLE_PURPLE:
                return Color.parseColor("#D1C4E9");

            case THEME_CUSTOM:
                return Color.parseColor("#BDBDBD");

            default:
                return Color.parseColor("#BDBDBD");
        }
    }

    /**
     * @param tNumber
     * @return
     */
    // For evaluating if the theme chosen has a darker shade
    public static boolean isThemeDark(int tNumber){
        return tNumber == THEME_DARK_COSMOS || tNumber == THEME_OF_TIME;
    }

    /**
     * @param tNumber
     * @return
     */
    // For evaluating if the theme chosen has a darker shade
    public static boolean isToastDark(int tNumber){
        return tNumber == THEME_BEACH || tNumber == THEME_MOUNTAINS || tNumber == THEME_THISTLE_PURPLE || tNumber == THEME_CYAN;
    }

    /**
     * @param tNumber
     * @return
     */
    public static boolean isAlarmBgWhite(int tNumber){
        return tNumber == THEME_MOUNTAINS || tNumber == THEME_AURORA || tNumber == THEME_RAINY_DAY || tNumber == THEME_SUNRISE || tNumber == THEME_BEACH || tNumber == THEME_CUSTOM;
    }

    public static boolean shouldMaskTheme(int tNumber){
        return tNumber == THEME_AURORA || tNumber == THEME_MOUNTAINS || tNumber == THEME_BEACH || tNumber == THEME_CUSTOM;
    }

}
