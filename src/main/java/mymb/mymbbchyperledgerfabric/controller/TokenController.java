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

    // n개의 티켓을 발행
    @PostMapping("/mintToken")
    public String mintToken(@RequestBody TokenDTO tokenDTO) {
        String categoryCode = tokenDTO.getCategoryCode();
        String pollingResultId = tokenDTO.getPollingResultId();
        String fundingId = tokenDTO.getFundingId();
        String ticketId = tokenDTO.getTicketId();
        String tokenType = tokenDTO.getTokenType();
        String sellStage = tokenDTO.getSellStage();
        int ticketCnt = 0;

        return tokenService.mintToken(categoryCode, pollingResultId, fundingId, ticketId, tokenType, sellStage, ticketCnt);
    }

    // 해당 토큰을 조회
    @GetMapping("/token/{tokenNumber}")
    public String getToken(@PathVariable String tokenNumber) {
        return tokenService.getToken(tokenNumber);
    }

    // 모든 토큰을 조회
    @GetMapping("/tokens")
    public String getAllTokens() {
        return tokenService.getAllTokens();
    }

    // 지전됭 토큰들을 전송
    @PutMapping("/transferToken")
    public String transferToken(@RequestBody TransferRequest transferRequest) {
        return tokenService.transferToken(transferRequest.getFrom(), transferRequest.getTo(), transferRequest.getTokenNumbers());
    }

    // 기존의 Pay 컬렉션에 가지고 있는 모든 도큐먼트들을 전송
    @PutMapping("/transferOldToken")
    public String transferOldToken(@RequestBody TransferRequest transferRequest) {
        return tokenService.transferOldToken(transferRequest.getFrom(), transferRequest.getTo());
    }

    // 커뮤니티 활동 포인트 적립
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
