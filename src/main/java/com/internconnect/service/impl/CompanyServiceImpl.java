package com.internconnect.service.impl;

import com.internconnect.entity.Company;
import com.internconnect.exception.ResourceNotFoundException;
import com.internconnect.repository.CompanyRepository;
import com.internconnect.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Company getCompanyByEmail(String email) {
        return companyRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> getCompaniesByStatus(Company.CompanyStatus status) {
        return companyRepository.findByStatus(status);
    }

    @Override
    public Company updateCompanyStatus(Long id, Company.CompanyStatus status) {
        Company company = getCompanyById(id);
        company.setStatus(status);
        return companyRepository.save(company);
    }

    @Override
    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Company", "id", id);
        }
        companyRepository.deleteById(id);
    }
}
