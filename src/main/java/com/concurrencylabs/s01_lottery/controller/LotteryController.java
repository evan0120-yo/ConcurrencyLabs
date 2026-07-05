package com.concurrencylabs.s01_lottery.controller;

import com.concurrencylabs.s01_lottery.object.dto.DrawReq;
import com.concurrencylabs.s01_lottery.object.dto.DrawResp;
import com.concurrencylabs.s01_lottery.usecase.DrawLotteryUsecase;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 抽獎入口（SD §5：POST /api/lottery/{activityId}/draw）。 */
@RestController
@RequestMapping("/api/lottery")
public class LotteryController {

    private final DrawLotteryUsecase drawLotteryUsecase;

    public LotteryController(DrawLotteryUsecase drawLotteryUsecase) {
        this.drawLotteryUsecase = drawLotteryUsecase;
    }

    @PostMapping("/{activityId}/draw")
    public DrawResp draw(@PathVariable Long activityId, @RequestBody DrawReq req) {
        return drawLotteryUsecase.draw(activityId, req);
    }
}
