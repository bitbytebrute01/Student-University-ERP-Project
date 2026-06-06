package com.university.erp.gui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tiny in-process UI event bus for component refresh notifications.
 * Used only by UI components to coordinate refreshes when backend changes occur.
 *
 * Added subscribeWithHandle which returns a Runnable to unsubscribe safely.
 */
public class UIEventBus {
    private static final Map<String, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    public static void subscribe(String topic, Consumer<Object> listener) {
        subscribeWithHandle(topic, listener); // ignore handle
    }

    public static Runnable subscribeWithHandle(String topic, Consumer<Object> listener) {
        Objects.requireNonNull(topic);
        Objects.requireNonNull(listener);
        listeners.computeIfAbsent(topic, k -> new ArrayList<>()).add(listener);
        return () -> {
            List<Consumer<Object>> list = listeners.get(topic);
            if (list != null) list.remove(listener);
        };
    }

    public static void unsubscribe(String topic, Consumer<Object> listener) {
        List<Consumer<Object>> list = listeners.get(topic);
        if (list != null) list.remove(listener);
    }

    public static void publish(String topic, Object payload) {
        List<Consumer<Object>> list = listeners.get(topic);
        if (list == null) return;
        // dispatch on EDT
        SwingUtilities.invokeLater(() -> {
            for (Consumer<Object> l : new ArrayList<>(list)) {
                try {
                    l.accept(payload);
                } catch (Exception ignored) {
                    // do not fail entire dispatch
                }
            }
        });
    }
}
