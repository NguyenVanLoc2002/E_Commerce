package com.locnguyen.ecommerce.domains.auth.mapper;

import com.locnguyen.ecommerce.domains.auth.dto.UserResponse;
import com.locnguyen.ecommerce.domains.user.entity.Role;
import com.locnguyen.ecommerce.domains.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps User entity to auth DTOs. Deliberately does NOT expose the
 * password hash or internal audit fields in any response.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @org.mapstruct.Mapping(target = "roles", source = "roles", qualifiedByName = "toRoleNames")
    UserResponse toUserResponse(User user);

    /**
     * Converts the entity's Set<Role> to a Set<String> of role names.
     */
    @Named("toRoleNames")
    default Set<String> toRoleNames(Set<Role> roles) {
        if (roles == null) return Collections.emptySet();
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}
