/******************************************************************************************
 *
 * Copyright (C) 2013 Zatta
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

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;

public enum Server {
	CONNECTION;
	private static final String TAG = "Zatta::Server";
	private transient static ReaderThread reader;
	private transient static WriterThread writer;
	private static Socket socket;
	private static BufferedReader bufferedReader = null;
	private static PrintStream printStream = null;
	private static boolean isWriting = false;
	private ArrayBlockingQueue<String> output;
	private ArrayBlockingQueue<String> command;
	private transient SetUp setupThread;

	public synchronized String setup() {
		disconnect();

		String toBeReturned = "";
		output = new ArrayBlockingQueue<String>(1);
		setupThread = new SetUp(output);
		setupThread.start();

		try {
			toBeReturned = output.take();
		} catch (InterruptedException e) {
			Log.w(TAG, "troubles taking output");
		}
		if (setupThread != null) {
			try {
				setupThread.finalize();
				setupThread = null;
			} catch (Throwable e) {
				Log.w(TAG, "couldn't stop setupThread");
			}
		}
		output.clear();
		output = null;
		return toBeReturned;
	}

	public synchronized void sentCommand(String message) {
		if (writer == null || !writer.isAlive()) {
			command = new ArrayBlockingQueue<String>(10);
			writer = new WriterThread(command);
			writer.setName("WriterThread");
			writer.start();
		}
		try {
			isWriting = true;
			command.put(message);
		} catch (InterruptedException e) {
			Log.w(TAG, "Failed to write command to queue");
			Log.w(TAG, e);
		}
	}

	public boolean isWriting() {
		return isWriting;
	}

	public synchronized void disconnect() {
		try {
			socket.close();
		} catch (Exception e) {
			Log.w(TAG, "couldn't close socket");
		}
		try {
			setupThread.finalize();
		} catch (Throwable e) {
			Log.w(TAG, "couldn't stop setupThread");
		}
		try {
			reader.interrupt();
		} catch (Throwable e) {
			Log.w(TAG, "couldn't stop reader");
		}
		try {
			writer.interrupt();
		} catch (Throwable e) {
			Log.w(TAG, "couldn't stop writer");
		}
		setupThread = null;
		reader = null;
		writer = null;
		bufferedReader = null;
		printStream = null;
	}

	private static class SetUp extends Thread {
		InetSocketAddress adress = null;
		;
		private ArrayBlockingQueue<String> output;
		private Step step = Step.ADRESS;
		private int runs = 0;
		private String line = "";
		private JSONObject collectJSON = new JSONObject();

		SetUp(ArrayBlockingQueue<String> output) {
			Log.v(TAG, "constructed SetUp thread");
			this.output = output;
		}

		@Override
		protected void finalize() throws Throwable {
			Log.v(TAG, "Finalize the SetupThread");
			this.interrupt();
			super.finalize();
		}

		@Override
		public void run() {
			while (runs < 5) {
				Log.d(TAG, step.name() + " " + Integer.toString(runs));
				switch (step) {
					case ADRESS:
						runs++;
						if (runs == 2) {
							////// TEMPORARILY CONNECTING HARDCODED OVER THE INTERNET ///
							adress = new InetSocketAddress("my.home.adress", 1404);
							//Log.d(TAG, "OKAY " + adress.toString());
							step = Step.SOCKET;
							runs = 0;
							break;
						}
						try {
							adress = SSDPfinder.pi();
						} catch (Exception e1) {
							Log.w(TAG, "no adress by exception");
							break;
						}
						if (!(adress.toString().equals("null:0"))) {
							step = Step.SOCKET;
							runs = 0;
						}
						break;
					case SOCKET:
						runs++;
						try {
							socket = new Socket();
							socket.connect(adress, 5000);
							socket.setSoTimeout(8000);
						} catch (SocketException e) {
							Log.w(TAG, "SocketExeption " + e);
						} catch (IOException e) {
							Log.w(TAG, "IOExeption " + e);
						}
						if (socket.isConnected()) {
							step = Step.GUI;
							runs = 0;
						}
						break;
					case GUI:
						try {
							if (bufferedReader == null) {
								bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
							}
							if (bufferedReader.ready()) {
								if ((line = bufferedReader.readLine()) != null) if (line.equals("{\"status\":\"success\"}")) {
									step = Step.LIST;
									runs = 0;
									bufferedReader = null;
									break;
								}
							}
							if (printStream == null) {
								printStream = new PrintStream(socket.getOutputStream(), false);
							}
							String identify = "{\"action\": \"identify\",\"options\": {\"core\": 0,\"stats\": 0,\"receiver\": 0,\"config\": 1,\"forward\": 0},\"uuid\": \"0000-d0-63-00-000000\",\"media\": \"mobile\"}";
							printStream.print(identify);
							printStream.flush();
							Thread.sleep(1000);
						} catch (Exception e) {
							Log.w(TAG, e);
						}
						runs++;
						break;
					case LIST:
						try {
							if (bufferedReader == null) {
								bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
							}
							if (bufferedReader.ready()) {
								if ((line = bufferedReader.readLine()) != null) {
									String goodConfig = "{\"message\":\"config\"";
									if (line.contains(goodConfig)) {
										JSONObject json = new JSONObject(line);
										JSONObject config = json.getJSONObject("config");
										collectJSON.put("gui", config.getJSONObject("gui"));
										step = Step.VALUES;
										runs = 0;
										bufferedReader = null;
										break;
									}
								}
							}
							if (printStream == null) {
								printStream = new PrintStream(socket.getOutputStream(), false);
							}
							String request = "{\"action\": \"request config\"}\n";
							printStream.print(request);
							printStream.flush();
							Thread.sleep(1000);
						} catch (Exception e) {
							Log.w(TAG, e);
						}
						runs++;
						break;
					case VALUES:
						try {
							if (bufferedReader == null) {
								bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
							}
							if (bufferedReader.ready()) {
								if ((line = bufferedReader.readLine()) != null) {
									String goodConfig = "{\"message\":\"values\"";
									if (line.contains(goodConfig)) {
										JSONObject json = new JSONObject(line);
										collectJSON.put("values", json.getJSONArray("values"));
										step = Step.VALUES;
										runs = 6;
										bufferedReader = null;
										break;
									}
								}
							}
							if (printStream == null) {
								printStream = new PrintStream(socket.getOutputStream(), false);
							}
							String request = "{\"action\": \"request values\"}\n";
							printStream.print(request);
							printStream.flush();
							Thread.sleep(1000);
						} catch (Exception e) {
							Log.w(TAG, e);
						}
						runs++;
						break;
				}
			}
			try {
				if ((collectJSON.has("gui") && collectJSON.has("values"))) {
					output.put(collectJSON.toString());
					if (reader == null || !reader.isAlive()) {
						Log.v(TAG, "starting new reader from SetupThread");
						reader = new ReaderThread();
						reader.setName("ReaderThread");
						reader.start();
					} else {
						Log.v(TAG, "reader was still alive");
					}
				} else {
					output.put(step.name());
				}
			} catch (InterruptedException e) {
				Log.w(TAG, "ohnoooo, couldn't put output! Now everything hangs!");
			}
		}

		private enum Step {
			ADRESS, SOCKET, GUI, LIST, VALUES
		}
	}

	private static class WriterThread extends Thread {
		private ArrayBlockingQueue<String> command = null;

		WriterThread(ArrayBlockingQueue<String> command) {
			this.command = command;
		}

		@Override
		public void run() {
			while (!socket.isClosed() && !(socket == null) && !command.isEmpty()) {
				try {
					write(command.take());
				} catch (InterruptedException e) {
					Log.w(TAG, "Failed to write command to queue");
					Log.w(TAG, e);
				}
			}
			printStream = null;
			isWriting = false;
		}

		private boolean write(String message) {
			Log.v(TAG, "write called: " + message);
			try {
				if (printStream == null) {
					printStream = new PrintStream(socket.getOutputStream(), false);
				}
				printStream.print(message + "\n");
				printStream.flush();
				Thread.sleep(500);
				return true;
			} catch (Exception e) {
				Log.w(TAG, "couldn't write to the socket");
				return false;
			}
		}

	}

	private static class ReaderThread extends Thread {
		private String line;

		ReaderThread() {
		}

		@Override
		public void run() {
			Log.v(TAG, "reader started");
			while (!socket.isClosed() && !(socket == null)) {
				read();
			}
			bufferedReader = null;
			ConnectionService.postUpdate("LOST_CONNECTION");
		}

		public void read() {

			try {
				if (bufferedReader == null)
					bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				if (bufferedReader.ready()) {
					while ((line = bufferedReader.readLine()) != null) {
						int start = 0;
						start = line.indexOf("{");
						if (start == -1) start = 0;
						ConnectionService.postUpdate(line.substring(start));
					}
					bufferedReader = null;
					socket.close();
					Log.v(TAG, "stopped reading bufferedReader");
				}
			} catch (Exception e) {
				Log.w(TAG, "exception caught in read");
				Log.w(TAG, e);
			}
		}
	}

	/*
	 * END OF READER THREAD
	 */
}
