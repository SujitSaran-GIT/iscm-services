package com.iscm.iam.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String code;
    
    private String description;
    
    @ManyToMany(mappedBy = "permissions")
    private List<Role> roles = new ArrayList<>();
}