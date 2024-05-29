package mymb.mymbbchyperledgerfabric.controller;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.dto.BCUserDTO;
import mymb.mymbbchyperledgerfabric.dto.TokenDTO;
import mymb.mymbbchyperledgerfabric.entity.TransferRequest;
import mymb.mymbbchyperledgerfabric.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    // 컨텐츠 판매 티켓 Token 토큰 단일 발행
    @PostMapping("/mintToken")
    public String mintToken(@RequestBody TokenDTO tokenDTO) {
        String categoryCode = tokenDTO.getCategoryCode();
        String pollingResultId = tokenDTO.getPollingResultId();
        String tokenType = tokenDTO.getTokenType();
        int ticketCnt = 0;

        return tokenService.mintToken(categoryCode, pollingResultId, tokenType, ticketCnt);
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
    @PutMapping("/transferToken")
    public String transferToken(@RequestBody TransferRequest transferRequest) {
        return tokenService.transferToken(transferRequest.getFrom(), transferRequest.getTo(), transferRequest.getTokenNumbers());
    }

    // 기존의 Pay 컬렉션에 가지고 있는 토큰 전체 전송
    @PutMapping("/transferOldToken")
    public String transferTokenExisting(@RequestBody TransferRequest transferRequest) {
        return tokenService.transferOldToken(transferRequest.getFrom(), transferRequest.getTo());
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
