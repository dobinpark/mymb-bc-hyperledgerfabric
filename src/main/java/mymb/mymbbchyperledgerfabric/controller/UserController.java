package mymb.mymbbchyperledgerfabric.controller;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.dto.BCUserDTO;
import mymb.mymbbchyperledgerfabric.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 유저 데이터 생성
    @PostMapping("/createUserBlock")
    public String createUserBlock(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();
        ArrayList<String> ownedToken = BCUserDTO.getOwnedToken();

        return userService.createUserBlock(nickName, mymPoint, ownedToken);
    }

    // 기존의 유저 데이터 생성
    @PostMapping("/createUserBlockExisting")
    public String createUserBlockExisting(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();
        ArrayList<String> ownedToken = BCUserDTO.getOwnedToken();

        return userService.createUserBlockExisting(nickName, mymPoint, ownedToken);
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
