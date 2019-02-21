package com.example.sudokux;

import android.content.Context;
import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SudokuXOrc {

    private String TAG = "SudokuXOrc";

    private Context mContext;
    private TessTwoHelper mTessTwoHelper = new TessTwoHelper();

    private int[][] mShuDuArray = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
    };

    public SudokuXOrc(Context context) {
        mContext = context;
        //初始化图片转文字工具
        mTessTwoHelper.init();
    }

    /**
     * 输入：在全屏截图Bitmap。
     * 返回：返回原始数独数组
     */
    public int[][] getOriginShuDuArray(Bitmap bitmapSource) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat binaryMat = new Mat();

        bitmapSource = getSudokuPicBitmap(bitmapSource);
//        saveBitmap(bitmapSource,"origin");

        Utils.bitmapToMat(bitmapSource, rgbMat);
        //灰度化
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
//        Utils.matToBitmap(grayMat,bitmapSource);
//        saveBitmap(bitmapSource,"gray");
        //二值化
        Imgproc.threshold(grayMat, binaryMat, 100, 255, Imgproc.THRESH_BINARY);
//        Utils.matToBitmap(binaryMat,bitmapSource);
//        saveBitmap(bitmapSource,"binary");
        //寻找轮廓
        Imgproc.findContours(binaryMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        printContoursInfo(contours, hierarchy);

        ArrayList<Integer> tmp = new ArrayList<>();
        for (int i = 0; i < hierarchy.cols(); i++) {
            if (hierarchy.get(0, i)[3] == 1) {
                tmp.add(i);
            }
        }

        //将二值化后的图片转成Bitmap，方便数字识别
        Utils.matToBitmap(binaryMat, bitmapSource);
//        Imgproc.cvtColor(binaryMat, binaryMat, Imgproc.COLOR_GRAY2RGB);
        int width = (int) SPUtils.get(mContext, SudokuXUtils.SP_RECT_HEIGH, 0) / 9;
        for (int i = 0; i < tmp.size(); i++) {
            for (int j = 0; j < hierarchy.cols(); j++) {
                if (hierarchy.get(0, j)[3] == tmp.get(i)) {
                    Rect rect = Imgproc.boundingRect(contours.get(j));//检测外轮廓

                    //在图片中绘制轮廓
//                    Imgproc.rectangle(binaryMat, new Point(rect.x, rect.y), new Point(rect.x
//                            + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));

                    //根据轮廓截取数字图片，进行文字识别
                    Bitmap tmpBitmap = Bitmap.createBitmap(bitmapSource, rect.x, rect.y, rect.width, rect.height);
                    int number = mTessTwoHelper.getText(tmpBitmap).charAt(0) - '0';
                    saveBitmap(tmpBitmap, "bitmap" + rect.x + "" + rect.y + "tag:" + number);

                    //将数字填入空白数独，得到原始数独
                    mShuDuArray[rect.y / width][rect.x / width] = number;
                }
            }
        }
//        Utils.matToBitmap(binaryMat,bitmapSource);
//        saveBitmap(bitmapSource,"contours");
        return mShuDuArray;
    }

    /**
     * 输入：手机屏幕截图
     * 输出：数独面板的截图
     */
    private Bitmap getSudokuPicBitmap(Bitmap bitmapSource) {
        int x = (int) SPUtils.get(mContext, SudokuXUtils.SP_RECT_LEFT, 0);
        int y = (int) SPUtils.get(mContext, SudokuXUtils.SP_RECT_TOP, 0);
        int heigh = (int) SPUtils.get(mContext, SudokuXUtils.SP_RECT_HEIGH, 0);

        return Bitmap.createBitmap(bitmapSource, x, y, heigh, heigh);
    }

    /*保存图片，便于调试*/
    private void saveBitmap(Bitmap bitmap, String fileName) {
        File cacheDir = new File(SudokuXUtils.APP_CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        try {
            File file = new File(SudokuXUtils.APP_CACHE_DIR + fileName + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printContoursInfo(List<MatOfPoint> contours, Mat hierarchy) {
        for (int i = 0; i < contours.size(); i++) {
            double[] doubles = hierarchy.get(0, i);
            System.out.println("当前轮廓：" + i + " 前一轮廓：" + doubles[0] + " 后一轮廓：" + doubles[1] + " 子轮廓：" + doubles[2] + " 父轮廓：" + doubles[3]);
        }
    }
}
