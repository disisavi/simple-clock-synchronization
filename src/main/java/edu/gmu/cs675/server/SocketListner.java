package edu.gmu.cs675.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Instant;
import java.util.Calendar;
import java.util.Map;

public class SocketListner implements Runnable {
    private DatagramSocket socket;
    private Map<String, String> packetsRecived;

    SocketListner(DatagramSocket socket, Map<String, String> packetsRecived) {
        this.socket = socket;
        this.packetsRecived = packetsRecived;
    }

    @Override
    public void run() {

        while (true) {
            byte[] message = new byte[256];
            DatagramPacket packet = new DatagramPacket(message, message.length);
            try {
                this.socket.receive(packet);
                String timeRecived = Long.toString(Instant.now().toEpochMilli());
                String reciverString = new String(packet.getData(), 0, packet.getLength());
                String input[] = reciverString.split(" ");
                if (input[0].equals("1")) {
                    packetsRecived.clear();
                }
                packetsRecived.put(input[0], input[1]);
                packet = returnPacket(reciverString, timeRecived, packet);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    DatagramPacket returnPacket(String stringRecived, String timeRecived, DatagramPacket packet) {
        Calendar tempCalendar = Calendar.getInstance();
        long timeToSend = Instant.now().toEpochMilli();
        String returnMessage = stringRecived + " " + timeRecived + " " + timeToSend;
        tempCalendar.setTimeInMillis(timeToSend);
        System.out.println("Time return = " + tempCalendar.getTime() + " in ms " + tempCalendar.getTimeInMillis());
        packet.setData(returnMessage.getBytes());
        return packet;
    }
}
