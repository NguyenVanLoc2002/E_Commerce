package com.locnguyen.ecommerce.domains.inventory.mapper;

import com.locnguyen.ecommerce.domains.inventory.dto.WarehouseResponse;
import com.locnguyen.ecommerce.domains.inventory.entity.Warehouse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    WarehouseResponse toResponse(Warehouse warehouse);
}
