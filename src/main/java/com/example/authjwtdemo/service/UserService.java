package com.example.authjwtdemo.service;

import com.example.authjwtdemo.dao.UserDao;
import com.example.authjwtdemo.data.Token;
import com.example.authjwtdemo.data.User;
import com.example.authjwtdemo.error.EmailAlreadyExistError;
import com.example.authjwtdemo.error.InvalidCredentialError;
import com.example.authjwtdemo.error.PasswordDoNotMatchError;
import com.example.authjwtdemo.error.UserNotFoundError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class UserService {
    @Autowired
    private UserDao userDao;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String accessSecretKey;
    private final String refreshSecretKey;

    public UserService(@Value("${application.security.access-token-secret}") String accessSecretKey,
                       @Value("${application.security.refresh-token-secret}") String refreshSecretKey) {
        this.accessSecretKey = accessSecretKey;
        this.refreshSecretKey = refreshSecretKey;
    }

    public User register(String firstName, String lastName, String email, String password, String confirmPassword){
        if(!Objects.equals(password,confirmPassword)) {
            throw new PasswordDoNotMatchError();
        }
        User user=null;
        try {
            user=userDao.save(User.of(firstName,lastName,email,passwordEncoder.encode(password)));
        }catch (DbActionExecutionException e){
            throw new EmailAlreadyExistError();
        }
        return user;
    }
    //2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b
    public Login login(String email, String password) {
        var user=userDao.findUserByEmail(email)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalid email"));
        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new InvalidCredentialError();
        }
        var login =Login.of(user.getId(),accessSecretKey,refreshSecretKey);
        var refreshJwt=login.getRefreshToken();
        user.addToken(new Token(
                refreshJwt.getToken(),
                refreshJwt.getIssuedAt(),
                refreshJwt.getExpiredAt()
        ));
        return Login.of(
                user.getId(), accessSecretKey,refreshSecretKey
        );
    }

    public User getUserFormToken(String token) {
        return userDao.findById(Jwt.from(token,accessSecretKey).getUserId()).orElseThrow(UserNotFoundError::new);
    }

    public Login refreshAccess(String refreshToken) {
        var refreshJwt= Jwt.from(refreshToken,refreshSecretKey);
        return Login.of(refreshJwt.getUserId(),accessSecretKey, refreshToken);
    }
}
