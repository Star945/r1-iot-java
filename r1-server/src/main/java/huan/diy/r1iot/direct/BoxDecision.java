package huan.diy.r1iot.direct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.hass.HassServiceImpl;
import huan.diy.r1iot.service.music.IMusicService;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Data
public class BoxDecision {


    private static final ObjectMapper objectMapper;

    private static final ObjectNode intent;

    static {
        objectMapper = new ObjectMapper();
        intent = objectMapper.createObjectNode();

        ObjectNode operationsObj = objectMapper.createObjectNode();
        ArrayNode operations = objectMapper.createArrayNode();
        operationsObj.set("operations", operations);
        intent.set("intent", operationsObj);
        ObjectNode operator = objectMapper.createObjectNode();
        operations.add(operator);

        operator.put("operator", "ACT_PLAY");

    }

    public BoxDecision(Device device,
                       Map<String, IMusicService> musicServiceMap,
                       Map<String, IAudioService> audioServiceMap,
                       HassServiceImpl iotService) {
        this.device = device;
        this.musicServiceMap = musicServiceMap;
        this.audioServiceMap = audioServiceMap;
        this.iotService = iotService;
    }

    private Device device;
    private Map<String, IMusicService> musicServiceMap;
    private Map<String, IAudioService> audioServiceMap;
    private HassServiceImpl iotService;


    @Tool("""
            用于回答用户的一般提问
            """)
    String questionAnswer(@P("用户输入") String userInput) {
        log.info("questionAnswer: {}", userInput);
        R1IotUtils.REPLACE_ANSWER.set(true);
        return userInput;
    }

    @Tool("""
            用于处理播放音乐请求
            """)
    void playMusic(@P(value = "歌曲作者，可以为空字符串", required = false) String author,
                   @P(value = "歌曲名称，可以为空字符串", required = false) String songName,
                   @P(value = "歌曲搜索关键词，可以为空字符串", required = false) String keyword) {
        JsonNode musicResp = musicServiceMap.get(device.getMusicConfig().getChoice()).fetchMusics(new MusicAiResp(author, songName, keyword), device);
        JsonNode jsonNode = R1IotUtils.JSON_RET.get();
        ObjectNode ret = ((ObjectNode) jsonNode);
        ret.set("data", musicResp);
        ret.set("semantic", intent);
        ret.put("code", "SETTING_EXEC");
        ret.put("matchType", "FUZZY");

        ObjectNode general = objectMapper.createObjectNode();
        general.put("text", "好的，已为您播放");
        general.put("type", "T");
        ret.set("general", general);

        ret.remove("taskName");
        R1IotUtils.JSON_RET.set(ret);
    }

    @Tool("""
            音箱一般设置：氛围灯，音量，停止，休眠等等
            """)
    void voiceBoxSetting(@P("用户输入") String userInput) {
        log.info("Called voiceBoxSetting with userInput={}", userInput);
    }

    @Tool("""
            智能家居控制，比如打开灯、热得快，空调，调节温度，查询湿度，等等
            """)
    String homeassistant(String actionCommand) {
        log.info("Called homeassistant with  actionCommand={}", actionCommand);

        R1IotUtils.REPLACE_ANSWER.set(true);

        String tts = iotService.replaceOutPut(R1IotUtils.JSON_RET.get(), device.getId());
        if (tts.isEmpty()) {
            return actionCommand;
        }
        R1IotUtils.JSON_RET.set(R1IotUtils.sampleChatResp(tts));

        return tts;

    }

    @Tool("""
            用于播放新闻，比如体育、财经、科技、娱乐等等
            """)
    void playNews(@P("用户输入") String userInput) {
        log.info("Called playNews with userInput={}", userInput);
    }

    @Tool("""
            用于播放故事、广播等
            """)
    void playAudio(@P("关键词") String keyword) {
        log.info("Called playAudio with userInput={}", keyword);
    }

}