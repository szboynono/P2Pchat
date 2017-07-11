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
import java.util.ArrayList;
import java.util.List;

public class Server {
	/**Server Socket*/
	private ServerSocket serverSocket;
	/**keep record of all the connections*/
	private List<Socket> clientList = new ArrayList<Socket>();
	public Server(int port){
		try {
			//Create a Server Socket
			serverSocket = new ServerSocket(port);
			System.out.println("Server begins to listen all the accepts from clients at port "+port+" ...");
			while(true){
				//listen to the clients' connection repeatedly
				Socket client = serverSocket.accept();
				System.out.println("client "+client.toString()+" connect to you!");
				//open a new thread for every Client
				new ListenThread(this,client);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * remove an client from the client list
	 * @param client
	 */
	public boolean removeClient(Socket client){
			clientList.remove(client);
			return true;
	}
	/**
	 * add the Client's socket in to the list
	 * if the Client's socket exist, then failed
	 * @param client
	 * @return
	 */
	public boolean addClient(Socket client){
		if(!clientList.contains(client)){
			clientList.add(client);
			return true;
		}
		return false;
	}
	/**
	 * invite a client to join a chat
	 * @param des_ip
	 * @param des_port
	 * @param src_ip
	 * @param src_port
	 * @return
	 * This method is used when: If Client A send a TCP request to Client B, Client A will run a TCP Server.
	 * Client B will connect to Client A's TCP Server as a client
	 */
	public boolean inviteClient(String des_ip, String des_port, String src_ip, String src_port){
		//loop to find the client that satisfies the requirement
		for(Socket client : clientList){
			if(client.getInetAddress().toString().endsWith(des_ip) && client.getPort()==Integer.parseInt(des_port)){
				try {
					PrintWriter chatWriter = new PrintWriter(client.getOutputStream(),true);
					Thread.sleep(200);// Delay to ensure the other client's ServerSocket has been set up, keep listening to the other side.
					chatWriter.println("CHAT_RQ"+","+src_ip+","+src_port);
					System.out.println("send invitation to client : "+"CHAT_RQ"+","+src_ip+","+src_port);
					return true;
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("faild to find "+des_ip+":"+des_port);
		return false;
	}
	/**
	 * Get the Socket list for all the clients
	 * @return
	 */
	public List<Socket> getClientList(){
		return clientList;
	}
	/**
	 * get all the information of the Clients
	 * @param msg
	 */
	public String getClientListInfo(){
		if(clientList.size()==0) return "NO clients in list!";
		StringBuffer msg = new StringBuffer();
		int index = 0;
		for(Socket client : clientList){
			msg.append(++index+". IP Address : "+client.getInetAddress()+" port : "+client.getPort()+"\n");
		}
		return msg.toString();
	}
	public static void main(String[]args){
		
		
			int port = 29093;
			new Server(port);
		
	}
}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class ListenThread extends Thread{
	private Server server;
	private Socket clientSocket;
	private BufferedReader reader;
    private PrintWriter writer;
	public ListenThread(Server server,Socket client){
		this.server = server;
		this.clientSocket = client;
		try {
			reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "utf-8"));
			writer = new PrintWriter(client.getOutputStream(),true);
			this.start();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		String msg = "";
        while (true) {
            try {
                reader = new BufferedReader(new InputStreamReader(
                		clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                msg = reader.readLine();
                if(msg.equals("QUIT")){
                	System.out.println(clientSocket.getInetAddress()+":"+clientSocket.getPort()+" quit!");
                	this.server.removeClient(clientSocket);
                	break;
                }else if(msg.equals("LIST")){
                	this.sendMsg(server.getClientListInfo());
                }else if(msg.equals("LEAVE")){
                	if(!this.server.getClientList().contains(this.clientSocket)){
            			sendMsg("You didn't join!");
            			
            		}else if(this.server.removeClient(clientSocket)){
                		sendMsg("Leave success!");
                	}
                }else if(msg.equals("JOIN")){
                	if(!this.server.addClient(clientSocket)){
                		sendMsg("Your already exist in list!");
                	}else{
                		sendMsg("Join success!");
                	}
                }else if(msg.startsWith("CHAT_RQ")){
                	parseChat(msg);
                }else{
                	System.out.println("message from "+clientSocket.getInetAddress()+":"+clientSocket.getPort()+" > "+msg);
                }
            } catch (IOException e) {
            	System.out.println(clientSocket.getInetAddress()+":"+clientSocket.getPort()+" disconnect!");
            	this.server.removeClient(clientSocket);
                break;
            }
        }
	}
	/**
	 * make the connection for the two clients to chat
	 * @param msg
	 */
	public void parseChat(String protocol){
		protocol = protocol.replace("CHAT_RQ:", "");
		System.out.println(protocol);
		String[] arr = protocol.split(",");
		if(arr.length!=2){
			sendMsg("Check your protocal 'CHAT_RQ:des_ip,des_port'!");
			return;
		}
		String des_ip = arr[0];  //the wanted client's ip
		String des_port = arr[1];//the wanted client's port
		String src_ip = this.clientSocket.getInetAddress().toString().replaceAll("/", "");  //the source client's ip
		String src_port = String.valueOf(this.clientSocket.getPort());//the source client's port
		System.out.println("a client want to create a chat");
		if(des_ip.equals(src_ip) && des_port.equals(src_port)){
			sendMsg("CHAT_ERROR > You can't chat with yourself!");
			return;
		}
		if(!this.server.getClientList().contains(this.clientSocket)){
			sendMsg("CHAT_ERROR > You should join first!");
			return;
		}
		boolean ret = this.server.inviteClient(des_ip, des_port, src_ip, src_port);
		if(ret){
			sendMsg("CHAT_SUCCESS");
		}else{
			sendMsg("CHAT_ERROR");
		}
	}
	public void sendMsg(String msg){
		writer.println(msg);
	}
}