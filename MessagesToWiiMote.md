# Introduction #

The following info is relevant for v0.2 and up.

Most control of the WiiMote is done using "broadcast" blocks in Scratch and all is done using broadcasts and variables staring with a tidle (~).

## Basic broadcast ##

  * ~ListenTo  - Get the Wii2Scratch to send particular info when an event occurs
  * ~Command - Send a command to the Wiimote, genreally for turning something on or off
  * ~Set - Set a WiiMote parameter generally used for setting sensitivities

## Basic sensor-update ##
> Wii2Scratch only listens for 1 Scratch global variable "~value" this is used with ~Set for using a Scratch variable to set a WiiMote parameter.


# Details #
## ~ListenTo ##
### Usage ###
`~ListenTo TYPE [true/false]`

(the false is available on all of the following but skipping for clarity.

| broadcast | Description |
|:----------|:------------|
| ` ~ListenTo Orientation ` | Starts sending Wii1Pitch, Wii1Roll, Wii1Roll and activates MotionSensing if not already done|
| ` ~ListenTo Orientation false `| Stops sending Wii1Pitch, Wii1Roll, Wii1Yaw |
| ` ~ListenTo GForce `| Starts sending Wii1GForceX, Wii1GForceY, Wii1GForceZ and activates MotionSensing if not already done |
| ` ~ListenTo RawAcceleration `| Starts sending Wii1RawAccX, Wii1RawAccY, Wii1RawAccZ and activates MotionSensing if not already done |
| ` ~ListenTo IrXYZ `| Starts sending Wii1IrX, Wii1IrY, Wii1IrZ, Wii1IrDistance (which is the pixel distance between Point1 and Point2) and activates IRTRacking if not already done |
| ` ~ListenTo IrPoints `| Starts sending a list of X,Y positions for all visible IR points and activates IRTRacking if not already done |

## ~Command ##
### Usage ###
`~Command TYPE`

| broadcast | Description |
|:----------|:------------|
| ` ~Command activateContinuous ` | Activate continuous monitoring (sends fake events as fast as it can (not recommended).|
| ` ~Command activateIRTRacking ` | Activate IR Tracking.|
| ` ~Command activateMotionSensing ` | Activate motion sensing.|
| ` ~Command activateRumble ` | Activate the rumble.|
| ` ~Command activateSmoothing ` | Activate smoothing.|
| ` ~Command deactivateContinuous ` | Deactivate continuous.|
| ` ~Command deactivateIRTRacking ` | Deactivate IR Tracking.|
| ` ~Command deactivateMotionSensing ` | Deactivate motion sensing.|
| ` ~Command deactivateRumble ` | Deactivate the rumble.|
| ` ~Command deactivateSmoothing ` | Deactivate smoothing.|


## ~Set ##
### Usage ###
`~Set TYPE VAL`
or
`~Set TYPE value`
| broadcast example | Description |
|:------------------|:------------|
| ` ~Set OrientationThreshold 10 `| Would set Wii2Scratch to trigger an event each time the orientation changes by more than 10 degrees (however watch for what the AccelerationThreshold is set to) |
| ` ~Set AccelerationThreshold 10 `| Would set Wii2Scratch to trigger an event each time the raw acceleration changes by more than 10 (out of 256) (setting this to a large number can reduce the number of events sent that are not related to Orientation if that is what you need) |

also the following can be done in scratch
` set ~value to 100 `
` broadcast ~Set OrientationThreshold value `

Which would set it to 100 and thus allows the use of Scratch variables when setting thresholds.