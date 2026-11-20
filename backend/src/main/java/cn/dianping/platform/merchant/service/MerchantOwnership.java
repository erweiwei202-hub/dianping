package cn.dianping.platform.merchant.service;

public interface MerchantOwnership {

    boolean isOwner(long userId, long merchantId);
}
