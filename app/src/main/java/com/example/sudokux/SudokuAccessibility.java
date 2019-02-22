package com.example.sudokux;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SudokuAccessibility extends AccessibilityService {
    private static final String TAG = "SudokuAccessibility";

    private LocalReceiver mLocalReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;

    private List<LocTextInfo> mLocTextInfos;
    private List<Point> mTypeNumberPointList = new ArrayList<>(9);
    private List<List<Point>> mShuDuPanelPointList = new ArrayList<>(9);

    private boolean mInitDataFlag = false;

    private Handler mHandler = new Handler(new Handler.Callback() {
        int i = 0;
        /**
         * 设置tag可以实现轮流按下数独面板和选择区按钮，
         * 同时配合变量@param fillingFlag，实现避免某些区域点击失效的情况。
         * */
        boolean tag = true;

        @Override
        public boolean handleMessage(Message msg) {
            if (i < mLocTextInfos.size()) {
                LocTextInfo locTextInfo = mLocTextInfos.get(i);
                if (tag) {
                    Point numberPoint = mShuDuPanelPointList.get(locTextInfo.locX).get(locTextInfo.locY);
                    dispatchGestureView(0, numberPoint.x, numberPoint.y);
                } else {
                    Point typeNumberPoint = mTypeNumberPointList.get(locTextInfo.number - 1);
                    dispatchGestureView(0, typeNumberPoint.x, typeNumberPoint.y);
                    i++;
                }
                tag = !tag;
                mHandler.sendEmptyMessageDelayed(0, 10);
            } else {
                i = 0;
                tag = true;
                mHandler.removeCallbacksAndMessages(null);
                mLocalBroadcastManager.sendBroadcast(new Intent(SudokuXUtils.ACTION_FILLING_COMPLETE));
            }
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SudokuXUtils.ACTION_FILLING_START);
        intentFilter.addAction(SudokuXUtils.ACTION_FILLING_COMPLETE);

        mLocalReceiver = new LocalReceiver();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mLocalReceiver, intentFilter);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent: " + event.toString());

        if (!mInitDataFlag) {
            initViewData(event);
        }
    }

    private void initViewData(AccessibilityEvent event) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        //初始化等待区数字1-9的中心位置
        for (int i = 0; i < 9; i++) {
            String id = String.format("com.easybrain.sudoku.android:id/button_%d", i + 1);
            List<AccessibilityNodeInfo> nodeInfos = root.findAccessibilityNodeInfosByViewId(id);
            if (!nodeInfos.isEmpty()) {
                Rect rect = new Rect();
                nodeInfos.get(0).getBoundsInScreen(rect);
                Point point = new Point(rect.centerX(), rect.centerY());
                mTypeNumberPointList.add(point);
            }
        }

        //生成数独面板81个格子的中心位置
        String id = String.format("com.easybrain.sudoku.android:id/sudoku_board");
        List<AccessibilityNodeInfo> nodeInfos = root.findAccessibilityNodeInfosByViewId(id);
        if (!nodeInfos.isEmpty()) {
            Rect rect = new Rect();
            nodeInfos.get(0).getBoundsInScreen(rect);

            int step = (rect.bottom - rect.top) / 9;
            //计算81格中，第一个格子的中心点
            int x = rect.left + step / 2;
            int y = rect.top + step / 2;

            /*保存数独面板的左上角顶点、高度信息，便于截图分析数独面板数字是使用。*/
            saveSudokuBroadInfo(rect);

            for (int i = 0; i < 9; i++) {
                List<Point> points = new ArrayList<>(9);
                for (int j = 0; j < 9; j++) {
                    Point point = new Point(x + step * j, y + step * i);
                    points.add(point);
                }
                mShuDuPanelPointList.add(points);
            }
        }

        if (mShuDuPanelPointList.size() == 9 && mTypeNumberPointList.size() == 9) {
            mInitDataFlag = true;
            Toast.makeText(this, "数独信息获取成功!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSudokuBroadInfo(Rect rect) {
        SPUtils.put(SudokuAccessibility.this, SudokuXUtils.SP_INIT, true);
        SPUtils.put(SudokuAccessibility.this, SudokuXUtils.SP_RECT_LEFT, rect.left - 5);
        SPUtils.put(SudokuAccessibility.this, SudokuXUtils.SP_RECT_TOP, rect.top - 5);
        SPUtils.put(SudokuAccessibility.this, SudokuXUtils.SP_RECT_HEIGH, rect.bottom - rect.top + 10);
    }

    @Override
    public void onInterrupt() {

    }

    public void dispatchGestureView(int startTime, int x, int y) {
        Point position = new Point(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path p = new Path();
        p.moveTo(position.x, position.y);
        /**
         * StrokeDescription参数：
         * path：笔画路径
         * startTime：时间 (以毫秒为单位)，从手势开始到开始笔划的时间，非负数
         * duration：笔划经过路径的持续时间(以毫秒为单位)，非负数*/
        builder.addStroke(new GestureDescription.StrokeDescription(p, startTime, 1));
        dispatchGesture(builder.build(), null, null);
    }

    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == SudokuXUtils.ACTION_FILLING_START) {
                mLocTextInfos = intent.getParcelableArrayListExtra("data");
                //开始填充数字
                mHandler.sendEmptyMessage(0);
            }
        }
    }

}
