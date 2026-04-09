package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.storage.DataStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final DataStore dataStore = DataStore.getInstance();
    private final String sensorId;
    private final Sensor sensor;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
        this.sensor = dataStore.getSensors().get(sensorId);
    }

    @GET
    public Response getReadings() {
        if (sensor == null) {
            return sensorNotFound();
        }

        List<SensorReading> readings = dataStore.getSensorReadings().get(sensorId);
        if (readings == null) {
            readings = new ArrayList<>();
        }

        return Response.ok(readings).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        if (sensor == null) {
            return sensorNotFound();
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        SensorReading created = new SensorReading(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                reading == null ? 0.0 : reading.getValue()
        );

        dataStore.getSensorReadings().computeIfAbsent(sensorId, key -> new ArrayList<>()).add(created);
        sensor.setCurrentValue(created.getValue());

        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    private Response sensorNotFound() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Sensor not found");
        body.put("sensorId", sensorId);
        return Response.status(Response.Status.NOT_FOUND).entity(body).build();
    }
}
