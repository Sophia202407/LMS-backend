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
    private MemberRepository memberRepository;

    @Override
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Override
    public Optional<Member> getMemberById(Long id) {
        return memberRepository.findById(id);
    }

    @Override
    public Member addMember(Member member) {
        if (member.getRegistrationDate() == null) {
            member.setRegistrationDate(LocalDate.now());
        }
        member.setMembershipExpiryDate(member.getRegistrationDate().plusYears(1));
        return memberRepository.save(member);
    }

    @Override
    public Member updateMember(Long id, Member memberDetails, String currentUsername, boolean isLibrarian) {
        Member existingMember = memberRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));

        // Security check: only librarian or the member themselves
        if (!isLibrarian && !existingMember.getUsername().equals(currentUsername)) {
            throw new SecurityException("Access denied: not allowed to update this member.");
        }

        existingMember.setName(memberDetails.getName());
        existingMember.setAddress(memberDetails.getAddress());
        existingMember.setContactInfo(memberDetails.getContactInfo());

        if (memberDetails.getRegistrationDate() != null) {
            existingMember.setRegistrationDate(memberDetails.getRegistrationDate());
            existingMember.setMembershipExpiryDate(memberDetails.getRegistrationDate().plusYears(1));
        }

        return memberRepository.save(existingMember);
    }

    @Override
    public void deleteMember(Long id, String currentUsername, boolean isLibrarian) {
        Member existingMember = memberRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));

        // Security check: only librarian or the member themselves
        if (!isLibrarian && !existingMember.getUsername().equals(currentUsername)) {
            throw new SecurityException("Access denied: not allowed to delete this member.");
        }

        memberRepository.deleteById(id);
    }

    @Override
    public List<Member> searchMembersByName(String name) {
        return memberRepository.findAll().stream()
            .filter(member -> member.getName().toLowerCase().contains(name.toLowerCase()))
            .toList();
    }
}