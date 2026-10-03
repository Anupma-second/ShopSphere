package com.shopsphere.ecommerce.dto;

public class AddressResponse {

    private String fullName;
    private String phone;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    public AddressResponse() {
    }

    public AddressResponse(String fullName, String phone,
                           String addressLine, String city,
                           String state, String postalCode,
                           String country) {

        this.fullName = fullName;
        this.phone = phone;
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }
}
