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

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final BCUserRepository BCUserRepository;
    private final PayRepository payRepository;
    private final UserRepository userRepository;
    private final FundingRepository fundingRepository;
    private final PollingResultRepository pollingResultRepository;

    String caFilePath = "/opt/home/managedblockchain-tls-chain.pem";
    String channelID = "mychannel";
    String chaincodeName = "mycc";

    // 단일 티켓을 발행하는 메서드
    public String mintToken(String categoryCode, String pollingResultId, String tokenType) {

        // sellStage 초기화
        String sellStage = "";

        // UUID 생성
        UUID uuid = UUID.randomUUID();

        // tokenNumber 생성
        String tokenNumber = generateTokenNumber(uuid.toString());
        System.out.println("tokenNumber : " + tokenNumber);

        // MongoDB에 TokenNumber가 이미 존재하는지 확인
        Token existingToken = tokenRepository.findByTokenNumber(tokenNumber);
        if (existingToken != null) {
            // 이미 존재하는 경우에는 아무 동작도 수행하지 않고 종료
            return "Token with tokenId " + tokenNumber + " already exists in MongoDB";
        }

        // AMB에 TokenNumber가 이미 존재하는지 확인
        String ambResult = getToken(tokenNumber);
        if (!ambResult.isEmpty()) {
            // 이미 존재하는 경우에는 아무 동작도 수행하지 않고 종료
            return "Token with tokenId " + tokenNumber + " already exists in AMB";
        }

        // BCUser 컬렉션의 ownedToken 필드에 토큰 추가
        BCUser BCUser = BCUserRepository.findByNickName("(주)밈비"); // 닉네임을 "(주)밈비"로 지정
        if (BCUser != null) {
            BCUser.getOwnedToken().add(tokenNumber);
            BCUserRepository.save(BCUser); // 변경된 사용자 정보 저장
        } else {
            return "사용자를 찾을 수 없습니다.";
        }

        // MongoDB에 데이터 저장
        Token token = Token.builder()
                .tokenNumber(tokenNumber)
                .categoryCode(categoryCode)
                .pollingResultId(pollingResultId)
                .tokenType(tokenType)
                .sellStage(sellStage)
                .tokenCreatedTime(LocalDateTime.now())
                .build();
        tokenRepository.save(token);

        // AMB에 데이터 저장 요청
        ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                        "--tls --cafile %s " +
                        "--channelID %s " +
                        "--name %s -c '{\"Args\":[\"MintToken\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\"]}'",
                caFilePath, channelID, chaincodeName, tokenNumber, categoryCode, pollingResultId, tokenType, sellStage));

        return "AMB " + ambResult + " MongoDB : Data saved successfully";
    }

    // 13,332장(임시로 30장) 티켓을 발행하는 메서드
    public String mintTokens(String categoryCode, String pollingResultId, String tokenType) {

        // sellStage 초기화
        String sellStage = "";

        // BCUser 컬렉션의 ownedToken 필드에 토큰 추가
        BCUser BCUser = BCUserRepository.findByNickName("(주)밈비"); // 닉네임을 "(주)밈비"로 지정
        if (BCUser == null) {
            return "사용자를 찾을 수 없습니다.";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 30; i++) {
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
                    .tokenType(tokenType)
                    .sellStage(sellStage)
                    .tokenCreatedTime(LocalDateTime.now())
                    .build();
            tokenRepository.save(token);

            // AMB에 데이터 저장 요청
            ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"MintToken\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, tokenNumber, categoryCode, pollingResultId, tokenType, sellStage));

            result.append("AMB ").append(ambResult).append(" MongoDB : Data saved successfully for token ").append(tokenNumber).append("\n");

            try {
                // 1밀리초 대기
                Thread.sleep(1);
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

    // 지정된 토큰들을 전송하는 메서드
    public String transferTokens(String from, String to, ArrayList<String> tokenNumbers) {

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

        // 보내는 사용자(from)가 소유한 토큰들을 가져오기
        List<Token> tokens = tokenRepository.findByTokenNumberIn(tokenNumbers);

        // 보내는 사용자가 소유한 모든 토큰들을 확인하고 전송
        for (Token token : tokens) {
            // 토큰이 fromBCUser에게 있는지 확인
            if (!fromBCUser.getOwnedToken().contains(token.getTokenNumber())) {
                return "해당 토큰을 소유한 사용자가 아닙니다.";
            }
        }

        // 받는 사용자(to)에게 토큰 전송
        for (String tokenNumber : tokenNumbers) {
            // 토큰의 소유자 변경
            fromBCUser.getOwnedToken().remove(tokenNumber);
            toBCUser.getOwnedToken().add(tokenNumber);
        }

        // BCUser 컬렉션 변경사항 저장
        BCUserRepository.save(fromBCUser);
        BCUserRepository.save(toBCUser);

        // Transfer 활성 체인코드 실행
        for (String tokenNumber : tokenNumbers) {
            executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"TransferTokens\", \"%s\", \"%s\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, fromBCUser.getNickName(), toBCUser.getNickName(), tokenNumber));
        }

        return "토큰 전송이 완료되었습니다.";
    }

    // 지정된 토큰들을 전송하는 메서드
    public String transferToken(String from, String to, ArrayList<String> tokenNumbers) {

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

        // Pay 컬렉션에서 memberId와 User 컬렉션의 Id값이 동일한지 확인
        Pay toPayUser = payRepository.findByMemberId(toUser.getId());

        // fromBCUser와 fromUser의 값이 동일하고, toBCUser와 toUser의 값이 동일한지 확인
        if (fromBCUser.getNickName().equals(fromUser.getNickName()) && toBCUser.getNickName().equals(toUser.getNickName())) {

            // Pay 컬렉션의 memberId값과 User 컬렉션의 Id값이 동일한지 확인
            if (toPayUser.getMemberId().equals(toUser.getId())) {

                // toPayUser의 status와 ticketAmount를 확인하여 sellStage 결정
                PayStatusEnum status = toPayUser.getStatus();
                int ticketAmount = toPayUser.getTicketAmount();

                String sellStage = "";

                if ((status == PayStatusEnum.OD || status == PayStatusEnum.RD) && ticketAmount == 50000) {
                    sellStage = "private";
                } else if ((status == PayStatusEnum.OD || status == PayStatusEnum.RD) && ticketAmount == 65000){
                    sellStage = "public";
                }

                // 보내는 사용자(from)가 소유한 토큰들 가져오기
                List<Token> tokens = tokenRepository.findByTokenNumberIn(tokenNumbers);

                // 받는 사용자(to)에게 토큰 전송
                for (Token token : tokens) {
                    fromBCUser.getOwnedToken().remove(token.getTokenNumber());
                    toBCUser.getOwnedToken().add(token.getTokenNumber());

                    // 토큰의 sellStage 값을 업데이트
                    token.setSellStage(sellStage);
                    tokenRepository.save(token);
                }

                // 변경사항 저장
                BCUserRepository.save(fromBCUser);
                BCUserRepository.save(toBCUser);

                // sellStage 값 변경 체인코드
                executeCommand(String.format("docker exec cli peer chaincode invoke " +
                                "--tls --cafile %s " +
                                "--channelID %s " +
                                "--name %s -c '{\"Args\":[\"UpdateSellStage\", \"%s\", \"%s\"]}'",
                        caFilePath, channelID, chaincodeName, tokens, sellStage));

                // transfer 활성 체인코드
                executeCommand(String.format("docker exec cli peer chaincode invoke " +
                                "--tls --cafile %s " +
                                "--channelID %s " +
                                "--name %s -c '{\"Args\":[\"TransferToken\", \"%s\", \"%s\", \"%s\"]}'",
                        caFilePath, channelID, chaincodeName, fromBCUser, toBCUser, tokens));

                return "토큰 전송이 완료되었습니다.";
            }
        }
        return "MongoDB Token Transfer Success";
    }

    // 기존의 Pay 컬렉션에 가지고 있는 모든 도큐먼트들을 전송하는 메서드
    public String transferTokenExisting(String from, String to) {
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

        // toUser의 모든 Pay 도큐먼트 찾기
        List<Pay> toPayUsers = payRepository.findAllByMemberId(toUser.getId());

        // fromBCUser와 fromUser의 값이 동일하고, toBCUser와 toUser의 값이 동일한지 확인
        if (fromBCUser.getNickName().equals(fromUser.getNickName()) && toBCUser.getNickName().equals(toUser.getNickName())) {

            // fromBCUser가 소유한 모든 토큰 가져오기
            List<String> fromOwnedTokens = new ArrayList<>(fromBCUser.getOwnedToken());

            // 조건에 해당되는 모든 도큐먼트들에 대해 반복
            for (Pay toPayUser : toPayUsers) {

                String fundingId = toPayUser.getFundingId();

                // 첫 번째 조건(MZ스님 박건우)
                String fundingId1 = "646f64f3b4c1f55a6bc43aa4";
                String pollingResultId1 = "64a4cccb448261263d7fb860";
                boolean match1 = fundingId.equals(fundingId1) && fromOwnedTokens.stream().anyMatch(tokenNumber -> {
                    Token token = tokenRepository.findByTokenNumber(tokenNumber);
                    return token != null && token.getPollingResultId().equals(pollingResultId1);
                });

                // 두 번째 조건(신도 직업입니다)
                String fundingId2 = "646f648db4c1f55a6bc43aa3";
                String pollingResultId2 = "6499554e9c5a271ed39476b2";
                boolean match2 = fundingId.equals(fundingId2) && fromOwnedTokens.stream().anyMatch(tokenNumber -> {
                    Token token = tokenRepository.findByTokenNumber(tokenNumber);
                    return token != null && token.getPollingResultId().equals(pollingResultId2);
                });

                // 세 번째 조건(당신, 보고 있구나)
                String fundingId3 = "646f653eb4c1f55a6bc43aa5";
                String pollingResultId3 = "64a4cceb448261263d7fb861";
                boolean match3 = fundingId.equals(fundingId3) && fromOwnedTokens.stream().anyMatch(tokenNumber -> {
                    Token token = tokenRepository.findByTokenNumber(tokenNumber);
                    return token != null && token.getPollingResultId().equals(pollingResultId3);
                });

                // 조건을 만족하는 경우에만 전송
                if (match1 || match2 || match3) {
                    // toPayUser의 status와 ticketAmount를 확인하여 sellStage 결정
                    PayStatusEnum status = toPayUser.getStatus();
                    int ticketAmount = toPayUser.getTicketAmount();

                    String sellStage = "";

                    if ((status == PayStatusEnum.OD || status == PayStatusEnum.RD) && ticketAmount == 50000) {
                        sellStage = "private";
                    } else if ((status == PayStatusEnum.OD || status == PayStatusEnum.RD) && ticketAmount == 65000) {
                        sellStage = "public";
                    }

                    // 조건에 맞는 경우에만 실행
                    if (!sellStage.isEmpty()) {
                        int ticketCount = toPayUser.getTicketCount(); // 도큐먼트에서 ticketCount 가져오기

                        // Pay 컬렉션의 fundingId에 맞는 Token 필터링
                        List<String> tokensToTransfer = new ArrayList<>();
                        for (String tokenNumber : fromOwnedTokens) {
                            Token token = tokenRepository.findByTokenNumber(tokenNumber);
                            if (token != null && (token.getPollingResultId().equals(pollingResultId1) ||
                                    token.getPollingResultId().equals(pollingResultId2) ||
                                    token.getPollingResultId().equals(pollingResultId3))) {
                                tokensToTransfer.add(tokenNumber);
                                if (tokensToTransfer.size() == ticketCount) break; // 필요한 만큼만 수집
                            }
                        }

                        // ticketCount 수만큼 토큰 전송
                        for (String tokenNumber : tokensToTransfer) {
                            fromBCUser.getOwnedToken().remove(tokenNumber);
                            toBCUser.getOwnedToken().add(tokenNumber);

                            // 토큰의 sellStage 값을 업데이트
                            Token token = tokenRepository.findByTokenNumber(tokenNumber);
                            if (token != null) {
                                token.setSellStage(sellStage);
                                tokenRepository.save(token);
                            }
                        }

                        // 변경사항 저장
                        BCUserRepository.save(fromBCUser);
                        BCUserRepository.save(toBCUser);

                        // sellStage 값 변경 체인코드
                        executeCommand(String.format("docker exec cli peer chaincode invoke " +
                                        "--tls --cafile %s " +
                                        "--channelID %s " +
                                        "--name %s -c '{\"Args\":[\"UpdateSellStage\", \"%s\", \"%s\"]}'",
                                caFilePath, channelID, chaincodeName, tokensToTransfer, sellStage));

                        // transfer 활성 체인코드
                        executeCommand(String.format("docker exec cli peer chaincode invoke " +
                                        "--tls --cafile %s " +
                                        "--channelID %s " +
                                        "--name %s -c '{\"Args\":[\"TransferToken\", \"%s\", \"%s\", \"%s\"]}'",
                                caFilePath, channelID, chaincodeName, fromBCUser.getNickName(), toBCUser.getNickName(), tokensToTransfer));
                    }
                }
            }
            return "토큰 전송이 완료되었습니다.";
        }
        return "MongoDB Token Transfer Success";
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

    // 해당 유저가 가지고 있는 지정된 토큰들을 삭제하는 메서드
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
