package com.eefood.iamservice.model;
import com.eefood.iamservice.enums.Gender;
import com.eefood.iamservice.enums.Provider;
import com.eefood.iamservice.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String authId;// id keycloak

  @Column(nullable = false)
  private String username;

  @Column(nullable = false, length = 255, unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  private LocalDate dob;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Gender gender;

  @Column(columnDefinition = "jsonb")
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Provider provider;

  private String avatarUrl;

  @ElementCollection
  @CollectionTable(name = "user_allergies", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "allergy")
  private List<String> allergies;

  @ElementCollection
  @CollectionTable(name = "user_eating_preferences", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "preference")
  private List<String> eatingPreferences;
}
