package com.jscheng.scamera.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import com.jscheng.scamera.render.CameraSurfaceRender;

/**
 * Created By Chengjunsen on 2018/8/25
 */
public class CameraGLSurfaceView extends GLSurfaceView implements CameraSurfaceRender.CameraSurfaceRenderCallback {
    /* mCallback是像陈吉一样封装一次接口回调给外部，在合适的时候进行回调
       在本类内部实现GLSurfaceView.Renderer（CameraSurfaceRender implements GLSurfaceView.Renderer）。
     */
    private CameraSurfaceRender mRender;
    private CameraGLSurfaceViewCallback mCallback;
    private Context mContext;

    public CameraGLSurfaceView(Context context) {
        super(context, null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext =context;
        init(context);
    }

    private String getGlesVersion()
    {
        ActivityManager am =(ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return info.getGlEsVersion();
    }

    private void init(Context context) {
        String glesVersion = getGlesVersion();
        //版本不对可能导致eglCreateContext 共享上下文错误
        if (glesVersion.startsWith("4.")) {
            setEGLContextClientVersion(4);
        }else if (glesVersion.startsWith("3.")) {
            setEGLContextClientVersion(3);
        } else {
            setEGLContextClientVersion(2);
        }
        setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
        mRender = new CameraSurfaceRender(context);
        mRender.setCallback(this);
        this.setRenderer(mRender);
        this.setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mRender.getCameraSurfaceTexture();
    }

    //摄像头有可用数据
    @Override
    public void onRequestRender() {
        requestRender();
    }

    @Override
    public void onCreate() {
        if (mCallback != null) {
            mCallback.onSurfaceViewCreate(getSurfaceTexture());
        }
    }



    @Override
    public void onChanged(int width, int height) {
        if (mCallback != null) {
            mCallback.onSurfaceViewChange(width, height);
        }
    }

    @Override
    public void onDraw() {
    }

    public void setCallback(CameraGLSurfaceViewCallback mCallback) {
        this.mCallback = mCallback;
    }

    public void releaseSurfaceTexture() {
        mRender.releaseSurfaceTexture();
    }

    public void resumeSurfaceTexture() {
        mRender.resumeSurfaceTexture();
    }

    public void startRecord() {
        mRender.startRecord();
    }

    public void stopRecord() {
        mRender.stopRecord();
    }

    public interface CameraGLSurfaceViewCallback {
        void onSurfaceViewCreate(SurfaceTexture texture);
        void onSurfaceViewChange(int width, int height);
    }
}
