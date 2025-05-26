package hev.sockstun;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private ImageView buttonView;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.view_floating_button, null);
        buttonView = floatingView.findViewById(R.id.floating_button);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.x = 50;  // 距离右边缘的偏移
        params.y = 400; // 距离底部的偏移

        // 初始化 UI 状态
        updateButtonUI();

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long downTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTime = System.currentTimeMillis();
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        long clickDuration = System.currentTimeMillis() - downTime;
                        if (clickDuration < 200) {
                            toggleVpn();
                        } else if (clickDuration > 800) {
                            stopSelf(); // 长按关闭悬浮窗
                            Toast.makeText(FloatingService.this, "已关闭悬浮窗", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingView, params);
    }

    private void toggleVpn() {
        Preferences prefs = new Preferences(this);
        boolean isEnable = prefs.getEnable();
        boolean newState = !isEnable;

        prefs.setEnable(newState); // 更新状态
        Intent intent = new Intent(this, TProxyService.class);
        intent.setAction(newState ? TProxyService.ACTION_CONNECT : TProxyService.ACTION_DISCONNECT);
        startService(intent);

//        Toast.makeText(this, newState ? "已启动 VPN" : "已停止 VPN", Toast.LENGTH_SHORT).show();
        updateButtonUI();
    }

    public void updateButtonUI() {
        Preferences prefs = new Preferences(this);
        boolean isEnable = prefs.getEnable();
        if (buttonView != null) {
            buttonView.setImageResource(isEnable ? android.R.drawable.presence_online : android.R.drawable.presence_offline);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
