/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xconns.samples.rotate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import com.xconns.peerdevicenet.DeviceInfo;
import com.xconns.peerdevicenet.Router;
import com.xconns.peerdevicenet.RouterGroupClient;


// a simple msg for rotation info. 
class RotateMsg {
	public static final String TAG = "RotateMsg";
	// msg ids
	public final static int INIT_ORIENT_REQ = 1; //inital query of peers' orientation
	public final static int INIT_ORIENT_RSP = 2; //responses of peers' orientation
	public final static int DELTA_ROTATION = 3;  //changes of orientation
	
	public int msgId; 
	public float rx, ry; //rotation along x & y axis
	public RotateMsg() {
	}
	public RotateMsg(int id, float x, float y) {
		msgId = id;
		rx = x;
		ry = y;
	}
	
	//the following using JSON to marshaling data.
	public byte[] marshall() {
		try {
			JSONArray json = new JSONArray();
			json.put(0, msgId);
			json.put(1, rx);
			json.put(2, ry);
			return json.toString().getBytes();
		}  catch(JSONException je) {
			Log.e(TAG, je.toString());
			return null;
		}
	}

	public void unmarshall(byte[] data, int len) {
		try {
			JSONArray json = new JSONArray(new String(data));
			msgId = json.getInt(0);
			rx = (float)json.getDouble(1);
			ry = (float)json.getDouble(2);
		}  catch(JSONException je) {
			Log.e(TAG, je.toString());
		}
	}

}


/**
 * Wrapper activity demonstrating the use of {@link GLSurfaceView}, a view that
 * uses OpenGL drawing into a dedicated surface.
 * 
 * Shows: + How to redraw in response to user input.
 */
public class TouchRotateActivity extends Activity {
	private static final String TAG = "TouchRotateActivity";

	private static final String groupId = "RotateWithPeers";
    private RouterGroupClient mGroupClient = null;

	// opengl canvas
	private TouchSurfaceView mGLSurfaceView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		LinearLayout mainView = (LinearLayout) findViewById(R.id.container);

		// Create our Preview view and set it as the content of our
		// Activity
		mGLSurfaceView = new TouchSurfaceView(this);
		mainView.addView(mGLSurfaceView);
		mGLSurfaceView.requestFocus();
		mGLSurfaceView.setFocusableInTouchMode(true);
		
		//add a button to allow device connect to peers, if it is not connected
		Button connBtn = (Button) findViewById(R.id.conn_btn);
		connBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(
						"com.xconns.peerdevicenet.CONNECTOR");
				startActivity(intent);
			}
		});

        mGroupClient = new RouterGroupClient(this, groupId, null, mGroupHandler);
        mGroupClient.bindService();
	}

	@Override
	protected void onResume() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();
		mGLSurfaceView.onResume();
	}

	@Override
	protected void onPause() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onPause();
		mGLSurfaceView.onPause();
	}

	@Override
	public void onDestroy() {
        mGroupClient.unbindService();
		super.onDestroy();
	}
	
	//send rotate info to peers
	public void sendRotateMsgToPeers(RotateMsg m) {
        mGroupClient.send(null, m.marshall());
	}
	
	//process rotate info from peers
	void procRotateMsgFromPeer(DeviceInfo dev, RotateMsg m) {
		//process init orientation req
		if (m.msgId == RotateMsg.INIT_ORIENT_REQ) {
			RotateMsg m1 = mGLSurfaceView.getCurrentOrientation();
			mGroupClient.send(dev, m1.marshall());
            return;
		}
		//handle init orientation resp and delta rotation
		mGLSurfaceView.procRotateMsgFromPeer(m);
	}

	private RouterGroupClient.GroupHandler mGroupHandler = new RouterGroupClient.GroupHandler() {

		public void onError(String errInfo) {
			Log.d(TAG, "group comm error : " + errInfo);
		}

		public void onSelfJoin(DeviceInfo[] devices) {
			if (devices != null && devices.length > 0) {
				//i have peers, sync my inital orientation with them
				RotateMsg m = new RotateMsg(RotateMsg.INIT_ORIENT_REQ, 0, 0); //req init orientation
				mGroupClient.send(null, m.marshall());
			}
		}

		public void onPeerJoin(DeviceInfo device) {
		}

		public void onSelfLeave() {
		}

		public void onPeerLeave(DeviceInfo device) {
		}

		public void onReceive(DeviceInfo src, byte[] b) {
			Log.d(TAG, "recv rotate info from "+src.toString());
			Message msg = mHandler.obtainMessage(Router.MsgId.RECV_MSG);
			msg.obj = new Object[]{src, b};
			mHandler.sendMessage(msg);
		}

		public void onGetPeerDevices(DeviceInfo[] devices) {
		}
	};

	/**
	 * Handler of incoming messages from service.
	 */
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Router.MsgId.RECV_MSG:
				Object[] data = (Object[]) msg.obj;
				DeviceInfo dev = (DeviceInfo) data[0];
				byte[] rawbytes = (byte[]) data[1];
				RotateMsg m = new RotateMsg();
				m.unmarshall(rawbytes, rawbytes.length);
				procRotateMsgFromPeer(dev, m);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

}


/**
 * Implement a simple rotation control.
 * 
 */
class TouchSurfaceView extends GLSurfaceView {
	
	TouchRotateActivity rotAct = null;

	public TouchSurfaceView(TouchRotateActivity context) {
		super(context);
		rotAct = context;
		mRenderer = new CubeRenderer();
		setRenderer(mRenderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent e) {
		float dx = e.getX() * TRACKBALL_SCALE_FACTOR;
		float dy = e.getY() * TRACKBALL_SCALE_FACTOR;
		rotAct.sendRotateMsgToPeers(new RotateMsg(RotateMsg.DELTA_ROTATION, dx, dy));
		mRenderer.mAngleX += dx;
		mRenderer.mAngleY += dy;
		requestRender();
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
		switch (e.getAction()) {
		case MotionEvent.ACTION_MOVE:
			float dx = (x - mPreviousX) * TOUCH_SCALE_FACTOR;
			float dy = (y - mPreviousY) * TOUCH_SCALE_FACTOR;
			rotAct.sendRotateMsgToPeers(new RotateMsg(RotateMsg.DELTA_ROTATION, dx, dy));
			mRenderer.mAngleX += dx;
			mRenderer.mAngleY += dy;
			requestRender();
		}
		mPreviousX = x;
		mPreviousY = y;
		return true;
	}
	
	//the following two methods are added for handling msgs from peers
	public void procRotateMsgFromPeer(RotateMsg m) {
		if (m.msgId == RotateMsg.INIT_ORIENT_RSP) {
			mRenderer.mAngleX = m.rx;
			mRenderer.mAngleY = m.ry;
		}
		else if (m.msgId == RotateMsg.DELTA_ROTATION) {
			mRenderer.mAngleX += m.rx;
			mRenderer.mAngleY += m.ry;
		}
		requestRender();		
	}
	
	public RotateMsg getCurrentOrientation() {
		return new RotateMsg(RotateMsg.INIT_ORIENT_RSP, mRenderer.mAngleX, mRenderer.mAngleY);
	}

	/**
	 * Render a cube.
	 */
	private class CubeRenderer implements GLSurfaceView.Renderer {
		public CubeRenderer() {
			mCube = new Cube();
		}

		public void onDrawFrame(GL10 gl) {
			/*
			 * Usually, the first thing one might want to do is to clear the
			 * screen. The most efficient way of doing this is to use glClear().
			 */

			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

			/*
			 * Now we're ready to draw some 3D objects
			 */

			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glTranslatef(0, 0, -3.0f);
			gl.glRotatef(mAngleX, 0, 1, 0);
			gl.glRotatef(mAngleY, 1, 0, 0);

			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

			mCube.draw(gl);
		}

		public void onSurfaceChanged(GL10 gl, int width, int height) {
			gl.glViewport(0, 0, width, height);

			/*
			 * Set our projection matrix. This doesn't have to be done each time
			 * we draw, but usually a new projection needs to be set when the
			 * viewport is resized.
			 */

			float ratio = (float) width / height;
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			/*
			 * By default, OpenGL enables features that improve quality but
			 * reduce performance. One might want to tweak that especially on
			 * software renderer.
			 */
			gl.glDisable(GL10.GL_DITHER);

			/*
			 * Some one-time OpenGL initialization can be made here probably
			 * based on features of this particular context
			 */
			gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

			gl.glClearColor(1, 1, 1, 1);
			gl.glEnable(GL10.GL_CULL_FACE);
			gl.glShadeModel(GL10.GL_SMOOTH);
			gl.glEnable(GL10.GL_DEPTH_TEST);
		}

		private Cube mCube;
		public float mAngleX;
		public float mAngleY;
	}

	private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	private final float TRACKBALL_SCALE_FACTOR = 36.0f;
	private CubeRenderer mRenderer;
	private float mPreviousX;
	private float mPreviousY;
}

/**
 * A vertex shaded cube.
 */
class Cube {
	public Cube() {
		int one = 0x10000;
		int vertices[] = { -one, -one, -one, one, -one, -one, one, one, -one,
				-one, one, -one, -one, -one, one, one, -one, one, one, one,
				one, -one, one, one, };

		int colors[] = { 0, 0, 0, one, one, 0, 0, one, one, one, 0, one, 0,
				one, 0, one, 0, 0, one, one, one, 0, one, one, one, one, one,
				one, 0, one, one, one, };

		byte indices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7,
				3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3, 1, 2 };

		// Buffers to be passed to gl*Pointer() functions
		// must be direct, i.e., they must be placed on the
		// native heap where the garbage collector cannot
		// move them.
		//
		// Buffers with multi-byte datatypes (e.g., short, int, float)
		// must have their byte order set to native order

		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asIntBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);

		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
		cbb.order(ByteOrder.nativeOrder());
		mColorBuffer = cbb.asIntBuffer();
		mColorBuffer.put(colors);
		mColorBuffer.position(0);

		mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
		mIndexBuffer.put(indices);
		mIndexBuffer.position(0);
	}

	public void draw(GL10 gl) {
		gl.glFrontFace(gl.GL_CW);
		gl.glVertexPointer(3, gl.GL_FIXED, 0, mVertexBuffer);
		gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer);
		gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_BYTE,
				mIndexBuffer);
	}

	private IntBuffer mVertexBuffer;
	private IntBuffer mColorBuffer;
	private ByteBuffer mIndexBuffer;
}
