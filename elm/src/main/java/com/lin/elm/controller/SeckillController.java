package com.lin.elm.controller;

import com.lin.elm.po.Seckill;
import com.lin.elm.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 秒杀接口
 * 注意：userId 一律从 token 里取（和前面越权修复保持一致的风格）
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /** 秒杀列表 */
    @GetMapping("/list")
    public List<Seckill> list() {
        return seckillService.listOnSale();
    }

    /** 抢购 */
    @PostMapping("/do/{seckillId}")
    public Map<String, Object> doSeckill(@PathVariable("seckillId") Integer seckillId,
                                         HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return seckillService.doSeckill(userId, seckillId);
    }

    /** 查询抢购结果（前端用它轮询"排队中 → 抢购成功"） */
    @GetMapping("/result/{seckillId}")
    public Map<String, Object> result(@PathVariable("seckillId") Integer seckillId,
                                      HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return seckillService.getResult(userId, seckillId);
    }

    /** 把数据库库存重置到 Redis（演示/压测用） */
    @PostMapping("/resetStock")
    public int resetStock() {
        return seckillService.resetStock();
    }
}
