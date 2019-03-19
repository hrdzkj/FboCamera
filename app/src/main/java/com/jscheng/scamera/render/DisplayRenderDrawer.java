package com.jscheng.scamera.render;

import android.opengl.GLES20;

/**
 * Created By Chengjunsen on 2018/8/27
  *  输入纹理和输出纹理相同，由外部传入
 */
public class DisplayRenderDrawer extends BaseRenderDrawer {
    private int av_Position;
    private int af_Position;
    private int s_Texture;
    private int mTextureId;

    @Override
    protected void onCreated() {
    }

    @Override
    protected void onChanged(int width, int height) {
        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
    }

    //
    @Override
    protected void onDraw() {
        GLES20.glEnableVertexAttribArray(av_Position);
        GLES20.glEnableVertexAttribArray(af_Position);

        //渲染时上传顶点数据到显卡
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);//传入顶点坐标
        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mDisplayTextureBufferId);//
        GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);//取消buffer的绑定

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(s_Texture, 0); //把当前纹理单元传递给shader
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount); // 绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisableVertexAttribArray(af_Position);
        GLES20.glDisableVertexAttribArray(av_Position);
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
    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " + // 定点位置
                "attribute vec2 af_Position; " + // 纹理位置
                "varying vec2 v_texPo; " + // 纹理位置 与fragment_shader交互
                "void main() { " +
                "    v_texPo = af_Position; " +
                "    gl_Position = av_Position; " +
                "}";
        return source;
    }

    //texture2D 第一个参数代表图片纹理,本例子GLES20.glUniform1i(s_Texture, 0)传递默认纹理单元过来;
    // 第二个参数代表纹理坐标点，本例子传递af_Position过来
    // 通过GLSL的内建函数texture2D来获取对应位置纹理的颜色RGBA值
    @Override
    protected String getFragmentSource() {
        final String source = "precision mediump float;\n" +
                "varying vec2 v_texPo;\n" + //纹理位置  接收于vertex_shader
                "uniform sampler2D s_Texture;\n" +
                "void main() {\n" +
                "   vec4 tc = texture2D(s_Texture, v_texPo);\n" +
                "   float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                "   gl_FragColor = texture2D(s_Texture, v_texPo);\n" +
                //"    gl_FragColor = vec4(0.4,0.4,0.8,1.0);\n" +
                "}";
        return source;
    }
}
