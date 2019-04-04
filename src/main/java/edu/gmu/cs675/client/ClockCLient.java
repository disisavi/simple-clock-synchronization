package edu.gmu.cs675.client;

import org.decimal4j.util.DoubleRounder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
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
    Calendar localCLienTime;
    long timeOffset;
    List<Map.Entry<Long, Double>> smoothTheta;
    double averageTheta;
    double averageRoundTrip;
    float timeInHours;
    Map<Double, Integer> thetaFrequency;

    public ClockCLient(String ip) {

        this.id = 0;
        this.packetsToSend = this.numberOfPacketsToSend();
        this.droppedPacketsSet = new HashSet<>();
        this.timeOffset = System.nanoTime();
        this.thetaFrequency = new ConcurrentHashMap<>();
        this.packetReceivedSet = Collections.synchronizedSortedSet(new TreeSet<>());
        this.clockClientSocket = new ClockClientSocket(ip);
        this.localCLienTime = Calendar.getInstance();
        this.smoothTheta = new ArrayList<>();

        this.averageTheta = 0;
        this.averageRoundTrip = 0;

        this.initializeFile();
        this.initializeSchedule();
    }

    void initializeFile() {
        try {
            fileWriter = new PrintWriter("log.txt", "UTF-8");
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
        System.out.println("The current system time  = " + clockCLient.localCLienTime.getTime());
        System.out.println("\n\nThe number of packets to be sent is " + clockCLient.packetsToSend);
        clockCLient.scheduler.schedule(() -> clockCLient.manageClientShutdown(), clockCLient.packetsToSend * 10, TimeUnit.SECONDS);

        try {
            clockCLient.future.get();
        } catch (ExecutionException e) {
            System.out.println("Schedule thread failed because " + e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException | CancellationException ignored) {

        }
    }

    void setCurrentTime() {
        long timenow = System.nanoTime();
        timenow = Math.round((double) ((timenow - timeOffset) / 1000000));
        this.localCLienTime.setTimeInMillis(timenow);
    }

    void run() {
        id++;
        int id = this.id;
//        System.out.println("Sending packet " + id + " from Thread id " + Thread.currentThread().getId());
        this.setCurrentTime();
        String now = Long.toString(this.localCLienTime.getTimeInMillis());
        String messageToServer = id + " " + now;
        try {
            this.clockClientSocket.sendToServer(messageToServer);
            String messageFromServer = this.clockClientSocket.receiveFromServer();
            packetReceivedSet.add(id);
            messageFromServer += " " + Instant.now().toEpochMilli();
            System.out.println(messageFromServer + " Process Thread -->" + Thread.currentThread().getId());
            processTime(messageFromServer, id, packetReceivedSet.size());
        } catch (IOException e) {
            droppedPacketsSet.add(id);
            System.out.println("The packet number " + id + " failed because " + e.getMessage() + " inside thread " + Thread.currentThread().getId());
            System.out.println("\n");
        }
    }

    int numberOfPacketsToSend() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the amount of time (in hours) you need to synchronize for ");
        System.out.println("Common Examples\n\t0.0166667 for 1 minutes\n\t0.16667 for 10 minutes\n\t0.5 for 30 minutes");
        System.out.print("Time --> ");
        timeInHours = scanner.nextFloat();
        return (int) Math.rint((timeInHours * 60 * 60) / 10);
    }

    void processTime(String messages, int id, int size) {
        String time[] = messages.split(" ", 0);
        StringBuilder printString = new StringBuilder();
        if (packetReceivedSet.last() <= id) {
            long t0 = Long.parseLong(time[4]);
            long t1 = Long.parseLong(time[3]);
            long t2 = Long.parseLong(time[2]);
            long t3 = Long.parseLong(time[1]);

            long rtt = (t2 - t3) + (t0 - t1);
            double theta = (double) (((t2 - t3) - (t0 - t1)) / 2);

            double histTheta = DoubleRounder.round(theta,3);

            if (thetaFrequency.containsKey(histTheta)) {
                thetaFrequency.put(histTheta, (thetaFrequency.get(histTheta) + 1));
            } else {
                thetaFrequency.put(histTheta, 1);
            }

            if (smoothTheta.size() == 8) {
                smoothTheta.remove(0);
            }
            smoothTheta.add(new AbstractMap.SimpleEntry<>(rtt, theta));

            long low = Integer.MAX_VALUE;
            Double newTheta = -1.0;
            for (Map.Entry<Long, Double> entry : smoothTheta) {
                if (entry.getKey() <= low) {
                    low = entry.getKey();
                    newTheta = entry.getValue();
                }
            }

            long newTime = (long) (t0 + newTheta);

            this.localCLienTime.setTimeInMillis(newTime);
            this.timeOffset = System.nanoTime();
            Calendar hwTime = Calendar.getInstance();
            hwTime.setTimeInMillis(t0);


            printString.append("\npacket - " + id +
                    ":- rtt : " + rtt + "; theta : " + theta + "; Smoothed Theta : " + newTheta + "; --  Hardware Time " + t3
                    + " V/S and current set time " + newTime);
            printString.append("\n\tHW time (in minutes and seconds)" + hwTime.getTime() + " vs New time " + this.localCLienTime.getTime() + "\n");
            averageTheta = ((averageTheta * (size - 1)) + theta) / size;
            averageRoundTrip = (averageRoundTrip * (size - 1) + rtt) / size;

        } else {
            droppedPacketsSet.add(id);
        }

        try {
            this.fileWriter.write(printString.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void manageClientShutdown() {

        try {
            if (droppedPacketsSet.size() > 0) {
                this.fileWriter.write("\n--> Dropped Packets List -- ");
                for (Integer packetId : this.droppedPacketsSet) {
                    this.fileWriter.write(" " + packetId + ",");
                }
            }
            this.fileWriter.close();

            fileWriter = new PrintWriter("Results.txt", "UTF-8");
            int packetsProcessed = this.packetsToSend - this.droppedPacketsSet.size();
            float packetsProcessedPercentage = (float) (this.droppedPacketsSet.size() * 100 / this.packetsToSend);
            StringBuilder reportMessage = new StringBuilder();
            reportMessage.append("---------- Report and Stats -------------\n");
            reportMessage.append("\n1. Hours of operations....").append(this.timeInHours).append(" (").append(this.timeInHours * 60).append(" minutes)");
            reportMessage.append("\n2. Number of Packets Processed ").append(packetsProcessed);
            reportMessage.append("\n3. Number of dropped Packets ").append(this.droppedPacketsSet.size());
            reportMessage.append("\n4. Percentage of dropped Packets ").append(packetsProcessedPercentage);
            reportMessage.append("\n5. Average rtt ").append(this.averageRoundTrip);
            reportMessage.append("\n5. Average theta ").append(this.averageTheta);
            reportMessage.append("\n6. The Histogram --> ").append(this.thetaFrequency);
            this.fileWriter.write(reportMessage.toString());
            this.fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.future.cancel(true);
        this.scheduler.shutdown();

        System.out.println("\n The program will now terminate");
    }
}