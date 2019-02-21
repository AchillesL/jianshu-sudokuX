package com.example.sudokux;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SudokuXService extends Service {

    private static final int MESSAGE_UNSOLVABLE = 0;
    private static final int MESSAGE_INIT_SUDOKU_FAIL = 1;
    private static final int MESSAGE_INIT_SOLVE_COMPLETE = 2;

    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    private Button mBtn;

    private LocalReceiver mLocalReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;
    private Handler mHandler;

    public SudokuXService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //设置悬浮窗参数并显示
        mParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);

        initView();
        initBroadcast();
        Toast.makeText(SudokuXService.this, "SudokuX 已开启", Toast.LENGTH_SHORT).show();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MESSAGE_INIT_SUDOKU_FAIL: {
                        Toast.makeText(SudokuXService.this, "获取数独面板信息失败!!!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case MESSAGE_UNSOLVABLE: {
                        Toast.makeText(SudokuXService.this, "无解!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case MESSAGE_INIT_SOLVE_COMPLETE: {
                        Toast.makeText(SudokuXService.this, "正在填入数据，求解耗时:" + msg.arg1 * 1.0 / 1000 + "秒", Toast.
                                LENGTH_LONG).show();
                        try2FillingSudoku((ArrayList<LocTextInfo>) msg.obj);
                        break;
                    }
                }
                mBtn.setEnabled(true);
            }
        };
    }

    private void initView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        mParams.gravity = Gravity.START | Gravity.TOP;
        mParams.x = SudokuXUtils.getScreenWidth();
        mParams.y = SudokuXUtils.getScreenHeight();

        mParams.width = SudokuXUtils.SMALL_SIZE_WIDTH;
        mParams.height = SudokuXUtils.SMALL_SIZE_HIGH;

        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(getApplication()).inflate(R.layout.layout, null);
        mBtn = linearLayout.findViewById(R.id.btn);
        mWindowManager.addView(linearLayout, mParams);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtn.setEnabled(false);
                getScreenshotBitmap();
            }
        });
    }

    private void initBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SudokuXUtils.ACTION_SCREEN_SHOT_FINISH);
        intentFilter.addAction(SudokuXUtils.ACTION_FILLING_COMPLETE);

        mLocalReceiver = new LocalReceiver();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mLocalReceiver, intentFilter);
    }

    private void getScreenshotBitmap() {
        Intent intent = new Intent(SudokuXUtils.ACTION_START_SCREEN_SHOT);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == SudokuXUtils.ACTION_SCREEN_SHOT_FINISH) {
                //截图完成
                final Bitmap bitmap = intent.getParcelableExtra(SudokuXUtils.INTENT_SCREEN_SHOT);
                /*开启子线程用于计算*/
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        analyseSudoku(bitmap);
                    }
                }).start();
            } else if (intent.getAction() == SudokuXUtils.ACTION_FILLING_COMPLETE) {
                //填充数独完成
                mBtn.setEnabled(true);
            }
        }
    }

    private void analyseSudoku(Bitmap bitmap) {
        Message message = new Message();
        boolean isAccessibilityInit = (boolean) SPUtils.get(SudokuXService.this, SudokuXUtils.SP_INIT, false);
        if (!isAccessibilityInit) {
            message.what = MESSAGE_INIT_SUDOKU_FAIL;
            mHandler.sendMessage(message);
            return;
        }
        long time1 = System.currentTimeMillis();
        SudokuXOrc sudokuXOrc = new SudokuXOrc(SudokuXService.this);
        //还未解决的数独
        int[][] shuDuOrigin = sudokuXOrc.getOriginShuDuArray(bitmap);
        //已解决的数独
        int[][] shuDuResult = null;
        try {
            shuDuResult = new SudokuXAnalyse(shuDuOrigin).getAns();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long time2 = System.currentTimeMillis();

        if (shuDuResult == null) {
            message.what = MESSAGE_UNSOLVABLE;
            mHandler.sendMessage(message);
            return;
        }

        message.what = MESSAGE_INIT_SOLVE_COMPLETE;
        message.arg1 = (int) (time2 - time1);
        message.obj = getFillingList(shuDuOrigin, shuDuResult);
        mHandler.sendMessage(message);
    }

    /*
    输入：原始的数独信息、求解后的数组信息。
    返回: 得到哪些位置需要填入数字（按数字1-9顺序）,结果存放在ArrayList<LocTextInfo>中。
    */
    private ArrayList<LocTextInfo> getFillingList(int[][] shuDuOrigin, int[][] shuDuResult) {
        ArrayList<LocTextInfo> locTextInfos = new ArrayList<>();
        List<List<LocTextInfo>> tmpList = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            List<LocTextInfo> tmp = new ArrayList<>();
            tmpList.add(tmp);
        }
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (shuDuOrigin[i][j] == 0) {
                    int number = shuDuResult[i][j];
                    tmpList.get(number - 1).add(new LocTextInfo(i, j, number));
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < tmpList.get(i).size(); j++) {
                locTextInfos.add(tmpList.get(i).get(j));
            }
        }

        return locTextInfos;
    }

    private void try2FillingSudoku(ArrayList<LocTextInfo> locTextInfos) {
        Intent intent = new Intent(SudokuXUtils.ACTION_FILLING_START);
        intent.putParcelableArrayListExtra("data", locTextInfos);
        mLocalBroadcastManager.sendBroadcast(intent);
    }
}
