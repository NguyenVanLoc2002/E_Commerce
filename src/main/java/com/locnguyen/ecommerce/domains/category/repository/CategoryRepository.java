package com.locnguyen.ecommerce.domains.category.repository;

import com.locnguyen.ecommerce.domains.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

    List<Category> findByStatusOrderBySortOrderAsc(com.locnguyen.ecommerce.domains.category.enums.CategoryStatus status);

    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);
}
