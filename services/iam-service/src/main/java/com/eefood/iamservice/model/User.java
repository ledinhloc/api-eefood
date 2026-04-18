package com.eefood.iamservice.model;

import com.eefood.iamservice.enums.ActivityLevel;
import com.eefood.iamservice.enums.Gender;
import com.eefood.iamservice.enums.Provider;
import com.eefood.iamservice.enums.Role;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String authId; // id keycloak

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

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private ActivityLevel activityLevel;

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON) // Hibernate 6 hỗ trợ
  private JsonNode address;

  //  @Column(columnDefinition = "jsonb")
  //  private String address;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Provider provider;

  private String avatarUrl;

  private String backgroundUrl;

  @ElementCollection
  @CollectionTable(name = "user_allergies", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "allergy")
  private List<String> allergies;

  @ElementCollection
  @CollectionTable(name = "user_eating_preferences", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "preference")
  private List<String> eatingPreferences;

  @ElementCollection
  @CollectionTable(name = "user_dietary_preferences", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "dietary")
  private List<String> dietaryPreferences;

  @ElementCollection
  @CollectionTable(name = "user_health_conditions", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "condition_name")
  private List<String> healthConditions;
}
