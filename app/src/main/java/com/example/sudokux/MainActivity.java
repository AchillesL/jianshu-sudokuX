package com.example.sudokux;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.osgi.OpenCVNativeLoader;

import java.io.File;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private Button mStartBtn;
    private static int REQUEST_MEDIA_PROJECTION = 0;
    private static int REQUEST_OTHER_PERMISSION = 1;

    private LocalReceiver mLocalReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initOpenCv();
        initBroadcast();
        initView();
    }

    private void initTessTwoFile() {
        File file = new File(TessTwoHelper.DATA_DIR_PATH + File.separator + TessTwoHelper.DATA_NAME);
        if (!file.exists()) {
            File dir = new File(TessTwoHelper.DATA_DIR_PATH);
            if (!dir.exists()) {
                dir.mkdir();
            }
            /*将TessTwo所需的数据文件复制到SD卡*/
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileStorageHelper.copyFilesFromAssets(MainActivity.this, TessTwoHelper.DATA_NAME, TessTwoHelper.DATA_DIR_PATH);
                }
            }).start();
        }
    }

    private void initOpenCv() {
        OpenCVNativeLoader loader = new OpenCVNativeLoader();
        loader.init();
    }

    private void initBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SudokuXUtils.ACTION_START_SCREEN_SHOT);

        mLocalReceiver = new LocalReceiver();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mLocalReceiver, intentFilter);
    }

    private void initView() {
        mStartBtn = findViewById(R.id.startBtn);
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (openAccessibility(MainActivity.this) && startOverLay() && checkReadWritePermission(MainActivity.this)) {
                    initTessTwoFile();
                    mStartBtn.setEnabled(false);
                    mStartBtn.setVisibility(View.INVISIBLE);
                    moveTaskToBack(true);

                    Intent intent = new Intent(MainActivity.this, SudokuXService.class);
                    startService(intent);
                }
            }
        });
    }

    public static boolean checkReadWritePermission(AppCompatActivity activity) {
        boolean isGranted = true;
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            isGranted = false;
        }
        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            isGranted = false;
        }
        if (!isGranted) {
            activity.requestPermissions(
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    },
                    REQUEST_OTHER_PERMISSION);
        }
        return isGranted;
    }

    private boolean startOverLay() {
        if (!Settings.canDrawOverlays(MainActivity.this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            Toast.makeText(this, "需要取得权限以使用悬浮窗", Toast.LENGTH_SHORT).show();
            startActivity(intent);
            return false;
        }
        return true;
    }

    private void try2StartScreenShot() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            moveTaskToBack(true);
            if (resultCode == RESULT_OK && data != null) {
                ScreenShotHelper screenShotHelper = new ScreenShotHelper(MainActivity.this, resultCode, data, new ScreenShotHelper.OnScreenShotListener() {
                    @Override
                    public void onFinish(Bitmap bitmap) {
                        Intent intent = new Intent(SudokuXUtils.ACTION_SCREEN_SHOT_FINISH);
                        intent.putExtra(SudokuXUtils.INTENT_SCREEN_SHOT, bitmap);
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }
                });
                screenShotHelper.startScreenShot();
            } else {
                mLocalBroadcastManager.sendBroadcast(new Intent(SudokuXUtils.ACTION_FILLING_COMPLETE));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_OTHER_PERMISSION) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PERMISSION_GRANTED) {
                    Toast.makeText(this, "需要取得权限以读写文件", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                    startActivity(intent);
                    break;
                }
            }
            finish();
        }
    }

    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == SudokuXUtils.ACTION_START_SCREEN_SHOT) {
                try2StartScreenShot();
            }
        }
    }

    /**
     * 该辅助功能开关是否打开
     */
    private boolean isAccessibilitySettingsOn(String serviceName, Context mContext) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(serviceName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 跳转到系统设置页面开启辅助功能
     */
    private boolean openAccessibility(Context context) {
        final String serviceName = getPackageName() + "/" + SudokuAccessibility.class.getCanonicalName();
        if (!isAccessibilitySettingsOn(serviceName, context)) {
            Toast.makeText(MainActivity.this, "需要取得权限以使用辅助服务", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            return false;
        }
        return true;
    }
}
