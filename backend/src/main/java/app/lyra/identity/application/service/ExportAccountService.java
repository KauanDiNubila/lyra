package app.lyra.identity.application.service;

import app.lyra.diary.application.port.in.ListDiaryEntriesUseCase;
import app.lyra.food.application.port.in.ListCustomFoodsUseCase;
import app.lyra.identity.application.port.in.ExportAccountUseCase;
import app.lyra.identity.application.port.out.UserRepository;
import app.lyra.identity.domain.User;
import app.lyra.identity.domain.UserNotFoundException;
import app.lyra.plan.application.port.in.ListPlansUseCase;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ExportAccountService implements ExportAccountUseCase {

    private final UserRepository userRepository;
    private final ListCustomFoodsUseCase listCustomFoods;
    private final ListPlansUseCase listPlans;
    private final ListDiaryEntriesUseCase listDiaryEntries;

    @Override
    public AccountExport export(UserId userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return new AccountExport(
                user,
                listCustomFoods.listCustomFoods(userId),
                listPlans.listPlans(userId),
                listDiaryEntries.listEntries(userId));
    }
}
