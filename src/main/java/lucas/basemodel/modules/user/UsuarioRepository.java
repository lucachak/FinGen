package lucas.basemodel.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    Optional<User> findByNomeCompletoIgnoreCase(String nomeCompleto);
    java.util.List<User> findAllByAtivoTrue();
}
