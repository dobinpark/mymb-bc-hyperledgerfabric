package mymb.mymbbchyperledgerfabric.service;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.dto.BCUserDTO;
import mymb.mymbbchyperledgerfabric.entity.*;
import mymb.mymbbchyperledgerfabric.repository.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final BCUserRepository BCUserRepository;
    private final PayRepository payRepository;
    private final UserRepository userRepository;
    private final FundingRepository fundingRepository;
    private final PollingResultRepository pollingResultRepository;
    private final TicketRepository ticketRepository;

    String caFilePath = "/opt/home/managedblockchain-tls-chain.pem";
    String channelID = "mychannel";
    String chaincodeName = "mycc";

    // n개의 티켓을 발행하는 메서드
    public String mintToken(String categoryCode, String pollingResultId, String fundingId, String ticketId, String tokenType, String sellStage, int ticketCnt) {

        // imageUrl 초기화
        String imageUrl = "";

        // BCUser 컬렉션의 ownedToken 필드에 토큰 추가
        BCUser BCUser = BCUserRepository.findByNickName("(주)밈비"); // 닉네임을 "(주)밈비"로 지정
        if (BCUser == null) {
            return "사용자를 찾을 수 없습니다.";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < ticketCnt; i++) {
            // UUID 생성
            UUID uuid = UUID.randomUUID();

            // tokenNumber 생성
            String tokenNumber = generateTokenNumber(uuid.toString());
            System.out.println("tokenNumber : " + tokenNumber);

            // MongoDB에 TokenNumber가 이미 존재하는지 확인
            Token existingToken = tokenRepository.findByTokenNumber(tokenNumber);
            if (existingToken != null) {
                // 이미 존재하는 경우에는 아무 동작도 수행하지 않고 다음 토큰 생성
                result.append("Token with tokenId ").append(tokenNumber).append(" already exists in MongoDB\n");
                continue;
            }

            // AMB에 TokenNumber가 이미 존재하는지 확인
            String ambResult = getToken(tokenNumber);
            if (!ambResult.isEmpty()) {
                // 이미 존재하는 경우에는 아무 동작도 수행하지 않고 다음 토큰 생성
                result.append("Token with tokenId ").append(tokenNumber).append(" already exists in AMB\n");
                continue;
            }

            // BCUser 컬렉션에 토큰 추가
            BCUser.getOwnedToken().add(tokenNumber);

            // MongoDB에 데이터 저장
            Token token = Token.builder()
                    .tokenNumber(tokenNumber)
                    .categoryCode(categoryCode)
                    .pollingResultId(pollingResultId)
                    .fundingId(fundingId)
                    .tokenType(tokenType)
                    .ticketId(ticketId)
                    .sellStage(sellStage)
                    .imageUrl(imageUrl)
                    .tokenCreatedTime(LocalDateTime.now())
                    .build();
            tokenRepository.save(token);

            // AMB에 데이터 저장 요청
            ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"MintToken\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, tokenNumber, categoryCode, pollingResultId, fundingId, ticketId, tokenType, sellStage, imageUrl));

            result.append("AMB ").append(ambResult).append(" MongoDB : Data saved successfully for token ").append(tokenNumber).append("\n");

            try {
                // 100밀리초 대기
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        // BCUser 저장 (한 번만 저장)
        BCUserRepository.save(BCUser);

        return result.toString();
    }

    // 해당 토큰을 조회하는 메서드
    public String getToken(String tokenNumber) {

        return executeCommand(String.format("docker exec cli peer chaincode query " +
                "--tls --cafile %s " +
                "--channelID %s " +
                "--name %s -c '{\"Args\":[\"GetToken\", \"%s\"]}'", caFilePath, channelID, chaincodeName, tokenNumber));
    }

    // 모든 토큰을 조회하는 메서드
    public String getAllTokens() {

        return executeCommand(String.format("docker exec cli peer chaincode query " +
                "--tls --cafile %s " +
                "--channelID %s " +
                "--name %s -c '{\"Args\":[\"GetAllTokens\"]}'", caFilePath, channelID, chaincodeName));
    }

/*    // 지정된 토큰들을 전송하는 메서드
    public String transferToken(String from, String to, List<String> tokenNumbers) {

        // User 컬렉션에 닉네임을 이용하여 사용자 찾기
        User fromUser = userRepository.findByNickName(from);
        User toUser = userRepository.findByNickName(to);

        if (fromUser == null && toUser != null) {
            return "해당 from 유저는 User 컬렉션에 존재하지 않습니다.";
        } else if (fromUser != null && toUser == null) {
            return "해당 to 유저는 User 컬렉션에 존재하지 않습니다.";
        } else if (fromUser == null && toUser == null) {
            return "해당 from 유저와 to 유저는 User 컬렉션에 존재하지 않습니다.";
        }

        // BCUser 컬렉션에 닉네임을 이용하여 사용자 찾기
        BCUser fromBCUser = BCUserRepository.findByNickName(from);
        BCUser toBCUser = BCUserRepository.findByNickName(to);

        if (fromBCUser == null && toBCUser != null) {
            return "해당 from 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        } else if (fromBCUser != null && toBCUser == null) {
            return "해당 to 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        } else if (fromBCUser == null && toBCUser == null) {
            return "해당 from 유저와 to 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        }

        // 전송할 토큰 리스트 가져오기
        List<Token> tokens = tokenRepository.findByTokenNumberIn(tokenNumbers);
        if (tokens.size() != tokenNumbers.size()) {
            return "일부 토큰이 존재하지 않습니다.";
        }

        // 지정된 토큰이 fromBCUser 소유인지 확인
        for (String tokenNumber : tokenNumbers) {
            if (!fromBCUser.getOwnedToken().contains(tokenNumber)) {
                return "from 유저는 토큰 " + tokenNumber + "를 소유하지 않습니다.";
            }
        }

        // 토큰 전송 및 처리
        for (String tokenNumber : tokenNumbers) {
            // 토큰 선택
            Token token = tokens.stream().filter(t -> t.getTokenNumber().equals(tokenNumber)).findFirst().orElse(null);
            if (token == null) {
                continue; // 토큰이 존재하지 않으면 다음 토큰으로 넘어감
            }

            // fromBCUser의 토큰 제거
            fromBCUser.getOwnedToken().remove(tokenNumber);

            // toBCUser에게 토큰 추가
            toBCUser.getOwnedToken().add(tokenNumber);

            // 요거는 sellStage 조건이 정해지면
            // 토큰의 sellStage 값 업데이트 (필요한 경우)
            tokenRepository.save(token);

            // sellStage 값 변경 체인코드
            executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"UpdateSellStage\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, tokenNumber, token.getSellStage()));

            // transfer 활성 체인코드
            executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"TransferToken\", \"%s\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, fromBCUser.getNickName(), toBCUser.getNickName(), tokenNumber));

            try {
                // 3000밀리초 대기
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        // 변경사항 저장
        BCUserRepository.save(fromBCUser);
        BCUserRepository.save(toBCUser);

        return "토큰 전송이 완료되었습니다.";
    }*/

    // 지정된 토큰들을 전송하는 메서드
    public String transferToken(String from, String to) {

        // User 컬렉션에 닉네임을 이용하여 사용자 찾기
        User fromUser = userRepository.findByNickName(from);
        User toUser = userRepository.findByNickName(to);

        if (fromUser == null && toUser != null) {
            return "해당 from 유저는 User 컬렉션에 존재하지 않습니다.";
        } else if (fromUser != null && toUser == null) {
            return "해당 to 유저는 User 컬렉션에 존재하지 않습니다.";
        } else if (fromUser == null && toUser == null) {
            return "해당 from 유저와 to 유저는 User 컬렉션에 존재하지 않습니다.";
        }

        // BCUser 컬렉션에 닉네임을 이용하여 사용자 찾기
        BCUser fromBCUser = BCUserRepository.findByNickName(from);
        BCUser toBCUser = BCUserRepository.findByNickName(to);

        if (fromBCUser == null && toBCUser != null) {
            return "해당 from 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        } else if (fromBCUser != null && toBCUser == null) {
            return "해당 to 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        } else if (fromBCUser == null && toBCUser == null) {
            return "해당 from 유저와 to 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        }

        // toUser의 UID값과 Pay 컬렉션의 memberId가 일치하고 status 필드값이 "OD"인 경우
        List<Pay> toPayUser = payRepository.findByMemberIdAndStatus(toUser.getId(), "OD");

        String sellStage = "";

        String[] privateTickets = {"64a53b8c4b1a285e22c361a9", "64a6553574906c3e62735399", "649bf39e4569af66164107ae"};
        String[] publicTickets = {"65dd909afb4fd168ca1c3ec9", "65dd909afb4fd168ca1c3eca", "65dd909afb4fd168ca1c3ecb"};
        String[] premiumTickets = {"665d7a14a3b473551423430b", "665d7acba3b473551423430c", "665d7afea3b473551423430d"};
        String[] notForSaleTickets = {"665d7be1a3b473551423430e", "665d7c0ca3b473551423430f", "665d7c3aa3b4735514234310"};

        // n개의 도큐먼트마다 순서대로 토큰 전송
        for (Pay pay : toPayUser) {

            // 2.1 ticketCount만큼 토큰 전송
            int ticketCount = pay.getTicketCount();
            List<String> transferTokens = new ArrayList<>();
            for (int i = 0; i < ticketCount; i++) {
                if (!fromBCUser.getOwnedToken().isEmpty()) {
                    String token = fromBCUser.getOwnedToken().remove(0);
                    transferTokens.add(token);
                } else {
                    // 2.1.1 토큰 부족 메시지 출력
                    System.out.println("fromBCUser가 가지고 있는 토큰이 부족합니다.");
                    return "토큰 전송이 실패했습니다.";
                }
            }

            // 2.2 ticketId와 sellStage 값에 따라 토큰 전송
            String ticketId = pay.getTicketId();
            if (Arrays.asList(privateTickets).contains(ticketId)) {
                sellStage = "private";
            } else if (Arrays.asList(publicTickets).contains(ticketId)) {
                sellStage = "public";
            } else if (Arrays.asList(premiumTickets).contains(ticketId)) {
                sellStage = "premium";
            } else if (Arrays.asList(notForSaleTickets).contains(ticketId)) {
                sellStage = "notForSale";
            } else {
                // 2.2.1 예상치 못한 ticketId 메시지 출력
                System.out.println("Unexpected ticket ID: " + ticketId);
                return "토큰 전송이 실패했습니다.";
            }

            // 2.3 transfer 활성 체인코드
            executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"TransferToken\", \"%s\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, fromBCUser.getNickName(), toBCUser.getNickName(), transferTokens));

            // 2.4 3초 대기
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

            // 2.5 변경사항 저장
            BCUserRepository.save(fromBCUser);
            BCUserRepository.save(toBCUser);
        }

        // 3. 성공 메시지 출력
        return "토큰 전송이 완료되었습니다.";
    }

/*    // 기존의 Pay 컬렉션에 가지고 있는 모든 도큐먼트들을 전송하는 메서드
    public String transferOldToken(String from, String to) {

        // User 컬렉션에 닉네임을 이용하여 사용자 찾기
        User fromUser = userRepository.findByNickName(from);
        User toUser = userRepository.findByNickName(to);

        if (fromUser == null && toUser != null) {
            return "해당 from 유저는 User 컬렉션에 존재하지 않습니다.";
        } else if (fromUser != null && toUser == null) {
            return "해당 to 유저는 User 컬렉션에 존재하지 않습니다.";
        } else if (fromUser == null && toUser == null) {
            return "해당 from 유저와 to 유저는 User 컬렉션에 존재하지 않습니다.";
        }

        // BCUser 컬렉션에 닉네임을 이용하여 사용자 찾기
        BCUser fromBCUser = BCUserRepository.findByNickName(from);
        BCUser toBCUser = BCUserRepository.findByNickName(to);

        if (fromBCUser == null && toBCUser != null) {
            return "해당 from 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        } else if (fromBCUser != null && toBCUser == null) {
            return "해당 to 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        } else if (fromBCUser == null && toBCUser == null) {
            return "해당 from 유저와 to 유저는 BCUser 컬렉션에 존재하지 않습니다.";
        }


        1. toUser 변수에 해당되는 User 컬렉션의 _id 필드값과 Pay 컬렉션의 memberId 필드값이
        일치하는 도큐먼트들을 조회.
        2. 조회된 도큐먼트들중에 status 필드값이 "OD"인 값이 있는 도큐먼트들을 다시 조회.
        3. n개의 도큐먼트가 조회되는데 여기에서 for문으로 돌린다.
        4. 첫 번째 도큐먼트에서 ticketCount 필드와 ticketId 필드가 존재하는데 ticketCount 필드는
        to 유저가 토큰을 몇 개 구매했는지, ticketId 필드는 Ticket 컬렉션에 있는 도큐먼트마다 uid 필드값이 있고
        ticketId 값과 동일한 도큐먼트를 보면 sellStage 필드값이 존재한다.(private, public 등등)
        5. fromBCUser가 가지고 있는 토큰들마다 ticketId와 sellStage 값들이 존재하는데 이 토큰과
        Pay 컬렉션의 ticketId와 그 값과 동일 Ticket 컬렉션의 sellStage 값이 fromBCUser가 소유한 토큰과 서로 일치한다면
        fromBCUser는 toBCUser에게 해당 토큰을 전송한다. 전송할 때 Pay 컬렉션의 첫 번째 도큐먼트의 ticketCount 값만큼
        토큰을 전송한다.
        6. 첫 번째 도큐먼트의 토큰이 전송을 다했다면 두 번째 도큐먼트도 마찬가지로 ticketCount, ticketId에 맞춰
        fromBCUser가 toBCUser에게 토큰을 전송한다.
        7. 모든 도큐먼트의 토큰들을 다 전송했다면 "토큰 전송이 완료되었습니다." 라고 리턴한다.


        // toUser의 UID값과 Pay 컬렉션의 memberId가 일치하고 status 필드값이 "OD"인 경우
        List<Pay> toPayUser = payRepository.findByMemberIdAndStatus(toUser.getId(), "OD");

        // 조건에 맞춘 도큐먼트들에 대한 반복문
        for (Pay pay : toPayUser) {
            // Ticket 컬렉션에서 ticketId와 동일한 UID값을 찾기
            Ticket ticket = ticketRepository.findByTicketId(pay.getTicketId());
            if (ticket == null) {
                return "해당 ticketId가 Ticket 컬렉션에 존재하지 않습니다.";
            }

            String ticketId = ticket.getTicketId();
            String sellStage = ticket.getSellStage();

            // tokenNumbers 리스트를 기반으로 Token 객체 리스트 가져오기
            List<Token> fromUserTokens = fromBCUser.getOwnedToken().stream()
                    .map(tokenRepository::findByTokenNumber)
                    .collect(Collectors.toList());

            // ticketCount만큼 토큰 전송
            int ticketCount = pay.getTicketCount();
            List<Token> transferTokens = new ArrayList<>();

            for (int i = 0; i < ticketCount; i++) {
                // fromUserTokens 리스트에서 ticketId와 sellStage가 일치하는 토큰을 찾기
                Optional<Token> tokenOptional = fromUserTokens.stream()
                        .filter(token -> {
                            System.out.println("Checking token : " + token);
                            return token.getTicketId().equals(ticketId) && token.getSellStage().equals(sellStage);
                        })
                        .findFirst();

                if (tokenOptional.isPresent()) {
                    Token token = tokenOptional.get();
                    fromBCUser.getOwnedToken().remove(token.getTokenNumber());
                    transferTokens.add(token);
                } else {
                    System.out.println("fromBCUser가 가지고 있는 토큰이 부족하거나 일치하는 토큰이 없습니다.");
                    return "토큰 전송이 실패했습니다.";
                }
            }

            // transfer 활성 체인코드
            String tokenNumbers = transferTokens.stream()
                    .map(Token::getTokenNumber) // 토큰 번호만 전송
                    .collect(Collectors.joining(","));

            String command = String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"TransferToken\", \"%s\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, fromBCUser.getNickName(), toBCUser.getNickName(), tokenNumbers);

            executeCommand(command);

            // 3초 대기
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

            // 변경사항 저장
            BCUserRepository.save(fromBCUser);
            BCUserRepository.save(toBCUser);
        }

        // 성공 메시지 출력
        return "토큰 전송이 완료되었습니다.";
    }*/

    public String transferOldToken() {

        // Pay 컬렉션에서 status가 "OD"인 도큐먼트들을 조회
        List<Pay> payList = payRepository.findByStatus("OD");

        // 각 도큐먼트마다 작업 수행
        for (Pay pay : payList) {
            // memberId 필드값을 통해 User 컬렉션에서 _id값을 찾고 닉네임을 저장
            User user = userRepository.findById(pay.getMemberId()).orElse(null);
            if (user == null) continue;

            String userNickName = user.getNickName();

            // BCUser 컬렉션에서 닉네임이 "(주)밈비"인 유저를 찾음
            BCUser fromBCUser = BCUserRepository.findByNickName("(주)밈비");
            if (fromBCUser == null) continue; // BCUser를 찾을 수 없으면 다음 도큐먼트로 넘어감

            // BCUser 컬렉션에서 해당 사용자의 닉네임을 가진 유저를 찾음
            BCUser toBCUser = BCUserRepository.findByNickName(userNickName);
            if (toBCUser == null) {
                continue; // toBCUser를 찾을 수 없으면 다음 도큐먼트로 넘어감
            }

            // ticketCount만큼 토큰 전송
            int ticketCount = pay.getTicketCount();
            List<String> transferTokens = new ArrayList<>();
            for (int i = 0; i < ticketCount; i++) {
                if (!fromBCUser.getOwnedToken().isEmpty()) {
                    String token = fromBCUser.getOwnedToken().remove(0);
                    transferTokens.add(token);
                } else {
                    // 토큰 부족 메세지 출력
                    System.out.println("(주)밈비가 가지고 있는 토큰이 부족합니다.");
                    return "토큰 전송이 실패했습니다.";
                }
            }

            // transfer 활성 체인코드
            executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"TransferToken\", \"%s\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, fromBCUser.getNickName(), toBCUser.getNickName(), transferTokens));

            // 2.4 3초 대기
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

            // 2.5 변경사항 저장
            BCUserRepository.save(fromBCUser);
            BCUserRepository.save(toBCUser);
        }

        // 모든 작업이 완료되면 메시지 반환
        return "토큰 전송이 완료되었습니다.";
    }

    // 커뮤니티 활동 포인트 적립하는 메서드
    public String updateMymPoint(BCUserDTO request) {
        String nickName = request.getNickName();
        int delta = request.getDelta();

        // MongoDB에서 사용자 찾기
        BCUser BCUser = BCUserRepository.findByNickName(nickName);
        if (BCUser != null) {
            // 사용자 데이터 업데이트
            BCUser.setMymPoint(BCUser.getMymPoint() + delta);
            BCUserRepository.save(BCUser);

            // 체인코드 호출
            executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"UpdateMymPoint\", \"%s\", \"%d\"]}'",
                    caFilePath, channelID, chaincodeName, nickName, delta));

            return "BCUser data updated successfully";
        } else {
            return "BCUser with nickname " + nickName + " not found in MongoDB";
        }
    }

    // 해당 유저가 가지고 있는 토큰들을 삭제하는 메서드
    public String deleteAllTokens(String nickName) {

        // MongoDB에서 사용자 찾기
        BCUser BCUser = BCUserRepository.findByNickName(nickName);
        if (BCUser != null) {
            // 소유한 토큰 리스트 가져오기
            List<String> ownedTokens = new ArrayList<>(BCUser.getOwnedToken());

            // 토큰 리스트 비우기
            BCUser.getOwnedToken().clear();
            BCUserRepository.save(BCUser);

            StringBuilder result = new StringBuilder();
            // 각 토큰 삭제
            for (String tokenNumber : ownedTokens) {
                // MongoDB에서 토큰 삭제
                Token token = tokenRepository.findByTokenNumber(tokenNumber);
                if (token != null) {
                    tokenRepository.delete(token);
                }
            }

            // Hyperledger Fabric에서 사용자의 모든 토큰 삭제
            String ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                    "--tls --cafile %s " +
                    "--channelID %s " +
                    "--name %s -c '{\"Args\":[\"DeleteAllTokens\", \"%s\"]}'", caFilePath, channelID, chaincodeName, nickName));
            result.append("Tokens deleted from AMB for user ").append(nickName).append(" with result: ").append(ambResult).append("\n");

            return result.toString();
        } else {
            return "BCUser with nickname " + nickName + " not found in MongoDB";
        }
    }

    // 리눅스 터미널 사용 메서드
    private String executeCommand(String command) {

        StringBuilder output = new StringBuilder();

        try {
            // ProcessBuilder를 사용하여 외부 명령어를 실행합니다.
            Process process = new ProcessBuilder("sh", "-c", command).start();

            // 실행 결과를 읽어오기 위한 BufferedReader를 생성합니다.
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 실행 결과를 읽어와 StringBuilder에 추가합니다.
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 프로세스가 종료될 때까지 기다립니다.
            process.waitFor();

            // 리소스를 해제합니다.
            reader.close();
            process.destroy();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    // 토큰 생성시 SHA-256 이용 메서드
    private static String generateTokenNumber(String input) {
        try {
            // SHA-256 해시 함수 사용
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());

            // 해시값을 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "0x1" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}