package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.storage.DataStore;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore dataStore = DataStore.getInstance();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        String roomId = sensor == null ? null : sensor.getRoomId();
        if (roomId == null || !dataStore.getRooms().containsKey(roomId)) {
            throw new LinkedResourceNotFoundException(roomId);
        }

        String sensorId = UUID.randomUUID().toString();
        Sensor created = new Sensor(
                sensorId,
                sensor.getType(),
                sensor.getStatus(),
                sensor.getCurrentValue(),
                roomId
        );

        dataStore.getSensors().put(sensorId, created);
        dataStore.getSensorReadings().putIfAbsent(sensorId, new ArrayList<>());
        dataStore.getRooms().get(roomId).getSensorIds().add(sensorId);

        URI location = uriInfo.getAbsolutePathBuilder().path(sensorId).build();
        return Response.created(location).entity(created).build();
    }

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(dataStore.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(sensor -> sensor.getType() != null && sensor.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    @GET
    @Path("{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "Sensor not found");
            body.put("sensorId", sensorId);
            return Response.status(Response.Status.NOT_FOUND).entity(body).build();
        }

        return Response.ok(sensor).build();
    }

    @Path("{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
