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

	private String serverIP = "127.0.0.1"; // "192.168.0.11"; should be taken from the config.
	private int serverPort = 11300;
	private int clientPort = 11200;
	NetAddress serverLocation;

	public static final boolean FULLSCREEN = true;

	private boolean transmitting = true;
	protected boolean receivingCommands = true;
	private boolean kinectPresent = true;
	private boolean debug = true;

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

	boolean calibrating;
	boolean keyCalibrating;
	boolean shiftPressed;

	PVector scaleFactor;
	PVector positionFactor;
	int calibratingVertex;
	PVector [] calibrationVertices;

	private final String CONFIG_FILE = "/Users/tom/devel/eclipse workspace/AAVS/bin/data/config.txt";

	Movie localMovie;

	public void setup() {
		//size(1024, 768, P3D);			
		size(displayWidth, displayHeight, P3D);
		frameRate(30);

		activeClient = true;
		calibrating = false;
		keyCalibrating = false;
		shiftPressed = false;

		scaleFactor = new PVector (1,1);
		positionFactor = new PVector (0, 0);

		vertices = new ArrayList(totalVertices);
		calibrationVertices = new PVector[4];

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

		img = loadImage("car.jpg");


		oscP5 = new OscP5(this, clientPort);
		serverLocation = new NetAddress(serverIP, serverPort);

		trackMessage = new OscMessage("/frame");

		thread = new ReceiverThread(this, cameraWidth, cameraHeight);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();


		float sx = 1;
		float sy = 1;
		float tx = 0;
		float ty = 0;

		try {
			String[] lines = loadStrings(CONFIG_FILE);  // load the configuration data
			/* lines format is as follows		 
		 int scaleX
		 int scaleY
		 int translateX
		 int translateY		 
			 */
			sx = new Float(lines[0]).floatValue();
			sy = new Float(lines[1]).floatValue();
			tx = new Float(lines[2]).floatValue();
			ty = new Float(lines[3]).floatValue();

		} catch (Exception e) {
			System.out.println("config file doesn't exist, using default values.");
		}

		scaleFactor.x = sx;
		scaleFactor.y = sy;
		positionFactor.x = tx;
		positionFactor.y = ty;

		strokeWeight(3);
		noFill();
	}

	private void doCalibration() {
		if (vertices.size() >= 4) {
			scaleFactor.x = (calibrationVertices[1].x - calibrationVertices[0].x) / (vertices.get(1).x - vertices.get(0).x);
			scaleFactor.y =  (calibrationVertices[2].y - calibrationVertices[1].y) / (vertices.get(2).y - vertices.get(1).y);

			positionFactor.x = calibrationVertices[0].x - vertices.get(0).x ;
			positionFactor.y = calibrationVertices[0].y - vertices.get(0).y;
		}
	}

	public void mouseClicked() {
		if (calibrating) {
			calibrationVertices[calibratingVertex] = new PVector(mouseX, mouseY);
			calibratingVertex++;

			if (calibratingVertex == totalVertices) {
				doCalibration();
				calibrating = false;			
			}
		} 
	}

	public void saveCalibration() {
		String [] lines = new String[4];
		lines[0] = "" + scaleFactor.x;
		lines[1] = "" + scaleFactor.y;
		lines[2] = "" + positionFactor.x;
		lines[3] = "" + positionFactor.y;
		saveStrings (CONFIG_FILE, lines);
		System.out.println("config saved");
	}

	public ArrayList<PVector> stupidOrdering(ArrayList<PVector> v) {
		ArrayList<PVector> res = new ArrayList<PVector>(4);

		if (v.size() < 4) return v;

		//lowest
		PVector v0 = v.get(0);
		int f = 0;

		for (int i = 1; i < v.size(); i++) {
			if (v.get(i).x + v.get(i).y < v0.x + v0.y) {
				v0 = v.get(i);
				f = i;
			}
		}		
		v.remove(f);

		// lowest y ! v0
		PVector v1 = v.get(0);
		for (int i = 1; i < v.size(); i++) {
			if (v.get(i).y < v1.y) { 
				v1 = v.get(i);
			}
		}

		// lowest x ! v0	
		PVector v3 = v.get(0);		
		for (int i = 1; i < v.size(); i++) {
			if (v.get(i).x < v3.x) { 
				v3 = v.get(i);		
			}
		}

		// highest x+y

		PVector v2 = v.get(0);	
		for (int i = 1; i < v.size(); i++) {
			if (v.get(i).x + v.get(i).y > v0.x + v0.y) {
				v0 = v.get(i);				
			}
		}		

		res.clear();
		res.add(v0);
		res.add(v1);
		res.add(v2);
		res.add(v3);


		return res;
	}
	

	public void draw() {

		if (debug) {
			background(128);
			fill (255,0,0);
			rect(0, 0, 50, 50);
			rect(width-50, 0, 50, 50);
			rect(0, height-50, 50, 50);
			rect(width-50, height-50, 50, 50);
			noFill();
		} else{
			background(0);
		}


		if (kinectPresent) {
			scale(cameraScaleFactor);

			//opencv.loadImage(video);
			PImage kinectFrame = kinect.getVideoImage();
			opencv.loadImage(kinectFrame);
			opencv.gray();
			opencv.threshold(70);
			dst = opencv.getOutput();
			vertices.clear();

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
		else { // kinect !present
			if (vertices.size() < 4) {
				vertices.clear();

				for (int i = 0; i < totalVertices; i++) {
					vertices.add(new PVector (random(cameraWidth), random(cameraHeight)));
				}
			}
		}


		pushMatrix();		
		translate(positionFactor.x, positionFactor.y);
		scale(scaleFactor.x, scaleFactor.y);

		if (vertices.size() > 0) {
			// sorting vertices
			Set<PVector> tempSet = GrahamScan.getSortedPVectorSet(vertices);

			vertices.clear();
			vertices.addAll(tempSet);

			// if (vertices.size() > 4) vertices = (ArrayList<PVector>) GrahamScan.getConvexHull(vertices);
			// Collections.sort(vertices, new TriangleVectorComparator(new PVector(0,0)));
			// vertices = stupidOrdering(vertices);
		}

		if (transmitting) {

			trackMessage.clearArguments();

			if (vertices.size() > 0) {

				int[] params = new int[vertices.size()*2];

				for (int i = 0; i < vertices.size(); i++) {
					params[2*i] = (int)vertices.get(i).x;
					params[2*i+1] = (int)vertices.get(i).y;				
				}
				trackMessage.add(params);
			}

			oscP5.send(trackMessage, serverLocation);
		}

		if (transmitting && !receivingCommands) {
			if (thread.available()) {			
				img = thread.getImage();
			}
		} 
		if (transmitting && receivingCommands) {
			if (localMovie != null) {
				img = localMovie;
			}
		}

		if (debug) {
			stroke (255, 0, 0);

		} else {
			noStroke();
		}

		if (activeClient) {

			if (vertices.size() >= 4) { // we have four blobs, we assume that they correspond to the four vertices of the frame
				beginShape();
				texture(img);
				vertex (vertices.get(0).x, vertices.get(0).y, 0, 0);
				vertex (vertices.get(1).x, vertices.get(1).y, img.width, 0);		
				vertex (vertices.get(2).x, vertices.get(2).y, img.width, img.height);
				vertex (vertices.get(3).x, vertices.get(3).y, 0, img.height);

				endShape(CLOSE);

			}
		}


		if (debug) {
			textSize(40);

			for (int i = 0; i < vertices.size(); i++) {
				ellipse(vertices.get(i).x, vertices.get(i).y, 10, 10);
				text("" + i , vertices.get(i).x + 15, vertices.get(i).y);
			}
		}

		popMatrix();


		if (calibrating) {
			fill(255);
			text("calibrating", 100, 700);
		}
		if (keyCalibrating) {
			fill(255);
			text("keyboard calibrating", 100, 700);
		}
	}

	public void captureEvent(Capture c) {
		c.read();
	}


	public void keyReleased(){
		if (keyCode == SHIFT) {
			shiftPressed = false;
		}
	}

	public void keyPressed() {

		switch (key) {
		
		case 'c':
			calibratingVertex = 0;
			calibrating = true;
			break;

		case 'C':
			keyCalibrating = !keyCalibrating;
			break;

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

		case 'k':
			if (kinectPresent) {
				colorDepth = !colorDepth;
				kinect.setColorDepth(colorDepth);
			}
			break;

		case 't':
			transmitting = !transmitting;
			System.out.println("transmitting: " + transmitting);
			break;

		case 'u':
			if (kinectPresent) {				
				deg++;
			}

			deg = constrain(deg, 0, 30);
			kinect.tilt(deg);
			break;

		case 'j':
			if (kinectPresent) {
				deg--;
			}

			deg = constrain(deg, 0, 30);
			kinect.tilt(deg);
			break;


		case CODED:
			float dx = 0, dy = 0;
			float sx = 0, sy = 0;
			float sf = 0.01f;

			if (shiftPressed) {
				switch (keyCode) {
				case UP: sy = -sf; break;
				case DOWN: sy = sf; break;
				case LEFT: sx = -sf; break;
				case RIGHT: sx = sf; break;			
				}
			} else {
				switch (keyCode) {
				case UP: dy = -1; break;
				case DOWN: dy = 1; break;
				case LEFT: dx = -1; break;
				case RIGHT: dx = 1; break;
				case SHIFT: shiftPressed = true; break;
				}
			}

			scaleFactor.x += sx;
			scaleFactor.y += sy;
			positionFactor.x += dx;
			positionFactor.y += dy;

			break;

		case 'p':
			vertices.clear();

			for (int i = 0; i < totalVertices; i++) {
				vertices.add(new PVector (random(640), random(480)));
			}
			break;

		case 's':
			saveCalibration();
			break;

		case ' ':
			debug = !debug;
			break;
		}
	}

	void oscEvent(OscMessage msg) {

		// if we get the message /active and we get the parameter "1" then we are the active, else we are not
		
		// String pattern = msg.addrPattern();
		// System.out.print("addr: " + pattern + "/");
		// System.out.println(msg.arguments()[0]);
		
		
		if(msg.checkAddrPattern("/active") == true) {			
			int a = (Integer) (msg.arguments()[0]);			
			activeClient = a == 1;			
			
		} else if (msg.checkAddrPattern("/play")) {			
			System.out.println("received play ");			
			String filename = (String) msg.arguments()[0];
			localMovie = new Movie(this, filename);
		}
	}

	void movieEvent(Movie m) {
		m.read();
	}

	static public void main(String args[]) { 
		if (FULLSCREEN) {
			PApplet.main(new String[] { "--present", "--bgcolor=#000000", "--hide-stop", "--present-stop-color=#000000", "org.tom.aavs.Client" });
		} else {
			PApplet.main(new String[] { "--bgcolor=#000000", "--hide-stop", "--present-stop-color=#000000", "org.tom.aavs.Client" });
		}

	}

}