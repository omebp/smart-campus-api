package com.smartcampus.storage;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        preloadData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    public ConcurrentHashMap<String, Room> getRooms() {
        return rooms;
    }

    public ConcurrentHashMap<String, Sensor> getSensors() {
        return sensors;
    }

    public ConcurrentHashMap<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }

    private void preloadData() {
        Room room1 = new Room("LIB-301", "Library Quiet Study", 50, new ArrayList<>());
        Room room2 = new Room("LAB-101", "Computer Science Lab", 30, new ArrayList<>());
        Room room3 = new Room("HALL-01", "Main Hall", 500, new ArrayList<>());

        rooms.put(room1.getId(), room1);
        rooms.put(room2.getId(), room2);
        rooms.put(room3.getId(), room3);

        Sensor sensor1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor sensor2 = new Sensor("CO2-001", "CO2", "ACTIVE", 450.0, "LAB-101");
        Sensor sensor3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LAB-101");
        Sensor sensor4 = new Sensor("TEMP-002", "Temperature", "OFFLINE", 19.0, "HALL-01");

        addSensor(sensor1);
        addSensor(sensor2);
        addSensor(sensor3);
        addSensor(sensor4);
    }

    private void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        sensorReadings.putIfAbsent(sensor.getId(), new ArrayList<>());

        Room room = rooms.get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().add(sensor.getId());
        }
    }
}
