package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.storage.DataStore;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore dataStore = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(dataStore.getRooms().values());
        return Response.ok(rooms).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        String roomId = UUID.randomUUID().toString();
        Room created = new Room(
                roomId,
                room == null ? null : room.getName(),
                room == null ? 0 : room.getCapacity(),
                room == null || room.getSensorIds() == null ? new ArrayList<>() : new ArrayList<>(room.getSensorIds())
        );

        dataStore.getRooms().put(roomId, created);
        URI location = uriInfo.getAbsolutePathBuilder().path(roomId).build();

        return Response.created(location).entity(created).build();
    }

    @GET
    @Path("{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRooms().get(roomId);
        if (room == null) {
            return roomNotFound(roomId);
        }

        return Response.ok(room).build();
    }

    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRooms().get(roomId);
        if (room == null) {
            return roomNotFound(roomId);
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        dataStore.getRooms().remove(roomId);
        return Response.noContent().build();
    }

    private Response roomNotFound(String roomId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Room not found");
        body.put("roomId", roomId);
        return Response.status(Response.Status.NOT_FOUND).entity(body).build();
    }
}
