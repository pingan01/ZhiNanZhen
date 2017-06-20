package com.lj.zhinanzhen;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by WX on 2017/6/16.
 */

public class ModelFragment extends Fragment implements SensorEventListener {
    private GLSurfaceView glView;
    private GLRenderer glRenderer;
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private float xAngle;
    private float yAngle;
    private float zAngle;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_foreign, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        glView = (GLSurfaceView) getActivity().findViewById(R.id.GLSurfaceView);
        Log.e("TAG", "glView=" + glView);
        glRenderer = new GLRenderer(getContext());
        glView.setRenderer(glRenderer);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        xAngle = event.values[1];
        yAngle = event.values[2];
        zAngle = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * 目标是显示任意模型，因此，必须把模型移动到我们的视野中，才能看得到（当然了，如果图形本身就是在我们的视野中，那就不一定需要这样的操作了）
     */
    class GLRenderer implements GLSurfaceView.Renderer {

        private Model model;
        private Point mCenterPoint;
        private Point eye = new Point(0, 0, -3);
        private Point up = new Point(0, 1, 0);
        private Point center = new Point(0, 0, 0);
        private float mScalef = 1;

        public GLRenderer(Context context) {
            try {
                model = new STLReader().parserBinStlInAssets(context, "zhinanzhen.stl");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // 清除屏幕和深度缓存
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

            gl.glLoadIdentity();// 重置当前的模型观察矩阵

            //眼睛对着原点看
            GLU.gluLookAt(gl, eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);

            //为了能有立体感觉，通过改变mDegree值，让模型不断旋转
            gl.glRotatef(xAngle, -1, 0, 0);
            gl.glRotatef(yAngle, 0, 1, 0);
            gl.glRotatef(zAngle, 0, 0, 1);

            //将模型放缩到View刚好装下
            gl.glScalef(mScalef, mScalef, mScalef);
            //把模型移动到原点
            gl.glTranslatef(-mCenterPoint.x, -mCenterPoint.y, -mCenterPoint.z);

            //===================begin==============================//

            //允许给每个顶点设置法向量
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
            // 允许设置顶点
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

            //设置法向量数据源
            gl.glNormalPointer(GL10.GL_FLOAT, 0, model.getVnormBuffer());
            // 设置三角形顶点数据源
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, model.getVertBuffer());

            // 绘制三角形
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, model.getFacetCount() * 3);

            // 取消顶点设置
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

            gl.glFinish();


            //=====================end============================//

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // 设置OpenGL场景的大小,(0,0)表示窗口内部视口的左下角，(width, height)指定了视口的大小
            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL10.GL_PROJECTION); // 设置投影矩阵
            gl.glLoadIdentity(); // 设置矩阵为单位矩阵，相当于重置矩阵

            GLU.gluPerspective(gl, 45.0f, ((float) width) / height, 1f, 100f);// 设置透视范围
            //以下两句声明，以后所有的变换都是针对模型(即我们绘制的图形)
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();

        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glClearColor(0, 0, 0, 0);
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            // 设置透明显示
            gl.glEnable(GL10.GL_ALPHA_TEST);
            gl.glAlphaFunc(GL10.GL_GREATER, 0f);
            /**
             *  gl.glEnable(GL10.GL_DEPTH_TEST); // 启用深度缓存
             gl.glClearDepthf(1.0f); // 设置深度缓存值
             gl.glDepthFunc(GL10.GL_LEQUAL); // 设置深度缓存比较函数
             gl.glShadeModel(GL10.GL_SMOOTH);// 设置阴影模式GL_SMOOTH
             */

            float r = model.getR();
            //r是半径，不是直径，因此用0.5/r可以算出放缩比例
            mScalef = 1.0f / r;
            mCenterPoint = model.getCentrePoint();
            /**
             *  gl.glEnable(GL10.GL_TEXTURE_2D);
             // 创建纹理
             IntBuffer textureBuffer = IntBuffer.allocate(1);
             gl.glGenTextures(1, textureBuffer);
             tex_id = textureBuffer.array();

             for (int i = 0; i < 1; i++) {
             gl.glBindTexture(GL10.GL_TEXTURE_2D, tex_id[i]);

             GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

             gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
             gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
             }

             // 启动纹理坐标数据
             gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
             //打开纹理
             gl.glEnable(GL10.GL_TEXTURE_2D);
             //创建纹理
             gl.glGenTextures(1, tex_id, 0);
             //绑定纹理
             gl.glBindTexture(GL10.GL_TEXTURE_2D, tex_id[0]);

             // 设置纹理参数
             gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,  GL10.GL_NEAREST);
             gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,   GL10.GL_NEAREST);

             gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_WRAP_S,GL10.GL_REPEAT);
             gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_WRAP_T,GL10.GL_REPEAT);
             // 生成纹理
             GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
             */

            //开启光
            //openLight(gl);
            //添加材质属性
            //enableMaterial(gl);
        }

        float[] ambient = {1.0f, 1.0f, 1.0f, 1.0f};//用于设置环境光强度
        float[] diffuse = {1.0f, 1.0f, 1.0f, 1.0f};//用于设置漫反射光强度
        float[] specular = {1.0f, 1.0f, 1.0f, 1.0f};//用于设置镜面反射光强度

        float[] lightPosition = {1.0f, -1.0f, -1.0f, 0.0f};//光源位置，指定光源的位置的参数为GL_POSITION,位置的值为(x,y,z,w)，
        // 如果是平行光则将w设为0，此时，(x,y,z)为平行光的方向；
        // 如果是点光源则将w设为1.0，此时（x,y,z）为点光源的位置坐标

        float[] lightDirection = {0.0f, 0.0f, 1.0f};//光源方向

        public void openLight(GL10 gl) {
            gl.glEnable(GL10.GL_LIGHTING);//启用光照功能
            gl.glEnable(GL10.GL_LIGHT0);//0号光源，该光源的默认颜色为白色，即RGBA为（1.0,1.0,1.0,1.0），其他的光源默认为黑色，即RGBA为（0.0,0.0,0.0,1.0）.

            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, Util.floatToBuffer(lightPosition));
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION, Util.floatToBuffer(lightDirection));
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, Util.floatToBuffer(ambient));
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, Util.floatToBuffer(diffuse));
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, Util.floatToBuffer(specular));
            /**
             * //设置聚光强度
             gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_EXPONENT, 64f);

             //设置聚光角度
             gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_CUTOFF, 45f);
             */

            //params: 参数的值（数组或是Buffer类型），数组里面含有4个值分别表示R,G,B,A。
        }

        float[] materialAmb = {1.0f, 1.0f, 1.0f, 1.0f};//环境光
        float[] materialDiff = {0.0f, 0.0f, 1.0f, 1.0f};//散射光--蓝色
        float[] materialSpec = {1.0f, 0.5f, 0.0f, 1.0f};//镜面光
        //float emi_mat[] = {1f, 1f, 1f, 1.0f};//本身颜色

        public void enableMaterial(GL10 gl) {

            //材料对环境光的反射情况(GL_AMBIENT----光源泛光强度的RGBA值)
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, Util.floatToBuffer(materialAmb));
            //在OpenGL ES中只能使用GL_FRONT_AND_BACK，表示修改物体的前面和后面的材质光线属性。----最后一个参数表示指定反射的颜色。

            //散射光的反射情况
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, Util.floatToBuffer(materialDiff));
            //镜面光的反射情况
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, Util.floatToBuffer(materialSpec));
            //本身
            // gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, Util.floatToBuffer(emi_mat));

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (glView != null) {
            glView.onPause();
        }
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (glView != null) {
            glView.onResume();
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
