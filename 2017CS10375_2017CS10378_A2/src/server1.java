import java.io.*;
import java.net.*;
import java.util.*;
class server1 {
	
	static Hashtable<String,Socket> users = new Hashtable();
	//static ServerSocket welcomeSocket = null; 
	public static void main(String argv[]) throws Exception{
		ServerSocket welcomeSocket = new ServerSocket(6789);
		while(true) {
		
      		Socket connectionSocket = welcomeSocket.accept();

	        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
   	 	    DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

			//Create new thread
			try {
							
			SocketThread socketThread = new SocketThread(connectionSocket, inFromClient, outToClient);
     	   Thread thread = new Thread(socketThread);
     	   thread.start();
		
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
    
    
    private static boolean validUsername(String name){
    	int i;
   		for(i=0;i<name.length();i++)
        	if(!((name.charAt(i)<=90 && name.charAt(i) >=65 )||(name.charAt(i)<=122 &&name.charAt(i) >=97 )||(name.charAt(i)<=57 && name.charAt(i) >=48 ) ) )
                 break;
   		if(i==name.length())
   			return true;
   		return false;
    }
   
    //----------
	static class SocketThread implements Runnable {
		String sentence=null;
		String username=null;
		char[] message=new char[10000];
		String Message = "";
		String Username=null;
		int len =0;
     	Socket connectionSocket;
     	BufferedReader inFromClient;
     	DataOutputStream outToClient;

	    SocketThread (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient) {
		this.connectionSocket = connectionSocket;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
     	}

	private int checkHeader(){
	try {	
		sentence = inFromClient.readLine();	//SEND [username]
		if(sentence==null)	//Disconnected
			return 4;//Deregister
		if(sentence.length()==0)
			return 2;	//no data in line IGNORE
		if(sentence.length()<6)
			return -1;	//Incomplete header
		if(!sentence.substring(0,4).equals("SEND")){
			if(sentence.length()<10)	//error
				return -1;
			else if(sentence.equals("UNREGISTER"))
				return 3;
			return -2;	//incomplete header
		}
		
		Username = sentence.substring(5);		//save username	
		
		if(!users.containsKey(Username)){	//user not found
			return 1;
		}	
		sentence = inFromClient.readLine();	//Content-length: [length]
		if(sentence.length()<16)
			return -3;	//incomplete header
		if(!sentence.substring(0,15).equals("Content-length:"))
			return -4;	//content length  field missing ##close?
		len = Integer.parseInt(sentence.substring(16));	// length
		
		sentence = inFromClient.readLine();	// \n
		if(!sentence.equals(""))
			return -5;	// \n missing
		
		//Read character by character
		inFromClient.read(message,0,len);	//(char[],offset,lenght)
		
		return 0;
	}
	catch (Exception e) {
		e.printStackTrace();
	}
		return -4;
		
	}
	
	/*-----Waits for client to enter a valid and unique username-----*/
	private void getUsername(){
	try {
	
		while ((!validUsername(username))||users.containsKey(username)) { // wait till valid  and unique username
			if((!validUsername(username)))
				outToClient.writeBytes("ERROR 100 Malformed Username\n\n");
			else
				outToClient.writeBytes("ERROR 105 Username Taken\n\n");
			sentence = inFromClient.readLine();
			username= sentence.substring(16);
			sentence = inFromClient.readLine();	// \n	
		}
	}
	catch (Exception e) {
		e.printStackTrace();
	}
	}
	/*------END getUsername-------*/
	
	/*----Converts message from char array to string--*/
	private void getMessage(){
		Message="";
		for(int i=0;i<len;i++)
			Message+=message[i];
	}/*---END getMessage---*/
	
	public void run(){
	try{
	while(true){	//##DO we need this?
		sentence = inFromClient.readLine();	//first request from client
		/*-------Send--------*/
		if((sentence.substring(0,15)).equals("REGISTER TOSEND")){
			username = sentence.substring(16);
			sentence = inFromClient.readLine();	// \n
			getUsername(); // wait till valid and unique username
				
			outToClient.writeBytes("REGISTERED TOSEND "+username+"\n\n");	//send ack
			System.out.println("Registered ToSend: "+username);
			
			/*-----Registered to send, Wait for data from client-----*/
			while (true) { // wait for data to send
			   		
				int ch = checkHeader();
				
				if(ch==2){
					continue;
				}
				if(ch==4){	//Deregister
					System.out.println("UNREGISTERED ^C: "+username);
					users.remove(username);
					return;
				}
				if(ch==3){	//Unregister
					outToClient.writeBytes("UNREGISTERED\n\n");
					System.out.println("UNREGISTERED: "+username);
					//also send unregister on RECEIVE
					DataOutputStream outToClientR = new DataOutputStream((users.get(username)).getOutputStream());
					outToClientR.writeBytes("UNREGISTERED\n\n");
					users.remove(username);	//Remove user from hash table
					return;	//close thread;
				}
				if(ch<0){
					outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
					System.out.println("~Header Incomplete~");
					System.out.println(sentence);
				
					sentence =null;
					//close connection
					//## how to close
					return;
				}
				else if(ch==1){	//User not found
					//Clear input by creatinf new instance of buffered reader
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					System.out.println("User "+Username+" Not Found");
					
					outToClient.writeBytes("ERROR 106 User Not Found\n\n");
				}
				else if(ch==0){//###CONTINUE form here forward message
					//Extract destination username

					BufferedReader inFromClient2 = new BufferedReader(new InputStreamReader((users.get(Username)).getInputStream()));
   	 	    		DataOutputStream outToClient2 = new DataOutputStream((users.get(Username)).getOutputStream());
   	 	    		getMessage();	//convert char array to string
					outToClient2.writeBytes("FORWARD "+username+"\nContent-length: "+len+"\n"+"\n"+Message);
					System.out.print(username+" 📨 "+Username+": "+Message+"\n");

//					//Get acknowlegdement
					System.out.print("✔");	//sent tick
			
					//reply from server ack or error
					sentence =inFromClient2.readLine();
				
					if(sentence.equals("ERROR 103 Header incomplete")){
						System.out.println("\n~Header incomplete");
						outToClient.writeBytes("ERROR 102 Unable to send\n\n");
						//Socket might have been closed attempt to reconnect
						//##this.reconnect();
					}
					else if(sentence.equals("RECEIVED "+username)){
						//System.out.println("~Message Delivered Successfully");
						System.out.println("✔");	//delivered ticl
						outToClient.writeBytes("SENT "+Username+"\n\n");
					}
					else{
						System.out.println("\nError detected");	
						outToClient.writeBytes("ERROR 102 Unable to send\n\n");
					}
					sentence = inFromClient2.readLine();// \n
				}
			}
		}
		/*----Recieve-----*/
		else if((sentence.substring(0,15)).equals("REGISTER TORECV")){
			username = sentence.substring(16);
			sentence = inFromClient.readLine();	// \n
			//Add user in hashtable
			getUsername();	//get Valid and unique username
			users.put(username,connectionSocket);	//add user to data base
			outToClient.writeBytes("REGISTERED TORECV "+username+"\n\n");
			System.out.println("Registered ToReceive: "+username);
			return;
		}
		else{
			outToClient.writeBytes("ERROR 101 No user registered\n\n");
		}
	}//loop till registered
		//---------END
	}	
	catch (Exception e) {
		e.printStackTrace();
	}
	}//END Run
}//END server Thread

}

