package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Member;
import java.util.List;
import java.util.Optional;

/**
* Service interface for managing member-related operations.
* Defines methods for CRUD operations and business logic for the Member 
entity.
*/
public interface MemberService {
/**
* Retrieves all members from the database.
* Only librarians should be able to use this.
*/
List<Member> getAllMembers();
/**
* Retrieves a member by their ID.
* Only librarians or the member themselves should be able to use this.
*/
Optional<Member> getMemberById(Long id);
/**
* Adds a new member to the database.
*/
Member addMember(Member member);
/**
* Updates an existing member in the database.
* Only librarians or the member themselves should be able to use this.
*/
Member updateMember(Long id, Member memberDetails, String currentUsername, boolean isLibrarian);
/**
* Deletes a member from the database by their ID.
* Only librarians or the member themselves should be able to use this.
*/
void deleteMember(Long id, String currentUsername, boolean isLibrarian);
/**
* Searches for members by name.
* Only librarians should be able to use this.
*/
List<Member> searchMembersByName(String name);
}