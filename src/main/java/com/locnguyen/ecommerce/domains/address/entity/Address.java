package com.locnguyen.ecommerce.domains.address.entity;

import com.locnguyen.ecommerce.common.auditing.SoftDeleteEntity;
import com.locnguyen.ecommerce.domains.address.enums.AddressType;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer address — supports Vietnamese address structure.
 *
 * <p>Structure: {@code street → ward → district → city}.
 * One customer can have multiple addresses (home, office, etc.).
 * Only one address can be marked as default per customer (enforced in service layer).
 *
 * <p>Soft-deletable per database guidelines.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "receiver_name", length = 100, nullable = false)
    private String receiverName;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "street_address", length = 255, nullable = false)
    private String streetAddress;

    @Column(name = "ward", length = 100, nullable = false)
    private String ward;

    @Column(name = "district", length = 100, nullable = false)
    private String district;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", length = 20, nullable = false)
    private AddressType addressType = AddressType.SHIPPING;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress = false;

    @Column(name = "label", length = 50)
    private String label;
}
