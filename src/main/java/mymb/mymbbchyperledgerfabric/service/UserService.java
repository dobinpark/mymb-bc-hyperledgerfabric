package mymb.mymbbchyperledgerfabric.service;

import lombok.RequiredArgsConstructor;
import mymb.mymbbchyperledgerfabric.entity.BCUser;
import mymb.mymbbchyperledgerfabric.entity.User;
import mymb.mymbbchyperledgerfabric.repository.BCUserRepository;
import mymb.mymbbchyperledgerfabric.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final BCUserRepository BCUserRepository;
    private final UserRepository userRepository;

    String caFilePath = "/opt/home/managedblockchain-tls-chain.pem";
    String channelID = "mychannel";
    String chaincodeName = "mycc";

    // 유저 정보 블록을 생성하는 메서드(회원가입시)
    public String createSigninUserBlock(String nickName, int mymPoint, List<String> ownedToken) {

        // MongoDB에 NickName이 이미 존재하는지 확인
        User existingUser = userRepository.findByNickName(nickName);
        if (existingUser != null) {
            // 이미 존재하는 경우에는 아무 동작도 수행하지 않고 종료
            return "User with nickname " + nickName + " already exists in MongoDB";
        }

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

        //AMB에 데이터 저장 요청
        ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                        "--tls --cafile %s " +
                        "--channelID %s " +
                        "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%d\", \"%s\"]}'",
                caFilePath, channelID, chaincodeName, nickName, mymPoint, ownedToken));

        // MongoDB에 데이터 저장
        BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                .nickName(nickName)
                .mymPoint(mymPoint)
                .ownedToken(ownedToken)
                .blockCreatedTime(LocalDateTime.now())
                .build();
        BCUserRepository.save(BCUser);

        return "AMB " + ambResult + " MongoDB : Data saved successfully";
    }

    // 유저 정보 블록을 생성하는 메서드(기존에 가입되어 있는 유저가 있을시)
    public String createLoginUserBlock(String nickName, int mymPoint, List<String> ownedToken) {

        // MongoDB에 NickName이 이미 존재하는지 확인
        BCUser existingBCUser = BCUserRepository.findByNickName(nickName);

        // AMB에 NickName이 이미 존재하는지 확인
        String ambResult = getUser(nickName);

        // MongoDB와 AMB에 모두 데이터가 있는 경우
        if (existingBCUser != null && !ambResult.isEmpty()) {
            return "BCUser with nickname " + nickName + " already exists in both MongoDB and AMB";
        }
        // MongoDB에만 데이터가 있는 경우
        else if (existingBCUser != null && ambResult.isEmpty()) {

            ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%d\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, nickName, mymPoint, ownedToken));

            return "BCUser with nickname " + nickName + " already exists in MongoDB";
        }
        // AMB에만 데이터가 있는 경우
        else if (existingBCUser == null && !ambResult.isEmpty()) {

            // MongoDB에 데이터 저장
            BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                    .nickName(nickName)
                    .mymPoint(mymPoint)
                    .ownedToken(ownedToken)
                    .blockCreatedTime(LocalDateTime.now())
                    .build();
            BCUserRepository.save(BCUser);

            return "AMB with nickname " + nickName + " already exists in AMB";
        }
        // 둘 다 데이터가 없는 경우
        else {
            //AMB에 데이터 저장 요청
            ambResult = executeCommand(String.format("docker exec cli peer chaincode invoke " +
                            "--tls --cafile %s " +
                            "--channelID %s " +
                            "--name %s -c '{\"Args\":[\"CreateUserBlock\", \"%s\", \"%d\", \"%s\"]}'",
                    caFilePath, channelID, chaincodeName, nickName, mymPoint, ownedToken));

            // MongoDB에 데이터 저장
            BCUser BCUser = mymb.mymbbchyperledgerfabric.entity.BCUser.builder()
                    .nickName(nickName)
                    .mymPoint(mymPoint)
                    .ownedToken(ownedToken)
                    .blockCreatedTime(LocalDateTime.now())
                    .build();
            BCUserRepository.save(BCUser);

            return "AMB " + ambResult + " MongoDB : Data saved successfully";
        }
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
}
