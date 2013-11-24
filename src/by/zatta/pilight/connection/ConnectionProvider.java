package by.zatta.pilight.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
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
	public static final String SERVERIP = "192.168.1.110"; //your computer IP address should be written here
	public static final int SERVERPORT = 5000;
	private enum Status {
		NO_CONNECTION,
		CONNECTING,
		FAILED,
		CONNECTED,
	}

	public synchronized String getCommandOutput(String command) {
		if (writer == null || !writer.isAlive() || reader == null || !reader.isAlive()) {
			
			Log.d(TAG, "setting up new connection");
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
			Log.d(TAG, "Interrupted while command_queue.put(command)");
			e.printStackTrace();
			return null;
		}
		try {
			output = output_queue.take();
			gimmeAnswer = false;
		} catch (InterruptedException e2) {
			Log.d(TAG, "Interrupted while output_queue.take()");
		}
		
		try {
			Log.d(TAG, "get output: " + command + " -> " + output);
		} catch (Exception e2) {
			Log.d(TAG, "Interrupted while output_queue.take()");
		}			
		return output;
	}
	
	public void finishTheWork(){
			Log.w(TAG, "called finishTheWork");
						
			try {connector.finalize();}	catch (Throwable e) { Log.w(TAG, "couldnt finalize the connector"); }
			try {writer.finalize();} 	catch (Throwable e) { Log.w(TAG, "couldnt finalize the writer"); }
			try {reader.finalize();} 	catch (Throwable e) { Log.w(TAG, "couldnt finalize the reader"); }
			
			if(socket != null) {
				try {socket.close();} 	catch (Exception e) { Log.w(TAG, "couldnt close the socket"); }
				socket = null;
			}
			
			if (output_queue != null) output_queue.clear();
			if (command_queue != null ) command_queue.clear();
			output_queue = null;
			command_queue=null;	
	}
	
	public synchronized boolean doConnect() {
		
		status = Status.NO_CONNECTION;
		connectionAttempts = 0;
		boolean toBeReturned =  (getCommandOutput("{\"message\":\"client gui\"}").contains("{\"message\":\"accept client\"}"));
		Log.d(TAG, "doConnect returns: " + toBeReturned);
		return toBeReturned;
	}
	
	public synchronized String getConfig() {
		return getCommandOutput("{\"message\":\"request config\"}");
	}
		
	public boolean stillConnected(){
		timeHeart = new Date().getTime();
		if (timeHeart - timeBeat < 4000){
			return true;
		}
		if (writer == null || !writer.isAlive() || reader == null || !reader.isAlive()){
			status = Status.NO_CONNECTION;
			return false;
		}
		try {
			command_queue.put("HEART");
			Thread.sleep(500);
		} catch (InterruptedException e) {
			status = Status.NO_CONNECTION;
			return false; 
		}
		if (timeHeart - timeBeat < 6000) {
			return true;
		}
		status = Status.NO_CONNECTION;
		return false;
	}
	
	//For testing purposes
	public void disturbConnetion(){
		Log.d(TAG, "disturbing, isnt it??");
		try {
			socket.close();
		} catch (Exception e) {
			socket = null;
		}
	}
	
/*
 * THREADS BELOW THIS POINT
 */
	
	private class ConnectorThread extends Thread{
		private ArrayBlockingQueue<String> command_queue;
		private ArrayBlockingQueue<String> output_queue;
		
		ConnectorThread(ArrayBlockingQueue<String> command_queue, ArrayBlockingQueue<String> output_queue) {
			this.command_queue = command_queue;
			this.output_queue = output_queue;
		}
		
		@Override
		protected void finalize() throws Throwable {
			Log.d(TAG, "Finalize the ConnectorThread");
			this.interrupt();
			super.finalize();
		}
		
		@Override
		public void run() {
			while(!(status == Status.CONNECTED) && !(status == Status.FAILED)) {
				ensure_connection();
			}
			Log.d(TAG, status.name());
		}
		
		public void ensure_connection() {
			if (status == Status.FAILED) return;
			if (status == Status.CONNECTED) return;
			init(SERVERIP, SERVERPORT);
		}

		public void init(String server, int port) {
			if (status == Status.NO_CONNECTION || status == Status.CONNECTING){
				connectionAttempts++;
			       try {
			    	   if(socket == null) {
			    		   socket = new Socket();
				           socket.setSoTimeout(30000);
			    	   }
			    	   if (!socket.isConnected())
			    		   socket.connect(new InetSocketAddress(server, port), 1000);
			       } catch (UnknownHostException e) {
			    	   Log.w(TAG, "UnknownHost");
			    	   status = Status.CONNECTING;
			       } catch(IOException e) {
			    	   Log.w(TAG, "IOException");
			    	   status = Status.CONNECTING;
			       }
			       if (socket.isConnected()){
			    	   Log.d(TAG, "socket made");
			    	   connectionAttempts = 0;
			    	   status = Status.CONNECTED;
			    	   
			    	   if (reader == null || !reader.isAlive()) {
			    		Log.w(TAG, "starting reader");
						reader = new ReaderThread(output_queue);
			   			reader.setName("ReaderThread");
			   			reader.start();
			   			}
			    	   if (writer == null || !writer.isAlive()) {
				    		Log.w(TAG, "starting writer");
							writer = new WriterThread(command_queue);
				   			writer.setName("WriterThread");
				   			writer.start();
				   		}    	   
			    	   return;
			       }else status = Status.CONNECTING;
			       
			       try { Thread.sleep(500); } catch (InterruptedException e) { Log.w(TAG, "no sleep"); }
			       
			       Log.d(TAG, "connectionAttempts: " + Integer.toString(connectionAttempts));
			       if (connectionAttempts > 10) {
			    	   status = Status.FAILED;
			    	   output_queue.add("Failed");
			       }
			}      
		}
	}

/*
 *  END OF CONNECTOR THREAD
 */
	
	private class WriterThread extends Thread {
		private ArrayBlockingQueue<String> command_queue = null;
		private PrintStream printStream = null;		

		WriterThread(ArrayBlockingQueue<String> command_queue) {
			this.command_queue = command_queue;
		}
		
		@Override
		protected void finalize() throws Throwable {
			Log.d(TAG, "Finalize the WriterThread");
			if(printStream != null) {
				printStream.close();
				printStream = null;
			}
			command_queue.clear();
			command_queue = null;
			status = Status.NO_CONNECTION;
			this.interrupt();
			super.finalize();
		}
		
		@Override
		public void interrupt(){
			Log.d(TAG, "interrupted writer");
			super.interrupt();
		}
		
		@Override
		public void run() {
			Log.w(TAG, "writer started");
			try {
			while (true){
					if (status == Status.CONNECTED){
						String command = command_queue.take();	
						write(command);
					} 
					if (status == Status.FAILED){
						command_queue.clear();
						output_queue.put("NO_CONNECTION");
						status = Status.NO_CONNECTION;
					}		
				}
			} catch (InterruptedException e) { Log.w(TAG, "writer interrupted"); }
			Log.w(TAG, "writer ended");
		}
		
		public boolean write(String message) {
			try {
				if(printStream == null) 
					printStream = new PrintStream(socket.getOutputStream(), false);
				printStream.print(message+"\n");
				printStream.flush();
				Thread.sleep(10);
				return true;
			} catch(Exception e) {
				status = Status.NO_CONNECTION;
				return false;
			}
		}
	}
	
/*
 * END  OF WRITER THREAD
 */
	
	private class ReaderThread extends Thread{
		private ArrayBlockingQueue<String> output_queue = null;
		private BufferedReader bufferedReader = null;
		private String line = null;
		
		ReaderThread(ArrayBlockingQueue<String> output_queue) {
			this.output_queue = output_queue;
		}
		
		@Override
		protected void finalize() throws Throwable {
			if(bufferedReader != null) {
				bufferedReader.close();
				bufferedReader = null;
			}
			output_queue.clear();
			output_queue = null;
			status = Status.NO_CONNECTION;
			line = null;
			this.interrupt();
			super.finalize();
		}
		
		@Override
		public void interrupt(){
			Log.d(TAG, "interrupted reader");
			super.interrupt();
		}
		
		@Override
		public void run() {
			Log.w(TAG, "reader started");
			while(status == Status.CONNECTED){
				read();	
			}
			Log.w(TAG, "reader ended");
		}
		
		public void read() {	
			try {
				if(bufferedReader == null)
					bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"), 1025);
				if(bufferedReader.ready()) {
					while((line = bufferedReader.readLine()) != null){
						timeBeat = new Date().getTime();
						try {
							Log.w(TAG, line);
							if(gimmeAnswer){
								output_queue.put(line);
							}else if (!line.equals("BEAT")){
								ConnectionService.postUpdate(line);
							}
						} catch (Exception e) { Log.w(TAG, "line leeg ofzo");	}
					}
					Log.w(TAG, "stopped reading bufferedReader");
				}
			} catch(Exception e1) {
				status = Status.NO_CONNECTION;
				Log.e(TAG, "problems in read()");
			}
		}
	}
	
/*
 * END OF READER THREAD
 */
	
}
