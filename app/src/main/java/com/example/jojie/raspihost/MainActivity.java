package com.example.jojie.raspihost;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    public String info = "data";

    EditText editTextPort;
    Button buttonConnect;
    TextView textResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        editTextPort = findViewById( R.id.port );
        buttonConnect = findViewById( R.id.connect );
        textResponse = findViewById( R.id.response );

        buttonConnect.setOnClickListener( buttonConnectOnClickListener );

        getDeviceIpAddress();

    }

    private void getDeviceIpAddress() {
        try {
            //Loop through all the network interface devices
            for (Enumeration<NetworkInterface> enumeration = NetworkInterface
                    .getNetworkInterfaces(); enumeration.hasMoreElements(); ) {
                NetworkInterface networkInterface = enumeration.nextElement();
                //Loop through all the ip addresses of the network interface devices
                for (Enumeration<InetAddress> enumerationIpAddr = networkInterface.getInetAddresses(); enumerationIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumerationIpAddr.nextElement();
                    //Filter out loopback address and other irrelevant ip addresses
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
                        //Print the device ip address in to the text view
                        textResponse.setText("Server is Running at IP: "+ inetAddress.getHostAddress() );
                    }
                }
            }
        } catch (SocketException e) {
            Log.e( "ERROR:", e.toString() );
        }
    }

    View.OnClickListener buttonConnectOnClickListener =
            new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {


                    MyClientTask myClientTask = new MyClientTask(
                            Integer.parseInt( editTextPort.getText().toString() ) );
                    myClientTask.execute();

                }

            };

    public class MyClientTask extends AsyncTask<Void, Void, Void> {


        int dstPort;
        String response;

        MyClientTask(int port) {
            dstPort = port;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            int count = 0;
            try {

                ServerSocket serverSocket = new ServerSocket( dstPort );
                response = "I'm waiting here: " + serverSocket.getLocalPort();

                while (true) {

                    try {
                        Socket socket = serverSocket.accept();

                        count++;
                        System.out.println( "#" + count + " from "
                                + socket.getInetAddress() + ":"
                                + socket.getPort() );
                        response = "#" + count + " from "
                                + socket.getInetAddress() + ":"
                                + socket.getPort();

                    /*  move to background thread
                    OutputStream outputStream = socket.getOutputStream();
                    try (PrintStream printStream = new PrintStream(outputStream)) {
                        printStream.print("Hello from Raspberry Pi, you are #" + count);
                    }
                    */
                        HostThread myHostThread = new HostThread( socket, count );
                        myHostThread.start();

                    } catch (IOException ex) {
                        System.out.println( ex.toString() );
                    }
                }
            } catch (IOException e) {
                System.out.println( e.toString() );
            }

            return null;
        }

        private class HostThread extends Thread {

            private Socket hostThreadSocket;
            int cnt;

            HostThread(Socket socket, int c) {
                hostThreadSocket = socket;
                cnt = c;
            }

            @Override
            public void run() {

                OutputStream outputStream;
                try {
                    outputStream = hostThreadSocket.getOutputStream();

                    PrintStream printStream = new PrintStream( outputStream );
                    printStream.print( "Hello from Raspberry Pi in background thread, you are #" + cnt );
                    printStream.print( "Hello from Raspberry Pi in background thread, 123 #" + cnt );
                    printStream.print( "EXIT" );

                } catch (IOException ex) {
                    Logger.getLogger( MainActivity.class.getName() ).log( Level.SEVERE, null, ex );
                } finally {
                    try {
                        hostThreadSocket.close();
                    } catch (IOException ex) {
                        Logger.getLogger( MainActivity.class.getName() ).log( Level.SEVERE, null, ex );
                    }
                }
            }
        }


    }
}
