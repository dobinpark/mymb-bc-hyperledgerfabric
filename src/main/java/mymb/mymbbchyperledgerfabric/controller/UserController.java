package mymb.mymbbchyperledgerfabric.controller;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.dto.BCUserDTO;
import mymb.mymbbchyperledgerfabric.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 유저 데이터 생성
    @PostMapping("/createSigninUserBlock")
    public ResponseEntity<?> createSigninUserBlock(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();
        List<String> ownedToken = BCUserDTO.getOwnedToken();

        String result = userService.createSigninUserBlock(nickName, mymPoint, ownedToken);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 기존의 유저 데이터 생성
    @PostMapping("/createLoginUserBlock")
    public ResponseEntity<?> createLoginUserBlock(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();
        List<String> ownedToken = BCUserDTO.getOwnedToken();

        String result = userService.createLoginUserBlock(nickName, mymPoint, ownedToken);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 기존의 유저 데이터 생성
    @PostMapping("/createdUsers")
    public ResponseEntity<?> createdUsers() {
        String result = userService.createdUsers();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 유저 닉네임 조회
    @GetMapping("/user/{nickName}")
    public ResponseEntity<?> getUser(@PathVariable String nickName) {
        String result = userService.getUser(nickName);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 유저 전체 조회
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        String result = userService.getAllUsers();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
