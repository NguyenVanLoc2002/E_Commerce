package com.locnguyen.ecommerce.domains.address.service;

import com.locnguyen.ecommerce.domains.address.dto.AddressResponse;
import com.locnguyen.ecommerce.domains.address.dto.CreateAddressRequest;
import com.locnguyen.ecommerce.domains.address.dto.UpdateAddressRequest;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    List<AddressResponse> getMyAddresses();

    AddressResponse getAddressById(UUID addressId);

    AddressResponse createAddress(CreateAddressRequest request);

    AddressResponse updateAddress(UUID addressId, UpdateAddressRequest request);

    void deleteAddress(UUID addressId);
}
