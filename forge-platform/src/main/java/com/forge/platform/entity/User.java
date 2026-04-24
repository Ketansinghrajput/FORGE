package com.forge.platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Getter @Setter
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @Column(precision = 19, scale = 4)
    private BigDecimal walletBalance = BigDecimal.ZERO;
}