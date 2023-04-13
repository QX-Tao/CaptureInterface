package com.android.captureinterface;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.android.captureinterface.service.ScreenShotService;
import com.android.captureinterface.utils.AppDialogManager;
import com.android.captureinterface.utils.PermissionManager;
import com.android.captureinterface.view.FloatingView;
public class MainActivity extends AppCompatActivity {

    // 自定义请求码
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 8;


    private TextView tv_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

    }

    /**
     * 初始化views
     */
    private void initViews() {
        tv_text = findViewById(R.id.textView);
//        bt_button = findViewById(R.id.button);

        // 申请悬浮窗及截图权限
        DialogInterface.OnClickListener negativeLister = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast toast = Toast.makeText(getApplicationContext(),"缺少必要权限，程序即将退出。",Toast.LENGTH_SHORT);
                toast.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 2000);
            }
        };
        DialogInterface.OnClickListener positiveLister = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 申请全局绘制的权限
                applyDrawingPermission();

                // 创建悬浮按钮并关联Activity
                setUpFloatingView();

                // 跳转开启无障碍
                checkAccessibility();

                tv_text.setText("开始收集啦！");
            }
        };
        AppDialogManager.getInstance().showCommonDialog(MainActivity.this,"申请权限","请允许使用悬浮窗并授予截图权限。","拒绝",negativeLister,"允许",positiveLister);



        // 进入应用，权限检查
        try {
            PermissionManager.permission(this, new PermissionManager.OnPermissionGrantCallback() {
                @Override
                public void onGranted() throws Exception {

                }

                @Override
                public void onDenied() throws Exception {
                    Toast toast = Toast.makeText(getApplicationContext(),"缺少必要权限，程序即将退出。",Toast.LENGTH_SHORT);
                    toast.show();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    }, 2000);
                }
            }, PermissionManager.STORAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 申请系统悬浮窗权限
     */
    private void applyDrawingPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast toast = Toast.makeText(getApplicationContext(),"请找到“界面信息收集”，并打开悬浮窗权限。",Toast.LENGTH_SHORT);
            toast.show();
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
        }
    }

    /**
     * 启动悬浮按钮
     */
    private void setUpFloatingView() {
        FloatingView floatingView = new FloatingView(this);
        floatingView.setActivity(this);
        floatingView.startFloatingView();
    }

    /**
     * 跳转设置开启无障碍
     */
    private void checkAccessibility(){
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }


    private static final int SCREENSHOT_REQUEST_CODE = 100;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 获取实时截图
        if (requestCode == SCREENSHOT_REQUEST_CODE && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, ScreenShotService.class);
            intent.putExtra("data", data);
            intent.putExtra("resultCode", resultCode);
            startService(intent);  //启动服务
        }
    }

    private void stopVirtualDisplay() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    private void stopMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }


    // 重写返回逻辑，使返回不退出。
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}