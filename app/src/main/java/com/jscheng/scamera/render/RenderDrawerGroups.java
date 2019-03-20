package com.jscheng.scamera.render;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.jscheng.scamera.util.GlesUtil;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/8/31
 *   统一管理所有的RenderDrawer 和 FBO
 */
public class RenderDrawerGroups {
    private int mInputTexture;//有来自camera的原始数据
    private int mFrameBuffer;
    private OriginalRenderDrawer mOriginalDrawer;
    private WaterMarkRenderDrawer mWaterMarkDrawer;
    private DisplayRenderDrawer mDisplayDrawer;
    private RecordRenderDrawer mRecordDrawer;

    public RenderDrawerGroups(Context context) {
        this.mOriginalDrawer = new OriginalRenderDrawer();
        this.mWaterMarkDrawer = new WaterMarkRenderDrawer(context);
        this.mDisplayDrawer = new DisplayRenderDrawer();
        this.mRecordDrawer = new RecordRenderDrawer(context);
        this.mFrameBuffer = 0;
        this.mInputTexture = 0;
    }

    public void setInputTexture(int texture) {
        this.mInputTexture = texture;
    }

    //GL_TEXTURE_2D纹理到FBO,OpenGL就会执行渲染到纹理
    // 由于我们的帧缓冲不是默认的帧缓冲，渲染命令对窗口的视频输出不会产生任何影响
    public void bindFrameBuffer(int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    public void unBindFrameBuffer() {
        //ID号为0表示缺省帧缓存，即默认的window提供的帧缓存
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void deleteFrameBuffer() {
        GLES20.glDeleteFramebuffers(1, new int[]{mFrameBuffer}, 0);
        GLES20.glDeleteTextures(1, new int[]{mInputTexture}, 0);
    }

    public void create() {
        this.mOriginalDrawer.create();
        this.mWaterMarkDrawer.create();
        this.mDisplayDrawer.create();
        this.mRecordDrawer.create();
    }

    public void surfaceChangedSize(int width, int height) {
        mFrameBuffer = GlesUtil.createFrameBuffer();
        mOriginalDrawer.surfaceChangedSize(width, height);
        mWaterMarkDrawer.surfaceChangedSize(width, height);
        mDisplayDrawer.surfaceChangedSize(width, height);
        mRecordDrawer.surfaceChangedSize(width, height);

        this.mOriginalDrawer.setInputTextureId(mInputTexture);
        int textureId = this.mOriginalDrawer.getOutputTextureId();
        mWaterMarkDrawer.setInputTextureId(textureId);
        mDisplayDrawer.setInputTextureId(textureId);
        mRecordDrawer. setInputTextureId(textureId);
    }

    //外部纹理进行了bindFrameBuffer就是离屏渲染，没有bindFrameBuffer，渲染操作都是在默认的帧缓冲之上进行
    public void drawRender(BaseRenderDrawer drawer, boolean useFrameBuffer, long timestamp, float[] transformMatrix) {
        if (useFrameBuffer) { //外部纹理绑定到FBO，之后的所有的OpenGL操作都会对当前所绑定的FBO造成影响。
            bindFrameBuffer(drawer.getOutputTextureId());
        }


        //BaseRenderDrawer.draw:清屏-->调用着色器语言-->设置视口-->回调onDraw
        drawer.draw(timestamp, transformMatrix);

        if (useFrameBuffer) {
            unBindFrameBuffer();
        }
    }

    public void draw(long timestamp, float[] transformMatrix) {
        if (mInputTexture == 0 || mFrameBuffer == 0) {
            Log.e(TAG, "draw: mInputTexture or mFramebuffer or list is zero");
            return;
        }
        // 执行OriginalRenderDrawer渲染，通过FBO就自然就渲染到了DisplayRenderDrawer的纹理(共享外部纹理)上??? 如何理解

         //mOriginalDrawer/mWaterMarkDrawer 将绑定到FBO中，最后转换成mOriginalDrawer中的Sample2D纹理
         //mDisplayDrawer/mRecordDrawer 不绑定FBO，直接绘制到屏幕上
        // 绘制顺序会控制着 水印绘制哪一层

        drawRender(mOriginalDrawer, true, timestamp, transformMatrix);  // 输出纹理＝Original
        drawRender(mDisplayDrawer, false,  timestamp, transformMatrix); //显示原始数据
        drawRender(mWaterMarkDrawer, true, timestamp, transformMatrix); //输出纹理＝Original+WaterMark
        drawRender(mRecordDrawer, false, timestamp, transformMatrix);//????


        /*
        顺序：1.mOriginalDrawer:绘制的数据基础是mInputTexture，渲染到FBO(FBO<--mOutputTexture,mOutputTexture=mInputTexture+ondraw)
          -->2.mDisplayDrawer:绘制的数据基础是共享外部纹理，渲染到默认缓冲(FBO=null,默认缓冲＝mOutputTexture＋ondraw)
          -->3.mWaterMarkDrawer:绘制的数据基础是共享外部纹理，渲染到FBO(FBO<--mOutputTexture,mOutputTexture=1.mOutputTexture+ondraw)
          -->mRecordDrawer:绘制的数据基础是共享外部纹理，渲染到默认缓冲(FBO=null,默认缓冲＝3.mOutputTexture＋ondraw)
        */
   /*
       drawRender(mOriginalDrawer, true, timestamp, transformMatrix);
       drawRender(mDisplayDrawer, false,  timestamp, transformMatrix);
       drawRender(mWaterMarkDrawer, true, timestamp, transformMatrix);
       drawRender(mRecordDrawer, false, timestamp, transformMatrix);
       */

    }

    public void startRecord() {
        mRecordDrawer.startRecord();
    }

    public void stopRecord() {
        mRecordDrawer.stopRecord();
    }
}
