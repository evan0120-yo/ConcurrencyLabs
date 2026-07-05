package com.concurrencylabs.common.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 共用身分 / 積分服務（common/account）。無 auth，userId 直接傳入。
 *
 * <p>扣減不透支靠 DB 原子條件更新，非分散式鎖（見 DEVELOPMENT §五）。
 */
@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    /** 扣積分：原子條件更新，餘額不足則回 {@link DeductResult#INSUFFICIENT}，餘額不動。 */
    @Transactional
    public DeductResult deductPoints(String userId, long amount) {
        int rows = repository.deduct(userId, amount);
        return rows == 1 ? DeductResult.SUCCESS : DeductResult.INSUFFICIENT;
    }

    /** 退積分（跨儲存失敗時的補償）。 */
    @Transactional
    public void refundPoints(String userId, long amount) {
        repository.refund(userId, amount);
    }

    public long getBalance(String userId) {
        return repository.findById(userId)
                .map(User::getPoints)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
    }

    /** 冪等註冊（測試 / 初始化用）；已存在則不動。 */
    @Transactional
    public void register(String userId, long initialPoints) {
        if (!repository.existsById(userId)) {
            repository.save(new User(userId, initialPoints));
        }
    }
}
