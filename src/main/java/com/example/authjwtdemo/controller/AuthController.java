package com.example.authjwtdemo.controller;

import com.example.authjwtdemo.data.User;
import com.example.authjwtdemo.service.UserService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private UserService userService;



   /* @GetMapping("/hello")
    public String hello(){
        return "Hello!";
    }
*/
    record RegisterRequest(@JsonProperty("first_name")String firstName,
                           @JsonProperty("last_name")String lastName,
                           String email,String password,
                            @JsonProperty("confirm_password")String confirmPassword){}

    record RegisterResponse(Long id,@JsonProperty("first_name")String firstName,
                            @JsonProperty("last_name")String lastName,
                            String email){}
    //curl -X POST -H "Content-Type: application/json" -d '{"firstnName":"John","lastName":"Doe","email":"john@gmail.com","password":"12345"}' localhost:8000/api/register
    //curl -X POST -H "Content-Type: application/json" -d '{"first_name":"Thomas","last_name":"Hardey","email":"thomas@gmail.com","password":"12345"}' localhost:8000/api/register
    //curl -X POST -H "Content-Type: application/json" -d '{"first_name":"Thomas","last_name":"Hardey","email":"thomas@gmail.com","password":"12345","confirm_password":"1234"}' localhost:8000/api/register
    // curl -X POST -H "Content-Type: application/json" -d '{"first_name":"Angle","last_name":"Chan","email":"chan@gmail.com","password":"12345","confirm_password":"12345"}' localhost:8000/api/register
    @PostMapping("/register")
    public UserResponse register(@RequestBody RegisterRequest registerRequest){

            User user = userService.register(
                    registerRequest.firstName,
                    registerRequest.lastName,
                    registerRequest.email,
                    registerRequest.password,
                    registerRequest.confirmPassword);

            return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());

    }
    record LoginRequest(String email,String password){}
   /* record LoginResponse(Long id, @JsonProperty("first_name")String firstName,
                         @JsonProperty("last_name")String lastName,
                         String email){}*/

    record LoginResponse(String token){}
    //curl -X POST -H "Content-Type: application/json" -d '{"email":"chan@gmail.com","password":"12345"}' localhost:8000/api/login
    //http://localhost:8000/api/login
    @PostMapping("/login")
    public LoginResponse loginResponse(@RequestBody LoginRequest loginRequest, HttpServletResponse response){
       /* var user=userService.login(loginRequest.email(),loginRequest.password());
        return new LoginResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());*/
        var login=userService.login(loginRequest.email(), loginRequest.password());
        Cookie cookie=new Cookie("refresh_token",login.getRefreshToken().getToken());
        cookie.setMaxAge(3600);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        response.addCookie(cookie);
        return new LoginResponse(
               login.getAccessToken().getToken()
        );
    }
    record UserResponse(Long id, @JsonProperty("first_name")String firstName,
                        @JsonProperty("last_name")String lastName,
                        String email){}
    @GetMapping("/user")
    public UserResponse userResponse(HttpServletRequest request){
        var user=(User) request.getAttribute("user");
        return new UserResponse(user.getId(),user.getFirstName(),user.getLastName(),user.getEmail());
    }

    record RefreshResponse(String token){}
    @PostMapping("/refresh")
    public RefreshResponse refreshResponse(@CookieValue("refresh_token")String refreshToken){
        return new RefreshResponse(userService.refreshAccess(refreshToken).getAccessToken().getToken());
    }
    record LogoutResponse(String msg){}
    @PostMapping("/logout")
    public LogoutResponse logoutResponse(@CookieValue("refresh_token")String refreshToken,
                                         HttpServletResponse response){
        Cookie cookie=new Cookie("refresh_token",null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return new  LogoutResponse("successful logout!");
    }
}
