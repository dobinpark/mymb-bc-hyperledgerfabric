package mymb.mymbbchyperledgerfabric.repository;

import mymb.mymbbchyperledgerfabric.entity.BCUser;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BCUserRepository extends MongoRepository<BCUser, String> {

    BCUser findByNickName(String nickName);
}
