package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    /**
     * 返回 userI d的起始行为offset，最长长度为limit的帖子列表（分页操作）
     * @param userId 选取userId的帖子，userId为0的时候表示所有人的帖子
     * @param offset 起始位置
     * @param limit 显示的帖子数量的最长长度
     * @return 返回帖子列表
     */
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    /*
    * @Param注解用于给参数起别名，以便在Mapper XML文件中引用该参数。
    * 当参数唯一，且sql中动态条件<if>需要用到该参数，必须取别名。
    * */
    int selectDiscussPostRows(@Param("userId") int userId);
}
