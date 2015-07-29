package org.tom.aavs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;

public class Frame {

	public static int totalPoints = 4;  

	public List<PVector> v; 

	public Frame(			
			int px0, int py0,
			int px1, int py1,
			int px2, int py2,
			int px3, int py3
			) {

		List<PVector> temp = new ArrayList<PVector>();

		temp.add(new PVector(px0, py0));
		temp.add(new PVector(px1, py1));
		temp.add(new PVector(px2, py2));
		temp.add(new PVector(px3, py3));

		v = new ArrayList<PVector> (totalPoints);		

		// we need to sort vertices in clockwise order to later use as a opengl quad

		Set<PVector> tempSet = GrahamScan.getSortedPVectorSet(temp);
		
		v.addAll(tempSet);

	}

	public float getArea() {
		if (v.size() < 4) {
			return 0;
			
		} else {
						
			float area = 0;
			PVector first = v.get(0);
			PVector last = first;
			
			for (int i = 0; i < v.size(); i++) {
				PVector next = v.get(i);
				area += next.x * last.y - last.x * next.y;
				last = next;
			}
			area += first.x * last.y - last.x * first.y;
			return area / 2;			
		}
	}

	public PVector centroid() {				
		PVector res = new PVector (0,0);
		
		if (v.size() > 0) {
			for (PVector p : v) {
				res.x += p.x;
				res.y += p.y;
			}
			res.x /= v.size();
			res.y /= v.size();
		}
		
		return res;
	}
	
	public int getTrackedVerticesSize() {
		if (v == null) {
			return 0;
		}
		else {
			return v.size();
		}
	}
	public void draw(PApplet p5, float x, float y, float scale, PImage img) {

		p5.stroke(255);
		p5.pushMatrix();
		p5.translate(x, y);
		p5.scale(scale, scale);

		try {
			if (v.size() > 3) {
				p5.beginShape();
				p5.texture(img);
				p5.vertex (v.get(0).x , v.get(0).y , 0, 0);
				p5.vertex (v.get(1).x , v.get(1).y , img.width, 0);	
				p5.vertex (v.get(2).x , v.get(2).y , img.width, img.height);
				p5.vertex (v.get(3).x , v.get(3).y , 0, img.height);		
				p5.endShape(PConstants.CLOSE);
			}

			for (int i = 0; i < v.size(); i++) {
				p5.stroke(255, 0, 0);			
				p5.ellipse (v.get(i).x, v.get(i).y, 10, 10);
			}

		} catch (Exception e) { //TODO see why it does die from time to time, it makes no sense
			System.out.println(":(");
		}
		p5.popMatrix();

		
	}		


	public void draw(PApplet p5, float x, float y, float scale) {		
		p5.pushMatrix();
		p5.translate(x, y);
		p5.scale(scale, scale);

		if (v.size() >= 3) {
			p5.beginShape();
			p5.fill(200);
			p5.vertex (v.get(0).x , v.get(0).y , 0, 0);
			p5.vertex (v.get(1).x , v.get(1).y , 100, 0);		
			p5.vertex (v.get(2).x , v.get(2).y , 0, 100);
			p5.vertex (v.get(3).x , v.get(3).y , 100, 100);		
			p5.endShape(PConstants.CLOSE);
		}

		p5.popMatrix();

	}		
}
