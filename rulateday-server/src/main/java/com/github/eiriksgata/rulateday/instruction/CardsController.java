package com.github.eiriksgata.rulateday.instruction;

import com.github.eiriksgata.rulateday.event.EventUtils;
import com.github.eiriksgata.rulateday.mapper.CardsGroupDataMapper;
import com.github.eiriksgata.rulateday.pojo.CardsGroupData;
import com.github.eiriksgata.rulateday.pojo.CardsTypeList;
import com.github.eiriksgata.trpg.dice.injection.InstructReflex;
import com.github.eiriksgata.trpg.dice.injection.InstructService;
import com.github.eiriksgata.trpg.dice.reply.CustomText;
import com.github.eiriksgata.trpg.dice.vo.MessageData;
import com.github.eiriksgata.rulateday.event.EventAdapter;
import com.github.eiriksgata.rulateday.mapper.CardsTypeListMapper;
import com.github.eiriksgata.rulateday.utlis.MyBatisUtil;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupTempMessageEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.ArrayList;
/**
 * author: create by Keith
 * version: v1.0
 * description: com.github.eiriksgata.rulateday.instruction
 * date: 2021/4/19
 **/
@InstructService
public class CardsController {

    public static CardsGroupDataMapper cardsGroupDataMapper = MyBatisUtil.getSqlSession().getMapper(CardsGroupDataMapper.class);
    public static CardsTypeListMapper cardsTypeListMapper = MyBatisUtil.getSqlSession().getMapper(CardsTypeListMapper.class);

    @InstructReflex(value = {"cards"})
    public String cardsList(MessageData<?> data) {
        //cardsGroupDataMapper.newtest();
        List<CardsTypeList> lists = cardsTypeListMapper.selectAll();
        if (lists.size() == 0) {
            return CustomText.getText("cards.type.not.found");
        }
        StringBuilder result = new StringBuilder();
        result.append(CustomText.getText("cards.type.list.title"));
        AtomicInteger count = new AtomicInteger();
        lists.forEach((cardsTypeList) -> {
            count.getAndIncrement();
            result.append("\n").append(count.get()).append(". ").append(cardsTypeList.getName());
        });
        return result.toString();
    }

    @InstructReflex(value = {"cardsAdd", "cardsadd"}, priority = 3)
    public String cardsAdd(MessageData<?> data) {
        String[] parsingData = data.getMessage().split(" ");
        if (parsingData.length < 2) {
            return CustomText.getText("cards.add.parameter.format.error");
        }
        CardsTypeList cardsTypeList = new CardsTypeList();
        cardsTypeList.setName(parsingData[0]);
        cardsTypeList.setContent(data.getMessage().substring(parsingData[0].length() + 1));
        try {
            cardsTypeListMapper.insert(cardsTypeList);
            MyBatisUtil.getSqlSession().commit();
        } catch (Exception e) {
            return CustomText.getText("cards.add.error");
        }
        return CustomText.getText("cards.add.success");
    }

    @InstructReflex(value = {"cardsDel", "cardsdel"}, priority = 3)
    public String cardsDel(MessageData<?> data) {
        cardsTypeListMapper.deleteByName(data.getMessage());
        MyBatisUtil.getSqlSession().commit();
        return CustomText.getText("cards.delete.success");
    }


    @InstructReflex(value = {"drawAdd", "drawadd"}, priority = 3)
    public String drawAdd(MessageData<?> data) {
        if (data.getMessage().equals("") || data.getMessage() == null) {
            return CustomText.getText("cards.draw.not.parameter");
        }
        CardsTypeList cardsTypeList = cardsTypeListMapper.selectByName(data.getMessage());
        if (cardsTypeList == null) {
            return CustomText.getText("cards.draw.not.found");
        }
        String[] addDataArr = cardsTypeList.getContent().split(",");
        final Long[] groupId = new Long[1];
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                groupId[0] = event.getGroup().getId();
            }

            @Override
            public void friend(FriendMessageEvent event) {
                groupId[0] = -event.getFriend().getId();
            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                groupId[0] = event.getGroup().getId();
            }
        });
        for (String anAddDataArr : addDataArr) {
            CardsGroupData cardsGroupData = new CardsGroupData();
            cardsGroupData.setTypeId(cardsTypeList.getId());
            cardsGroupData.setGroupId(groupId[0]);
            cardsGroupData.setValue(anAddDataArr);
            cardsGroupData.setAutoShuffle(false);
            cardsGroupDataMapper.insert(cardsGroupData);
        }
        MyBatisUtil.getSqlSession().commit();
        return CustomText.getText("cards.draw.add.success", cardsTypeList.getName());
    }

    @InstructReflex(value = {"drawList", "drawlist"}, priority = 3)
    public String drawList(MessageData<?> data) {
        final Long[] groupId = new Long[1];
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                groupId[0] = event.getGroup().getId();
            }

            @Override
            public void friend(FriendMessageEvent event) {
                groupId[0] = -event.getFriend().getId();
            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                groupId[0] = event.getGroup().getId();
            }
        });
        List<CardsGroupData> list = cardsGroupDataMapper.getGroupCardsList(groupId[0]);
        if (list.size() <= 0) {
            return CustomText.getText("cards.draw.not.data");
        }
        StringBuilder result = new StringBuilder();

        result.append(CustomText.getText("cards.draw.list")).append("(" + String.valueOf(list.size()) + ")").append(" - [");
        list.forEach(cardsGroupData -> result.append(cardsGroupData.getValue()).append(","));
        result.delete(result.length() - 1, result.length());
        result.append("]");
        return result.toString();
    }

    @InstructReflex(value = {"drawHide", "drawhide"}, priority = 3)
    public String drawHideOut(MessageData<?> data) {
        final Long[] groupId = new Long[1];
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                groupId[0] = event.getGroup().getId();
                CardsGroupData result = cardsGroupDataMapper.randomGetCard(groupId[0]);
                event.getSender().sendMessage(
                        CustomText.getText("cards.draw.hide.group.result",
                                event.getGroup().getId(), result.getValue())
                );
            }

            @Override
            public void friend(FriendMessageEvent event) {
                groupId[0] = -event.getFriend().getId();
                CardsGroupData result = cardsGroupDataMapper.randomGetCard(groupId[0]);
                event.getSender().sendMessage(
                        CustomText.getText("cards.draw.hide.friend.result",
                                event.getFriend().getId(), result.getValue())
                );

            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                groupId[0] = event.getGroup().getId();
                CardsGroupData result = cardsGroupDataMapper.randomGetCard(groupId[0]);
                event.getSender().sendMessage(
                        CustomText.getText("cards.draw.hide.group.result",
                                event.getGroup().getId(), result.getValue())
                );
            }
        });
        CardsGroupData result = cardsGroupDataMapper.randomGetCard(groupId[0]);
        if (result == null) {
            return CustomText.getText("cards.draw.not.data");
        }
        cardsGroupDataMapper.deleteById(result.getId());
        MyBatisUtil.getSqlSession().commit();
        return CustomText.getText("cards.draw.hide.success");
    }

    @InstructReflex(value = {"draw"}, priority = 2)
    public String drawOut(MessageData<?> data) {
        int countNumber = 1;
        if (data.getMessage().equals("") || data.getMessage() == null) {
            countNumber = 1;
        }
        else
        {
            String MessageContent = data.getMessage().trim();
            if(MessageContent != null && MessageContent.matches("-?\\d+(\\.\\d+)?"))
            {
                if(Integer.parseInt(MessageContent) > 0)
                {
                    countNumber = Integer.parseInt(MessageContent);
                }
            }
            else
            {
                return CustomText.getText("dice.en.parameter.format.error");
            }
        }
        final Long[] groupId = new Long[1];
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                groupId[0] = event.getGroup().getId();
            }

            @Override
            public void friend(FriendMessageEvent event) {
                groupId[0] = -event.getFriend().getId();
            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                groupId[0] = event.getGroup().getId();
            }
        });

        List<CardsGroupData> list = cardsGroupDataMapper.getGroupCardsList(groupId[0]);//获取剩余卡组
        int listSize = list.size();//剩余卡组数量
        CardsGroupData resultHelper = cardsGroupDataMapper.randomGetCard(groupId[0]);
        Boolean needAutoShuffle = cardsGroupDataMapper.GetGeoupCardAutoShuffle(groupId[0]);
        if(needAutoShuffle == null)
        {
            needAutoShuffle = false;
        }
        int arrayLength = 0;
        List<String> addDataList = new ArrayList<>();
        Long cardListTypeId = 0L;
        if(listSize > 0)
        {
            CardsTypeList cardsTypeList = cardsTypeListMapper.selectById(cardsGroupDataMapper.GetGeoupCardTypeId(groupId[0]));
            String[] addDataArr = cardsTypeList.getContent().split(",");//获取目标完整卡组
            arrayLength = addDataArr.length;//目标完整卡组数量
            addDataList = Arrays.asList(addDataArr);
            cardListTypeId = cardsTypeList.getId();
        }
        String[] currentValueList = new String[arrayLength];
        if(needAutoShuffle)
        {
            if(listSize <= countNumber && arrayLength > countNumber)
            {
                for(int i = 0; i < list.size(); i = i + 1)
                {
                    currentValueList[i] = list.get(i).getValue();//记录剩余卡组的所有Value
                }
            }

        }
        String resultList = "";
        for(int i = 0; i < countNumber; i = i + 1)
        {
            CardsGroupData result = cardsGroupDataMapper.randomGetCard(groupId[0]);
            if (result == null) {
                if(resultList=="")
                {
                    return CustomText.getText("cards.draw.not.data");
                }
            }

            if(resultList == "")
            {
                resultList = result.getValue();
            }
            else
            {
                resultList = result.getValue() + " | " + resultList;
            }
            cardsGroupDataMapper.deleteById(result.getId());
            MyBatisUtil.getSqlSession().commit();

            CardsGroupData nextResult = cardsGroupDataMapper.randomGetCard(groupId[0]);
            if (nextResult == null) {
                if(needAutoShuffle)
                {
                    if(listSize > 0)
                    {
                        for (int j = 0; j < addDataList.size(); j = j + 1) {//将剩余此次未被抽到的卡洗牌
                            String anAddDataArr = addDataList.get(j);
                            Boolean arrContains = Arrays.asList(currentValueList).contains(anAddDataArr);
                            if(!arrContains)
                            {
                                CardsGroupData cardsGroupDataTemp = new CardsGroupData();
                                cardsGroupDataTemp.setTypeId(cardListTypeId);
                                cardsGroupDataTemp.setGroupId(groupId[0]);
                                cardsGroupDataTemp.setValue(anAddDataArr);
                                cardsGroupDataTemp.setAutoShuffle(true);
                                cardsGroupDataMapper.insert(cardsGroupDataTemp);
                            }
                        }
                    }
                }
            }
        }

        return CustomText.getText("cards.draw.success", resultList);
    }

    @InstructReflex(value = {"drawclear", "drawClear"}, priority = 3)
    public String drawClear(MessageData<?> data) {
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                cardsGroupDataMapper.clearByGroupId(event.getGroup().getId());
                MyBatisUtil.getSqlSession().commit();
            }

            @Override
            public void friend(FriendMessageEvent event) {
                cardsGroupDataMapper.clearByGroupId(-event.getFriend().getId());
                MyBatisUtil.getSqlSession().commit();
            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                cardsGroupDataMapper.clearByGroupId(event.getGroup().getId());
                MyBatisUtil.getSqlSession().commit();
            }
        });
        return CustomText.getText("cards.draw.clear");
    }

    @InstructReflex(value = {"drawreload", "drawReload"}, priority = 3)
    public String drawClear(MessageData<?> data) {
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                cardsGroupDataMapper.clearByGroupId(event.getGroup().getId());
                MyBatisUtil.getSqlSession().commit();
            }

            @Override
            public void friend(FriendMessageEvent event) {
                cardsGroupDataMapper.clearByGroupId(-event.getFriend().getId());
                MyBatisUtil.getSqlSession().commit();
            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                cardsGroupDataMapper.clearByGroupId(event.getGroup().getId());
                MyBatisUtil.getSqlSession().commit();
            }
        });
        return CustomText.getText("cards.draw.clear");
    }

    @InstructReflex(value = {"drawautoon", "drawAutoOn"}, priority = 3)
    public String drawAutoOn(MessageData<?> data) {
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                cardsGroupDataMapper.updateGroupCardsListAutoShuffle(event.getGroup().getId(), true);
                MyBatisUtil.getSqlSession().commit();
            }

            @Override
            public void friend(FriendMessageEvent event) {
                cardsGroupDataMapper.updateGroupCardsListAutoShuffle(-event.getFriend().getId(), true);
                MyBatisUtil.getSqlSession().commit();
            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                cardsGroupDataMapper.updateGroupCardsListAutoShuffle(event.getGroup().getId(), true);
                MyBatisUtil.getSqlSession().commit();
            }
        });
        return CustomText.getText("cards.draw.auto.on");
    }

    @InstructReflex(value = {"drawautooff", "drawAutoOff"}, priority = 3)
    public String drawAutoOff(MessageData<?> data) {
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                cardsGroupDataMapper.updateGroupCardsListAutoShuffle(event.getGroup().getId(), false);
                MyBatisUtil.getSqlSession().commit();
            }

            @Override
            public void friend(FriendMessageEvent event) {
                cardsGroupDataMapper.updateGroupCardsListAutoShuffle(-event.getFriend().getId(), false);
                MyBatisUtil.getSqlSession().commit();
            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                cardsGroupDataMapper.updateGroupCardsListAutoShuffle(event.getGroup().getId(), false);
                MyBatisUtil.getSqlSession().commit();
            }
        });
        return CustomText.getText("cards.draw.auto.off");
    }


}
