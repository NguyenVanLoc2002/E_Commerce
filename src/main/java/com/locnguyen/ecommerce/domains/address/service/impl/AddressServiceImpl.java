package com.locnguyen.ecommerce.domains.address.service.impl;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.utils.SecurityUtils;
import com.locnguyen.ecommerce.domains.address.dto.AddressResponse;
import com.locnguyen.ecommerce.domains.address.dto.CreateAddressRequest;
import com.locnguyen.ecommerce.domains.address.dto.UpdateAddressRequest;
import com.locnguyen.ecommerce.domains.address.entity.Address;
import com.locnguyen.ecommerce.domains.address.mapper.AddressMapper;
import com.locnguyen.ecommerce.domains.address.repository.AddressRepository;
import com.locnguyen.ecommerce.domains.address.service.AddressService;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        Customer customer = userService.getCurrentCustomer();
        List<Address> addresses = addressRepository.findByCustomerIdAndDeletedFalse(
                customer.getId(),
                Sort.by(Sort.Order.desc("defaultAddress"), Sort.Order.desc("createdAt"))
        );
        return addresses.stream().map(addressMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UUID addressId) {
        Address address = findOwnedAddress(addressId);
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(CreateAddressRequest request) {
        Customer customer = userService.getCurrentCustomer();

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByCustomerId(customer.getId());
        }

        Address address = new Address();
        address.setCustomer(customer);
        address.setReceiverName(request.getReceiverName().trim());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreetAddress(request.getStreetAddress().trim());
        address.setWard(request.getWard().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setCity(request.getCity().trim());
        address.setPostalCode(request.getPostalCode());
        address.setAddressType(request.getAddressType());
        address.setDefaultAddress(Boolean.TRUE.equals(request.getIsDefault()));
        address.setLabel(request.getLabel());

        address = addressRepository.save(address);
        log.info("Address created: id={} customerId={}", address.getId(), customer.getId());

        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID addressId, UpdateAddressRequest request) {
        Address address = findOwnedAddress(addressId);

        if (request.getReceiverName() != null) {
            address.setReceiverName(request.getReceiverName().trim());
        }
        if (request.getPhoneNumber() != null) {
            address.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getStreetAddress() != null) {
            address.setStreetAddress(request.getStreetAddress().trim());
        }
        if (request.getWard() != null) {
            address.setWard(request.getWard().trim());
        }
        if (request.getDistrict() != null) {
            address.setDistrict(request.getDistrict().trim());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity().trim());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }
        if (request.getLabel() != null) {
            address.setLabel(request.getLabel());
        }
        if (request.getIsDefault() != null) {
            if (request.getIsDefault()) {
                addressRepository.clearDefaultByCustomerId(address.getCustomer().getId());
            }
            address.setDefaultAddress(request.getIsDefault());
        }

        address = addressRepository.save(address);
        log.info("Address updated: id={}", addressId);

        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID addressId) {
        Address address = findOwnedAddress(addressId);
        String actor = SecurityUtils.getCurrentUsernameOrSystem();
        address.softDelete(actor);
        addressRepository.save(address);
        log.info("Address deleted: id={} deletedBy={}", addressId, actor);
    }

    private Address findOwnedAddress(UUID addressId) {
        Customer customer = userService.getCurrentCustomer();
        Address address = addressRepository.findByIdAndDeletedFalse(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        return address;
    }
}
