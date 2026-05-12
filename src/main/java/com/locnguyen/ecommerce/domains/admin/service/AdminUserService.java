package com.locnguyen.ecommerce.domains.admin.service;

import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.admin.dto.AdminUserFilter;
import com.locnguyen.ecommerce.domains.admin.dto.CreateUserRequest;
import com.locnguyen.ecommerce.domains.admin.dto.UpdateUserRequest;
import com.locnguyen.ecommerce.domains.auth.dto.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminUserService {

    UserResponse createUser(CreateUserRequest request);

    PagedResponse<UserResponse> getUsers(AdminUserFilter filter, Pageable pageable);

    UserResponse getUserById(UUID id);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void deleteUser(UUID id);
}
