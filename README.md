# ROLF-EV3
**R**emote c**O**ntrolled **L**ive **F**eed EV3

# What is this about?
This project is a collection of 3 sub-projects, for 3 platforms - PC, Android and leJOS EV3.
Its' purpose is to be able to command an EV3 robot with your PC keyboard and show a live-feed video stream (h264 over RTSP&RTP or MJPEG over TCP/UDP sockets) from a phone mounted on the Mindstorms EV3 robot - to an extent, a real-life driving PC game.

## PC - EV3VideoControl_PC
A Visual Studio (WPF - C#) project, using libVLC.NET to play the RTSP stream, or alternatively, receive MJPEG "video" from UDP or TCP socket.

## Android - EV3VideoControl_Android
An Android Studio project using the capabilities of the [libstreaming library](https://github.com/fyhertz/libstreaming) to create a RTSP server and send the media content (h.264) over RTP.
Alternatively, it can send MJPEG over TCP/UDP.

## Mindstorms EV3 - EV3VideoControl_EV3
An Eclipse (+ EV3 leJOS plugin) project written in Java (for [EV3 leJOS](http://www.lejos.org/ev3.php) OS) to interpret commands sent from the PC over an Android phone to the EV3, to move accordingly.

# Installation Quick-Guide
1. install leJOS on your EV3
2. pair your EV3 and Android phone devices over Bluetooth
3. set-up your EV3 (so that you have a properly running MainClass.jar on it) - detailed explanation in title below
4. set-up your Android phone - detailed explanation in title below
5. set-up your PC: you need to have VLC installed (full installation)! And change the desired IP variable to match your phone's real IP address.
6. mount your phone on the EV3. I have it in portrait mode, but rotated for 180 degrees.

## set-up your EV3 - in detail
if you have eJRE8 on your leJOS, you can just copy the MainClass.jar on your robot. If not, you will have to compile it from the source code, to get a proper eJRE7 jar file.

I've used Eclipse + leJOS plugin [tutorial here](https://sourceforge.net/p/lejos/wiki/Installing%20the%20Eclipse%20plugin/)

On the EV3, you will probably have to enable PAN --> USB Client and set a custom IP for it (if it fails on automatic) to be able to run a .jar on the EV3 directly from ecliplse.

If you have Windows 10, you may need to sort out your drivers... Instructions:
If you are one of the people that found this because your device is showing up as a COM port instead of RNDIS, you may be able to get away using the RNDIS 5.1 driver. Find your device under Ports (COM & LTP) in Device Manager. Right-click it and select Update Driver Software..., then Browse my computer for driver software and then Let me pick from a list of device drivers on my computer and finally choose Remote NDIS Compatible Device. This should install the Microsoft RNDIS 5.1 driver (shows "Acer" as the manufacturer). If this works great. If it causes Network and Settings and other network related things to lock up, then you need the RNDIS 6.0 driver instead.
[Acer RNDIS 5.1 driver download link](http://catalog.update.microsoft.com/v7/site/ScopedViewRedirect.aspx?updateid=37e35bd4-d788-4b83-9416-f78e439f90a2)

## set-up your Android phone - in detail
Install APK, or even better, import the project in your Android Studio and build it for yourself.
Make sure the phone is reachable over network for the PC (WiFi hotspot/LAN + WAP/...), and **make sure the PC application has the right IP address**!

# Controlling the robot
you can control the robot using the arrow keys on the keyboard and pressing spacebar to toggle flash on or off.

# Troubleshooting
**if something does not work, import the project into the IDE and observe the logs! They're meant for debugging!**

- My Android phone doesn't have Lollipop or newer
  * Set the minSdkVersion variable in build.gradle to 16, but be aware that you'll only be able to use RTSP then.

- RTSP server shows a lot of errors
  * **try setting the forceMediaCodec variable to false**. Some phones do not support MediaCodec and this will make the app use MediaRecorder instead.

- I cannot see video in the PC app.
  * look at bullet-line above
  * **try different values in VideoQuality** in *RTSPservice.java*
  * try using the real VLC instead of the PC client application and use following URL pattern: "rtsp://" + IP + ":" + RTSP_PORT + "?videoapi=mc&camera=back&h264=1000-15-640-480" to connect to a network stream. IP is usually 192.168.43.1 , if you're using a WiFi hotspot and the port is predefined as 5678.
  * make sure the phone and PC are accessible through the network. You can make sure by pinging each other's IP address.
  * try using TransferMode.TCP (preferably) or TransferMode.UDP (it works, but EV3 needs to be "connected" to the phone as well) instead of TransferMode.RTSP as a temporary workaround in both the Android app and in the PC application!

- I cannot control the EV3 robot
  * (Bluetooth problem) make sure the EV3 and your phone are paired 
  * (Bluetooth problem) try using a hardcoded BT device address - pass a string containing the address, to the BTManager() constructor in PCcommands.java, for example ```java btManager = new BTManager("00:16:53:3F:61:F0") ``` where the string is your EV3's BT address
  * (Network problem) make sure the phone and PC are accessible through the network. You can make sure by pinging each other's IP address.

- video is rotated/flipped
  * Change the *--transform-type=270* string in the RTSPway method (PC application) to a different value. It depends on your phone's rotation on the robot.

- Something else
  * you'll have to figure it out through the debug logs, unfortunately. But it's probably a platform (various Android versions) specific issue or a general network/bluetooth connection problem.


The project is licensed under GNU / GPL 3 .
If you find an insurmountable issue, file it on github.