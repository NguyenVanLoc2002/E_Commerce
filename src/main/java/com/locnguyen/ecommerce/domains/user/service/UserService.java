package com.locnguyen.ecommerce.domains.user.service;

import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.user.dto.UpdateProfileRequest;
import com.locnguyen.ecommerce.domains.user.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getMyProfile();

    UserProfileResponse updateMyProfile(UpdateProfileRequest request);

    Customer getCurrentCustomer();
}
