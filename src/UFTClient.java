/*
 * Son Vuong and Anton Stoytchev
 * Computer Networking CS321
 * Prof. Choong-Soo Lee
 * Spring 2014
 * 
 * UDP File Transfer Client Application
 * 
 * This class represents a UDP File Transfer Client Application.
 * The client application sends a binary file to the Server Application 
 * The client application takes 3 system arguments:
 * 		1.) The Server Application IP
 * 		2.) The filename to be sent
 * 		3.) The size of the DatagramPackets. (max. 1000);
 * 
 */


import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class UFTClient {
	public static void main(String[] args) throws Exception{
		
		//initialize variables
		int eof=0;
		int sequence=0;
		int totaltrans=0, retrans=0,totalACK=0, dupACK=0,timeout=0;
				
		
		//take in the packetsize
		int packetsize=Integer.parseInt(args[2]);
		byte[] sendData = new byte[packetsize+3];
		
		//take in corruption rate and loss rate
		double corruptRate=Double.parseDouble(args[3]);
		double lossRate=Double.parseDouble(args[4]);
		
		//create socket to send
		int toPort =54321;
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress toAddress = InetAddress.getByName(args[0]);
		
		
		//take in the filename
		FileInputStream file= new FileInputStream( args[1]);
		
		//start timer
		double startTime = System.currentTimeMillis();
		
		while(eof==0){
		
			//read the file and put it in sendData
			int readsize=file.read(sendData,3,packetsize);
			
			
			//update eof and put it in sendData
			int testeof =file.available();
			if (testeof==0)
				eof=1;	
			sendData[2] = (byte) (eof);
			
			
			//update sequence value and append it to sendData to client
			sequence++;
			sendData[0] = (byte) (sequence/256);
			sendData[1] = (byte) (sequence%256);
			
			
			//compose and send packet
			DatagramPacket sendPacket=new DatagramPacket(sendData, 
						readsize+3, 
						toAddress, 
						toPort);
			
			//randomize to see if the packet is loss
			Random rand= new Random(); 
			boolean loss= (lossRate>=rand.nextDouble());
			
			//send packet if not loss-stimulus loss
			if(!loss){
				clientSocket.send(sendPacket);
				System.out.println("Sent Packet #"+sequence );
			}
			else
				System.out.println("Lost Packet #"+sequence );
			
			//increase total transmission
			totaltrans++;
			
			//prepare to receive respond packet
			byte[] receivedData = new byte[3];
			DatagramPacket receivedPacket = new DatagramPacket(receivedData,
					receivedData.length);	

			
			
			boolean ACKed=false;
			
			//while the packet is not yet acknowledged
			while (!ACKed){
				
					//	randomize to see if the packet is corrupt
			    	boolean corrupt= (corruptRate>=rand.nextDouble());
			    	totalACK++;
					//resend the packet if ACK is corrupt
					if(corrupt){		
						clientSocket.send(sendPacket);
						System.out.println("Received a Corrupt ACK");
						System.out.println("Retransmitted #"+sequence );
						totaltrans++;
						retrans++;
					}
					//if ACKed is not considered corrupted
					else{
						clientSocket.setSoTimeout(200);
						ACKed=true;
						try {
								clientSocket.receive(receivedPacket);
								byte[] sequenceData=Arrays.copyOfRange(receivedPacket.getData(),0,2);
								int sequenceReceived=((sequenceData[0] & 0xFF)*256)+(sequenceData[1] & 0xFF);
								
								//if the ACK doesn't contain the same sequence 
								if (sequenceReceived!=sequence){
									ACKed=false;
									dupACK++;	
								}
						} catch (SocketTimeoutException ste) {
							clientSocket.send(sendPacket);
							
							System.out.println("Timeout for Packet #"+sequence );
							System.out.println("Retransmitted #"+sequence );
							ACKed=false;
							totaltrans++;
							retrans++;
							timeout++;
						}
					}
			}
				
			
		}
		//End Timer
		double endTime = System.currentTimeMillis();
		double elapTime= endTime-startTime;
		System.out.println("Elapsed time: "+ elapTime+" ms");
		System.out.println("Total Packets Transmitted: " +totaltrans);
		System.out.println("Retransmitted Packets: "+retrans);
		System.out.println("Total ACK Received:" + totalACK);
		System.out.println("Duplicate ACK Received:" +dupACK);
		System.out.println("Total # of Timeouts:" + timeout);
		file.close();
		clientSocket.close();
		
	}

}
