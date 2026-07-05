package com.concurrencylabs.common.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 共用使用者身分 + 積分餘額（common/account）。
 *
 * <p>無 auth：id（= userId）由呼叫端直接傳入，本模組不驗證。積分餘額 {@code points}
 * 恆 &gt;= 0，靠 DB 原子條件更新（{@link AccountRepository#deduct}）保證不透支。
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    private String id;

    /** 積分餘額（>= 0）。 */
    @Column(nullable = false)
    private long points;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User(String id, long points) {
        this.id = id;
        this.points = points;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}
