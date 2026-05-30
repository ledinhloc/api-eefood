package com.eefood.reactionservice.model.payment;

import com.eefood.reactionservice.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "diamond_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DiamondPackage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long diamondAmount;

    @Column(nullable = false)
    private Long bonusDiamond = 0L;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 10)
    private String currency = "VND";

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "diamondPackage")
    private List<Transaction> transactions;
}
