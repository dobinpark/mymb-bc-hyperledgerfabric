package mymb.mymbbchyperledgerfabric.controller;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.dto.BCUserDTO;
import mymb.mymbbchyperledgerfabric.dto.TokenDTO;
import mymb.mymbbchyperledgerfabric.entity.TransferRequest;
import mymb.mymbbchyperledgerfabric.repository.BCUserRepository;
import mymb.mymbbchyperledgerfabric.repository.TokenRepository;
import mymb.mymbbchyperledgerfabric.repository.UserRepository;
import mymb.mymbbchyperledgerfabric.service.TokenService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final BCUserRepository bcUserRepository;

    // 컨텐츠 판매 티켓 Token 토큰 발행
    @PostMapping("/mintToken")
    public String mintToken(@RequestBody TokenDTO tokenDTO) {
        String categoryCode = tokenDTO.getCategoryCode();
        String pollingResultId = tokenDTO.getPollingResultId();
        String tokenType = tokenDTO.getTokenType();

        return tokenService.mintToken(categoryCode, pollingResultId, tokenType);
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

    // 유저의 포인트 업데이트
    @PutMapping("/updateMymPoint")
    public String updateMymPoint(@RequestBody BCUserDTO request) {
        return tokenService.updateMymPoint(request);
    }
}
