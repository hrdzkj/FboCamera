package com.jscheng.scamera.view;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.jscheng.scamera.BaseActivity;
import com.jscheng.scamera.R;

/**
 * Created By Chengjunsen on 2018/8/22
 * https://blog.csdn.net/qq_15893929/article/details/82864976
 */
public class MainActivity extends BaseActivity {
    private CameraFragment mCameraFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getGlesVersion();
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mCameraFragment = new CameraFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.main_container, mCameraFragment);
        transaction.commit();
    }

    private void getGlesVersion()
    {
        ActivityManager am =(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        String glesVersion= info.getGlEsVersion();
        Log.d("------->",glesVersion);
    }
}
