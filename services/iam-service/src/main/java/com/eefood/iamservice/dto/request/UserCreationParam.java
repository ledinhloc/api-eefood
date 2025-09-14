package com.eefood.iamservice.dto.request;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationParam {
  String username;
  boolean enabled;
  String email;
  boolean emailVerified;
  String firstName;
  String lastName;
  List<Credential> credentials;
}
