package mymb.mymbbchyperledgerfabric.controller;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.dto.BCUserDTO;
import mymb.mymbbchyperledgerfabric.dto.TokenDTO;
import mymb.mymbbchyperledgerfabric.entity.TransferRequest;
import mymb.mymbbchyperledgerfabric.repository.BCUserRepository;
import mymb.mymbbchyperledgerfabric.repository.TokenRepository;
import mymb.mymbbchyperledgerfabric.repository.UserRepository;
import mymb.mymbbchyperledgerfabric.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final BCUserRepository bcUserRepository;

    // 컨텐츠 판매 티켓 Token 토큰 단일 발행
    @PostMapping("/mintToken")
    public String mintToken(@RequestBody TokenDTO tokenDTO) {
        String categoryCode = tokenDTO.getCategoryCode();
        String pollingResultId = tokenDTO.getPollingResultId();
        String tokenType = tokenDTO.getTokenType();

        return tokenService.mintToken(categoryCode, pollingResultId, tokenType);
    }

    // 컨텐츠 판매 티켓 Token 토큰 13,332장(임시로 30장) 발행
    @PostMapping("/mintTokens")
    public String mintTokens(@RequestBody TokenDTO tokenDTO) {
        String categoryCode = tokenDTO.getCategoryCode();
        String pollingResultId = tokenDTO.getPollingResultId();
        String tokenType = tokenDTO.getTokenType();

        return tokenService.mintTokens(categoryCode, pollingResultId, tokenType);
    }

    // 토큰 ID 조회
    @GetMapping("/token/{tokenNumber}")
    public String getToken(@PathVariable String tokenNumber) {
        return tokenService.getToken(tokenNumber);
    }

    // 토큰 전체 조회
    @GetMapping("/tokens")
    public String getAllTokens() {
        return tokenService.getAllTokens();
    }

    // 토큰 전송
    @PutMapping("/transferTokenExisting")
    public String transferTokenExisting(@RequestBody TransferRequest transferRequest) {
        return tokenService.transferTokenExisting(transferRequest.getFrom(), transferRequest.getTo());
    }

    // 토큰 전송(테스트용)
    @PutMapping("/transferTokenOne")
    public String transferTokenOne(@RequestParam String from, @RequestParam String to, @RequestParam String tokenNumber) {
        return tokenService.transferTokenOne(from, to, tokenNumber);
    }

    // 토큰 전송
    @PutMapping("/transferToken")
    public String transferToken(@RequestBody TransferRequest transferRequest) {
        return tokenService.transferToken(transferRequest.getFrom(), transferRequest.getTo(), transferRequest.getTokenNumbers());
    }

    // 유저의 포인트 업데이트
    @PutMapping("/updateMymPoint")
    public String updateMymPoint(@RequestBody BCUserDTO request) {
        return tokenService.updateMymPoint(request);
    }

    // 해당 유저가 가지고 있는 지정된 토큰들을 삭제
    @DeleteMapping("/deleteAllTokens/{nickName}")
    public ResponseEntity<String> deleteAllTokens(@PathVariable String nickName) {
        String result = tokenService.deleteAllTokens(nickName);
        if (result.contains("not found")) {
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }
}
