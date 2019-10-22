import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

public class client1{

	
	static BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	static DataOutputStream outToServerSend = null;
	static BufferedReader inFromServerSend = null;
	static DataOutputStream outToServerReceive = null;
	static BufferedReader inFromServerReceive = null;
	
	static Socket clientSocket_send = null;//new Socket(IP, 6789);
    static Socket clientSocket_receive = null;//new Socket(IP, 6789);
    
	static String Username="";
    static String IP="";
    static String input ="Default";

	private static boolean close(){

		try {
			if(true){
				clientSocket_receive.close();
				clientSocket_send.close();
				return false;
			}
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}  
		return false;
	}

    public static void main(String argv[]) throws Exception
    {
        String sentence;
        String modifiedSentence;
        
        //parseRegister();//Read username and IP
        Username = argv[0];
        IP = argv[1];        
        
        //Make TCP connections for recieve and send
        /*Socket*/ clientSocket_send = new Socket(IP, 6789);
        /*Socket*/ clientSocket_receive = new Socket(IP, 6789);
        
        //Set input and output for SEND socket
        /*DataOutputStream*/ outToServerSend = new DataOutputStream(clientSocket_send.getOutputStream());
        /*BufferedReader*/ inFromServerSend = new BufferedReader(new InputStreamReader(clientSocket_send.getInputStream()));
        
        //Set input and output for RECEIVE socket
        /*DataOutputStream*/ outToServerReceive = new DataOutputStream(clientSocket_receive.getOutputStream());
        /*BufferedReader*/ inFromServerReceive = new BufferedReader(new InputStreamReader(clientSocket_receive.getInputStream()));
        
        //REGISTERING TO SEND
		//send username to server to register tosend
		outToServerSend.writeBytes("REGISTER TOSEND "+ Username +"\n\n");
		input=inFromServerSend.readLine();		//read
		//loop till valid username entered		
		while(input .equals ("ERROR 100 Malformed Username")||input.equals ("ERROR 105 Username Taken")){
			if(input .equals ("ERROR 100 Malformed Username"))
				System.out.println("~Invalid Username🚫\nPlease enter a valid username");
			else 
				System.out.println("~Username Already Taken⚠\nPlease enter a different username");
		 	input=inFromServerSend.readLine();	// \n ##Do we have to check?
			Username =  inFromUser.readLine();	//get new username
        	outToServerSend.writeBytes("REGISTER TOSEND "+ Username +"\n\n");
        	input=inFromServerSend.readLine();
        }
        //registered to send
        if(input .equals ("REGISTERED TOSEND "+ Username)){
        	System.out.println("~~Registered tosend 📤");
        }
        else{//DEAD
        	System.out.println("~~Unable to register to Send 😕\nWith Error: "+input);
        	//close();
        	return ;
        }
        input=inFromServerSend.readLine();	// \n
        
        //REGISTERING TO RECEIVE
        outToServerReceive.writeBytes("REGISTER TORECV "+ Username + "\n\n");
        input= inFromServerReceive.readLine();
		if(input .equals ("REGISTERED TORECV "+ Username)){
            System.out.println("~~Registered torecv 📥");
        }
        else{//DEAD
        	System.out.println("~~Unable to register to Receive 😕\nWith error: "+input);
        	//close();
        	return ;
        }
       	input=inFromServerReceive.readLine();	// \n
       	
        //User Registerd Successfully 
		System.out.println("~~User Registered Successfully!☑"); //## Remove this line

		try {
		   		//Make Threads
			SendThread threadSend = new SendThread();
			ReceiveThread threadReceive = new ReceiveThread();
			Thread thrdSend = new Thread(threadSend);
			Thread thrdReceive = new Thread(threadReceive);
			thrdSend.start();
			thrdReceive.start();

		} catch (Exception e) {
		   e.printStackTrace();
		}
		
    }
    
    //Sub classes for threads
static class SendThread implements Runnable {
	String username ="";
	String message = "";
	String sentence = "";
	
	private int split(){
		int i;
		if(sentence.length()<3)
			return -3;
		if(sentence.charAt(0)=='#'){	//might be quit application
			if(sentence.length()<6)
				return -3;
			if(!sentence.equals("#CLOSE"))
				return -3;
			return 1;
		}
		if(sentence.charAt(0)!='@')
			return -1;
		
		for(i=1;i<sentence.length();i++)
			if(sentence.charAt(i)==' ')
				break;
		
		if(i==sentence.length())
			return -2;
		username = sentence.substring(1,i);
		message = sentence.substring(i+1,sentence.length());
		return 0;
	}
	
	public boolean deregister(){
	try {
		outToServerSend.writeBytes("UNREGISTER\n\n");
		sentence=inFromServerSend.readLine();
		if(sentence.equals("UNREGISTERED")){
			System.out.println("~Unregistered from SEND");
			return true;
		}
		System.out.println("~Unable to Unregister form SEND");
		return false;
	}
	catch (Exception e) {
			e.printStackTrace();
	}
		return false;
	}
	
	public void run(){
		try{
		while(true) {
		
            sentence = inFromUser.readLine();
                
			//Split sentence into username and content then send
			int splt = this.split();
			if(splt ==-1){
				System.out.println("~Please specify username after '@'");
				continue;
			}
			else if(splt == -2){
				System.out.println("~Please add space between username and message");
				continue;
			}
			else if(splt == -3){
				System.out.println("~Please write in format '@[username] [message]' OR Write '#CLOSE' to close application");
				continue;
			}
			else if(splt == 1){	//Deregister and Close application
				boolean dereg = deregister();
				if(dereg)	//CLose application
					return;
				else
					continue;	//continue taking input
			}
				
			//send well formed message ## Do we need to send each line separatelydat??
			outToServerSend.writeBytes("SEND "+username+"\nContent-length: "+message.length()+"\n"+"\n"+message);
			System.out.print("✔");	//sent tick
			
			//reply from server ack or error
			sentence =inFromServerSend.readLine();
			 
			if(sentence.equals("ERROR 101 No user registered"))
				System.out.println("\n~You are not registered yet");
			else if(sentence.equals("ERROR 102 Unable to send"))
				System.out.println("\n~Unable to send to "+username);				
			else if(sentence.equals("ERROR 103 Header incomplete")){
				System.out.println("\n~Header incomplete");
				//Socket might have been closed attempt to reconnect
				//##this.reconnect();
			}
			else if(sentence.equals("SENT "+username))
				//System.out.println("~Message Delivered Successfully");
				System.out.println("✔");	//delivered ticl
			else if(sentence.equals("ERROR 106 User Not Found"))
				System.out.println("\n~User "+username+" does not exits");
			else{
				System.out.println("\nError detected");	
			}
			sentence = inFromServerSend.readLine();// \n
        }
    }catch(Exception e){e.printStackTrace();}
	}
}

static class ReceiveThread implements Runnable{

	String sentence = null;
	String username = "";
	char[] message =new char[10000];
	int len = 0;
	
	private int checkHeader(){
	try {

		sentence = inFromServerReceive.readLine();	//FORWARD [username]
		if(sentence.length()<9)
			return -1;	//Incomplete header
		if(!sentence.substring(0,7).equals("FORWARD")){
			if(sentence.length()<10)
				return -1;
			else if(sentence.equals("UNREGISTERED"))	//You have been unregistered
				return 1;	//promt to close thread
			return -1;	//incomplete header
		}
		username = sentence.substring(8);		//save username	
		
		sentence = inFromServerReceive.readLine();	//Content-length: [length]
		if(sentence.length()<16)
			return -2;	//incomplete header
		if(!sentence.substring(0,15).equals("Content-length:"))
			return -2;	//content length  field missing ##close?
		len = Integer.parseInt(sentence.substring(16));	// length
		
		sentence = inFromServerReceive.readLine();	// \n
		if(!sentence.equals(""))
			return -3;	// \n missing
		
		//Read character by character
		inFromServerReceive.read(message,0,len);	//(char[],offset,lenght)
		
		
		return 0;
	}
	catch (Exception e) {
		e.printStackTrace();
	}
		return -4;
		
	}
	
	private void printMessage(){
		for(int i=0;i<len;i++)
			System.out.print(message[i]);
		System.out.println("");		
	}
	
	public void run(){
		try{
		while(true) {
			
			int ch = checkHeader();
			if(ch==1){	//deregister
				System.out.println("~Unregistered from RECV");
				System.out.println("XXX---Closing Application---XXX");
				return;
			}
			if(ch<0){
				outToServerReceive.writeBytes("ERROR 103 Header incomplete\n\n");
				System.out.println("~Header Incomplete~");
				System.out.println(sentence);
				
				sentence =null;
				//close connection
				//## how to close
				return;
			}
			if(ch==0){
				outToServerReceive.writeBytes("RECEIVED "+username+"\n\n");
				System.out.print("			#"+username+": ");
				printMessage();
			}
        }	
        }catch(Exception e){e.printStackTrace();}
	}
}
}


