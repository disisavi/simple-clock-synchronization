package edu.gmu.cs675.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClockClientSocket {
    InetAddress serverIP;
    DatagramSocket socket;
    int port = 1025;
    DatagramPacket packet = null;
    int timeoutTIme = 9000;

    ClockClientSocket(String ip) {
        try {
            this.serverIP = InetAddress.getByName(ip);
            this.socket = new DatagramSocket();
        } catch (IOException e) {
            System.out.println("Error -- Couldn't connect to server" + e.getMessage());
            e.printStackTrace();
        }
    }

    void sendToServer(String stringPackket) throws IOException {
        byte[] message = stringPackket.getBytes();
        DatagramPacket packet = new DatagramPacket(message, message.length, this.serverIP, this.port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Couldn't Send packet because " + e.getMessage());
            throw e;
        }
    }

    String receiveFromServer() throws IOException {

        try {
            byte[] message = new byte[256];
            this.socket.setSoTimeout(this.timeoutTIme);
            DatagramPacket packet = new DatagramPacket(message, message.length);
            socket.receive(packet);
            String returnString = new String(packet.getData(), 0, packet.getLength());
            this.socket.setSoTimeout(0);
            return returnString;
        } catch (IOException e) {
            System.out.println("Couldn't Receive packet because " + e.getMessage());
            throw e;
        }


    }
}
