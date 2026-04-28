package com.locnguyen.ecommerce.domains.promotion.repository;

import com.locnguyen.ecommerce.domains.promotion.entity.PromotionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.UUID;
public interface PromotionRuleRepository extends JpaRepository<PromotionRule, UUID> {

    List<PromotionRule> findByPromotionId(UUID promotionId);

    void deleteByPromotionId(UUID promotionId);
}
