package com.locnguyen.ecommerce.domains.brand.service;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.domains.brand.dto.BrandResponse;
import com.locnguyen.ecommerce.domains.brand.dto.CreateBrandRequest;
import com.locnguyen.ecommerce.domains.brand.dto.UpdateBrandRequest;
import com.locnguyen.ecommerce.domains.brand.entity.Brand;
import com.locnguyen.ecommerce.domains.brand.enums.BrandStatus;
import com.locnguyen.ecommerce.domains.brand.mapper.BrandMapper;
import com.locnguyen.ecommerce.domains.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        return brandRepository.findByStatusOrderBySortOrderAsc(BrandStatus.ACTIVE)
                .stream().map(brandMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        return brandMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        if (brandRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.SLUG_ALREADY_EXISTS);
        }
        Brand brand = new Brand();
        brand.setName(request.getName().trim());
        brand.setSlug(request.getSlug().trim());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setDescription(request.getDescription());
        brand.setStatus(BrandStatus.ACTIVE);
        brand = brandRepository.save(brand);
        log.info("Brand created: id={} name={}", brand.getId(), brand.getName());
        return brandMapper.toResponse(brand);
    }

    @Transactional
    public BrandResponse updateBrand(Long id, UpdateBrandRequest request) {
        Brand brand = findOrThrow(id);
        if (request.getName() != null) brand.setName(request.getName().trim());
        if (request.getSlug() != null) {
            String newSlug = request.getSlug().trim();
            if (!newSlug.equals(brand.getSlug()) && brandRepository.existsBySlug(newSlug)) {
                throw new AppException(ErrorCode.SLUG_ALREADY_EXISTS);
            }
            brand.setSlug(newSlug);
        }
        if (request.getLogoUrl() != null) brand.setLogoUrl(request.getLogoUrl());
        if (request.getDescription() != null) brand.setDescription(request.getDescription());
        if (request.getStatus() != null) brand.setStatus(BrandStatus.valueOf(request.getStatus()));
        brand = brandRepository.save(brand);
        log.info("Brand updated: id={}", id);
        return brandMapper.toResponse(brand);
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = findOrThrow(id);
        String actor = com.locnguyen.ecommerce.common.utils.SecurityUtils.getCurrentUsernameOrSystem();
        brand.softDelete(actor);
        brandRepository.save(brand);
        log.info("Brand deleted: id={} by={}", id, actor);
    }

    private Brand findOrThrow(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
    }
}
