/**
 * @author Martyn Eggleton
 * V0.2
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
 * TODO: Add command line switches (or interprocess call to set number of Wiimotes (listeners should be ok up to 4) but the wiimotes array and all the comm stuff will ned refactoring
 * TODO: Add code  Numchucks, Classic Controllers etc. should be trivial.
 * TODO: Add receiving broadcasts from Scratch for rumbling and lights.
 * TODO: Possibly round the pitch and roll etc to 1 decimal place to shorten the strings sent
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

	private static Map<String, Boolean> subListeners;
	private static String sIncomingValue = "1";

	/**
	 * Opens the port for talking to Scratch
	 * and sets up the listeners
	 * @param args
	 */
	public static void main(String[] args) {	
		try {
			echoSocket = new Socket("localhost", 42001);
			echoSocket.setSoTimeout(1000);
			toScratch = echoSocket.getOutputStream();
			fromScratch = new 
			InputStreamReader(echoSocket.getInputStream(), "UTF-8");
		} catch (UnknownHostException e) {
			System.err.println("Don't know about Scratch (port 42001).");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for "
					+ "the connection to Scratch.");
			System.exit(1);
		}

		buttonMap = new HashMap<Integer, String>();
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

		subListeners = new HashMap<String, Boolean>();
		subListeners.put("Orientation", false);
		subListeners.put("GForce", false);
		subListeners.put("RawAcceleration", false);
		subListeners.put("IrXYZ", false);
		subListeners.put("IrPoints", false);
		

		Wiimote[] wiimotes = WiiUseApiManager.getWiimotes(1, true);
		Wiimote wiimote = wiimotes[0];
		wiimote.deactivateContinuous();
		wiimote.addWiiMoteEventListeners(new Wii2Scratch());

		sendtoscratch("broadcast \"Wii2ScratchStarted\"");

		while (true) {
			try { // Poll every ~10 ms
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {}

			try {
				// Receive data
				while(fromScratch.ready())
				{					
					int iMsgLength = 0;
					iMsgLength += (fromScratch.read() * 256 * 256 * 256);
					iMsgLength += (fromScratch.read() * 256 * 256);
					iMsgLength += (fromScratch.read() * 256);
					iMsgLength += (fromScratch.read());
					
					char aMsg[] = new char[iMsgLength];
					fromScratch.read(aMsg, 0, iMsgLength);
					String sMsg = new String(aMsg);
					
					/*when setting a variable loop round all the wiimotes checking if the id = the 
					current wiiToControl variable  or 0 
					
					if using special char could you do 
					broadcast "^CommTest"
					
					
					
					Map<String, Boolean> aListeners = new HashMap<String, Boolean>();
					broadcast "~ListenTo Orientation"
					broadcast "~ListenTo Orientation true" //same as above
					broadcast "~ListenTo Orientation false"
					
					broadcast "~ListenTo GForce"
					broadcast "~ListenTo RawAcceleration"
					broadcast "~ListenTo IrXYZ"
					
					broadcast "~Command activateMotionSensing"
					broadcast "~Command activateIRTracking"
					broadcast "~Command deactivateIRTracking"
					broadcast "~Command activateRumble"
					
					broadcast "~Set AccelerationThreshold 150"  /theshold being a switch statement that firethe correct one.
					broadcast "~Set OrientationThreshold 5"
					
					broadcast "~Set OrientationThreshold value"
					
					broadcast "~Command activateIRTracking"
					broadcast "~Command deactivateIRTracking"
					broadcast "~Command activateRumble"
					
					sensor-update "Scratch-~value"
					
					*/
					System.out.println(sMsg);
					//just doing the one variable
					if (sMsg.startsWith("sensor-update \"Scratch-~value"))
					{
						sIncomingValue = sMsg.substring(sMsg.lastIndexOf(" ") + 1);
						System.out.println("sIncomingValue="+sIncomingValue);
					}
					
					if (sMsg.startsWith("broadcast \"~"))
					{
						int iSubStart = 0;
						int iSubEnd = 0;
						int iLastQuote = sMsg.lastIndexOf("\"");
						String sCommand = ""; 
						String[] aCommandParts; 
						while (iSubStart < iLastQuote)
						{
							iSubStart = sMsg.indexOf("\"", iSubStart);
							iSubEnd = sMsg.indexOf("\"", iSubStart+1 );
							sCommand = sMsg.substring(iSubStart+2, iSubEnd);
							aCommandParts = sCommand.split(" ");
							
							//System.out.println("aCommandParts[0]=\""+aCommandParts[0]+"\"");
							
							if(aCommandParts[0].equals("Command"))
							{
								//System.out.println("aCommandParts[1]=\""+aCommandParts[1]+"\"");
								if(aCommandParts[1].equals("activateMotionSensing"))
								{
									wiimote.activateMotionSensing();	
								}
								
								if(aCommandParts[1].equals("deactivateMotionSensing"))
								{
									wiimote.deactivateMotionSensing();	
								}
								
								
								if(aCommandParts[1].equals("activateIRTracking"))
								{
									wiimote.activateIRTRacking();	
								}
								
								if(aCommandParts[1].equals("deactivateIRTracking"))
								{
									wiimote.deactivateIRTRacking();	
								}
								
								if(aCommandParts[1].equals("activateSmoothing"))
								{
									wiimote.activateSmoothing();	
								}
								
								if(aCommandParts[1].equals("deactivateSmoothing"))
								{
									wiimote.deactivateSmoothing();	
								}
								
								if(aCommandParts[1].equals("activateContinuous"))
								{
									wiimote.activateContinuous();	
								}
								
								if(aCommandParts[1].equals("deactivateContinuous"))
								{
									wiimote.deactivateContinuous();	
								}
								

								if(aCommandParts[1].equals("activateRumble"))
								{
									wiimote.activateRumble();	
								}
								
								if(aCommandParts[1].equals("deactivateRumble"))
								{
									wiimote.deactivateRumble();	
								}
							}
							
							if(aCommandParts[0].equals("ListenTo"))
							{
								if(aCommandParts.length > 2 && aCommandParts[2].equals("false"))
								{
									subListeners.put(aCommandParts[1], false);
									
								}
								else
								{
									subListeners.put(aCommandParts[1], true);
									
									if(aCommandParts[1].equals("Orientation")
									||aCommandParts[1].equals("GForce")
									||aCommandParts[1].equals("RawAcceleration"))
									{
										if(true)
										{
											wiimote.activateMotionSensing();
										}
									}
									
									if(aCommandParts[1].equals("IrXYZ")
										||aCommandParts[1].equals("IrPoints"))
											{
												if(true)
												{
													wiimote.activateIRTRacking();
												}
											}
								}
								
								
							}
							
							
							if(aCommandParts[0].equals("Set") && aCommandParts.length == 3)
							{
								String sValue = aCommandParts[2];
								if (sValue.equals("value"))
								{
									sValue = sIncomingValue;
									System.out.println("sValue=\""+sValue+"\"");
								}
								
								if(aCommandParts[1].equals("AccelerationThreshold"))
								{
									
									Integer AccelerationThreshold = Integer.parseInt(sValue);
									wiimote.setAccelerationThreshold(AccelerationThreshold);
								}

								if(aCommandParts[1].equals("OrientationThreshold"))
								{
									try{
										Double OrientationThreshold = Double.parseDouble(sValue);
										wiimote.setOrientationThreshold(OrientationThreshold.floatValue() );
									}
									catch(NumberFormatException e)
									{
										try{
											Integer OrientationThreshold = Integer.parseInt(sValue);
											wiimote.setOrientationThreshold(OrientationThreshold.floatValue() );
										}
										catch(NumberFormatException e2)
										{
											Double OrientationThreshold = 0.1;
											wiimote.setOrientationThreshold(OrientationThreshold.floatValue() );
										}
									}
									
								}

							}
												
							
							iSubStart = iSubEnd + 1;
							
							
						}
						
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}


	}

	private static Socket echoSocket;
	private static OutputStream toScratch;
	private static InputStreamReader fromScratch;

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
				toScratch.write(sizeBytes[i]);
			}
			toScratch.write(dataOut.getBytes());
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
	private static Map<Integer, String> buttonMap;
	
	public void onButtonsEvent(WiimoteButtonsEvent arg0) {
		//System.out.println(arg0);
		String sMsg = "";
		String sMsg2 = "";
		Short justPressed = arg0.getButtonsJustPressed();
		Short justReleased = arg0.getButtonsJustReleased();
		String sNamePrefix = "Wii" + arg0.getWiimoteId();


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

	//public boolean bIrPointVisible = false;


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


		String sMsg = "";
		String sNamePrefix = "Wii" + arg0.getWiimoteId();


		IRSource[] WiiIRSources = arg0.getIRPoints();

		/*if (WiiIRSources.length == 0)
		{
			if(bIrPointVisible)
			{
				bIrPointVisible = false;
			}
			else
			{
				return;
			}
		}*/
		//System.out.println(arg0);

		if (subListeners.get("IrXYZ"))
		{
			sMsg = sMsg + " \""+sNamePrefix+"IrX\" "+arg0.getX();
			sMsg = sMsg + " \""+sNamePrefix+"IrY\" "+arg0.getY();
			sMsg = sMsg + " \""+sNamePrefix+"IrZ\" "+arg0.getZ();
			sMsg = sMsg + " \""+sNamePrefix+"IrDistance\" "+arg0.getDistance();
		}
		
		if (subListeners.get("IrPoints"))
		{
		    for(int i=0; i < WiiIRSources.length; i++)
	        {	
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"X\" "+WiiIRSources[i].getX();
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"Y\" "+WiiIRSources[i].getY();
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"Rx\" "+WiiIRSources[i].getRx();
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"Ry\" "+WiiIRSources[i].getRy();
	        	sMsg = sMsg + " \""+sNamePrefix+"IrPoint"+(i+1)+"Size\" "+WiiIRSources[i].getSize();
	        }
		}
		
		if (sMsg.length() > 0)
		{
			//System.out.println(sMsg);
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

		if (subListeners.get("Orientation").equals(true))
		{
			Orientation WiiOr = arg0.getOrientation();
			sMsg = sMsg + " \""+sNamePrefix+"Pitch\" "+WiiOr.getPitch();
			sMsg = sMsg + " \""+sNamePrefix+"Roll\" "+WiiOr.getRoll();
			sMsg = sMsg + " \""+sNamePrefix+"Yaw\" "+WiiOr.getYaw();
		}
		
		if (subListeners.get("GForce"))
		{
	        GForce WiiGForce = arg0.getGforce();
	        sMsg = sMsg + " \""+sNamePrefix+"GForceX\" "+WiiGForce.getX();
	        sMsg = sMsg + " \""+sNamePrefix+"GForceY\" "+WiiGForce.getY();
	        sMsg = sMsg + " \""+sNamePrefix+"GForceZ\" "+WiiGForce.getZ();
		}

		if (subListeners.get("RawAcceleration"))
		{
			RawAcceleration WiiRawAcc = arg0.getRawAcceleration();
			sMsg = sMsg + " \""+sNamePrefix+"RawAccX\" "+WiiRawAcc.getX();
			sMsg = sMsg + " \""+sNamePrefix+"RawAccY\" "+WiiRawAcc.getY();
			sMsg = sMsg + " \""+sNamePrefix+"RawAccZ\" "+WiiRawAcc.getZ();
		}

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
