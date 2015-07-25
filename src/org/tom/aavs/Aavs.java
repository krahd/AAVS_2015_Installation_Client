package org.tom.aavs;



import processing.*;
import processing.core.*;
import processing.video.*;
import gab.opencv.*;

import java.awt.*;
import java.util.ArrayList;

import org.openkinect.freenect.*;
import org.openkinect.processing.*;

public class Aavs extends PApplet {

	//Capture video;
	OpenCV opencv;
	int scl = 1;

	PImage src, dst;

	ArrayList<Contour> contours;
	ArrayList<Contour> polygons;

	Kinect kinect;

	float deg;

	boolean ir = true;
	boolean colorDepth = false;
	PVector[] vertices;
	PImage img;
	
	int cameraWidth = 640;
	int cameraHeight = 480;

	public void setup() {
		size(640, 480, P3D);
		 
		vertices = new PVector[4];

		kinect = new Kinect(this);
		//if (kinect.numDevices() != 1) exit();
		kinect.startDepth();
		kinect.startVideo();
		kinect.setIR(ir);
		kinect.setColorDepth(colorDepth);

		deg = kinect.getTilt();
		// kinect.tilt(deg);

		//video = new Capture(this, width/scl, height/scl);

		opencv = new OpenCV(this, cameraWidth, cameraHeight);


		// video.start();
		img = loadImage("bridge.jpg");
	}

	public void draw() {
		background(0);
		scale(scl);

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
			
			if (i < 4) { // we only store the first 4 blobs' coordinates
				vertices[i] = new PVector ((float)bbox.getCenterX(), (float)bbox.getCenterY());
				stroke (255, 0 ,0);
				ellipse(vertices[i].x, vertices[i].y, 10, 10);
				i++;
			}
		}
		
		
		if (i == 4) { // we have four blobs, we assume that they correspond to the four vertices of the frame			
			beginShape();
			texture(img);
			vertex (vertices[0].x, vertices[0].y, 0, 0);
			vertex (vertices[1].x, vertices[1].y, 100, 0);		
			vertex (vertices[3].x, vertices[3].y, 0, 100);
			vertex (vertices[2].x, vertices[2].y, 100, 100);
			endShape();
		}

		
		/*
			stroke(255, 0, 0);
			beginShape();
			for (PVector point : contour.getPolygonApproximation ().getPoints()) {
				vertex(point.x, point.y);
			}
			endShape();
		}
		 */
	}

	public void captureEvent(Capture c) {
		c.read();
	}

	public void keyPressed() {
		if (key == 'd') {
			kinect.toggleDepth();
		} else if (key == 'r') {
			kinect.toggleVideo();
		} else if (key == 'i') {
			ir = !ir;
			kinect.setIR(ir);
		} else if (key == 'c') {
			colorDepth = !colorDepth;
			kinect.setColorDepth(colorDepth);
		} else if (key == CODED) {
			if (keyCode == UP) {
				System.out.println("up");
				deg++;
			} else if (keyCode == DOWN) {
				deg--;
			}
			System.out.println(deg);
			deg = constrain(deg, 0, 30);
			System.out.println(deg);
			kinect.tilt(deg);
		}
	}
}