package org.tom.aavs;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.video.Movie;


public class Server extends PApplet {

	float viewX = 520;
	float viewY = 0;
	float viewHeight = 320;
	float viewWidth = 426;
	float viewSeparation = 10;

	int totalClients = 4;
	int activeClient = 0;

	private int serverPort = 11300;
	private int clientPort = 11200;
	private int clientDatagramPort = 11100;

	OscP5 oscP5;
	String[] clients;
	Frame[] trackedFrames;

	NetAddress[] clientAddresses;
	OscMessage activeMessage, videoPlayMessage;

	boolean[] receivedTrackingClient;

	DatagramSocket ds;  // to stream video
	
	private PVector[] kinects;
	
	float stageSide = 10f;
	MIDI [] midiBackground;
	
	MIDI test;
	
	private boolean transmittingFrames = true;
	private boolean transmittingCommands = false;
	
	PVector frameCoordinates = new PVector(-1000, -1000);
	
	Movie currentVideo;
	String currentFilename;
	
	public void setup() {
		size (1400, 800, P3D);
		frameRate(30);
		currentFilename = "";
		
		trackedFrames = new Frame[totalClients];
		receivedTrackingClient = new boolean[totalClients];
		clients = new String[totalClients];
		clientAddresses = new NetAddress[totalClients];
		kinects = new PVector[totalClients];
	
		for (int i = 0; i < totalClients; i++) kinects[i] = new PVector();
	
		kinects[0].x = 0;
		kinects[0].y = 0;
		
		kinects[1].x = stageSide / 2;
		kinects[1].y = stageSide / 2;
		
		kinects[2].x = stageSide;
		kinects[2].y = 0;
		
		kinects[3].x = stageSide / 2;
		kinects[3].y = -stageSide / 2;
		
		
		// todo get this info from txt file
		for (int i = 0; i < totalClients; i++) {
			//clientAddresses[i] = new NetAddress("127.0.0" + (i+1), clientPort);   // 192.168.0."  FIXME
			clientAddresses[i] = new NetAddress("192.168.0" + (i+1), clientPort);   // 192.168.0."  FIXME
			clients[i] = "127.0.0." + (i+1); // clients we are writing to

			//trackedFrames[i] = new Frame (-100, -100, -100, -100, -100, -100, -100, -100);
			trackedFrames[i] = new Frame (
					this,
					(int)random (640), (int)random (480),
					(int)random (640), (int)random (480),
					(int)random (640), (int)random (480),
					(int)random (640), (int)random (480)
					);
			//200, 100, 100, 200, 200, 200);

			receivedTrackingClient[i] = false;
		}
		oscP5 = new OscP5(this, serverPort); // port we are listening to

		try {
			ds = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		activeMessage = new OscMessage("/active");
		videoPlayMessage = new OscMessage("/play");
		
		midiBackground = new MIDI[totalClients];
		for (int i = 0; i < totalClients; i ++) {
			midiBackground[i] = new MIDI(this, i);		 // second parameter is the device number
		}
		currentVideo = null;	
		
		test = new MIDI(this, 0);
		

	}
	
	public void movieEvent(Movie m) {
		  m.read();
	}
	
	public void sendImage (PImage img, String ip, int port) {
		// We need a buffered image to do the JPG encoding
	
		BufferedImage bimg = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB);

		// Transfer pixels from localFrame to the BufferedImage
		img.loadPixels();
		bimg.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);

		// Need these output streams to get image as bytes for UDP communication
		ByteArrayOutputStream baStream = new ByteArrayOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(baStream);

		// compress the BufferedImage into a JPG and put it in the BufferedOutputStream
		try {
			ImageIO.write(bimg, "jpg", bos);
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		// Get the byte array, which we will send out via UDP!
		byte[] packet = baStream.toByteArray();

		// Send JPEG data as a datagram
		// println("Sending datagram with " + packet.length + " bytes");

		// we send the frame to the active client

		//		System.out.println(packet.length);
		try {			
			ds.send(new DatagramPacket(packet, packet.length, InetAddress.getByName(ip), port));
		} 
		catch (Exception e) {
			e.printStackTrace();
		}	
	}

	private String getVideoFilename (PVector coords) {
		return "/Users/tom/devel/eclipse workspace/AAVS/bin/data/fingers640.mov";
	}

	public PImage getVideoFrame (PVector coords) {
		PImage img;
		// img = loadImage("car.jpg");
		
		// TODO select video in function of coords in getVideoFilename()
		
	
		if (!currentFilename.equals(getVideoFilename(coords))) {
			currentFilename = getVideoFilename(coords);
			currentVideo = new Movie (this, currentFilename); //TODO make it relative path			
			currentVideo.loop();
			
			if (transmittingCommands) {
				sendPlayCommand();
			}
			
		}
		
		
		img = currentVideo;		
		
	//	System.out.println(img.width + " " + img.height);
		
		
		//TODO
		/* get the corresponding frame from the video and return it
		 * also this method needs to manage the playing / stopping the videos
		 */
		return img;
	}


	public void draw() {
		background(0);
		stroke(255);
		strokeWeight(2);
		fill(0);

		// 4 "views"
		if (activeClient == 0) {
			stroke (255, 0, 0);			
		} else {
			stroke (255, 255, 255);
		}		
		rect(viewX + viewSeparation, viewY + viewSeparation, viewWidth, viewHeight);
		if (activeClient == 1) {
			stroke (255, 0, 0);			
		} else {
			stroke (255, 255, 255);
		}
		rect(viewX + viewWidth + viewSeparation * 2, viewY + viewSeparation, viewWidth, viewHeight);
		if (activeClient == 2) {
			stroke (255, 0, 0);			
		} else {
			stroke (255, 255, 255);
		}
		rect(viewX + viewSeparation, viewY + viewHeight + viewSeparation * 2, viewWidth, viewHeight);
		if (activeClient == 3) {
			stroke (255, 0, 0);			
		} else {
			stroke (255, 255, 255);
		}
		rect(viewX + viewWidth + viewSeparation * 2, viewY + viewHeight + viewSeparation * 2, viewWidth, viewHeight);
		stroke (255, 255, 255);
		rect(viewSeparation, viewSeparation, 510, 510);

		frameCoordinates.x = 0;
		frameCoordinates.y = 0;
		
		
		if (receivedMessagesFromAllClients()) {

			/* 	
			 * need to do the following:

			  		locate frame in 2D space
					decide which module is the active module
					getFrameToProject (currentState, location3D)
					broadcast modules off
					send JPEG frame to active module
					send coordinates to active module

			 */

			/*
			 *  locate frame in 2D space
			 
			 		find the frame with the biggest area (Active frame)
					find the secondary frame // 
					distance (depending on which frame active = x or y)
					distance to center = y or x
			
			draw the location on the location part of the interface
			
			*/
			
			int active = 0;
			float maxArea = trackedFrames[0].getArea();
			for (int i = 1; i < totalClients; i++) {
				if (trackedFrames[i].getArea() > maxArea) {
					maxArea = trackedFrames[i].getArea();
					active = i;
				}
			}
			
			int side = (active + 1) % 4;
			if (!(trackedFrames[side].totalPoints >= 2)) {
				side = (side + 2) % 4;				
			}
			
			
			if (active % 2 == 0) { 
				frameCoordinates.x = trackedFrames[active].centroid().x;
				frameCoordinates.y = trackedFrames[side].centroid().y;
			} else {
				frameCoordinates.x = trackedFrames[side].centroid().x;
				frameCoordinates.y = trackedFrames[active].centroid().y;
			}
			
			fill (255, 0, 0);
			ellipse (frameCoordinates.x, frameCoordinates.y, 10, 10);
			
								
			/*
						
			 if coordinate in area then 
					if not playing video load corresponding video; current frame = 0
			
						load frame
						if applyinf effect, apply effect (x,y)
						send frame 
						advance current frame
				
			else if coordinate in area-traversing (path)
				play corresponding sound
				if not video loaded, load video
				get corresponding frame (x,y)
				send frame
									
			
			update background sound(s)
			*/
			
			

		}	
		
		PImage img = getVideoFrame (frameCoordinates);

		activateClient(activeClient);
		
		if (transmittingFrames) {
			sendImage(img, clients[activeClient], clientDatagramPort);
		}
		
		drawStatus();

		trackedFrames[0].draw((PApplet)this, viewX + viewSeparation, viewY + viewSeparation, 0.66f, img);
		trackedFrames[1].draw((PApplet)this, viewX + viewSeparation * 2 + viewWidth, viewY + viewSeparation, 0.66f, img);
		trackedFrames[2].draw((PApplet)this, viewX + viewSeparation, viewY + viewHeight + viewSeparation * 2, 0.66f, img);
		trackedFrames[3].draw((PApplet)this, viewX + viewSeparation * 2 + viewWidth, viewY + viewHeight + viewSeparation * 2, 0.66f, img);

	}
	
	private void sendPlayCommand() {
		videoPlayMessage.clearArguments();
		videoPlayMessage.add(currentFilename);
		oscP5.send(videoPlayMessage, clientAddresses[activeClient]); 
		
	}

	private void activateClient(int which) {
		
		// fixme! (we only have one client in testing)  
		
		for (int i = 0; i < totalClients; i++) {
			activeMessage.clearArguments();
			
			int[] params = new int[1];
			if (i == which) {
				params[0] = 1;
			}
			else {
				params[0] = 0;
			}
					
 			activeMessage.add(params); 			
 			
			if (i == 0) { //TODO delete this is because we have only one client
				oscP5.send(activeMessage, clientAddresses[i]);
			}
		}
				
	}

	private boolean receivedMessagesFromAllClients() {

		boolean all = true; 

		for (int i = 0; i < totalClients && !all; i++) {
			all = all && receivedTrackingClient[i];
		}

		return all;
	}

	private void drawStatus() {
		textSize(14);

		fill(255);
	}

	void oscEvent(OscMessage msg) {

		// println("server: received " + msg.addrPattern() + ", typetad: " + msg.typetag() + ", from: " + msg.address());


		String adr = msg.address();
		String[] adrBytes = split (adr, '.');
		int clientNumber = new Integer(adrBytes [3]).intValue() - 1; // 192.168.0.1 -> client 0

		// println("server: client number (last ip byte): " + clientNumber);

		receivedTrackingClient[clientNumber] = true;

		int totalVertices =  msg.typetag ().length() / 2; // 2 coordiantes per vertex, 
		// and we are only sending a list of integers with the coordinates

		trackedFrames[clientNumber].v = new ArrayList<PVector>();

		for (int i = 0; i < totalVertices; i++) {
			PVector vertex = new PVector(((Integer) msg.arguments()[i*2]).intValue(), 
					((Integer) msg.arguments()[i*2+1]).intValue());

			trackedFrames[clientNumber].v.add(vertex);

		}
	}

	private void reset() {
		for (int i = 0; i < totalClients; i++) {
			receivedTrackingClient[i] = false;
		}
	}

	public void keyPressed() {
		switch (key) {
		case 'p':
			for (int i = 0; i < totalClients; i++) {
				trackedFrames[i] = new Frame (
						this,
						(int)random (640), (int)random (480),
						(int)random (640), (int)random (480),
						(int)random (640), (int)random (480),
						(int)random (640), (int)random (480)
						);
			}a
			break;
			
		case '0': case '1': case '2': case '3':			
			activeClient = keyCode-48;			
			backgroundSound (activeClient);
			break;
			
		case 'a':
			
				test.note(0,  127);
				
			
			break;
			
		case 's':
			
			for (int i = 0; i < totalClients; i++) {
				midiBackground[i].note(60, 128); 
			}
			break;
			
		case 'm':
			sendPlayCommand();
			break;
		}
	}
	
	public void backgroundSound(int activeClient) {
		
	}

}






