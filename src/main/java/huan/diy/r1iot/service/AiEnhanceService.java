package huan.diy.r1iot.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface AiEnhanceService {

    String systemInfo = "你是一个智能音箱，简短回答问题。";

    JsonNode buildRequest(String userInput);

    String responseToUser(JsonNode request);

}
