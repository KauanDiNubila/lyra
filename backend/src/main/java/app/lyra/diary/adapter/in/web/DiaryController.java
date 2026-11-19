package app.lyra.diary.adapter.in.web;

import app.lyra.diary.application.port.in.ChangeEntryQuantityUseCase;
import app.lyra.diary.application.port.in.DeleteEntryUseCase;
import app.lyra.diary.application.port.in.GetDiaryDayUseCase;
import app.lyra.diary.application.port.in.LogEntryUseCase;
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
    private final ChangeEntryQuantityUseCase changeEntryQuantity;
    private final DeleteEntryUseCase deleteEntry;

    @GetMapping("/{date}")
    DiaryDayResponse day(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DiaryDayResponse.from(getDiaryDay.getDay(date));
    }

    @PostMapping("/{date}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    EntryResponse log(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                      @RequestBody LogEntryRequest request) {
        return EntryResponse.from(logEntry.log(request.toCommand(date)));
    }

    @PatchMapping("/entries/{id}")
    EntryResponse changeQuantity(@PathVariable UUID id, @RequestBody ChangeQuantityRequest request) {
        return EntryResponse.from(changeEntryQuantity.changeQuantity(id, request.toQuantity()));
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        deleteEntry.delete(id);
    }
}
