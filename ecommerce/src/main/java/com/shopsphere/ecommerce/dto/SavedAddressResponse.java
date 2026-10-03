package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.Address;

/** Address returned to its owner (includes the id, unlike AddressResponse). */
public class SavedAddressResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    public SavedAddressResponse() {
    }

    public static SavedAddressResponse from(Address a) {
        SavedAddressResponse r = new SavedAddressResponse();
        r.id = a.getId();
        r.fullName = a.getFullName();
        r.phone = a.getPhone();
        r.addressLine = a.getAddressLine();
        r.city = a.getCity();
        r.state = a.getState();
        r.postalCode = a.getPostalCode();
        r.country = a.getCountry();
        return r;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
}
