package wethinkcode.web;


import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import wethinkcode.loadshed.common.transfer.DayDO;
import wethinkcode.loadshed.common.transfer.ScheduleDO;
import wethinkcode.loadshed.common.transfer.SlotDO;
import wethinkcode.places.model.Town;
import wethinkcode.schedule.handlyScheduleDO;

import java.time.LocalDate;
import java.util.*;

public class WebServiceController {

    private static final int PROVINCE_PORT = 7000;
    private static final int STAGE_PORT = 7001;
    private static final int SCHEDULE_PORT = 7002;


    // store the stage
    public static int stage;
    // store provinces
    public static HttpResponse<JsonNode> provinces;
    public static Collection<Town> towns;

    public static String provinceId;

    // A handler that is called when the user selects a province. It gets the provinceID from the form, and then calls the
    // API to get the towns for that province. It then renders the towns on the page.
    public static final Handler view = context -> {
        initJsonMapper();

        Map<String, Object> viewModel = new HashMap<>();

        /// run listener for the stageservice
        stage = WebService.initTrackStage();
        provinces = Unirest.get( serverUrl( PROVINCE_PORT ) +"/provinces").asJson();

        viewModel.put("loadshedstage", stage);
        viewModel.put("provinces", provinces.getBody().getArray());

        context.render("/index.html", viewModel);
    };


    // A handler that is called when the user selects a province. It gets the provinceID from the form, and then calls the
    // API to
    // get the towns for that province. It then renders the towns on the page.
    public static final Handler viewProvinces = context -> {
        initJsonMapper();

        provinceId = context.formParamAsClass("provinceId", String.class).get();

        Map<String, Object> viewModel = new HashMap<>();

        HttpResponse<JsonNode> getTownsInProvince = Unirest.get(serverUrl( PROVINCE_PORT ) + "/towns/" + provinceId)
                .asJson();

        towns = townObjects( getTownsInProvince.getBody().getArray() );

        viewModel.put("provinceId", provinceId );
        viewModel.put("towns", towns );
        viewModel.put("loadshedstage", WebService.initTrackStage() );
        viewModel.put("provinces", provinces );

        context.render("/index.html", viewModel);

    };


    // A handler that is called when the user selects a town. It gets the townID from the form, and then calls the API to
    // get the schedule for that town. It then renders the schedule on the page.
    public static Handler viewTown = context -> {
        initJsonMapper();

        Map<String, Object> viewModel = new HashMap<>();

        String townID = context.formParamAsClass("towns", String.class).get();

        HttpResponse<ScheduleDO> getSchedule = Unirest.get( serverUrl( SCHEDULE_PORT ) + "/" + provinceId +
                        "/" + townID + "/" + stage)
                .asObject(ScheduleDO.class);

        Collection<handlyScheduleDO> schedule = toScheduleObject(getSchedule.getBody());

        viewModel.put("provinceId", provinceId);
        viewModel.put("townId", townID);
        viewModel.put("towns", towns);
        viewModel.put("provinces", provinces.getBody().getArray());
        viewModel.put("loadshedstage", WebService.initTrackStage() );
        viewModel.put("schedule", schedule);
        context.render("/index.html", viewModel);

    };

    private static String serverUrl(int port) {
        return "http://localhost:"+ port;
    }

    /**
     * > We're creating a new Jackson ObjectMapper, registering the JavaTimeModule, disabling the
     * SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, and then setting the Unirest ObjectMapper to the new
     * JacksonObjectMapper
     */
    public static void initJsonMapper(){
        final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule( new JavaTimeModule() );
        mapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        Unirest.config().setObjectMapper( new kong.unirest.jackson.JacksonObjectMapper( mapper ) );
    }

    /**
     * It takes a JSONArray of JSONObjects, and returns a Collection of Town objects
     *
     * @param array The JSONArray that contains the towns.
     * @return A collection of Town objects.
     */
    private static Collection<Town> townObjects(JSONArray array) {
        Collection<Town> townsInThatProvince = new ArrayList();

        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            townsInThatProvince.add(new Town(object.getString("name"), object.getString("province")));
        }

        return townsInThatProvince;
    }

    /**
     * It takes a ScheduleDO object and returns a Collection of handlyScheduleDO objects
     *
     * @param responseSchedule The response from the API
     * @return A collection of handlyScheduleDO objects.
     */
    private static Collection<handlyScheduleDO> toScheduleObject(ScheduleDO responseSchedule) {

        Collection<handlyScheduleDO> allSchedule = new ArrayList();
        List<DayDO> loadSheddingDays1 = responseSchedule.getDays();
        LocalDate startDate1 = responseSchedule.getStartDate();
        int start = 0;
        for(DayDO vc :loadSheddingDays1){
            allSchedule.add(new handlyScheduleDO( startDate1.plusDays(start)));
            for(SlotDO cx : vc.getSlots()){
                allSchedule.add(new handlyScheduleDO( cx ));
            }
            start ++;
        }
        return allSchedule;

    }
}