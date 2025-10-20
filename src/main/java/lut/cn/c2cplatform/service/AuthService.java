package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.payload.RegisterRequest;
import lut.cn.c2cplatform.entity.User;

public interface AuthService {
    User registerUser(RegisterRequest registerRequest);
}

