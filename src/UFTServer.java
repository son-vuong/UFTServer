/*
 * Anton Stoytchev
 * Computer Networking CS321
 * Prof. Choong-Soo Lee
 * Spring 2014
 * 
 * UDP File Transfer Server Application
 * 
 * This class represents a UDP File Transfer Server Application.
 * The Server Application opens a UDP socket and waits for any DatagramPackets sent by the client application.
 * The Server Application requires one system argument which is the name of the file that the server needs to write all the data incoming.
 * 
 */

import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

public class UFTServer {
	
	private static int TotalPacketsReceived = 0;
	private static int DuplicatePacketsReceived = 0;
	private static int TotalACKSent = 0;
	private static int DuplicateACKSent = 0;
	private static byte[] returnData;
	private static byte[] fileData;
		
	public static void main (String[] args) throws Exception {

		//Opens a binary file to write the bytes that the client sends.
		FileOutputStream WriteToFile = new FileOutputStream(args[0]);
		
		//Determine corruption and loss rate
		double corruptionRate = Double.parseDouble(args[1]);
		double lossRate = Double.parseDouble(args[2]);

		int sequence = 0;
				
		//Opens and binds the socket to port 54321.
		DatagramSocket serverSocket = new DatagramSocket(54321);

		byte[] data = new byte[1024];
		DatagramPacket receivedPacket = new DatagramPacket(data, data.length);
		
		byte[] relevantData = new byte[1003];
		relevantData[2]=0;
		
		//start timer
		double startTime = System.currentTimeMillis();

		while (relevantData[2] != (byte) 1) {
			//Receive the first data packet from the client
			serverSocket.receive(receivedPacket);			
			TotalPacketsReceived ++;
			
			//check if the packet is either corrupt or lost
			Random random = new Random();
			boolean checkCorrupt = (random.nextDouble() <= corruptionRate);
			boolean checkLoss = (random.nextDouble() <= lossRate);
						
			//Check if the packet received is corrupt
			if(!checkCorrupt){
				relevantData = Arrays.copyOf(receivedPacket.getData(), receivedPacket.getLength());
				//Separate the byte data from the acknowledgment packet data.			
				returnData = Arrays.copyOf(receivedPacket.getData(), 3);
				fileData = Arrays.copyOfRange(receivedPacket.getData(), 3, receivedPacket.getLength());
				
				//Check if the packet is a duplicate
				if(sequence == ((returnData[0] & 0xFF)*256) + (returnData[1] & 0xFF)){
					System.out.println("Received Packet #"+sequence+" (Duplicate)");
					DuplicatePacketsReceived ++;
				}else{				
					sequence = ((returnData[0] & 0xFF)*256) + (returnData[1] & 0xFF);
					System.out.println("Recieved Packet #"+sequence);
										
					//Packet is not corrupt. Write data to file.
					WriteToFile.write(fileData);
				}
			}else{
				System.out.println("Recieved a Corrupt Packet");
				DuplicateACKSent ++;
			}
			
			//Simulate a packet loss,guarantee the eof packet ACK is not lost 
			//and if the first packet is corrupt then assume that there is no return address to send back (ACK is Lost)
			if((checkLoss && (relevantData[2] != (byte) 1)) || (sequence==0)){
				System.out.println("Lost ACK #"+sequence);
				TotalACKSent ++;				
			}else{
				//Convert the Sequence Number back to Bytes.
				returnData[0] = (byte) (sequence/256);
				returnData[1] = (byte) (sequence%256);
				
				//Send the acknowledgment packets back to client
				InetAddress returnAddress = receivedPacket.getAddress();
				int returnPort = receivedPacket.getPort();
				DatagramPacket responsePacket = new DatagramPacket(returnData, returnData.length, returnAddress, returnPort);
				serverSocket.send(responsePacket);
				TotalACKSent ++;
				System.out.println("Sent ACK #"+sequence);
			}
		}

		//File transfer complete. Close all sockets.
		System.out.println("File Transferred Successfully");
		System.out.println("Closing all sockets");
		
		//End Timer
		double endTime = System.currentTimeMillis();
		double elapTime= endTime-startTime;
		
		//Print statistical information
		System.out.println("Elapsed time: "+ elapTime+" ms");
		System.out.println("Total Packets Received: "+TotalPacketsReceived);
		System.out.println("Duplicate Packets Received: "+DuplicatePacketsReceived);
		System.out.println("Total ACK Sent: "+TotalACKSent);
		System.out.println("Duplicate ACK Sent: "+DuplicateACKSent);
				
		serverSocket.close();
		WriteToFile.close();
	}	
}