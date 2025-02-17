package mymb.mymbbchyperledgerfabric.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public String mintToken(String owner, String categoryCode, String fundingId, String ticketId,
                            String tokenType, String sellStage, String imageUrl,int ticketCnt) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < ticketCnt; i++) {
            // UUID 생성
            UUID uuid = UUID.randomUUID();

            // tokenNumber 생성
            String tokenNumber = generateTokenNumber(uuid.toString());
            System.out.println("tokenNumber : " + tokenNumber);

            // MongoDB에 데이터 저장
            Token token = Token.builder()
                    .tokenNumber(tokenNumber)
                    .owner(owner)
                    .categoryCode(categoryCode)
                    .fundingId(fundingId)
                    .ticketId(ticketId)
                    .tokenType(tokenType)
                    .sellStage(sellStage)
                    .imageUrl(imageUrl)
                    .tokenCreatedTime(LocalDateTime.now())
                    .build();
            tokenRepository.save(token);

            // AMB에 데이터 저장 요청
            String ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"MintToken\", \"%s\", \"%s\", \"%s\", \"%s\",\"%s\", \"%s\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, tokenNumber, owner, categoryCode, fundingId, ticketId, tokenType, sellStage, imageUrl));

            result.append("AMB ").append(ambResult).append(" MongoDB : Data saved successfully for token ").append(tokenNumber).append("\n");

            try {
                // 3000밀리초 대기
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        return "티켓 민팅이 완료되었습니다.";
    }

    // n개의 티켓을 발행하는 메서드(몽고디비만)
    public String mintTokenMongo(String owner, String categoryCode, String fundingId, String ticketId,
                                 String tokenType, String sellStage, String imageUrl,int ticketCnt) {

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

            // MongoDB에 데이터 저장
            Token token = Token.builder()
                    .tokenNumber(tokenNumber)
                    .owner(owner)
                    .categoryCode(categoryCode)
                    .fundingId(fundingId)
                    .ticketId(ticketId)
                    .tokenType(tokenType)
                    .sellStage(sellStage)
                    .imageUrl(imageUrl)
                    .tokenCreatedTime(LocalDateTime.now())
                    .build();
            tokenRepository.save(token);
        }

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

    // 해당 토큰들을 전송하는 메서드
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

        // 토큰 전송 및 처리
        for (String tokenNumber : tokenNumbers) {
            // 토큰 선택
            Token token = tokens.stream().filter(t -> t.getTokenNumber().equals(tokenNumber)).findFirst().orElse(null);
            if (token == null) {
                continue; // 토큰이 존재하지 않으면 다음 토큰으로 넘어감
            }

            // 토큰의 owner 필드를 'to'로 변경
            token.setOwner(to);

            tokenRepository.save(token);

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

    // Pay 컬렉션의 모든 도큐먼트들을 트랜스퍼하는 메서드
    public String transferOldToken() {

        String from = "(주)밈비";

        // User 컬렉션에서 fromUserNickName을 가진 유저를 찾음
        User fromUser = userRepository.findByNickName(from);
        if (fromUser == null) {
            return from + " 유저를 찾을 수 없습니다.";
        }

        // BCUser 컬렉션에서 fromUserNickName을 가진 유저를 찾음
        BCUser fromBCUser = BCUserRepository.findByNickName(from);
        if (fromBCUser == null) {
            return from + " BCUser를 찾을 수 없습니다.";
        }

        // Pay 컬렉션에서 status "OD"인 도큐먼트들을 조회
        List<Pay> payList = payRepository.findByStatus("OD");
        List<Pay> filteredPayList = new ArrayList<>();
        for (Pay pay : payList) {
            Optional<User> optionalToUser = userRepository.findById(pay.getMemberId());
            if (!optionalToUser.isPresent()) {
                System.out.println(pay.getMemberId() + " 유저를 찾을 수 없습니다.");
                continue;
            }

            User toUser = optionalToUser.get();

            BCUser toBCUser = BCUserRepository.findByNickName(toUser.getNickName());
            if (toBCUser == null) {
                System.out.println(toUser.getNickName() + " BCUser를 찾을 수 없습니다.");
                continue;
            }

            // 각 도큐먼트마다 작업 수행
            int ticketCount = pay.getTicketCount();
            for (int i = 0; i < ticketCount; i++) {
                boolean tokenFound = false;

                // Token 컬렉션에서 owner가 fromUserNickName과 일치하고 ticketId가 일치하는 토큰을 찾음
                List<Token> fromTokens = tokenRepository.findByOwner(from);
                String tokenNumber = null;
                for (Token token : fromTokens) {
                    if (token.getTicketId().equals(pay.getTicketId())) {
                        // 일치하는 토큰을 찾으면 전송 목록에 추가하고 owner를 to로 업데이트
                        tokenNumber = token.getTokenNumber();
                        token.setOwner(toUser.getNickName());
                        tokenRepository.save(token);
                        tokenFound = true;
                        break;
                    }
                }

                if (!tokenFound) {
                    // 토큰 부족 메시지 출력
                    System.out.println(from + "가 가지고 있는 토큰 중에서 일치하는 ticketId의 토큰이 부족합니다.");
                    return "토큰 전송이 실패했습니다.";
                }

                // transfer 활성 체인코드
                String command = String.format("docker exec cli peer chaincode invoke " +
                                "--tls --cafile %s " +
                                "--channelID %s " +
                                "--name %s " +
                                "-c '{\"Args\":[\"TransferToken\", \"%s\", \"%s\", \"%s\"]}'",
                        caFilePath, channelID, chaincodeName, from, toUser.getNickName(), tokenNumber);

                System.out.println("Executing command: " + command);

                executeCommand(command);

                // 3초 대기
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }

            // 변경사항 저장
            BCUserRepository.save(fromBCUser);
            BCUserRepository.save(toBCUser);
        }

        // 모든 작업이 완료되면 메시지 반환
        return "토큰 전송이 완료되었습니다.";
    }

    // 지정된 유저의 Pay 컬렉션 조건에 맞춘 메서드
    public String transferTokens(String from, String to) {

        // User 컬렉션에서 fromUserNickName을 가진 유저를 찾음
        User fromUser = userRepository.findByNickName(from);
        if (fromUser == null) {
            return from + " 유저를 찾을 수 없습니다.";
        }

        // User 컬렉션에서 toUserNickName을 가진 유저를 찾음
        User toUser = userRepository.findByNickName(to);
        if (toUser == null) {
            return to + " 유저를 찾을 수 없습니다.";
        }

        // BCUser 컬렉션에서 fromUserNickName을 가진 유저를 찾음
        BCUser fromBCUser = BCUserRepository.findByNickName(from);
        if (fromBCUser == null) {
            return from + " BCUser를 찾을 수 없습니다.";
        }

        // BCUser 컬렉션에서 toUserNickName을 가진 유저를 찾음
        BCUser toBCUser = BCUserRepository.findByNickName(to);
        if (toBCUser == null) {
            return to + " BCUser를 찾을 수 없습니다.";
        }

        // Pay 컬렉션에서 status "OD"인 도큐먼트들을 조회
        List<Pay> payList = payRepository.findByStatus("OD");
        List<Pay> filteredPayList = new ArrayList<>();
        for (Pay pay : payList) {
            if (toUser.getId().equals(pay.getMemberId())) {
                filteredPayList.add(pay);
            }
        }

        // 각 도큐먼트마다 작업 수행
        for (Pay pay : filteredPayList) {
            // ticketCount 만큼 토큰 전송
            int ticketCount = pay.getTicketCount();
            for (int i = 0; i < ticketCount; i++) {
                boolean tokenFound = false;

                // Token 컬렉션에서 owner가 fromUserNickName과 일치하고 ticketId가 일치하는 토큰을 찾음
                List<Token> fromTokens = tokenRepository.findByOwner(from);
                String tokenNumber = null;
                for (Token token : fromTokens) {
                    if (token.getTicketId().equals(pay.getTicketId())) {
                        // 일치하는 토큰을 찾으면 전송 목록에 추가하고 owner를 to로 업데이트
                        tokenNumber = token.getTokenNumber();
                        token.setOwner(to);
                        tokenRepository.save(token);
                        tokenFound = true;
                        break;
                    }
                }

                if (!tokenFound) {
                    // 토큰 부족 메시지 출력
                    System.out.println(from + "가 가지고 있는 토큰 중에서 일치하는 ticketId의 토큰이 부족합니다.");
                    return "토큰 전송이 실패했습니다.";
                }

                // transfer 활성 체인코드
                String command = String.format("docker exec cli peer chaincode invoke " +
                                "--tls --cafile %s " +
                                "--channelID %s " +
                                "--name %s " +
                                "-c '{\"Args\":[\"TransferToken\", \"%s\", \"%s\", \"%s\"]}'",
                        caFilePath, channelID, chaincodeName, from, to, tokenNumber);

                System.out.println("Executing command: " + command);

                executeCommand(command);

                // 3초 대기
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }

            // 변경사항 저장
            BCUserRepository.save(fromBCUser);
            BCUserRepository.save(toBCUser);
        }

        // 모든 작업이 완료되면 메시지 반환
        return "토큰 전송이 완료되었습니다.";
    }

    // Pay 컬렉션 조건에 맞춰 일괄적으로 트랜스퍼하는 메서드(몽고디비만)
    public String transferTokensMongo() {

        String from = "(주)밈비"; // from 유저를 고정

        // User 컬렉션에서 fromUserNickName을 가진 유저를 찾음
        User fromUser = userRepository.findByNickName(from);
        if (fromUser == null) {
            return from + " 유저를 찾을 수 없습니다.";
        }

        // BCUser 컬렉션에서 fromUserNickName을 가진 유저를 찾음
        BCUser fromBCUser = BCUserRepository.findByNickName(from);
        if (fromBCUser == null) {
            return from + " BCUser를 찾을 수 없습니다.";
        }

        // Pay 컬렉션에서 status "OD"인 도큐먼트들을 조회
        List<Pay> payList = payRepository.findByStatus("OD");

        // 각 도큐먼트마다 작업 수행
        for (Pay pay : payList) {
            String to = pay.getMemberId(); // to 유저를 Pay 컬렉션의 MemberNickName으로 설정

            // User 컬렉션에서 toUserNickName을 가진 유저를 찾음
            User toUser = userRepository.findByNickName(to);
            if (toUser == null) {
                return to + " 유저를 찾을 수 없습니다.";
            }

            // BCUser 컬렉션에서 toUserNickName을 가진 유저를 찾음
            BCUser toBCUser = BCUserRepository.findByNickName(to);
            if (toBCUser == null) {
                return to + " BCUser를 찾을 수 없습니다.";
            }

            // ticketCount 만큼 토큰 전송
            int ticketCount = pay.getTicketCount();
            for (int i = 0; i < ticketCount; i++) {
                boolean tokenFound = false;

                // Token 컬렉션에서 owner가 fromUserNickName과 일치하고 ticketId가 일치하는 토큰을 찾음
                List<Token> fromTokens = tokenRepository.findByOwner(from);
                String tokenNumber = null;
                for (Token token : fromTokens) {
                    if (token.getTicketId().equals(pay.getTicketId())) {
                        // 일치하는 토큰을 찾으면 전송 목록에 추가하고 owner를 to로 업데이트
                        tokenNumber = token.getTokenNumber();
                        token.setOwner(to);
                        tokenRepository.save(token);
                        tokenFound = true;
                        break;
                    }
                }

                if (!tokenFound) {
                    // 토큰 부족 메시지 출력
                    System.out.println(from + "가 가지고 있는 토큰 중에서 일치하는 ticketId의 토큰이 부족합니다.");
                    return "토큰 전송이 실패했습니다.";
                }
            }

            // 변경사항 저장
            BCUserRepository.save(fromBCUser);
            BCUserRepository.save(toBCUser);
        }

        // 모든 작업이 완료되면 메시지 반환
        return "토큰 전송이 완료되었습니다.";
    }

    // 모든 토큰들의 소유주를 "(주)밈비"로 바꾸는 메서드(몽고디비만)
    public String updateTokenOwners() {
        String newOwner = "(주)밈비"; // 새로운 owner 값

        // Token 컬렉션에서 모든 도큐먼트를 조회
        List<Token> tokenList = tokenRepository.findAll();

        if (tokenList.isEmpty()) {
            return "변경할 토큰이 없습니다.";
        }

        // 각 도큐먼트마다 owner 값을 업데이트
        for (Token token : tokenList) {
            token.setOwner(newOwner);
            tokenRepository.save(token);
        }

        // 모든 작업이 완료되면 메시지 반환
        return "모든 토큰의 owner 값이 '(주)밈비'로 변경되었습니다.";
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
            // Token 컬렉션에서 owner가 nickName인 모든 도큐먼트 찾기
            List<Token> ownedTokens = tokenRepository.findByOwner(nickName);

            // 각 토큰 삭제
            for (Token token : ownedTokens) {
                tokenRepository.delete(token);
            }

            // Hyperledger Fabric에서 사용자의 모든 토큰 삭제
            String ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                    "--tls --cafile %s " +
                    "--channelID %s " +
                    "--name %s -c '{\"Args\":[\"DeleteAllTokens\", \"%s\"]}'", caFilePath, channelID, chaincodeName, nickName));

            return "Tokens deleted from MongoDB and AMB for user " + nickName + " with result: " + ambResult;
        } else {
            return "BCUser with nickname " + nickName + " not found in MongoDB";
        }
    }

    // 누락된 유저 ID를 찾는 메서드
    public String findMissingUsers() {

        // Pay 컬렉션에서 status "OD"인 도큐먼트들을 조회
        List<Pay> payList = payRepository.findByStatus("OD");

        // 누락된 유저 ID를 저장할 리스트
        List<String> missingUserIds = new ArrayList<>();

        // 각 도큐먼트마다 작업 수행
        for (Pay pay : payList) {
            // memberId 필드값을 통해 User 컬렉션에서 _id값을 찾음
            Optional<User> userOptional = userRepository.findById(pay.getMemberId());
            if (!userOptional.isPresent()) {
                // User가 존재하지 않으면 missingUserIds 리스트에 추가
                missingUserIds.add(pay.getMemberId());
            }
        }

        // 누락된 유저 ID를 출력
        if (missingUserIds.isEmpty()) {
            return "모든 memberId가 유효합니다.";
        } else {
            String missingIds = String.join(", ", missingUserIds);
            System.out.println("유효하지 않은 memberId: " + missingIds);
            return "유효하지 않은 memberId: " + missingIds;
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