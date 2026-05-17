package com.articleservice.mapper;

import com.articleservice.entity.Board;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BoardMapper {
    /**
     * 新增版块记录：管理员创建新版块时调用。
     */
    @Insert("""
            insert into tb_board(board_name, board_code, description, sort_order, status)
            values(#{boardName}, #{boardCode}, #{description}, #{sortOrder}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Board board);

    /**
     * 按 id 查询启用中的版块。
     */
    @Select("select * from tb_board where id = #{id} and status = 1")
    Board selectById(Long id);

    /**
     * 按版块编码查询版块：用于创建版块前检查编码是否重复。
     */
    @Select("select * from tb_board where board_code = #{boardCode} limit 1")
    Board selectByCode(String boardCode);

    /**
     * 查询所有启用中的版块：按排序值和 id 从小到大排列。
     */
    @Select("select * from tb_board where status = 1 order by sort_order asc, id asc")
    List<Board> selectEnabledList();

    /**
     * 批量按 id 查询启用中的版块：文章列表页一次性补齐版块名称。
     */
    @Select({"<script>",
            "select * from tb_board where id in",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "and status = 1",
            "</script>"})
    List<Board> selectByIds(@Param("ids") List<Long> ids);
}
