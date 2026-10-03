package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.AddressRequest;
import com.shopsphere.ecommerce.dto.SavedAddressResponse;
import com.shopsphere.ecommerce.entity.Address;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.AddressNotFoundException;
import com.shopsphere.ecommerce.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Every method is scoped to the logged-in user - no cross-user access. */
@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    private Address findOwned(User user, Long id) {
        // not found and "someone else's" look identical on purpose
        return addressRepository
                .findByIdAndUserIdAndDeletedFalse(id, user.getId())
                .orElseThrow(() ->
                        new AddressNotFoundException(
                                "Address not found with id: " + id));
    }

    private void copy(AddressRequest r, Address a) {
        a.setFullName(r.getFullName().trim());
        a.setPhone(r.getPhone().trim());
        a.setAddressLine(r.getAddressLine().trim());
        a.setCity(r.getCity().trim());
        a.setState(r.getState().trim());
        a.setPostalCode(r.getPostalCode().trim());
        a.setCountry(r.getCountry().trim());
    }

    @Transactional
    public SavedAddressResponse create(User user, AddressRequest request) {
        Address address = new Address();
        copy(request, address);
        address.setUser(user);
        return SavedAddressResponse.from(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<SavedAddressResponse> list(User user) {
        return addressRepository.findByUserIdAndDeletedFalse(user.getId())
                .stream()
                .map(SavedAddressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavedAddressResponse get(User user, Long id) {
        return SavedAddressResponse.from(findOwned(user, id));
    }

    @Transactional
    public SavedAddressResponse update(User user, Long id, AddressRequest request) {
        Address address = findOwned(user, id);
        copy(request, address);
        return SavedAddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void delete(User user, Long id) {
        Address address = findOwned(user, id);
        address.setDeleted(true);   // soft delete keeps old orders intact
        addressRepository.save(address);
    }
}
