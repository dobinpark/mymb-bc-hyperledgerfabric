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
    public ResponseEntity<?> mintToken(@RequestBody TokenDTO tokenDTO) {
        String owner = tokenDTO.getOwner();
        String categoryCode = tokenDTO.getCategoryCode();
        String fundingId = tokenDTO.getFundingId();
        String ticketId = tokenDTO.getTicketId();
        String tokenType = tokenDTO.getTokenType();
        String sellStage = tokenDTO.getSellStage();
        String imageUrl = tokenDTO.getImageUrl();
        int ticketCnt = tokenDTO.getTicketCnt();

        String result = tokenService.mintToken(
                owner, categoryCode, fundingId, ticketId,
                tokenType, sellStage, imageUrl, ticketCnt);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // n개의 티켓을 발행
    @PostMapping("/mintTokenMongo")
    public ResponseEntity<?> mintTokenMongo(@RequestBody TokenDTO tokenDTO) {
        String owner = tokenDTO.getOwner();
        String categoryCode = tokenDTO.getCategoryCode();
        String fundingId = tokenDTO.getFundingId();
        String ticketId = tokenDTO.getTicketId();
        String tokenType = tokenDTO.getTokenType();
        String sellStage = tokenDTO.getSellStage();
        String imageUrl = tokenDTO.getImageUrl();
        int ticketCnt = tokenDTO.getTicketCnt();

        String result = tokenService.mintTokenMongo(
                owner, categoryCode, fundingId, ticketId,
                tokenType, sellStage, imageUrl, ticketCnt);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 해당 토큰을 조회
    @GetMapping("/getToken/{tokenNumber}")
    public ResponseEntity<?> getToken(@PathVariable String tokenNumber) {
        String result = tokenService.getToken(tokenNumber);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 모든 토큰을 조회
    @GetMapping("/getAllTokens")
    public ResponseEntity<?> getAllTokens() {
        String result = tokenService.getAllTokens();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 누락된 유저 ID를 찾는 메서드
    @GetMapping("/findMissingUsers/missingUsers")
    public ResponseEntity<?> findMissingUsers() {
        String result = tokenService.findMissingUsers();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 지정된 토큰들을 전송
    @PutMapping("/transferToken")
    public ResponseEntity<?> transferToken(@RequestBody TransferRequest transferRequest) {
        tokenService.transferToken(transferRequest.getFrom(), transferRequest.getTo(), transferRequest.getTokenNumbers());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 기존의 Pay 컬렉션에 가지고 있는 모든 도큐먼트들을 전송
    @PutMapping("/transferOldToken")
    public ResponseEntity<?> transferOldToken() {
        String result = tokenService.transferOldToken();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 지정된 유저의 Pay 컬렉션 조건에 맞춘 토큰 전송
    @PutMapping("/transferTokensMongo")
    public ResponseEntity<?> transferTokensMongo() {
        String result = tokenService.transferTokensMongo();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 모든 토큰들의 소유주를 "(주)밈비"로 바꾸는 메서드(몽고디비만)
    @PutMapping("/updateTokenOwners")
    public ResponseEntity<?> updateTokenOwners() {
        String result = tokenService.updateTokenOwners();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 커뮤니티 활동 포인트 적립
    @PutMapping("/updateMymPoint")
    public ResponseEntity<?> updateMymPoint(@RequestBody BCUserDTO request) {
        tokenService.updateMymPoint(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 해당 유저가 가지고 있는 지정된 토큰들을 삭제
    @DeleteMapping("/deleteAllTokens/{nickName}")
    public ResponseEntity<?> deleteAllTokens(@PathVariable String nickName) {
        String result = tokenService.deleteAllTokens(nickName);
        if (result.contains("not found")) {
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }
}