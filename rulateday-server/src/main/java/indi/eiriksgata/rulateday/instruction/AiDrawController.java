package indi.eiriksgata.rulateday.instruction;

import com.alibaba.fastjson.JSONObject;
import indi.eiriksgata.dice.injection.InstructReflex;
import indi.eiriksgata.dice.injection.InstructService;
import indi.eiriksgata.dice.reply.CustomText;
import indi.eiriksgata.dice.vo.MessageData;
import indi.eiriksgata.rulateday.event.EventAdapter;
import indi.eiriksgata.rulateday.event.EventUtils;
import indi.eiriksgata.rulateday.service.OtherApiService;
import indi.eiriksgata.rulateday.vo.AiTextDrawVo;
import indi.eiriksgata.rulateday.websocket.client.EventType;
import indi.eiriksgata.rulateday.websocket.client.WebSocketClientInit;
import indi.eiriksgata.rulateday.websocket.client.vo.AiTextDrawGenVo;
import indi.eiriksgata.rulateday.websocket.client.vo.WsDataBean;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import org.mindrot.jbcrypt.BCrypt;

@InstructService
public class AiDrawController {

    @InstructReflex(value = {"ai-text-draw"}, priority = 3)
    public String textDrawImage(MessageData<?> data) {
        AiTextDrawVo aiTextDrawVo = JSONObject.parseObject(data.getMessage(), AiTextDrawVo.class);

        if (aiTextDrawVo.getPrompt() == null || aiTextDrawVo.getPrompt().equals("")) {
            return CustomText.getText("ai.text.draw.prompt.null");
        }

        if (!WebSocketClientInit.webSocketClient.isOpen()) {
            return CustomText.getText("net.ws.link.fail");
        }

        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                AiTextDrawGenVo genVo = new AiTextDrawGenVo();
                genVo.setGroupId(event.getGroup().getId());
                genVo.setCreatedById(event.getSender().getId());
                if (aiTextDrawVo.getTranslate() == 1) {
                    genVo.setPrompt(OtherApiService.translateToEnglishByYouDu(aiTextDrawVo.getPrompt()));
                    genVo.setNegativePrompt(OtherApiService.translateToEnglishByYouDu(aiTextDrawVo.getNegativePrompt()));
                } else {
                    genVo.setPrompt(aiTextDrawVo.getPrompt());
                    genVo.setNegativePrompt(aiTextDrawVo.getNegativePrompt());
                }
                genVo.setTranslate(false);
                genVo.setSamplingSteps(aiTextDrawVo.getSamplingSteps());
                genVo.setPictureShape(aiTextDrawVo.getPictureShape());
                WsDataBean<AiTextDrawGenVo> wsDataBean = new WsDataBean<>();
                wsDataBean.setEventId(BCrypt.gensalt());
                wsDataBean.setCurrentTimestamp(System.currentTimeMillis());
                wsDataBean.setEventType(EventType.AI_TEXT_DRAW_TASK_CREATED);
                wsDataBean.setData(genVo);

                WebSocketClientInit.webSocketClient.send(
                        JSONObject.toJSONString(wsDataBean)
                );
            }
        });

        return CustomText.getText("ai.text.draw.task.success");
    }

}