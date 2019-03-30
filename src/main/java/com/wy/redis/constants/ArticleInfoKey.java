package com.wy.redis.constants;

/**
 * @author wangyong
 * @description
 * @date 2019-03-30
 */
public interface ArticleInfoKey {
    /**
     * 文章名称
     */
    String ARTICLE = "article";
    /**
     * 文章链接
     */
    String LINK = "link";
    /**
     * 发布者
     */
    String POSTER = "poster";
    /**
     * 发布时间
     */
    String TIME = "time";
    /**
     * 投票数
     */
    String VOTES = "votes";

    String OPPOSED_VOTES = "opposed_votes";
    /**
     * 每一票的分值
     */
    Integer VOTE_SCORE = 432;
    /**
     *
     */
    Integer PAGE_SIZE = 20;
}
