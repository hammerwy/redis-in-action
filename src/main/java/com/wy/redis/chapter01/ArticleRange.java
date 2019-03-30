package com.wy.redis.chapter01;

import com.wy.redis.constants.ArticleInfoKey;
import com.wy.redis.constants.ExprTime;
import com.wy.redis.utils.JedisPoolUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

/**
 * @author wangyong
 * @description
 * @date 2019-03-29
 */
@Slf4j
public class ArticleRange {

    private Jedis connection = JedisPoolUtil.getConn();

    {
        connection.select(0);
    }

    /**
     * 发布文章 需要添加redis事务
     *
     * @param user
     * @param title
     * @param link
     * @return
     */
    public String postArticle(String user, String title, String link) {
        //生成文章id，incr命令是使key自增1
        String articleId = String.valueOf(connection.incr("article:"));
        //创建为当前文章投票的人的集合，并把文章的发布者添加到集合中，并设置过期时间为一周
        String voted = "voted:" + articleId;
        connection.sadd(voted, user);
        connection.expire(voted, ExprTime.EXRP_WEEK);

        //保存文章信息
        Long timestamp = System.currentTimeMillis();
        Map<String, String> articleInfo = new HashMap<String, String>();
        articleInfo.put(ArticleInfoKey.ARTICLE, title);
        articleInfo.put(ArticleInfoKey.LINK, link);
        articleInfo.put(ArticleInfoKey.POSTER, user);
        articleInfo.put(ArticleInfoKey.TIME, timestamp.toString());
        articleInfo.put(ArticleInfoKey.VOTES, "1");
        articleInfo.put(ArticleInfoKey.OPPOSED_VOTES, "0");
        String article = "article:" + articleId;
        connection.hmset(article, articleInfo);
        //保存文章发布的时间排名
        connection.zadd("score:", timestamp + ArticleInfoKey.VOTE_SCORE, article);
        //保存文章的投票分值排名
        connection.zadd("post_time", timestamp, article);
        return articleId;
    }

    /**
     * 对文章进行投票，需要添加redis事务
     *
     * @param user
     * @param article
     */
    public void voteArticle(String user, String article) {
        Long cutoff = System.currentTimeMillis() / 1000 - ExprTime.EXRP_WEEK;
        if (connection.zscore("post_time", article) < cutoff) {
            return;
        }

        String articleId = article.substring(article.indexOf(":") + 1);
        //如果当前用户并没有对这篇文章投过票，则把用户信息添加到当前文章的用户set中
        if (connection.sadd("voted:" + articleId, user) == 1) {
            connection.zincrby("score:", ArticleInfoKey.VOTE_SCORE, article);
            //当前文章的投票数加1
            connection.hincrBy(article, ArticleInfoKey.VOTES, 1);
        }

    }

    /**
     * 对文章投反对票
     *
     * @param user
     * @param article
     */
    public void voteOpposed(String user, String article) {
        Long cutoff = System.currentTimeMillis() / 1000 - ExprTime.EXRP_WEEK;
        if (connection.zscore("post_time", article) < cutoff) {
            return;
        }
        String articleId = article.substring(article.indexOf(":") + 1);
        //首先要先在投赞同票的集合中移除当前用户,并把文章的投票数减1
        connection.srem("voted:" + articleId, user);
        connection.hincrBy(article, ArticleInfoKey.VOTES, -1);
        //如果当前用户并没有对当前文章投反对票，则把当前用户添加到当前文章对应的支对用户set中
        if (connection.sadd("opposed_voted:" + articleId, user) == 1) {
            //把文章的分值减去一个单位的大小
            connection.zincrby("score:", -ArticleInfoKey.VOTE_SCORE, article);
            connection.hincrBy(article, ArticleInfoKey.OPPOSED_VOTES, 1);
        }
    }

    /**
     * 获取当前页的文章id，文章根据分值进行排名
     *
     * @param page
     * @return
     */
    public List<Map<String, String>> getArticles(int page) {
        return getArticles(page, "score:");
    }

    /**
     * 获取当前页的文章id
     *
     * @param page
     * @param orderby 排名依据，分值或者发布时间
     * @return
     */
    public List<Map<String, String>> getArticles(int page, String orderby) {
        //当前页的起止
        Integer startIndex = (page - 1) * ArticleInfoKey.PAGE_SIZE;
        Integer endIndex = startIndex + ArticleInfoKey.PAGE_SIZE;

        Set<String> ids = connection.zrevrange(orderby, startIndex, endIndex);
        List<Map<String, String>> result = new ArrayList<>();
        for (String id : ids) {
            Map<String, String> articleInfo = connection.hgetAll(id);
            result.add(articleInfo);
        }
        return result;
    }

    /**
     * 把文章添加到分组
     *
     * @param articleId
     * @param toAddGroupIDs 需要添加到的分组
     */
    public void addArticleToGroup(String articleId, String[] toAddGroupIDs) {
        String article = "article:" + articleId;
        for (String group : toAddGroupIDs) {
            connection.sadd("group:" + group, article);
        }
    }

    /**
     * 把文章从分组中移除
     *
     * @param articleId
     * @param fromRemoveGroupIDs 需要移除的分组
     */
    public void removeArticleFromGroup(String articleId, String[] fromRemoveGroupIDs) {
        String article = "article:" + articleId;
        for (String group : fromRemoveGroupIDs) {
            connection.srem("group:" + group, article);
        }
    }

    /**
     * 根据分值列出某个组的所有文章
     *
     * @param group
     * @param page
     * @return
     */
    public List<Map<String, String>> getGroupArticles(String group, int page) {
        return getGroupArticles(group, page, "score:");
    }

    /**
     * 列出某个组的所有文章
     *
     * @param group
     * @param page
     * @param orderby 排序依据
     * @return
     */
    public List<Map<String, String>> getGroupArticles(String group, int page, String orderby) {
        //聚合后的结果保存的集合
        String destinationKey = orderby + group;
        if (!connection.exists(destinationKey)) {
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
            //聚合保留分值最大的
            connection.zinterstore(destinationKey, params, "group:" + group, orderby);
            connection.expire(destinationKey, 60);
        }
        return getArticles(page, destinationKey);
    }

    public static void main(String[] args) {
        ArticleRange articleRange = new ArticleRange();
        //清除所有缓存
        articleRange.connection.flushAll();

        String articleId = articleRange.postArticle("wy", "titleA", "http://www.google.com");
        System.out.println(articleId);

        //释放连接与连接池
        articleRange.connection.close();
        JedisPoolUtil.closePool();
    }
}
