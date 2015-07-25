package org.tom.aavs;


import processing.*;
import processing.core.*;
import processing.video.*;
import gab.opencv.*;

import java.awt.*;
import java.awt.List;
import java.util.*;

import org.openkinect.freenect.*;
import org.openkinect.processing.*;

import oscP5.*;
import netP5.*;

public class Client extends PApplet {

	private String serverIP = "127.0.0.1"; // "192.168.0.11"; // this should be taken from a txt
	private int serverPort = 11300;
	private String clientIP = "127.0.0.1"; // "192.168.0.1";
	private int clientPort = 11200;
	private int clientDatagramPort = 11100;
	NetAddress serverLocation;
	private String pp;

	private boolean transmitting = true;
	private boolean kinectPresent = false;

	//Capture video;
	OpenCV opencv;
	int cameraScaleFactor = 1;

	PImage dst;

	ArrayList<Contour> contours;
	ArrayList<Contour> polygons;

	Kinect kinect;

	float deg;

	boolean ir = true;
	boolean colorDepth = false;
	ArrayList<PVector> vertices;
	PImage img;

	int cameraWidth = 640;
	int cameraHeight = 480;

	final int totalVertices = 4;
	OscP5 oscP5;

	OscMessage trackMessage;
	ReceiverThread thread;

	boolean activeClient;

	public void setup() {
		size(1024, 768, P3D);

		activeClient = true;

		vertices = new ArrayList(totalVertices);

		if (kinectPresent) {
			kinect = new Kinect(this);
			//if (kinect.numDevices() != 1) exit();
			kinect.startDepth();
			kinect.startVideo();
			kinect.setIR(ir);
			kinect.setColorDepth(colorDepth);

			deg = kinect.getTilt();

			opencv = new OpenCV(this, cameraWidth, cameraHeight);
		}

		// video.start();
		img = loadImage("bridge.jpg");

		oscP5 = new OscP5(this, clientPort);
		serverLocation = new NetAddress(serverIP, serverPort);

		trackMessage = new OscMessage("/frame");

		thread = new ReceiverThread(this, cameraWidth, cameraHeight);
		thread.start();
	}


	public void draw() {

		background(0);

		if (kinectPresent) {
			scale(cameraScaleFactor);

			//opencv.loadImage(video);
			PImage kinectFrame = kinect.getVideoImage();
			opencv.loadImage(kinectFrame);
			opencv.gray();
			opencv.threshold(70);
			dst = opencv.getOutput();

			fill(255);  
			//	image(kinectFrame, 0, 0);

			noFill();
			strokeWeight(3);

			contours = opencv.findContours();
			int i = 0;

			for (Contour contour : contours) {

				stroke(0, 255, 0);
				//	contour.draw();
				Rectangle bbox = contour.getBoundingBox();

				if (i < totalVertices) { // we only store the first totalVertices blobs' coordinates
					vertices.add(new PVector ((float)bbox.getCenterX(), (float)bbox.getCenterY()));
				}
			}
		}

		if (!kinectPresent) {
			if (vertices.size() < 4) {
				vertices.clear();

				for (int i = 0; i < totalVertices; i++) {
					vertices.add(new PVector (random(cameraWidth), random(cameraHeight)));
				}
			}
		}


		stroke (255, 0 ,0);
		for (int i = 0; i < vertices.size(); i++) {
			ellipse(vertices.get(i).x, vertices.get(i).y, 10, 10);

		}

		// let's sort the vertices out
		Set<PVector> tempSet = GrahamScan.getSortedPVectorSet(vertices);
		vertices.clear();
		vertices.addAll(tempSet);

		if (transmitting) {

			int[] params = new int[vertices.size()*2];

			for (int i = 0; i < vertices.size(); i++) {
				params[2*i] = (int)vertices.get(i).x;
				params[2*i+1] = (int)vertices.get(i).y;				
			}

			trackMessage.clearArguments();
			trackMessage.add(params);

			oscP5.send(trackMessage, serverLocation);
		}


		if (thread.available()) {			
			img = thread.getImage();
		}

		if (activeClient) {

			if (vertices.size() >= 4) { // we have four blobs, we assume that they correspond to the four vertices of the frame			
				beginShape();
				texture(img);
				vertex (vertices.get(0).x, vertices.get(0).y, 0, 0);
				vertex (vertices.get(1).x, vertices.get(1).y, img.width, 0);		
				vertex (vertices.get(2).x, vertices.get(2).y, 0, img.height);
				vertex (vertices.get(3).x, vertices.get(3).y, img.width, img.height);
				endShape(CLOSE);
			}
		}

	}

	public void captureEvent(Capture c) {
		c.read();
	}

	public void keyPressed() {

		switch (key) {
		case 'd': 
			if (kinectPresent) {
				kinect.toggleDepth();
			}
			break;

		case 'r' :
			if (kinectPresent) {
				kinect.toggleVideo();
			}
			break;

		case 'i':
			if (kinectPresent) {
				ir = !ir;
				kinect.setIR(ir);
			}
			break;

		case 'c':
			if (kinectPresent) {
				colorDepth = !colorDepth;
				kinect.setColorDepth(colorDepth);
			}
			break;

		case 't':
			transmitting = !transmitting;
			System.out.println("transmitting: " + transmitting);
			break;

		case CODED:

			if (kinectPresent) {
				if (keyCode == UP) {
					System.out.println("up");
					deg++;
				} else if (keyCode == DOWN) {
					deg--;
				}
				deg = constrain(deg, 0, 30);
				kinect.tilt(deg);
			}
			break;

		case 'p':
			vertices.clear();

			for (int i = 0; i < totalVertices; i++) {
				vertices.add(new PVector (random(640), random(480)));
			}
			break;
		}
	}

	void oscEvent(OscMessage msg) {
		
		// if we get the message /active and we get the parameter "1" then we are the active, else we are not

		String adr = msg.address();		
		if(msg.checkAddrPattern("/active")==true) {			
			int a = (msg.get(0).intValue());			
			activeClient = a == 1;			
		}
		
		
	
	}


}