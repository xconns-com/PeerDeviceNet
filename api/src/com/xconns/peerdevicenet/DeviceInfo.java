/*
 * Copyright (C) 2013 Yigong Liu, XCONNS, LLC
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

package com.xconns.peerdevicenet;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Info of a device in peer-peer network:
 * its name, address and port number.
 */
public class DeviceInfo implements Parcelable {
    /**
     * Device's name in peer-peer network; started initially with Android OS build model name
     * and can be changed later by ConnectionService.setConnectionInfo().
     */
	public String name = null;
    /**
     * Device's address in peer-peer network.
     */
	public String addr = null;
    /**
     * Device's acceptance port number at which other peers can connect.
     */
	public String port = null;
    /**
     * Parcel marshaling setting.
     */
	public static final Creator<DeviceInfo> CREATOR = new Creator<DeviceInfo>() {
		public DeviceInfo[] newArray(int size) {
			return new DeviceInfo[size];
		}
		
		public DeviceInfo createFromParcel(Parcel in) {
			return new DeviceInfo(in);
		}
	};
	
	public DeviceInfo(Parcel in) {
		readFromParcel(in);
	}
	
	public DeviceInfo() {
		// TODO Auto-generated constructor stub
	}

	public DeviceInfo(String n,String a,String p) {
		name = n;
		addr = a;
		port = p;
	}
	
	@Override
	public String toString() {
		return String.format("%s (%s, %s)", name, addr, port);
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeString(addr);
		out.writeString(port);
	}

	public void readFromParcel(Parcel in) {
		name = in.readString();
		addr = in.readString();
		port = in.readString();
	}
}

