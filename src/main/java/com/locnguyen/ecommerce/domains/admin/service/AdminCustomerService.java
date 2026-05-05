package com.locnguyen.ecommerce.domains.admin.service;

import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.admin.dto.AdminCustomerFilter;
import com.locnguyen.ecommerce.domains.admin.dto.AdminCustomerResponse;
import com.locnguyen.ecommerce.domains.admin.dto.UpdateCustomerRequest;
import com.locnguyen.ecommerce.domains.admin.dto.UpdateCustomerStatusRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminCustomerService {

    PagedResponse<AdminCustomerResponse> getCustomers(AdminCustomerFilter filter, Pageable pageable);

    AdminCustomerResponse getCustomerById(UUID id);

    AdminCustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request);

    AdminCustomerResponse updateCustomerStatus(UUID id, UpdateCustomerStatusRequest request);

    void deleteCustomer(UUID id);
}
