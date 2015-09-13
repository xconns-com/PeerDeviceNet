This sample is a slight change of standard android example TouchRotateActivity. 
The original TouchRotateActivity allows you rotate a 3D cube thru touch screen. 
This sample add PeerDeviceNet group communication to allow several android devices 
rotate the cube together, a simple demo of multi-player GUI applications.

The changes are as following.

1. configuration changes
	1.1> install PeerDeviceNet (free edition)
	1.2> in AndroidManifest.xml, add the following permission to enable group communication:
		<uses-permission android:name="com.xconns.peerdevicenet.permission.REMOTE_MESSAGING" />
			
	1.3> this sample will use AIDL interface to connect to group service and communicate 
		with peers; so add the following aidl files under package com.xconns.peerdevicenet:
		DeviceInfo.java - a simple class containing info about device: name, address, port
		DeviceInfo.aidl
 		IRouterGroupService.aidl - async calls to join/leave group and send messages
 		IRouterGroupHandler.aidl - callback interface to receive messages and group events 
 									such as peer join/leave.
 		Router.java - optionally included for convenience, define commonly used message ids.
 		
2. code changes
	2.1> connect to peer devices
		We could call connection service api to set up peer connections. Normally we'll go 
		the simpler route: reuse PeerDeviceNet's connection manager.
		So add a button in GUI to bring up connection manager. In button's callback, 
		add following code:
			Intent intent = new Intent("com.xconns.peerdevicenet.CONNECTION_MANAGEMENT");
			startActivity(intent);
		
	2.2> bind to PeerDeviceNet group service
		The group service is named as: "com.xconns.peerdevicenet.GroupService"
		As normal, we bind/unbind to aidl group service during life-cycle callback methods:
		   onCreate():
			Intent intent = new Intent("com.xconns.peerdevicenet.GroupService");
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		   onDestroy():
		   	unbindService(mConnection);
		
	2.3> join/leave group
		All devices participating in rotating cube will join a group named "RotateWithPeers".
		We'll join group when binding to group service is complete, and register group handler
		   to handle events:
		   onServiceConnected():
		   	mGroupService.joinGroup(groupId, null, mGroupHandler);
		leave group when app is destroyed
		   onDestroy():
		   	mGroupService.leaveGroup(groupId, mGroupHandler);
		   	
	2.4> application message
		We use RotateMsg class to send rotation information, with 
			3 message ids for initial orientation requests and orientation change events.
		In PeerDeviceNet api, messages are sent as binary blobs. We need marshal/serialize
			messages objects into binary blob and demarshal/deserialize them back.
			For this sample, we use android "Parcel" class. We could use JSON too.
			class RotateMsg {
				byte[] marshall() {...}
				void unmarshall(byte[] data, int len) {...}
			}
			
	2.5> send messages
		We use group service's async api method to send message. 
		When sending initial orientation requests and orientation change events, we 
			broadcast messages to all peers in group:
			mGroupService.send(groupId, null, msg.marshall());
		When replying to peer's initial orientation request, we send point-to-point
			message to just send to the requesting peer:
			mGroupService.send(groupId, requesting_peer, msg.marshall());
	
	2.6> receive messages
		To receive messages and other group communication events, define a handler
			object implementing IRouterGroupHandler aidl interface:
			mGroupHandler = new IRouterGroupHandler() {...}
		
			onSelfJoin(DeviceInfo[] devices):
				here we find out if there are existing peers in the group, 
				if so, send message requesting their orientation to sync initial orientation.
				
			onReceive(DeviceInfo src, byte[] b):
				here we start processing messages from peers.
				there are two kinds of messages: 
					initial orientation response and orientation change events;
					based on these events data, rotate the cube in GUI
				
			Please note that group handler's methods are executed in a thread pool
				managed by android runtime, while GUI changes should be done in main GUI thread.
				So create a android.os.Handler object with GUI thread and the above onReceive() method will
				forward event message to this handler object for processing.
				

			