package com.concurrencylabs.common.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * users 表存取。積分扣減 / 退回一律走「原子條件更新」，多台 server 安全（DB 行鎖）。
 */
public interface AccountRepository extends JpaRepository<User, String> {

    /**
     * 原子條件扣減：餘額 &gt;= amount 才扣。
     *
     * @return 受影響列數：1 = 扣成功；0 = 積分不足（餘額不動，永不扣成負）。
     */
    @Modifying
    @Query("update User u set u.points = u.points - :amount, u.updatedAt = CURRENT_TIMESTAMP "
            + "where u.id = :id and u.points >= :amount")
    int deduct(@Param("id") String id, @Param("amount") long amount);

    /** 原子退回（補償用）。 */
    @Modifying
    @Query("update User u set u.points = u.points + :amount, u.updatedAt = CURRENT_TIMESTAMP "
            + "where u.id = :id")
    int refund(@Param("id") String id, @Param("amount") long amount);
}
