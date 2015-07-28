package org.tom.aavs;


import processing.core.PApplet;
import rwmidi.*;

public class MIDI {
	
	MidiOutput output;
	PApplet parent;
	
	MIDI(PApplet _parent, int inputDeviceNumber) {
		MidiOutputDevice devices[] = RWMidi.getOutputDevices();
		
		
		for (int i = 0; i < devices.length; i++) {
			System.out.println("id: " + i + " - " + devices[i].getName());
		}
		
		
		output = devices[inputDeviceNumber].createOutput();   
		
		// pick the correct device
		
		this.parent = _parent;
	}
	
	MidiOutput getOutput() {
		return output;
	}

	void note (float pitch, float vel) {
		pitch = PApplet.constrain (pitch, 0, 127);
		vel = PApplet.constrain (vel, 0, 127);
		output.sendNoteOn (1, (int)pitch, (int) vel);                             //  noteOn channel 1
		
	}

	void off (float pitch){
		pitch = PApplet.constrain (pitch, 0, 127);
		output.sendNoteOn (1, (int)pitch, 0);                                   
	}

	void cc (float controller, float value){    
		controller = PApplet.constrain (controller, 0, 127);
		value = PApplet.constrain (value, 0, 127);
		output.sendController (1, (int)controller, (int) value);                 
	}

	void patch (float value){    
		value = PApplet.constrain (value, 0, 127);                                    
		output.sendProgramChange ((int) value, 0);                              
	}

	void bend (float value){    
		value = PApplet.constrain (value, 0, 16383);
		int [] data = new int [2];
		data [0] = (int) (value/128); 
		data [1] = (int) (value) % 128;
		output.sendPitchBend (0, data[1], data[0]);                              //  [we are using a old version of rwmidi fixed by Pablo Gindel, we should prolly migrate to a newer rwmidi]
	}

}
