package org.example.securitypractica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;
    @Column(name = "password", nullable = false, length = 200)
    private String password;
    @Column(name = "role", nullable = false)
    private String role;

    public User(String role, String username, String password) {
        this.role = role;
        this.username = username;
        this.password = password;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
