package com.iscm.iam.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity{
    
    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String domain;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_org_id")
    private Organization parentOrganization;

    @Column(length = 50)
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();
}