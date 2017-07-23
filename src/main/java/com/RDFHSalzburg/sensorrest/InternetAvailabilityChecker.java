/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.RDFHSalzburg.sensorrest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Date;

public class InternetAvailabilityChecker
{
    public static boolean isInternetAvailable() throws IOException
    {
        return isHostAvailable("google.com") || isHostAvailable("amazon.com")
                || isHostAvailable("facebook.com")|| isHostAvailable("apple.com");
    }

    private static boolean isHostAvailable(String hostName) throws IOException
    {
        try(Socket socket = new Socket())
        {
            int port = 80;
            InetSocketAddress socketAddress = new InetSocketAddress(hostName, port);
            socket.connect(socketAddress, 3000);

            return true;
        }
        catch(UnknownHostException unknownHost)
        {
            return false;
        }
    }
    
    
    
    public static String getPublicIpAddress() throws MalformedURLException,IOException {
        URL connection = new URL("http://checkip.amazonaws.com/");
        URLConnection con = connection.openConnection();
        String str = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        str = reader.readLine();

        return str;
    }
    
    
    public static String pingIP(String hostname){
        long time = ScanIP.test(hostname);
        
        return time + "";
    }


    public static class ScanIP {
	/*public static void main(String[] args) {
		String hostAddress = "";
		int port;
		long timeToRespond = 0; // in milliseconds

		if (args.length < 1 || args.length > 2) {
			System.out.println("usage: " + usage);
			return;
		}

		try {
			hostAddress = args[0]; // copy the string
			if (args.length == 2)
				port = Integer.parseInt(args[1]); // convert the integer
			else
				port = 80;

			if (args.length == 1) {
				System.out.printf("scan using inetAddress.isReachable:");
				timeToRespond = test(hostAddress);
			}
			else {
				System.out.printf("scan using SocketChannel.connect:");
				timeToRespond = test(hostAddress, port);
			}
		} catch (NumberFormatException e) {
			System.out.println("Problem with arguments, usage: " + usage);
			e.printStackTrace();
		}

		if (timeToRespond >= 0)
			System.out.println(" responded in " + timeToRespond + " ms");
		else
			System.out.println("Failed");

	}*/
	/**
	 * Connect using layer3
	 * 
	 * @param hostAddress
	 * @return delay if the specified host responded, -1 if failed
	 */
	static long test(String hostAddress) {
		InetAddress inetAddress = null;
		Date start, stop;

		try {
			inetAddress = InetAddress.getByName(hostAddress);
		} catch (UnknownHostException e) {
			System.out.println("Problem, unknown host:");
			e.printStackTrace();
		}

		try {
			start = new Date();
			if (inetAddress.isReachable(5000)) {
				stop = new Date();
				return (stop.getTime() - start.getTime());
			}

		} catch (IOException e1) {
			System.out.println("Problem, a network error has occurred:");
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			System.out.println("Problem, timeout was invalid:");
			e1.printStackTrace();
		}

		return -1; // to indicate failure

	}

	/**
	 * Connect using layer4 (sockets)
	 * 
	 * @param
	 * @return delay if the specified host responded, -1 if failed
	 */
	static long test(String hostAddress, int port) {
		InetAddress inetAddress = null;
		InetSocketAddress socketAddress = null;
		SocketChannel sc = null;
		long timeToRespond = -1;
		Date start, stop;

		try {
			inetAddress = InetAddress.getByName(hostAddress);
		} catch (UnknownHostException e) {
			System.out.println("Problem, unknown host:");
			e.printStackTrace();
		}

		try {
			socketAddress = new InetSocketAddress(inetAddress, port);
		} catch (IllegalArgumentException e) {
			System.out.println("Problem, port may be invalid:");
			e.printStackTrace();
		}

		// Open the channel, set it to non-blocking, initiate connect
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(true);
			start = new Date();
			if (sc.connect(socketAddress)) {
				stop = new Date();
				timeToRespond = (stop.getTime() - start.getTime());
			}
		} catch (IOException e) {
			System.out.println("Problem, connection could not be made:");
			e.printStackTrace();
		}

		try {
			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return timeToRespond;
	}

    }
    
}
