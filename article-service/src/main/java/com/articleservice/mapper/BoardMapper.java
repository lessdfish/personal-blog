package com.articleservice.mapper;

import com.articleservice.entity.Board;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BoardMapper {
    @Insert("""
            insert into tb_board(board_name, board_code, description, sort_order, status)
            values(#{boardName}, #{boardCode}, #{description}, #{sortOrder}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Board board);

    @Select("select * from tb_board where id = #{id} and status = 1")
    Board selectById(Long id);

    @Select("select * from tb_board where board_code = #{boardCode} limit 1")
    Board selectByCode(String boardCode);

    @Select("select * from tb_board where status = 1 order by sort_order asc, id asc")
    List<Board> selectEnabledList();
}
