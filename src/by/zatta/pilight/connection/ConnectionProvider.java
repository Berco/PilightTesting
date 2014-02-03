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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import android.util.Log;

public enum ConnectionProvider {
	INSTANCE;
	private static final String TAG = "Zatta::ConnectionProvider";
	private static Status status = Status.NO_CONNECTION;
	private static long timeHeart = new Date().getTime();
	private static long timeBeat = new Date().getTime();
	private int connectionAttempts = 0;
	private ArrayBlockingQueue<String> command_queue;
	private ArrayBlockingQueue<String> output_queue;
	private transient ConnectorThread connector;
	private transient WriterThread writer;
	private transient ReaderThread reader;
	private boolean gimmeAnswer;
	private Socket socket;

	private enum Status {
		NO_CONNECTION, CONNECTING, FAILED, CONNECTED,
	}

	public synchronized String getCommandOutput(String command) {
		// Log.v(TAG, "getCommandOutput");
		if (writer == null || !writer.isAlive() || reader == null || !reader.isAlive()) {
			// Log.v(TAG, "setting up new connection");
			output_queue = new ArrayBlockingQueue<String>(6);
			command_queue = new ArrayBlockingQueue<String>(10);
			connector = new ConnectorThread(command_queue, output_queue);
			connector.setName("SocketThread");
			connector.start();
		}
		String output = "";
		output_queue.clear();

		try {
			gimmeAnswer = true;
			command_queue.put(command);

		} catch (InterruptedException e) {
			// Log.w(TAG, "Interrupted while command_queue.put(command)");
			e.printStackTrace();
			return null;
		}
		try {
			output = output_queue.take();
			gimmeAnswer = false;
		} catch (InterruptedException e2) {
			// Log.w(TAG, "Interrupted while output_queue.take()");
		}

		try {
			// Log.v(TAG, "get output: " + command + " -> " + output);
		} catch (Exception e2) {
			// Log.w(TAG, "Interrupted while output_queue.take()");
		}
		return output;
	}

	public synchronized void finishTheWork() {
		// Log.v(TAG, "called finishTheWork");

		try {
			connector.finalize();
		} catch (Throwable e) {
			// Log.w(TAG, "couldnt finalize the connector");
		}
		try {
			writer.finalize();
		} catch (Throwable e) {
			// Log.w(TAG, "couldnt finalize the writer");
		}
		try {
			reader.finalize();
		} catch (Throwable e) {
			// Log.w(TAG, "couldnt finalize the reader");
		}

		writer = null;
		reader = null;

		if (socket != null) {
			// Log.v(TAG, "socket wasn't null");
			try {
				socket.close();
			} catch (Exception e) {
				// Log.w(TAG, "couldnt close the socket");
			}
			socket = null;
		}

		if (output_queue != null) output_queue.clear();
		if (command_queue != null) command_queue.clear();
		// Log.v(TAG, "queueu's cleared");
		output_queue = null;
		command_queue = null;
		// Log.v(TAG, "queueu's nulled");
	}

	public synchronized boolean doConnect() {
		status = Status.NO_CONNECTION;
		connectionAttempts = 0;
		boolean toBeReturned = (getCommandOutput("{\"message\":\"client gui\"}").contains("{\"message\":\"accept client\"}"));
		// Log.v(TAG, "doConnect returns: " + toBeReturned);
		return toBeReturned;
	}

	public synchronized String getConfig() {
		return getCommandOutput("{\"message\":\"request config\"}");
	}

	public synchronized void sendCommand(String command) {
		// Log.v(TAG, "sending command:" + "{\"message\":\"send\",\"code\":{" + command + "}}");
		command_queue.add("{\"message\":\"send\",\"code\":{" + command + "}}");
	}

	public boolean stillConnected() {
		// Log.v(TAG, "calling stillConnected");
		timeHeart = new Date().getTime();
		if (timeHeart - timeBeat < 5000) {
			return true;
		}
		if (writer == null || !writer.isAlive() || reader == null || !reader.isAlive()) {
			// Log.v(TAG, "writer or reader null of not alive");
			status = Status.NO_CONNECTION;
			return false;
		}

		try {
			// Log.v(TAG, "trying to write HEART");
			command_queue.put("HEART");
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// Log.v(TAG, "couldn't write HEART");
			status = Status.NO_CONNECTION;
			return false;
		}
		if (timeHeart - timeBeat < 8000) {
			// Log.v(TAG, "returning true");
			return true;
		}
		status = Status.NO_CONNECTION;
		return false;
	}

	// For testing purposes
	public void disturbConnetion() {
		Log.e(TAG, "disturbing, isnt it??");
		try {
			socket.close();
		} catch (Exception e) {
			socket = null;
		}
	}

	/*
	 * THREADS BELOW THIS POINT
	 */

	private class ConnectorThread extends Thread {
		private ArrayBlockingQueue<String> command_queue;
		private ArrayBlockingQueue<String> output_queue;

		ConnectorThread(ArrayBlockingQueue<String> command_queue, ArrayBlockingQueue<String> output_queue) {
			// Log.v(TAG, "constructed connector thread");
			this.command_queue = command_queue;
			this.output_queue = output_queue;
		}

		@Override
		protected void finalize() throws Throwable {
			// Log.v(TAG, "Finalize the ConnectorThread");
			this.interrupt();
			super.finalize();
		}

		@Override
		public void run() {
			while (!(status == Status.CONNECTED) && !(status == Status.FAILED)) {
				// Log.v(TAG, "run connector, about to ensure");
				ensure_connection();
			}
			// Log.v(TAG, "Ending connectorthread with: "+ status.name());
		}

		public void ensure_connection() {
			if (status == Status.FAILED) return;
			if (status == Status.CONNECTED) return;
			init();
		}

		public void init() {
			// Log.v(TAG, "init connection");
			if (status == Status.NO_CONNECTION || status == Status.CONNECTING) {
				connectionAttempts++;
				try {
					if (socket == null) {
						// Log.v(TAG, "socket was null");
						socket = new Socket();
						socket.setSoTimeout(30000);
					}
					if (!socket.isConnected()) socket.connect(SSDPfinder.pi(), 10000);
				} catch (UnknownHostException e) {
					// Log.w(TAG, "UnknownHost");
					status = Status.CONNECTING;
				} catch (IOException e) {
					// Log.w(TAG, "IOException");
					status = Status.CONNECTING;
				} catch (IllegalArgumentException e) {
					// Log.w(TAG, "IllegalArgumentException");
					status = Status.CONNECTING;
				}
				if (!(socket == null)) {
					// Log.v(TAG, "socket wasn't null");
					if (socket.isConnected()) {
						// Log.v(TAG, "socket made");
						connectionAttempts = 0;
						status = Status.CONNECTED;

						if (reader == null || !reader.isAlive()) {
							// Log.v(TAG, "starting new reader");
							reader = new ReaderThread(output_queue);
							reader.setName("ReaderThread");
							reader.start();
						} else {
							// Log.v(TAG, "reader was still alive");
						}
						if (writer == null || !writer.isAlive()) {
							// Log.v(TAG, "starting new writer");
							writer = new WriterThread(command_queue);
							writer.setName("WriterThread");
							writer.start();
						} else {
							// Log.v(TAG, "writer was still alive");
						}

						if (!(writer == null || !writer.isAlive() || reader == null || !reader.isAlive())) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
							}
							return;
						}
					} else status = Status.CONNECTING;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// Log.w(TAG, "no sleep");
				}

				// Log.v(TAG, "connectionAttempts: " + Integer.toString(connectionAttempts));
				if (connectionAttempts > 3) {
					status = Status.FAILED;
					output_queue.add("Failed");
				}
			}
		}
	}

	/*
	 * END OF CONNECTOR THREAD
	 */

	private class WriterThread extends Thread {
		private ArrayBlockingQueue<String> command_queue = null;
		private PrintStream printStream = null;

		WriterThread(ArrayBlockingQueue<String> command_queue) {
			// Log.v(TAG, "constructed writer thread");
			this.command_queue = command_queue;
		}

		@Override
		protected void finalize() throws Throwable {
			// Log.d(TAG, "Finalize the WriterThread");
			this.interrupt();
			super.finalize();
		}

		@Override
		public void interrupt() {
			// Log.v(TAG, "interrupted writer");
			if (printStream != null) {
				// Log.v(TAG, "printstream wasn't null");
				printStream.close();
				printStream = null;
			}
			if (!(this.command_queue == null)) {
				this.command_queue.clear();
				this.command_queue = null;
			}
			status = Status.NO_CONNECTION;
			super.interrupt();
		}

		@Override
		public void run() {
			// Log.v(TAG, "writer started");
			try {
				while (!(command_queue == null)) {
					if (status == Status.CONNECTED) {
						// Log.v(TAG, "run writer, status connected");
						String command;
						try {
							command = command_queue.take();
							write(command);
						} catch (Exception e) {
							// Log.w(TAG, "couldn't take command from queueu");
							status = Status.NO_CONNECTION;
						}

					}
					if (status == Status.FAILED) {
						// Log.v(TAG, "run writer, status failed");
						command_queue.clear();
						output_queue.put("NO_CONNECTION");
						status = Status.NO_CONNECTION;
					}
				}
			} catch (InterruptedException e) {
				// Log.w(TAG, "writer interrupted");
			}
			// Log.v(TAG, "writer ended");
		}

		public boolean write(String message) {
			// Log.v(TAG, "write called");
			try {
				if (printStream == null) {
					// Log.v(TAG, "creating new printStream");
					printStream = new PrintStream(socket.getOutputStream(), false);
				}
				printStream.print(message + "\n");
				printStream.flush();
				Thread.sleep(500);
				return true;
			} catch (Exception e) {
				// Log.w(TAG, "couldn't write to the socket");
				status = Status.NO_CONNECTION;
				return false;
			}
		}
	}

	/*
	 * END OF WRITER THREAD
	 */

	private class ReaderThread extends Thread {
		private ArrayBlockingQueue<String> output_queue = null;
		private BufferedReader bufferedReader = null;
		private String line = null;

		ReaderThread(ArrayBlockingQueue<String> output_queue) {
			// Log.v(TAG, "constructed reader thread");
			this.output_queue = output_queue;
		}

		@Override
		protected void finalize() throws Throwable {
			// Log.v(TAG, "finalize reader");
			this.interrupt();
			super.finalize();
		}

		@Override
		public void interrupt() {
			// Log.v(TAG, "interrupted reader");
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					// Log.w(TAG, "couldn't close buffered reader");
					e.printStackTrace();
				}
				bufferedReader = null;
			}
			if (!(this.output_queue == null)) {
				this.output_queue.clear();
				this.output_queue = null;
			}
			status = Status.NO_CONNECTION;
			line = null;
			super.interrupt();
		}

		@Override
		public void run() {
			// Log.v(TAG, "reader started");
			while (status == Status.CONNECTED) {
				read();
			}
			// Log.v(TAG, "reader ended");
		}

		public void read() {
			try {
				if (bufferedReader == null)
					bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"), 1025);
				if (bufferedReader.ready()) {
					while ((line = bufferedReader.readLine()) != null) {
						timeBeat = new Date().getTime();
						try {
							// Log.v(TAG, line);
							if (gimmeAnswer) {
								output_queue.put(line);
							} else if (!line.equals("BEAT")) {
								ConnectionService.postUpdate(line);
							}
						} catch (Exception e) {
							// Log.w(TAG, "line leeg ofzo");
						}
					}
					status = Status.FAILED;
					// Log.w(TAG, "stopped reading bufferedReader");
				}
			} catch (Exception e1) {
				status = Status.NO_CONNECTION;
				// Log.w(TAG, "problems in read()");
			}
		}
	}

	/*
	 * END OF READER THREAD
	 */

}
