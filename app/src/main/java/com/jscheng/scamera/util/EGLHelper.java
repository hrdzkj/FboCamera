package com.jscheng.scamera.util;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/9/20
 */
public class EGLHelper {
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY; //EGLDisplay 是一个关联系统物理屏幕的通用数据类型
    private EGLConfig mEglConfig;
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;


    public void createGL(EGLContext eglContext) {
        // 设置显示设备
        setDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        // 设置属性
        setConfig();
        // 创建上下文
        createContext(eglContext);
    }

    /**
     * 设置显示设备
     */
    public void setDisplay(int key) {
        // 获取显示默认设备

        mEglDisplay = EGL14.eglGetDisplay(key);
        // 初始化
        int version[] = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("EGL error" + EGL14.eglGetError());
        }
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_VENDOR));
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_VERSION));
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_EXTENSIONS));
    }

    public void setConfig() {
        int configAttribs[] = {
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,      // 渲染类型
                EGL10.EGL_RED_SIZE, 8,  // 指定 RGB 中的 R 大小（bits）
                EGL10.EGL_GREEN_SIZE, 8, // 指定 G 大小
                EGL10.EGL_BLUE_SIZE, 8,  // 指定 B 大小
                EGL10.EGL_ALPHA_SIZE, 8, // 指定 Alpha 大小
                EGL10.EGL_DEPTH_SIZE, 8, // 指定深度(Z Buffer) 大小
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, // 指定渲染 api 类别,
                EGL10.EGL_NONE
        };
        setConfig(configAttribs);
    }

    public void setConfig(int configAttribs[]) {
        int numConfigs[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, configAttribs, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException("EGL error " + EGL14.eglGetError());
        }
        mEglConfig = configs[0];
    }

    public void createContext(EGLContext context) {
        // 创建openGL上下文
        int contextAttribs[] = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
        };
        mEglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, context, contextAttribs, 0);
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("EGL error " + EGL14.eglGetError());
        }
    }

    public void destroyGL() {
        EGL14.eglDestroyContext(mEglDisplay, mEglContext);
        mEglContext = EGL14.EGL_NO_CONTEXT;
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
    }

    public EGLSurface createWindowSurface(EGLConfig config, Object surface) {
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, config, surface, new int[]{EGL14.EGL_NONE}, 0);
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.d(TAG, "createWindowSurface" + EGL14.eglGetError());
            return null;
        }
        return eglSurface;
    }

    public EGLSurface createWindowSurface(Object surface) {
       return createWindowSurface(mEglConfig, surface);
    }

    public EGLSurface createPbufferSurface(EGLConfig config, int width, int height) {
        return EGL14.eglCreatePbufferSurface(mEglDisplay, config, new int[]{EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE}, 0);
    }

    //
    public boolean makeCurrent(EGLSurface draw, EGLSurface read, EGLContext context) {
        /**
         * eglMakeCurrent把context绑定到当前的渲染线程以及draw和read指定的Surface。
         * draw用于数据回读(glReadPixels、glCopyTexImage2D和glCopyTexSubImage2D)之外的所有GL操作。
         * 回读操作作用于read指定的Surface上的帧缓冲(frame buffer)。
         *
         * 因此，当我们在线程T上调用GL 指令，OpenGL ES 会查询T线程绑定是哪个Context C，
         * 进而查询是哪个Surface draw和哪个Surface read绑定到了这个Context C上。
         *
         * https://juejin.im/post/58dca46b61ff4b006b03bd57
         */
        if (!EGL14.eglMakeCurrent(mEglDisplay, draw, read, context)) {
            Log.d(TAG, "makeCurrent" + EGL14.eglGetError());
            return false;
        }
        return true;
    }

    public boolean makeCurrent(EGLSurface surface, EGLContext context) {
        return makeCurrent(surface, surface, context); // 从这里体现共享surface????
    }

    public boolean makeCurrent(EGLSurface surface) {
        return makeCurrent(surface, mEglContext);
    }

    public boolean setPresentationTime(EGLSurface surface, long timeStamp) {
        if (!EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, timeStamp)) {
            Log.d(TAG, "setPresentationTime" + EGL14.eglGetError());
            return false;
        }
        return true;
    }

    public boolean swapBuffers(EGLSurface surface) {
        //eglSwapBuffers performs an implicit flush operation on the context
        if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
            Log.d(TAG, "swapBuffers" + EGL14.eglGetError());
            return false;
        }
        return true;
    }

    public boolean destroyGL(EGLSurface surface, EGLContext context) {
        EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        if (surface != null) {
            EGL14.eglDestroySurface(mEglDisplay, surface);
        }
        if (context != null) {
            EGL14.eglDestroyContext(mEglDisplay, context);
        }
        EGL14.eglTerminate(mEglDisplay);
        return true;
    }

    public void destroySurface(EGLSurface surface) {
        EGL14.eglDestroySurface(mEglDisplay, surface);
    }

}
