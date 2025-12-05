package com.eefood.iamservice.service;

import com.eefood.iamservice.enums.Provider;
import com.eefood.iamservice.enums.Role;
import com.eefood.iamservice.model.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecification {
  public static Specification<User> searchByUsernameOrEmail(String search) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(search)) {
        return cb.conjunction(); // Không filter nếu search rỗng
      }

      String pattern = "%" + search.toLowerCase() + "%";
      return cb.or(
        cb.like(cb.lower(root.get("username")), pattern),
        cb.like(cb.lower(root.get("email")), pattern)
      );
    };
  }

  public static Specification<User> filterByRole(Role role) {
    if (role == null) return null;
    return (root, query, cb) -> cb.equal(root.get("role"), role);
  }

  public static Specification<User> filterByProvider(Provider provider) {
    if (provider == null) return null;
    return (root, query, cb) -> cb.equal(root.get("provider"), provider);
  }

  public static Specification<User> notDeleted() {
    return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
  }
}