package com.lin.elm.conf;

/**
 * 缓存 key 命名集中管理（避免 key 散落在各处拼错）
 * 命名规范：cache:业务:维度:值
 */
public class CacheKeys {

    /** 商家列表：按餐饮分类 */
    public static String businessList(Integer orderTypeId) {
        return "cache:business:type:" + orderTypeId;
    }

    /** 商家详情 */
    public static String businessInfo(Integer businessId) {
        return "cache:business:info:" + businessId;
    }

    /** 菜品列表：按商家 */
    public static String foodList(Integer businessId) {
        return "cache:food:business:" + businessId;
    }

    /** 菜品详情 */
    public static String foodInfo(Integer foodId) {
        return "cache:food:info:" + foodId;
    }

    /** 整个项目的缓存前缀，清缓存时用 */
    public static final String PREFIX = "cache:";
}
