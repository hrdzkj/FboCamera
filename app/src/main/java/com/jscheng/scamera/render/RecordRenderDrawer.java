package com.jscheng.scamera.render;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.jscheng.scamera.record.VideoEncoder;
import com.jscheng.scamera.util.EGLHelper;
import com.jscheng.scamera.util.GlesUtil;
import com.jscheng.scamera.util.StorageUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/9/21
  *   输出纹理/输入纹理同一个，外部传入的。
 */
public class RecordRenderDrawer extends BaseRenderDrawer implements Runnable{
    private SimpleDateFormat mFormatter = new SimpleDateFormat("HHmmss");

    // 绘制的纹理 ID
    private int mTextureId;
    private VideoEncoder mVideoEncoder;
    private String mVideoPath;
    private Handler mMsgHandler;
    private EGLHelper mEglHelper;
    private EGLSurface mEglSurface;
    private static boolean isRecording;
    private EGLContext mEglContext;

    private int av_Position;
    private int af_Position;
    private int s_Texture;


    public static boolean isRecording()
    {
        return isRecording;
    }

    public RecordRenderDrawer(Context context) {
        this.mVideoEncoder = null;
        this.mEglHelper = null;
        this.mTextureId = 0;
        this.isRecording = false;
        new Thread(this).start();
    }

    @Override
    public void setInputTextureId(int textureId) {
        this.mTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        return mTextureId;
    }

    @Override
    public void create() {
        mEglContext = EGL14.eglGetCurrentContext();
    }

    public void startRecord() {
        Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_START_RECORD, width, height, mEglContext);
        mMsgHandler.sendMessage(msg);
        isRecording = true;
    }

    public void stopRecord() {
        isRecording = false;
        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MsgHandler.MSG_STOP_RECORD));
    }

    @Override
    public void surfaceChangedSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    //如果没有在绘制的话，什么也不做。正在录制才发出录制消息
    @Override
    public void draw(long timestamp, float[] transformMatrix) {
        if (isRecording) {
            Log.d(TAG, "draw: ");
            Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_FRAME, timestamp);
            mMsgHandler.sendMessage(msg);
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        mMsgHandler = new MsgHandler();
        Looper.loop();
    }

    private class MsgHandler extends Handler {
        public static final int MSG_START_RECORD = 1;
        public static final int MSG_STOP_RECORD = 2;
        public static final int MSG_UPDATE_CONTEXT = 3;
        public static final int MSG_UPDATE_SIZE = 4;
        public static final int MSG_FRAME = 5;
        public static final int MSG_QUIT = 6;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD: //开始录制
                    prepareVideoEncoder((EGLContext) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_STOP_RECORD: //停止录制
                    stopVideoEncoder();
                    break;

                case MSG_UPDATE_CONTEXT:
                    updateEglContext((EGLContext) msg.obj);
                    break;

                case MSG_UPDATE_SIZE:
                    updateChangedSize(msg.arg1, msg.arg2);
                    break;

                case MSG_FRAME: //有帧数据刷新
                    drawFrame((long)msg.obj);
                    break;

                case MSG_QUIT: //退出
                    quitLooper();
                    break;
                default:
                    break;
            }
        }
    }



    // 准备视频编码器 getInputSurface---->mEglSurface-->makeCurrent
    private void prepareVideoEncoder(EGLContext context, int width, int height) {
        String fileName = mFormatter.format(new Date())+".mp4";
        try {
            mEglHelper = new EGLHelper();
            mEglHelper.createGL(context);
            mVideoPath = StorageUtil.getVedioPath(true) + fileName;
            mVideoEncoder = new VideoEncoder(width, height, new File(mVideoPath));
            mEglSurface = mEglHelper.createWindowSurface(mVideoEncoder.getInputSurface());
            boolean error = mEglHelper.makeCurrent(mEglSurface);
            if (!error) {
                Log.e(TAG, "prepareVideoEncoder: make current error");
            }
            onCreated();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopVideoEncoder() {
        mVideoEncoder.drainEncoder(true);
        if (mEglHelper != null) {
            mEglHelper.destroySurface(mEglSurface);
            mEglHelper.destroyGL();
            mEglSurface = EGL14.EGL_NO_SURFACE;
            mVideoEncoder.release();
            mEglHelper = null;
            mVideoEncoder = null;
        }
    }

    private void updateEglContext(EGLContext context) {
        mEglSurface = EGL14.EGL_NO_SURFACE;
        mEglHelper.destroyGL();
        mEglHelper.createGL(context);
        mEglSurface = mEglHelper.createWindowSurface(mVideoEncoder.getInputSurface());
        boolean error = mEglHelper.makeCurrent(mEglSurface);
        if (!error) {
            Log.e(TAG, "prepareVideoEncoder: make current error");
        }
    }


    private void drawFrame(long timeStamp) {
        //指定显示器，渲染的Surface,会读数据的Surface（这两个Surface是由Medicode mEncoder创建的）,EGL上下文
        mEglHelper.makeCurrent(mEglSurface);// 因为在其他线程执行，所以要函设定OpenGL当前渲染环境(线程相关)

        onDraw(); //https://www.bigflake.com/mediacodec/EncodeAndMuxTest.java.txt 也没有换。根据不写黑屏，我认为要换
        mVideoEncoder.drainEncoder(false);// mEncoder从缓冲区取数据，然后交给mMuxer编码
         //onDraw();

        mEglHelper.setPresentationTime(mEglSurface, timeStamp);//设置显示时间戳pts

        //通过这种方法强制执行glFlush，交换缓冲，保证供另一帧，而因为输入缓冲满导致阻塞
        mEglHelper.swapBuffers(mEglSurface);
        /**
         * glFlush和glFinish被称为显式同步操作。某些情况下也会发生隐式同步操作。调用eglSwapBuffers时，就可能发生这种情况。
         * 由于这个操作是由驱动直接执行的，此时GPU 可能把所有待执行的glDraw*绘制指令，作用在一个不符合预期的surface缓冲上(如果之前前端缓冲和后端缓冲已经交换过了)。
         * 为了防止这种情形，在交换缓冲前，驱动必须阻塞当前线程，等待所有的影响当前surface的glDraw*指令执行完毕。
         * 当然，使用双重缓冲的surfaces时，不需要主动调用glFlush或glFinish：因为eglSwapBuffers进行了隐式同步操作。
         * 但在使用单缓冲surfaces(如上文提到的第二个线程里)的情况，需要及时调用glFlush，
         * 例如：在线程退出前，必须调用glFlush，否则，GL 指令可能从未发送到GPU。
         *
         * 作者：Geedio
         * 链接：https://juejin.im/post/58dca46b61ff4b006b03bd57
         */
    }

    private void updateChangedSize(int width, int height) {
        onChanged(width, height);
    }

    private void quitLooper() {
        Looper.myLooper().quit();
    }

    @Override
    protected void onCreated() {
        mProgram = GlesUtil.createProgram(getVertexSource(), getFragmentSource());
        initVertexBufferObjects();
        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
    }

    @Override
    protected void onChanged(int width, int height) {

    }

    @Override
    protected void onDraw() {
        clear();
        useProgram();
        viewPort(0, 0, width, height);

        GLES20.glEnableVertexAttribArray(av_Position);
        GLES20.glEnableVertexAttribArray(af_Position);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);//使用这个纹理贴图进行渲染，最终渲染到帧缓冲区(这里是默认窗口缓冲区)
        GLES20.glUniform1i(s_Texture, 0);
        // 绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);//恢复默认纹理
    }

    @Override
    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
                "attribute vec2 af_Position; " +
                "varying vec2 v_texPo; " +
                "void main() { " +
                "    v_texPo = af_Position; " +
                "    gl_Position = av_Position; " +
                "}";
        return source;
    }

    @Override
    protected String getFragmentSource() {
        final String source = "precision mediump float;\n" +
                "varying vec2 v_texPo;\n" +
                "uniform sampler2D s_Texture;\n" +
                "void main() {\n" +
                "   vec4 tc = texture2D(s_Texture, v_texPo);\n" +
                "   gl_FragColor = texture2D(s_Texture, v_texPo);\n" +
                "}";
        return source;
    }
}
