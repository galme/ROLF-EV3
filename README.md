# ROLF-EV3
Remote Controlled Life Feed EV3

# What is this about?
This project is a collection of 3 apps, for 3 platforms - PC, Android and EV3.
Its' purpose is to be able to command an EV3 robot with your PC keyboard and show a live-feed video stream from a phone mounted on the Mindstorms EV3 robot - to an extent, a real-life "racing" game. :)

## PC - EV3VideoControl_PC
A Visual Studio (WPF - C#) project, using libVLC.NET to play the RTSP stream, or alternatively, receive MJPEG "video" from UDP or TCP socket.

## Android - EV3VideoControl_Android
An Android Studio project using the capabilities of the [libstreaming library](https://github.com/fyhertz/libstreaming) to create a RTSP server and send the media content (h.264) over RTP.
Alternatively, it can send MJPEG over TCP/UDP.

## Mindstorms EV3 - EV3VideoControl_EV3
An Eclipse (+ EV3 Plugin) project written in Java to interpret commands sent from the PC over an Android phone to move around.

# Detailed explanations are given in the wiki.