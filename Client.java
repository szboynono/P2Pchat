/**
 * Assignment 2
 * This program was developed base on the my assignment 1.
 * Last name:Liu

 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client extends Thread{
	/**Client's Socket*/
	private Socket clientSocket;
	/**get message from the Server*/
	private BufferedReader reader;
	/**Send message to the Server*/
    private PrintWriter writer;
    /** Socket is used when two clients are connected, used as a client in p2p mode*/
    private Socket chatClientSocket;
    /** Socket is used when two clients are connected, used as a server in p2p mode*/
    private ServerSocket chatServerSocket;
    /**In p2p mode, Send message to other side*/
    private PrintWriter chatWriter;
    /**
     * This Blocking queue is used when: in the the P2P mode, when the Client act as a Server and ChatServerThread setup the 
     * connection with another client successfully. Then the Blocking queue will store the object of PrintWriter and send message in Client class.
     */
    private BlockingQueue<PrintWriter> writerQueue = new LinkedBlockingQueue<PrintWriter>();
    /**port that use for chat*/
    private int chatPort;
	public Client(String ip,int port){
			//connect with the Server
	try {
			this.clientSocket = new Socket(ip,port);
			System.out.println("connect to server at IP : "+ip+" and port "+port+" at port : "+clientSocket.getLocalPort());
			reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream(), "utf-8"));
			writer = new PrintWriter(this.clientSocket.getOutputStream(),true);
			
			//open up a listener thread, keep track of the message sent by Server
			this.start();
			Scanner scan = new Scanner(System.in);
			//client input
			while(true){
				String msg = scan.nextLine();
				//quit p2p
				if(msg.equals("CHAT_QUIT")){
					this.sendToChat("CHAT_QUIT");
					this.closeChatServer();
					continue;
				}
				
				//if the message needed to be sent to another client via P2P connection 
				if(msg.startsWith("CHAT_TK:") && chatWriter!=null){
					sendToChat(msg);
					continue;
				}
				sendMsg(msg);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**Listener to the Server*/
	@Override
	public void run(){
		String msg = "";
        while (true) {
            try {
                msg = reader.readLine();
            } catch (IOException e) {
                System.out.println("disconnect!");
                break;
            }
            if(msg.startsWith("CHAT_ERROR")){
            	this.closeChatServer();//when the client which you requested can't connect to you
            	System.out.println("Connect to client error!");
            }else if(msg.startsWith("CHAT_SUCCESS")){
            	createChatServer();//create a ServerSocket by myself.
				sendMsg(msg);// Send the TCP connect request to the target Client via Server
				try {
					chatWriter = writerQueue.take();//When the client is connected by another client, get the matched printWriter to send messages.
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }else if(msg.startsWith("CHAT_RQ")){//get a request from a client and connect to it.
            	String []arr = msg.split(",");
            	this.connectToChatServer(arr[1], Integer.parseInt(arr[2]));
            }else{
            	System.out.println(msg);
            }
        }
	}
	/**Send messages to the Server*/
	 public void sendMsg(String msg) {
	      try {
	           writer.println(msg);
	           if(msg.equals("QUIT")){
	        	   this.sendMsg("CHAT_QUIT");
	        	   this.closeChatServer();
	        	   this.clientSocket.close();
	        	   System.exit(0);
	           }
	      } catch (Exception e) {
	    	  System.out.println(e.toString());
	      }
	 }
	 /**Set up a ServerSocket*/
	 public void createChatServer(){
		 chatPort = clientSocket.getLocalPort()+100;
		 try {
			chatServerSocket = new ServerSocket(chatPort);
			System.out.println("Start a listen for a client at port : "+chatPort);
			new ChatServerThread(chatServerSocket,writerQueue);
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	 }
	 /**
	  * close the server as a server
	  */
	 public void closeChatServer(){
		 try {
			 if(chatServerSocket!=null && !chatServerSocket.isClosed())
				 chatServerSocket.close();
			 System.out.println("chat server closed");
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	 }
	 /**
	  * connect to a server as client
	  * @param ip
	  * @param port
	  */
	 public void connectToChatServer(String ip,int port){
		 try {
			System.out.println("try connect to a client at IP :"+ip+" port :"+(port+100)+" ... ");
			chatClientSocket = new Socket(ip,port+100);
			System.out.println("connect to a client at IP :"+ip+" port :"+(port+100)+" success!");
			chatWriter = new PrintWriter(chatClientSocket.getOutputStream(),true);
			new ChatClientThread(chatClientSocket);
		} catch (IOException e) {
			e.toString();
		}
	 }
	 /**
	  * disconnect to a server as client
	  */
	 public void disconnectToChatServer(){
		 try {
			chatClientSocket.close();
			System.out.println("disconnect to the server");
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	 }

	 /**
	  * send message to client in chat
	  * @param msg
	  */
	 public void sendToChat(String msg){
		 chatWriter.println(msg);
	 }
	 public static void main(String[]args){
		 if(args.length!=2){
			 System.out.println("Usage : java <executor> <ip> <port> ");
			 return;
		 }
		 try{
				int port = Integer.parseInt(args[1]);
				new Client(args[0],port);
			}catch (Exception e) {
				System.out.println("Please enter the correct port!");
			}
	 }
}
///////////////////////////////////////////////////////////////////////
class ChatServerThread extends Thread{
    /**used to read from and write to the client*/
    private BufferedReader chatReader;
    private PrintWriter chatWriter;
    private BlockingQueue<PrintWriter> writerQueue;
    private Socket clientSocket;
    private ServerSocket serverSocket;
	public ChatServerThread(ServerSocket serverSocket,BlockingQueue<PrintWriter> writerQueue){
		this.serverSocket = serverSocket;
		this.writerQueue = writerQueue;
		this.start();
	}
	
	@Override
	public void run(){
		String msg = "";
		try {
			clientSocket = serverSocket.accept();
			System.out.println(clientSocket.toString()+" connect to you!");
			chatReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "utf-8"));
			chatWriter = new PrintWriter(this.clientSocket.getOutputStream(),true);
			this.writerQueue.add(chatWriter);
			 while (true) {
		            try {
		                msg = chatReader.readLine();
		                if(msg==null) break;
		                if("CHAT_QUIT".equals(msg)) {
		            		System.out.println(clientSocket.toString()+"disconnect!");
		                	this.serverSocket.close();
		                	break;
		                }
		                msg = msg.replace(msg.split(":")[0], "");
		                System.out.println("message from "+clientSocket.getInetAddress()+":"+clientSocket.getPort()+">"+msg);
		            } catch (IOException e) {
		        		System.out.println(clientSocket.toString()+"disconnect!");
		            	this.serverSocket.close();
		                break;
		            }
		        }
		} catch (IOException e1) {
			System.out.println(e1.toString());
		}
	}
}
/////////////////////////////////////////////////////////////////////////////////////
class ChatClientThread extends Thread{
	private Socket clientSocket;
	private BufferedReader chatReader;
	
	public ChatClientThread(Socket clientSocket){
		try {
			this.clientSocket = clientSocket;
			chatReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "utf-8"));
			this.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void run(){
		String msg = "";
		System.out.println("begin to listen to "+clientSocket.toString()+" ...");
		while (true) {
		        try {
		            msg = chatReader.readLine();
		            if(msg==null) break;
		            if("CHAT_QUIT".equals(msg)) {
		            	this.clientSocket.close();
		            	break;
		            }
		            msg = msg.replace(msg.split(":")[0], "");
	                System.out.println("message from "+clientSocket.getInetAddress()+":"+clientSocket.getPort()+">"+msg);
		        } catch (IOException e) {
		            System.out.println(clientSocket.toString()+" disconnect!");
		            try {
						this.clientSocket.close();
						break;
					} catch (IOException e1) {
						e1.printStackTrace();
					}
		            break;
		        }
	    }
		 System.out.println(clientSocket.toString()+" disconnect!");
	}
}

