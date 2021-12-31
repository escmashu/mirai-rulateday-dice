package indi.eiriksgata.rulateday.instruction;

import indi.eiriksgata.dice.operation.RollBasics;
import indi.eiriksgata.dice.operation.RollRole;
import indi.eiriksgata.dice.operation.impl.RollRoleImpl;
import indi.eiriksgata.dice.utlis.RegularExpressionUtils;
import indi.eiriksgata.dice.vo.MessageData;
import indi.eiriksgata.dice.config.DiceConfig;
import indi.eiriksgata.dice.exception.DiceInstructException;
import indi.eiriksgata.dice.exception.ExceptionEnum;
import indi.eiriksgata.dice.injection.InstructService;
import indi.eiriksgata.dice.injection.InstructReflex;
import indi.eiriksgata.dice.operation.DiceSet;
import indi.eiriksgata.dice.operation.impl.RollBasicsImpl;
import indi.eiriksgata.dice.reply.CustomText;
import indi.eiriksgata.rulateday.event.EventAdapter;
import indi.eiriksgata.rulateday.event.EventUtils;
import indi.eiriksgata.rulateday.service.HumanNameService;
import indi.eiriksgata.rulateday.service.UserTempDataService;
import indi.eiriksgata.rulateday.service.impl.HumanNameServiceImpl;
import indi.eiriksgata.rulateday.service.impl.UserTempDataServiceImpl;
import indi.eiriksgata.rulateday.utlis.CharacterUtils;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupTempMessageEvent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.security.SecureRandom;
import java.util.List;

/**
 * author: create by Keith
 * version: v1.0
 * description: indi.eiriksgata.dice
 * date:2020/9/24
 **/

@InstructService
public class RollController {

    @Resource
    public static final UserTempDataService userTempDataService = new UserTempDataServiceImpl();

    @Resource
    public static final RollBasics rollBasics = new RollBasicsImpl();

    @Resource
    public static final DiceSet diceSet = new DiceSet();

    @Resource
    public static final RollRole rollRole = new RollRoleImpl();

    @Resource
    public static final HumanNameService humanNameService = new HumanNameServiceImpl();

    @InstructReflex(value = {".ra", ".rc", "。ra", "。rc"}, priority = 2)
    public String attributeCheck(MessageData<?> data) {
        String attribute = userTempDataService.getUserAttribute(data.getQqID());
        data.setMessage(data.getMessage().replaceAll(" ", ""));
        data.setMessage(CharacterUtils.operationSymbolProcessing(
                data.getMessage()
        ));
        if (attribute == null) {
            attribute = "";
        }
        try {
            return rollBasics.attributeCheck(data.getMessage(), attribute);
        } catch (DiceInstructException e) {
            e.printStackTrace();
            return CustomText.getText("dice.attribute.error");
        }
    }

    @InstructReflex(value = {".st", "。st"})
    public String setAttribute(MessageData<?> data) {
        if (data.getMessage().equals("")) {
            return CustomText.getText("dice.set.attribute.error");
        }
        try {
            userTempDataService.updateUserAttribute(data.getQqID(), data.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return CustomText.getText("dice.set.attribute.error");
        }
        return CustomText.getText("dice.set.attribute.success");
    }

    @InstructReflex(value = {".r", "。r"})
    public String roll(MessageData<?> data) {
        Integer diceFace = userTempDataService.getUserDiceFace(data.getQqID());
        data.setMessage(CharacterUtils.operationSymbolProcessing(data.getMessage()));
        if (diceFace != null) {
            diceSet.setDiceFace(data.getQqID(), diceFace);
        }
        if (data.getMessage().equals("") || data.getMessage().equals(" ")) {
            return rollBasics.rollRandom("d", data.getQqID());
        } else {
            //正则筛选
            String result = RegularExpressionUtils.getMatcher("[0-9dD +\\-*/＋－×÷]+", data.getMessage());
            if (result != null) {
                return rollBasics.rollRandom(result, data.getQqID()) + data.getMessage().replace(result, "");
            }
            return rollBasics.rollRandom(data.getMessage(), data.getQqID());
        }
    }


    @InstructReflex(value = {".MessageData", ".set", "。set"})
    public String setDiceFace(MessageData<?> data) throws DiceInstructException {
        //移除所有的空格
        int setDiceFace;
        data.setMessage(data.getMessage().replaceAll(" ", ""));
        try {
            setDiceFace = Integer.parseInt(data.getMessage());
        } catch (Exception e) {
            return CustomText.getText("dice.set.face.error");
        }

        if (setDiceFace > Integer.parseInt(DiceConfig.diceSet.getString("dice.face.max"))) {
            throw new DiceInstructException(ExceptionEnum.DICE_SET_FACE_MAX_ERR);
        }
        if (setDiceFace <= Integer.parseInt(DiceConfig.diceSet.getString("dice.face.min"))) {
            throw new DiceInstructException(ExceptionEnum.DICE_SET_FACE_MIN_ERR);
        }
        diceSet.setDiceFace(data.getQqID(), setDiceFace);
        userTempDataService.updateUserDiceFace(data.getQqID(), setDiceFace);
        return CustomText.getText("dice.set.face.success", setDiceFace);
    }

    @InstructReflex(value = {".sc", "。sc"})
    public String sanCheck(MessageData<?> data) {
        data.setMessage(CharacterUtils.operationSymbolProcessing(data.getMessage()));

        //检查指令前缀空格符
        for (int i = 0; i < data.getMessage().length(); i++) {
            if (data.getMessage().charAt(i) != ' ') {
                data.setMessage(data.getMessage().substring(i));
                break;
            }
        }

        //优先检测指令是否包含有数值
        if (data.getMessage().matches("(([0-9]?[Dd][0-9]+|[Dd]|[0-9])\\+?)+/(([0-9]?[Dd][0-9]+|[Dd]|[0-9])\\+?)+ [0-9]+")) {
            //检测到包含数值 进行 空格符 分割 0为计算公式，1为给定的数值
            String[] tempArr = data.getMessage().split(" ");
            return rollBasics.sanCheck(tempArr[0], "san" + tempArr[1], (attribute, random, sanValue, calculationProcess, surplus) -> {
            });
        }

        //检测用户输入的指令格式是否正确
        if (data.getMessage().matches("(([0-9]?[Dd][0-9]+|[Dd]|[0-9])\\+?)+/(([0-9]?[Dd][0-9]+|[Dd]|[0-9])\\+?)+")) {
            //查询用户数据
            String attribute = userTempDataService.getUserAttribute(data.getQqID());
            String inputData = RegularExpressionUtils.getMatcher("(([0-9]?[Dd][0-9]+|[Dd]|[0-9])\\+?)+/(([0-9]?[Dd][0-9]+|[Dd]|[0-9])\\+?)+", data.getMessage());
            //要进行是否有用户属性确认
            //对于没有属性的用户 返回错误
            if (attribute == null) {
                return CustomText.getText("dice.sc.not-found.error");
            }

            return rollBasics.sanCheck(inputData, attribute, (resultAttribute, random, sanValue, calculationProcess, surplus) -> {
                //修改属性
                userTempDataService.updateUserAttribute(data.getQqID(), resultAttribute);
            });

        }
        return CustomText.getText("dice.sc.instruct.error");
    }

    @InstructReflex(value = {".rh", "。rh"}, priority = 3)
    public String rollHide(MessageData<?> data) {
        EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
            @Override
            public void group(GroupMessageEvent event) {
                event.getSender().sendMessage(roll(data));
            }

            @Override
            public void friend(FriendMessageEvent event) {
                event.getFriend().sendMessage(roll(data));
            }

            @Override
            public void groupTemp(GroupTempMessageEvent event) {
                event.getSender().sendMessage(roll(data));
            }
        });
        return CustomText.getText("coc7.roll.hide");
    }

    @InstructReflex(value = {".rb", "。rb", ",rb"}, priority = 3)
    public String rollBonusDice(MessageData<?> data) {
        data.setMessage(data.getMessage().replaceAll(" ", ""));
        data.setMessage(CharacterUtils.operationSymbolProcessing(data.getMessage()));

        String attribute = userTempDataService.getUserAttribute(data.getQqID());
        return rollBasics.rollBonus(data.getMessage(), attribute, true);
    }

    @InstructReflex(value = {".rp", "。rp", ",rp", ".Rp"}, priority = 3)
    public String rollPunishment(MessageData<?> data) {
        data.setMessage(data.getMessage().replaceAll(" ", ""));
        data.setMessage(CharacterUtils.operationSymbolProcessing(data.getMessage()));
        String attribute = userTempDataService.getUserAttribute(data.getQqID());
        return rollBasics.rollBonus(data.getMessage(), attribute, false);
    }

    @InstructReflex(value = {".coc", "。coc", ".Coc"})
    public String randomCocRole(MessageData<?> data) {
        int createNumber;
        createNumber = checkCreateRandomRoleNumber(data.getMessage());
        if (createNumber == -1) return CustomText.getText("dice.base.parameter.error");
        if (createNumber > 20 | createNumber < 1) {
            return CustomText.getText("coc7.role.create.size.max");
        }
        return rollRole.createCocRole(createNumber);
    }

    @InstructReflex(value = {".dnd", "。dnd", ".Dnd", "。DND"})
    public String randomDndRole(MessageData<?> data) {
        int createNumber;
        createNumber = checkCreateRandomRoleNumber(data.getMessage());
        if (createNumber == -1) return CustomText.getText("dice.base.parameter.error");
        if (createNumber > 20 | createNumber < 1) {
            return CustomText.getText("dr5e.role.create.size.max");
        }
        return rollRole.createDndRole(createNumber);
    }

    @InstructReflex(value = {".jrrp", ".JRRP", "。jrrp", ".todayRandom"})
    public String todayRandom(MessageData<?> data) {
        return rollBasics.todayRandom(data.getQqID(), 8);
    }


    @InstructReflex(value = {".name"})
    public String randomName(MessageData<?> data) {
        if (StringUtils.isNumeric(data.getMessage())) {
            int number;
            try {
                number = Integer.parseInt(data.getMessage());
            } catch (Exception e) {
                return CustomText.getText("dice.base.parameter.error");
            }
            if (number > 0 && number <= 20) {
                return humanNameService.randomName(Integer.parseInt(data.getMessage()));
            }
            return CustomText.getText("names.create.size.max");
        } else {
            return humanNameService.randomName(1);
        }
    }


    @InstructReflex(value = {".ga", "。ga"}, priority = 2)
    public String attributeGetAttribute(MessageData<?> data) {
        return userTempDataService.getUserAttribute(data.getQqID());
    }

    @InstructReflex(value = {".en"})
    public String attributeEn(MessageData<?> data) {
        if (data.getMessage().equals("")) {
            return CustomText.getText("dice.en.parameter.null");
        }
        String checkAttribute = RegularExpressionUtils.getMatcher("[\\u4E00-\\u9FA5A-z]+[0-9]+", data.getMessage());
        if (checkAttribute == null) {
            checkAttribute = RegularExpressionUtils.getMatcher("[\\u4E00-\\u9FA5A-z]+", data.getMessage());
            if (checkAttribute == null) {
                return CustomText.getText("dice.en.parameter.format.error");
            }

            String userAttribute = userTempDataService.getUserAttribute(data.getQqID());
            if (userAttribute == null || userAttribute.equals("")) {
                return CustomText.getText("dice.en.not.set.attribute");
            }

            String tempData = RegularExpressionUtils.getMatcher(checkAttribute + "[0-9]+", userAttribute);
            if (tempData == null) {
                return CustomText.getText("dice.en.not.found.attribute", checkAttribute);
            }
            int checkNumber = Integer.parseInt(tempData.substring(checkAttribute.length()));

            int randomNumber = new SecureRandom().nextInt(100);
            if (randomNumber > checkNumber) {
                int addValue = new SecureRandom().nextInt(10);
                int count = checkNumber + addValue;
                String updateAttribute = userAttribute.replaceAll(tempData, checkAttribute + count);
                userTempDataService.updateUserAttribute(data.getQqID(), updateAttribute);
                return CustomText.getText("dice.en.success",
                        randomNumber, checkNumber, checkAttribute, checkAttribute, addValue, checkNumber, count);
            }
            return CustomText.getText("dice.en.fail", randomNumber, checkNumber, checkAttribute);
        }

        int randomNumber = new SecureRandom().nextInt(100);
        int checkNumber = Integer.parseInt(RegularExpressionUtils.getMatcher("[0-9]+", checkAttribute));
        if (randomNumber > checkNumber) {
            int addValue = new SecureRandom().nextInt(10);
            int count = addValue + checkNumber;
            return CustomText.getText("dice.en.success",
                    randomNumber, checkNumber, checkAttribute, checkAttribute, addValue, checkNumber, count);
        }
        return CustomText.getText("dice.en.fail", randomNumber, checkNumber, checkAttribute);
    }

    @InstructReflex(value = {".sa", "。sa"})
    public String attributeSetAttribute(MessageData<?> data) {
        String changeValue = RegularExpressionUtils.getMatcher("[0-9]+", data.getMessage());
        if (changeValue == null) {
            return CustomText.getText("dice.sa.parameter.null");
        }
        String changeName = data.getMessage().substring(0, data.getMessage().length() - changeValue.length());
        if (changeName.equals("")) {
            return CustomText.getText("dice.sa.parameter.error");
        }

        //查询属性
        String findAttribute = userTempDataService.getUserAttribute(data.getQqID());
        if (findAttribute == null || findAttribute.equals("")) {
            return CustomText.getText("dice.sa.not.set.attribute");
        }

        String attribute = RegularExpressionUtils.getMatcher(changeName + "[0-9]+", findAttribute);
        if (attribute == null) {
            return CustomText.getText("dice.sa.not.found.attribute");
        }
        String updateData = findAttribute.replaceAll(attribute, changeName + changeValue);
        userTempDataService.updateUserAttribute(data.getQqID(), updateData);
        return CustomText.getText("dice.sa.update.success", attribute, changeName, changeValue);
    }

    @InstructReflex(value = {".ww", ".dp", "。ww"})
    public static String dicePoolGen(MessageData<?> data) {
        data.setMessage(data.getMessage().toLowerCase());
        data.setMessage(data.getMessage().trim());
        int diceNumber = 1;
        int addDiceCheck = 10;
        StringBuilder resultText = new StringBuilder();
        StringBuilder returnText = new StringBuilder();
        int count = 0;
        int successDiceCheck = 8;
        int diceFace = 10;
        if (data.getMessage().equals("") || data.getMessage() == null) {
            return CustomText.getText("dice.pool.parameter.format.error");
        }
        int repeat = 1;
        int index = data.getMessage().indexOf("#");
        if (index != -1) {
            try {
                repeat = Integer.parseInt(data.getMessage().substring(0, index));
            } catch (Exception e) {
                return CustomText.getText("dice.pool.parameter.format.error");
            }
        }


        List<String> parametersList = RegularExpressionUtils.getMatchers("[0-9]+|a[0-9]+|k[0-9]+|m[0-9]+|\\+[0-9]+|b[0-9]+", data.getMessage());
        if (parametersList.size() <= 0) {
            return CustomText.getText("dice.pool.parameter.format.error");
        }
        try {
            diceNumber = Integer.parseInt(parametersList.get(0));
            if (diceNumber < 0 || diceNumber > 300) {
                return CustomText.getText("dice.pool.parameter.range.error");
            }
            parametersList.remove(0);
        } catch (Exception e) {
            return CustomText.getText("dice.pool.parameter.format.error");
        }
        for (String parameter : parametersList) {
            switch (parameter.charAt(0)) {
                case 'a':
                    addDiceCheck = Integer.parseInt(parameter.substring(1));
                    break;
                case 'k':
                    successDiceCheck = Integer.parseInt(parameter.substring(1));
                    break;
                case 'm':
                    diceFace = Integer.parseInt(parameter.substring(1));
                    break;
                case '+':
                case 'b':
                    count += Integer.parseInt(parameter.substring(1));
                    break;
            }
        }
        returnText.append(diceNumber);
        returnText.append("a").append(addDiceCheck);
        returnText.append("k").append(successDiceCheck);
        returnText.append("m").append(diceFace);
        returnText.append("b").append(count);
        for (int i = 1; i < repeat; i++) {
            rollBasics.dicePoolCount(diceNumber, resultText, count, addDiceCheck, count, diceFace, successDiceCheck);
            EventUtils.eventCallback(data.getEvent(), new EventAdapter() {
                @Override
                public void group(GroupMessageEvent event) {
                    String senderName = event.getSender().getNameCard();
                    if (senderName.trim().equals("")) {
                        senderName = event.getSender().getNick();
                    }
                    event.getGroup().sendMessage(
                            "[" + senderName + "]" +
                                    CustomText.getText("dice.pool.result", returnText, resultText)
                    );
                }

                @Override
                public void friend(FriendMessageEvent event) {
                    event.getFriend().sendMessage(
                            CustomText.getText("dice.pool.result", returnText, resultText));
                }

                @Override
                public void groupTemp(GroupTempMessageEvent event) {
                    event.getGroup().sendMessage(
                            CustomText.getText("dice.pool.result", returnText, resultText));
                }
            });
            resultText.delete(0, resultText.length());
        }
        rollBasics.dicePoolCount(diceNumber, resultText, count, addDiceCheck, count, diceFace, successDiceCheck);
        return CustomText.getText("dice.pool.result", returnText, resultText);
    }


    private int checkCreateRandomRoleNumber(String message) {
        if (message.equals("")) {
            return 1;
        } else {
            try {
                return Integer.parseInt(message);
            } catch (Exception e) {
                return -1;
            }
        }
    }


}
