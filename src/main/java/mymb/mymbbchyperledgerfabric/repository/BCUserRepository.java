package mymb.mymbbchyperledgerfabric.repository;

import mymb.mymbbchyperledgerfabric.entity.BCUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BCUserRepository extends MongoRepository<BCUser, String> {

    BCUser findByNickName(String nickName);
}
