package mymb.mymbbchyperledgerfabric.controller;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.dto.BCUserDTO;
import mymb.mymbbchyperledgerfabric.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 유저 데이터 생성
    @PostMapping("/createSigninUserBlock")
    public String createSigninUserBlock(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();
        List<String> ownedToken = BCUserDTO.getOwnedToken();

        return userService.createSigninUserBlock(nickName, mymPoint, ownedToken);
    }

    // 기존의 유저 데이터 생성
    @PostMapping("/createLoginUserBlock")
    public String createLoginUserBlock(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();
        List<String> ownedToken = BCUserDTO.getOwnedToken();

        return userService.createLoginUserBlock(nickName, mymPoint, ownedToken);
    }

    // 유저 닉네임 조회
    @GetMapping("/user/{nickName}")
    public String getUser(@PathVariable String nickName) {
        return userService.getUser(nickName);
    }

    // 유저 전체 조회
    @GetMapping("/users")
    public String getAllUsers() {
        return userService.getAllUsers();
    }
}
