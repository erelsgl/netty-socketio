/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.namespace;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.ClientOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.transport.NamespaceClient;

public class Namespace implements SocketIONamespace {

    public static final String DEFAULT_NAME = "";

    private final Set<SocketIOClient> clients = Collections.newSetFromMap(new ConcurrentHashMap<SocketIOClient, Boolean>());
    private final ConcurrentMap<String, EventEntry<?>> eventListeners =
                                                            new ConcurrentHashMap<String, EventEntry<?>>();
    private final Queue<DataListener<Object>> jsonObjectListeners = new ConcurrentLinkedQueue<DataListener<Object>>();
    private final Queue<DataListener<String>> messageListeners = new ConcurrentLinkedQueue<DataListener<String>>();
    private final Queue<ConnectListener> connectListeners = new ConcurrentLinkedQueue<ConnectListener>();
    private final Queue<DisconnectListener> disconnectListeners = new ConcurrentLinkedQueue<DisconnectListener>();

    private final String name;
    private final JsonSupport jsonSupport;

    public Namespace(String name, JsonSupport jsonSupport) {
        super();
        this.name = name;
        this.jsonSupport = jsonSupport;
    }

    public void addClient(SocketIOClient client) {
        clients.add(client);
    }

    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener) {
        EventEntry entry = eventListeners.get(eventName);
        if (entry == null) {
            entry = new EventEntry<T>(eventClass);
            EventEntry<?> oldEntry = eventListeners.putIfAbsent(eventName, entry);
            if (oldEntry != null) {
                entry = oldEntry;
            }
        }
        entry.addListener(listener);
        jsonSupport.addEventMapping(eventName, eventClass);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(SocketIOClient client, String eventName, Object data) {
        EventEntry entry = eventListeners.get(eventName);
        Queue<DataListener> listeners = entry.getListeners();
        for (DataListener dataListener : listeners) {
            dataListener.onData(client, data);
        }
    }

    @Override
    public void addJsonObjectListener(DataListener<Object> listener) {
        jsonObjectListeners.add(listener);
    }

    public Queue<DataListener<Object>> getJsonObjectListeners() {
        return jsonObjectListeners;
    }

    @Override
    public void addDisconnectListener(DisconnectListener listener) {
        disconnectListeners.add(listener);
    }

    public void onDisconnect(SocketIOClient client) {
        for (DisconnectListener listener : disconnectListeners) {
            listener.onDisconnect(client);
        }
        clients.remove(client);
        ((NamespaceClient)client).onDisconnect();
    }

    @Override
    public void addConnectListener(ConnectListener listener) {
        connectListeners.add(listener);
    }

    public void onConnect(SocketIOClient client) {
        for (ConnectListener listener : connectListeners) {
            listener.onConnect(client);
        }
    }

    @Override
    public void addMessageListener(DataListener<String> listener) {
        messageListeners.add(listener);
    }

    public Queue<DataListener<String>> getMessageListeners() {
        return messageListeners;
    }

    public void onMessage(SocketIOClient client, String data) {
        for (DataListener<String> listener : messageListeners) {
            listener.onData(client, data);
        }
    }

    public void onJsonObject(SocketIOClient client, Object data) {
        for (DataListener<Object> listener : jsonObjectListeners) {
            listener.onData(client, data);
        }
    }

    @Override
    public ClientOperations getBroadcastOperations() {
        return new BroadcastOperations(clients);
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Namespace other = (Namespace) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}