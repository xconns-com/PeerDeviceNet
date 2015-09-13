Connector_wifidirect_hotspot
============================

This sample connector using Router's ConnectionService aidl api to discover and connect to peer devices. It uses android WifiP2pManager to create a p2p group with the current device as the group owner. This essentially creates a hotspot that can accept connections from legacy wifi devices as well as other p2p devices.

It doesn't embed router directly, instead invokes an external router's APIs embedded in another app such as ConnectionSettings app. 

1. In AndroidManifest.xml, add the following permission to enable access router APIs:

		<uses-permission android:name="com.xconns.peerdevicenet.permission.REMOTE_MESSAGING" />

   Also add permissions to access network facilities.

2. To access router's api, add peerdevicenet-api.jar in one of two ways:
             
        * download peerdevicenet-api.jar from MavenCentral(http://search.maven.org/#search|ga|1|peerdevicenet) and copy to project's "libs/" directory.
        * if you are using android's new gradle build system, you can import it as 'com.xconns.peerdevicenet:peerdevicenet-api:1.1.6'.

3. Router project also has a WifiDirectGroupManager which encapsulate the functionality to create and remove p2p groups, which we'll also copy here for reuse. 

	When calling WifiDirectGroupManager.createNetwork(), android's p2p framework will be invoked to create a p2p group. Router runtime will detect this new network and pass its info such as ssid and passcode to apps.

4. This connector has a single activity ConnectorByWifiIdl with a simple GUI:

		* a TextView showing info about wifi direct network: ssid and passcode
		* a button to start/stop peer search
		* a ListView showing found and connected peer devices
		* a button to shutdown and disconnect router


5. The major components for talking to ConnectionService:
	
		* connClient: the wrapper object to talk to ConnectionService aidl api.
		* connHandler: callback handler registered in connClient constructor.
		* mHandler: an os.Handler object; since connHandler methods run inside aidl threadpool and we can only update GUI inside main thread, so connHandler methods will forward messages to mHandler to perform real job.


6. These components are created and destroyed following normal life cycle conventions:

		* in activity's onCreate() or fragment's onCreateView(), start and bind to Router's ConnectionService (connClient/connHandler created). 
		* in onDestroy(), unbind from ConnectionService.
		* Please note we call startService() explicitly to start ConnectionService and then bind to it. Without calling startService(), when connector activity finishes and unbind from ConnectionService inside onDestroy(), RouterService will be also destroyed since nobody bind to it anymore. Connectors setup router's connections so that other apps (Chat, Rotate) can communicate, so we keep router alive by startService(). Router must be explicitly killed by first unbinding all clients and call stopService() with intent ACTION_ROUTER_SHUTDOWN.

7. During startup inside activity's onCreate(), we'll check if this device support WifiDirect or not. If not, a warning dialog is shown and app exits. 

	Also when a WifiDirect network is detected and activated, its info (ssid and passcode) will be shown in the textview for network info. To connect another device to this wifi direct group/hotspot, using sample connectors such as Connector_wifi_aidl, we need to manually choose ssid and enter passcode in system settings. A more advanced connector can transfer these info to peer devices using NFC or camera scanning QR code.

8. Typical interaction with ConnectionService involves a oneway call to API and router call back at connHandler to reply. During typical workflow for network detection, we give higher priority to WifiDirect network, always activate it when it is available, as in the following steps:

	8.1. find all networks this device is connected to, by calling connClient.getNetworks(). When handling callback GET_NEWORKS message, if no wifi direct network found, update GUI showing a message about it. otherwise go on to next step.

	8.2. detect which network is active in use for PeerDeviceNet communication, by calling connClient.getActiveNetwork. When handling callback message GET_ACTIVE_NETWORK, if active network is Wifi Direct, goto 8.4.; if a WifiDirect network is attached but not active, goto 8.3..

	8.3. activate the wifi direct network, by calling connClient.activateNetwork(). When handling callbakc message ACTIVATE_NETWORK, update GUI showing active network info (ssid and passcode) and go to 8.4.

	8.4. retrieve my device info in active network (ip addr, port), by calling connClient.getDeviceInfo(). When handling callback message GET_DEVICE_INFO, we can save device info and start search at 8.5.

	8.5. start searching for peer devices, by calling connClient.startPeerSearch(). There are 3 possible callbacks for this call:

		* SEARCH_START: search started, we can update GUI about it.

		* SEARCH_FOUND_DEVICE: a new peer device is found, we can perform authentication or connect to this device at step 8.6.

		* SEARCH_COMPLETE: either search time out or terminated by users.

	8.6. connect to found peer device, by calling connClient.connect(). There are 2 possible callbacks:

		* CONNECTED: add the connected device to list view
		
		* CONNECTION_FAILED: the message will contain an error code showing why connection failed (such as rejected by peer). We can show it in GUI or log it.


