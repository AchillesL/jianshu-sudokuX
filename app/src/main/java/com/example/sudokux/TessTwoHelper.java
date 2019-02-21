package com.example.sudokux;

import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

public class TessTwoHelper {

    public static final String DATA_DIR_PATH = "/storage/emulated/0/tessdata";
    public static final String DATA_NAME = "eng.traineddata";
    private TessBaseAPI tessBaseAPI = new TessBaseAPI();

    public void init() {
        tessBaseAPI.init("/storage/emulated/0/", "eng");
        tessBaseAPI.setDebug(true);
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "123456789");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST,
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0!@#$%^&*()_+=-[]}{;:'\"\\|~`,./<>?");
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT);
    }

    public String getText(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        return tessBaseAPI.getUTF8Text();
    }
}
