using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using AxAXVLC;
using System.Threading;
using System.Net.Sockets;
using System.Net;
using System.IO;
using System.Drawing;
using System.Diagnostics;
using Declarations;
using Declarations.Media;
using Declarations.Players;
using Implementation;
using System.Windows.Interop;

namespace EV3_ActiveX_PC
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    /// 

    public enum TransferMode { TCP, UDP, RTSP };

    public partial class MainWindow : Window
    {

        AxVLCPlugin2 vlc = null;
        TcpClient BTsocket = null;
        TcpClient videoSocket = null;
        const string IP = "192.168.43.1"; // phone IP ... WiFi hotspots usually take this one, but if you're on LAN, this can be pretty much anything
        const int BT_PORT = 5778;
        const int BT_BACKUP_PORT = 49592;
        const int TCP_VIDEO_PORT = 6778;
        const int TCP_VIDEO_BACKUP_PORT = 59592;
        const int UDP_VIDEO_PORT = 54029;
        const int RTSP_PORT = 5678;
        int prevDegrees = -1;
        bool portSwitch = false;
        static TransferMode transferMode = TransferMode.RTSP; // set this to the mode you want (RTSP uses h.264 encoding, TCP/UDP is MJPEG) ... it must match the Android app's setting!

        IMediaPlayerFactory factory = null;
        IVideoPlayer player = null;


        public MainWindow()
        {
            InitializeComponent();

            this.KeyDown += new KeyEventHandler(KeyEventHandler);
            this.KeyUp += new KeyEventHandler(KeyEventHandler);

            // VLC player
            vlc = new AxVLCPlugin2();
            vlc.CreateControl();
            vlc.AutoPlay = false;

            FormsHost.Child = vlc;

            new Thread(() =>
            {
                Thread.CurrentThread.IsBackground = true;

                getVideoFromPhone();

            }).Start();

            // povezava do telefona
            new Thread(() =>
            {
                Thread.CurrentThread.IsBackground = true;

                connectToPhoneForBT();

            }).Start();

        }

        void TCPway()
        {
            TCPconnectToPhoneForVideo();

            int i = 0;
            while (true)
            {
                byte[] numB = new byte[4];
                videoSocket.GetStream().Read(numB, 0, 4);

                int length = BitConverter.ToInt32(numB.Reverse().ToArray(), 0);
                Debug.WriteLine("IMG RECV! Len:" + length);

                byte[] data = new byte[length];
                int readTillNow = 0;

                while (readTillNow != length)
                {
                    readTillNow += videoSocket.GetStream().Read(data, readTillNow, length - readTillNow);
                }

                Debug.WriteLine("IMG RECV! " + (++i));

                try
                {
                    this.Dispatcher.Invoke((Action)(() =>
                    {
                        ImageControl.Source = LoadImage(data);
                    }));
                } catch { }
                
            }
        }

        void UDPway()
        {
            UdpClient udpServer = new UdpClient(UDP_VIDEO_PORT);
            int i = 0;
            while (true)
            {
                Debug.WriteLine("IMG RECV! " + (++i));

                byte[] data = null;
                try
                {
                    var remoteEP = new IPEndPoint(IPAddress.Parse(IP), UDP_VIDEO_PORT);
                    data = udpServer.Receive(ref remoteEP);
                } catch (SocketException se)
                {
                    Debug.WriteLine(se.ErrorCode + " " + se.Message);
                }

                try
                {
                    this.Dispatcher.Invoke((Action)(() =>
                    {
                        ImageControl.Source = LoadImage(data);
                    }));
                } catch { }
                


            }
        }


        void getMedia()
        {
            player.Delay = 0;
            IMedia media = factory.CreateMedia<IMedia>("rtsp://" + IP + ":" + RTSP_PORT + "?videoapi=mc&camera=back&h264=1000-15-640-480");
            player.Open(media);

            player.Play();
        }

        void playStream()
        {

            while(true)
            {
                bool connectLoop = true;

                // loop and try to connect to the stream
                while (connectLoop)
                {
                    getMedia();

                    Thread.Sleep(5000);
                    if (player.IsPlaying)
                        connectLoop = false;
                }

                // loop to check if the stream fell
                while (true)
                {
                    Thread.Sleep(5000);
                    if (!player.IsPlaying)
                    {
                        break;
                    }
                }
            } 
        }

        void RTSPway()
        {
            string[] options = { "--video-filter=transform", "--transform-type=270", "--network-caching=200" };
            factory = new MediaPlayerFactory(options);
            player = factory.CreatePlayer<IVideoPlayer>();
            this.Dispatcher.Invoke((Action)(() =>
            {
                player.WindowHandle = new WindowInteropHelper(this).Handle;
            }));

            playStream();
        }

        void getVideoFromPhone()
        {
            if (transferMode == TransferMode.TCP)
            {
                TCPway();
            }
            else if(transferMode == TransferMode.UDP)
            {
                UDPway();
            }
            else if (transferMode == TransferMode.RTSP)
            {
                RTSPway();
            }
        }

        private static BitmapImage LoadImage(byte[] imageData)
        {
            if (imageData == null || imageData.Length == 0) return null;
            var image = new BitmapImage();
            using (var mem = new MemoryStream(imageData))
            {
                mem.Position = 0;
                image.BeginInit();
                image.CreateOptions = BitmapCreateOptions.PreservePixelFormat;
                image.CacheOption = BitmapCacheOption.OnLoad;
                image.UriSource = null;
                image.StreamSource = mem;
                image.EndInit();
            }
            image.Freeze();
            return image;
        }

        void connectToPhoneForBT()
        {
            try
            {
                if (portSwitch)
                {
                    BTsocket = new TcpClient(IP, BT_BACKUP_PORT);
                    portSwitch = false;
                }
                else
                {
                    BTsocket = new TcpClient(IP, BT_PORT);
                    portSwitch = true;
                }
            }
            catch (SocketException ex)
            {
                connectToPhoneForBT();
            }
        }

        void TCPconnectToPhoneForVideo()
        {
            try
            {
                if (portSwitch)
                {
                    videoSocket = new TcpClient(IP, TCP_VIDEO_PORT);
                    portSwitch = false;
                }
                else
                {
                    videoSocket = new TcpClient(IP, TCP_VIDEO_PORT);
                    portSwitch = true;
                }
            }
            catch (SocketException ex)
            {
                TCPconnectToPhoneForVideo();
            }
        }

        private void KeyEventHandler(object sender, KeyEventArgs e)
        {
            Debug.WriteLine("key pressed!");
            Int32 degrees = -1;
            
            // kateri ukaz ?
            if (Keyboard.IsKeyDown(Key.Up) && Keyboard.IsKeyDown(Key.Left)) // fwd-left
            {
                degrees = 135;
            }
            else if (Keyboard.IsKeyDown(Key.Up) && Keyboard.IsKeyDown(Key.Right)) // fwd-right
            {
                degrees = 45;
            }
            else if (Keyboard.IsKeyDown(Key.Down) && Keyboard.IsKeyDown(Key.Left)) // bck-left
            {
                degrees = 225;
            }
            else if (Keyboard.IsKeyDown(Key.Down) && Keyboard.IsKeyDown(Key.Right)) // bck-right
            {
                degrees = 315;
            }
            else if (Keyboard.IsKeyDown(Key.Up)) // fwd
            {
                degrees = 90;
            }
            else if (Keyboard.IsKeyDown(Key.Down)) // bck
            {
                degrees = 270;
            }
            else if (Keyboard.IsKeyDown(Key.Left)) // left
            {
                degrees = 180;
            }
            else if (Keyboard.IsKeyDown(Key.Right)) // right
            {
                degrees = 0;
            }
            else if(Keyboard.IsKeyDown(Key.Space)) // flash
            {
                degrees = -2;
            }
            else // null ... stand still!
            {
                degrees = -1;
            }

            if (prevDegrees != degrees)
            {
                try
                {
                    BTsocket.GetStream().Write(BitConverter.GetBytes(degrees).Reverse().ToArray(), 0, sizeof(Int32));
                    BTsocket.GetStream().FlushAsync();
                    prevDegrees = degrees;
                }
                catch { connectToPhoneForBT(); }
            }
        }
    }
}
