package com.internconnect.service;

import com.internconnect.entity.Company;

import java.util.List;

public interface CompanyService {
    Company getCompanyById(Long id);
    Company getCompanyByEmail(String email);
    List<Company> getAllCompanies();
    List<Company> getCompaniesByStatus(Company.CompanyStatus status);
    Company updateCompanyStatus(Long id, Company.CompanyStatus status);
    void deleteCompany(Long id);
}
