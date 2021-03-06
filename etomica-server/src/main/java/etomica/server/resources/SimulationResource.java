package etomica.server.resources;

import etomica.data.DataDump;
import etomica.data.DataPipeForked;
import etomica.meta.SimulationModel;
import etomica.server.dao.DataStreamStore;
import etomica.server.dao.SimulationStore;
import etomica.server.representations.SimClassInfo;
import etomica.server.representations.SimulationConstructor;
import etomica.simulation.Simulation;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Path("/simulations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SimulationResource {
    private final SimulationStore simStore;
    private final DataStreamStore dataStore;

    @Inject
    public SimulationResource(SimulationStore simStore, DataStreamStore dataStore) {
        this.simStore = simStore;
        this.dataStore = dataStore;
    }

    @GET
    @Path("{id}")
    public Map<String, Object> structure(@PathParam("id") String id) {
        UUID uuid = UUID.fromString(id);
        if(!simStore.containsKey(uuid)) {
            throw new WebApplicationException("Simulation instance not found", Response.Status.NOT_FOUND);
        }
        SimulationModel model = simStore.get(uuid);
        SimClassInfo classInfo = SimClassInfo.forClass(model.getSimulation().getClass());
        Map<String, Object> map = new HashMap<>();
        map.put("model", model);
        map.put("classInfo", classInfo);
        return map;
    }

    @POST
    public UUID createSimulation(@NotNull SimulationConstructor constructionParams) {
        UUID id = UUID.randomUUID();
        try {
            Simulation sim = (Simulation) Class.forName(constructionParams.className).newInstance();
            SimulationModel model = new SimulationModel(sim);
            simStore.put(id, model);

            model.getAllIdsOfType(DataPipeForked.class).stream()
                    .map(model::getWrapperById)
                    .forEach(wrapper -> {
                        DataPipeForked pipe = (DataPipeForked) wrapper.getWrapped();
                        DataDump dump = new DataDump();
                        pipe.addDataSink(dump);
                        this.dataStore.put(UUID.randomUUID(), new DataStreamStore.DataPlumbing(null, dump, id));
                    });

            return id;
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new ServerErrorException("Simulation class not found", Response.Status.NOT_FOUND);
        }
    }

    @DELETE
    @Path("{simId}")
    public void deleteInstance(@PathParam("simId") String simId) {
        UUID id = UUID.fromString(simId);
        SimulationModel model = simStore.get(id);
        model.getSimulation().getController().halt();
        simStore.remove(id);
    }


}
