package com.university.threads;

import com.university.interfaces.Notifiable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NotificationThread extends Thread implements Notifiable {
    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private String name;

    public NotificationThread(String name) {
        this.name = name;
        // Start a background daemon to periodically clean orphaned uploaded files
        Thread cleaner = new Thread(() -> {
            try {
                while (true) {
                    try {
                        // Clean files older than 6 hours
                        com.university.utils.OrphanedFileCleaner.cleanOrphanedFiles(6);
                    } catch (Exception e) {
                        System.err.println("Orphaned file cleaner error: " + e.getMessage());
                    }
                    Thread.sleep(java.util.concurrent.TimeUnit.HOURS.toMillis(6));
                }
            } catch (InterruptedException ignored) {
            }
        });
        cleaner.setDaemon(true);
        cleaner.setName("OrphanedFileCleaner-Thread");
        cleaner.start();
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
