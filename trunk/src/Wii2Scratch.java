/**
 * @author Martyn Eggleton
 * Program for connecting WiiMote to Scratch > 1.3
 * It sends sensor values for all events and also does broadcasts for button presses.
 * 
 * 
 * Code will be released under GPL once i get round to putting source code up.
 * For this you will need wiiusej.jar from http://code.google.com/p/wiiusej/
 * this ships with libwiiuse.so, libWiiuseJ.so wiiuse.dll and WiiUseJ.dll 
 * that all need to be placed in the root of your project.
 * 
 * It is built upon the WiiUse C library so see their documentation
 * (http://wiiuse.net/?nav=docs) for list a of compatible Bluetooth stacks 
 * and hardware.
 * 
 * TODO:
 * Add command line switches to switch on and off various events.
 * Add command line switches to set number of Wiimotes (listeners should be ok up to 4)
 * Add code  Numchucks, Classic Controllers etc. should be trivial.
 * Add receiving broadcasts from Scratch for rumbling and lights.
 * Possibly round the pitch and roll etc to 1 decimal place to shorten the strings sent
 * Possibly re-factor so that Wii2Scrath ran on a timing loop and sent all current values at once. 
 */

	import wiiusej.WiiUseApiManager;
	import wiiusej.Wiimote;
	import wiiusej.values.Orientation;
	import wiiusej.values.GForce;
	import wiiusej.values.RawAcceleration;
	import wiiusej.values.IRSource;
	import wiiusej.wiiusejevents.physicalevents.ExpansionEvent;
	import wiiusej.wiiusejevents.physicalevents.IREvent;
	import wiiusej.wiiusejevents.physicalevents.MotionSensingEvent;
	import wiiusej.wiiusejevents.physicalevents.WiimoteButtonsEvent;
	import wiiusej.wiiusejevents.utils.WiimoteListener;
	import wiiusej.wiiusejevents.wiiuseapievents.DisconnectionEvent;
	import wiiusej.wiiusejevents.wiiuseapievents.NunchukInsertedEvent;
	import wiiusej.wiiusejevents.wiiuseapievents.NunchukRemovedEvent;
	import wiiusej.wiiusejevents.wiiuseapievents.ClassicControllerInsertedEvent;
	import wiiusej.wiiusejevents.wiiuseapievents.ClassicControllerRemovedEvent;
	import wiiusej.wiiusejevents.wiiuseapievents.GuitarHeroInsertedEvent;
	import wiiusej.wiiusejevents.wiiuseapievents.GuitarHeroRemovedEvent;

	import wiiusej.wiiusejevents.wiiuseapievents.StatusEvent;

	import java.io.*;
	import java.net.*;
	import java.util.*;

	public class Wii2Scratch implements WiimoteListener{

		   
	    /**
	     * Opens the port for talking to Scratch
	     * and sets up the listeners
	     * @param args
	     */
	    public static void main(String[] args) {	
	         try {
	             echoSocket = new Socket("127.0.0.1", 42001);
	             echoSocket.setSoTimeout(100);
	             ScratchClient = echoSocket.getOutputStream();
	         } catch (UnknownHostException e) {
	             System.err.println("Don't know about Scratch (port 42001).");
	             System.exit(1);
	         } catch (IOException e) {
	             System.err.println("Couldn't get I/O for "
	                                + "the connection to Scratch.");
	             System.exit(1);
	         }
	    	
	        Wiimote[] wiimotes = WiiUseApiManager.getWiimotes(1, true);
	        Wiimote wiimote = wiimotes[0];
	        wiimote.activateIRTRacking();
	        wiimote.activateMotionSensing();
	        wiimote.addWiiMoteEventListeners(new Wii2Scratch());
	        
	        sendtoscratch("broadcast \"Wii2ScratchStarted\"");
	    }
		
	    private static Socket echoSocket;
	    private static OutputStream ScratchClient;
	    
	    
	    /**
	     * Sends dataOut to Scratch using the format described here
	     * http://scratch.mit.edu/forums/viewtopic.php?id=9458
	     * @param dataOut
	     */
	    public static void sendtoscratch(String dataOut){
	    	//System.out.println(dataOut);
			
	    	try
	    	{
		    	byte [] sizeBytes = { 0, 0, 0, 0 };
		    	int len = dataOut.length();
		  
				sizeBytes[0] =(byte)( len >> 24 );
				sizeBytes[1] =(byte)( (len << 8) >> 24 );
				sizeBytes[2] =(byte)( (len << 16) >> 24 );
				sizeBytes[3] =(byte)( (len << 24) >> 24 );
				
				for (int i=0; i<4; i++) {                  
					ScratchClient.write(sizeBytes[i]);
				}
				ScratchClient.write(dataOut.getBytes());
	    	}
	    	catch(IOException e) {
	            System.err.println("Couldn't send "+ dataOut
	                    + " to Scratch.");
	            System.exit(1);
	        }
	    }	

	    
	    /* 
	     * Listener for buttons when Just pressed sets a Scratch Sensor with a name like
	     * Wii1ButtonA to 1 and sends a broadcast of Wii1ButtonAPressed
	     * when its just been release would set Wii1ButtonA to 0 and broadcast Wii1ButtonAReleased
	     * 
	     * When the Wii1 home button is pressed Wii2Scratch exits after sending the data.
	     * 
	     * (non-Javadoc)
	     * @see wiiusej.wiiusejevents.utils.WiimoteListener#onButtonsEvent(wiiusej.wiiusejevents.physicalevents.WiimoteButtonsEvent)
	     */
	    public void onButtonsEvent(WiimoteButtonsEvent arg0) {
	        //System.out.println(arg0);
	        String sMsg = "";
	        String sMsg2 = "";
	        Short justPressed = arg0.getButtonsJustPressed();
	        Short justReleased = arg0.getButtonsJustReleased();
	        String sNamePrefix = "Wii" + arg0.getWiimoteId();
	        
	        Map<Integer, String> buttonMap = new HashMap<Integer, String>();
	        buttonMap.put(128, "ButtonHome");
	        buttonMap.put(2, "Button1");
	        buttonMap.put(1, "Button2");
	        buttonMap.put(8, "ButtonA");
	        buttonMap.put(4, "ButtonB");
	        buttonMap.put(2048, "ButtonUp");
	        buttonMap.put(1024, "ButtonDown");
	        buttonMap.put(256, "ButtonLeft");
	        buttonMap.put(512, "ButtonRight");
	        buttonMap.put(4096, "ButtonPlus");
	        buttonMap.put(16, "ButtonMinus");
	        
	        
	        for (Map.Entry<Integer, String> e : buttonMap.entrySet())
	        {
	            if ((justPressed & e.getKey()) > 0)
	            {
	            	sMsg = sMsg + " \""+sNamePrefix+e.getValue()+"\" 1";
	            	sMsg2 = sMsg2 + " \""+sNamePrefix+e.getValue()+"Pressed\"";
	            }
	            
	            if ((justReleased & e.getKey()) > 0)
	            {
	            	sMsg = sMsg + " \""+sNamePrefix+e.getValue()+"\" 0";
	            	sMsg2 = sMsg2 + " \""+sNamePrefix+e.getValue()+"Released\"";
	            }
	        }
	        
	        if (sMsg.length() > 0)
	        {
		        	sMsg = "sensor-update" + sMsg;
		        	sMsg2 = "broadcast" + sMsg2;
		        	sendtoscratch(sMsg);
		    		sendtoscratch(sMsg2);
	        }
	        
	        if(arg0.isButtonHomePressed())
	        {
	        	System.exit(1);
	        }
	    }
	    
	    public boolean bIrPointVisible = false;
	    
	    
	    /* Listener for the IR events.
	     * It throws a lot of these which can easily swamp Scratch 
	     * so it is now rigged to stop sending one events after it 
	     * stops being able to see the lights
	     * TODO: add broadcast for can see can't see
	     *  
	     *  
	     * (non-Javadoc)
	     * @see wiiusej.wiiusejevents.utils.WiimoteListener#onIrEvent(wiiusej.wiiusejevents.physicalevents.IREvent)
	     */
	    public void onIrEvent(IREvent arg0) {
	        //System.out.println(arg0);
        
	        String sMsg = "";
	        String sNamePrefix = "Wii" + arg0.getWiimoteId();
	        
	        
	        IRSource[] WiiIRSources = arg0.getIRPoints();
	        
	        if (WiiIRSources.length == 0)
	        {
	        	if(bIrPointVisible)
	        	{
	        		bIrPointVisible = false;
	        	}
	        	else
	        	{
	        		return;
	        	}
	        }
	        
	        sMsg = sMsg + " \""+sNamePrefix+"IrX\" "+arg0.getX();
	        sMsg = sMsg + " \""+sNamePrefix+"IrY\" "+arg0.getY();
	        sMsg = sMsg + " \""+sNamePrefix+"IrZ\" "+arg0.getZ();
	        sMsg = sMsg + " \""+sNamePrefix+"IrDistance\" "+arg0.getDistance();
	        
	        
	        for(int i=0; i < WiiIRSources.length; i++)
	        {	
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"X\" "+WiiIRSources[i].getX();
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"Y\" "+WiiIRSources[i].getY();
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"Rx\" "+WiiIRSources[i].getRx();
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"Ry\" "+WiiIRSources[i].getRy();
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"Size\" "+WiiIRSources[i].getSize();
	        }
	        
	        if (sMsg.length() > 0)
	        {
		        	sMsg = "sensor-update" + sMsg;
		        	sendtoscratch(sMsg);
		    }
	    }
	    

	    /* Listener for the motions sensing events
	     * Send pitch, roll, yaw and raw acceleration numbers, not currently sending the GForce
	     * 
	     * (non-Javadoc)
	     * @see wiiusej.wiiusejevents.utils.WiimoteListener#onMotionSensingEvent(wiiusej.wiiusejevents.physicalevents.MotionSensingEvent)
	     */
	    public void onMotionSensingEvent(MotionSensingEvent arg0) {
	        //System.out.println(arg0);
	        
	        String sMsg = "";
	        String sNamePrefix = "Wii" + arg0.getWiimoteId();
	        
	        Orientation WiiOr = arg0.getOrientation();
	        sMsg = sMsg + " \""+sNamePrefix+"Pitch\" "+WiiOr.getPitch();
	        sMsg = sMsg + " \""+sNamePrefix+"Roll\" "+WiiOr.getRoll();
	        sMsg = sMsg + " \""+sNamePrefix+"Yaw\" "+WiiOr.getYaw();
	        
	        /*
	        GForce WiiGForce = arg0.getGforce();
	        sMsg = sMsg + " \""+sNamePrefix+"GForceX\" "+WiiGForce.getX();
	        sMsg = sMsg + " \""+sNamePrefix+"GForceY\" "+WiiGForce.getY();
	        sMsg = sMsg + " \""+sNamePrefix+"GForceZ\" "+WiiGForce.getZ();
	        */
	        
	        RawAcceleration WiiRawAcc = arg0.getRawAcceleration();
	        sMsg = sMsg + " \""+sNamePrefix+"GRawAccX\" "+WiiRawAcc.getX();
	        sMsg = sMsg + " \""+sNamePrefix+"GRawAccY\" "+WiiRawAcc.getY();
	        sMsg = sMsg + " \""+sNamePrefix+"GRawAccZ\" "+WiiRawAcc.getZ();
	        
	        
	        if (sMsg.length() > 0)
	        {
		        	sMsg = "sensor-update" + sMsg;
		        	sendtoscratch(sMsg);
		    }
	        
	        
	        //ScratchSet("Moving", "1");
	    }

	    public void onExpansionEvent(ExpansionEvent arg0) {
	        System.out.println(arg0);
	    }

	    public void onStatusEvent(StatusEvent arg0) {
	        //System.out.println(arg0);
	    }

	    public void onDisconnectionEvent(DisconnectionEvent arg0) {
	        System.out.println(arg0);
	    }

	    public void onNunchukInsertedEvent(NunchukInsertedEvent arg0) {
	        System.out.println(arg0);
	    }

	    public void onNunchukRemovedEvent(NunchukRemovedEvent arg0) {
	        System.out.println(arg0);
	    }

	    public void onClassicControllerInsertedEvent(ClassicControllerInsertedEvent arg0) {
	        System.out.println(arg0);
	    }

	    public void onClassicControllerRemovedEvent(ClassicControllerRemovedEvent arg0) {
	        System.out.println(arg0);
	    }

	    public void onGuitarHeroInsertedEvent(GuitarHeroInsertedEvent arg0) {
	        System.out.println(arg0);
	    }

	    public void onGuitarHeroRemovedEvent(GuitarHeroRemovedEvent arg0) {
	        System.out.println(arg0);
	    }

	    
	 	}
