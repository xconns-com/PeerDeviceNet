RotateWithPeers
===============

This sample is a slight change of standard android example TouchRotateActivity. 
The original TouchRotateActivity allows you rotate a 3D cube thru touch screen. 
This sample add PeerDeviceNet group communication to allow several android devices 
rotate the cube together, a simple demo of multi-player GUI applications.

The changes are as following.

1. configuration changes

	1.1. in AndroidManifest.xml, add the following permission to enable group communication:
		
		<uses-permission android:name="com.xconns.peerdevicenet.permission.REMOTE_MESSAGING" />

	1.2. To access Router's api, add peerdevicenet-api.jar in one of two ways:
             
        * download peerdevicenet-api.jar from MavenCentral(http://search.maven.org/#search|ga|1|peerdevicenet) and copy to project's "libs/" directory.
        * if you are using android's new gradle build system, you can import it as 'com.xconns.peerdevicenet:peerdevicenet-api:1.1.6'.

 		
2. code changes

	2.1. connect to peer devices

	We could directly call connection service api to set up peer connections. Or we can
		reuse one of the sample connectors to search and connect to peer devices.
		So add a button in GUI to bring up a connector. In button's callback, 
		add following code:

		Intent intent = new Intent("com.xconns.peerdevicenet.CONNECTOR");
		startActivity(intent);
		
	2.2. bind to PeerDeviceNet group service

	The group service is named as: "com.xconns.peerdevicenet.GroupService".
	    To simplify coding, here we use the wrapper class of GroupService: RouterGroupClient.
		As normal, we bind/unbind to aidl group service during life-cycle callback methods:

		onCreate():
            mGroupClient = new RouterGroupClient(this, groupId/*"RotateWithPeers"*/, null, mGroupHandler);
            mGroupClient.bindService();
		onDestroy():
		   	mGroupClient.unbindService();
		
	2.3. join/leave group

	All devices participating in rotating cube will join a group named "RotateWithPeers".

	We'll join group when binding to group service is complete, and register group handler
		   to handle events, leave group when app is destroyed, these are all handled automatically
		   at above mGroupClient.bindService(), mGroupClient.unbindService().

	2.4. application message

	We use RotateMsg class to send rotation information, with 
			3 message ids for initial orientation requests and orientation change events.

	In PeerDeviceNet api, messages are sent as binary blobs. We need marshal/serialize
			messages objects into binary blob and demarshal/deserialize them back.
			For this sample, we use android "Parcel" class. We could use JSON too.

		class RotateMsg {
			//message ids
			public final static int INIT_ORIENT_REQ = 1; //inital query of peers' orientation
			public final static int INIT_ORIENT_RSP = 2; //responses of peers' orientation
			public final static int DELTA_ROTATION = 3;  //changes of orientation
			byte[] marshall() {...}
			void unmarshall(byte[] data, int len) {...}
		}
			
	2.5. send messages

	We use group service's async api method to send message. 

	When sending initial orientation requests and orientation change events, we 
			broadcast messages to all peers in group:

		mGroupClient.send(null, msg.marshall());

	When replying to peer's initial orientation request, we send point-to-point
			message to just send to the requesting peer:

		mGroupClient.send(requesting_peer, msg.marshall());
	
	2.6. receive messages

	To receive messages and other group communication events, define a handler
			object implementing RouterGroupClient.GroupHandler interface:

			mGroupHandler = new RouterGroupClient.GroupHandler() {
				onSelfJoin(DeviceInfo[] devices):
					here we find out if there are existing peers in the group, 
					if so, send message requesting their orientation to sync initial orientation.
				
				onReceive(DeviceInfo src, byte[] b):
					here we start processing messages from peers.
					there are two kinds of messages: 
						initial orientation response and orientation change events;
						based on these events data, rotate the cube in GUI
			    ......
			}	
			
	Please note that group handler's methods are executed in a thread pool
				managed by android runtime, while GUI changes should be done in main GUI thread.
				So create a android.os.Handler object (mHandler) with GUI thread and the above onReceive() method will forward event message to this handler object for processing.
				

			
