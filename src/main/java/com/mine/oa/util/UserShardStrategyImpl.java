package com.mine.oa.util;

import com.google.code.shardbatis.strategy.ShardStrategy;

/***
 *
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 *
 * @author liupeng
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class UserShardStrategyImpl implements ShardStrategy {
    @Override
    public String getTargetTableName(String baseTableName, Object params, String mapperId) {
        int index = (int) params % 2;
        return baseTableName + index/*(index != 0 ? index : "")*/;
    }
}
