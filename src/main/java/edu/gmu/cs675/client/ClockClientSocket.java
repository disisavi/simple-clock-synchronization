package edu.gmu.cs675.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class ClockClientSocket {
    InetAddress serverIP;
    DatagramSocket socket;
    int port = 1024;

    ClockClientSocket(String ip) {
        try {
            this.serverIP = InetAddress.getByName(ip);
            this.socket = new DatagramSocket();
        } catch (IOException e) {
            System.out.println("Error -- Couldn't connect to server" + e.getMessage());
            e.printStackTrace();
        }
    }

    public String serverConnectionPoint(String stringPackket) throws IOException {
        byte[] message = stringPackket.getBytes();
        DatagramPacket packet = new DatagramPacket(message,message.length,this.serverIP,this.port);
        try {
            socket.send(packet);
            message = new byte[256];
            packet = new DatagramPacket(message,message.length);
            socket.receive(packet);
            String returnString = new String(packet.getData(),0,packet.getLength());
            return returnString;
        } catch (IOException e) {
            System.out.println("Couldn't Send/receive packet because "+e.getMessage());
            throw e;
        }
    }
}
