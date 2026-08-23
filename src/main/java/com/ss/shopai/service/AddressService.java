package com.ss.shopai.service;

import com.ss.shopai.dto.request.AddressRequest;
import com.ss.shopai.dto.response.AddressResponse;
import com.ss.shopai.entity.Address;
import com.ss.shopai.entity.User;
import com.ss.shopai.enums.AddressType;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.AddressRepository;
import com.ss.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;
        boolean shouldBeDefault = isFirstAddress || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault) {
            clearExistingDefault(userId);
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry() == null || request.getCountry().isBlank() ? "India" : request.getCountry())
                .addressType(request.getAddressType() == null ? AddressType.HOME : request.getAddressType())
                .isDefault(shouldBeDefault)
                .build();

        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = findByIdAndUser(addressId, userId);

        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            address.setCountry(request.getCountry());
        }
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            clearExistingDefault(userId);
            address.setDefault(true);
        }

        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findByIdAndUser(addressId, userId);
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserId(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = findByIdAndUser(addressId, userId);
        clearExistingDefault(userId);
        address.setDefault(true);
        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public Address getAddressEntity(Long userId, Long addressId) {
        return findByIdAndUser(addressId, userId);
    }

    private void clearExistingDefault(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(existing -> {
            existing.setDefault(false);
            addressRepository.save(existing);
        });
    }

    private Address findByIdAndUser(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .build();
    }
}