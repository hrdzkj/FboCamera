package com.jscheng.scamera.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.jscheng.scamera.util.CameraUtil;
import com.jscheng.scamera.util.GlesUtil;

/**
 * Created By Chengjunsen on 2018/8/27
  *  输出纹理是GL_TEXTURE_2D的纹理；输入纹理外部传入
 */
public class OriginalRenderDrawer extends BaseRenderDrawer {
    private int av_Position;
    private int af_Position;
    private int s_Texture;
    private int mInputTextureId;
    private int mOutputTextureId;//外部纹理对象

    @Override
    protected void onCreated() {
    }

    @Override
    protected void onChanged(int width, int height) {
        mOutputTextureId = GlesUtil.createFrameTexture(width, height);

        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
    }

    // 原始数据的绘制，是在摄像头数据mInputTextureId的基础上进行绘制
    @Override
    protected void onDraw() {
        if (mInputTextureId == 0 || mOutputTextureId == 0) {
            return;
        }

        GLES20.glEnableVertexAttribArray(av_Position);
        GLES20.glEnableVertexAttribArray(af_Position);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, 0, 0);
        // 激活缓冲区对象
        if (CameraUtil.isBackCamera()) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBackTextureBufferId);
        } else {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mFrontTextureBufferId);
        }
        GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        bindTexture(mInputTextureId);
        //glDrawArrays作用：使用当前激活的顶点着色器的顶点数据和片段着色器数据来绘制基本图形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        unBindTexure();
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);

    }

    //将绑定纹理textureId到目标GLES11Ext.GL_TEXTURE_EXTERNAL_OES指定的当前活动纹理单元GL_TEXTURE0
    /*
    绑定纹理,值得注意的是，纹理帮定的目标(target)并不是通常的GL_TEXTURE_2D，而是GL_TEXTURE_EXTERNAL_OES,
    这是因为Camera使用的输出texture是一种特殊的格式。同样的，在shader中我们也必须使用SamperExternalOES
    的变量类型来访问该纹理
     */
    private void bindTexture(int textureId) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(s_Texture, 0);
    }

    private void unBindTexure() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    @Override
    public void setInputTextureId(int textureId) {
        mInputTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        return mOutputTextureId;
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
        final String source = "#extension GL_OES_EGL_image_external : require \n" +
                "precision mediump float; " +
                "varying vec2 v_texPo; " +
                "uniform samplerExternalOES s_Texture; " +
                "void main() { " +
                "   gl_FragColor = texture2D(s_Texture, v_texPo); " +
                "} ";
        return source;
    }
}
