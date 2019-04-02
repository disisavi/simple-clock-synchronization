package edu.gmu.cs675.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class ClockCLient {
    Integer id;
    ScheduledFuture future;
    ScheduledExecutorService scheduler;
    ClockClientSocket clockClientSocket;
    final int packetsToSend;
    Writer fileWriter;
    SortedSet<Integer> packetReceivedSet;
    Set<Integer> droppedPacketsSet;
    Calendar localCLientTime;

    public ClockCLient(String ip) {

        this.id = 0;
        packetsToSend = this.numberOfPacketsToSend();
        droppedPacketsSet = new HashSet<>();
        packetReceivedSet = Collections.synchronizedSortedSet(new TreeSet<>());
        this.initializeFile();
        this.clockClientSocket = new ClockClientSocket(ip);
        localCLientTime = Calendar.getInstance();
        this.initializeSchedule();
    }

    void initializeFile() {
        try {
            fileWriter = new PrintWriter("time.txt", "UTF-8");
            fileWriter.write("-----------------------------------------------------------\n\tTime Analysis over "
                    + packetsToSend + " packets");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void initializeSchedule() {
        this.scheduler = Executors.newScheduledThreadPool(100);
        this.future = scheduler.scheduleAtFixedRate(this::run, 10, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Kindly enter the IP for sync server");
        String serverIP = scanner.nextLine();
        ClockCLient clockCLient = new ClockCLient(serverIP);
        System.out.println("The current system time  = " + clockCLient.localCLientTime.getTime());
        System.out.println("\n\nThe number of packets to be sent is " + clockCLient.packetsToSend);
        clockCLient.scheduler.schedule(() -> {

            try {
                clockCLient.fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            clockCLient.future.cancel(true);
            clockCLient.scheduler.shutdown();

            System.out.println("Cancelled");
        }, clockCLient.packetsToSend * 10, TimeUnit.SECONDS);

        try {
            clockCLient.future.get();
        } catch (ExecutionException e) {
            System.out.println("Schedule thread failed because " + e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException | CancellationException ignored) {

        }
    }

    void run() {
        id++;
        int id = this.id;
//        System.out.println("Sending packet " + id + " from Thread id " + Thread.currentThread().getId());
        String now = Long.toString(Instant.now().toEpochMilli());
        String messageToServer = id + " " + now;
        try {
            this.clockClientSocket.sendToServer(messageToServer);
            String messageFromServer = this.clockClientSocket.receiveFromServer();
            packetReceivedSet.add(id);
            messageFromServer += " " + Instant.now().toEpochMilli();
            System.out.println(messageFromServer);
            processTime(messageFromServer, id);
        } catch (IOException e) {
            System.out.println("The packet number " + id + " failed because " + e.getMessage() + " inside thread " + Thread.currentThread().getId());
            System.out.println("\n");
        }
    }

    int numberOfPacketsToSend() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the amount of time (in hours) you need to synchronize for ");
        System.out.println("Common Examples\n\t0.0166667 for 1 minutes\n\t0.16667 for 10 minutes\n\t0.5 for 30 minutes");
        System.out.print("Time --> ");
        float timeInHours = scanner.nextFloat();
        return (int) Math.rint((timeInHours * 60 * 60) / 10);
    }

    void processTime(String messages, int id) {
        String time[] = messages.split(" ", 0);
        StringBuilder printString = new StringBuilder();
        if (packetReceivedSet.last() <= id) {
            long t0 = Long.parseLong(time[4]);
            long t1 = Long.parseLong(time[3]);
            long t2 = Long.parseLong(time[2]);
            long t3 = Long.parseLong(time[1]);
            long rtt = (t2 - t3) + (t0 - t1);
            double theta = (double) rtt / 2;
            this.localCLientTime.setTimeInMillis((long) (t0 + theta));
            printString.append("\npacket - " + id + " rtt : " + rtt + "; theta : " + theta + " and current set time " + this.localCLientTime.getTime());
            long drift = (((long) (t0 + theta)) - t3);
            printString.append(" Drift :  "+drift);

        } else {

            droppedPacketsSet.add(id);

            printString.append("\nPacket " + id + " came too late");

        }

        try {
            this.fileWriter.write(printString.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}