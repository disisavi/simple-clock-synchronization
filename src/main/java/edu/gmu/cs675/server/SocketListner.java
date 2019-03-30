package edu.gmu.cs675.server;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Scanner;

public class SocketListner implements Runnable {
    private DatagramSocket socket;

    SocketListner(DatagramSocket socket) {
        this.socket = socket;

    }

    @Override
    public void run() {

        while (true) {
            byte[] message = new byte[256];
            DatagramPacket packet
                    = new DatagramPacket(message, message.length);
            try {
                this.socket.receive(packet);
                String reciverString = new String(packet.getData(), 0, packet.getLength());
                System.out.println(reciverString);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
