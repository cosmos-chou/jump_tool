package com.com.jumptool;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.filterfw.geometry.Point;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.com.jumptools.R;

import java.util.ArrayList;
import java.util.List;


public class OperationView extends View {

    private static final long BACK_TO_FULL_SCREEN_DELAYED = 5000;

    private List<Point> mPointList = new ArrayList<>();
    enum Status{
        FULLSCREEN,
        FADED,
        CLOSED
    }
    private Status mStatus;

    private float mLastTouchX;
    private float mLastTouchY;
    private OnJumpListener mOnJumpListener;

    private WindowManager.LayoutParams mLayoutParams;

    private ViewGroup mParent;

    private float scale = 1;

    private Vibrator mVibrateManager;

    public OperationView(Context context) {
        super(context);
        init();
    }

    public OperationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OperationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OperationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void setOnClickListener(final OnClickListener l) {
        throw new RuntimeException("can not setOnClickListener for this view");
    }


    public void detach(){
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        try{
            wm.removeView(mParent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public float getJumpDistance(){
        int size = mPointList.size();
        if(size < 2){
            return 0;
        }
        return mPointList.get(size -1).distanceTo(mPointList.get(size -2)) / scale;

    }

    public void setOnJumpListener(OnJumpListener listener){
        mOnJumpListener = listener;
    }

    private void init(){
        mVibrateManager = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        mParent = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.operation_container, null);
        mParent.addView(this, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mParent.findViewById(R.id.begin).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPointList.clear();
                changeStatusManul(Status.FULLSCREEN);
            }
        });

        mParent.findViewById(R.id.close).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeStatusManul(Status.CLOSED);
            }
        });

        mParent.findViewById(R.id.close).setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                changeStatusManul(Status.CLOSED);
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
                return true;
            }
        });

        mParent.findViewById(R.id.quit).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getContext() instanceof  JumpTools){
                    ((JumpTools) getContext()).quit();
                }
            }
        });

        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Settings.getInstance().isVibrateEnable()){
                    mVibrateManager.vibrate(200);
                }
                mPointList.add(mPointList.size(), new Point(mLastTouchX, mLastTouchY));
                if (mPointList.size() > 1) {
                    if (mOnJumpListener != null) {
                        if (mOnJumpListener.shouldChangeStatus(OperationView.this)) {
                            changeStatus();
                        }
                        mOnJumpListener.onJump(OperationView.this);
                    }
                    mPointList.clear();
                }
            }
        });
        scale = getContext().getResources().getDisplayMetrics().density;
        if (scale == 0){
            scale = 1;
        }
        changeStatus();
    }

    private void changeStatusManul(Status status){
        if(mStatus != status && status != null){
            mStatus = status;
            syncView();
        }
    }

    private void changeStatus(){
        Status old = mStatus;
        if(mStatus == null){
            mStatus = Status.CLOSED;
        }else{
            switch(mStatus){
                case FULLSCREEN:
                    mStatus = Status.FADED;
                    break;
                case FADED:
                    mStatus = Status.FULLSCREEN;
                    break;
                default:
                    break;
            }
        }

        if(mStatus != old){
            syncView();
        }

    }

    @TargetApi(Build.VERSION_CODES.O)
    private void syncView(){
        removeCallbacks(mBackToFullScreenRunnable);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :  WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        switch(mStatus){
            case FULLSCREEN:
                mLayoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,type , WindowManager.LayoutParams.FLAG_FULLSCREEN , PixelFormat.TRANSPARENT);
                setVisibility(VISIBLE);
                break;
            case FADED:
                mLayoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,  type, 0 , PixelFormat.TRANSPARENT);
                mLayoutParams.flags |= (WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN);
                postDelayed(mBackToFullScreenRunnable, BACK_TO_FULL_SCREEN_DELAYED);
                setVisibility(VISIBLE);
                break;
            case CLOSED:
                mLayoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_FULLSCREEN , PixelFormat.TRANSPARENT);
                mLayoutParams.flags |= (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN);
                mLayoutParams.gravity = Gravity.TOP;
                setVisibility(GONE);
                break;
            default:
                break;
        }

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if(isAttachedToWindow()){
                wm.updateViewLayout(mParent, mLayoutParams);
            }else{
                wm.addView(mParent, mLayoutParams);
            }
        }
    }



    Runnable mBackToFullScreenRunnable = new Runnable() {
        @Override
        public void run() {
            changeStatus();
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mStatus == Status.FADED){
            return false;
        }
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }


    public interface OnJumpListener{
        void onJump(OperationView view);

        boolean shouldChangeStatus(OperationView view);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(getColor(), PorterDuff.Mode.SRC);
    }

    private int getColor(){
        return mStatus == Status.FULLSCREEN ? 0x90000000 : 0x90FF0000;
    }

    public void jumpDone(){
        post(mBackToFullScreenRunnable);
    }

}
