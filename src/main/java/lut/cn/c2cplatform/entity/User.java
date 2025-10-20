package lut.cn.c2cplatform.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

@Data
public class User implements Serializable {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<Role> roles;
}
