package edu.gmu.cs675.server;


import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class SocketServer {
    public InetAddress inetAddress;
    private int port = 1024;


    public InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 1024);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());

        return ip;
    }


    public void startSocketServer() {
        try {
            DatagramSocket socket = new DatagramSocket(this.port);
            this.inetAddress = getSelfIP();
            System.out.println("Clock Server Startup Complete");
            System.out.println("ip -- " + this.inetAddress.getHostAddress());
            Thread clock = new Thread(new SocketListner(socket));
            clock.start();
            exitCommand();
        } catch (IOException e) {
            System.out.println("Problem in starting server ... " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void exitCommand() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please Enter 'E' for exit");
        while (true) {
            if (scanner.nextLine().toUpperCase().charAt(0) == 'E') {
                System.exit(1);
            } else {
                System.out.println("Please enter the right command.");
            }
        }
    }

    public static void main(String[] args) {
        SocketServer socketServer = new SocketServer();
        socketServer.startSocketServer();
    }
}
