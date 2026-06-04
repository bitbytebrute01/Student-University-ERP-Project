package com.university.threads;

import com.university.interfaces.Notifiable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NotificationThread extends Thread implements Notifiable {
    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private String name;

    public NotificationThread(String name) {
        this.name = name;
    }

    @Override
    public void sendNotification(String message) {
        messageQueue.add(message);
    }

    @Override
    public String getContactInfo() {
        return "System Channel: " + name;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String msg = messageQueue.take();
                System.out.println("[Notification - " + name + "] BROADCAST: " + msg);
            } catch (InterruptedException e) {
                System.out.println("Notification thread interrupted.");
                break;
            }
        }
    }
}
