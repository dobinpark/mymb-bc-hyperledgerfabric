package mymb.mymbbchyperledgerfabric.controller;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.dto.BCUserDTO;
import mymb.mymbbchyperledgerfabric.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 유저 데이터 생성
    @PostMapping("/createSigninUserBlock")
    public ResponseEntity<?> createSigninUserBlock(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();

        String result = userService.createSigninUserBlock(nickName, mymPoint);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 기존의 유저 데이터 생성
    @PostMapping("/createLoginUserBlock")
    public ResponseEntity<?> createLoginUserBlock(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();

        String result = userService.createLoginUserBlock(nickName, mymPoint);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 기존의 유저 데이터 생성
    @PostMapping("/createdUsers")
    public ResponseEntity<?> createdUsers() {
        String result = userService.createUsersTest();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 기존의 유저 데이터 생성(몽고디비만)
    @PostMapping("/createdUserMongo")
    public ResponseEntity<?> createdUserMongo() {
        String result = userService.createdUserMongo();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 유저 닉네임 조회
    @GetMapping("/getUser/{nickName}")
    public ResponseEntity<?> getUser(@PathVariable String nickName) {
        String result = userService.getUser(nickName);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 유저 전체 조회
    @GetMapping("/getAllUsers")
    public ResponseEntity<?> getAllUsers() {
        String result = userService.getAllUsers();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 중복되지 않는 닉네임 조회
    @GetMapping("/getUserNickName")
    public ResponseEntity<?> getUserNickName() {
        String result = userService.findMismatchedNickNames().toString();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 중복되는 닉네임 조회
    @GetMapping("/findDuplicatedBCUsers")
    public ResponseEntity<?> findDuplicatedBCUsers() {
        String result = userService.findDuplicatedBCUsers().toString();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/data/{memberId}")
    public ResponseEntity<Map<String, Object>> getUserData(@PathVariable String memberId) {
        try {
            Map<String, Object> result = userService.getUserWebtoonData(memberId);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(Collections.singletonMap("error", e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    // 지정된 유저 블록 삭제
    @DeleteMapping("/deleteUser/{nickName}")
    public ResponseEntity<?> deleteUser(@PathVariable String nickName) {
        String result = userService.deleteUser(nickName);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 모든 유저 블록 삭제
    @DeleteMapping("/deleteAllUserBlocks")
    public ResponseEntity<?> deleteAllUserBlocks() {
        String result = userService.deleteAllUserBlocks();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // n개의 유저 정보 생성
    @PostMapping("/createMultipleUsers")
    public ResponseEntity<?> createMultipleUsers(@RequestBody BCUserDTO BCUserDTO) {
        String nickName = BCUserDTO.getNickName();
        int mymPoint = BCUserDTO.getMymPoint();
        int count = BCUserDTO.getCount();

        String result = userService.createMultipleUsersTest(nickName, mymPoint, count);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
