/******************************************************************************************
 * 
 * Copyright (C) 2013 Zatta
 * original file by Curlymo
 * 
 * This file is part of pilight for android.
 * 
 * pilight for android is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by the 
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * pilight for android is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with pilightfor android.
 * If not, see <http://www.gnu.org/licenses/>
 * 
 * Copyright (c) 2013 pilight project
 ********************************************************************************************/

package by.zatta.pilight.connection;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class SSDPfinder {

	private static final String TAG = "SSDPFinder";
	private static String server = null;
	private static int port = 0;
	private static String line = null;
	static DatagramSocket ssdp;

	static String msg = "M-SEARCH * HTTP/1.1\r\n" + "Host:239.255.255.250:1900\r\n"
			+ "ST:urn:schemas-upnp-org:service:pilight:1\r\n" + "Man:\"ssdp:discover\"\r\n" + "MX:3\r\n\r\n";

	public static String findServerAndPort() {
		server = null;
		port = 0;
		line = null;
		find();
		// Log.v(TAG, server + ":" + Integer.toString(port));
		return server + ":" + Integer.toString(port);
	}

	public static InetSocketAddress pi() {
		server = null;
		port = 0;
		line = null;
		find();
		Log.v(TAG, server + ":" + Integer.toString(port));
		return new InetSocketAddress(server, port);
	}

	private static void find() {
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets)) {
				Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					if (!inetAddress.isLoopbackAddress() && (inetAddress.getClass().equals(Inet4Address.class))) {
						// Log.v(TAG, "checking: " + inetAddress.getHostAddress().toString());
						try {
							ssdp = new DatagramSocket(new InetSocketAddress(inetAddress.getHostAddress().toString(), 0));
							byte[] buff = msg.getBytes();
							DatagramPacket sendPack = new DatagramPacket(buff, buff.length);
							sendPack.setAddress(InetAddress.getByName("239.255.255.250"));
							sendPack.setPort(1900);
							try {
								ssdp.send(sendPack);
								ssdp.setSoTimeout(3000);
								boolean loop = true;
								while (loop) {
									DatagramPacket recvPack = new DatagramPacket(new byte[1024], 1024);
									ssdp.receive(recvPack);
									byte[] recvData = recvPack.getData();
									InputStreamReader recvInput = new InputStreamReader(new ByteArrayInputStream(recvData),
											Charset.forName("UTF-8"));
									StringBuilder recvOutput = new StringBuilder();
									for (int value; (value = recvInput.read()) != -1;) {
										recvOutput.append((char) value);
									}
									BufferedReader bufReader = new BufferedReader(new StringReader(recvOutput.toString()));
									Pattern pattern = Pattern.compile("Location:([0-9.]+):(.*)");
									while ((line = bufReader.readLine()) != null) {
										// Log.v(TAG, line);
										Matcher matcher = pattern.matcher(line);
										if (matcher.matches()) {
											server = matcher.group(1);
											port = Integer.parseInt(matcher.group(2));
											loop = false;
											break;
										}
									}
								}
							} catch (SocketTimeoutException e) {
								Log.w(TAG, "socketTimeoutEx 102");
							} catch (IOException e) {
								Log.w(TAG, "IOEXeption 104");
								ssdp.close();
							}
						} catch (UnknownHostException e) {
							Log.w(TAG, "Unknownhost 108");
						}
					}
				}
			}
		} catch (SocketException e) {
			Log.w(TAG, "SocketEx 114");
		}

		if (ssdp != null) {
			ssdp.close();
			ssdp = null;
		}
	}
}
