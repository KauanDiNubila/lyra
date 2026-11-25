package app.lyra.diary.adapter.in.web;

import app.lyra.diary.application.port.in.ChangeEntryUseCase;
import app.lyra.diary.application.port.in.DeleteEntryUseCase;
import app.lyra.diary.application.port.in.FillDayFromPlanUseCase;
import app.lyra.diary.application.port.in.GetDiaryDayUseCase;
import app.lyra.diary.application.port.in.LogEntryUseCase;
import app.lyra.diary.application.port.in.QuickAddUseCase;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
class DiaryController {

    private final GetDiaryDayUseCase getDiaryDay;
    private final LogEntryUseCase logEntry;
    private final QuickAddUseCase quickAdd;
    private final FillDayFromPlanUseCase fillDay;
    private final ChangeEntryUseCase changeEntry;
    private final DeleteEntryUseCase deleteEntry;

    @GetMapping("/{date}")
    DiaryDayResponse getDay(UserId userId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DiaryDayResponse.from(getDiaryDay.getDay(userId, date));
    }

    @PostMapping("/{date}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    EntryResponse addEntry(UserId userId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                      @RequestBody LogEntryRequest request) {
        return EntryResponse.from(logEntry.log(userId, request.toCommand(date)));
    }

    @PostMapping("/{date}/quick-entries")
    @ResponseStatus(HttpStatus.CREATED)
    EntryResponse addQuickEntry(UserId userId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestBody QuickAddRequest request) {
        return EntryResponse.from(quickAdd.quickAdd(userId, request.toCommand(date)));
    }

    @PostMapping("/{date}/plan")
    DiaryDayResponse fillDayFromPlan(UserId userId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DiaryDayResponse.from(fillDay.fillDay(userId, date));
    }

    @PatchMapping("/entries/{id}")
    EntryResponse changeEntry(UserId userId, @PathVariable UUID id, @RequestBody ChangeEntryRequest request) {
        return EntryResponse.from(changeEntry.change(userId, id, request.toCommand()));
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeEntry(UserId userId, @PathVariable UUID id) {
        deleteEntry.delete(userId, id);
    }
}
