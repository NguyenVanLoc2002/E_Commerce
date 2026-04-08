package com.locnguyen.ecommerce.domains.address.mapper;

import com.locnguyen.ecommerce.domains.address.dto.AddressResponse;
import com.locnguyen.ecommerce.domains.address.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Maps Address entity ↔ DTOs. Does NOT expose the customer foreign key.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressMapper {

    @Mapping(target = "fullAddress", source = ".", qualifiedByName = "buildFullAddress")
    AddressResponse toResponse(Address address);

    /**
     * Builds a human-readable full address string from component fields.
     * Example: "123 Nguyen Hue, Ben Nghe, Quan 1, TP. Ho Chi Minh 700000"
     */
    @Named("buildFullAddress")
    default String buildFullAddress(Address address) {
        if (address == null) return null;

        StringBuilder sb = new StringBuilder();
        append(sb, address.getStreetAddress());
        append(sb, address.getWard());
        append(sb, address.getDistrict());
        append(sb, address.getCity());
        append(sb, address.getPostalCode());
        return sb.toString();
    }

    private void append(StringBuilder sb, String part) {
        if (part != null && !part.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.trim());
        }
    }
}
