package com.jscheng.scamera.render;

import android.opengl.GLES20;

import com.jscheng.scamera.util.GlesUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created By Chengjunsen on 2018/8/27
 * 功能：
 * 1.提供顶点着色器、片段着色器 的接口，具体子类实现。
 * 2.设置InputTextureId接口，具体子类实现
 * 3.获取outputTextureId，具体子类实现，需要输出到FBO时候用到。
 * 4.draw流程
 */
public abstract class BaseRenderDrawer {
    protected int width;

    protected int height;

    protected int mProgram;

    //顶点坐标 Buffer
    private FloatBuffer mVertexBuffer;
    protected int mVertexBufferId;

    //纹理坐标 Buffer
    private FloatBuffer mFrontTextureBuffer;
    protected int mFrontTextureBufferId;

    //纹理坐标 Buffer
    private FloatBuffer mBackTextureBuffer;
    protected int mBackTextureBufferId;

    //同时用于DisplayRenderDrawer/RecordRenderDawer 是什么坐标呢?????
    private FloatBuffer mDisplayTextureBuffer;
    protected int mDisplayTextureBufferId;

    //VBO定点数据,用于画水印
    private FloatBuffer mFrameTextureBuffer;
    protected int mFrameTextureBufferId;

    protected float vertexData[] = {
            -1f, -1f,// 左下角
            1f, -1f, // 右下角
            -1f, 1f, // 左上角
             1f, 1f,  // 右上角
    };

    protected float frontTextureData[] = {
            1f, 1f, // 右上角
            1f, 0f, // 右下角
            0f, 1f, // 左上角
            0f, 0f //  左下角
    };

    //使用 ??为什么坐标循序是这样
    protected float backTextureData[] = {
            0f, 1f, // 左上角
            0f, 0f, //  左下角
            1f, 1f, // 右上角
            1f, 0f  // 右下角
    };

    //显示纹理坐标，用于纹理位置af_Position
    //与vertexData相比，翻转过来了；目前是整个纹理片的范围
    protected float displayTextureData[] = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
    };


    // 画水印时候传递给avPosition。
    protected float frameBufferData[] = {
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
    };

    protected final int CoordsPerVertexCount = 2;

    protected final int VertexCount = vertexData.length / CoordsPerVertexCount;

    protected final int VertexStride = CoordsPerVertexCount * 4;

    protected final int CoordsPerTextureCount = 2;

    protected final int TextureStride = CoordsPerTextureCount * 4;

    public BaseRenderDrawer() {

    }

    public void create() {
        mProgram = GlesUtil.createProgram(getVertexSource(), getFragmentSource());
        initVertexBufferObjects();
        onCreated();
    }

    public void surfaceChangedSize(int width, int height) {
        this.width = width;
        this.height = height;
        onChanged(width, height);
    }

    public void draw(long timestamp, float[] transformMatrix){
        clear();
        useProgram();
        viewPort(0, 0, width, height);
        onDraw();
    }

    protected void clear(){
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);//白色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    protected void initVertexBufferObjects() {
        int[] vbo = new int[5];
        GLES20.glGenBuffers(5, vbo, 0);//生成新缓存对象

        mVertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        mVertexBuffer.position(0);
        mVertexBufferId = vbo[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);//绑定缓存对象
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, mVertexBuffer, GLES20.GL_STATIC_DRAW);//将顶点数据拷贝到缓存对象中


        mBackTextureBuffer = ByteBuffer.allocateDirect(backTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(backTextureData);
        mBackTextureBuffer.position(0);
        mBackTextureBufferId = vbo[1];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBackTextureBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, backTextureData.length * 4, mBackTextureBuffer, GLES20.GL_STATIC_DRAW);

        mFrontTextureBuffer = ByteBuffer.allocateDirect(frontTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frontTextureData);
        mFrontTextureBuffer.position(0);
        mFrontTextureBufferId = vbo[2];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mFrontTextureBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, frontTextureData.length * 4, mFrontTextureBuffer, GLES20.GL_STATIC_DRAW);


        mDisplayTextureBuffer = ByteBuffer.allocateDirect(displayTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(displayTextureData);
        mDisplayTextureBuffer.position(0);
        mDisplayTextureBufferId = vbo[3];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, displayTextureData.length * 4, mDisplayTextureBuffer, GLES20.GL_STATIC_DRAW);

        mFrameTextureBuffer = ByteBuffer.allocateDirect(frameBufferData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frameBufferData);
        mFrameTextureBuffer.position(0);
        mFrameTextureBufferId = vbo[4];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mFrameTextureBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, frameBufferData.length * 4, mFrameTextureBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
    }

    protected void useProgram(){
        GLES20.glUseProgram(mProgram); //使用程序对象作为当前渲染状态的一部分
    }

    protected void viewPort(int x, int y, int width, int height) {
        GLES20.glViewport(x, y, width,  height);
    }

    public abstract void setInputTextureId(int textureId);

    public abstract int getOutputTextureId();

    protected abstract String getVertexSource();

    protected abstract String getFragmentSource();

    protected abstract void onCreated();

    protected abstract void onChanged(int width, int height);

    protected abstract void onDraw();

}
