package com.example.restarter_backend.repository;

import com.example.restarter_backend.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
* Repository interface for Member entity.
* Provides CRUD operations and custom queries for managing members in the 
database.
*/
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}
