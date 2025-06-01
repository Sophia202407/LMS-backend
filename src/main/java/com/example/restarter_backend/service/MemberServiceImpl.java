package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Member;
import com.example.restarter_backend.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
/**
* Implementation of the MemberService interface.
* Contains the business logic for managing members using the 
MemberRepository.
*/
@Service
public class MemberServiceImpl implements MemberService {
@Autowired
private MemberRepository memberRepository; // Injects the MemberRepository to perform database operations
/**
* Retrieves all members from the database.
* @return List of all members
*/
@Override
public List<Member> getAllMembers() {
return memberRepository.findAll();
}
/**
* Retrieves a member by their ID.
* @param id The ID of the member to find
* @return Optional containing the member if found, empty otherwise
*/
@Override
public Optional<Member> getMemberById(Long id) {
return memberRepository.findById(id);
}
/**
* Adds a new member to the database.
* Sets membership expiry date to 1 year from registration date.
* @param member The member to add
* @return The saved member
*/
@Override
public Member addMember(Member member) {
// Set registration date to today if not provided
if (member.getRegistrationDate() == null) {
member.setRegistrationDate(LocalDate.now());
}
// Set membership expiry date to 1 year from registration date
member.setMembershipExpiryDate(member.getRegistrationDate().plusYears(1));
return memberRepository.save(member);
}
/**
* Updates an existing member in the database.
* @param id The ID of the member to update
* @param memberDetails The updated member details
* @return The updated member
*/
@Override
public Member updateMember(Long id, Member memberDetails) {
    // Find the existing member by ID
    Member existingMember = memberRepository.findById(id)
    .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));

// Update the fields with the new details
existingMember.setName(memberDetails.getName());
existingMember.setAddress(memberDetails.getAddress());
existingMember.setContactInfo(memberDetails.getContactInfo());

// Do not update registrationDate or membershipExpiryDate to maintain consistency
if (memberDetails.getRegistrationDate() != null) {
existingMember.setRegistrationDate(memberDetails.getRegistrationDate());
existingMember.setMembershipExpiryDate(memberDetails.getRegistrationDate().plusYears(1));
}

// Save the updated member back to the database
return memberRepository.save(existingMember);
}
/**
* Deletes a member from the database by their ID.
* @param id The ID of the member to delete
*/
@Override
public void deleteMember(Long id) {
// Check if the member exists before deleting
if (!memberRepository.existsById(id)) {
throw new RuntimeException("Member not found with id: " + id);
}
memberRepository.deleteById(id);
}
/**
* Searches for members by name (case-insensitive partial match).
* @param name The name to search for (partial match)
* @return List of members matching the name
*/
@Override
public List<Member> searchMembersByName(String name) {
// This is a placeholder. For a real implementation, you would need a custom query method in MemberRepository.
// For now, we'll simulate a simple filter (case-insensitive).
return memberRepository.findAll().stream()
        .filter(member ->
member.getName().toLowerCase().contains(name.toLowerCase()))
        .toList();
}
}