package com.example.restarter_backend.controller;

import com.example.restarter_backend.entity.Member;
import com.example.restarter_backend.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = auth.getName();

    Optional<Member> member = memberService.getMemberById(id);
    if (member.isPresent()) {
        // Allow if librarian or if the member's username matches the authenticated user
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN")) ||
            member.get().getUsername().equals(currentUsername)) {
            return ResponseEntity.ok(member.get());
        } else {
            return ResponseEntity.status(403).build();
        }
    }
    return ResponseEntity.notFound().build();
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
public ResponseEntity<Member> updateMember(@PathVariable Long id, @RequestBody Member memberDetails) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = auth.getName();
    boolean isLibrarian = auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));

    Optional<Member> member = memberService.getMemberById(id);
    if (member.isPresent()) {
        if (isLibrarian || member.get().getUsername().equals(currentUsername)) {
            Member updated = memberService.updateMember(id, memberDetails, currentUsername, isLibrarian);
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.status(403).build();
        }
    }
    return ResponseEntity.notFound().build();
}
/**
* DELETE endpoint to delete a member by their ID.
* @param id The ID of the member to delete
* @return ResponseEntity with no content if successful
*/
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = auth.getName();
    boolean isLibrarian = auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));

    Optional<Member> member = memberService.getMemberById(id);
    if (member.isPresent()) {
        if (isLibrarian || member.get().getUsername().equals(currentUsername)) {
            memberService.deleteMember(id, currentUsername, isLibrarian);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }
    return ResponseEntity.notFound().build();
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