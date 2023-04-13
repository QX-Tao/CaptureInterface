package com.android.captureinterface.view;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.android.captureinterface.constant.ScreenShotConstant.DUMP_VIEW_TREE_TAG;
import static com.android.captureinterface.constant.ScreenShotConstant.SCREEN_SHOT_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.example.myapplicationtest.R;
import com.android.captureinterface.concurrent.ChannelFactory;
import com.android.captureinterface.socket.ClientSocket;
import com.android.captureinterface.utils.currentClickUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 悬浮按钮的视图
 */
public class FloatingView implements View.OnTouchListener, View.OnClickListener {


    // 截图请求码
    private static final int SCREENSHOT_REQUEST_CODE = 100;

    private final Context context;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams layoutParams;

    // 按钮的坐标消息
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    // 是否移动
    private boolean isMove;

    private MediaProjectionManager mediaProjectionManager;


    public FloatingView(Context context) {
        this.context = context;
    }

    public void setActivity(Activity activity) {
        Window window = activity.getWindow();
        windowManager = activity.getWindowManager();
    }

    public void startFloatingView() {
        // 1. 创建用于展示悬浮按钮的 view
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_widget, null);

        // 2. 设置view的位置和大小
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = 100;

        // 3. 添加触摸监听器，实现按钮拖动
        floatingView.setOnTouchListener(this);

        // 4. 添加点击监听器，实现点击事件
        floatingView.setOnClickListener(this);

        // 5. 实例化MediaProjectionManager,开启截屏意图
        mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
        ((Activity) context).startActivityForResult(screenCaptureIntent, SCREENSHOT_REQUEST_CODE);

        // 6. 将 View 添加到 window 中
        windowManager.addView(floatingView, layoutParams);

        // 创建文件夹
        File filePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        newDirectory(filePath.toString(), context.getResources().getString(R.string.app_name));
    }

    public void stopFloatingView() {
        windowManager.removeView(floatingView);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //获取手指相对于悬浮窗口的坐标和悬浮窗口的初始位置
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isMove = false;
                break;
            case MotionEvent.ACTION_MOVE:
                // 根据手指的位置更新悬浮窗口的位置
                layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                // 更新悬浮窗口的位置
                windowManager.updateViewLayout(floatingView, layoutParams);
                isMove = true;
                break;
        }
        return isMove;
    }

    private final Handler handler = new Handler();

    @Override
    public void onClick(View view) {

        File filePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        String clickFilePath = filePath + File.separator + context.getResources().getString(R.string.app_name);
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
        String dateStr = dateformat.format(System.currentTimeMillis());
        newDirectory(clickFilePath, dateStr);
        currentClickUtil.setClickFilePath(clickFilePath + "/" + dateStr);


        CountDownLatch countDownLatch = new CountDownLatch(3);
        // 隐藏按钮
        floatingView.setVisibility(View.INVISIBLE);
        Executor executor = Executors.newFixedThreadPool(4); // 使用单独的线程池，以避免阻塞主UI线程
        executor.execute(() -> {
            boolean end = ChannelFactory.getEndScreenShot().receive();
            if (end) {
                Log.d(SCREEN_SHOT_TAG, "receive end screenshot");
                countDownLatch.countDown();
            }
        });
        executor.execute(() -> {
            boolean end = ChannelFactory.getEndDumpViewTree().receive();
            if (end) {
                Log.d(DUMP_VIEW_TREE_TAG, "receive end dumpViewTree");
                countDownLatch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                countDownLatch.await();
                floatingView.post(() -> floatingView.setVisibility(View.VISIBLE));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


        Log.d(SCREEN_SHOT_TAG, "send start screenshot");
        // 通知开启截屏
        ChannelFactory.getStartScreenShot().send(true);
        // 通知无障碍收集控件树
        Log.d(DUMP_VIEW_TREE_TAG, "send start dumpViewTree");
        ChannelFactory.getStartDumpViewTree().send(true);


        Thread thread1 = new Thread(() -> {
            try {
                ClientSocket c = new ClientSocket("10.161.186.123", 9000);
                c.send("开始收集",countDownLatch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread1.start();

    }

    /**
     * 创建文件夹
     */
    public void newDirectory(String _path, String dirName) {
        File file = new File(_path + "/" + dirName);
        try {
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

