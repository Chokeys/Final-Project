package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.entity.ClassInquiry;

@Repository
public interface ClassInquiryRepository extends JpaRepository<ClassInquiry, Long> {

    // 1. 문의 목록 조회 by classcode + page
    List<ClassInquiry> findByClassproduct_classcodeOrderByNoDesc(long classcode);

    // 2. 문의 목록 총 개수 조회 by classcode
    long countByClassproduct_classcode(long classcode);

   
}
