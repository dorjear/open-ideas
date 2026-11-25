package com.demo.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository repository;
    private final AgifyClient agifyClient;

    public CustomerService(CustomerRepository repository, AgifyClient agifyClient) {
        this.repository = repository;
        this.agifyClient = agifyClient;
    }

    public List<Customer> getAll() {
        return repository.findAll();
    }

    public Optional<Customer> getById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        Integer age = agifyClient.getAge(request.firstName());
        Customer customer = new Customer(request.firstName(), request.lastName(), request.email());
        customer.setAge(age);
        return repository.save(customer);
    }

    @Transactional
    public Optional<Customer> update(Long id, CustomerRequest request) {
        return repository.findById(id).map(existing -> {
            Integer age = agifyClient.getAge(request.firstName());
            existing.setFirstName(request.firstName());
            existing.setLastName(request.lastName());
            existing.setEmail(request.email());
            existing.setAge(age);
            return repository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
