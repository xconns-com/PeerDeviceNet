ConnectionSettings
===================

This sample settings app allows people change the following preferences / parameters of peerdevicenet router services:

		* device name: which will be shown to peer devices to identify this device.
		* max sessions: max number of concurrent sessions with a peer device.
		* search timeout: Duration of peer device search session.
		* connection timeout: Timeout to wait for connection to peer device to be established.
		* liveness timeout: Timeout to check the liveness of connected peer device by heartbeat.
		* connection PIN: is PIN required for connecting peer devices.
		* auto scan: should this device start automatically a peer search session when join a network.
		* auto connect: should this device connect automatically to any device found during search.
		* auto accept: should this device automatically accept connect request from any device.
		* use ssl: should the connections between this device and peers use SSL.

It instantiates/embeds an Router instance which run in a background service process. Other apps (such as Connectors, Chat app, etc.) can invoke peerdevicenet-api calls to communicate with the router services and perform operations related to device connection and group communication.

The following intent can be used to start / bring up ConnectionSettings app:

		<action android:name="com.xconns.peerdevicenet.CONNECTION_SETTINGS" />
