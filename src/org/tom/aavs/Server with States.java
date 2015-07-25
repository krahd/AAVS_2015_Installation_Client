package org.tom.aavs;

import processing.*;
import processing.core.*;
import processing.video.*;
import gab.opencv.*;

import java.awt.*;
import java.util.ArrayList;

import org.openkinect.freenect.*;
import org.openkinect.processing.*;

import oscP5.*;
import netP5.*;



public class Server extends PApplet {

	float viewX = 520;
	float viewY = 0;
	float viewHeight = 320;
	float viewWidth = 426;
	float viewSeparation = 10;

	int totalClients = 4;

	int OSCport = 11300;

	OscP5 oscP5;
	NetAddress[] clients;
	Frame[] trackedFrames;

	OscMessage trackMessage;

	boolean[] receivedTrackingClient;

	int state = 0;



	public void setup() {
		size (1400, 800, P3D);

		clients = new NetAddress[totalClients];
		trackedFrames = new Frame[totalClients];
		receivedTrackingClient = new boolean[totalClients];

		// todo get this info from txt file
		for (int i = 0; i < totalClients; i++) {
			clients[i] = new NetAddress("192.168.0." + (i+1), OSCport);
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
		oscP5 = new OscP5(this, OSCport);

		trackMessage = new OscMessage("/track");	
					
	}


	public void draw() {
		background(0);
		stroke(255);
		strokeWeight(2);
		fill(0);

		// 4 "views"
		rect(viewX + viewSeparation, viewY + viewSeparation, viewWidth, viewHeight);
		rect(viewX + viewWidth + viewSeparation * 2, viewY + viewSeparation, viewWidth, viewHeight);
		rect(viewX + viewSeparation, viewY + viewHeight + viewSeparation * 2, viewWidth, viewHeight);
		rect(viewX + viewWidth + viewSeparation * 2, viewY + viewHeight + viewSeparation * 2, viewWidth, viewHeight);

		rect(viewSeparation, viewSeparation, 510, 510);

		switch (state) {

		case STATE_ASKING:

			// ask for tracking data to all modules (OSC), wait till we have data from all modules (OSC)	

			for (int i = 0; i < totalClients; i++) {
				oscP5.send(trackMessage, clients[i]);	
			}
			state = STATE_WAITING;
			break;

		case STATE_WAITING:
			if (receivedMessagesFromAllClients()) {
				state = STATE_CALCULATING;
			}

			break;

		case STATE_CALCULATING: // we have the messages from all clients
			reset();

			/* 	
			 * need to do the following:

			  		locate frame in 3D space
					decide which module is the active module
					getFrameToProject (currentState, location3D)
					broadcast modules off
					send JPEG frame to active module
					send coordinates to active module

			 */

			//1 we need to locate the frame in 3D space our of the information we have from the four "eyes"



		default:			

		}				

		drawStatus();

		PImage img = loadImage("smile.jpg");

		trackedFrames[0].draw((PApplet)this, viewX + viewSeparation, viewY + viewSeparation, 0.66f, img);
		trackedFrames[1].draw((PApplet)this, viewX + viewSeparation * 2 + viewWidth, viewY + viewSeparation, 0.66f, img);
		trackedFrames[2].draw((PApplet)this, viewX + viewSeparation, viewY + viewHeight + viewSeparation * 2, 0.66f, img);
		trackedFrames[3].draw((PApplet)this, viewX + viewSeparation * 2 + viewWidth, viewY + viewHeight + viewSeparation * 2, 0.66f, img);



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
		text("State: ", viewSeparation, 540 + viewSeparation);
		text(""+ state, viewSeparation + 40, 540 + viewSeparation);
	}

	void oscEvent(OscMessage msg) {

		println("server: received " + msg.addrPattern() + ", typetad: " +
		msg.typetag() + ", from: " + msg.address());
		

		String adr = msg.address();
		String[] adrBytes = split (adr, '.');
		int clientNumber = new Integer(adrBytes [3]).intValue();
		
		println("server: client number (last ip byte): " + clientNumber);
		
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

			}
			break;
					
		}
	}

}






