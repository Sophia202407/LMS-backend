package com.example.restarter_backend.controller;

import com.example.restarter_backend.entity.Member;
import com.example.restarter_backend.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
/**
* REST controller for managing member-related operations.
* Exposes endpoints for CRUD operations and searching members.
*/
@RestController
@RequestMapping("/api/members")
public class MemberController {
@Autowired
private MemberService memberService; // Injects the MemberService to handle business logic
/**
* GET endpoint to retrieve all members.
* @return List of all members
*/
@GetMapping
public List<Member> getAllMembers() {
return memberService.getAllMembers();
}
/**
* GET endpoint to retrieve a member by their ID.
* @param id The ID of the member to retrieve
* @return ResponseEntity containing the member if found, or 404 if not 
found
*/
@GetMapping("/{id}")
public ResponseEntity<Member> getMemberById(@PathVariable Long id) {
Optional<Member> member = memberService.getMemberById(id);
return member.map(ResponseEntity::ok)
.orElseGet(() -> ResponseEntity.notFound().build());
}
/**
* POST endpoint to add a new member.
* @param member The member to add
* @return The added member
*/
@PostMapping
public Member addMember(@RequestBody Member member) {
return memberService.addMember(member);
}
/**
* PUT endpoint to update an existing member.
* @param id The ID of the member to update
* @param memberDetails The updated member details
* @return The updated member
*/
@PutMapping("/{id}")
public Member updateMember(@PathVariable Long id, @RequestBody Member
memberDetails) {
return memberService.updateMember(id, memberDetails);
}
/**
* DELETE endpoint to delete a member by their ID.
* @param id The ID of the member to delete
* @return ResponseEntity with no content if successful
*/
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
memberService.deleteMember(id);
return ResponseEntity.noContent().build();
}
/**
* GET endpoint to search members by name.
* @param name The name to search for (partial match)
* @return List of members matching the name
*/
@GetMapping("/search")
public List<Member> searchMembersByName(@RequestParam String name) {
return memberService.searchMembersByName(name);
}
}