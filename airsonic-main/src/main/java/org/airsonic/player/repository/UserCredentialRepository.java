package org.airsonic.player.repository;

import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Integer> {

    List<UserCredential> findByUserUsernameAndApp(String username, App app);

    List<UserCredential> findByUserAndAppIn(User user, Iterable<App> apps);

    List<UserCredential> findByEncoderStartsWith(String encoder);

    Integer countByUserAndApp(User user, App app);

    boolean existsByEncoderStartsWith(String encoder);

    boolean existsByEncoderIn(Iterable<String> encoders);

    boolean existsByEncoderInAndAppNot(Iterable<String> encoders, App app);

    Optional<UserCredential> findFirstByAppAndAppUsernameAndCredential(App app, String appUsername,
            String credential);

}
