package br.com.agendaflex.security;

import br.com.agendaflex.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase();
        return usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .map(UsuarioPrincipal::de)
                .orElseThrow(() -> new UsernameNotFoundException("E-mail ou senha inválidos."));
    }
}
