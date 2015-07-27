/**
 * Pixelate  
 * by Hernando Barragan.  
 * 
 * Load a QuickTime file and display the video signal 
 * using rectangles as pixels by reading the values stored 
 * in the current video frame pixels array. 
 */

import processing.video.*;

//int numPixelsWide, numPixelsHigh;
int blockSize = 40;
Movie mov;
color movColors[];
int start;
int fadeRate; 

void setup() {
  size(640, 360);
  noStroke();
  mov = new Movie(this, "protestfire.mov");
  mov.loop();
 // numPixelsWide = width / blockSize;
 // numPixelsHigh = height / blockSize;
  println(numPixelsWide);
  movColors = new color[numPixelsWide * numPixelsHigh];
  start = 10; 
  fadeRate = 10; 
}

// Display values from movie
void draw() {
  
   if (frameCount > start){
       println(width / (frameCount - start)/fadeRate+blockSize); 
     if (width / ((frameCount - start)/fadeRate+blockSize) < width){
       pixelFocus(fadeRate, start, blockSize, movColors, mov);
     } else {
    pixelate(blockSize, numPixelsHigh, numPixelsWide, movColors, mov);
     }
  }
}

void pixelFade(int fadeRate, int startFrame, int blockSize, color colorArray[], Movie mov){
    int newBlockSize; 
    int pixelsHigh = height / blockSize;
    int pixelsWide = width / blockSize; 
  
   if ((frameCount - startFrame) % fadeRate == 0){
     newBlockSize = blockSize + ((frameCount-startFrame)/fadeRate);
   } else {
     newBlockSize = blockSize + floor((frameCount-startFrame)/fadeRate); 
   }
 
   pixelate(newBlockSize, pixelsHigh, pixelsWide, colorArray, mov);
 
 }

void pixelFocus(int fadeRate, int startFrame, int blockSize, color colorArray[], Movie mov){
   int newBlockSize; 
   int pixelsHigh = height / blockSize;
   int pixelsWide = width / blockSize;
  
   if ((frameCount - startFrame) % fadeRate == 0){
     newBlockSize = blockSize - ((frameCount-startFrame)/fadeRate);
   } else {
     newBlockSize = blockSize - floor((frameCount-startFrame)/fadeRate); 
   }
 
 
 pixelate(newBlockSize, pixelsHigh, pixelsWide, colorArray, mov);

}

void glitchPixelate(int blockSize, int pixelsHigh, int pixelsWide, color colorArray[], Movie mov){
    mov.read(); 
    mov.loadPixels();
    background(255);
    
    //for loop populates color Array with colors in frame
    int count = 0;   
    for (int j=0; j<pixelsHigh; j++){
      for(int i =0; i<pixelsWide; i++) {
        colorArray[count] = mov.get(i*blockSize, j*blockSize);
        count++;
      }
    }
   
    for(int j = 0; j < pixelsHigh; j++){
      for (int i = 0; i<pixelsWide; i++){
        fill(colorArray[j*pixelsWide+i]);
        rect(i*blockSize, j*blockSize, blockSize, blockSize); 
      }
    }

}

void pixelate(int blockSize, int pixelsHigh, int pixelsWide, color colorArray[], Movie mov){
  if(mov.available() == true){
    mov.read(); 
    mov.loadPixels();
    background(255);
    
    //for loop populates color Array with colors in frame
    int count = 0;   
    for (int j=0; j<pixelsHigh; j++){
      for(int i =0; i<pixelsWide; i++) {
        colorArray[count] = mov.get(i*blockSize, j*blockSize);
        count++;
      }
    }
   
    for(int j = 0; j < pixelsHigh; j++){
      for (int i = 0; i<pixelsWide; i++){
        fill(colorArray[j*pixelsWide+i]);
        rect(i*blockSize, j*blockSize, blockSize, blockSize); 
      }
    }
  }
}
