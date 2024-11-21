package com.github.eiriksgata.rulateday.mapper;

import com.github.eiriksgata.rulateday.pojo.CardsGroupData;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * author: create by Keith
 * version: v1.0
 * description: com.github.eiriksgata.rulateday.mapper
 * date: 2021/4/19
 **/
@Mapper
public interface CardsGroupDataMapper {

    @Insert("insert into cards_group_data (id,group_id,type_id,value, auto_shuffle) values (#{id},#{groupId},#{typeId},#{value},#{autoShuffle})")
    void insert(CardsGroupData cardsGroupData);

    @Delete("delete from cards_group_data where id = #{id}")
    void deleteById(@Param("id") Long id);

    @Delete("delete from cards_group_data where group_id = #{groupId}")
    void clearByGroupId(@Param("groupId") Long groupId);

    @Select("select * from cards_group_data where group_id =#{groupId} ORDER BY RANDOM() LIMIT 1")
    CardsGroupData randomGetCard(@Param("groupId") Long groupId);

    @Select("select * from cards_group_data where group_id = #{groupId}")
    List<CardsGroupData> getGroupCardsList(@Param("groupId") Long groupId);

    @Update("update cards_group_data set auto_shuffle = #{autoShuffle} where group_id = #{groupId}")
    void updateGroupCardsListAutoShuffle(@Param("groupId") Long groupId, @Param("autoShuffle") Boolean autoShuffle);

    @Select("select type_id from cards_group_data where group_id =#{groupId} LIMIT 1")
    Long GetGeoupCardTypeId(@Param("groupId") Long groupId);

    @Select("select auto_shuffle from cards_group_data where group_id =#{groupId} LIMIT 1")
    Boolean GetGeoupCardAutoShuffle(@Param("groupId") Long groupId);

    // @Update("alter table cards_group_data add column auto_shuffle boolean default false")
    // void newtest();

}
