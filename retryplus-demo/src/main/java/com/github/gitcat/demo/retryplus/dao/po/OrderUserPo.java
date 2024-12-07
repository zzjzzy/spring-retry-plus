package com.github.gitcat.demo.retryplus.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("order_user")
public class OrderUserPo {

    @TableId
    private String orderId;

    private String userId;
}
