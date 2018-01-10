package com.com.jumptool;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.com.jumptools.R;

import java.io.PrintWriter;

public class JumpTools extends AccessibilityService{
    private static final String PACKAGE_NAME_TENCENT_MM = "com.tencent.mm";
    private static final int SERVICE_ID = 10001;
    private static final int MSG_EXCUTE_CMD = 1;
    private  static final String TAG = JumpTools.class.getSimpleName();
    HandlerThread mOperationThread;
    Handler mOperationHandler;
    Toast mToast;

    private OperationView mView;

    private class JumpHandler extends Handler{

        JumpHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_EXCUTE_CMD:
                    PrintWriter printWriter = null;
                    Process process = null;
                    try {
                        process = Runtime.getRuntime().exec("su");
                        printWriter = new PrintWriter(process.getOutputStream());
                        if (msg.obj != null){
                            printWriter.println(String.valueOf(msg.obj));
                        }
                        printWriter.flush();
                        printWriter.close();
                        int value = process.waitFor();
                        System.out.println("excute root cmd:" + msg.obj + "  with result: " + value);
                        mView.jumpDone();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (process != null) {
                            process.destroy();
                        }
                    }
                break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pkgName = event.getPackageName().toString();
        int eventType = event.getEventType();
        if(PACKAGE_NAME_TENCENT_MM.equalsIgnoreCase(pkgName)){
            switch (eventType){
                case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                    break;
            }
        }
    }

    @Override
    public void onInterrupt() {
        System.out.println("onInterrupt");
    }


    @Override
    protected void onServiceConnected() {
        System.out.println("onServiceConnected");
        super.onServiceConnected();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreate() {
        super.onCreate();
        Settings.getInstance().init(this);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(TAG).setSmallIcon(R.drawable.encroid_waiting);
        startForeground(SERVICE_ID, builder.build());
        mOperationThread = new HandlerThread(TAG);
        mOperationThread.start();
        mOperationHandler = new JumpHandler(mOperationThread.getLooper());
        OperationView view = mView = new OperationView(this);
        view.setOnJumpListener(new OperationView.OnJumpListener() {
            @Override
            public void onJump(OperationView view) {
                float rate = Settings.getInstance().getRate();
                System.out.print("rate: " + rate);
                int distance = (int) (view.getJumpDistance() / rate);
                if(distance > 0){
                    showToast("object: " + distance + " origin: " + (distance * rate));
                    mOperationHandler.sendMessage(Message.obtain(mOperationHandler, MSG_EXCUTE_CMD, "input swipe 30 30 30 30 " + distance));
                }
            }
            @Override
            public boolean shouldChangeStatus(OperationView view) {
                return true;
            }
        });
        mOperationHandler.sendEmptyMessage(MSG_EXCUTE_CMD);
    }

    private void showToast(Object value){
        CharSequence text = value + "";
        if(mToast == null){
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        }else {
            mToast.setText(text);
        }

        mToast.cancel();
        mToast.show();

        System.out.println(text);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mView != null){
            mView.detach();
        }

        if(mOperationHandler != null){
            mOperationHandler.removeCallbacksAndMessages(null);
        }

        if(mOperationThread != null){
            mOperationThread.quit();
        }
    }


    public void quit(){
        stopSelf();
    }
}
