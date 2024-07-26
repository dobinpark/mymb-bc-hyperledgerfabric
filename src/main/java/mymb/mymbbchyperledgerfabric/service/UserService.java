package mymb.mymbbchyperledgerfabric.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.entity.BCUser;
import mymb.mymbbchyperledgerfabric.entity.Funding;
import mymb.mymbbchyperledgerfabric.entity.Pay;
import mymb.mymbbchyperledgerfabric.entity.User;
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
public class UserService {

    private final BCUserRepository BCUserRepository;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final FundingRepository fundingRepository;
    private final TicketRepository ticketRepository;
    private final PayRepository payRepository;

    String caFilePath = "/opt/home/managedblockchain-tls-chain.pem";
    String channelID = "mychannel";
    String chaincodeName = "mycc";

    // 유저 정보 블록을 생성하는 메서드(회원가입시)
    public String createSigninUserBlock(String nickName, int mymPoint) {

        // MongoDB에 NickName이 이미 존재하는지 확인
        BCUser existingBCUser = BCUserRepository.findByNickName(nickName);
        if (existingBCUser != null) {
            // 이미 존재하는 경우에는 아무 동작도 수행하지 않고 종료
            return "BCUser with nickname " + nickName + " already exists in MongoDB";
        }

        // AMB에 NickName이 이미 존재하는지 확인
        String ambResult = getUser(nickName);
        if (!ambResult.isEmpty()) {
            // 이미 존재하는 경우에는 아무 동작도 수행하지 않고 종료
            return "BCUser with nickname " + nickName + " already exists in AMB";
        }

        // UUID 생성
        UUID uuid = UUID.randomUUID();

        // userNumber 생성
        String userNumber = generateUserId(uuid.toString());
        System.out.println("userNumber " + userNumber);

        // ownedToken 지역 변수 생성
        List<String> ownedToken = new ArrayList<>();

        // AMB에 데이터 저장 요청
        ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                        "--tls --cafile %s " +
                        "--channelID %s " +
                        "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%s\", \"%d\", \"%s\"]}'",
                caFilePath, channelID, chaincodeName, userNumber, nickName, mymPoint, ownedToken));

        // MongoDB에 데이터 저장 (ownedToken 필드 제외)
        BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                .userNumber(userNumber)
                .nickName(nickName)
                .mymPoint(mymPoint)
                .blockCreatedTime(LocalDateTime.now())
                .build();
        BCUserRepository.save(BCUser);

        return "AMB " + ambResult + " MongoDB : Data saved successfully";
    }

    // 유저 정보 블록을 생성하는 메서드(기존에 가입되어 있는 유저가 있을시)
    public String createLoginUserBlock(String nickName, int mymPoint) {
        // MongoDB에 NickName이 이미 존재하는지 확인
        BCUser existingBCUser = BCUserRepository.findByNickName(nickName);

        // AMB에 NickName이 이미 존재하는지 확인
        String ambResult = getUser(nickName);

        // UUID 생성
        UUID uuid = UUID.randomUUID();

        // userNumber 생성
        String userNumber = generateUserId(uuid.toString());
        System.out.println("userNumber " + userNumber);

        // ownedToken 지역 변수 생성
        List<String> ownedToken = new ArrayList<>();

        // MongoDB와 AMB에 모두 데이터가 있는 경우
        if (existingBCUser != null && !ambResult.isEmpty()) {
            return "BCUser with nickname " + nickName + " already exists in both MongoDB and AMB";
        }
        // MongoDB에만 데이터가 있는 경우
        else if (existingBCUser != null && ambResult.isEmpty()) {
            // AMB에 데이터 저장 요청
            ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%s\", \"%d\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, userNumber, nickName, mymPoint, ownedToken));

            return "BCUser with nickname " + nickName + " already exists in MongoDB";
        }
        // AMB에만 데이터가 있는 경우
        else if (existingBCUser == null && !ambResult.isEmpty()) {
            // MongoDB에 데이터 저장 (ownedToken 필드 제외)
            BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                    .userNumber(userNumber)
                    .nickName(nickName)
                    .mymPoint(mymPoint)
                    .blockCreatedTime(LocalDateTime.now())
                    .build();
            BCUserRepository.save(BCUser);

            return "AMB with nickname " + nickName + " already exists in AMB";
        }
        // 둘 다 데이터가 없는 경우
        else {
            // AMB에 데이터 저장 요청
            ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%s\", \"%d\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, userNumber, nickName, mymPoint, ownedToken));

            // MongoDB에 데이터 저장 (ownedToken 필드 제외)
            BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                    .userNumber(userNumber)
                    .nickName(nickName)
                    .mymPoint(mymPoint)
                    .blockCreatedTime(LocalDateTime.now())
                    .build();
            BCUserRepository.save(BCUser);

            return "AMB " + ambResult + " MongoDB : Data saved successfully";
        }
    }

    // 일괄적으로 모든 유저 블록 생성하는 메서드
    public String createdUsers() {
        List<User> users = userRepository.findAll();
        List<String> results = new ArrayList<>();

        for (User user : users) {
            String nickName = user.getNickName();
            int mymPoint = 0;
            List<String> ownedToken = new ArrayList<>();

            // UUID 생성
            UUID uuid = UUID.randomUUID();

            // userNumber 생성
            String userNumber = generateUserId(uuid.toString());
            System.out.println("userNumber " + userNumber);

            // MongoDB에 NickName이 이미 존재하는지 확인
            BCUser existingBCUser = BCUserRepository.findByNickName(nickName);

            // AMB에 NickName이 이미 존재하는지 확인
            String ambResult = getUser(nickName);

            // 둘 다 데이터가 없는 경우
            if (existingBCUser == null && ambResult.isEmpty()) {
                // AMB와 MongoDB에 동시에 데이터 저장 요청
                try {
                    ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                                    "--tls --cafile %s " +
                                    "--channelID %s " +
                                    "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%s\", %d, \"%s\"]}'",
                            caFilePath, channelID, chaincodeName, userNumber, nickName, mymPoint, ownedToken));
                } catch (Exception e) {
                    e.printStackTrace();
                    results.add("닉네임 " + nickName + "의 AMB 데이터 생성 중 오류가 발생했습니다: " + e.getMessage());
                    continue;
                }

                // 3초 대기
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }

                // MongoDB에 데이터 저장 (ownedToken 필드 제외)
                BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                        .userNumber(userNumber)
                        .nickName(nickName)
                        .mymPoint(mymPoint)
                        .blockCreatedTime(LocalDateTime.now())
                        .build();
                BCUserRepository.save(BCUser);

                if (ambResult.contains("success")) {
                    results.add("AMB " + ambResult + " MongoDB : 데이터가 성공적으로 저장되었습니다.");
                } else {
                    results.add("닉네임 " + nickName + "의 AMB 데이터 생성에 실패했습니다: " + ambResult);
                }
            }

            // MongoDB에만 데이터가 있는 경우
            else if (existingBCUser != null && ambResult.isEmpty()) {
                // AMB에 데이터 저장 요청
                try {
                    String ownedTokenJson = new ObjectMapper().writeValueAsString(ownedToken);
                    ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                                    "--tls --cafile %s " +
                                    "--channelID %s " +
                                    "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%s\", %d, %s]}'",
                            caFilePath, channelID, chaincodeName, userNumber, nickName, mymPoint, ownedTokenJson));
                } catch (Exception e) {
                    e.printStackTrace();
                    results.add("닉네임 " + nickName + "의 AMB 데이터 생성 중 오류가 발생했습니다: " + e.getMessage());
                    continue;
                }

                // 3초 대기
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }

                if (ambResult.contains("success")) {
                    results.add("닉네임 " + nickName + "을(를) 가진 BCUser가 MongoDB에 이미 존재합니다.");
                } else {
                    results.add("닉네임 " + nickName + "을(를) 가진 BCUser의 AMB 데이터 생성에 실패했습니다: " + ambResult);
                }
            }

            // AMB에만 데이터가 있는 경우
            else if (existingBCUser == null && !ambResult.isEmpty()) {
                // MongoDB에 데이터 저장 (ownedToken 필드 제외)
                BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                        .userNumber(userNumber)
                        .nickName(nickName)
                        .mymPoint(mymPoint)
                        .blockCreatedTime(LocalDateTime.now())
                        .build();
                BCUserRepository.save(BCUser);

                results.add("닉네임 " + nickName + "을(를) 가진 사용자가 AMB에 이미 존재합니다.");
            }

            // MongoDB와 AMB에 모두 데이터가 있는 경우
            else if (existingBCUser != null && !ambResult.isEmpty()) {
                results.add("닉네임 " + nickName + "을(를) 가진 BCUser가 MongoDB와 AMB에 모두 이미 존재합니다.");
            }
        }
        // 결과 리스트를 문자열로 변환하여 반환
        return results.stream().collect(Collectors.joining("\n"));
    }

    public String createUsersTest() {
        List<User> users = userRepository.findAll();
        List<String> results = new ArrayList<>();

        for (User user : users) {
            String nickName = user.getNickName();

            // BCUser 컬렉션에 중복되는 닉네임이 있는지 확인
            BCUser existingUser = BCUserRepository.findByNickName(nickName);
            if (existingUser != null) {
                results.add("닉네임 " + nickName + "은(는) 이미 존재합니다.");
                continue;
            }

            int mymPoint = 0;
            List<String> ownedToken = new ArrayList<>();

            // UUID 생성
            UUID uuid = UUID.randomUUID();

            // userNumber 생성
            String userNumber = generateUserId(uuid.toString());
            System.out.println("userNumber " + userNumber);

            // AMB와 MongoDB에 동시에 데이터 저장 요청
            String ambResult;
            try {
                ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                                "--tls --cafile %s " +
                                "--channelID %s " +
                                "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%s\", \"%d\", \"%s\"]}'",
                        caFilePath, channelID, chaincodeName, userNumber, nickName, mymPoint, ownedToken));
            } catch (Exception e) {
                e.printStackTrace();
                results.add("닉네임 " + nickName + "의 AMB 데이터 생성 중 오류가 발생했습니다: " + e.getMessage());
                continue;
            }

            // 3초 대기
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

            // MongoDB에 데이터 저장 (ownedToken 필드 제외)
            BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                    .userNumber(userNumber)
                    .nickName(nickName)
                    .mymPoint(mymPoint)
                    .blockCreatedTime(LocalDateTime.now())
                    .build();
            BCUserRepository.save(BCUser);

            if (ambResult.contains("success")) {
                results.add("닉네임 " + nickName + "의 데이터가 AMB와 MongoDB에 성공적으로 저장되었습니다.");
            } else {
                results.add("닉네임 " + nickName + "의 AMB 데이터 생성에 실패했습니다: " + ambResult);
            }
        }
        // 결과 리스트를 문자열로 변환하여 반환
        return results.stream().collect(Collectors.joining("\n"));
    }

    // 기존의 유저 데이터 생성(몽고디비만)
    public String createdUserMongo() {
        List<User> users = userRepository.findAll();
        List<String> results = new ArrayList<>();
        Set<String> processedNickNames = new HashSet<>();

        for (User user : users) {
            String nickName = user.getNickName();
            int mymPoint = 0;
            List<String> ownedToken = new ArrayList<>();

            // UUID 생성
            UUID uuid = UUID.randomUUID();

            // userNumber 생성
            String userNumber = generateUserId(uuid.toString());
            System.out.println("userNumber " + userNumber);

            // MongoDB에 NickName이 이미 존재하는지 확인
            BCUser existingBCUser = BCUserRepository.findByNickName(nickName);

            if (existingBCUser == null) {
                // MongoDB에 데이터 저장
                BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                        .userNumber(userNumber)
                        .nickName(nickName)
                        .mymPoint(mymPoint)
                        .blockCreatedTime(LocalDateTime.now())
                        .build();
                BCUserRepository.save(BCUser);

                results.add("닉네임 " + nickName + "을(를) 가진 사용자가 MongoDB에 추가되었습니다.");
            } else {
                if (!processedNickNames.contains(nickName)) {
                    results.add("닉네임 " + nickName + "을(를) 가진 사용자가 MongoDB에 이미 존재합니다.");
                    processedNickNames.add(nickName);
                }
            }
        }
        // 결과 리스트를 문자열로 변환하여 반환
        return results.stream().collect(Collectors.joining("\n"));
    }

    // 해당 유저를 조회하는 메서드
    public String getUser(String nickName) {

        return executeCommand(String.format("docker exec cli peer chaincode query " +
                "--tls --cafile %s " +
                "--channelID %s " +
                "--name %s -c '{\"Args\":[\"GetUser\", \"%s\"]}'", caFilePath, channelID, chaincodeName, nickName));
    }

    // 모든 유저를 조회하는 메서드
    public String getAllUsers() {

        return executeCommand(String.format("docker exec cli peer chaincode query " +
                "--tls --cafile %s " +
                "--channelID %s " +
                "--name %s -c '{\"Args\":[\"GetAllUsers\"]}'", caFilePath, channelID, chaincodeName));
    }

    // 모든 유저를 조회하는 메서드(자바 객체 변환)
    public List<BCUser> getAllUsersView() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();

        String result = executeCommand(String.format("docker exec cli peer chaincode query " +
                "--tls --cafile %s " +
                "--channelID %s " +
                "--name %s -c '{\"Args\":[\"GetAllUsers\"]}'", caFilePath, channelID, chaincodeName));

        return objectMapper.readValue(result, new TypeReference<>() {
        });
    }

    // 중복되지 않는 닉네임 조회하는 메서드
    public String findMismatchedNickNames() {
        // 모든 User 컬렉션의 nickName 필드 가져오기
        List<String> userNickNames = userRepository.findAll()
                .stream()
                .map(User::getNickName)
                .collect(Collectors.toList());

        // 모든 BCUser 컬렉션의 nickName 필드 가져오기
        List<String> bcUserNickNames = BCUserRepository.findAll()
                .stream()
                .map(BCUser::getNickName)
                .collect(Collectors.toList());

        // 중복되지 않는 닉네임 찾기
        List<String> mismatchedNickNames = userNickNames.stream()
                .filter(nickName -> !bcUserNickNames.contains(nickName))
                .collect(Collectors.toList());

        // 중복되는 닉네임 찾기
        List<String> duplicatedNickNames = userNickNames.stream()
                .filter(bcUserNickNames::contains)
                .collect(Collectors.toList());

        // 결과 문자열 생성
        String result = "중복되고 있는 도큐먼트: " + String.join(", ", duplicatedNickNames) + "\n" +
                "중복되지 않는 도큐먼트: " + String.join(", ", mismatchedNickNames);

        return result;
    }

    public String findDuplicatedBCUsers() {
        // 모든 BCUser 컬렉션의 nickName 필드 가져오기
        List<BCUser> bcUsers = BCUserRepository.findAll();

        // 중복되는 닉네임을 가진 BCUser 찾기
        Map<String, List<BCUser>> nickNameToUsersMap = bcUsers.stream()
                .collect(Collectors.groupingBy(BCUser::getNickName));

        List<BCUser> duplicatedBCUsers = nickNameToUsersMap.values().stream()
                .filter(users -> users.size() > 1)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 결과 문자열 생성
        String result = "중복되는 닉네임을 가진 BCUser: \n" +
                duplicatedBCUsers.stream()
                        .map(user -> "닉네임: " + user.getNickName() + ", ID: ")
                        .collect(Collectors.joining("\n"));

        return result;
    }

    public Map<String, Object> getUserWebtoonData(String memberId) {
        // 조건 1: status가 "OD"인 도큐먼트 필터링
        List<Pay> payList = payRepository.findByStatus("OD");

        // 조건 2: 해당 유저의 memberId값의 도큐먼트 필터링
        List<Pay> filteredPayList = payList.stream()
                .filter(pay -> pay.getMemberId().equals(memberId))
                .collect(Collectors.toList());

        // 결과를 담을 List
        List<Map<String, Object>> webtoonDataList = new ArrayList<>();

        // 웹툰 별로 나누기
        Set<String> fundingIds = filteredPayList.stream()
                .map(Pay::getFundingId)
                .collect(Collectors.toSet());

        int totalTickets = 0;
        int totalTicketCount = 546;  // 고정된 총 갯수

        for (String fundingId : fundingIds) {
            // 해당 fundingId의 Pay 도큐먼트 필터링
            List<Pay> fundingPayList = filteredPayList.stream()
                    .filter(pay -> pay.getFundingId().equals(fundingId))
                    .collect(Collectors.toList());

            // ticketCount 합산
            int totalTicket = fundingPayList.stream()
                    .mapToInt(Pay::getTicketCount)
                    .sum();

            // Funding 컬렉션에서 해당 fundingId로 데이터 조회
            Optional<Funding> fundingOptional = fundingRepository.findById(fundingId);

            if (fundingOptional.isPresent()) {
                Funding funding = fundingOptional.get();

                // 구매 비율 계산
                double purchaseRatio = (double) totalTicket / totalTicketCount * 100;

                // 결과 데이터 구성
                Map<String, Object> webtoonData = new HashMap<>();
                webtoonData.put("fundingID", funding.getFundingId().toString());
                webtoonData.put("synopsisTitle", funding.getSynopsisTitle());
                webtoonData.put("nftImageWebThumbnail", funding.getIntroImageLink3());
                webtoonData.put("nftImageMobileThumbnail", funding.getIntroMobImageLink3());
                webtoonData.put("totalTicket", totalTicket);
                webtoonData.put("purchaseRatio", purchaseRatio);

                webtoonDataList.add(webtoonData);
            }
        }

        // 유저 정보 조회
        Optional<User> userOptional = userRepository.findById(memberId);
        String userNickName = "";
        String userBlockchainId = "";

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            userNickName = user.getNickName();

            // Blockchain 유저 정보 조회
            BCUser bcUser = BCUserRepository.findByNickName(userNickName);
            if (bcUser != null) {
                userBlockchainId = bcUser.getUserNumber();
            }
        }

        // Token 컬렉션에서 해당 유저 닉네임과 일치하는 owner 필드의 도큐먼트 수를 totalTickets로 설정
        totalTickets = tokenRepository.countByOwner(userNickName);

        // extradata 구성
        Map<String, Object> exdata = new HashMap<>();
        exdata.put("userNickName", userNickName);
        exdata.put("userBlockchainID", userBlockchainId);
        exdata.put("userTotalTicket", totalTickets);

        // 최종 결과 구성
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("data", webtoonDataList);  // 웹툰 데이터 리스트를 추가
        finalResult.put("extradata", exdata);

        return finalResult;
    }

    // 지정된 유저를 삭제하는 메서드
    public String deleteUser(String nickName) {
        // MongoDB에서 사용자 찾기
        BCUser bcUser = BCUserRepository.findByNickName(nickName);
        if (bcUser == null) {
            return "User with nickname " + nickName + " does not exist in MongoDB.";
        }
        // 몽고디비에서 삭제
        BCUserRepository.delete(bcUser);

        String ambResult;
        try {
            // Hyperledger Fabric에서 사용자 블록 삭제
            ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                    "--tls --cafile %s " +
                    "--channelID %s " +
                    "--name %s -c '{\"Args\":[\"DeleteUser\", \"%s\"]}'", caFilePath, channelID, chaincodeName, nickName));
        } catch (Exception e) {
            return "Failed to delete user block from Hyperledger Fabric: " + e.getMessage();
        }

        return "Tokens deleted from MongoDB and Hyperledger Fabric for user " + nickName + " with result: " + ambResult;
    }

    // 모든 유저 데이터를 삭제하는 메서드
    public String deleteAllUserBlocks() {
        // MongoDB에서 모든 사용자 삭제
        BCUserRepository.deleteAll();

        // AMB에서 모든 사용자 블록 삭제 요청
        String ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                "--tls --cafile %s " +
                "--channelID %s " +
                "--name %s -c '{\"Args\":[\"DeleteAllUserBlocks\"]}'", caFilePath, channelID, chaincodeName));

        // 결과 메시지 생성
        StringBuilder result = new StringBuilder();
        result.append("User block deleted from AMB with result: ").append(ambResult).append("\n");
        result.append("삭제가 완료되었습니다.");

        return result.toString();
    }

    // n개의 유저 정보를 BCUser 컬렉션에 생성하는 메서드
    public String createMultipleUsers(String NickName, int mymPoint, int count) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < count; i++) {
            String nickName = NickName + i;

            // ownedToken 지역 변수 생성
            List<String> ownedToken = new ArrayList<>();

            // AMB에 데이터 저장 요청
            String ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%d\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, nickName, mymPoint, ownedToken));

            // MongoDB에 데이터 저장 (ownedToken 필드 제외)
            BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                    .nickName(nickName)
                    .mymPoint(mymPoint)
                    .blockCreatedTime(LocalDateTime.now())
                    .build();
            BCUserRepository.save(BCUser);

            result.append("User ").append(nickName).append(" created in AMB with result: ").append(ambResult).append(" and MongoDB.\n");

            try {
                // 3000밀리초 대기
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        return result.toString();
    }

    // n개의 유저 정보를 BCUser 컬렉션에 생성하는 메서드(테스트)
    public String createMultipleUsersTest(String NickName, int mymPoint, int count) {
        StringBuilder result = new StringBuilder();
        List<String> successfulNickNames = new ArrayList<>();

        // AMB에 데이터 저장 요청
        for (int i = 0; i < count; i++) {
            String nickName = NickName + i;

            // ownedToken 지역 변수 생성
            List<String> ownedToken = new ArrayList<>();

            // AMB에 데이터 저장 요청
            String ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%d\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, nickName, mymPoint, ownedToken));

            if (!ambResult.contains("error")) {
                successfulNickNames.add(nickName);
                result.append("User ").append(nickName).append(" created in AMB with result: ").append(ambResult).append("\n");
            } else {
                result.append("Failed to create user ").append(nickName).append(" in AMB with result: ").append(ambResult).append("\n");
            }
        }

        // MongoDB에 데이터 저장 (ownedToken 필드 제외)
        for (String nickName : successfulNickNames) {
            BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                    .nickName(nickName)
                    .mymPoint(mymPoint)
                    .blockCreatedTime(LocalDateTime.now())
                    .build();
            BCUserRepository.save(BCUser);
            result.append("User ").append(nickName).append(" created in MongoDB.\n");
        }

        return result.toString();
    }

    /// 리눅스 및 윈도우 터미널 사용 메서드
    private String executeCommand(String command) {

        StringBuilder output = new StringBuilder();
        Process process = null;
        BufferedReader reader = null;

        try {
            // 운영 체제에 따라 명령어 실행 방법을 결정합니다.
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Windows인 경우
                process = new ProcessBuilder("cmd.exe", "/c", command).start();
            } else {
                // Unix 계열 (Linux, macOS 등)인 경우
                process = new ProcessBuilder("sh", "-c", command).start();
            }

            // 실행 결과를 읽어오기 위한 BufferedReader를 생성합니다.
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 실행 결과를 읽어와 StringBuilder에 추가합니다.
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 프로세스가 종료될 때까지 기다립니다.
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 리소스를 해제합니다.
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }

    // 토큰 생성시 SHA-256 이용 메서드
    private static String generateUserId(String input) {
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
