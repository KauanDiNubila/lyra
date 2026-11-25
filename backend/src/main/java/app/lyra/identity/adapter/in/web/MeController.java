package app.lyra.identity.adapter.in.web;

import app.lyra.identity.application.port.in.DeleteAccountUseCase;
import app.lyra.identity.application.port.in.ExportAccountUseCase;
import app.lyra.identity.application.port.in.GetCurrentUserUseCase;
import app.lyra.shared.domain.UserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
class MeController {

    private static final String EXPORT_DISPOSITION = "attachment; filename=\"lyra-export.json\"";

    private final GetCurrentUserUseCase getCurrentUser;
    private final ExportAccountUseCase exportAccountUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;

    @GetMapping
    MeResponse me(UserId userId) {
        return MeResponse.from(getCurrentUser.getUser(userId));
    }

    @GetMapping("/export")
    ResponseEntity<AccountExportResponse> exportAccount(UserId userId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, EXPORT_DISPOSITION)
                .body(AccountExportResponse.from(exportAccountUseCase.export(userId)));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAccount(UserId userId, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        deleteAccountUseCase.delete(userId);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
}
