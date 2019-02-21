package com.example.sudokux;

import android.content.res.Resources;

/**
 * Created by Achilles on 2017/12/31.
 */

public class SudokuXUtils {

    /*截图相关action*/
    public static final String ACTION_START_SCREEN_SHOT = "ACTION_START_SCREEN_SHOT";
    public static final String ACTION_SCREEN_SHOT_FINISH = "ACTION_SCREEN_SHOT_FINISH";

    /*填充数独相关action*/
    public static final String ACTION_FILLING_START = "ACTION_FILLING_START";
    public static final String ACTION_FILLING_COMPLETE = "ACTION_FILLING_COMPLETE";

    public static final String INTENT_SCREEN_SHOT = "INTENT_SCREEN_SHOT";

    public static final String SP_INIT = "SP_INIT";
    public static final String SP_RECT_LEFT = "SP_RECT_LEFT";
    public static final String SP_RECT_TOP = "SP_RECT_TOP";
    public static final String SP_RECT_HEIGH = "SP_RECT_HEIGH";

    public static final int SMALL_SIZE_WIDTH = 280;
    public static final int SMALL_SIZE_HIGH = 150;

    public static final String APP_CACHE_DIR = "/storage/emulated/0/sudokuX/" ;

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
}
